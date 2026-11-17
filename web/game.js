/* Mu Offline Mobile — Season 6 Episode 3 (Web HTML5 Canvas + Flat JSON) */
(() => {
  const WORLD_SIZE = 1400;
  const SAFE_CENTER = 480;
  const SAFE_RADIUS = 135;

  const STORAGE_KEYS = {
    account: "mu_web_account",
    heroesPrefix: "mu_web_heroes_",
    vaultPrefix: "mu_web_vault_",
    mods: "mu_web_mods",
    maps: "mu_web_maps",
    npcs: "mu_web_npcs",
    settings: "mu_web_settings",
    manifest: "mu_web_manifest"
  };

  const CLASSES = {
    dk: {
      id: "dk", name: "Dark Knight", evo150: "Blade Knight", evo400: "Blade Master",
      role: "Cuerpo a cuerpo · 5 pts/nv", color: "#ef4444", ptsPerLevel: 5, unlockLevel: 1,
      canHelm: true, baseStats: { str: 28, agi: 20, vit: 25, ene: 10 }, baseHp: 220, baseMp: 90, range: 78,
      starterWeapon: { templateId: "short-sword", name: "Kris Blade", slot: "weapon", rarity: "normal", plus: 0, attack: 18, defense: 0, magic: 0, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "dk", color: "#ef4444", valueZen: 1200, quantity: 1 },
      starterArmor: { templateId: "leather-armor", name: "Leather Armor", slot: "armor", rarity: "normal", plus: 0, attack: 0, defense: 12, magic: 0, hpBonus: 30, speedBonus: 0, wingsTier: 0, classReq: "dk", color: "#b45309", valueZen: 1000, quantity: 1 },
      skills: [
        { id: "twisting-slash", name: "Twisting Slash", mp: 12, cd: 1300, mult: 1.85, range: 95, aoe: 105, color: "#f97316" },
        { id: "death-stab", name: "Death Stab", mp: 16, cd: 2100, mult: 2.45, range: 105, aoe: 55, color: "#ef4444" },
        { id: "rageful-blow", name: "Rageful Blow", mp: 24, cd: 3600, mult: 2.95, range: 100, aoe: 140, color: "#f59e0b" }
      ]
    },
    dw: {
      id: "dw", name: "Dark Wizard", evo150: "Soul Master", evo400: "Grand Master",
      role: "Daño mágico masivo · 5 pts/nv", color: "#3b82f6", ptsPerLevel: 5, unlockLevel: 1,
      canHelm: true, baseStats: { str: 14, agi: 18, vit: 16, ene: 35 }, baseHp: 155, baseMp: 190, range: 190,
      starterWeapon: { templateId: "skull-staff", name: "Skull Staff", slot: "weapon", rarity: "normal", plus: 0, attack: 10, defense: 0, magic: 22, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "dw", color: "#3b82f6", valueZen: 1400, quantity: 1 },
      starterArmor: { templateId: "pad-armor", name: "Pad Armor", slot: "armor", rarity: "normal", plus: 0, attack: 0, defense: 9, magic: 0, hpBonus: 15, speedBonus: 0, wingsTier: 0, classReq: "dw", color: "#2563eb", valueZen: 950, quantity: 1 },
      skills: [
        { id: "evil-spirit", name: "Evil Spirit", mp: 18, cd: 1400, mult: 1.75, range: 210, aoe: 150, color: "#6366f1" },
        { id: "meteorite", name: "Meteorite", mp: 20, cd: 2000, mult: 2.45, range: 225, aoe: 75, color: "#f59e0b" },
        { id: "ice-storm", name: "Ice Storm", mp: 28, cd: 3500, mult: 2.75, range: 215, aoe: 160, color: "#38bdf8" }
      ]
    },
    elf: {
      id: "elf", name: "Fairy Elf", evo150: "Muse Elf", evo400: "High Elf",
      role: "Arco y curación · 5 pts/nv", color: "#10b981", ptsPerLevel: 5, unlockLevel: 1,
      canHelm: true, baseStats: { str: 18, agi: 32, vit: 18, ene: 15 }, baseHp: 175, baseMp: 140, range: 205,
      starterWeapon: { templateId: "elven-bow", name: "Elven Bow", slot: "weapon", rarity: "normal", plus: 0, attack: 19, defense: 0, magic: 0, hpBonus: 0, speedBonus: 10, wingsTier: 0, classReq: "elf", color: "#10b981", valueZen: 1350, quantity: 1 },
      starterArmor: { templateId: "vine-armor", name: "Vine Armor", slot: "armor", rarity: "normal", plus: 0, attack: 0, defense: 10, magic: 0, hpBonus: 20, speedBonus: 0, wingsTier: 0, classReq: "elf", color: "#059669", valueZen: 1000, quantity: 1 },
      skills: [
        { id: "triple-shot", name: "Triple Shot", mp: 10, cd: 1100, mult: 1.75, range: 225, aoe: 90, color: "#10b981" },
        { id: "ice-arrow", name: "Ice Arrow", mp: 18, cd: 2100, mult: 2.50, range: 235, aoe: 55, color: "#38bdf8" },
        { id: "heal-aura", name: "Heal Aura", mp: 22, cd: 3800, mult: 0, range: 0, aoe: 120, color: "#34d399" }
      ]
    },
    mg: {
      id: "mg", name: "Magic Gladiator", evo150: "Duel Master", evo400: "Magic Knight",
      role: "Híbrido DK+DW · 7 pts/nv · Sin casco · Req Nv.220+", color: "#a855f7", ptsPerLevel: 7, unlockLevel: 220,
      canHelm: false, baseStats: { str: 26, agi: 26, vit: 24, ene: 26 }, baseHp: 210, baseMp: 155, range: 138,
      starterWeapon: { templateId: "rune-blade", name: "Rune Bastard Sword", slot: "weapon", rarity: "excellent", plus: 0, attack: 24, defense: 0, magic: 20, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "mg", color: "#a855f7", valueZen: 2800, quantity: 1 },
      starterArmor: { templateId: "stormcrow-armor", name: "Storm Crow Armor", slot: "armor", rarity: "normal", plus: 0, attack: 0, defense: 16, magic: 0, hpBonus: 40, speedBonus: 0, wingsTier: 0, classReq: "mg", color: "#9333ea", valueZen: 2200, quantity: 1 },
      skills: [
        { id: "power-slash", name: "Power Slash", mp: 16, cd: 1350, mult: 2.05, range: 155, aoe: 105, color: "#a855f7" },
        { id: "fire-slash", name: "Fire Slash", mp: 20, cd: 2100, mult: 2.55, range: 130, aoe: 80, color: "#f97316" },
        { id: "flame-strike", name: "Flame Strike", mp: 28, cd: 3400, mult: 3.05, range: 165, aoe: 145, color: "#ef4444" }
      ]
    },
    dl: {
      id: "dl", name: "Dark Lord", evo150: "Lord Emperor", evo400: "Empire Lord",
      role: "Físico/Mágico + Cuervo · 7 pts/nv · Req Nv.300+", color: "#f59e0b", ptsPerLevel: 7, unlockLevel: 300,
      canHelm: true, baseStats: { str: 26, agi: 22, vit: 22, ene: 28 }, baseHp: 215, baseMp: 160, range: 168,
      starterWeapon: { templateId: "battle-scepter", name: "Battle Scepter", slot: "weapon", rarity: "excellent", plus: 0, attack: 25, defense: 0, magic: 24, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "dl", color: "#f59e0b", valueZen: 3200, quantity: 1 },
      starterArmor: { templateId: "lightplate-armor", name: "Light Plate Armor", slot: "armor", rarity: "normal", plus: 0, attack: 0, defense: 18, magic: 0, hpBonus: 45, speedBonus: 0, wingsTier: 0, classReq: "dl", color: "#d97706", valueZen: 2600, quantity: 1 },
      skills: [
        { id: "fire-burst", name: "Fire Burst", mp: 16, cd: 1350, mult: 2.0, range: 185, aoe: 95, color: "#f59e0b" },
        { id: "chaotic-diseier", name: "Chaotic Diseier", mp: 24, cd: 2300, mult: 2.6, range: 195, aoe: 140, color: "#ec4899" },
        { id: "dark-raven", name: "Dark Raven", mp: 20, cd: 3100, mult: 2.85, range: 220, aoe: 85, color: "#38bdf8" }
      ]
    }
  };

  const DEFAULT_MAPS = [
    { id: "lorencia", name: "Lorencia", minLevel: 1, warpLevel: 10, expRate: 0.35, dropZen: 1.0, dropItems: 0.40, g1: "#1a2419", g2: "#222f20", accent: "#d97706", subtitle: "Ciudadela central · Arañas y Dragón Rojo" },
    { id: "noria", name: "Noria", minLevel: 1, warpLevel: 10, expRate: 0.35, dropZen: 1.0, dropItems: 0.40, g1: "#132721", g2: "#18342c", accent: "#10b981", subtitle: "Bosque élfico · Chaos Goblin" },
    { id: "devias", name: "Devias", minLevel: 15, warpLevel: 50, expRate: 0.35, dropZen: 1.15, dropItems: 0.42, g1: "#182433", g2: "#203247", accent: "#38bdf8", subtitle: "Bastión nevado · Yetis y Golden Archer" },
    { id: "dungeon1", name: "Dungeon 1", minLevel: 20, warpLevel: 20, expRate: 0.35, dropZen: 1.2, dropItems: 0.42, g1: "#15141e", g2: "#1e1b2b", accent: "#a855f7", subtitle: "Catacumbas Nv.1 · Esqueletos y Larvas" },
    { id: "dungeon2", name: "Dungeon 2", minLevel: 40, warpLevel: 50, expRate: 0.35, dropZen: 1.35, dropItems: 0.44, g1: "#171422", g2: "#221d33", accent: "#9333ea", subtitle: "Catacumbas Nv.2 · Cíclopes y Fantasmas" },
    { id: "dungeon3", name: "Dungeon 3", minLevel: 60, warpLevel: 60, expRate: 0.35, dropZen: 1.5, dropItems: 0.48, g1: "#1b1224", g2: "#271834", accent: "#c084fc", subtitle: "Cámara profunda · Gorgon" },
    { id: "atlans", name: "Atlans", minLevel: 60, warpLevel: 70, expRate: 0.35, dropZen: 1.7, dropItems: 0.50, g1: "#0f2233", g2: "#152f47", accent: "#0ea5e9", subtitle: "Reino submarino · Bahamut e Hydra" },
    { id: "losttower", name: "Lost Tower 1-7", minLevel: 80, warpLevel: 80, expRate: 0.35, dropZen: 2.0, dropItems: 0.52, g1: "#211520", g2: "#2d1c2b", accent: "#ec4899", subtitle: "Torre maldita · Death Knight y Balrog" },
    { id: "kalima", name: "Kalima", minLevel: 70, warpLevel: 70, expRate: 0.38, dropZen: 2.2, dropItems: 0.56, g1: "#122126", g2: "#192f36", accent: "#14b8a6", subtitle: "Templo sumergido · Kundun" },
    { id: "tarkan", name: "Tarkan", minLevel: 130, warpLevel: 140, expRate: 0.35, dropZen: 2.6, dropItems: 0.58, g1: "#2b1e14", g2: "#38271a", accent: "#f97316", subtitle: "Desierto abrasador · Zaikan" },
    { id: "icarus", name: "Icarus", minLevel: 160, warpLevel: 170, expRate: 0.35, dropZen: 3.0, dropItems: 0.60, g1: "#18233a", g2: "#202f4f", accent: "#60a5fa", subtitle: "Cielos flotantes (requiere Alas)" },
    { id: "kanturu", name: "Kanturu Ruins", minLevel: 170, warpLevel: 170, expRate: 0.35, dropZen: 3.4, dropItems: 0.62, g1: "#1e1f26", g2: "#282a33", accent: "#94a3b8", subtitle: "Ruinas antiguas · Nightmare" },
    { id: "aida", name: "Aida", minLevel: 150, warpLevel: 150, expRate: 0.35, dropZen: 2.9, dropItems: 0.60, g1: "#15261a", g2: "#1e3625", accent: "#4ade80", subtitle: "Selva mutante · Hell Maine" },
    { id: "crywolf", name: "CryWolf", minLevel: 180, warpLevel: 180, expRate: 0.35, dropZen: 3.6, dropItems: 0.65, g1: "#221d1a", g2: "#2e2723", accent: "#fbbf24", subtitle: "Fortaleza fronteriza · Balgass" },
    { id: "stadium", name: "Stadium", minLevel: 255, warpLevel: 255, expRate: 0.45, dropZen: 4.2, dropItems: 0.70, g1: "#211618", g2: "#2e1e21", accent: "#f43f5e", subtitle: "Arena de entrenamiento · Solo Warp" },
    { id: "custom", name: "Arena Mod", minLevel: 1, warpLevel: 1, expRate: 1.0, dropZen: 3.0, dropItems: 0.80, g1: "#241418", g2: "#301b21", accent: "#fb7185", subtitle: "Zona personalizada por Mods" }
  ];

  const DEFAULT_NPCS = [
    { id: "smith", mapId: "lorencia", name: "Hanzo, Blacksmith", role: "shop", subtitle: "Armas DK/MG", wx: 420, wy: 430 },
    { id: "armor-lorencia", mapId: "lorencia", name: "Lumen, Armaduras", role: "shop", subtitle: "Sets Lorencia", wx: 480, wy: 405 },
    { id: "wizard", mapId: "lorencia", name: "Pasi Wizard", role: "shop", subtitle: "Bastones y Magia", wx: 540, wy: 430 },
    { id: "barmaid", mapId: "lorencia", name: "Liaman Barmaid", role: "shop", subtitle: "Pociones", wx: 420, wy: 530 },
    { id: "vault-lorencia", mapId: "lorencia", name: "Guardia del Baúl", role: "vault", subtitle: "Almacén Seguro", wx: 540, wy: 530 },
    { id: "marlon", mapId: "lorencia", name: "Marlon (Evolución)", role: "quest", subtitle: "Quest Nv.150/400", wx: 480, wy: 555 },
    { id: "chaos", mapId: "noria", name: "Chaos Goblin", role: "chaos", subtitle: "Alas S1/S2/S3 y +13", wx: 535, wy: 455 },
    { id: "elf-merchant", mapId: "noria", name: "Eo the Craftsman", role: "shop", subtitle: "Arcos Elfa", wx: 430, wy: 455 },
    { id: "golden-archer", mapId: "devias", name: "Golden Archer", role: "archer", subtitle: "3 Talismanes -> Alas S2", wx: 535, wy: 455 },
    { id: "devias-potion", mapId: "devias", name: "Zienna, Mercader", role: "shop", subtitle: "Tienda Devias", wx: 430, wy: 455 }
  ];

  const MONSTERS = [
    { id: "spider", name: "Spider", mapId: "lorencia", level: 2, maxHp: 70, atk: 8, def: 2, exp: 32, zen: 130, speed: 56, r: 15, color: "#64748b", boss: false },
    { id: "budge-dragon", name: "Budge Dragon", mapId: "lorencia", level: 4, maxHp: 100, atk: 11, def: 3, exp: 44, zen: 180, speed: 58, r: 16, color: "#84cc16", boss: false },
    { id: "bull-fighter", name: "Bull Fighter", mapId: "lorencia", level: 8, maxHp: 175, atk: 16, def: 6, exp: 72, zen: 260, speed: 62, r: 18, color: "#dc2626", boss: false },
    { id: "red-dragon", name: "Red Dragon", mapId: "lorencia", level: 24, maxHp: 1200, atk: 48, def: 18, exp: 640, zen: 2600, speed: 68, r: 28, color: "#ef4444", boss: true },
    { id: "goblin", name: "Goblin", mapId: "noria", level: 3, maxHp: 85, atk: 9, def: 3, exp: 36, zen: 150, speed: 60, r: 15, color: "#4ade80", boss: false },
    { id: "stone-golem", name: "Stone Golem", mapId: "noria", level: 16, maxHp: 360, atk: 28, def: 12, exp: 175, zen: 580, speed: 52, r: 22, color: "#64748b", boss: false },
    { id: "yeti", name: "Elite Yeti", mapId: "devias", level: 32, maxHp: 820, atk: 56, def: 22, exp: 410, zen: 1400, speed: 68, r: 22, color: "#e0f2fe", boss: false },
    { id: "ice-queen", name: "Ice Queen", mapId: "devias", level: 46, maxHp: 2400, atk: 86, def: 30, exp: 1400, zen: 5200, speed: 72, r: 30, color: "#0ea5e9", boss: true },
    { id: "larva", name: "Skeleton & Larva", mapId: "dungeon1", level: 24, maxHp: 520, atk: 40, def: 15, exp: 250, zen: 820, speed: 62, r: 18, color: "#a855f7", boss: false },
    { id: "cyclops", name: "Cyclops", mapId: "dungeon2", level: 42, maxHp: 980, atk: 66, def: 24, exp: 520, zen: 1700, speed: 62, r: 22, color: "#9333ea", boss: false },
    { id: "gorgon", name: "Gorgon", mapId: "dungeon3", level: 72, maxHp: 4200, atk: 128, def: 42, exp: 2400, zen: 7800, speed: 68, r: 30, color: "#c084fc", boss: true },
    { id: "bahamut", name: "Bahamut", mapId: "atlans", level: 62, maxHp: 1580, atk: 92, def: 32, exp: 880, zen: 2700, speed: 70, r: 19, color: "#38bdf8", boss: false },
    { id: "hydra", name: "Hydra", mapId: "atlans", level: 82, maxHp: 5200, atk: 145, def: 48, exp: 3100, zen: 9800, speed: 68, r: 32, color: "#0ea5e9", boss: true },
    { id: "balrog", name: "Balrog", mapId: "losttower", level: 115, maxHp: 7600, atk: 195, def: 62, exp: 4600, zen: 14000, speed: 70, r: 32, color: "#ef4444", boss: true },
    { id: "kundun", name: "Illusion of Kundun", mapId: "kalima", level: 110, maxHp: 8200, atk: 205, def: 65, exp: 5200, zen: 16500, speed: 72, r: 34, color: "#2dd4bf", boss: true },
    { id: "zaikan", name: "Zaikan", mapId: "tarkan", level: 160, maxHp: 11500, atk: 260, def: 85, exp: 7400, zen: 22000, speed: 74, r: 34, color: "#ea580c", boss: true },
    { id: "phoenix", name: "Phoenix of Darkness", mapId: "icarus", level: 185, maxHp: 14500, atk: 305, def: 96, exp: 9200, zen: 28000, speed: 76, r: 34, color: "#3b82f6", boss: true },
    { id: "nightmare", name: "Nightmare", mapId: "kanturu", level: 205, maxHp: 17500, atk: 340, def: 108, exp: 11800, zen: 34000, speed: 74, r: 34, color: "#6366f1", boss: true },
    { id: "hell-maine", name: "Hell Maine", mapId: "aida", level: 178, maxHp: 13800, atk: 290, def: 92, exp: 8600, zen: 25000, speed: 72, r: 33, color: "#16a34a", boss: true },
    { id: "balgass", name: "Balgass", mapId: "crywolf", level: 220, maxHp: 21000, atk: 380, def: 118, exp: 14000, zen: 42000, speed: 76, r: 36, color: "#d97706", boss: true },
    { id: "arena-lord", name: "Kundun Avatar", mapId: "stadium", level: 290, maxHp: 28000, atk: 450, def: 138, exp: 19500, zen: 56000, speed: 78, r: 36, color: "#e11d48", boss: true }
  ];

  const ITEM_POOL = [
    { templateId: "hp-potion", name: "Large Healing Potion", slot: "potion", potionType: "hp", rarity: "normal", plus: 0, attack: 0, defense: 0, magic: 0, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "all", color: "#ef4444", valueZen: 150 },
    { templateId: "mp-potion", name: "Large Mana Potion", slot: "potion", potionType: "mp", rarity: "normal", plus: 0, attack: 0, defense: 0, magic: 0, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "all", color: "#3b82f6", valueZen: 150 },
    { templateId: "jewel-bless", name: "Jewel of Bless", slot: "jewel", jewelType: "bless", rarity: "excellent", plus: 0, attack: 0, defense: 0, magic: 0, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "all", color: "#a855f7", valueZen: 4500 },
    { templateId: "jewel-soul", name: "Jewel of Soul", slot: "jewel", jewelType: "soul", rarity: "excellent", plus: 0, attack: 0, defense: 0, magic: 0, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "all", color: "#ec4899", valueZen: 6000 },
    { templateId: "jewel-chaos", name: "Jewel of Chaos", slot: "jewel", jewelType: "chaos", rarity: "mythic", plus: 0, attack: 0, defense: 0, magic: 0, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "all", color: "#f59e0b", valueZen: 8500 },
    { templateId: "jewel-life", name: "Jewel of Life", slot: "jewel", jewelType: "life", rarity: "excellent", plus: 0, attack: 0, defense: 0, magic: 0, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "all", color: "#34d399", valueZen: 9500 },
    { templateId: "jewel-creation", name: "Jewel of Creation", slot: "jewel", jewelType: "creation", rarity: "mythic", plus: 0, attack: 0, defense: 0, magic: 0, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "all", color: "#fbbf24", valueZen: 12000 },
    { templateId: "rena-talisman", name: "Talisman / Rena", slot: "talisman", rarity: "excellent", plus: 0, attack: 0, defense: 0, magic: 0, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "all", color: "#fde047", valueZen: 2500 },
    { templateId: "blade-of-archangel", name: "Archangel Sword", slot: "weapon", rarity: "excellent", plus: 3, attack: 46, defense: 0, magic: 0, hpBonus: 0, speedBonus: 12, wingsTier: 0, classReq: "dk", color: "#f59e0b", valueZen: 9500 },
    { templateId: "dragon-armor", name: "Dragon Armor", slot: "armor", rarity: "excellent", plus: 2, attack: 0, defense: 34, magic: 0, hpBonus: 110, speedBonus: 0, wingsTier: 0, classReq: "dk", color: "#ef4444", valueZen: 7800 },
    { templateId: "dragon-helm", name: "Dragon Helm", slot: "helm", rarity: "normal", plus: 1, attack: 0, defense: 20, magic: 0, hpBonus: 55, speedBonus: 0, wingsTier: 0, classReq: "dk", color: "#dc2626", valueZen: 4200 },
    { templateId: "viper-staff", name: "Grand Viper Staff", slot: "weapon", rarity: "excellent", plus: 3, attack: 24, defense: 0, magic: 52, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "dw", color: "#6366f1", valueZen: 9800 },
    { templateId: "viper-bow", name: "Sylph Wind Bow", slot: "weapon", rarity: "excellent", plus: 3, attack: 48, defense: 0, magic: 0, hpBonus: 0, speedBonus: 18, wingsTier: 0, classReq: "elf", color: "#10b981", valueZen: 9400 },
    { templateId: "dark-reign-blade", name: "Dark Reign Blade", slot: "weapon", rarity: "excellent", plus: 3, attack: 54, defense: 0, magic: 44, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "mg", color: "#a855f7", valueZen: 12500 },
    { templateId: "soleil-scepter", name: "Soleil Scepter", slot: "weapon", rarity: "excellent", plus: 3, attack: 50, defense: 0, magic: 48, hpBonus: 0, speedBonus: 0, wingsTier: 0, classReq: "dl", color: "#f59e0b", valueZen: 12800 },
    { templateId: "dark-raven", name: "Dark Raven (Mascota DL)", slot: "pet", rarity: "ancient", plus: 1, attack: 28, defense: 0, magic: 0, hpBonus: 0, speedBonus: 10, wingsTier: 0, classReq: "dl", color: "#38bdf8", valueZen: 15000 },
    { templateId: "dark-horse", name: "Dark Horse (Mascota DL)", slot: "pet", rarity: "mythic", plus: 1, attack: 0, defense: 32, magic: 0, hpBonus: 150, speedBonus: 24, wingsTier: 0, classReq: "dl", color: "#f59e0b", valueZen: 22000 },
    { templateId: "plate-shield", name: "Dragon Slayer Shield", slot: "offhand", rarity: "normal", plus: 2, attack: 0, defense: 24, magic: 0, hpBonus: 45, speedBonus: 0, wingsTier: 0, classReq: "all", color: "#94a3b8", valueZen: 3900 }
  ];

  const BUILTIN_MODS = [
    { id: "mod-fast-x50", name: "Fast Server x50", author: "MU Community", description: "EXP x12, Zen x8, Drop x1.6 y Chaos +15%.", enabled: true, expMultiplier: 12, zenMultiplier: 8, dropRateMultiplier: 1.6, chaosSuccessBonus: 15, resetLevel: 120 },
    { id: "mod-conqueror-wings", name: "Wings S3 & Godlike Pack", author: "Kundun Forge", description: "Añade multiplicador +50% EXP y Chaos +10%.", enabled: true, expMultiplier: 1.5, zenMultiplier: 1.5, dropRateMultiplier: 1.3, chaosSuccessBonus: 10, resetLevel: 400 },
    { id: "mod-classic-x1", name: "Classic Hardcore 35%", author: "Webzen Oldschool", description: "Modo clásico lento 0.25x para farmeo puro.", enabled: false, expMultiplier: 0.25, zenMultiplier: 0.3, dropRateMultiplier: 0.7, chaosSuccessBonus: -5, resetLevel: 400 }
  ];

  // --- JSON Persistence Helpers (Zero SQL) ---
  const loadJson = (key, fallback) => {
    try { const raw = localStorage.getItem(key); return raw ? JSON.parse(raw) : fallback; } catch { return fallback; }
  };
  const saveJson = (key, val) => localStorage.setItem(key, JSON.stringify(val));
  const uid = (p = "id") => `${p}-${Math.random().toString(36).slice(2, 10)}`;
  const clone = (o) => JSON.parse(JSON.stringify(o));

  // --- WebAudio Synth ---
  let audioCtx = null;
  const playTone = (freq, dur = 0.1, type = "triangle") => {
    if (state.settings.sfxVolume <= 0.02) return;
    try {
      audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = type;
      osc.frequency.value = freq;
      gain.gain.value = state.settings.sfxVolume * 0.14;
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + dur);
      osc.connect(gain); gain.connect(audioCtx.destination);
      osc.start(); osc.stop(audioCtx.currentTime + dur);
    } catch {}
  };

  // --- Global State ---
  const state = {
    accountId: localStorage.getItem(STORAGE_KEYS.account) || "lorencia",
    selectedClass: "dk",
    heroes: [],
    vault: [],
    mods: loadJson(STORAGE_KEYS.mods, BUILTIN_MODS),
    maps: loadJson(STORAGE_KEYS.maps, DEFAULT_MAPS),
    npcs: loadJson(STORAGE_KEYS.npcs, DEFAULT_NPCS),
    settings: loadJson(STORAGE_KEYS.settings, { sfxVolume: 0.75, brightness: 1.0, hdGraphics: true, animations: true, manifestUrl: "" }),
    manifest: loadJson(STORAGE_KEYS.manifest, { version: "6.3.1", notes: "Configuración inicial", updatedAt: "2026-11-16" }),
    activeHero: null,
    monsters: [],
    drops: [],
    vfx: [],
    floats: [],
    cooldowns: {},
    autoHunt: false,
    facing: 0,
    keys: {},
    pad: { x: 0, y: 0 },
    lastAttackAt: 0
  };

  const getActiveModRates = () => {
    const en = state.mods.filter(m => m.enabled);
    return en.reduce((acc, m) => ({
      expMul: Math.min(250, acc.expMul * m.expMultiplier),
      zenMul: Math.min(250, acc.zenMul * m.zenMultiplier),
      dropMul: Math.min(10, acc.dropMul * m.dropRateMultiplier),
      chaosBonus: acc.chaosBonus + (m.chaosSuccessBonus || 0),
      resetLevel: Math.min(acc.resetLevel, m.resetLevel || 400)
    }), { expMul: 1, zenMul: 1, dropMul: 1, chaosBonus: 0, resetLevel: 400 });
  };

  const computeDerived = (h) => {
    const cls = CLASSES[h.classId];
    let atk = 0, mag = 0, def = 0, hpB = 0, spd = 0;
    Object.values(h.equipment || {}).forEach(it => {
      if (!it) return;
      const mult = 1 + (it.plus || 0) * 0.14;
      atk += Math.round((it.attack || 0) * mult);
      mag += Math.round((it.magic || 0) * mult);
      def += Math.round((it.defense || 0) * mult);
      hpB += (it.hpBonus || 0);
      spd += (it.speedBonus || 0);
    });
    const evoMult = 1 + (h.evolutionTier || 0) * 0.15;
    let rawAtk = h.stats.str * 1.3 + h.stats.agi * 0.35 + atk + h.level * 1.5;
    if (h.classId === "dw") rawAtk = h.stats.ene * 1.35 + mag + atk * 0.4 + h.level * 1.4;
    if (h.classId === "elf") rawAtk = h.stats.agi * 1.25 + h.stats.str * 0.45 + atk + h.level * 1.4;
    if (h.classId === "mg") rawAtk = h.stats.str + h.stats.ene * 0.95 + atk + mag * 0.8 + h.level * 1.55;
    if (h.classId === "dl") rawAtk = h.stats.str * 1.05 + h.stats.ene * 1.05 + atk + mag * 0.75 + h.level * 1.6;
    return {
      attackPower: Math.round(rawAtk * evoMult),
      defense: Math.round((h.stats.agi * 0.45 + h.stats.vit * 0.3 + def + h.level * 0.9) * evoMult),
      maxHp: cls.baseHp + h.stats.vit * 6 + h.level * 14 + hpB + (h.evolutionTier || 0) * 120,
      maxMp: cls.baseMp + h.stats.ene * 4 + h.level * 8 + (h.evolutionTier || 0) * 60,
      critChance: Math.min(0.48, 0.08 + h.stats.agi * 0.0012 + (h.equipment.wings ? 0.06 : 0)),
      moveSpeed: Math.min(285, 165 + spd + Math.floor(h.stats.agi * 0.25))
    };
  };

  const createStarterHero = (name, classId) => {
    const cls = CLASSES[classId];
    return {
      id: uid("hero"),
      accountId: state.accountId,
      name: (name || "").trim() || cls.name,
      classId,
      evolutionTier: 0,
      level: 1,
      resets: 0,
      exp: 0,
      expToNext: 120,
      zen: 5000,
      hp: cls.baseHp,
      maxHp: cls.baseHp,
      mp: cls.baseMp,
      maxMp: cls.baseMp,
      statPoints: 10,
      stats: { ...cls.baseStats },
      mapId: "lorencia",
      x: SAFE_CENTER,
      y: SAFE_CENTER,
      equipment: {
        weapon: { ...clone(cls.starterWeapon), id: uid("eq") },
        armor: { ...clone(cls.starterArmor), id: uid("eq") },
        offhand: null, helm: null, wings: null, pendant: null, pet: null
      },
      inventory: [
        { ...clone(ITEM_POOL[0]), id: uid("it"), quantity: 8 },
        { ...clone(ITEM_POOL[1]), id: uid("it"), quantity: 6 },
        { ...clone(ITEM_POOL[2]), id: uid("it"), quantity: 2 },
        { ...clone(ITEM_POOL[4]), id: uid("it"), quantity: 1 }
      ],
      kills: 0
    };
  };

  const saveCurrentAccountData = () => {
    if (state.activeHero) {
      state.heroes = [state.activeHero, ...state.heroes.filter(h => h.id !== state.activeHero.id)];
    }
    saveJson(STORAGE_KEYS.heroesPrefix + state.accountId, state.heroes);
    saveJson(STORAGE_KEYS.vaultPrefix + state.accountId, state.vault);
  };

  const loadAccount = (acc) => {
    state.accountId = (acc || "lorencia").trim().toLowerCase() || "lorencia";
    localStorage.setItem(STORAGE_KEYS.account, state.accountId);
    state.heroes = loadJson(STORAGE_KEYS.heroesPrefix + state.accountId, []);
    state.vault = loadJson(STORAGE_KEYS.vaultPrefix + state.accountId, []);
    if (state.heroes.length === 0) {
      state.heroes = [createStarterHero("Guerrero", "dk")];
      saveJson(STORAGE_KEYS.heroesPrefix + state.accountId, state.heroes);
    }
    renderTitleScreen();
  };

  // --- Spawn Monsters ---
  const spawnMapMonsters = (mapId) => {
    let tpls = MONSTERS.filter(m => m.mapId === mapId);
    if (tpls.length === 0) tpls = MONSTERS.filter(m => m.mapId === "lorencia");
    const list = [];
    tpls.forEach(t => {
      const count = t.boss ? 1 : 4;
      for (let i = 0; i < count; i++) {
        let mx = 220, my = 220;
        for (let k = 0; k < 20; k++) {
          const cx = 120 + Math.random() * (WORLD_SIZE - 240);
          const cy = 120 + Math.random() * (WORLD_SIZE - 240);
          mx = cx; my = cy;
          if (Math.hypot(cx - SAFE_CENTER, cy - SAFE_CENTER) > SAFE_RADIUS + 110) break;
        }
        list.push({ ...clone(t), uid: uid("mob"), hp: t.maxHp, x: mx, y: my, homeX: mx, homeY: my, respawnAt: 0, flashUntil: 0, atkCd: 0 });
      }
    });
    state.monsters = list;
  };

  // --- Title Screen Rendering ---
  const elTitle = document.getElementById("title-screen");
  const elGame = document.getElementById("game-screen");
  const elModalBackdrop = document.getElementById("modal-backdrop");
  const elModalTitle = document.getElementById("modal-title");
  const elModalBody = document.getElementById("modal-body");

  const openModal = (title, buildFn) => {
    elModalTitle.textContent = title;
    elModalBody.innerHTML = "";
    buildFn(elModalBody);
    elModalBackdrop.classList.remove("hidden");
  };
  const closeModal = () => elModalBackdrop.classList.add("hidden");
  document.getElementById("modal-close").onclick = closeModal;
  elModalBackdrop.onclick = (e) => { if (e.target === elModalBackdrop) closeModal(); };

  const renderTitleScreen = () => {
    document.getElementById("account-badge").textContent = `Cuenta: ${state.accountId}`;
    document.getElementById("input-account").value = state.accountId;
    document.getElementById("slots-count").textContent = `${state.heroes.length}/5 slots ocupados`;
    const maxLv = state.heroes.reduce((mx, h) => Math.max(mx, h.level), 1);

    const slotsEl = document.getElementById("hero-slots");
    slotsEl.innerHTML = "";
    for (let i = 0; i < 5; i++) {
      const h = state.heroes[i];
      const div = document.createElement("div");
      div.className = "card";
      if (h) {
        const cls = CLASSES[h.classId];
        const title = h.evolutionTier >= 2 ? cls.evo400 : h.evolutionTier === 1 ? cls.evo150 : cls.name;
        div.style.borderColor = cls.color;
        div.innerHTML = `
          <div style="font-weight:700;color:${cls.color}">${h.name}</div>
          <div class="muted">${title} · Nv.${h.level}</div>
          <div class="muted">${h.zen} Zen · ${h.kills} bajas</div>
          <div class="row" style="margin-top:8px;">
            <button class="gold sm" style="flex:1;">Entrar</button>
            <button class="crimson sm">X</button>
          </div>`;
        const [btnEnter, btnDel] = div.querySelectorAll("button");
        btnEnter.onclick = () => enterWorld(h);
        btnDel.onclick = () => {
          state.heroes = state.heroes.filter(x => x.id !== h.id);
          saveJson(STORAGE_KEYS.heroesPrefix + state.accountId, state.heroes);
          renderTitleScreen();
        };
      } else {
        div.innerHTML = `<div class="muted">Slot ${i + 1} vacío</div>`;
      }
      slotsEl.appendChild(div);
    }

    const picker = document.getElementById("class-picker");
    picker.innerHTML = "";
    Object.values(CLASSES).forEach(cls => {
      const unlocked = maxLv >= cls.unlockLevel;
      const card = document.createElement("div");
      card.className = `class-card ${state.selectedClass === cls.id ? "selected" : ""}`;
      card.innerHTML = `
        <div style="font-weight:700;color:${cls.color}">${cls.name}</div>
        <div style="font-size:11px;color:#fde68a;">${cls.evo150} → ${cls.evo400}</div>
        <div class="muted" style="font-size:11px;">${cls.role}</div>
        ${unlocked ? "" : `<span class="badge" style="background:rgba(239,68,68,.2);color:#ef4444;border-color:#ef4444;">Requiere Nv.${cls.unlockLevel}+</span>`}
      `;
      card.onclick = () => { state.selectedClass = cls.id; renderTitleScreen(); };
      picker.appendChild(card);
    });
  };

  document.getElementById("btn-switch-account").onclick = () => loadAccount(document.getElementById("input-account").value);
  document.getElementById("btn-create-hero").onclick = () => {
    const errEl = document.getElementById("create-error");
    errEl.textContent = "";
    if (state.heroes.length >= 5) { errEl.textContent = "Límite de 5 slots alcanzado."; return; }
    const maxLv = state.heroes.reduce((mx, h) => Math.max(mx, h.level), 1);
    const cls = CLASSES[state.selectedClass];
    if (maxLv < cls.unlockLevel) { errEl.textContent = `Necesitas un personaje nivel ${cls.unlockLevel}+ en la cuenta.`; return; }
    const hero = createStarterHero(document.getElementById("input-hero-name").value, state.selectedClass);
    state.heroes.unshift(hero);
    saveJson(STORAGE_KEYS.heroesPrefix + state.accountId, state.heroes);
    document.getElementById("input-hero-name").value = "";
    enterWorld(hero);
  };

  // --- Enter / Exit World ---
  const populateMapSelect = () => {
    const sel = document.getElementById("hud-map-select");
    sel.innerHTML = "";
    state.maps.forEach(m => {
      const opt = document.createElement("option");
      opt.value = m.id;
      opt.textContent = `${m.name} (Nv.${m.minLevel}+)`;
      if (state.activeHero && state.activeHero.mapId === m.id) opt.selected = true;
      sel.appendChild(opt);
    });
  };

  const enterWorld = (hero) => {
    state.activeHero = clone(hero);
    const der = computeDerived(state.activeHero);
    state.activeHero.maxHp = der.maxHp;
    state.activeHero.maxMp = der.maxMp;
    state.activeHero.hp = Math.min(der.maxHp, state.activeHero.hp || der.maxHp);
    state.activeHero.mp = Math.min(der.maxMp, state.activeHero.mp || der.maxMp);
    state.drops = [];
    state.vfx = [];
    state.floats = [];
    spawnMapMonsters(state.activeHero.mapId);
    elTitle.classList.add("hidden");
    elGame.classList.remove("hidden");
    populateMapSelect();
    refreshHud();
  };

  document.getElementById("hud-exit").onclick = () => {
    saveCurrentAccountData();
    state.activeHero = null;
    state.autoHunt = false;
    elGame.classList.add("hidden");
    elTitle.classList.remove("hidden");
    renderTitleScreen();
  };

  document.getElementById("hud-map-select").onchange = (e) => {
    const h = state.activeHero;
    if (!h) return;
    const target = state.maps.find(m => m.id === e.target.value);
    if (!target) return;
    if (h.level < target.minLevel) {
      pushFloat(h.x, h.y - 26, `${target.name} requiere Nv.${target.minLevel}`, "#ef4444");
      e.target.value = h.mapId;
      return;
    }
    if (target.id === "icarus" && !h.equipment.wings) {
      pushFloat(h.x, h.y - 26, "Icarus requiere Alas equipadas", "#38bdf8");
      e.target.value = h.mapId;
      return;
    }
    h.mapId = target.id;
    h.x = SAFE_CENTER; h.y = SAFE_CENTER;
    state.drops = [];
    spawnMapMonsters(target.id);
    saveCurrentAccountData();
    refreshHud();
  };

  // --- Combat & Rewards ---
  const pushFloat = (x, y, text, color = "#fbbf24", crit = false) => {
    state.floats.push({ x, y, text, color, crit, at: Date.now() });
    if (state.floats.length > 26) state.floats.shift();
  };

  const addItem = (h, item) => {
    if (["potion", "jewel", "talisman"].includes(item.slot)) {
      const ex = h.inventory.find(i => i.templateId === item.templateId);
      if (ex) { ex.quantity += (item.quantity || 1); return; }
    }
    if (h.inventory.length < 24) h.inventory.push(item);
  };

  const consumeJewel = (h, jewelType) => {
    const idx = h.inventory.findIndex(i => i.jewelType === jewelType);
    if (idx < 0) return false;
    if (h.inventory[idx].quantity > 1) h.inventory[idx].quantity--;
    else h.inventory.splice(idx, 1);
    return true;
  };

  const performCombat = (skillIdx = null) => {
    const h = state.activeHero;
    if (!h) return;
    const now = Date.now();
    const cls = CLASSES[h.classId];
    const der = computeDerived(h);
    let mult = 1, range = cls.range, aoe = 45, mpCost = 0, color = cls.color, sid = "basic";

    if (skillIdx !== null) {
      const sk = cls.skills[skillIdx];
      if (!sk || (state.cooldowns[sk.id] || 0) > now) return;
      if (h.mp < sk.mp) { pushFloat(h.x, h.y - 24, "Sin Mana", "#60a5fa"); return; }
      mpCost = sk.mp; mult = sk.mult; range = Math.max(range, sk.range); aoe = sk.aoe; color = sk.color; sid = sk.id;
      state.cooldowns[sk.id] = now + sk.cd;
      playTone(520, 0.14, "sawtooth");
      if (sk.id === "heal-aura") {
        const heal = Math.round(der.maxHp * 0.45);
        h.mp -= mpCost; h.hp = Math.min(der.maxHp, h.hp + heal);
        pushFloat(h.x, h.y - 24, `+${heal} HP`, "#34d399", true);
        if (state.settings.animations) state.vfx.push({ id: sid, x: h.x, y: h.y, tx: h.x, ty: h.y, color, r: aoe, at: now, dur: 400 });
        return;
      }
    } else {
      if (now - state.lastAttackAt < 300) return;
      state.lastAttackAt = now;
      playTone(h.classId === "dw" ? 440 : 220, 0.08, "square");
    }

    let primary = null, best = Infinity;
    state.monsters.forEach(m => {
      if (m.hp <= 0) return;
      const d = Math.hypot(m.x - h.x, m.y - h.y);
      if (d <= range + m.r && d < best) { best = d; primary = m; }
    });
    const tx = primary ? primary.x : h.x, ty = primary ? primary.y : h.y;
    if (state.settings.animations) state.vfx.push({ id: sid, x: h.x, y: h.y, tx, ty, color, r: aoe, at: now, dur: 360 });
    h.mp = Math.max(0, h.mp - mpCost);
    if (!primary) return;

    const mapMeta = state.maps.find(m => m.id === h.mapId) || DEFAULT_MAPS[0];
    const rates = getActiveModRates();
    let gainExp = 0, gainZen = 0, kills = 0;

    state.monsters.forEach(m => {
      if (m.hp <= 0) return;
      const hit = m.uid === primary.uid || (skillIdx !== null && Math.hypot(m.x - tx, m.y - ty) <= aoe + m.r);
      if (!hit) return;
      const crit = Math.random() < der.critChance;
      const dmg = Math.max(4, Math.round((der.attackPower * mult - m.def * 0.45) * (crit ? 1.65 : 0.92 + Math.random() * 0.16)));
      m.hp = Math.max(0, m.hp - dmg);
      m.flashUntil = now + 130;
      pushFloat(m.x, m.y - m.r, crit ? `${dmg}!` : `${dmg}`, crit ? "#38bdf8" : "#fbbf24", crit);
      if (m.hp === 0) {
        kills++;
        gainExp += Math.max(5, Math.round(m.exp * mapMeta.expRate * rates.expMul));
        gainZen += Math.max(10, Math.round(m.zen * 0.35 * mapMeta.dropZen * rates.zenMul));
        m.respawnAt = now + (m.boss ? 16000 : 6000);
        // Spawn drops
        const dropZen = Math.max(15, Math.round(m.zen * mapMeta.dropZen * rates.zenMul));
        state.drops.push({ id: uid("dz"), x: m.x - 14, y: m.y + 8, zen: dropZen, at: now });
        const chance = m.boss ? 1 : Math.min(0.95, mapMeta.dropItems * rates.dropMul);
        if (Math.random() <= chance) {
          const picked = m.boss ? ITEM_POOL.filter(i => i.slot === "jewel" || i.slot === "pet")[Math.floor(Math.random() * 7)] : ITEM_POOL[Math.floor(Math.random() * ITEM_POOL.length)];
          if (picked) state.drops.push({ id: uid("di"), x: m.x + 14, y: m.y - 6, item: { ...clone(picked), id: uid("it"), quantity: 1 }, at: now });
        }
      }
    });

    if (kills > 0) {
      h.kills += kills;
      h.zen += gainZen;
      h.exp += gainExp;
      while (h.exp >= h.expToNext && h.level < 400) {
        h.exp -= h.expToNext;
        h.level++;
        h.statPoints += cls.ptsPerLevel;
        h.expToNext = Math.round(120 * Math.pow(h.level, 1.38));
        const d2 = computeDerived(h);
        h.maxHp = d2.maxHp; h.maxMp = d2.maxMp; h.hp = d2.maxHp; h.mp = d2.maxMp;
        playTone(760, 0.22, "triangle");
        pushFloat(h.x, h.y - 38, `LEVEL UP! Nv.${h.level}`, "#fde68a", true);
      }
    }
  };

  const usePotion = (kind) => {
    const h = state.activeHero;
    if (!h) return;
    const idx = h.inventory.findIndex(i => i.potionType === kind);
    if (idx < 0) return;
    if (h.inventory[idx].quantity > 1) h.inventory[idx].quantity--;
    else h.inventory.splice(idx, 1);
    playTone(580, 0.09);
    if (kind === "hp") {
      const amt = Math.round(h.maxHp * 0.45);
      h.hp = Math.min(h.maxHp, h.hp + amt);
      pushFloat(h.x, h.y - 24, `+${amt} HP`, "#34d399");
    } else {
      const amt = Math.round(h.maxMp * 0.5);
      h.mp = Math.min(h.maxMp, h.mp + amt);
      pushFloat(h.x, h.y - 24, `+${amt} MP`, "#60a5fa");
    }
  };

  // --- Dialogs (PJ, Bag/Chaos, Vault, NPC, Mods, Admin, Settings) ---
  const showCharacterModal = () => {
    const h = state.activeHero; if (!h) return;
    const cls = CLASSES[h.classId];
    const rates = getActiveModRates();
    openModal(`Personaje: ${h.name}`, (body) => {
      const der = computeDerived(h);
      const title = h.evolutionTier >= 2 ? cls.evo400 : h.evolutionTier === 1 ? cls.evo150 : cls.name;
      body.innerHTML = `
        <div class="card">
          <div><b>${title}</b> · Nivel ${h.level} · Resets ${h.resets} · ${h.kills} bajas · +${cls.ptsPerLevel} pts/nv</div>
          <div class="muted" style="margin-top:4px;">ATK ${der.attackPower} · DEF ${der.defense} · Crítico ${Math.round(der.critChance * 100)}% · Vel ${der.moveSpeed}</div>
          ${h.equipment.pet ? `<div style="color:var(--sky);font-size:12px;margin-top:4px;">Mascota: ${h.equipment.pet.name}</div>` : ""}
        </div>
        <div class="msg-banner">Puntos disponibles: <b>${h.statPoints}</b></div>
      `;
      const evoBtn = document.createElement("button");
      evoBtn.className = "purple";
      evoBtn.textContent = h.evolutionTier === 0 ? `Quest Evolución Nv.150 → ${cls.evo150}` : h.evolutionTier === 1 ? `Quest Evolución Nv.400 → ${cls.evo400}` : `Evolución Máxima (${cls.evo400})`;
      evoBtn.disabled = h.evolutionTier >= 2;
      evoBtn.onclick = () => {
        if (h.evolutionTier === 0 && h.level >= 150) { h.evolutionTier = 1; h.statPoints += 25; showCharacterModal(); }
        else if (h.evolutionTier === 1 && h.level >= 400) { h.evolutionTier = 2; h.statPoints += 50; showCharacterModal(); }
        else alert(h.evolutionTier === 0 ? "Requiere Nivel 150" : "Requiere Nivel 400");
        saveCurrentAccountData();
      };
      body.appendChild(evoBtn);

      [["str", "Fuerza (STR)"], ["agi", "Agilidad (AGI)"], ["vit", "Vitalidad (VIT)"], ["ene", "Energía (ENE)"]].forEach(([k, label]) => {
        const r = document.createElement("div");
        r.className = "item-row";
        r.innerHTML = `<span><b>${label}:</b> ${h.stats[k]}</span><div class="row"><button class="sm">+1</button><button class="sm gold">+10</button></div>`;
        const [b1, b10] = r.querySelectorAll("button");
        b1.disabled = h.statPoints < 1; b10.disabled = h.statPoints < 10;
        b1.onclick = () => { h.stats[k] += 1; h.statPoints -= 1; const d = computeDerived(h); h.maxHp = d.maxHp; h.maxMp = d.maxMp; saveCurrentAccountData(); showCharacterModal(); };
        b10.onclick = () => { h.stats[k] += 10; h.statPoints -= 10; const d = computeDerived(h); h.maxHp = d.maxHp; h.maxMp = d.maxMp; saveCurrentAccountData(); showCharacterModal(); };
        body.appendChild(r);
      });

      const resetBtn = document.createElement("button");
      resetBtn.className = "gold";
      resetBtn.disabled = h.level < rates.resetLevel;
      resetBtn.textContent = h.level >= rates.resetLevel ? "Master Reset (+150 pts base)" : `Master Reset en Nivel ${rates.resetLevel}`;
      resetBtn.onclick = () => {
        h.resets++; h.level = 1; h.exp = 0; h.expToNext = 120; h.statPoints = h.resets * 150; h.stats = { ...cls.baseStats };
        const d = computeDerived(h); h.maxHp = d.maxHp; h.hp = d.maxHp; h.maxMp = d.maxMp; h.mp = d.maxMp;
        saveCurrentAccountData(); showCharacterModal();
      };
      body.appendChild(resetBtn);
    });
  };

  const showBagChaosModal = (startChaos = false, msg = null) => {
    const h = state.activeHero; if (!h) return;
    let tab = startChaos ? "chaos" : "bag";
    const render = (feedback) => openModal(`Mochila y Chaos Machine (${h.zen} Zen)`, (body) => {
      const tabs = document.createElement("div");
      tabs.className = "row";
      tabs.innerHTML = `<button class="${tab === "bag" ? "gold" : ""}" style="flex:1;">Equipo y Mochila (${h.inventory.length}/24)</button><button class="${tab === "chaos" ? "purple" : ""}" style="flex:1;">Chaos Machine</button>`;
      const [tb, tc] = tabs.querySelectorAll("button");
      tb.onclick = () => { tab = "bag"; render(null); };
      tc.onclick = () => { tab = "chaos"; render(null); };
      body.appendChild(tabs);
      if (feedback) { const m = document.createElement("div"); m.className = "msg-banner"; m.textContent = feedback; body.appendChild(m); }

      if (tab === "bag") {
        ["weapon", "offhand", "armor", "helm", "wings", "pendant", "pet"].forEach(slot => {
          const eq = h.equipment[slot];
          const r = document.createElement("div");
          r.className = "item-row";
          r.innerHTML = `<span><b>${slot.toUpperCase()}:</b> ${eq ? `<span style="color:${eq.color}">${eq.name} +${eq.plus}</span>` : (slot === "helm" && !CLASSES[h.classId].canHelm ? "No usa casco (MG)" : "Vacío")}</span>`;
          if (eq) {
            const btn = document.createElement("button"); btn.className = "sm"; btn.textContent = "Quitar";
            btn.onclick = () => { if (h.inventory.length < 24) { h.inventory.push(eq); h.equipment[slot] = null; saveCurrentAccountData(); render(null); } };
            r.appendChild(btn);
          }
          body.appendChild(r);
        });
        h.inventory.forEach((it, idx) => {
          const r = document.createElement("div"); r.className = "item-row";
          r.innerHTML = `<div><div style="color:${it.color};font-weight:700;">${it.name}${it.plus ? " +" + it.plus : ""}${it.quantity > 1 ? " x" + it.quantity : ""}</div><div class="muted">${it.slot} · Valor ${it.valueZen} Zen</div></div><div class="row"></div>`;
          const act = r.querySelector(".row");
          if (!["jewel", "potion", "talisman"].includes(it.slot)) {
            const eqB = document.createElement("button"); eqB.className = "sm sky"; eqB.textContent = "Equipar";
            eqB.onclick = () => {
              if (it.slot === "helm" && !CLASSES[h.classId].canHelm) return render("Magic Gladiator no puede equipar Casco.");
              if (it.classReq !== "all" && it.classReq !== h.classId) return render(`Exclusivo de ${it.classReq.toUpperCase()}.`);
              const old = h.equipment[it.slot]; h.equipment[it.slot] = it; h.inventory.splice(idx, 1); if (old) h.inventory.push(old);
              const d = computeDerived(h); h.maxHp = d.maxHp; h.maxMp = d.maxMp; saveCurrentAccountData(); render(`Equipaste ${it.name}.`);
            };
            act.appendChild(eqB);
            const blB = document.createElement("button"); blB.className = "sm purple"; blB.textContent = "+Bless";
            blB.onclick = () => { if (!consumeJewel(h, "bless")) return render("Falta Jewel of Bless."); it.plus = Math.min(9, it.plus + 1); saveCurrentAccountData(); render(`${it.name} subió a +${it.plus}.`); };
            act.appendChild(blB);
            const lfB = document.createElement("button"); lfB.className = "sm emerald"; lfB.textContent = "+Life";
            lfB.onclick = () => { if (!consumeJewel(h, "life")) return render("Falta Jewel of Life."); it.attack += 4; it.defense += 4; saveCurrentAccountData(); render(`Jewel of Life aplicada a ${it.name}.`); };
            act.appendChild(lfB);
          }
          const sellB = document.createElement("button"); sellB.className = "sm"; sellB.textContent = "Vender";
          sellB.onclick = () => { h.zen += it.valueZen * (it.quantity || 1); h.inventory.splice(idx, 1); saveCurrentAccountData(); render(`Vendiste ${it.name}.`); };
          act.appendChild(sellB);
          body.appendChild(r);
        });
      } else {
        const rates = getActiveModRates();
        [[1, "Alas Nivel 1 (Wings of Satan S1)", "1 Chaos + 2,500 Zen", 2500, 82],
         [2, "Alas Nivel 2 (Dragon/Soul/Cape S2)", "1 Chaos + 1 Bless + 5,000 Zen", 5000, 70],
         [3, "Alas Nivel 3 (Wings of Storm S3)", "1 Chaos + 1 Bless + 1 Creation + 12,000 Zen", 12000, 55]].forEach(([tier, title, costDesc, zenCost, baseRate]) => {
          const card = document.createElement("div"); card.className = "item-row";
          card.innerHTML = `<div><b>${title}</b><div class="muted">${costDesc} · Éxito ${(baseRate + rates.chaosBonus)}%</div></div>`;
          const b = document.createElement("button"); b.className = "gold sm"; b.textContent = "Combinar";
          b.onclick = () => {
            if (h.zen < zenCost) return render(`Necesitas ${zenCost} Zen.`);
            if (!consumeJewel(h, "chaos")) return render("Falta Jewel of Chaos.");
            if (tier >= 2 && !consumeJewel(h, "bless")) return render("Falta Jewel of Bless.");
            if (tier >= 3 && !consumeJewel(h, "creation")) return render("Falta Jewel of Creation.");
            h.zen -= zenCost;
            if (Math.random() * 100 < Math.min(95, baseRate + rates.chaosBonus)) {
              const wings = { id: uid("wings"), templateId: `wings-s${tier}`, name: title, slot: "wings", rarity: tier === 3 ? "mythic" : "excellent", plus: tier, attack: 20 * tier, magic: 20 * tier, defense: 16 * tier, hpBonus: 80 * tier, speedBonus: 10 + tier * 6, wingsTier: tier, classReq: "all", color: tier === 3 ? "#fbbf24" : "#38bdf8", valueZen: 18000 * tier, quantity: 1 };
              addItem(h, wings); saveCurrentAccountData(); render(`¡Forjaste ${wings.name}!`);
            } else { saveCurrentAccountData(); render("La combinación falló en Chaos Goblin."); }
          };
          card.appendChild(b); body.appendChild(card);
        });
      }
    });
    render(msg);
  };

  const showVaultModal = () => {
    const h = state.activeHero; if (!h) return;
    openModal(`Baúl Compartido (${state.vault.length}/40)`, (body) => {
      const t1 = document.createElement("h3"); t1.textContent = "En el Baúl"; body.appendChild(t1);
      if (state.vault.length === 0) body.innerHTML += `<div class="muted">Baúl vacío.</div>`;
      state.vault.forEach((it, idx) => {
        const r = document.createElement("div"); r.className = "item-row";
        r.innerHTML = `<span style="color:${it.color}">${it.name} +${it.plus}</span>`;
        const b = document.createElement("button"); b.className = "sm sky"; b.textContent = "Retirar";
        b.onclick = () => { if (h.inventory.length < 24) { state.vault.splice(idx, 1); addItem(h, it); saveCurrentAccountData(); showVaultModal(); } };
        r.appendChild(b); body.appendChild(r);
      });
      const t2 = document.createElement("h3"); t2.textContent = "Tu Mochila"; body.appendChild(t2);
      h.inventory.forEach((it, idx) => {
        const r = document.createElement("div"); r.className = "item-row";
        r.innerHTML = `<span style="color:${it.color}">${it.name} +${it.plus}</span>`;
        const b = document.createElement("button"); b.className = "sm gold"; b.textContent = "Guardar";
        b.onclick = () => { if (state.vault.length < 40) { h.inventory.splice(idx, 1); state.vault.push(it); saveCurrentAccountData(); showVaultModal(); } };
        r.appendChild(b); body.appendChild(r);
      });
    });
  };

  const showNpcModal = (npc) => {
    const h = state.activeHero; if (!h) return;
    if (npc.role === "chaos") return showBagChaosModal(true);
    if (npc.role === "vault") return showVaultModal();
    openModal(`${npc.name} (${npc.subtitle})`, (body) => {
      if (npc.role === "quest") {
        const b = document.createElement("button"); b.className = "purple"; b.textContent = "Completar Quest de Evolución (Nv.150 / Nv.400)";
        b.onclick = () => { closeModal(); showCharacterModal(); };
        body.appendChild(b); return;
      }
      if (npc.role === "archer") {
        const tal = h.inventory.find(i => i.slot === "talisman");
        const qty = tal ? tal.quantity : 0;
        body.innerHTML = `<div class="msg-banner">Entrega 3 Talisman / Rena + 3,000 Zen por Alas S2. Tienes: ${qty}</div>`;
        const b = document.createElement("button"); b.className = "gold"; b.disabled = qty < 3 || h.zen < 3000; b.textContent = "Canjear por Alas S2";
        b.onclick = () => {
          tal.quantity -= 3; if (tal.quantity <= 0) h.inventory = h.inventory.filter(x => x.id !== tal.id);
          h.zen -= 3000;
          addItem(h, { id: uid("wings"), templateId: "wings-s2-archer", name: "Wings of Spirits (S2 · Archer)", slot: "wings", rarity: "excellent", plus: 3, attack: 44, magic: 44, defense: 34, hpBonus: 160, speedBonus: 24, wingsTier: 2, classReq: "all", color: "#fde047", valueZen: 35000, quantity: 1 });
          saveCurrentAccountData(); showNpcModal(npc);
        };
        body.appendChild(b); return;
      }
      ITEM_POOL.forEach(tpl => {
        const cost = tpl.slot === "potion" ? tpl.valueZen * 5 : Math.round(tpl.valueZen * 1.4);
        const r = document.createElement("div"); r.className = "item-row";
        r.innerHTML = `<div><div style="color:${tpl.color};font-weight:700;">${tpl.name}${tpl.slot === "potion" ? " (x5)" : ""}</div><div class="muted">${cost} Zen</div></div>`;
        const b = document.createElement("button"); b.className = "gold sm"; b.textContent = "Comprar"; b.disabled = h.zen < cost;
        b.onclick = () => {
          h.zen -= cost; addItem(h, { ...clone(tpl), id: uid("shop"), quantity: tpl.slot === "potion" ? 5 : 1 });
          saveCurrentAccountData(); showNpcModal(npc);
        };
        r.appendChild(b); body.appendChild(r);
      });
    });
  };

  const showModsModal = () => openModal("Gestor de Mods Offline (JSON)", (body) => {
    state.mods.forEach(m => {
      const r = document.createElement("div"); r.className = "item-row";
      r.innerHTML = `<div><b>${m.name}</b> <span class="muted">EXP x${m.expMultiplier} · Zen x${m.zenMultiplier}</span><div class="muted">${m.description}</div></div>`;
      const b = document.createElement("button"); b.className = m.enabled ? "emerald sm" : "sm"; b.textContent = m.enabled ? "Activo" : "Inactivo";
      b.onclick = () => { m.enabled = !m.enabled; saveJson(STORAGE_KEYS.mods, state.mods); showModsModal(); };
      r.appendChild(b); body.appendChild(r);
    });
  });

  const showAdminModal = () => openModal("Panel Admin · JSON Plano (Sin SQL)", (body) => {
    body.innerHTML = `<div class="msg-banner">Versión local: v${state.manifest.version} · Edita mapas.json o sincroniza desde manifest.json</div>`;
    const ta = document.createElement("textarea"); ta.rows = 12; ta.value = JSON.stringify({ maps: state.maps }, null, 2);
    body.appendChild(ta);
    const row = document.createElement("div"); row.className = "row";
    const btnSave = document.createElement("button"); btnSave.className = "emerald"; btnSave.textContent = "Aplicar mapas.json";
    btnSave.onclick = () => {
      try {
        const parsed = JSON.parse(ta.value);
        if (!Array.isArray(parsed.maps)) throw new Error("Falta arreglo 'maps'");
        state.maps = parsed.maps; saveJson(STORAGE_KEYS.maps, state.maps); populateMapSelect();
        alert("mapas.json guardado correctamente.");
      } catch (e) { alert("JSON inválido: " + e.message); }
    };
    const btnExport = document.createElement("button"); btnExport.className = "gold"; btnExport.textContent = "Descargar mapas.json";
    btnExport.onclick = () => {
      const blob = new Blob([ta.value], { type: "application/json" });
      const a = document.createElement("a"); a.href = URL.createObjectURL(blob); a.download = "mapas.json"; a.click();
    };
    row.append(btnSave, btnExport); body.appendChild(row);
  });

  const showSettingsModal = () => openModal("Configuración y Controles", (body) => {
    body.innerHTML = `
      <label>Volumen SFX: <span id="v-sfx">${Math.round(state.settings.sfxVolume * 100)}%</span></label>
      <input type="range" min="0" max="1" step="0.05" value="${state.settings.sfxVolume}" id="in-sfx" />
      <label>Brillo de Pantalla: <span id="v-br">${Math.round(state.settings.brightness * 100)}%</span></label>
      <input type="range" min="0.4" max="1" step="0.05" value="${state.settings.brightness}" id="in-br" />
      <label class="row"><input type="checkbox" id="in-hd" ${state.settings.hdGraphics ? "checked" : ""} style="width:auto;" /> Calidad Gráfica HD</label>
      <label class="row"><input type="checkbox" id="in-anim" ${state.settings.animations ? "checked" : ""} style="width:auto;" /> Animaciones y Partículas VFX</label>
    `;
    body.querySelector("#in-sfx").oninput = (e) => { state.settings.sfxVolume = +e.target.value; body.querySelector("#v-sfx").textContent = Math.round(state.settings.sfxVolume * 100) + "%"; saveJson(STORAGE_KEYS.settings, state.settings); };
    body.querySelector("#in-br").oninput = (e) => { state.settings.brightness = +e.target.value; body.querySelector("#v-br").textContent = Math.round(state.settings.brightness * 100) + "%"; document.getElementById("brightness-veil").style.opacity = String(Math.max(0, 1 - state.settings.brightness)); saveJson(STORAGE_KEYS.settings, state.settings); };
    body.querySelector("#in-hd").onchange = (e) => { state.settings.hdGraphics = e.target.checked; saveJson(STORAGE_KEYS.settings, state.settings); };
    body.querySelector("#in-anim").onchange = (e) => { state.settings.animations = e.target.checked; saveJson(STORAGE_KEYS.settings, state.settings); };
  });

  // Wire HUD Buttons
  document.getElementById("btn-title-mods").onclick = showModsModal;
  document.getElementById("btn-title-admin").onclick = showAdminModal;
  document.getElementById("btn-title-settings").onclick = showSettingsModal;
  document.getElementById("hud-pj").onclick = showCharacterModal;
  document.getElementById("hud-bag").onclick = () => showBagChaosModal(false);
  document.getElementById("hud-vault").onclick = showVaultModal;
  document.getElementById("hud-mods").onclick = showModsModal;
  document.getElementById("hud-admin").onclick = showAdminModal;
  document.getElementById("hud-settings").onclick = showSettingsModal;
  document.getElementById("hud-auto").onclick = () => {
    state.autoHunt = !state.autoHunt;
    document.getElementById("hud-auto").textContent = `MU Helper: ${state.autoHunt ? "ON" : "OFF"}`;
  };
  document.getElementById("btn-attack").onclick = () => performCombat(null);
  document.getElementById("btn-pot-hp").onclick = () => usePotion("hp");
  document.getElementById("btn-pot-mp").onclick = () => usePotion("mp");
  [0, 1, 2].forEach(i => { document.getElementById(`btn-skill-${i}`).onclick = () => performCombat(i); });

  const bindPad = (id, dx, dy) => {
    const el = document.getElementById(id);
    const start = () => { state.pad.x = dx; state.pad.y = dy; };
    const stop = () => { state.pad.x = 0; state.pad.y = 0; };
    el.onmousedown = start; el.onmouseup = stop; el.onmouseleave = stop;
    el.ontouchstart = (e) => { e.preventDefault(); start(); }; el.ontouchend = stop;
  };
  bindPad("pad-up", 0, -1); bindPad("pad-down", 0, 1); bindPad("pad-left", -1, 0); bindPad("pad-right", 1, 0);

  window.addEventListener("keydown", (e) => {
    state.keys[e.key.toLowerCase()] = true;
    if (!state.activeHero) return;
    if (e.code === "Space") { e.preventDefault(); performCombat(null); }
    if (e.key === "1") performCombat(0);
    if (e.key === "2") performCombat(1);
    if (e.key === "3") performCombat(2);
    if (e.key.toLowerCase() === "q") usePotion("hp");
    if (e.key.toLowerCase() === "e") usePotion("mp");
    if (e.key.toLowerCase() === "f") {
      const near = getNearbyNpc();
      if (near) showNpcModal(near);
    }
  });
  window.addEventListener("keyup", (e) => { state.keys[e.key.toLowerCase()] = false; });

  const worldCanvas = document.getElementById("world-canvas");
  const ctx = worldCanvas.getContext("2d");
  const miniCanvas = document.getElementById("minimap-canvas");
  const mctx = miniCanvas.getContext("2d");

  worldCanvas.addEventListener("click", (e) => {
    const h = state.activeHero; if (!h) return;
    const rect = worldCanvas.getBoundingClientRect();
    const wx = h.x + (e.clientX - rect.left - worldCanvas.width / 2);
    const wy = h.y + (e.clientY - rect.top - worldCanvas.height / 2);
    const npc = state.npcs.find(n => n.mapId === h.mapId && Math.hypot(n.wx - wx, n.wy - wy) <= 36);
    if (npc) return showNpcModal(npc);
    const mob = state.monsters.find(m => m.hp > 0 && Math.hypot(m.x - wx, m.y - wy) <= m.r + 24);
    if (mob && Math.hypot(mob.x - h.x, mob.y - h.y) <= CLASSES[h.classId].range + 30) return performCombat(null);
    const dist = Math.hypot(wx - h.x, wy - h.y);
    if (dist > 8) {
      const step = Math.min(dist, 56);
      h.x = Math.max(24, Math.min(WORLD_SIZE - 24, h.x + ((wx - h.x) / dist) * step));
      h.y = Math.max(24, Math.min(WORLD_SIZE - 24, h.y + ((wy - h.y) / dist) * step));
      state.facing = Math.atan2(wx - h.x, wy - h.y);
    }
  });

  const getNearbyNpc = () => {
    const h = state.activeHero; if (!h) return null;
    let best = null, bestD = 95;
    state.npcs.filter(n => n.mapId === h.mapId).forEach(n => {
      const d = Math.hypot(n.wx - h.x, n.wy - h.y);
      if (d < bestD) { bestD = d; best = n; }
    });
    return best;
  };

  document.getElementById("btn-talk-npc").onclick = () => { const n = getNearbyNpc(); if (n) showNpcModal(n); };

  const refreshHud = () => {
    const h = state.activeHero; if (!h) return;
    const cls = CLASSES[h.classId];
    const mapMeta = state.maps.find(m => m.id === h.mapId) || DEFAULT_MAPS[0];
    document.getElementById("hud-pj").textContent = `${h.name} (Nv.${h.level}${h.statPoints > 0 ? " +" : ""})`;
    document.getElementById("hud-coords").textContent = `X:${Math.round(h.x)} Y:${Math.round(h.y)}`;
    document.getElementById("lbl-hp").textContent = h.hp;
    document.getElementById("lbl-mp").textContent = h.mp;
    document.querySelector("#globe-hp .globe-fill").style.height = `${(h.hp / Math.max(1, h.maxHp)) * 100}%`;
    document.querySelector("#globe-mp .globe-fill").style.height = `${(h.mp / Math.max(1, h.maxMp)) * 100}%`;
    document.getElementById("lbl-hero-summary").textContent = `Nv.${h.level} · RR ${h.resets}`;
    document.getElementById("lbl-zen").textContent = `${h.zen.toLocaleString()} Zen`;
    document.getElementById("exp-bar").style.width = `${Math.min(100, (h.exp / Math.max(1, h.expToNext)) * 100)}%`;
    document.getElementById("lbl-map-subtitle").textContent = `${mapMeta.name} · ${mapMeta.subtitle}`;
    const hpQty = h.inventory.filter(i => i.potionType === "hp").reduce((s, i) => s + (i.quantity || 1), 0);
    const mpQty = h.inventory.filter(i => i.potionType === "mp").reduce((s, i) => s + (i.quantity || 1), 0);
    document.getElementById("btn-pot-hp").textContent = `HP x${hpQty} [Q]`;
    document.getElementById("btn-pot-mp").textContent = `MP x${mpQty} [E]`;
    cls.skills.forEach((sk, idx) => {
      const b = document.getElementById(`btn-skill-${idx}`);
      const rem = ((state.cooldowns[sk.id] || 0) - Date.now()) / 1000;
      b.textContent = rem > 0 ? rem.toFixed(1) + "s" : sk.name.split(" ")[0];
      b.style.borderColor = sk.color;
    });
    const near = getNearbyNpc();
    const npcBtn = document.getElementById("btn-talk-npc");
    if (near) { npcBtn.textContent = `Hablar con ${near.name} [F]`; npcBtn.classList.remove("hidden"); }
    else npcBtn.classList.add("hidden");
  };

  // --- Simulation + Render Loop ---
  let lastTick = Date.now(), lastSave = Date.now();
  const loop = () => {
    const now = Date.now();
    const dt = Math.min(0.1, (now - lastTick) / 1000);
    lastTick = now;

    if (state.activeHero) {
      const h = state.activeHero;
      const der = computeDerived(h);
      let vx = state.pad.x, vy = state.pad.y;
      if (state.keys["w"] || state.keys["arrowup"]) vy -= 1;
      if (state.keys["s"] || state.keys["arrowdown"]) vy += 1;
      if (state.keys["a"] || state.keys["arrowleft"]) vx -= 1;
      if (state.keys["d"] || state.keys["arrowright"]) vx += 1;
      if (vx || vy) {
        const len = Math.hypot(vx, vy);
        h.x = Math.max(24, Math.min(WORLD_SIZE - 24, h.x + (vx / len) * der.moveSpeed * dt));
        h.y = Math.max(24, Math.min(WORLD_SIZE - 24, h.y + (vy / len) * der.moveSpeed * dt));
        state.facing = Math.atan2(vx, vy);
      }

      const inSafe = Math.hypot(h.x - SAFE_CENTER, h.y - SAFE_CENTER) <= SAFE_RADIUS;
      // Drops pickup
      state.drops = state.drops.filter(d => {
        if (Math.hypot(d.x - h.x, d.y - h.y) <= 44) {
          if (d.zen) { h.zen += d.zen; pushFloat(h.x, h.y - 26, `+${d.zen} Zen`, "#fbbf24"); }
          if (d.item) { addItem(h, d.item); pushFloat(h.x, h.y - 34, `+${d.item.name}`, d.item.color); }
          return false;
        }
        return now - d.at < 45000;
      });

      // Monsters AI
      state.monsters.forEach(m => {
        if (m.hp <= 0) { if (m.respawnAt && now >= m.respawnAt) { m.hp = m.maxHp; m.x = m.homeX; m.y = m.homeY; m.respawnAt = 0; } return; }
        const dist = Math.hypot(h.x - m.x, h.y - m.y);
        if (!inSafe && dist < 240 && dist > m.r + 20) {
          const cx = m.x + ((h.x - m.x) / dist) * m.speed * dt;
          const cy = m.y + ((h.y - m.y) / dist) * m.speed * dt;
          if (Math.hypot(cx - SAFE_CENTER, cy - SAFE_CENTER) >= SAFE_RADIUS + m.r) { m.x = cx; m.y = cy; }
        }
        if (!inSafe && dist <= m.r + 26 && now >= m.atkCd) {
          const dmg = Math.max(1, Math.round(m.atk - der.defense * 0.45));
          h.hp -= dmg; m.atkCd = now + 1150;
          pushFloat(h.x, h.y - 22, `-${dmg}`, "#ef4444");
        }
      });

      if (inSafe) h.hp = Math.min(der.maxHp, h.hp + 18 * dt);
      h.mp = Math.min(der.maxMp, h.mp + (inSafe ? 12 : 4) * dt);
      h.hp = Math.round(h.hp); h.mp = Math.round(h.mp);
      if (h.hp <= 0) { h.hp = der.maxHp; h.mp = der.maxMp; h.x = SAFE_CENTER; h.y = SAFE_CENTER; pushFloat(h.x, h.y - 30, "Zona Segura", "#fbbf24"); }

      if (state.autoHunt) {
        const alive = state.monsters.filter(m => m.hp > 0);
        const nearMob = alive.sort((a, b) => Math.hypot(a.x - h.x, a.y - h.y) - Math.hypot(b.x - h.x, b.y - h.y))[0];
        if (nearMob) {
          const d = Math.hypot(nearMob.x - h.x, nearMob.y - h.y);
          if (d <= CLASSES[h.classId].range) performCombat(h.mp >= 20 ? 0 : null);
          else { h.x += ((nearMob.x - h.x) / d) * der.moveSpeed * dt; h.y += ((nearMob.y - h.y) / d) * der.moveSpeed * dt; }
        }
      }

      state.vfx = state.vfx.filter(v => now - v.at < v.dur);
      state.floats = state.floats.filter(f => now - f.at < 950);
      if (now - lastSave > 3500) { lastSave = now; saveCurrentAccountData(); }

      drawWorld(now);
      drawMinimap();
      refreshHud();
    }
    requestAnimationFrame(loop);
  };

  const drawWorld = (now) => {
    if (worldCanvas.width !== window.innerWidth || worldCanvas.height !== window.innerHeight) {
      worldCanvas.width = window.innerWidth; worldCanvas.height = window.innerHeight;
    }
    const h = state.activeHero;
    const mapMeta = state.maps.find(m => m.id === h.mapId) || DEFAULT_MAPS[0];
    const camX = h.x - worldCanvas.width / 2, camY = h.y - worldCanvas.height / 2;

    ctx.fillStyle = mapMeta.g1;
    ctx.fillRect(0, 0, worldCanvas.width, worldCanvas.height);

    const tile = state.settings.hdGraphics ? 64 : 96;
    const sx0 = Math.floor(camX / tile) * tile, sy0 = Math.floor(camY / tile) * tile;
    for (let wx = sx0; wx < camX + worldCanvas.width + tile; wx += tile) {
      for (let wy = sy0; wy < camY + worldCanvas.height + tile; wy += tile) {
        const sx = wx - camX, sy = wy - camY;
        if (wx < 0 || wy < 0 || wx >= WORLD_SIZE || wy >= WORLD_SIZE) { ctx.fillStyle = "#05070b"; ctx.fillRect(sx, sy, tile + 1, tile + 1); continue; }
        ctx.fillStyle = ((Math.round(wx / tile) + Math.round(wy / tile)) & 1) === 0 ? mapMeta.g1 : mapMeta.g2;
        ctx.fillRect(sx, sy, tile + 1, tile + 1);
      }
    }

    const safeSx = SAFE_CENTER - camX, safeSy = SAFE_CENTER - camY;
    ctx.beginPath(); ctx.arc(safeSx, safeSy, SAFE_RADIUS, 0, Math.PI * 2);
    ctx.fillStyle = "rgba(46,43,38,0.55)"; ctx.fill();
    ctx.strokeStyle = mapMeta.accent; ctx.lineWidth = 3; ctx.stroke();

    // NPCs
    state.npcs.filter(n => n.mapId === h.mapId).forEach(n => {
      const nx = n.wx - camX, ny = n.wy - camY;
      ctx.fillStyle = n.role === "chaos" ? "#fbbf24" : n.role === "archer" ? "#38bdf8" : n.role === "quest" ? "#a855f7" : "#10b981";
      ctx.fillRect(nx - 12, ny - 6, 24, 20);
      ctx.beginPath(); ctx.arc(nx, ny - 12, 8, 0, Math.PI * 2); ctx.fillStyle = "#fde68a"; ctx.fill();
      ctx.font = "bold 12px sans-serif"; ctx.textAlign = "center"; ctx.fillStyle = "#fde68a";
      ctx.fillText(n.name, nx, ny - 26);
    });

    // Drops
    state.drops.forEach(d => {
      const dx = d.x - camX, dy = d.y - camY;
      ctx.fillStyle = d.zen ? "#fbbf24" : d.item.color;
      ctx.beginPath(); ctx.arc(dx, dy, 6, 0, Math.PI * 2); ctx.fill();
      ctx.font = "bold 11px sans-serif"; ctx.textAlign = "center";
      ctx.fillText(d.zen ? `${d.zen} Zen` : d.item.name, dx, dy - 12);
    });

    // Monsters
    state.monsters.forEach(m => {
      if (m.hp <= 0) return;
      const mx = m.x - camX, my = m.y - camY;
      ctx.beginPath(); ctx.arc(mx, my, m.r, 0, Math.PI * 2);
      ctx.fillStyle = now < m.flashUntil ? "#fff" : m.color; ctx.fill();
      ctx.fillStyle = "#0f172a"; ctx.fillRect(mx - 22, my - m.r - 12, 44, 5);
      ctx.fillStyle = m.boss ? "#f59e0b" : "#ef4444"; ctx.fillRect(mx - 22, my - m.r - 12, 44 * (m.hp / m.maxHp), 5);
      ctx.font = "bold 11px sans-serif"; ctx.textAlign = "center"; ctx.fillStyle = m.boss ? "#fbbf24" : "#fff";
      ctx.fillText(`${m.name} Nv.${m.level}`, mx, my - m.r - 16);
    });

    // Hero
    const hx = worldCanvas.width / 2, hy = worldCanvas.height / 2;
    const cls = CLASSES[h.classId];
    if (h.equipment.wings) {
      ctx.fillStyle = h.equipment.wings.wingsTier >= 3 ? "rgba(251,191,36,0.75)" : "rgba(56,189,248,0.75)";
      ctx.beginPath(); ctx.moveTo(hx - 6, hy); ctx.lineTo(hx - 34, hy - 18); ctx.lineTo(hx - 24, hy + 10); ctx.fill();
      ctx.beginPath(); ctx.moveTo(hx + 6, hy); ctx.lineTo(hx + 34, hy - 18); ctx.lineTo(hx + 24, hy + 10); ctx.fill();
    }
    ctx.fillStyle = cls.color; ctx.fillRect(hx - 12, hy - 4, 24, 20);
    ctx.beginPath(); ctx.arc(hx, hy - 11, 8, 0, Math.PI * 2); ctx.fillStyle = "#fde68a"; ctx.fill();
    ctx.font = "bold 12px sans-serif"; ctx.textAlign = "center"; ctx.fillStyle = "#fde68a";
    ctx.fillText(`${h.name} [Nv.${h.level}]`, hx, hy - 28);

    // VFX & Floating Text
    state.vfx.forEach(v => {
      const p = (now - v.at) / v.dur;
      ctx.strokeStyle = v.color; ctx.lineWidth = 4; ctx.globalAlpha = 1 - p;
      ctx.beginPath(); ctx.arc(v.tx - camX, v.ty - camY, v.r * p, 0, Math.PI * 2); ctx.stroke();
      ctx.globalAlpha = 1;
    });
    state.floats.forEach(f => {
      const p = (now - f.at) / 950;
      ctx.globalAlpha = 1 - p; ctx.fillStyle = f.color;
      ctx.font = f.crit ? "bold 15px sans-serif" : "bold 13px sans-serif";
      ctx.textAlign = "center";
      ctx.fillText(f.text, f.x - camX, f.y - camY - p * 32);
      ctx.globalAlpha = 1;
    });
  };

  const drawMinimap = () => {
    const h = state.activeHero; if (!h) return;
    const sc = miniCanvas.width / WORLD_SIZE;
    mctx.fillStyle = "#0d131f"; mctx.fillRect(0, 0, miniCanvas.width, miniCanvas.height);
    mctx.strokeStyle = "rgba(251,191,36,0.45)";
    mctx.beginPath(); mctx.arc(SAFE_CENTER * sc, SAFE_CENTER * sc, SAFE_RADIUS * sc, 0, Math.PI * 2); mctx.stroke();
    mctx.fillStyle = "#34d399";
    state.npcs.filter(n => n.mapId === h.mapId).forEach(n => { mctx.beginPath(); mctx.arc(n.wx * sc, n.wy * sc, 2.5, 0, Math.PI * 2); mctx.fill(); });
    state.monsters.forEach(m => { if (m.hp > 0) { mctx.fillStyle = m.boss ? "#fbbf24" : "#ef4444"; mctx.beginPath(); mctx.arc(m.x * sc, m.y * sc, m.boss ? 3 : 1.8, 0, Math.PI * 2); mctx.fill(); } });
    mctx.fillStyle = "#ffffff"; mctx.beginPath(); mctx.arc(h.x * sc, h.y * sc, 3.2, 0, Math.PI * 2); mctx.fill();
  };

  loadAccount(state.accountId);
  requestAnimationFrame(loop);
})();
