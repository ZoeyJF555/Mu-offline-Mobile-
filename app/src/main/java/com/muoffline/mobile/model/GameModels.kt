package com.muoffline.mobile.model

import androidx.annotation.DrawableRes

enum class ClassId(val wire: String) {
    DK("dk"),
    DW("dw"),
    ELF("elf");

    companion object {
        fun fromWire(value: String?): ClassId =
            entries.firstOrNull { it.wire == value?.lowercase() } ?: DK
    }
}

enum class MapId(val wire: String) {
    LORENCIA("lorencia"),
    DUNGEON("dungeon"),
    DEVIAS("devias"),
    NORIA("noria"),
    CUSTOM("custom");

    companion object {
        fun fromWire(value: String?): MapId =
            entries.firstOrNull { it.wire == value?.lowercase() } ?: LORENCIA
    }
}

enum class ItemSlot(val wire: String, val labelEs: String) {
    WEAPON("weapon", "Arma"),
    OFFHAND("offhand", "Escudo"),
    ARMOR("armor", "Armadura"),
    HELM("helm", "Casco"),
    WINGS("wings", "Alas"),
    PENDANT("pendant", "Colgante"),
    JEWEL("jewel", "Joya"),
    POTION("potion", "Poción");

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
    LIFE("life");

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
    val jewelType: JewelType? = null,
    val potionType: PotionType? = null,
    val classReq: String = "all",
    val colorHex: Long = 0xFFE2E8F0,
    val valueZen: Int = 100,
    val quantity: Int = 1
)

data class EquipSlots(
    val weapon: Item? = null,
    val offhand: Item? = null,
    val armor: Item? = null,
    val helm: Item? = null,
    val wings: Item? = null,
    val pendant: Item? = null
) {
    fun allEquipped(): List<Item> = listOfNotNull(weapon, offhand, armor, helm, wings, pendant)

    fun getSlot(slot: ItemSlot): Item? = when (slot) {
        ItemSlot.WEAPON -> weapon
        ItemSlot.OFFHAND -> offhand
        ItemSlot.ARMOR -> armor
        ItemSlot.HELM -> helm
        ItemSlot.WINGS -> wings
        ItemSlot.PENDANT -> pendant
        else -> null
    }

    fun withSlot(slot: ItemSlot, item: Item?): EquipSlots = when (slot) {
        ItemSlot.WEAPON -> copy(weapon = item)
        ItemSlot.OFFHAND -> copy(offhand = item)
        ItemSlot.ARMOR -> copy(armor = item)
        ItemSlot.HELM -> copy(helm = item)
        ItemSlot.WINGS -> copy(wings = item)
        ItemSlot.PENDANT -> copy(pendant = item)
        else -> this
    }

    fun mapItems(transform: (Item) -> Item): EquipSlots = copy(
        weapon = weapon?.let(transform),
        offhand = offhand?.let(transform),
        armor = armor?.let(transform),
        helm = helm?.let(transform),
        wings = wings?.let(transform),
        pendant = pendant?.let(transform)
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
    val title: String,
    val role: String,
    @DrawableRes val portraitRes: Int,
    val accentColor: Long,
    val baseStats: HeroStats,
    val baseHp: Int,
    val baseMp: Int,
    val attackRange: Float,
    val starterWeapon: Item,
    val starterArmor: Item
)

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

data class LorenciaNpcDef(
    val id: String,
    val name: String,
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
