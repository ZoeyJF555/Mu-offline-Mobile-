package com.muoffline.mobile.model

import com.muoffline.mobile.R
import java.util.UUID
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

object MuWorldConstants {
    const val SAFE_CENTER = 480f
    const val SAFE_RADIUS = 135f
    const val WORLD_SIZE = 1400f
}

object MuCatalog {

    val LORENCIA_NPCS: List<LorenciaNpcDef> = listOf(
        LorenciaNpcDef("smith", "Hanzo, Blacksmith", "Tienda Armas", 425f, 438f),
        LorenciaNpcDef("wizard", "Pasi Wizard", "Tienda Magia", 536f, 438f),
        LorenciaNpcDef("barmaid", "Liaman Barmaid", "Pociones", 425f, 526f),
        LorenciaNpcDef("chaos", "Chaos Goblin", "Chaos Machine", 536f, 526f)
    )

    val CLASS_CATALOG: Map<ClassId, ClassMeta> = mapOf(
        ClassId.DK to ClassMeta(
            id = ClassId.DK,
            name = "Dark Knight",
            title = "Blade Knight",
            role = "Cuerpo a cuerpo · Alta resistencia",
            portraitRes = R.drawable.img_dark_knight,
            accentColor = 0xFFEF4444,
            baseStats = HeroStats(str = 28, agi = 20, vit = 25, ene = 10),
            baseHp = 220,
            baseMp = 90,
            attackRange = 68f,
            starterWeapon = Item(
                id = "starter-kris",
                templateId = "short-sword",
                name = "Kris Blade",
                slot = ItemSlot.WEAPON,
                rarity = ItemRarity.NORMAL,
                attack = 18,
                classReq = "dk",
                colorHex = 0xFFEF4444,
                valueZen = 1200
            ),
            starterArmor = Item(
                id = "starter-leather",
                templateId = "leather-armor",
                name = "Leather Armor",
                slot = ItemSlot.ARMOR,
                rarity = ItemRarity.NORMAL,
                defense = 12,
                hpBonus = 30,
                classReq = "dk",
                colorHex = 0xFFB45309,
                valueZen = 1000
            )
        ),
        ClassId.DW to ClassMeta(
            id = ClassId.DW,
            name = "Dark Wizard",
            title = "Soul Master",
            role = "Magia AoE · Alto daño arcano",
            portraitRes = R.drawable.img_dark_wizard,
            accentColor = 0xFF3B82F6,
            baseStats = HeroStats(str = 14, agi = 18, vit = 16, ene = 35),
            baseHp = 155,
            baseMp = 180,
            attackRange = 175f,
            starterWeapon = Item(
                id = "starter-skull-staff",
                templateId = "skull-staff",
                name = "Skull Staff",
                slot = ItemSlot.WEAPON,
                rarity = ItemRarity.NORMAL,
                attack = 10,
                magic = 22,
                classReq = "dw",
                colorHex = 0xFF3B82F6,
                valueZen = 1400
            ),
            starterArmor = Item(
                id = "starter-pad",
                templateId = "pad-armor",
                name = "Pad Armor",
                slot = ItemSlot.ARMOR,
                rarity = ItemRarity.NORMAL,
                defense = 9,
                hpBonus = 15,
                classReq = "dw",
                colorHex = 0xFF2563EB,
                valueZen = 950
            )
        ),
        ClassId.ELF to ClassMeta(
            id = ClassId.ELF,
            name = "Fairy Elf",
            title = "Muse Elf",
            role = "Arco a distancia · Críticos y curación",
            portraitRes = R.drawable.img_fairy_elf,
            accentColor = 0xFF10B981,
            baseStats = HeroStats(str = 18, agi = 32, vit = 18, ene = 15),
            baseHp = 175,
            baseMp = 130,
            attackRange = 195f,
            starterWeapon = Item(
                id = "starter-elven-bow",
                templateId = "elven-bow",
                name = "Elven Bow",
                slot = ItemSlot.WEAPON,
                rarity = ItemRarity.NORMAL,
                attack = 19,
                speedBonus = 10,
                classReq = "elf",
                colorHex = 0xFF10B981,
                valueZen = 1350
            ),
            starterArmor = Item(
                id = "starter-vine",
                templateId = "vine-armor",
                name = "Vine Armor",
                slot = ItemSlot.ARMOR,
                rarity = ItemRarity.NORMAL,
                defense = 10,
                hpBonus = 20,
                classReq = "elf",
                colorHex = 0xFF059669,
                valueZen = 1000
            )
        )
    )

