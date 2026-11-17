package com.muoffline.mobile.model

import androidx.annotation.DrawableRes

enum class ClassId(val wire: String) {
    DK("dk"),
    DW("dw"),
    ELF("elf"),
    MG("mg"),
    DL("dl");

    companion object {
        fun fromWire(value: String?): ClassId =
            entries.firstOrNull { it.wire == value?.lowercase() } ?: DK
    }
}

enum class MapId(val wire: String) {
    LORENCIA("lorencia"),
    NORIA("noria"),
    DEVIAS("devias"),
    DUNGEON1("dungeon1"),
    DUNGEON2("dungeon2"),
    DUNGEON3("dungeon3"),
    ATLANS("atlans"),
    LOSTTOWER("losttower"),
    KALIMA("kalima"),
    TARKAN("tarkan"),
    ICARUS("icarus"),
    KANTURU("kanturu"),
    AIDA("aida"),
    CRYWOLF("crywolf"),
    STADIUM("stadium"),
    CUSTOM("custom");

    companion object {
        fun fromWire(value: String?): MapId = when (value?.lowercase()) {
            "dungeon" -> DUNGEON1
            else -> entries.firstOrNull { it.wire == value?.lowercase() } ?: LORENCIA
        }
    }
}

enum class ItemSlot(val wire: String, val labelEs: String) {
    WEAPON("weapon", "Arma"),
    OFFHAND("offhand", "Escudo"),
    ARMOR("armor", "Pechera"),
    HELM("helm", "Casco"),
    GLOVES("gloves", "Guantes"),
    BOOTS("boots", "Botas"),
    WINGS("wings", "Alas"),
    PENDANT("pendant", "Collar"),
    RING("ring", "Anillo"),
    PET("pet", "Mascota"),
    JEWEL("jewel", "Joya"),
    POTION("potion", "Poción"),
    TALISMAN("talisman", "Talismán");

    companion object {
        fun fromWire(value: String?): ItemSlot =
            entries.firstOrNull { it.wire == value?.lowercase() } ?: WEAPON
    }
}

enum class ItemRarity(val wire: String) {
    NORMAL("normal"),
    EXCELLENT("excellent"),
    ANCIENT("ancient"),
    MYTHIC("mythic");

    companion object {
        fun fromWire(value: String?): ItemRarity =
            entries.firstOrNull { it.wire == value?.lowercase() } ?: NORMAL
    }
}

enum class JewelType(val wire: String) {
    BLESS("bless"),
    SOUL("soul"),
    CHAOS("chaos"),
    LIFE("life"),
    CREATION("creation");

    companion object {
        fun fromWire(value: String?): JewelType? =
            entries.firstOrNull { it.wire == value?.lowercase() }
    }
}

enum class PotionType(val wire: String) {
    HP("hp"),
    MP("mp");

    companion object {
        fun fromWire(value: String?): PotionType? =
            entries.firstOrNull { it.wire == value?.lowercase() }
    }
}

data class AccountRecord(
    val accountId: String,
    val passwordHash: String,
    val createdAt: Long
)

data class Item(
    val id: String,
    val templateId: String,
    val name: String,
    val slot: ItemSlot,
    val rarity: ItemRarity,
    val plus: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val magic: Int = 0,
    val speedBonus: Int = 0,
    val hpBonus: Int = 0,
    val wingsTier: Int = 0,
    val jewelType: JewelType? = null,
    val potionType: PotionType? = null,
    val classReq: String = "all",
    val colorHex: Long = 0xFFE2E8F0,
    val valueZen: Int = 100,
    val quantity: Int = 1,
    val durability: Int = 100
)

