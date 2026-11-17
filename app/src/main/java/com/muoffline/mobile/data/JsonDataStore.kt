package com.muoffline.mobile.data

import android.content.Context
import com.muoffline.mobile.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Pure flat-JSON persistence (zero SQL) inside filesDir/mu_data/ and bundled assets/data/.
 */
class JsonDataStore(context: Context) {

    private val appContext = context.applicationContext
    private val dataDir = File(appContext.filesDir, "mu_data").apply { mkdirs() }

    private val accountsFile = File(dataDir, "accounts.json")
    private val modsFile = File(dataDir, "mods.json")
    private val settingsFile = File(dataDir, "settings.json")
    private val mapasFile = File(dataDir, "mapas.json")
    private val npcsFile = File(dataDir, "npcs.json")
    private val manifestFile = File(dataDir, "manifest.json")

    init {
        ensureSeededFromAssets("mapas.json", mapasFile)
        ensureSeededFromAssets("npcs.json", npcsFile)
        ensureSeededFromAssets("manifest.json", manifestFile)
    }

    private fun ensureSeededFromAssets(assetName: String, target: File) {
        if (!target.exists()) {
            runCatching {
                appContext.assets.open("data/$assetName").bufferedReader().use { it.readText() }
            }.onSuccess { raw ->
                target.writeText(raw)
            }
        }
    }

    @Synchronized
    fun getAllAccounts(): List<AccountRecord> {
        if (!accountsFile.exists()) return emptyList()
        val arr = runCatching { JSONObject(accountsFile.readText()).optJSONArray("accounts") }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            AccountRecord(
                accountId = obj.optString("accountId"),
                passwordHash = obj.optString("passwordHash"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }

    @Synchronized
    fun putAccount(account: AccountRecord) {
        val current = getAllAccounts().filterNot { it.accountId == account.accountId } + account
        val root = JSONObject().apply {
            put("accounts", JSONArray().apply {
                current.forEach { a ->
                    put(JSONObject().apply {
                        put("accountId", a.accountId)
                        put("passwordHash", a.passwordHash)
                        put("createdAt", a.createdAt)
                    })
                }
            })
        }
        accountsFile.writeText(root.toString(2))
    }

    private fun heroesFileFor(accountId: String): File =
        File(dataDir, "heroes_${accountId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.json")

    private fun vaultFileFor(accountId: String): File =
        File(dataDir, "vault_${accountId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.json")

    @Synchronized
    fun loadHeroes(accountId: String): List<HeroSave> {
        val f = heroesFileFor(accountId)
        if (!f.exists()) return emptyList()
        val arr = runCatching { JSONObject(f.readText()).optJSONArray("heroes") }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let(::decodeHero)
        }.sortedByDescending { it.updatedAt }
    }

    @Synchronized
    fun saveHero(hero: HeroSave) {
        val list = loadHeroes(hero.accountId).filterNot { it.id == hero.id }
        val updated = listOf(hero) + list
        val root = JSONObject().apply {
            put("accountId", hero.accountId)
            put("heroes", JSONArray().apply {
                updated.forEach { put(encodeHero(it)) }
            })
        }
        heroesFileFor(hero.accountId).writeText(root.toString(2))
    }

    @Synchronized
    fun deleteHero(accountId: String, heroId: String) {
        val updated = loadHeroes(accountId).filterNot { it.id == heroId }
        val root = JSONObject().apply {
            put("accountId", accountId)
            put("heroes", JSONArray().apply {
                updated.forEach { put(encodeHero(it)) }
            })
        }
        heroesFileFor(accountId).writeText(root.toString(2))
    }

    @Synchronized
    fun loadVault(accountId: String): List<Item> {
        val f = vaultFileFor(accountId)
        if (!f.exists()) return emptyList()
        val arr = runCatching { JSONObject(f.readText()).optJSONArray("items") }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let(::decodeItem)
        }
    }

    @Synchronized
    fun saveVault(accountId: String, items: List<Item>) {
        val root = JSONObject().apply {
            put("accountId", accountId)
            put("items", JSONArray().apply { items.forEach { put(encodeItem(it)) } })
        }
        vaultFileFor(accountId).writeText(root.toString(2))
    }

