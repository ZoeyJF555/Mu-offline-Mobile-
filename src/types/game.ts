export type ClassId = 'dk' | 'dw' | 'elf';

export type MapId = 'lorencia' | 'dungeon' | 'devias' | 'noria' | 'custom';

export type ItemSlot = 'weapon' | 'offhand' | 'armor' | 'helm' | 'wings' | 'pendant' | 'jewel' | 'potion';

export type ItemRarity = 'normal' | 'excellent' | 'ancient' | 'mythic';

export interface AccountRecord {
  accountId: string;
  passwordHash: string;
  createdAt: number;
}

export interface Item {
  id: string;
  templateId: string;
  name: string;
  slot: ItemSlot;
  rarity: ItemRarity;
  plus: number; // +0 to +13
  attack?: number;
  defense?: number;
  magic?: number;
  speedBonus?: number;
  hpBonus?: number;
  jewelType?: 'bless' | 'soul' | 'chaos' | 'life';
  potionType?: 'hp' | 'mp';
  classReq?: ClassId | 'all';
  colorHex: string;
  valueZen: number;
  quantity?: number;
}

export interface EquipSlots {
  weapon: Item | null;
  offhand: Item | null;
  armor: Item | null;
  helm: Item | null;
  wings: Item | null;
  pendant: Item | null;
}

export interface HeroStats {
  str: number;
  agi: number;
  vit: number;
  ene: number;
}

export interface HeroSave {
  id: string;
  accountId?: string;
  name: string;
  classId: ClassId;
  level: number;
  resets: number;
  exp: number;
  expToNext: number;
  zen: number;
  hp: number;
  maxHp: number;
  mp: number;
  maxMp: number;
  statPoints: number;
  stats: HeroStats;
  mapId: MapId;
  x: number;
  y: number;
  equipment: EquipSlots;
  inventory: Item[];
  kills: number;
  updatedAt: number;
}

export interface SkillDef {
  id: string;
  name: string;
  classId: ClassId;
  mpCost: number;
  cooldownMs: number;
  damageMultiplier: number;
  range: number;
  aoeRadius: number;
  color: string;
  description: string;
}

export interface MonsterTemplate {
  id: string;
  name: string;
  mapId: MapId;
  level: number;
  maxHp: number;
  attack: number;
  defense: number;
  expReward: number;
  zenReward: number;
  speed: number;
  radius: number;
  color: string;
  isBoss?: boolean;
  guaranteedJewel?: boolean;
}

export interface LiveMonster {
  uid: string;
  templateId: string;
  name: string;
  mapId: MapId;
  level: number;
  hp: number;
  maxHp: number;
  attack: number;
  defense: number;
  expReward: number;
  zenReward: number;
  speed: number;
  radius: number;
  color: string;
  isBoss: boolean;
  guaranteedJewel?: boolean;
  x: number;
  y: number;
  homeX: number;
  homeY: number;
  respawnAt: number;
  hitFlashUntil: number;
  attackCooldownUntil: number;
}

export interface GroundDrop {
  uid: string;
  x: number;
  y: number;
  zen?: number;
  item?: Item;
  createdAt: number;
}

export interface FloatingText {
  id: string;
  x: number;
  y: number;
  text: string;
  color: string;
  isCrit?: boolean;
  createdAt: number;
}

export interface SkillVfx {
  id: string;
  skillId: string;
  x: number;
  y: number;
  targetX: number;
  targetY: number;
  color: string;
  radius: number;
  createdAt: number;
  durationMs: number;
}

export interface ModItemDef {
  templateId: string;
  name: string;
  slot: Exclude<ItemSlot, 'jewel' | 'potion'>;
  rarity: ItemRarity;
  basePlus: number;
  attack?: number;
  defense?: number;
  magic?: number;
  hpBonus?: number;
  speedBonus?: number;
  classReq: ClassId | 'all';
  colorHex: string;
  valueZen: number;
}

export interface ModMonsterDef {
  id: string;
  name: string;
  mapId: MapId;
  level: number;
  maxHp: number;
  attack: number;
  defense: number;
  expReward: number;
  zenReward: number;
  color: string;
  isBoss: boolean;
}

export interface ModPackage {
  id: string;
  name: string;
  author: string;
  version: string;
  description: string;
  enabled: boolean;
  builtIn?: boolean;
  expMultiplier: number;
  zenMultiplier: number;
  dropRateMultiplier: number;
  chaosSuccessBonus: number;
  resetLevel: number;
  customItems: ModItemDef[];
  customMonsters: ModMonsterDef[];
}

export interface LanPlayer {
  id: string;
  name: string;
  classId: ClassId;
  level: number;
  resets: number;
  mapId: string;
  x: number;
  y: number;
  dir: number;
  wingsTier: number;
  weaponPlus: number;
  hp: number;
  maxHp: number;
}

export interface LanBossState {
  id: string;
  name: string;
  mapId: string;
  hp: number;
  maxHp: number;
  x: number;
  y: number;
  respawnAt: number;
}

export interface LanEventState {
  id: string;
  name: string;
  mapId: MapId;
  active: boolean;
  endsAt: number;
  startedBy: string;
  rewardZen: number;
}

export interface TradeSession {
  tradeId: string;
  partnerId: string;
  partnerName: string;
  myItems: Item[];
  myZen: number;
  myConfirmed: boolean;
  partnerItems: Item[];
  partnerZen: number;
  partnerConfirmed: boolean;
}

export interface IncomingTradeRequest {
  tradeId: string;
  fromPlayerId: string;
  fromPlayerName: string;
}

export interface LanChatMessage {
  id: string;
  senderId: string;
  senderName: string;
  classId: ClassId;
  text: string;
  timestamp: number;
}