    val SKILLS_BY_CLASS: Map<ClassId, List<SkillDef>> = mapOf(
        ClassId.DK to listOf(
            SkillDef("twisting-slash", "Twisting Slash", ClassId.DK, 12, 1400L, 1.75f, 85f, 95f, 0xFFF97316, "Giro mortal con espada alrededor del guerrero."),
            SkillDef("death-stab", "Death Stab", ClassId.DK, 16, 2200L, 2.35f, 95f, 48f, 0xFFEF4444, "Estocada perforante con ráfaga de viento."),
            SkillDef("rageful-blow", "Rageful Blow", ClassId.DK, 24, 4000L, 2.9f, 90f, 130f, 0xFFF59E0B, "Impacto sísmico que sacude a todos los enemigos cercanos.")
        ),
        ClassId.DW to listOf(
            SkillDef("evil-spirit", "Evil Spirit", ClassId.DW, 18, 1500L, 1.65f, 200f, 145f, 0xFF6366F1, "Invoca espíritus oscuros que azotan toda la zona."),
            SkillDef("meteorite", "Meteorite", ClassId.DW, 20, 2100L, 2.4f, 220f, 72f, 0xFFF59E0B, "Impacto estelar directo sobre el objetivo."),
            SkillDef("ice-storm", "Ice Storm", ClassId.DW, 28, 3800L, 2.6f, 210f, 155f, 0xFF38BDF8, "Tormenta glacial que congela y daña un área amplia.")
        ),
        ClassId.ELF to listOf(
            SkillDef("triple-shot", "Triple Shot", ClassId.ELF, 10, 1200L, 1.7f, 220f, 85f, 0xFF10B981, "Dispara tres flechas simultáneas en abanico."),
            SkillDef("ice-arrow", "Ice Arrow", ClassId.ELF, 18, 2300L, 2.45f, 230f, 50f, 0xFF38BDF8, "Flecha de escarcha con alto daño crítico."),
            SkillDef("heal-aura", "Bless & Heal", ClassId.ELF, 22, 4200L, 0f, 0f, 110f, 0xFF34D399, "Restaura 42% de HP al instante.")
        )
    )

    val MAP_CATALOG: Map<MapId, MapMeta> = linkedMapOf(
        MapId.LORENCIA to MapMeta(MapId.LORENCIA, "Lorencia", 1, 0xFF1A2419, 0xFF222F20, 0xFF2E2B26, 0xFFD97706, "Ciudadela central y praderas iniciales"),
        MapId.DUNGEON to MapMeta(MapId.DUNGEON, "Dungeon", 15, 0xFF15141E, 0xFF1E1B2B, 0xFF252236, 0xFFA855F7, "Catacumbas subterráneas de Gorgon"),
        MapId.DEVIAS to MapMeta(MapId.DEVIAS, "Devias", 35, 0xFF182433, 0xFF203247, 0xFF283A4E, 0xFF38BDF8, "Bastión helado de la Reina del Hielo"),
        MapId.NORIA to MapMeta(MapId.NORIA, "Noria", 60, 0xFF132721, 0xFF18342C, 0xFF253D34, 0xFF10B981, "Bosque ancestral élfico"),
        MapId.CUSTOM to MapMeta(MapId.CUSTOM, "Arena Mod", 1, 0xFF241418, 0xFF301B21, 0xFF382228, 0xFFF43F5E, "Zona de desafío personalizada por Mods")
    )

