package com.muoffline.mobile.data

import android.content.Context
import com.muoffline.mobile.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest

data class AuthResult(
    val ok: Boolean,
    val accountId: String? = null,
    val error: String? = null
)

class GameRepository(context: Context) {

    private val db = LocalDatabase(context)
    private val prefs = context.applicationContext.getSharedPreferences("mu_offline_prefs", Context.MODE_PRIVATE)

    fun getActiveSessionAccount(): String? = prefs.getString("active_account", null)

    fun logoutAccount() {
        prefs.edit().remove("active_account").apply()
    }

    private fun hashPassword(accountId: String, password: String): String {
        val input = "${accountId.lowercase()}::$password"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun registerAccount(rawId: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val accountId = rawId.trim().lowercase()
        if (accountId.length < 3 || !Regex("^[a-z0-9_-]+$").matches(accountId)) {
            return@withContext AuthResult(false, error = "El ID debe tener al menos 3 caracteres (solo letras, números, - o _).")
        }
        if (password.length < 4) {
            return@withContext AuthResult(false, error = "La contraseña debe tener al menos 4 caracteres.")
        }
        val accounts = db.getAllAccounts()
        if (accounts.any { it.accountId == accountId }) {
            return@withContext AuthResult(false, error = "Ese ID de cuenta ya existe en este dispositivo. Usa Ingresar.")
        }
        db.putAccount(AccountRecord(accountId, hashPassword(accountId, password), System.currentTimeMillis()))
        prefs.edit().putString("active_account", accountId).apply()
        AuthResult(true, accountId = accountId)
    }

    suspend fun loginAccount(rawId: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val accountId = rawId.trim().lowercase()
        val found = db.getAllAccounts().firstOrNull { it.accountId == accountId }
            ?: return@withContext AuthResult(false, error = "Cuenta no encontrada en este dispositivo. Regístrate primero.")
        if (found.passwordHash != hashPassword(accountId, password)) {
            return@withContext AuthResult(false, error = "Contraseña incorrecta.")
        }
        prefs.edit().putString("active_account", accountId).apply()
        AuthResult(true, accountId = accountId)
    }

    suspend fun loadHeroes(accountId: String): List<HeroSave> = withContext(Dispatchers.IO) {
        db.loadHeroes(accountId)
    }

    suspend fun saveHero(hero: HeroSave) = withContext(Dispatchers.IO) {
        db.saveHero(hero)
    }

    suspend fun deleteHero(heroId: String) = withContext(Dispatchers.IO) {
        db.deleteHero(heroId)
    }

    suspend fun loadMods(): List<ModPackage> = withContext(Dispatchers.IO) {
        val stored = db.loadCustomOrStoredMods()
        val byId = linkedMapOf<String, ModPackage>()
        for (builtIn in MuCatalog.BUILT_IN_MODS) {
            byId[builtIn.id] = builtIn
        }
        for (mod in stored) {
            val existing = byId[mod.id]
            byId[mod.id] = existing?.copy(enabled = mod.enabled) ?: mod
        }
        byId.values.toList()
    }

    suspend fun saveMods(mods: List<ModPackage>) = withContext(Dispatchers.IO) {
        db.saveMods(mods)
    }

    fun exportModJson(mod: ModPackage): String = LocalDatabase.encodeMod(mod).toString(2)

    fun parseImportedModJson(rawJson: String): Result<ModPackage> = runCatching {
        val obj = JSONObject(rawJson)
        require(obj.has("name") && obj.getString("name").isNotBlank()) { "Archivo inválido" }
        LocalDatabase.decodeMod(obj, forceImported = true)
    }
}
