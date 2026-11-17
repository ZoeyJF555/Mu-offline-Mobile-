import type {
  ClassId,
  HeroSave,
  Item,
  MapId,
  ModPackage,
  MonsterTemplate,
  SkillDef,
} from '../types/game';

export interface ClassMeta {
  id: ClassId;
  name: string;
  title: string;
  role: string;
  portrait: string;
  accentColor: string;
  baseStats: { str: number; agi: number; vit: number; ene: number };
  baseHp: number;
  baseMp: number;
  attackRange: number;
  starterWeapon: Omit<Item, 'id'>;
  starterArmor: Omit<Item, 'id'>;
}

export const CLASS_CATALOG: Record<ClassId, ClassMeta> = {
  dk: {
    id: 'dk',
    name: 'Dark Knight',
    title: 'Blade Knight',
    role: 'Cuerpo a cuerpo · Alta resistencia',
    portrait: '/src/assets/images/mu_dark_knight_1794870013439.jpg',
    accentColor: '#EF4444',
    baseStats: { str: 28, agi: 20, vit: 25, ene: 10 },
    baseHp: 220,
    baseMp: 90,
    attackRange: 68,
    starterWeapon: {
      templateId: 'short-sword',
      name: 'Kris Blade',
      slot: 'weapon',
      rarity: 'normal',
      plus: 0,
      attack: 18,
      classReq: 'dk',
      colorHex: '#EF4444',
      valueZen: 1200,
    },
    starterArmor: {
      templateId: 'leather-armor',
      name: 'Leather Armor',
      slot: 'armor',
      rarity: 'normal',
      plus: 0,
      defense: 12,
      hpBonus: 30,
      classReq: 'dk',
      colorHex: '#B45309',
      valueZen: 1000,
    },
  },
  dw: {
    id: 'dw',
    name: 'Dark Wizard',
    title: 'Soul Master',
    role: 'Magia AoE · Alto daño arcano',
    portrait: '/src/assets/images/mu_dark_wizard_1794870013219.jpg',
    accentColor: '#3B82F6',
    baseStats: { str: 14, agi: 18, vit: 16, ene: 35 },
    baseHp: 155,
    baseMp: 180,
    attackRange: 175,
    starterWeapon: {
      templateId: 'skull-staff',
      name: 'Skull Staff',
      slot: 'weapon',
      rarity: 'normal',
      plus: 0,
      attack: 10,
      magic: 22,
      classReq: 'dw',
      colorHex: '#3B82F6',
      valueZen: 1400,
    },
    starterArmor: {
      templateId: 'pad-armor',
      name: 'Pad Armor',
      slot: 'armor',
      rarity: 'normal',
      plus: 0,
      defense: 9,
      hpBonus: 15,
      classReq: 'dw',
      colorHex: '#2563EB',
      valueZen: 950,
    },
  },
  elf: {
    id: 'elf',
    name: 'Fairy Elf',
    title: 'Muse Elf',
    role: 'Arco a distancia · Críticos y curación',
    portrait: '/src/assets/images/mu_fairy_elf_1794870013824.jpg',
    accentColor: '#10B981',
    baseStats: { str: 18, agi: 32, vit: 18, ene: 15 },
    baseHp: 175,
    baseMp: 130,
    attackRange: 195,
    starterWeapon: {
      templateId: 'elven-bow',
      name: 'Elven Bow',
      slot: 'weapon',
      rarity: 'normal',
      plus: 0,
      attack: 19,
      speedBonus: 10,
      classReq: 'elf',
      colorHex: '#10B981',
      valueZen: 1350,
    },
    starterArmor: {
      templateId: 'vine-armor',
      name: 'Vine Armor',
      slot: 'armor',
      rarity: 'normal',
      plus: 0,
      defense: 10,
      hpBonus: 20,
      classReq: 'elf',
      colorHex: '#059669',
      valueZen: 1000,
    },
  },
};