enum class EquipSlotKey(val labelEs: String, val itemSlot: ItemSlot) {
    HELM("Casco", ItemSlot.HELM),
    WINGS("Alas", ItemSlot.WINGS),
    WEAPON("Arma", ItemSlot.WEAPON),
    ARMOR("Pechera", ItemSlot.ARMOR),
    OFFHAND("Escudo", ItemSlot.OFFHAND),
    GLOVES("Guantes", ItemSlot.GLOVES),
    BOOTS("Botas", ItemSlot.BOOTS),
    PENDANT("Collar", ItemSlot.PENDANT),
    RING1("Anillo 1", ItemSlot.RING),
    RING2("Anillo 2", ItemSlot.RING),
    PET("Mascota", ItemSlot.PET)
}

data class EquipSlots(
    val weapon: Item? = null,
    val offhand: Item? = null,
    val armor: Item? = null,
    val helm: Item? = null,
    val gloves: Item? = null,
    val boots: Item? = null,
    val wings: Item? = null,
    val pendant: Item? = null,
    val ring1: Item? = null,
    val ring2: Item? = null,
    val pet: Item? = null
) {
    fun allEquipped(): List<Item> = listOfNotNull(weapon, offhand, armor, helm, gloves, boots, wings, pendant, ring1, ring2, pet)

    fun getSlot(slot: ItemSlot): Item? = when (slot) {
        ItemSlot.WEAPON -> weapon
        ItemSlot.OFFHAND -> offhand
        ItemSlot.ARMOR -> armor
        ItemSlot.HELM -> helm
        ItemSlot.GLOVES -> gloves
        ItemSlot.BOOTS -> boots
        ItemSlot.WINGS -> wings
        ItemSlot.PENDANT -> pendant
        ItemSlot.RING -> ring1 ?: ring2
        ItemSlot.PET -> pet
        else -> null
    }

    fun getSlotKey(key: EquipSlotKey): Item? = when (key) {
        EquipSlotKey.WEAPON -> weapon
        EquipSlotKey.OFFHAND -> offhand
        EquipSlotKey.ARMOR -> armor
        EquipSlotKey.HELM -> helm
        EquipSlotKey.GLOVES -> gloves
        EquipSlotKey.BOOTS -> boots
        EquipSlotKey.WINGS -> wings
        EquipSlotKey.PENDANT -> pendant
        EquipSlotKey.RING1 -> ring1
        EquipSlotKey.RING2 -> ring2
        EquipSlotKey.PET -> pet
    }

    fun withSlotKey(key: EquipSlotKey, item: Item?): EquipSlots = when (key) {
        EquipSlotKey.WEAPON -> copy(weapon = item)
        EquipSlotKey.OFFHAND -> copy(offhand = item)
        EquipSlotKey.ARMOR -> copy(armor = item)
        EquipSlotKey.HELM -> copy(helm = item)
        EquipSlotKey.GLOVES -> copy(gloves = item)
        EquipSlotKey.BOOTS -> copy(boots = item)
        EquipSlotKey.WINGS -> copy(wings = item)
        EquipSlotKey.PENDANT -> copy(pendant = item)
        EquipSlotKey.RING1 -> copy(ring1 = item)
        EquipSlotKey.RING2 -> copy(ring2 = item)
        EquipSlotKey.PET -> copy(pet = item)
    }

    fun withSlot(slot: ItemSlot, item: Item?): EquipSlots = when (slot) {
        ItemSlot.WEAPON -> copy(weapon = item)
        ItemSlot.OFFHAND -> copy(offhand = item)
        ItemSlot.ARMOR -> copy(armor = item)
        ItemSlot.HELM -> copy(helm = item)
        ItemSlot.GLOVES -> copy(gloves = item)
        ItemSlot.BOOTS -> copy(boots = item)
        ItemSlot.WINGS -> copy(wings = item)
        ItemSlot.PENDANT -> copy(pendant = item)
        ItemSlot.RING -> if (ring1 == null || item == null) copy(ring1 = item) else copy(ring2 = item)
        ItemSlot.PET -> copy(pet = item)
        else -> this
    }

    fun mapItems(transform: (Item) -> Item): EquipSlots = copy(
        weapon = weapon?.let(transform),
        offhand = offhand?.let(transform),
        armor = armor?.let(transform),
        helm = helm?.let(transform),
        gloves = gloves?.let(transform),
        boots = boots?.let(transform),
        wings = wings?.let(transform),
        pendant = pendant?.let(transform),
        ring1 = ring1?.let(transform),
        ring2 = ring2?.let(transform),
        pet = pet?.let(transform)
    )
}

