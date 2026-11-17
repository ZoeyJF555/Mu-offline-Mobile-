package com.muoffline.mobile.data

import android.content.Context
import com.muoffline.mobile.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AuthResult(
    val ok: Boolean,
    val accountId: String? = null,
    val error: String? = null
)

data class RemoteSyncResult(
    val ok: Boolean,
    val message: String,
    val manifest: ManifestInfo? = null
)

class GameRepository(context: Context) {

    private val store = JsonDataStore(context)
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
            return@withContext AuthResult(false, error = "El ID debe tener al menos 3 caracteres (letras, números, - o _).")
        }
        if (password.length < 4) {
            return@withContext AuthResult(false, error = "La contraseña debe tener al menos 4 caracteres.")
        }
        val accounts = store.getAllAccounts()
        if (accounts.any { it.accountId == accountId }) {
            return@withContext AuthResult(false, error = "Ese ID de cuenta ya existe en este dispositivo.")
        }
        store.putAccount(AccountRecord(accountId, hashPassword(accountId, password), System.currentTimeMillis()))
        prefs.edit().putString("active_account", accountId).apply()
        AuthResult(true, accountId = accountId)
    }

    suspend fun loginAccount(rawId: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val accountId = rawId.trim().lowercase()
        val found = store.getAllAccounts().firstOrNull { it.accountId == accountId }
            ?: return@withContext AuthResult(false, error = "Cuenta no encontrada. Regístrate primero.")
        if (found.passwordHash != hashPassword(accountId, password)) {
            return@withContext AuthResult(false, error = "Contraseña incorrecta.")
        }
        prefs.edit().putString("active_account", accountId).apply()
        AuthResult(true, accountId = accountId)
    }

    suspend fun loadHeroes(accountId: String): List<HeroSave> = withContext(Dispatchers.IO) {
        store.loadHeroes(accountId)
    }

    suspend fun saveHero(hero: HeroSave) = withContext(Dispatchers.IO) {
        store.saveHero(hero)
    }

    suspend fun deleteHero(accountId: String, heroId: String) = withContext(Dispatchers.IO) {
        store.deleteHero(accountId, heroId)
    }

    suspend fun loadVault(accountId: String): List<Item> = withContext(Dispatchers.IO) {
        store.loadVault(accountId)
    }

    suspend fun saveVault(accountId: String, items: List<Item>) = withContext(Dispatchers.IO) {
        store.saveVault(accountId, items)
    }

    suspend fun loadMods(): List<ModPackage> = withContext(Dispatchers.IO) {
        val stored = store.loadStoredMods()
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
        store.saveMods(mods)
    }

    suspend fun loadSettings(): MobileHudSettings = withContext(Dispatchers.IO) {
        store.loadSettings()
    }

    suspend fun saveSettings(settings: MobileHudSettings) = withContext(Dispatchers.IO) {
        store.saveSettings(settings)
    }

    suspend fun loadMapsConfig(): Map<MapId, MapMeta> = withContext(Dispatchers.IO) {
        store.loadMapsConfig()
    }

    suspend fun saveMapsConfig(maps: Map<MapId, MapMeta>) = withContext(Dispatchers.IO) {
        store.saveMapsConfig(maps)
    }

    suspend fun loadMapsRawJson(): String = withContext(Dispatchers.IO) { store.loadMapsRawJson() }

    suspend fun saveMapsRawJson(raw: String): Result<Map<MapId, MapMeta>> = withContext(Dispatchers.IO) {
        store.saveMapsRawJson(raw)
    }

    suspend fun loadNpcs(): List<MapNpcDef> = withContext(Dispatchers.IO) { store.loadNpcs() }

    suspend fun loadNpcsRawJson(): String = withContext(Dispatchers.IO) { store.loadNpcsRawJson() }

    suspend fun saveNpcsRawJson(raw: String): Result<List<MapNpcDef>> = withContext(Dispatchers.IO) {
        store.saveNpcsRawJson(raw)
    }

    suspend fun loadManifest(): ManifestInfo = withContext(Dispatchers.IO) { store.loadManifest() }

    fun exportModJson(mod: ModPackage): String = JsonDataStore.encodeMod(mod).toString(2)

    fun parseImportedModJson(rawJson: String): Result<ModPackage> = runCatching {
        val obj = JSONObject(rawJson)
        require(obj.has("name") && obj.getString("name").isNotBlank()) { "Archivo inválido" }
        JsonDataStore.decodeMod(obj, forceImported = true)
    }

    suspend fun syncFromManifestUrl(manifestUrlRaw: String): RemoteSyncResult = withContext(Dispatchers.IO) {
        val trimmed = manifestUrlRaw.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return@withContext RemoteSyncResult(false, "Ingresa una URL http:// o https:// válida hacia manifest.json.")
        }
        return@withContext runCatching {
            val manifestText = fetchUrlText(trimmed)
            val mObj = JSONObject(manifestText)
            val version = mObj.optString("version", "1.0.0")
            val notes = mObj.optString("notes", "Actualización descargada")
            val updatedAt = mObj.optString("updatedAt", "Hoy")

            val baseDir = trimmed.substringBeforeLast("/", trimmed)
            val filesArr = mObj.optJSONArray("files")
            val filesList = mutableListOf<String>()
            if (filesArr != null) {
                for (i in 0 until filesArr.length()) {
                    filesList += filesArr.optString(i)
                }
            }
            if ("mapas.json" in filesList) {
                runCatching { fetchUrlText("$baseDir/mapas.json") }.getOrNull()?.let { store.saveMapsRawJson(it) }
            }
            if ("npcs.json" in filesList) {
                runCatching { fetchUrlText("$baseDir/npcs.json") }.getOrNull()?.let { store.saveNpcsRawJson(it) }
            }
            store.saveManifestRaw(manifestText)
            val info = ManifestInfo(version = version, notes = notes, updatedAt = updatedAt)
            RemoteSyncResult(true, "Sincronizado v$version: $notes", info)
        }.getOrElse { err ->
            RemoteSyncResult(false, "Sin conexión o URL inaccesible (${err.message ?: "error de red"}).")
        }
    }

    private fun fetchUrlText(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            requestMethod = "GET"
        }
        return try {
            val code = conn.responseCode
            require(code in 200..299) { "HTTP $code" }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