export const SKILLS_BY_CLASS: Record<ClassId, SkillDef[]> = {
  dk: [
    {
      id: 'twisting-slash',
      name: 'Twisting Slash',
      classId: 'dk',
      mpCost: 12,
      cooldownMs: 1400,
      damageMultiplier: 1.75,
      range: 85,
      aoeRadius: 95,
      color: '#F97316',
      description: 'Giro mortal con espada alrededor del guerrero.',
    },
    {
      id: 'death-stab',
      name: 'Death Stab',
      classId: 'dk',
      mpCost: 16,
      cooldownMs: 2200,
      damageMultiplier: 2.35,
      range: 95,
      aoeRadius: 48,
      color: '#EF4444',
      description: 'Estocada perforante con ráfaga de viento.',
    },
    {
      id: 'rageful-blow',
      name: 'Rageful Blow',
      classId: 'dk',
      mpCost: 24,
      cooldownMs: 4000,
      damageMultiplier: 2.9,
      range: 90,
      aoeRadius: 130,
      color: '#F59E0B',
      description: 'Impacto sísmico que sacude a todos los enemigos cercanos.',
    },
  ],
  dw: [
    {
      id: 'evil-spirit',
      name: 'Evil Spirit',
      classId: 'dw',
      mpCost: 18,
      cooldownMs: 1500,
      damageMultiplier: 1.65,
      range: 200,
      aoeRadius: 145,
      color: '#6366F1',
      description: 'Invoca espíritus oscuros que azotan toda la zona.',
    },
    {
      id: 'meteorite',
      name: 'Meteorite',
      classId: 'dw',
      mpCost: 20,
      cooldownMs: 2100,
      damageMultiplier: 2.4,
      range: 220,
      aoeRadius: 72,
      color: '#F59E0B',
      description: 'Impacto estelar directo sobre el objetivo.',
    },
    {
      id: 'ice-storm',
      name: 'Ice Storm',
      classId: 'dw',
      mpCost: 28,
      cooldownMs: 3800,
      damageMultiplier: 2.6,
      range: 210,
      aoeRadius: 155,
      color: '#38BDF8',
      description: 'Tormenta glacial que congela y daña un área amplia.',
    },
  ],
  elf: [
    {
      id: 'triple-shot',
      name: 'Triple Shot',
      classId: 'elf',
      mpCost: 10,
      cooldownMs: 1200,
      damageMultiplier: 1.7,
      range: 220,
      aoeRadius: 85,
      color: '#10B981',
      description: 'Dispara tres flechas simultáneas en abanico.',
    },
    {
      id: 'ice-arrow',
      name: 'Ice Arrow',
      classId: 'elf',
      mpCost: 18,
      cooldownMs: 2300,
      damageMultiplier: 2.45,
      range: 230,
      aoeRadius: 50,
      color: '#38BDF8',
      description: 'Flecha de escarcha con alto daño crítico.',
    },
    {
      id: 'heal-aura',
      name: 'Bless & Heal',
      classId: 'elf',
      mpCost: 22,
      cooldownMs: 4200,
      damageMultiplier: 0,
      range: 0,
      aoeRadius: 110,
      color: '#34D399',
      description: 'Restaura 40% de HP y bendice tu ataque temporalmente.',
    },
  ],
};

export interface MapMeta {
  id: MapId;
  name: string;
  minLevel: number;
  groundPrimary: string;
  groundSecondary: string;
  safeZoneColor: string;
  accentColor: string;
  subtitle: string;
}

export const MAP_CATALOG: Record<MapId, MapMeta> = {
  lorencia: {
    id: 'lorencia',
    name: 'Lorencia',
    minLevel: 1,
    groundPrimary: '#1A2419',
    groundSecondary: '#222F20',
    safeZoneColor: '#2E2B26',
    accentColor: '#D97706',
    subtitle: 'Ciudadela central y praderas iniciales',
  },
  dungeon: {
    id: 'dungeon',
    name: 'Dungeon',
    minLevel: 15,
    groundPrimary: '#15141E',
    groundSecondary: '#1E1B2B',
    safeZoneColor: '#252236',
    accentColor: '#A855F7',
    subtitle: 'Catacumbas subterráneas de Gorgon',
  },
  devias: {
    id: 'devias',
    name: 'Devias',
    minLevel: 35,
    groundPrimary: '#182433',
    groundSecondary: '#203247',
    safeZoneColor: '#283A4E',
    accentColor: '#38BDF8',
    subtitle: 'Bastión helado de la Reina del Hielo',
  },
  noria: {
    id: 'noria',
    name: 'Noria',
    minLevel: 60,
    groundPrimary: '#132721',
    groundSecondary: '#18342C',
    safeZoneColor: '#253D34',
    accentColor: '#10B981',
    subtitle: 'Bosque ancestral élfico',
  },
  custom: {
    id: 'custom',
    name: 'Arena Mod',
    minLevel: 1,
    groundPrimary: '#241418',
    groundSecondary: '#301B21',
    safeZoneColor: '#382228',
    accentColor: '#F43F5E',
    subtitle: 'Zona de desafío personalizada por Mods',
  },
};