data class HeroStats(
    val str: Int,
    val agi: Int,
    val vit: Int,
    val ene: Int
)

enum class StatKind { STR, AGI, VIT, ENE }

data class DerivedHeroStats(
    val attackPower: Int,
    val defense: Int,
    val maxHp: Int,
    val maxMp: Int,
    val critChance: Float,
    val moveSpeed: Float
)

data class HeroSave(
    val id: String,
    val accountId: String,
    val name: String,
    val classId: ClassId,
    val evolutionTier: Int = 0,
    val level: Int,
    val resets: Int,
    val exp: Int,
    val expToNext: Int,
    val zen: Int,
    val hp: Int,
    val maxHp: Int,
    val mp: Int,
    val maxMp: Int,
    val statPoints: Int,
    val stats: HeroStats,
    val mapId: MapId,
    val x: Float,
    val y: Float,
    val equipment: EquipSlots,
    val inventory: List<Item>,
    val kills: Int,
    val updatedAt: Long
)

data class ClassMeta(
    val id: ClassId,
    val name: String,
    val evo150Title: String,
    val evo400Title: String,
    val role: String,
    @DrawableRes val portraitRes: Int,
    val accentColor: Long,
    val pointsPerLevel: Int,
    val unlockLevel: Int,
    val canEquipHelm: Boolean,
    val baseStats: HeroStats,
    val baseHp: Int,
    val baseMp: Int,
    val attackRange: Float,
    val starterWeapon: Item,
    val starterArmor: Item
) {
    fun titleForTier(tier: Int): String = when {
        tier >= 2 -> evo400Title
        tier == 1 -> evo150Title
        else -> name
    }
}

data class SkillDef(
    val id: String,
    val name: String,
    val classId: ClassId,
    val mpCost: Int,
    val cooldownMs: Long,
    val damageMultiplier: Float,
    val range: Float,
    val aoeRadius: Float,
    val colorHex: Long,
    val description: String
)

data class MapMeta(
    val id: MapId,
    val name: String,
    val minLevel: Int,
    val warpLevel: Int,
    val expRate: Double,
    val dropZenRate: Double,
    val dropItemRate: Double,
    val groundPrimary: Long,
    val groundSecondary: Long,
    val safeZoneColor: Long,
    val accentColor: Long,
    val subtitle: String
)

data class MonsterTemplate(
    val id: String,
    val name: String,
    val mapId: MapId,
    val level: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val expReward: Int,
    val zenReward: Int,
    val speed: Float,
    val radius: Float,
    val colorHex: Long,
    val isBoss: Boolean = false,
    val guaranteedJewel: Boolean = false
)

data class LiveMonster(
    val uid: String,
    val templateId: String,
    val name: String,
    val mapId: MapId,
    val level: Int,
    val hp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val expReward: Int,
    val zenReward: Int,
    val speed: Float,
    val radius: Float,
    val colorHex: Long,
    val isBoss: Boolean,
    val guaranteedJewel: Boolean,
    val x: Float,
    val y: Float,
    val homeX: Float,
    val homeY: Float,
    val respawnAt: Long = 0L,
    val hitFlashUntil: Long = 0L,
    val attackCooldownUntil: Long = 0L
)

data class GroundDrop(
    val uid: String,
    val x: Float,
    val y: Float,
    val zen: Int? = null,
    val item: Item? = null,
    val createdAt: Long
)

data class FloatingText(
    val id: String,
    val x: Float,
    val y: Float,
    val text: String,
    val colorHex: Long,
    val isCrit: Boolean = false,
    val createdAt: Long
)

data class SkillVfx(
    val id: String,
    val skillId: String,
    val x: Float,
    val y: Float,
    val targetX: Float,
    val targetY: Float,
    val colorHex: Long,
    val radius: Float,
    val createdAt: Long,
    val durationMs: Long = 380L
)

