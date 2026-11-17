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

    val DEFAULT_NPCS: List<MapNpcDef> = listOf(
        MapNpcDef("smith", MapId.LORENCIA, "Hanzo, Blacksmith", "shop", "Armas DK/MG", 370f, 420f),
        MapNpcDef("armor-lorencia", MapId.LORENCIA, "Lumen, Armaduras", "shop", "Sets Lorencia", 480f, 385f),
        MapNpcDef("wizard", MapId.LORENCIA, "Pasi Wizard", "shop", "Bastones y Magia", 590f, 420f),
        MapNpcDef("barmaid", MapId.LORENCIA, "Liaman Barmaid", "shop", "Pociones", 370f, 545f),
        MapNpcDef("vault-lorencia", MapId.LORENCIA, "Guardia del Baúl", "vault", "Almacén Seguro", 590f, 545f),
        MapNpcDef("marlon", MapId.LORENCIA, "Marlon (Evolución)", "quest", "Quest Nv.150+ / 400+", 480f, 585f),
        MapNpcDef("elf-merchant", MapId.NORIA, "Eo the Craftsman", "shop", "Arcos y Sets Elfa", 385f, 435f),
        MapNpcDef("chaos", MapId.NORIA, "Chaos Goblin", "chaos", "Alas S1/S2/S3 y +13", 575f, 435f),
        MapNpcDef("vault-noria", MapId.NORIA, "Guardián Noria", "vault", "Baúl Compartido", 480f, 565f),
        MapNpcDef("devias-potion", MapId.DEVIAS, "Zienna, Mercader", "shop", "Pociones y Armas Devias", 385f, 435f),
        MapNpcDef("golden-archer", MapId.DEVIAS, "Golden Archer", "archer", "Talismanes -> Alas S2", 575f, 435f),
        MapNpcDef("vault-devias", MapId.DEVIAS, "Guardián Devias", "vault", "Baúl Compartido", 480f, 565f),
        MapNpcDef("shop-dungeon1", MapId.DUNGEON1, "Alex, Explorador", "shop", "Pociones Dungeon", 420f, 430f),
        MapNpcDef("shop-dungeon2", MapId.DUNGEON2, "Sombra Mercader", "shop", "Equipo Catacumbas", 420f, 430f),
        MapNpcDef("shop-dungeon3", MapId.DUNGEON3, "Thompson, Maestro", "shop", "Suministros Gorgon", 420f, 430f),
        MapNpcDef("shop-atlans", MapId.ATLANS, "Guardia Marino", "shop", "Tienda de Atlans", 420f, 430f),
        MapNpcDef("shop-losttower", MapId.LOSTTOWER, "Centinela Torre", "shop", "Pociones Lost Tower", 420f, 430f),
        MapNpcDef("shop-kalima", MapId.KALIMA, "Oráculo Kalima", "shop", "Provisiones Kundun", 420f, 430f),
        MapNpcDef("shop-tarkan", MapId.TARKAN, "Mercader Tarkan", "shop", "Suministros Desierto", 420f, 430f),
        MapNpcDef("shop-icarus", MapId.ICARUS, "Guía de Icarus", "shop", "Pociones Celestiales", 420f, 430f),
        MapNpcDef("shop-kanturu", MapId.KANTURU, "Ingeniero Kanturu", "shop", "Armería Antigua", 420f, 430f),
        MapNpcDef("shop-aida", MapId.AIDA, "Druida de Aida", "shop", "Elixires del Bosque", 420f, 430f),
        MapNpcDef("shop-crywolf", MapId.CRYWOLF, "Comandante CryWolf", "shop", "Frente de Batalla", 420f, 430f),
        MapNpcDef("shop-stadium", MapId.STADIUM, "Maestro Stadium", "shop", "Suministros Arena", 420f, 430f),
        MapNpcDef("shop-custom", MapId.CUSTOM, "Goblin Modder", "chaos", "Chaos & Arena Mod", 420f, 430f)
    )

    val CLASS_CATALOG: Map<ClassId, ClassMeta> = linkedMapOf(
        ClassId.DK to ClassMeta(
            id = ClassId.DK,
            name = "Dark Knight",
            evo150Title = "Blade Knight",
            evo400Title = "Blade Master",
            role = "Cuerpo a cuerpo · Alta defensa · 5 pts/nv",
            portraitRes = R.drawable.img_dark_knight,
            accentColor = 0xFFEF4444,
            pointsPerLevel = 5,
            unlockLevel = 1,
            canEquipHelm = true,
            baseStats = HeroStats(str = 28, agi = 20, vit = 25, ene = 10),
            baseHp = 220,
            baseMp = 90,
            attackRange = 72f,
            starterWeapon = Item("starter-kris", "short-sword", "Kris Blade", ItemSlot.WEAPON, ItemRarity.NORMAL, attack = 18, classReq = "dk", colorHex = 0xFFEF4444, valueZen = 1200),
            starterArmor = Item("starter-leather", "leather-armor", "Leather Armor", ItemSlot.ARMOR, ItemRarity.NORMAL, defense = 12, hpBonus = 30, classReq = "dk", colorHex = 0xFFB45309, valueZen = 1000)
        ),
        ClassId.DW to ClassMeta(
            id = ClassId.DW,
            name = "Dark Wizard",
            evo150Title = "Soul Master",
            evo400Title = "Grand Master",
            role = "Daño mágico masivo · 5 pts/nv",
            portraitRes = R.drawable.img_dark_wizard,
            accentColor = 0xFF3B82F6,
            pointsPerLevel = 5,
            unlockLevel = 1,
            canEquipHelm = true,
            baseStats = HeroStats(str = 14, agi = 18, vit = 16, ene = 35),
            baseHp = 155,
            baseMp = 190,
            attackRange = 185f,
            starterWeapon = Item("starter-skull-staff", "skull-staff", "Skull Staff", ItemSlot.WEAPON, ItemRarity.NORMAL, attack = 10, magic = 22, classReq = "dw", colorHex = 0xFF3B82F6, valueZen = 1400),
            starterArmor = Item("starter-pad", "pad-armor", "Pad Armor", ItemSlot.ARMOR, ItemRarity.NORMAL, defense = 9, hpBonus = 15, classReq = "dw", colorHex = 0xFF2563EB, valueZen = 950)
        ),
        ClassId.ELF to ClassMeta(
            id = ClassId.ELF,
            name = "Fairy Elf",
            evo150Title = "Muse Elf",
            evo400Title = "High Elf",
            role = "Arco, buffs y curación · 5 pts/nv",
            portraitRes = R.drawable.img_fairy_elf,
            accentColor = 0xFF10B981,
            pointsPerLevel = 5,
            unlockLevel = 1,
            canEquipHelm = true,
            baseStats = HeroStats(str = 18, agi = 32, vit = 18, ene = 15),
            baseHp = 175,
            baseMp = 140,
            attackRange = 200f,
            starterWeapon = Item("starter-elven-bow", "elven-bow", "Elven Bow", ItemSlot.WEAPON, ItemRarity.NORMAL, attack = 19, speedBonus = 10, classReq = "elf", colorHex = 0xFF10B981, valueZen = 1350),
            starterArmor = Item("starter-vine", "vine-armor", "Vine Armor", ItemSlot.ARMOR, ItemRarity.NORMAL, defense = 10, hpBonus = 20, classReq = "elf", colorHex = 0xFF059669, valueZen = 1000)
        ),
        ClassId.MG to ClassMeta(
            id = ClassId.MG,
            name = "Magic Gladiator",
            evo150Title = "Duel Master",
            evo400Title = "Magic Knight",
            role = "Híbrido DK+DW · 7 pts/nv · Sin casco",
            portraitRes = R.drawable.img_dark_knight,
            accentColor = 0xFFA855F7,
            pointsPerLevel = 7,
            unlockLevel = 220,
            canEquipHelm = false,
            baseStats = HeroStats(str = 26, agi = 26, vit = 24, ene = 26),
            baseHp = 210,
            baseMp = 155,
            attackRange = 135f,
            starterWeapon = Item("starter-runeblade", "rune-blade", "Rune Bastard Sword", ItemSlot.WEAPON, ItemRarity.EXCELLENT, attack = 24, magic = 20, classReq = "mg", colorHex = 0xFFA855F7, valueZen = 2800),
            starterArmor = Item("starter-stormcrow", "stormcrow-armor", "Storm Crow Armor", ItemSlot.ARMOR, ItemRarity.NORMAL, defense = 16, hpBonus = 40, classReq = "mg", colorHex = 0xFF9333EA, valueZen = 2200)
        ),
        ClassId.DL to ClassMeta(
            id = ClassId.DL,
            name = "Dark Lord",
            evo150Title = "Lord Emperor",
            evo400Title = "Empire Lord",
            role = "Líder físico/mágico · Cuervo y Caballo · 7 pts/nv",
            portraitRes = R.drawable.img_dark_wizard,
            accentColor = 0xFFF59E0B,
            pointsPerLevel = 7,
            unlockLevel = 300,
            canEquipHelm = true,
            baseStats = HeroStats(str = 26, agi = 22, vit = 22, ene = 28),
            baseHp = 215,
            baseMp = 160,
            attackRange = 165f,
            starterWeapon = Item("starter-battle-scepter", "battle-scepter", "Battle Scepter", ItemSlot.WEAPON, ItemRarity.EXCELLENT, attack = 25, magic = 24, classReq = "dl", colorHex = 0xFFF59E0B, valueZen = 3200),
            starterArmor = Item("starter-lightplate", "lightplate-armor", "Light Plate Armor", ItemSlot.ARMOR, ItemRarity.NORMAL, defense = 18, hpBonus = 45, classReq = "dl", colorHex = 0xFFD97706, valueZen = 2600)
        )
    )

    val SKILLS_BY_CLASS: Map<ClassId, List<SkillDef>> = mapOf(
        ClassId.DK to listOf(
            SkillDef("twisting-slash", "Twisting Slash", ClassId.DK, 12, 1300L, 1.80f, 90f, 100f, 0xFFF97316, "Giro mortal con espada alrededor del guerrero."),
            SkillDef("death-stab", "Death Stab", ClassId.DK, 16, 2100L, 2.40f, 100f, 52f, 0xFFEF4444, "Estocada perforante con ráfaga de viento."),
            SkillDef("rageful-blow", "Rageful Blow", ClassId.DK, 24, 3800L, 2.95f, 95f, 135f, 0xFFF59E0B, "Impacto sísmico de Blade Knight.")
        ),
        ClassId.DW to listOf(
            SkillDef("evil-spirit", "Evil Spirit", ClassId.DW, 18, 1400L, 1.70f, 205f, 150f, 0xFF6366F1, "Invoca espíritus oscuros que azotan la zona."),
            SkillDef("meteorite", "Meteorite", ClassId.DW, 20, 2000L, 2.45f, 225f, 75f, 0xFFF59E0B, "Impacto estelar directo sobre el objetivo."),
            SkillDef("ice-storm", "Ice Storm", ClassId.DW, 28, 3600L, 2.70f, 215f, 160f, 0xFF38BDF8, "Tormenta glacial masiva de Soul Master.")
        ),
        ClassId.ELF to listOf(
            SkillDef("triple-shot", "Triple Shot", ClassId.ELF, 10, 1100L, 1.75f, 225f, 88f, 0xFF10B981, "Dispara tres flechas simultáneas en abanico."),
            SkillDef("ice-arrow", "Ice Arrow", ClassId.ELF, 18, 2200L, 2.50f, 235f, 54f, 0xFF38BDF8, "Flecha de escarcha con alto daño crítico."),
            SkillDef("heal-aura", "Heal & Defense", ClassId.ELF, 22, 4000L, 0f, 0f, 120f, 0xFF34D399, "Restaura 45% de HP al instante.")
        ),
        ClassId.MG to listOf(
            SkillDef("power-slash", "Power Slash", ClassId.MG, 16, 1350L, 2.05f, 155f, 105f, 0xFFA855F7, "Corte arcano de largo alcance con espada rúnica."),
            SkillDef("fire-slash", "Fire Slash", ClassId.MG, 20, 2100L, 2.55f, 125f, 78f, 0xFFF97316, "Cuchillada flamígera que reduce defensa."),
            SkillDef("flame-strike", "Flame Strike", ClassId.MG, 28, 3500L, 3.05f, 165f, 145f, 0xFFEF4444, "Explosión mixta física y mágica de Duel Master.")
        ),
        ClassId.DL to listOf(
            SkillDef("fire-burst", "Fire Burst", ClassId.DL, 16, 1350L, 2.00f, 185f, 95f, 0xFFF59E0B, "Cadena de llamas invocadas con el cetro imperial."),
            SkillDef("chaotic-diseier", "Chaotic Diseier", ClassId.DL, 24, 2300L, 2.60f, 195f, 140f, 0xFFEC4899, "Destello imperial de Lord Emperor en área."),
            SkillDef("dark-raven-strike", "Dark Raven", ClassId.DL, 20, 3200L, 2.85f, 220f, 85f, 0xFF38BDF8, "Ordena al Cuervo Oscuro embestir al enemigo.")
        )
    )

    val DEFAULT_MAP_CATALOG: Map<MapId, MapMeta> = linkedMapOf(
        MapId.LORENCIA to MapMeta(MapId.LORENCIA, "Lorencia", 1, 10, 0.35, 1.0, 0.40, 0xFF1A2419, 0xFF222F20, 0xFF2E2B26, 0xFFD97706, "Ciudadela central · Arañas, Budge Dragon y Dragón Rojo"),
        MapId.NORIA to MapMeta(MapId.NORIA, "Noria", 1, 10, 0.35, 1.0, 0.40, 0xFF132721, 0xFF18342C, 0xFF253D34, 0xFF10B981, "Bosque élfico · Chaos Goblin y Golems"),
        MapId.DEVIAS to MapMeta(MapId.DEVIAS, "Devias", 15, 50, 0.35, 1.15, 0.42, 0xFF182433, 0xFF203247, 0xFF283A4E, 0xFF38BDF8, "Bastión nevado · Yetis, Golden Archer y Reina del Hielo"),
        MapId.DUNGEON1 to MapMeta(MapId.DUNGEON1, "Dungeon 1", 20, 20, 0.35, 1.2, 0.42, 0xFF15141E, 0xFF1E1B2B, 0xFF252236, 0xFFA855F7, "Catacumbas nivel 1 · Esqueletos y Larvas"),
        MapId.DUNGEON2 to MapMeta(MapId.DUNGEON2, "Dungeon 2", 40, 50, 0.35, 1.35, 0.44, 0xFF171422, 0xFF221D33, 0xFF2A243D, 0xFF9333EA, "Catacumbas nivel 2 · Cíclopes y Fantasmas"),
        MapId.DUNGEON3 to MapMeta(MapId.DUNGEON3, "Dungeon 3", 60, 60, 0.35, 1.5, 0.48, 0xFF1B1224, 0xFF271834, 0xFF2F203D, 0xFFC084FC, "Cámara profunda de Gorgon y Poison Bull"),
        MapId.ATLANS to MapMeta(MapId.ATLANS, "Atlans", 60, 70, 0.35, 1.7, 0.50, 0xFF0F2233, 0xFF152F47, 0xFF1C3B57, 0xFF0EA5E9, "Reino submarino · Bahamut, Vepar e Hydra"),
        MapId.LOSTTOWER to MapMeta(MapId.LOSTTOWER, "Lost Tower 1-7", 80, 80, 0.35, 2.0, 0.52, 0xFF211520, 0xFF2D1C2B, 0xFF3B2638, 0xFFEC4899, "Torre maldita de Shadow, Death Knight y Balrog"),
        MapId.KALIMA to MapMeta(MapId.KALIMA, "Kalima", 70, 70, 0.38, 2.2, 0.56, 0xFF122126, 0xFF192F36, 0xFF233E47, 0xFF14B8A6, "Templo sumergido · Illusion of Kundun"),
        MapId.TARKAN to MapMeta(MapId.TARKAN, "Tarkan", 130, 140, 0.35, 2.6, 0.58, 0xFF2B1E14, 0xFF38271A, 0xFF473324, 0xFFF97316, "Desierto abrasador · Mutant, Iron Wheel y Zaikan"),
        MapId.ICARUS to MapMeta(MapId.ICARUS, "Icarus", 160, 170, 0.35, 3.0, 0.60, 0xFF18233A, 0xFF202F4F, 0xFF2A3C63, 0xFF60A5FA, "Cielos flotantes · Phoenix of Darkness"),
        MapId.KANTURU to MapMeta(MapId.KANTURU, "Kanturu Ruins", 170, 170, 0.35, 3.4, 0.62, 0xFF1E1F26, 0xFF282A33, 0xFF333642, 0xFF94A3B8, "Ruinas mecánicas antiguas · Persona y Nightmare"),
        MapId.AIDA to MapMeta(MapId.AIDA, "Aida", 150, 150, 0.35, 2.9, 0.60, 0xFF15261A, 0xFF1E3625, 0xFF284530, 0xFF4ADE80, "Selva mutante · Witch Queen y Hell Maine"),
        MapId.CRYWOLF to MapMeta(MapId.CRYWOLF, "CryWolf", 180, 180, 0.35, 3.6, 0.65, 0xFF221D1A, 0xFF2E2723, 0xFF3C332E, 0xFFFBBF24, "Fortaleza fronteriza · Balgass y Dark Elf"),
        MapId.STADIUM to MapMeta(MapId.STADIUM, "Stadium", 255, 255, 0.45, 4.2, 0.70, 0xFF211618, 0xFF2E1E21, 0xFF3B282B, 0xFFF43F5E, "Arena de entrenamiento de alto nivel · Solo Warp"),
        MapId.CUSTOM to MapMeta(MapId.CUSTOM, "Arena Mod", 1, 1, 1.0, 3.0, 0.80, 0xFF241418, 0xFF301B21, 0xFF382228, 0xFFFB7185, "Zona de desafío personalizada por Mods")
    )

    val BASE_MONSTERS: List<MonsterTemplate> = listOf(
        // Lorencia
        MonsterTemplate("spider", "Spider", MapId.LORENCIA, 2, 70, 8, 2, 32, 130, 56f, 15f, 0xFF64748B),
        MonsterTemplate("budge-dragon", "Budge Dragon", MapId.LORENCIA, 4, 100, 11, 3, 44, 180, 58f, 16f, 0xFF84CC16),
        MonsterTemplate("bull-fighter", "Bull Fighter", MapId.LORENCIA, 8, 175, 16, 6, 72, 260, 62f, 18f, 0xFFDC2626),
        MonsterTemplate("elite-skeleton", "Skeleton Warrior", MapId.LORENCIA, 14, 280, 24, 10, 130, 480, 65f, 18f, 0xFFE2E8F0),
        MonsterTemplate("red-dragon", "Red Dragon", MapId.LORENCIA, 24, 1200, 48, 18, 640, 2600, 68f, 28f, 0xFFEF4444, isBoss = true, guaranteedJewel = true),

        // Noria
        MonsterTemplate("goblin", "Goblin", MapId.NORIA, 3, 85, 9, 3, 36, 150, 60f, 15f, 0xFF4ADE80),
        MonsterTemplate("chain-scorpion", "Chain Scorpion", MapId.NORIA, 6, 140, 14, 5, 58, 220, 64f, 17f, 0xFF22C55E),
        MonsterTemplate("stone-golem", "Stone Golem", MapId.NORIA, 16, 360, 28, 12, 175, 580, 52f, 22f, 0xFF64748B),
        MonsterTemplate("bali", "Bali Guardian", MapId.NORIA, 28, 1450, 55, 20, 780, 3100, 68f, 30f, 0xFF10B981, isBoss = true, guaranteedJewel = true),

        // Devias
        MonsterTemplate("worm", "Ice Worm", MapId.DEVIAS, 18, 390, 32, 13, 195, 650, 60f, 18f, 0xFF7DD3FC),
        MonsterTemplate("ice-monster", "Ice Monster", MapId.DEVIAS, 24, 540, 42, 16, 260, 900, 62f, 20f, 0xFF38BDF8),
        MonsterTemplate("yeti", "Elite Yeti", MapId.DEVIAS, 32, 820, 56, 22, 410, 1400, 68f, 22f, 0xFFE0F2FE),
        MonsterTemplate("ice-queen", "Ice Queen", MapId.DEVIAS, 46, 2400, 86, 30, 1400, 5200, 72f, 30f, 0xFF0EA5E9, isBoss = true, guaranteedJewel = true),

        // Dungeon 1, 2, 3
        MonsterTemplate("larva", "Larva", MapId.DUNGEON1, 22, 460, 38, 14, 220, 740, 60f, 16f, 0xFF84CC16),
        MonsterTemplate("skeleton-archer", "Skeleton Archer", MapId.DUNGEON1, 26, 580, 44, 16, 280, 920, 64f, 18f, 0xFFCBD5E1),
        MonsterTemplate("cyclops", "Cyclops", MapId.DUNGEON2, 42, 980, 66, 24, 520, 1700, 62f, 22f, 0xFFA855F7),
        MonsterTemplate("ghost", "Dark Ghost", MapId.DUNGEON2, 48, 1180, 74, 27, 640, 2100, 76f, 19f, 0xFF9333EA),
        MonsterTemplate("poison-bull", "Poison Bull", MapId.DUNGEON3, 62, 1650, 95, 34, 920, 2900, 66f, 21f, 0xFF22C55E),
        MonsterTemplate("gorgon", "Gorgon", MapId.DUNGEON3, 72, 4200, 128, 42, 2400, 7800, 68f, 30f, 0xFFC084FC, isBoss = true, guaranteedJewel = true),

        // Atlans
        MonsterTemplate("bahamut", "Bahamut", MapId.ATLANS, 62, 1580, 92, 32, 880, 2700, 70f, 19f, 0xFF38BDF8),
        MonsterTemplate("vepar", "Vepar", MapId.ATLANS, 68, 1880, 106, 36, 1080, 3300, 72f, 20f, 0xFF0284C7),
        MonsterTemplate("hydra", "Hydra", MapId.ATLANS, 82, 5200, 145, 48, 3100, 9800, 68f, 32f, 0xFF0EA5E9, isBoss = true, guaranteedJewel = true),

        // Lost Tower 1-7
        MonsterTemplate("shadow", "Shadow", MapId.LOSTTOWER, 82, 2350, 126, 42, 1380, 4100, 74f, 20f, 0xFF6B21A8),
        MonsterTemplate("death-knight", "Death Knight", MapId.LOSTTOWER, 96, 3100, 152, 50, 1850, 5400, 72f, 22f, 0xFFEC4899),
        MonsterTemplate("balrog", "Balrog", MapId.LOSTTOWER, 115, 7600, 195, 62, 4600, 14000, 70f, 32f, 0xFFEF4444, isBoss = true, guaranteedJewel = true),

        // Kalima
        MonsterTemplate("aegis", "Aegis", MapId.KALIMA, 76, 2200, 118, 40, 1320, 3900, 68f, 20f, 0xFF14B8A6),
        MonsterTemplate("kundun", "Illusion of Kundun", MapId.KALIMA, 110, 8200, 205, 65, 5200, 16500, 72f, 34f, 0xFF2DD4BF, isBoss = true, guaranteedJewel = true),

        // Tarkan
        MonsterTemplate("mutant", "Mutant", MapId.TARKAN, 132, 4200, 188, 62, 2600, 7500, 74f, 21f, 0xFFF97316),
        MonsterTemplate("iron-wheel", "Iron Wheel", MapId.TARKAN, 144, 5100, 210, 70, 3200, 9200, 78f, 22f, 0xFFFB923C),
        MonsterTemplate("zaikan", "Zaikan", MapId.TARKAN, 160, 11500, 260, 85, 7400, 22000, 74f, 34f, 0xFFEA580C, isBoss = true, guaranteedJewel = true),

        // Icarus
        MonsterTemplate("alquamos", "Alquamos", MapId.ICARUS, 162, 5800, 232, 76, 3800, 10500, 78f, 20f, 0xFF60A5FA),
        MonsterTemplate("phoenix-darkness", "Phoenix of Darkness", MapId.ICARUS, 185, 14500, 305, 96, 9200, 28000, 76f, 34f, 0xFF3B82F6, isBoss = true, guaranteedJewel = true),

        // Kanturu Ruins
        MonsterTemplate("persona", "Persona", MapId.KANTURU, 174, 6900, 265, 86, 4600, 12800, 75f, 22f, 0xFF94A3B8),
        MonsterTemplate("nightmare", "Nightmare", MapId.KANTURU, 205, 17500, 340, 108, 11800, 34000, 74f, 34f, 0xFF6366F1, isBoss = true, guaranteedJewel = true),

        // Aida
        MonsterTemplate("witch-queen", "Witch Queen", MapId.AIDA, 155, 5600, 225, 74, 3600, 9800, 74f, 21f, 0xFF4ADE80),
        MonsterTemplate("hell-maine", "Hell Maine", MapId.AIDA, 178, 13800, 290, 92, 8600, 25000, 72f, 33f, 0xFF16A34A, isBoss = true, guaranteedJewel = true),

        // CryWolf
        MonsterTemplate("dark-elf", "Dark Elf (CryWolf)", MapId.CRYWOLF, 188, 7800, 288, 92, 5200, 14500, 80f, 22f, 0xFFFBBF24),
        MonsterTemplate("balgass", "Balgass", MapId.CRYWOLF, 220, 21000, 380, 118, 14000, 42000, 76f, 36f, 0xFFD97706, isBoss = true, guaranteedJewel = true),

        // Stadium
        MonsterTemplate("hero-mutant", "Arena Champion", MapId.STADIUM, 258, 11000, 350, 115, 7800, 21000, 82f, 24f, 0xFFF43F5E),
        MonsterTemplate("arena-lord", "Kundun Avatar", MapId.STADIUM, 290, 28000, 450, 138, 19500, 56000, 78f, 36f, 0xFFE11D48, isBoss = true, guaranteedJewel = true)
    )

    val DROP_ITEM_POOL: List<Item> = listOf(
        Item("pool-bless", "jewel-bless", "Jewel of Bless", ItemSlot.JEWEL, ItemRarity.EXCELLENT, jewelType = JewelType.BLESS, colorHex = 0xFFA855F7, valueZen = 4500),
        Item("pool-soul", "jewel-soul", "Jewel of Soul", ItemSlot.JEWEL, ItemRarity.EXCELLENT, jewelType = JewelType.SOUL, colorHex = 0xFFEC4899, valueZen = 6000),
        Item("pool-chaos", "jewel-chaos", "Jewel of Chaos", ItemSlot.JEWEL, ItemRarity.MYTHIC, jewelType = JewelType.CHAOS, colorHex = 0xFFF59E0B, valueZen = 8500),
        Item("pool-life", "jewel-life", "Jewel of Life", ItemSlot.JEWEL, ItemRarity.EXCELLENT, jewelType = JewelType.LIFE, colorHex = 0xFF34D399, valueZen = 9500),
        Item("pool-creation", "jewel-creation", "Jewel of Creation", ItemSlot.JEWEL, ItemRarity.MYTHIC, jewelType = JewelType.CREATION, colorHex = 0xFFFBBF24, valueZen = 12000),
        Item("pool-rena", "rena-talisman", "Talisman / Rena", ItemSlot.TALISMAN, ItemRarity.EXCELLENT, colorHex = 0xFFFDE047, valueZen = 2500),
        Item("pool-hp", "hp-potion", "Large Healing Potion", ItemSlot.POTION, ItemRarity.NORMAL, potionType = PotionType.HP, colorHex = 0xFFEF4444, valueZen = 150),
        Item("pool-mp", "mp-potion", "Large Mana Potion", ItemSlot.POTION, ItemRarity.NORMAL, potionType = PotionType.MP, colorHex = 0xFF3B82F6, valueZen = 150),
        Item("pool-aa", "blade-of-archangel", "Archangel Sword", ItemSlot.WEAPON, ItemRarity.EXCELLENT, plus = 3, attack = 46, speedBonus = 12, classReq = "dk", colorHex = 0xFFF59E0B, valueZen = 9500),
        Item("pool-darmor", "dragon-armor", "Dragon Armor", ItemSlot.ARMOR, ItemRarity.EXCELLENT, plus = 2, defense = 34, hpBonus = 110, classReq = "dk", colorHex = 0xFFEF4444, valueZen = 7800),
        Item("pool-dhelm", "dragon-helm", "Dragon Helm", ItemSlot.HELM, ItemRarity.NORMAL, plus = 1, defense = 20, hpBonus = 55, classReq = "dk", colorHex = 0xFFDC2626, valueZen = 4200),
        Item("pool-vstaff", "viper-staff", "Grand Viper Staff", ItemSlot.WEAPON, ItemRarity.EXCELLENT, plus = 3, attack = 24, magic = 52, classReq = "dw", colorHex = 0xFF6366F1, valueZen = 9800),
        Item("pool-gsarmor", "grand-soul-armor", "Grand Soul Armor", ItemSlot.ARMOR, ItemRarity.EXCELLENT, plus = 2, defense = 28, hpBonus = 90, classReq = "dw", colorHex = 0xFF3B82F6, valueZen = 7600),
        Item("pool-vbow", "viper-bow", "Sylph Wind Bow", ItemSlot.WEAPON, ItemRarity.EXCELLENT, plus = 3, attack = 48, speedBonus = 18, classReq = "elf", colorHex = 0xFF10B981, valueZen = 9400),
        Item("pool-garmor", "guardian-armor", "Guardian Armor", ItemSlot.ARMOR, ItemRarity.EXCELLENT, plus = 2, defense = 30, hpBonus = 95, classReq = "elf", colorHex = 0xFF059669, valueZen = 7500),
        Item("pool-mgblade", "dark-reign-blade", "Dark Reign Blade", ItemSlot.WEAPON, ItemRarity.EXCELLENT, plus = 3, attack = 54, magic = 44, classReq = "mg", colorHex = 0xFFA855F7, valueZen = 12500),
        Item("pool-dlscepter", "soleil-scepter", "Soleil Scepter", ItemSlot.WEAPON, ItemRarity.EXCELLENT, plus = 3, attack = 50, magic = 48, classReq = "dl", colorHex = 0xFFF59E0B, valueZen = 12800),
        Item("pool-raven", "dark-raven", "Dark Raven (Mascota DL)", ItemSlot.PET, ItemRarity.ANCIENT, plus = 1, attack = 28, speedBonus = 10, classReq = "dl", colorHex = 0xFF38BDF8, valueZen = 15000),
        Item("pool-horse", "dark-horse", "Dark Horse (Mascota DL)", ItemSlot.PET, ItemRarity.MYTHIC, plus = 1, defense = 32, hpBonus = 150, speedBonus = 24, classReq = "dl", colorHex = 0xFFF59E0B, valueZen = 22000),
        Item("pool-pshield", "plate-shield", "Dragon Slayer Shield", ItemSlot.OFFHAND, ItemRarity.NORMAL, plus = 2, defense = 24, hpBonus = 45, classReq = "all", colorHex = 0xFF94A3B8, valueZen = 3900),
        Item("pool-pendant", "pendant-lightning", "Pendant of Lighting", ItemSlot.PENDANT, ItemRarity.ANCIENT, attack = 16, magic = 16, speedBonus = 8, classReq = "all", colorHex = 0xFF38BDF8, valueZen = 8200),
        Item("pool-wings-s1", "wings-satan-s1", "Wings of Satan (S1)", ItemSlot.WINGS, ItemRarity.NORMAL, plus = 1, attack = 20, magic = 20, defense = 16, speedBonus = 14, wingsTier = 1, classReq = "all", colorHex = 0xFFA855F7, valueZen = 16000)
    )

    val SHOP_TEMPLATES: List<Item> = DROP_ITEM_POOL.filter {
        it.templateId in setOf(
            "hp-potion", "mp-potion", "blade-of-archangel", "dragon-armor",
            "viper-staff", "grand-soul-armor", "viper-bow", "guardian-armor",
            "dark-reign-blade", "soleil-scepter", "dark-raven", "plate-shield", "rena-talisman"
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
            name = "Wings S3 & Godlike Pack",
            author = "Kundun Forge",
            version = "2.1.0",
            description = "Añade Wings of Storm (S3) y Mythic Bone Blade a la tabla de drops.",
            enabled = true,
            builtIn = true,
            expMultiplier = 1.5,
            zenMultiplier = 1.5,
            dropRateMultiplier = 1.3,
            chaosSuccessBonus = 10,
            resetLevel = 400,
            customItems = listOf(
                ModItemDef("mod-wings-s3", "Wings of Storm (S3)", ItemSlot.WINGS, ItemRarity.MYTHIC, 9, attack = 74, magic = 74, defense = 58, hpBonus = 280, speedBonus = 30, wingsTier = 3, colorHex = 0xFFFBBF24, valueZen = 68000),
                ModItemDef("mod-bone-blade", "Mythic Bone Blade", ItemSlot.WEAPON, ItemRarity.MYTHIC, 7, attack = 96, magic = 96, speedBonus = 16, colorHex = 0xFFF43F5E, valueZen = 38000)
            )
        ),
        ModPackage(
            id = "mod-golden-invasion",
            name = "Golden Invasion Bosses",
            author = "Lorencia Vault",
            version = "1.0.4",
            description = "Invoca al Golden Budge Dragon y Golden Tantallos con drop garantizado de Joyas.",
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
            name = "Classic Hardcore 35% EXP",
            author = "Webzen Oldschool",
            version = "1.0.0",
            description = "Desactiva bonos rápidos para jugar con la tasa clásica 35% por mapa.",
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
            evolutionTier = 0,
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

        val evoMultiplier = 1.0 + hero.evolutionTier * 0.15
        val baseMeta = CLASS_CATALOG.getValue(hero.classId)
        val rawAttack = when (hero.classId) {
            ClassId.DW -> (hero.stats.ene * 1.35 + itemMagic + itemAtk * 0.4 + hero.level * 1.4)
            ClassId.ELF -> (hero.stats.agi * 1.25 + hero.stats.str * 0.45 + itemAtk + hero.level * 1.4)
            ClassId.DK -> (hero.stats.str * 1.3 + hero.stats.agi * 0.35 + itemAtk + hero.level * 1.5)
            ClassId.MG -> (hero.stats.str * 1.0 + hero.stats.ene * 0.95 + itemAtk + itemMagic * 0.8 + hero.level * 1.55)
            ClassId.DL -> (hero.stats.str * 1.05 + hero.stats.ene * 1.05 + itemAtk + itemMagic * 0.75 + hero.level * 1.6)
        }
        val attackPower = (rawAttack * evoMultiplier).roundToInt()

        val defense = ((hero.stats.agi * 0.45 + hero.stats.vit * 0.3 + itemDef + hero.level * 0.9) * evoMultiplier).roundToInt()
        val maxHp = (baseMeta.baseHp + hero.stats.vit * 6 + hero.level * 14 + itemHp + hero.evolutionTier * 120)
        val maxMp = (baseMeta.baseMp + hero.stats.ene * 4 + hero.level * 8 + hero.evolutionTier * 60)
        val critChance = min(0.48f, 0.08f + hero.stats.agi * 0.0012f + if (hero.equipment.wings != null) 0.06f else 0f)
        val moveSpeed = min(285f, 165f + itemSpeed + floor(hero.stats.agi * 0.25f))

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