export const BASE_MONSTERS: MonsterTemplate[] = [
  // Lorencia
  { id: 'budge-dragon', name: 'Budge Dragon', mapId: 'lorencia', level: 4, maxHp: 90, attack: 10, defense: 3, expReward: 38, zenReward: 160, speed: 55, radius: 16, color: '#84CC16' },
  { id: 'bull-fighter', name: 'Bull Fighter', mapId: 'lorencia', level: 8, maxHp: 170, attack: 16, defense: 6, expReward: 68, zenReward: 260, speed: 62, radius: 18, color: '#DC2626' },
  { id: 'hound', name: 'Hound', mapId: 'lorencia', level: 11, maxHp: 220, attack: 20, defense: 8, expReward: 95, zenReward: 340, speed: 74, radius: 17, color: '#F97316' },
  { id: 'elite-skeleton', name: 'Elite Skeleton', mapId: 'lorencia', level: 16, maxHp: 320, attack: 27, defense: 11, expReward: 145, zenReward: 520, speed: 65, radius: 18, color: '#E2E8F0' },
  { id: 'red-dragon', name: 'Red Dragon', mapId: 'lorencia', level: 24, maxHp: 1200, attack: 48, defense: 18, expReward: 620, zenReward: 2500, speed: 68, radius: 28, color: '#EF4444', isBoss: true, guaranteedJewel: true },

  // Dungeon
  { id: 'skeleton-archer', name: 'Skeleton Archer', mapId: 'dungeon', level: 22, maxHp: 420, attack: 38, defense: 14, expReward: 210, zenReward: 740, speed: 64, radius: 18, color: '#CBD5E1' },
  { id: 'ghost', name: 'Dark Ghost', mapId: 'dungeon', level: 28, maxHp: 560, attack: 48, defense: 18, expReward: 290, zenReward: 980, speed: 76, radius: 19, color: '#A855F7' },
  { id: 'poison-bull', name: 'Poison Bull', mapId: 'dungeon', level: 34, maxHp: 780, attack: 58, defense: 22, expReward: 380, zenReward: 1300, speed: 65, radius: 20, color: '#22C55E' },
  { id: 'gorgon', name: 'Gorgon', mapId: 'dungeon', level: 44, maxHp: 2600, attack: 88, defense: 30, expReward: 1400, zenReward: 5200, speed: 66, radius: 30, color: '#9333EA', isBoss: true, guaranteedJewel: true },

  // Devias
  { id: 'ice-monster', name: 'Ice Monster', mapId: 'devias', level: 42, maxHp: 980, attack: 72, defense: 26, expReward: 520, zenReward: 1800, speed: 62, radius: 20, color: '#38BDF8' },
  { id: 'yeti', name: 'Elite Yeti', mapId: 'devias', level: 50, maxHp: 1350, attack: 86, defense: 32, expReward: 740, zenReward: 2400, speed: 68, radius: 22, color: '#E0F2FE' },
  { id: 'ice-queen', name: 'Ice Queen', mapId: 'devias', level: 65, maxHp: 4200, attack: 122, defense: 42, expReward: 2400, zenReward: 8500, speed: 72, radius: 30, color: '#0EA5E9', isBoss: true, guaranteedJewel: true },

  // Noria
  { id: 'forest-goblin', name: 'Forest Goblin', mapId: 'noria', level: 68, maxHp: 1750, attack: 112, defense: 38, expReward: 980, zenReward: 3200, speed: 74, radius: 18, color: '#4ADE80' },
  { id: 'stone-golem', name: 'Stone Golem', mapId: 'noria', level: 78, maxHp: 2450, attack: 138, defense: 48, expReward: 1350, zenReward: 4200, speed: 56, radius: 24, color: '#64748B' },
  { id: 'bali', name: 'Bali Guardian', mapId: 'noria', level: 92, maxHp: 6400, attack: 175, defense: 58, expReward: 3800, zenReward: 13000, speed: 70, radius: 32, color: '#10B981', isBoss: true, guaranteedJewel: true },
];