    val BASE_MONSTERS: List<MonsterTemplate> = listOf(
        MonsterTemplate("budge-dragon", "Budge Dragon", MapId.LORENCIA, 4, 90, 10, 3, 38, 160, 55f, 16f, 0xFF84CC16),
        MonsterTemplate("bull-fighter", "Bull Fighter", MapId.LORENCIA, 8, 170, 16, 6, 68, 260, 62f, 18f, 0xFFDC2626),
        MonsterTemplate("hound", "Hound", MapId.LORENCIA, 11, 220, 20, 8, 95, 340, 74f, 17f, 0xFFF97316),
        MonsterTemplate("elite-skeleton", "Elite Skeleton", MapId.LORENCIA, 16, 320, 27, 11, 145, 520, 65f, 18f, 0xFFE2E8F0),
        MonsterTemplate("red-dragon", "Red Dragon", MapId.LORENCIA, 24, 1200, 48, 18, 620, 2500, 68f, 28f, 0xFFEF4444, isBoss = true, guaranteedJewel = true),

        MonsterTemplate("skeleton-archer", "Skeleton Archer", MapId.DUNGEON, 22, 420, 38, 14, 210, 740, 64f, 18f, 0xFFCBD5E1),
        MonsterTemplate("ghost", "Dark Ghost", MapId.DUNGEON, 28, 560, 48, 18, 290, 980, 76f, 19f, 0xFFA855F7),
        MonsterTemplate("poison-bull", "Poison Bull", MapId.DUNGEON, 34, 780, 58, 22, 380, 1300, 65f, 20f, 0xFF22C55E),
        MonsterTemplate("gorgon", "Gorgon", MapId.DUNGEON, 44, 2600, 88, 30, 1400, 5200, 66f, 30f, 0xFF9333EA, isBoss = true, guaranteedJewel = true),

        MonsterTemplate("ice-monster", "Ice Monster", MapId.DEVIAS, 42, 980, 72, 26, 520, 1800, 62f, 20f, 0xFF38BDF8),
        MonsterTemplate("yeti", "Elite Yeti", MapId.DEVIAS, 50, 1350, 86, 32, 740, 2400, 68f, 22f, 0xFFE0F2FE),
        MonsterTemplate("ice-queen", "Ice Queen", MapId.DEVIAS, 65, 4200, 122, 42, 2400, 8500, 72f, 30f, 0xFF0EA5E9, isBoss = true, guaranteedJewel = true),

        MonsterTemplate("forest-goblin", "Forest Goblin", MapId.NORIA, 68, 1750, 112, 38, 980, 3200, 74f, 18f, 0xFF4ADE80),
        MonsterTemplate("stone-golem", "Stone Golem", MapId.NORIA, 78, 2450, 138, 48, 1350, 4200, 56f, 24f, 0xFF64748B),
        MonsterTemplate("bali", "Bali Guardian", MapId.NORIA, 92, 6400, 175, 58, 3800, 13000, 70f, 32f, 0xFF10B981, isBoss = true, guaranteedJewel = true)
    )

    val DROP_ITEM_POOL: List<Item> = listOf(
        Item("pool-bless", "jewel-bless", "Jewel of Bless", ItemSlot.JEWEL, ItemRarity.EXCELLENT, jewelType = JewelType.BLESS, colorHex = 0xFFA855F7, valueZen = 4500),
        Item("pool-soul", "jewel-soul", "Jewel of Soul", ItemSlot.JEWEL, ItemRarity.EXCELLENT, jewelType = JewelType.SOUL, colorHex = 0xFFEC4899, valueZen = 6000),
        Item("pool-chaos", "jewel-chaos", "Jewel of Chaos", ItemSlot.JEWEL, ItemRarity.MYTHIC, jewelType = JewelType.CHAOS, colorHex = 0xFFF59E0B, valueZen = 8500),
        Item("pool-hp", "hp-potion", "Large Healing Potion", ItemSlot.POTION, ItemRarity.NORMAL, potionType = PotionType.HP, colorHex = 0xFFEF4444, valueZen = 150),
        Item("pool-mp", "mp-potion", "Large Mana Potion", ItemSlot.POTION, ItemRarity.NORMAL, potionType = PotionType.MP, colorHex = 0xFF3B82F6, valueZen = 150),
        Item("pool-aa", "blade-of-archangel", "Archangel Sword", ItemSlot.WEAPON, ItemRarity.EXCELLENT, plus = 3, attack = 46, speedBonus = 12, classReq = "dk", colorHex = 0xFFF59E0B, valueZen = 9500),
        Item("pool-darmor", "dragon-armor", "Dragon Armor", ItemSlot.ARMOR, ItemRarity.EXCELLENT, plus = 2, defense = 34, hpBonus = 110, classReq = "dk", colorHex = 0xFFEF4444, valueZen = 7800),
        Item("pool-dhelm", "dragon-helm", "Dragon Helm", ItemSlot.HELM, ItemRarity.NORMAL, plus = 1, defense = 20, hpBonus = 55, classReq = "dk", colorHex = 0xFFDC2626, valueZen = 4200),
        Item("pool-vstaff", "viper-staff", "Grand Viper Staff", ItemSlot.WEAPON, ItemRarity.EXCELLENT, plus = 3, attack = 24, magic = 52, classReq = "dw", colorHex = 0xFF6366F1, valueZen = 9800),
        Item("pool-gsarmor", "grand-soul-armor", "Grand Soul Armor", ItemSlot.ARMOR, ItemRarity.EXCELLENT, plus = 2, defense = 28, hpBonus = 90, classReq = "dw", colorHex = 0xFF3B82F6, valueZen = 7600),
        Item("pool-vbow", "viper-bow", "Sylph Wind Bow", ItemSlot.WEAPON, ItemRarity.EXCELLENT, plus = 3, attack = 48, speedBonus = 18, classReq = "elf", colorHex = 0xFF10B981, valueZen = 9400),
        Item("pool-garmor", "guardian-armor", "Guardian Armor", ItemSlot.ARMOR, ItemRarity.EXCELLENT, plus = 2, defense = 30, hpBonus = 95, classReq = "elf", colorHex = 0xFF059669, valueZen = 7500),
        Item("pool-pshield", "plate-shield", "Dragon Slayer Shield", ItemSlot.OFFHAND, ItemRarity.NORMAL, plus = 2, defense = 24, hpBonus = 45, classReq = "all", colorHex = 0xFF94A3B8, valueZen = 3900),
        Item("pool-pendant", "pendant-lightning", "Pendant of Lighting", ItemSlot.PENDANT, ItemRarity.ANCIENT, attack = 16, magic = 16, speedBonus = 8, classReq = "all", colorHex = 0xFF38BDF8, valueZen = 8200),
        Item("pool-wings", "wings-satan", "Wings of Satan", ItemSlot.WINGS, ItemRarity.EXCELLENT, plus = 2, attack = 22, defense = 18, speedBonus = 14, classReq = "all", colorHex = 0xFFF43F5E, valueZen = 18000)
    )