data class MapNpcDef(
    val id: String,
    val mapId: MapId,
    val name: String,
    val role: String,
    val subtitle: String,
    val wx: Float,
    val wy: Float
)

data class ModItemDef(
    val templateId: String,
    val name: String,
    val slot: ItemSlot,
    val rarity: ItemRarity,
    val basePlus: Int,
    val attack: Int = 0,
    val defense: Int = 0,
    val magic: Int = 0,
    val hpBonus: Int = 0,
    val speedBonus: Int = 0,
    val wingsTier: Int = 0,
    val classReq: String = "all",
    val colorHex: Long = 0xFFF43F5E,
    val valueZen: Int = 25000
)

data class ModMonsterDef(
    val id: String,
    val name: String,
    val mapId: MapId,
    val level: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val expReward: Int,
    val zenReward: Int,
    val colorHex: Long = 0xFFF43F5E,
    val isBoss: Boolean = true
)

data class ModPackage(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val enabled: Boolean,
    val builtIn: Boolean = false,
    val expMultiplier: Double,
    val zenMultiplier: Double,
    val dropRateMultiplier: Double,
    val chaosSuccessBonus: Int,
    val resetLevel: Int,
    val customItems: List<ModItemDef> = emptyList(),
    val customMonsters: List<ModMonsterDef> = emptyList()
)

data class ActiveModRates(
    val expMul: Double = 1.0,
    val zenMul: Double = 1.0,
    val dropMul: Double = 1.0,
    val chaosBonus: Int = 0,
    val resetLevel: Int = 400
)

data class HudButtonOffset(
    val dxDp: Float = 0f,
    val dyDp: Float = 0f
)

data class MobileHudSettings(
    val sfxVolume: Float = 0.8f,
    val musicVolume: Float = 0.45f,
    val brightness: Float = 1.0f,
    val hdGraphics: Boolean = true,
    val animationsEnabled: Boolean = true,
    val hudScale: Float = 1.0f,
    val hudBottomPaddingDp: Float = 6f,
    val manifestUrl: String = "",
    val buttonOffsets: Map<String, HudButtonOffset> = emptyMap()
)

data class ManifestInfo(
    val version: String = "6.3.1",
    val notes: String = "Configuración local inicial",
    val updatedAt: String = "2026-11-16"
)

data class LanPlayer(
    val id: String,
    val name: String,
    val classId: ClassId,
    val level: Int,
    val resets: Int,
    val mapId: MapId,
    val x: Float,
    val y: Float,
    val dir: Float,
    val wingsTier: Int,
    val weaponPlus: Int,
    val hp: Int,
    val maxHp: Int
)

data class LanBossState(
    val id: String,
    val name: String,
    val mapId: MapId,
    val hp: Int,
    val maxHp: Int,
    val x: Float,
    val y: Float,
    val respawnAt: Long
)

data class LanEventState(
    val id: String,
    val name: String,
    val mapId: MapId,
    val active: Boolean,
    val endsAt: Long,
    val startedBy: String,
    val rewardZen: Int
)

data class TradeSession(
    val tradeId: String,
    val partnerId: String,
    val partnerName: String,
    val myItems: List<Item> = emptyList(),
    val myZen: Int = 0,
    val myConfirmed: Boolean = false,
    val partnerItems: List<Item> = emptyList(),
    val partnerZen: Int = 0,
    val partnerConfirmed: Boolean = false
)

data class IncomingTradeRequest(
    val tradeId: String,
    val fromPlayerId: String,
    val fromPlayerName: String
)

data class LanChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val classId: ClassId,
    val text: String,
    val timestamp: Long
)

data class DiscoveredLanRoom(
    val serviceName: String,
    val roomCode: String,
    val hostAddress: String,
    val port: Int
)

enum class LanConnectionStatus {
    DISCONNECTED,
    HOSTING,
    CONNECTING,
    CONNECTED,
    ERROR
}