export const DROP_ITEM_POOL: Omit<Item, 'id'>[] = [
  { templateId: 'jewel-bless', name: 'Jewel of Bless', slot: 'jewel', rarity: 'excellent', plus: 0, jewelType: 'bless', classReq: 'all', colorHex: '#A855F7', valueZen: 4500 },
  { templateId: 'jewel-soul', name: 'Jewel of Soul', slot: 'jewel', rarity: 'excellent', plus: 0, jewelType: 'soul', classReq: 'all', colorHex: '#EC4899', valueZen: 6000 },
  { templateId: 'jewel-chaos', name: 'Jewel of Chaos', slot: 'jewel', rarity: 'mythic', plus: 0, jewelType: 'chaos', classReq: 'all', colorHex: '#F59E0B', valueZen: 8500 },
  { templateId: 'hp-potion', name: 'Large Healing Potion', slot: 'potion', rarity: 'normal', plus: 0, potionType: 'hp', classReq: 'all', colorHex: '#EF4444', valueZen: 150 },
  { templateId: 'mp-potion', name: 'Large Mana Potion', slot: 'potion', rarity: 'normal', plus: 0, potionType: 'mp', classReq: 'all', colorHex: '#3B82F6', valueZen: 150 },
  { templateId: 'blade-of-archangel', name: 'Archangel Sword', slot: 'weapon', rarity: 'excellent', plus: 3, attack: 46, speedBonus: 12, classReq: 'dk', colorHex: '#F59E0B', valueZen: 9500 },
  { templateId: 'dragon-armor', name: 'Dragon Armor', slot: 'armor', rarity: 'excellent', plus: 2, defense: 34, hpBonus: 110, classReq: 'dk', colorHex: '#EF4444', valueZen: 7800 },
  { templateId: 'dragon-helm', name: 'Dragon Helm', slot: 'helm', rarity: 'normal', plus: 1, defense: 20, hpBonus: 55, classReq: 'dk', colorHex: '#DC2626', valueZen: 4200 },
  { templateId: 'viper-staff', name: 'Grand Viper Staff', slot: 'weapon', rarity: 'excellent', plus: 3, attack: 24, magic: 52, classReq: 'dw', colorHex: '#6366F1', valueZen: 9800 },
  { templateId: 'grand-soul-armor', name: 'Grand Soul Armor', slot: 'armor', rarity: 'excellent', plus: 2, defense: 28, hpBonus: 90, classReq: 'dw', colorHex: '#3B82F6', valueZen: 7600 },
  { templateId: 'viper-bow', name: 'Sylph Wind Bow', slot: 'weapon', rarity: 'excellent', plus: 3, attack: 48, speedBonus: 18, classReq: 'elf', colorHex: '#10B981', valueZen: 9400 },
  { templateId: 'guardian-armor', name: 'Guardian Armor', slot: 'armor', rarity: 'excellent', plus: 2, defense: 30, hpBonus: 95, classReq: 'elf', colorHex: '#059669', valueZen: 7500 },
  { templateId: 'plate-shield', name: 'Dragon Slayer Shield', slot: 'offhand', rarity: 'normal', plus: 2, defense: 24, hpBonus: 45, classReq: 'all', colorHex: '#94A3B8', valueZen: 3900 },
  { templateId: 'pendant-lightning', name: 'Pendant of Lighting', slot: 'pendant', rarity: 'ancient', plus: 0, attack: 16, magic: 16, speedBonus: 8, classReq: 'all', colorHex: '#38BDF8', valueZen: 8200 },
  { templateId: 'wings-satan', name: 'Wings of Satan', slot: 'wings', rarity: 'excellent', plus: 2, attack: 22, defense: 18, speedBonus: 14, classReq: 'all', colorHex: '#F43F5E', valueZen: 18000 },
];