    @Synchronized
    fun loadStoredMods(): List<ModPackage> {
        if (!modsFile.exists()) return emptyList()
        val arr = runCatching { JSONObject(modsFile.readText()).optJSONArray("mods") }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { decodeMod(it) }
        }
    }

    @Synchronized
    fun saveMods(mods: List<ModPackage>) {
        val root = JSONObject().apply {
            put("mods", JSONArray().apply {
                mods.forEach { put(encodeMod(it)) }
            })
        }
        modsFile.writeText(root.toString(2))
    }

    @Synchronized
    fun loadSettings(): MobileHudSettings {
        if (!settingsFile.exists()) return MobileHudSettings()
        val o = runCatching { JSONObject(settingsFile.readText()) }.getOrNull() ?: return MobileHudSettings()
        return MobileHudSettings(
            sfxVolume = o.optDouble("sfxVolume", 0.8).toFloat().coerceIn(0f, 1f),
            musicVolume = o.optDouble("musicVolume", 0.45).toFloat().coerceIn(0f, 1f),
            brightness = o.optDouble("brightness", 1.0).toFloat().coerceIn(0.35f, 1.0f),
            hdGraphics = o.optBoolean("hdGraphics", true),
            animationsEnabled = o.optBoolean("animationsEnabled", true),
            hudScale = o.optDouble("hudScale", 1.0).toFloat().coerceIn(0.75f, 1.35f),
            hudBottomPaddingDp = o.optDouble("hudBottomPaddingDp", 6.0).toFloat().coerceIn(0f, 40f),
            manifestUrl = o.optString("manifestUrl", "")
        )
    }

    @Synchronized
    fun saveSettings(s: MobileHudSettings) {
        val root = JSONObject().apply {
            put("sfxVolume", s.sfxVolume.toDouble())
            put("musicVolume", s.musicVolume.toDouble())
            put("brightness", s.brightness.toDouble())
            put("hdGraphics", s.hdGraphics)
            put("animationsEnabled", s.animationsEnabled)
            put("hudScale", s.hudScale.toDouble())
            put("hudBottomPaddingDp", s.hudBottomPaddingDp.toDouble())
            put("manifestUrl", s.manifestUrl)
        }
        settingsFile.writeText(root.toString(2))
    }

    @Synchronized
    fun loadManifest(): ManifestInfo {
        if (!manifestFile.exists()) return ManifestInfo()
        val o = runCatching { JSONObject(manifestFile.readText()) }.getOrNull() ?: return ManifestInfo()
        return ManifestInfo(
            version = o.optString("version", "6.3.1"),
            notes = o.optString("notes", "Configuración local"),
            updatedAt = o.optString("updatedAt", "2026-11-16")
        )
    }

    @Synchronized
    fun saveManifestRaw(raw: String) {
        manifestFile.writeText(raw)
    }

    @Synchronized
    fun loadMapsConfig(): Map<MapId, MapMeta> {
        val merged = MuCatalog.DEFAULT_MAP_CATALOG.toMutableMap()
        if (!mapasFile.exists()) return merged
        val obj = runCatching { JSONObject(mapasFile.readText()) }.getOrNull() ?: return merged
        val arr = obj.optJSONArray("maps") ?: return merged
        for (i in 0 until arr.length()) {
            val m = arr.optJSONObject(i) ?: continue
            val id = MapId.fromWire(m.optString("id"))
            val base = merged[id] ?: continue
            merged[id] = base.copy(
                name = m.optString("name", base.name),
                minLevel = m.optInt("minLevel", base.minLevel),
                warpLevel = m.optInt("warpLevel", base.warpLevel),
                expRate = m.optDouble("expRate", base.expRate).coerceIn(0.05, 50.0),
                dropZenRate = m.optDouble("dropZen", base.dropZenRate).coerceIn(0.1, 50.0),
                dropItemRate = m.optDouble("dropItems", base.dropItemRate).coerceIn(0.05, 1.0),
                subtitle = m.optString("subtitle", base.subtitle)
            )
        }
        return merged
    }

    @Synchronized
    fun saveMapsConfig(maps: Map<MapId, MapMeta>) {
        val root = JSONObject().apply {
            put("maps", JSONArray().apply {
                maps.values.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id.wire)
                        put("name", m.name)
                        put("minLevel", m.minLevel)
                        put("warpLevel", m.warpLevel)
                        put("expRate", m.expRate)
                        put("dropZen", m.dropZenRate)
                        put("dropItems", m.dropItemRate)
                        put("subtitle", m.subtitle)
                    })
                }
            })
        }
        mapasFile.writeText(root.toString(2))
    }

    @Synchronized
    fun loadMapsRawJson(): String =
        if (mapasFile.exists()) mapasFile.readText() else "{}"

    @Synchronized
    fun saveMapsRawJson(raw: String): Result<Map<MapId, MapMeta>> = runCatching {
        val parsed = JSONObject(raw)
        require(parsed.has("maps")) { "El JSON debe contener la clave 'maps'." }
        mapasFile.writeText(parsed.toString(2))
        loadMapsConfig()
    }

    @Synchronized
    fun loadNpcs(): List<MapNpcDef> {
        if (!npcsFile.exists()) return MuCatalog.DEFAULT_NPCS
        val rootObj = runCatching { JSONObject(npcsFile.readText()) }.getOrNull() ?: return MuCatalog.DEFAULT_NPCS
        if (rootObj.optInt("schemaVersion", 1) < 2) {
            runCatching {
                appContext.assets.open("data/npcs.json").bufferedReader().use { it.readText() }
            }.onSuccess { raw ->
                npcsFile.writeText(raw)
                return MuCatalog.DEFAULT_NPCS
            }
        }
        val arr = rootObj.optJSONArray("npcs") ?: return MuCatalog.DEFAULT_NPCS
        val list = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            MapNpcDef(
                id = o.optString("id"),
                mapId = MapId.fromWire(o.optString("mapId")),
                name = o.optString("name"),
                role = o.optString("role", "shop"),
                subtitle = o.optString("subtitle", "Tienda"),
                wx = o.optDouble("wx", 440.0).toFloat(),
                wy = o.optDouble("wy", 440.0).toFloat()
            )
        }
        return list.ifEmpty { MuCatalog.DEFAULT_NPCS }
    }

    @Synchronized
    fun saveNpcsRawJson(raw: String): Result<List<MapNpcDef>> = runCatching {
        val parsed = JSONObject(raw)
        require(parsed.has("npcs")) { "El JSON debe contener la clave 'npcs'." }
        npcsFile.writeText(parsed.toString(2))
        loadNpcs()
    }

    @Synchronized
    fun loadNpcsRawJson(): String =
        if (npcsFile.exists()) npcsFile.readText() else "{}"

    companion object {
        fun encodeItem(item: Item): JSONObject = JSONObject().apply {
            put("id", item.id)
            put("templateId", item.templateId)
            put("name", item.name)
            put("slot", item.slot.wire)
            put("rarity", item.rarity.wire)
            put("plus", item.plus)
            put("attack", item.attack)
            put("defense", item.defense)
            put("magic", item.magic)
            put("speedBonus", item.speedBonus)
            put("hpBonus", item.hpBonus)
            put("wingsTier", item.wingsTier)
            item.jewelType?.let { put("jewelType", it.wire) }
            item.potionType?.let { put("potionType", it.wire) }
            put("classReq", item.classReq)
            put("color", MuCatalog.formatColorHex(item.colorHex))
            put("valueZen", item.valueZen)
            put("quantity", item.quantity)
        }

        fun decodeItem(obj: JSONObject): Item = Item(
            id = obj.optString("id", MuCatalog.newUid("it")),
            templateId = obj.optString("templateId", "item"),
            name = obj.optString("name", "Ítem"),
            slot = ItemSlot.fromWire(obj.optString("slot")),
            rarity = ItemRarity.fromWire(obj.optString("rarity")),
            plus = obj.optInt("plus", 0),
            attack = obj.optInt("attack", 0),
            defense = obj.optInt("defense", 0),
            magic = obj.optInt("magic", 0),
            speedBonus = obj.optInt("speedBonus", 0),
            hpBonus = obj.optInt("hpBonus", 0),
            wingsTier = obj.optInt("wingsTier", 0),
            jewelType = JewelType.fromWire(obj.optString("jewelType").takeIf { it.isNotBlank() }),
            potionType = PotionType.fromWire(obj.optString("potionType").takeIf { it.isNotBlank() }),
            classReq = obj.optString("classReq", "all"),
            colorHex = MuCatalog.parseColorHex(obj.optString("color"), 0xFFE2E8F0),
            valueZen = obj.optInt("valueZen", 100),
            quantity = obj.optInt("quantity", 1).coerceAtLeast(1)
        )

        fun encodeHero(hero: HeroSave): JSONObject = JSONObject().apply {
            put("id", hero.id)
            put("accountId", hero.accountId)
            put("name", hero.name)
            put("classId", hero.classId.wire)
            put("evolutionTier", hero.evolutionTier)
            put("level", hero.level)
            put("resets", hero.resets)
            put("exp", hero.exp)
            put("expToNext", hero.expToNext)
            put("zen", hero.zen)
            put("hp", hero.hp)
            put("maxHp", hero.maxHp)
            put("mp", hero.mp)
            put("maxMp", hero.maxMp)
            put("statPoints", hero.statPoints)
            put("stats", JSONObject().apply {
                put("str", hero.stats.str)
                put("agi", hero.stats.agi)
                put("vit", hero.stats.vit)
                put("ene", hero.stats.ene)
            })
            put("mapId", hero.mapId.wire)
            put("x", hero.x.toDouble())
            put("y", hero.y.toDouble())
            put("equipment", JSONObject().apply {
                hero.equipment.weapon?.let { put("weapon", encodeItem(it)) }
                hero.equipment.offhand?.let { put("offhand", encodeItem(it)) }
                hero.equipment.armor?.let { put("armor", encodeItem(it)) }
                hero.equipment.helm?.let { put("helm", encodeItem(it)) }
                hero.equipment.wings?.let { put("wings", encodeItem(it)) }
                hero.equipment.pendant?.let { put("pendant", encodeItem(it)) }
                hero.equipment.pet?.let { put("pet", encodeItem(it)) }
            })
            put("inventory", JSONArray().apply { hero.inventory.forEach { put(encodeItem(it)) } })
            put("kills", hero.kills)
            put("updatedAt", hero.updatedAt)
        }

        fun decodeHero(o: JSONObject): HeroSave {
            val statsObj = o.optJSONObject("stats") ?: JSONObject()
            val eqObj = o.optJSONObject("equipment") ?: JSONObject()
            val invArr = o.optJSONArray("inventory") ?: JSONArray()
            return HeroSave(
                id = o.optString("id", MuCatalog.newUid("hero")),
                accountId = o.optString("accountId"),
                name = o.optString("name", "Guerrero"),
                classId = ClassId.fromWire(o.optString("classId")),
                evolutionTier = o.optInt("evolutionTier", 0),
                level = o.optInt("level", 1),
                resets = o.optInt("resets", 0),
                exp = o.optInt("exp", 0),
                expToNext = o.optInt("expToNext", 120),
                zen = o.optInt("zen", 5000),
                hp = o.optInt("hp", 200),
                maxHp = o.optInt("maxHp", 200),
                mp = o.optInt("mp", 100),
                maxMp = o.optInt("maxMp", 100),
                statPoints = o.optInt("statPoints", 10),
                stats = HeroStats(
                    str = statsObj.optInt("str", 20),
                    agi = statsObj.optInt("agi", 20),
                    vit = statsObj.optInt("vit", 20),
                    ene = statsObj.optInt("ene", 15)
                ),
                mapId = MapId.fromWire(o.optString("mapId")),
                x = o.optDouble("x", MuWorldConstants.SAFE_CENTER.toDouble()).toFloat(),
                y = o.optDouble("y", MuWorldConstants.SAFE_CENTER.toDouble()).toFloat(),
                equipment = EquipSlots(
                    weapon = eqObj.optJSONObject("weapon")?.let(::decodeItem),
                    offhand = eqObj.optJSONObject("offhand")?.let(::decodeItem),
                    armor = eqObj.optJSONObject("armor")?.let(::decodeItem),
                    helm = eqObj.optJSONObject("helm")?.let(::decodeItem),
                    wings = eqObj.optJSONObject("wings")?.let(::decodeItem),
                    pendant = eqObj.optJSONObject("pendant")?.let(::decodeItem),
                    pet = eqObj.optJSONObject("pet")?.let(::decodeItem)
                ),
                inventory = (0 until invArr.length()).mapNotNull { invArr.optJSONObject(it)?.let(::decodeItem) },
                kills = o.optInt("kills", 0),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        fun encodeMod(mod: ModPackage): JSONObject = JSONObject().apply {
            put("id", mod.id)
            put("name", mod.name)
            put("author", mod.author)
            put("version", mod.version)
            put("description", mod.description)
            put("enabled", mod.enabled)
            put("builtIn", mod.builtIn)
            put("expMultiplier", mod.expMultiplier)
            put("zenMultiplier", mod.zenMultiplier)
            put("dropRateMultiplier", mod.dropRateMultiplier)
            put("chaosSuccessBonus", mod.chaosSuccessBonus)
            put("resetLevel", mod.resetLevel)
            put("customItems", JSONArray().apply {
                mod.customItems.forEach { ci ->
                    put(JSONObject().apply {
                        put("templateId", ci.templateId)
                        put("name", ci.name)
                        put("slot", ci.slot.wire)
                        put("rarity", ci.rarity.wire)
                        put("basePlus", ci.basePlus)
                        put("attack", ci.attack)
                        put("defense", ci.defense)
                        put("magic", ci.magic)
                        put("hpBonus", ci.hpBonus)
                        put("speedBonus", ci.speedBonus)
                        put("wingsTier", ci.wingsTier)
                        put("classReq", ci.classReq)
                        put("color", MuCatalog.formatColorHex(ci.colorHex))
                        put("valueZen", ci.valueZen)
                    })
                }
            })
            put("customMonsters", JSONArray().apply {
                mod.customMonsters.forEach { cm ->
                    put(JSONObject().apply {
                        put("id", cm.id)
                        put("name", cm.name)
                        put("mapId", cm.mapId.wire)
                        put("level", cm.level)
                        put("maxHp", cm.maxHp)
                        put("attack", cm.attack)
                        put("defense", cm.defense)
                        put("expReward", cm.expReward)
                        put("zenReward", cm.zenReward)
                        put("color", MuCatalog.formatColorHex(cm.colorHex))
                        put("isBoss", cm.isBoss)
                    })
                }
            })
        }

        fun decodeMod(o: JSONObject, forceImported: Boolean = false): ModPackage {
            val itemsArr = o.optJSONArray("customItems") ?: JSONArray()
            val monstersArr = o.optJSONArray("customMonsters") ?: JSONArray()
            val customItems = (0 until itemsArr.length()).mapNotNull { i ->
                val ci = itemsArr.optJSONObject(i) ?: return@mapNotNull null
                ModItemDef(
                    templateId = ci.optString("templateId", MuCatalog.newUid("moditem")),
                    name = ci.optString("name", "Mod Item"),
                    slot = ItemSlot.fromWire(ci.optString("slot")),
                    rarity = ItemRarity.fromWire(ci.optString("rarity")),
                    basePlus = ci.optInt("basePlus", 0),
                    attack = ci.optInt("attack", 0),
                    defense = ci.optInt("defense", 0),
                    magic = ci.optInt("magic", 0),
                    hpBonus = ci.optInt("hpBonus", 0),
                    speedBonus = ci.optInt("speedBonus", 0),
                    wingsTier = ci.optInt("wingsTier", 0),
                    classReq = ci.optString("classReq", "all"),
                    colorHex = MuCatalog.parseColorHex(ci.optString("color"), 0xFFF43F5E),
                    valueZen = ci.optInt("valueZen", 25000)
                )
            }
            val customMonsters = (0 until monstersArr.length()).mapNotNull { i ->
                val cm = monstersArr.optJSONObject(i) ?: return@mapNotNull null
                ModMonsterDef(
                    id = cm.optString("id", MuCatalog.newUid("modmob")),
                    name = cm.optString("name", "Mod Boss"),
                    mapId = MapId.fromWire(cm.optString("mapId")),
                    level = cm.optInt("level", 25),
                    maxHp = cm.optInt("maxHp", 1500),
                    attack = cm.optInt("attack", 55),
                    defense = cm.optInt("defense", 18),
                    expReward = cm.optInt("expReward", 800),
                    zenReward = cm.optInt("zenReward", 4000),
                    colorHex = MuCatalog.parseColorHex(cm.optString("color"), 0xFFF43F5E),
                    isBoss = cm.optBoolean("isBoss", true)
                )
            }
            return ModPackage(
                id = if (forceImported) MuCatalog.newUid("import") else o.optString("id", MuCatalog.newUid("mod")),
                name = o.optString("name", "Custom Mod"),
                author = o.optString("author", "MU Offline"),
                version = o.optString("version", "1.0.0"),
                description = o.optString("description", "Mod importado"),
                enabled = if (forceImported) true else o.optBoolean("enabled", true),
                builtIn = if (forceImported) false else o.optBoolean("builtIn", false),
                expMultiplier = o.optDouble("expMultiplier", 1.0),
                zenMultiplier = o.optDouble("zenMultiplier", 1.0),
                dropRateMultiplier = o.optDouble("dropRateMultiplier", 1.0),
                chaosSuccessBonus = o.optInt("chaosSuccessBonus", 0),
                resetLevel = o.optInt("resetLevel", 400),
                customItems = customItems,
                customMonsters = customMonsters
            )
        }
    }
}