    val SHOP_TEMPLATES: List<Item> = DROP_ITEM_POOL.filter {
        it.templateId in setOf(
            "hp-potion", "mp-potion", "blade-of-archangel", "dragon-armor",
            "viper-staff", "grand-soul-armor", "viper-bow", "guardian-armor", "plate-shield"
        )
    }

    val BUILT_IN_MODS: List<ModPackage> = listOf(
        ModPackage(
            id = "mod-fast-x50",
            name = "Fast Server x50",
            author = "MU Community",
            version = "1.2.0",
            description = "Acelera EXP x12, Zen x8 y probabilidad de joyas +60% ideal para sesiones rápidas en móvil.",
            enabled = true,
            builtIn = true,
            expMultiplier = 12.0,
            zenMultiplier = 8.0,
            dropRateMultiplier = 1.6,
            chaosSuccessBonus = 15,
            resetLevel = 120
        ),
        ModPackage(
            id = "mod-conqueror-wings",
            name = "Conqueror Wings & Godlike Pack",
            author = "Kundun Forge",
            version = "2.0.1",
            description = "Añade Alas de Conquistador +9, Bone Blade mítico y Set Dark Phoenix a la tabla de drops.",
            enabled = true,
            builtIn = true,
            expMultiplier = 1.5,
            zenMultiplier = 1.5,
            dropRateMultiplier = 1.3,
            chaosSuccessBonus = 10,
            resetLevel = 400,
            customItems = listOf(
                ModItemDef("mod-wings-conqueror", "Wings of Conqueror", ItemSlot.WINGS, ItemRarity.MYTHIC, 9, attack = 68, magic = 68, defense = 48, hpBonus = 260, speedBonus = 25, colorHex = 0xFFFBBF24, valueZen = 45000),
                ModItemDef("mod-bone-blade", "Mythic Bone Blade", ItemSlot.WEAPON, ItemRarity.MYTHIC, 7, attack = 96, magic = 96, speedBonus = 16, colorHex = 0xFFF43F5E, valueZen = 38000)
            )
        ),
        ModPackage(
            id = "mod-golden-invasion",
            name = "Golden Invasion Bosses",
            author = "Lorencia Vault",
            version = "1.0.4",
            description = "Invoca al Golden Tantallos y Golden Budge Dragon con drop garantizado de Joyas.",
            enabled = true,
            builtIn = true,
            expMultiplier = 2.0,
            zenMultiplier = 3.0,
            dropRateMultiplier = 1.4,
            chaosSuccessBonus = 0,
            resetLevel = 400,
            customMonsters = listOf(
                ModMonsterDef("mod-golden-budge", "Golden Budge Dragon", MapId.LORENCIA, 20, 950, 36, 14, 520, 3200, 0xFFFBBF24, true),
                ModMonsterDef("mod-golden-tantallos", "Golden Tantallos", MapId.CUSTOM, 85, 7500, 165, 55, 4800, 18000, 0xFFF59E0B, true)
            )
        ),
        ModPackage(
            id = "mod-classic-x1",
            name = "Classic Hardcore x1",
            author = "Webzen Oldschool",
            version = "1.0.0",
            description = "Desactiva los bonos de velocidad para experimentar el progreso clásico lento.",
            enabled = false,
            builtIn = true,
            expMultiplier = 0.25,
            zenMultiplier = 0.3,
            dropRateMultiplier = 0.7,
            chaosSuccessBonus = -5,
            resetLevel = 400
        )
    )