export const BUILT_IN_MODS: ModPackage[] = [
  {
    id: 'mod-fast-x50',
    name: 'Fast Server x50',
    author: 'MU Community',
    version: '1.2.0',
    description: 'Acelera EXP x12, Zen x8 y probabilidad de joyas +60% ideal para sesiones rápidas en móvil.',
    enabled: true,
    builtIn: true,
    expMultiplier: 12,
    zenMultiplier: 8,
    dropRateMultiplier: 1.6,
    chaosSuccessBonus: 15,
    resetLevel: 120,
    customItems: [],
    customMonsters: [],
  },
  {
    id: 'mod-conqueror-wings',
    name: 'Conqueror Wings & Godlike Pack',
    author: 'Kundun Forge',
    version: '2.0.1',
    description: 'Añade Alas de Conquistador +9, Bone Blade mítico y Set Dark Phoenix a la tabla de drops.',
    enabled: true,
    builtIn: true,
    expMultiplier: 1.5,
    zenMultiplier: 1.5,
    dropRateMultiplier: 1.3,
    chaosSuccessBonus: 10,
    resetLevel: 400,
    customItems: [
      {
        templateId: 'mod-wings-conqueror',
        name: 'Wings of Conqueror',
        slot: 'wings',
        rarity: 'mythic',
        basePlus: 9,
        attack: 68,
        magic: 68,
        defense: 48,
        hpBonus: 260,
        speedBonus: 25,
        classReq: 'all',
        colorHex: '#FBBF24',
        valueZen: 45000,
      },
      {
        templateId: 'mod-bone-blade',
        name: 'Mythic Bone Blade',
        slot: 'weapon',
        rarity: 'mythic',
        basePlus: 7,
        attack: 96,
        magic: 96,
        speedBonus: 16,
        classReq: 'all',
        colorHex: '#F43F5E',
        valueZen: 38000,
      },
    ],
    customMonsters: [],
  },
  {
    id: 'mod-golden-invasion',
    name: 'Golden Invasion Bosses',
    author: 'Lorencia Vault',
    version: '1.0.4',
    description: 'Invoca al Golden Tantallos y Golden Budge Dragon con drop garantizado de Joyas.',
    enabled: true,
    builtIn: true,
    expMultiplier: 2,
    zenMultiplier: 3,
    dropRateMultiplier: 1.4,
    chaosSuccessBonus: 0,
    resetLevel: 400,
    customItems: [],
    customMonsters: [
      {
        id: 'mod-golden-budge',
        name: 'Golden Budge Dragon',
        mapId: 'lorencia',
        level: 20,
        maxHp: 950,
        attack: 36,
        defense: 14,
        expReward: 520,
        zenReward: 3200,
        color: '#FBBF24',
        isBoss: true,
      },
      {
        id: 'mod-golden-tantallos',
        name: 'Golden Tantallos',
        mapId: 'custom',
        level: 85,
        maxHp: 7500,
        attack: 165,
        defense: 55,
        expReward: 4800,
        zenReward: 18000,
        color: '#F59E0B',
        isBoss: true,
      },
    ],
  },
  {
    id: 'mod-classic-x1',
    name: 'Classic Hardcore x1',
    author: 'Webzen Oldschool',
    version: '1.0.0',
    description: 'Desactiva los bonos de velocidad para experimentar el progreso clásico lento.',
    enabled: false,
    builtIn: true,
    expMultiplier: 0.25,
    zenMultiplier: 0.3,
    dropRateMultiplier: 0.7,
    chaosSuccessBonus: -5,
    resetLevel: 400,
    customItems: [],
    customMonsters: [],
  },
];

