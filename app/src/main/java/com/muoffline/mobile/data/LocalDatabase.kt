package com.muoffline.mobile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.muoffline.mobile.model.*
import org.json.JSONArray
import org.json.JSONObject

class LocalDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "mu_offline_mobile.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE accounts (
                account_id TEXT PRIMARY KEY,
                password_hash TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE heroes (
                id TEXT PRIMARY KEY,
                account_id TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                payload_json TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE mods (
                id TEXT PRIMARY KEY,
                payload_json TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun getAllAccounts(): List<AccountRecord> {
        val list = mutableListOf<AccountRecord>()
        readableDatabase.rawQuery("SELECT account_id, password_hash, created_at FROM accounts", null).use { c ->
            while (c.moveToNext()) {
                list += AccountRecord(c.getString(0), c.getString(1), c.getLong(2))
            }
        }
        return list
    }

    fun putAccount(record: AccountRecord) {
        val cv = ContentValues().apply {
            put("account_id", record.accountId)
            put("password_hash", record.passwordHash)
            put("created_at", record.createdAt)
        }
        writableDatabase.insertWithOnConflict("accounts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun loadHeroes(accountId: String): List<HeroSave> {
        val list = mutableListOf<HeroSave>()
        readableDatabase.rawQuery(
            "SELECT payload_json FROM heroes WHERE account_id = ? ORDER BY updated_at DESC",
            arrayOf(accountId)
        ).use { c ->
            while (c.moveToNext()) {
                runCatching { decodeHero(JSONObject(c.getString(0))) }.getOrNull()?.let { list += it }
            }
        }
        return list
    }

    fun saveHero(hero: HeroSave) {
        val updated = hero.copy(updatedAt = System.currentTimeMillis())
        val cv = ContentValues().apply {
            put("id", updated.id)
            put("account_id", updated.accountId)
            put("updated_at", updated.updatedAt)
            put("payload_json", encodeHero(updated).toString())
        }
        writableDatabase.insertWithOnConflict("heroes", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteHero(heroId: String) {
        writableDatabase.delete("heroes", "id = ?", arrayOf(heroId))
    }

    fun loadCustomOrStoredMods(): List<ModPackage> {
        val list = mutableListOf<ModPackage>()
        readableDatabase.rawQuery("SELECT payload_json FROM mods", null).use { c ->
            while (c.moveToNext()) {
                runCatching { decodeMod(JSONObject(c.getString(0))) }.getOrNull()?.let { list += it }
            }
        }
        return list
    }

    fun saveMods(mods: List<ModPackage>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("mods", null, null)
            for (mod in mods) {
                val cv = ContentValues().apply {
                    put("id", mod.id)
                    put("payload_json", encodeMod(mod).toString())
                }
                db.insertWithOnConflict("mods", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

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
            item.jewelType?.let { put("jewelType", it.wire) }
            item.potionType?.let { put("potionType", it.wire) }
            put("classReq", item.classReq)
            put("colorHex", MuCatalog.formatColorHex(item.colorHex))
            put("valueZen", item.valueZen)
            put("quantity", item.quantity)
        }

        fun decodeItem(o: JSONObject): Item = Item(
            id = o.optString("id", MuCatalog.newUid("it")),
            templateId = o.optString("templateId", "item"),
            name = o.optString("name", "Ítem"),
            slot = ItemSlot.fromWire(o.optString("slot")),
            rarity = ItemRarity.fromWire(o.optString("rarity")),
            plus = o.optInt("plus", 0),
            attack = o.optInt("attack", 0),
            defense = o.optInt("defense", 0),
            magic = o.optInt("magic", 0),
            speedBonus = o.optInt("speedBonus", 0),
            hpBonus = o.optInt("hpBonus", 0),
            jewelType = if (o.has("jewelType")) JewelType.fromWire(o.optString("jewelType")) else null,
            potionType = if (o.has("potionType")) PotionType.fromWire(o.optString("potionType")) else null,
            classReq = o.optString("classReq", "all"),
            colorHex = MuCatalog.parseColorHex(o.optString("colorHex"), 0xFFE2E8F0),
            valueZen = o.optInt("valueZen", 100),
            quantity = o.optInt("quantity", 1)
        )

        fun encodeHero(h: HeroSave): JSONObject = JSONObject().apply {
            put("id", h.id)
            put("accountId", h.accountId)
            put("name", h.name)
            put("classId", h.classId.wire)
            put("level", h.level)
            put("resets", h.resets)
            put("exp", h.exp)
            put("expToNext", h.expToNext)
            put("zen", h.zen)
            put("hp", h.hp)
            put("maxHp", h.maxHp)
            put("mp", h.mp)
            put("maxMp", h.maxMp)
            put("statPoints", h.statPoints)
            put("stats", JSONObject().apply {
                put("str", h.stats.str)
                put("agi", h.stats.agi)
                put("vit", h.stats.vit)
                put("ene", h.stats.ene)
            })
            put("mapId", h.mapId.wire)
            put("x", h.x.toDouble())
            put("y", h.y.toDouble())
            put("equipment", JSONObject().apply {
                h.equipment.weapon?.let { put("weapon", encodeItem(it)) }
                h.equipment.offhand?.let { put("offhand", encodeItem(it)) }
                h.equipment.armor?.let { put("armor", encodeItem(it)) }
                h.equipment.helm?.let { put("helm", encodeItem(it)) }
                h.equipment.wings?.let { put("wings", encodeItem(it)) }
                h.equipment.pendant?.let { put("pendant", encodeItem(it)) }
            })
            put("inventory", JSONArray().apply { h.inventory.forEach { put(encodeItem(it)) } })
            put("kills", h.kills)
            put("updatedAt", h.updatedAt)
        }

        fun decodeHero(o: JSONObject): HeroSave {
            val st = o.optJSONObject("stats") ?: JSONObject()
            val eq = o.optJSONObject("equipment") ?: JSONObject()
            val invArr = o.optJSONArray("inventory") ?: JSONArray()
            val inv = (0 until invArr.length()).mapNotNull { i ->
                invArr.optJSONObject(i)?.let { decodeItem(it) }
            }
            return HeroSave(
                id = o.getString("id"),
                accountId = o.optString("accountId", "guest"),
                name = o.optString("name", "Guerrero"),
                classId = ClassId.fromWire(o.optString("classId")),
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
                    str = st.optInt("str", 20),
                    agi = st.optInt("agi", 20),
                    vit = st.optInt("vit", 20),
                    ene = st.optInt("ene", 15)
                ),
                mapId = MapId.fromWire(o.optString("mapId")),
                x = o.optDouble("x", MuWorldConstants.SAFE_CENTER.toDouble()).toFloat(),
                y = o.optDouble("y", MuWorldConstants.SAFE_CENTER.toDouble()).toFloat(),
                equipment = EquipSlots(
                    weapon = eq.optJSONObject("weapon")?.let(::decodeItem),
                    offhand = eq.optJSONObject("offhand")?.let(::decodeItem),
                    armor = eq.optJSONObject("armor")?.let(::decodeItem),
                    helm = eq.optJSONObject("helm")?.let(::decodeItem),
                    wings = eq.optJSONObject("wings")?.let(::decodeItem),
                    pendant = eq.optJSONObject("pendant")?.let(::decodeItem)
                ),
                inventory = inv,
                kills = o.optInt("kills", 0),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        fun encodeMod(m: ModPackage): JSONObject = JSONObject().apply {
            put("id", m.id)
            put("name", m.name)
            put("author", m.author)
            put("version", m.version)
            put("description", m.description)
            put("enabled", m.enabled)
            put("builtIn", m.builtIn)
            put("expMultiplier", m.expMultiplier)
            put("zenMultiplier", m.zenMultiplier)
            put("dropRateMultiplier", m.dropRateMultiplier)
            put("chaosSuccessBonus", m.chaosSuccessBonus)
            put("resetLevel", m.resetLevel)
            put("customItems", JSONArray().apply {
                m.customItems.forEach { ci ->
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
                        put("classReq", ci.classReq)
                        put("colorHex", MuCatalog.formatColorHex(ci.colorHex))
                        put("valueZen", ci.valueZen)
                    })
                }
            })
            put("customMonsters", JSONArray().apply {
                m.customMonsters.forEach { cm ->
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
            val items = (0 until itemsArr.length()).mapNotNull { idx ->
                itemsArr.optJSONObject(idx)?.let { ci ->
                    ModItemDef(
                        templateId = ci.optString("templateId", "mod-item-$idx"),
                        name = ci.optString("name", "Ítem Mod"),
                        slot = ItemSlot.fromWire(ci.optString("slot")),
                        rarity = ItemRarity.fromWire(ci.optString("rarity")),
                        basePlus = ci.optInt("basePlus", 0),
                        attack = ci.optInt("attack", 0),
                        defense = ci.optInt("defense", 0),
                        magic = ci.optInt("magic", 0),
                        hpBonus = ci.optInt("hpBonus", 0),
                        speedBonus = ci.optInt("speedBonus", 0),
                        classReq = ci.optString("classReq", "all"),
                        colorHex = MuCatalog.parseColorHex(ci.optString("colorHex"), 0xFFF43F5E),
                        valueZen = ci.optInt("valueZen", 25000)
                    )
                }
            }

            val mobsArr = o.optJSONArray("customMonsters") ?: JSONArray()
            val mobs = (0 until mobsArr.length()).mapNotNull { idx ->
                mobsArr.optJSONObject(idx)?.let { cm ->
                    ModMonsterDef(
                        id = cm.optString("id", "mod-mob-$idx"),
                        name = cm.optString("name", "Jefe Mod"),
                        mapId = MapId.fromWire(cm.optString("mapId")),
                        level = cm.optInt("level", 45),
                        maxHp = cm.optInt("maxHp", 3200),
                        attack = cm.optInt("attack", 75),
                        defense = cm.optInt("defense", 25),
                        expReward = cm.optInt("expReward", 2200),
                        zenReward = cm.optInt("zenReward", 8500),
                        colorHex = MuCatalog.parseColorHex(cm.optString("color"), 0xFFF43F5E),
                        isBoss = cm.optBoolean("isBoss", true)
                    )
                }
            }

            return ModPackage(
                id = if (forceImported) "imported-${System.currentTimeMillis()}" else o.optString("id", "mod-${System.currentTimeMillis()}"),
                name = o.getString("name"),
                author = o.optString("author", if (forceImported) "Importado" else "Jugador Local"),
                version = o.optString("version", "1.0.0"),
                description = o.optString("description", "Mod importado localmente."),
                enabled = if (forceImported) true else o.optBoolean("enabled", true),
                builtIn = if (forceImported) false else o.optBoolean("builtIn", false),
                expMultiplier = o.optDouble("expMultiplier", 1.0),
                zenMultiplier = o.optDouble("zenMultiplier", 1.0),
                dropRateMultiplier = o.optDouble("dropRateMultiplier", 1.0),
                chaosSuccessBonus = o.optInt("chaosSuccessBonus", 0),
                resetLevel = o.optInt("resetLevel", 400),
                customItems = items,
                customMonsters = mobs
            )
        }
    }
}