    fun newUid(prefix: String = "id"): String = "$prefix-${UUID.randomUUID().toString().take(8)}"

    fun createStarterHero(accountId: String, rawName: String, classId: ClassId): HeroSave {
        val meta = CLASS_CATALOG.getValue(classId)
        val name = rawName.trim().ifEmpty { meta.name }
        return HeroSave(
            id = newUid("hero"),
            accountId = accountId,
            name = name,
            classId = classId,
            level = 1,
            resets = 0,
            exp = 0,
            expToNext = 120,
            zen = 5000,
            hp = meta.baseHp,
            maxHp = meta.baseHp,
            mp = meta.baseMp,
            maxMp = meta.baseMp,
            statPoints = 10,
            stats = meta.baseStats,
            mapId = MapId.LORENCIA,
            x = MuWorldConstants.SAFE_CENTER,
            y = MuWorldConstants.SAFE_CENTER,
            equipment = EquipSlots(
                weapon = meta.starterWeapon.copy(id = newUid("eq")),
                armor = meta.starterArmor.copy(id = newUid("eq"))
            ),
            inventory = listOf(
                Item(newUid("it"), "hp-potion", "Large Healing Potion", ItemSlot.POTION, ItemRarity.NORMAL, potionType = PotionType.HP, colorHex = 0xFFEF4444, valueZen = 150, quantity = 8),
                Item(newUid("it"), "mp-potion", "Large Mana Potion", ItemSlot.POTION, ItemRarity.NORMAL, potionType = PotionType.MP, colorHex = 0xFF3B82F6, valueZen = 150, quantity = 6),
                Item(newUid("it"), "jewel-bless", "Jewel of Bless", ItemSlot.JEWEL, ItemRarity.EXCELLENT, jewelType = JewelType.BLESS, colorHex = 0xFFA855F7, valueZen = 4500, quantity = 2),
                Item(newUid("it"), "jewel-chaos", "Jewel of Chaos", ItemSlot.JEWEL, ItemRarity.MYTHIC, jewelType = JewelType.CHAOS, colorHex = 0xFFF59E0B, valueZen = 8500, quantity = 1)
            ),
            kills = 0,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun computeDerivedHeroStats(hero: HeroSave): DerivedHeroStats {
        var itemAtk = 0
        var itemMagic = 0
        var itemDef = 0
        var itemHp = 0
        var itemSpeed = 0

        for (item in hero.equipment.allEquipped()) {
            val plusBonus = 1.0 + item.plus * 0.14
            itemAtk += (item.attack * plusBonus).roundToInt()
            itemMagic += (item.magic * plusBonus).roundToInt()
            itemDef += (item.defense * plusBonus).roundToInt()
            itemHp += item.hpBonus
            itemSpeed += item.speedBonus
        }

        val baseMeta = CLASS_CATALOG.getValue(hero.classId)
        val attackPower = when (hero.classId) {
            ClassId.DW -> (hero.stats.ene * 1.35 + itemMagic + itemAtk * 0.4 + hero.level * 1.4).roundToInt()
            ClassId.ELF -> (hero.stats.agi * 1.25 + hero.stats.str * 0.45 + itemAtk + hero.level * 1.4).roundToInt()
            ClassId.DK -> (hero.stats.str * 1.3 + hero.stats.agi * 0.35 + itemAtk + hero.level * 1.5).roundToInt()
        }

        val defense = (hero.stats.agi * 0.45 + hero.stats.vit * 0.3 + itemDef + hero.level * 0.9).roundToInt()
        val maxHp = (baseMeta.baseHp + hero.stats.vit * 6 + hero.level * 14 + itemHp)
        val maxMp = (baseMeta.baseMp + hero.stats.ene * 4 + hero.level * 8)
        val critChance = min(0.45f, 0.08f + hero.stats.agi * 0.0012f + if (hero.equipment.wings != null) 0.06f else 0f)
        val moveSpeed = min(275f, 165f + itemSpeed + floor(hero.stats.agi * 0.25f))

        return DerivedHeroStats(attackPower, defense, maxHp, maxMp, critChance, moveSpeed)
    }

    fun computeActiveModRates(mods: List<ModPackage>): ActiveModRates {
        val enabled = mods.filter { it.enabled }
        if (enabled.isEmpty()) return ActiveModRates()
        var expMul = 1.0
        var zenMul = 1.0
        var dropMul = 1.0
        var chaosBonus = 0
        var resetLevel = 400
        for (m in enabled) {
            expMul *= m.expMultiplier
            zenMul *= m.zenMultiplier
            dropMul *= m.dropRateMultiplier
            chaosBonus += m.chaosSuccessBonus
            if (m.resetLevel < resetLevel) resetLevel = m.resetLevel
        }
        return ActiveModRates(
            expMul = expMul.coerceIn(0.1, 250.0),
            zenMul = zenMul.coerceIn(0.1, 250.0),
            dropMul = dropMul.coerceIn(0.2, 10.0),
            chaosBonus = chaosBonus,
            resetLevel = resetLevel
        )
    }

    fun spawnMonstersForMap(mapId: MapId, mods: List<ModPackage>): List<LiveMonster> {
        val modTemplates = mods.filter { it.enabled }.flatMap { m ->
            m.customMonsters.map { cm ->
                MonsterTemplate(
                    id = cm.id,
                    name = cm.name,
                    mapId = cm.mapId,
                    level = cm.level,
                    maxHp = cm.maxHp,
                    attack = cm.attack,
                    defense = cm.defense,
                    expReward = cm.expReward,
                    zenReward = cm.zenReward,
                    speed = 68f,
                    radius = if (cm.isBoss) 28f else 18f,
                    colorHex = cm.colorHex,
                    isBoss = cm.isBoss,
                    guaranteedJewel = cm.isBoss
                )
            }
        }
        var templates = (BASE_MONSTERS + modTemplates).filter { it.mapId == mapId }
        if (templates.isEmpty()) {
            templates = BASE_MONSTERS.filter { it.mapId == MapId.LORENCIA }
        }

        val result = mutableListOf<LiveMonster>()
        for (tpl in templates) {
            val count = if (tpl.isBoss) 1 else 4
            for (i in 0 until count) {
                var mx = 220f
                var my = 220f
                for (attempt in 0 until 20) {
                    val cx = 120f + Random.nextFloat() * (MuWorldConstants.WORLD_SIZE - 240f)
                    val cy = 120f + Random.nextFloat() * (MuWorldConstants.WORLD_SIZE - 240f)
                    mx = cx
                    my = cy
                    if (hypot((cx - MuWorldConstants.SAFE_CENTER).toDouble(), (cy - MuWorldConstants.SAFE_CENTER).toDouble()) > MuWorldConstants.SAFE_RADIUS + 110f) {
                        break
                    }
                }
                result += LiveMonster(
                    uid = "${tpl.id}-$i-${Random.nextInt(1000, 9999)}",
                    templateId = tpl.id,
                    name = tpl.name,
                    mapId = mapId,
                    level = tpl.level,
                    hp = tpl.maxHp,
                    maxHp = tpl.maxHp,
                    attack = tpl.attack,
                    defense = tpl.defense,
                    expReward = tpl.expReward,
                    zenReward = tpl.zenReward,
                    speed = tpl.speed,
                    radius = tpl.radius,
                    colorHex = tpl.colorHex,
                    isBoss = tpl.isBoss,
                    guaranteedJewel = tpl.guaranteedJewel,
                    x = mx,
                    y = my,
                    homeX = mx,
                    homeY = my
                )
            }
        }
        return result
    }

    fun expNeededForLevel(level: Int): Int = (120.0 * level.toDouble().pow(1.38)).roundToInt()

    fun parseColorHex(raw: String?, fallback: Long = 0xFFF59E0B): Long {
        if (raw.isNullOrBlank()) return fallback
        val clean = raw.trim().removePrefix("#")
        return runCatching {
            when (clean.length) {
                6 -> (0xFF000000L or clean.toLong(16))
                8 -> clean.toLong(16)
                else -> fallback
            }
        }.getOrDefault(fallback)
    }

    fun formatColorHex(colorHex: Long): String {
        val rgb = (colorHex and 0x00FFFFFFL).toInt()
        return String.format("#%06X", rgb)
    }
}