export function createStarterHero(name: string, classId: ClassId): HeroSave {
  const meta = CLASS_CATALOG[classId];
  const uid = () => `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;

  return {
    id: `hero-${uid()}`,
    name: name.trim() || meta.name,
    classId,
    level: 1,
    resets: 0,
    exp: 0,
    expToNext: 120,
    zen: 5000,
    hp: meta.baseHp,
    maxHp: meta.baseHp,
    mp: meta.baseMp,
    maxMp: meta.baseMp,
    statPoints: 10,
    stats: { ...meta.baseStats },
    mapId: 'lorencia',
    x: 480,
    y: 480,
    equipment: {
      weapon: { ...meta.starterWeapon, id: uid() },
      offhand: null,
      armor: { ...meta.starterArmor, id: uid() },
      helm: null,
      wings: null,
      pendant: null,
    },
    inventory: [
      {
        id: uid(),
        templateId: 'hp-potion',
        name: 'Large Healing Potion',
        slot: 'potion',
        rarity: 'normal',
        plus: 0,
        potionType: 'hp',
        classReq: 'all',
        colorHex: '#EF4444',
        valueZen: 150,
        quantity: 8,
      },
      {
        id: uid(),
        templateId: 'mp-potion',
        name: 'Large Mana Potion',
        slot: 'potion',
        rarity: 'normal',
        plus: 0,
        potionType: 'mp',
        classReq: 'all',
        colorHex: '#3B82F6',
        valueZen: 150,
        quantity: 6,
      },
      {
        id: uid(),
        templateId: 'jewel-bless',
        name: 'Jewel of Bless',
        slot: 'jewel',
        rarity: 'excellent',
        plus: 0,
        jewelType: 'bless',
        classReq: 'all',
        colorHex: '#A855F7',
        valueZen: 4500,
        quantity: 2,
      },
      {
        id: uid(),
        templateId: 'jewel-chaos',
        name: 'Jewel of Chaos',
        slot: 'jewel',
        rarity: 'mythic',
        plus: 0,
        jewelType: 'chaos',
        classReq: 'all',
        colorHex: '#F59E0B',
        valueZen: 8500,
        quantity: 1,
      },
    ],
    kills: 0,
    updatedAt: Date.now(),
  };
}

export function computeDerivedHeroStats(hero: HeroSave) {
  const eq = hero.equipment;
  let itemAtk = 0;
  let itemMagic = 0;
  let itemDef = 0;
  let itemHp = 0;
  let itemSpeed = 0;

  const slots = [eq.weapon, eq.offhand, eq.armor, eq.helm, eq.wings, eq.pendant];
  for (const item of slots) {
    if (!item) continue;
    const plusBonus = 1 + item.plus * 0.14;
    itemAtk += Math.round((item.attack || 0) * plusBonus);
    itemMagic += Math.round((item.magic || 0) * plusBonus);
    itemDef += Math.round((item.defense || 0) * plusBonus);
    itemHp += item.hpBonus || 0;
    itemSpeed += item.speedBonus || 0;
  }

  const baseMeta = CLASS_CATALOG[hero.classId];
  const attackPower =
    hero.classId === 'dw'
      ? Math.round(hero.stats.ene * 1.35 + itemMagic + itemAtk * 0.4 + hero.level * 1.4)
      : hero.classId === 'elf'
      ? Math.round(hero.stats.agi * 1.25 + hero.stats.str * 0.45 + itemAtk + hero.level * 1.4)
      : Math.round(hero.stats.str * 1.3 + hero.stats.agi * 0.35 + itemAtk + hero.level * 1.5);

  const defense = Math.round(hero.stats.agi * 0.45 + hero.stats.vit * 0.3 + itemDef + hero.level * 0.9);
  const maxHp = Math.round(baseMeta.baseHp + hero.stats.vit * 6 + hero.level * 14 + itemHp);
  const maxMp = Math.round(baseMeta.baseMp + hero.stats.ene * 4 + hero.level * 8);
  const critChance = Math.min(0.45, 0.08 + hero.stats.agi * 0.0012 + (eq.wings ? 0.06 : 0));
  const moveSpeed = Math.min(275, 165 + itemSpeed + Math.floor(hero.stats.agi * 0.25));

  return { attackPower, defense, maxHp, maxMp, critChance, moveSpeed };
}
