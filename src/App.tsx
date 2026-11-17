import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  BASE_MONSTERS,
  CLASS_CATALOG,
  DROP_ITEM_POOL,
  SKILLS_BY_CLASS,
  computeDerivedHeroStats,
} from './data/muCatalog';
import type {
  EquipSlots,
  FloatingText,
  GroundDrop,
  HeroSave,
  HeroStats,
  IncomingTradeRequest,
  Item,
  LanBossState,
  LanChatMessage,
  LanEventState,
  LanPlayer,
  LiveMonster,
  MapId,
  ModPackage,
  MonsterTemplate,
  SkillVfx,
  TradeSession,
} from './types/game';
import {
  deleteHero as deleteHeroFromDb,
  getActiveSessionAccount,
  loadMods,
  loadSavedHeroes,
  logoutAccount,
  saveHero,
  saveMods,
} from './utils/storage';
import { soundFx } from './utils/soundFx';
import { Smartphone, X } from 'lucide-react';
import { AuthScreen } from './components/AuthScreen';
import { TitleScreen } from './components/TitleScreen';
import { GameCanvas } from './components/GameCanvas';
import { MobileHUD } from './components/MobileHUD';
import { CharacterModal } from './components/CharacterModal';
import { InventoryChaosModal } from './components/InventoryChaosModal';
import { ModsModal } from './components/ModsModal';
import { LanModal } from './components/LanModal';
import { TradeModal } from './components/TradeModal';
import { NpcShopModal } from './components/NpcShopModal';
import { CreditsModal } from './components/CreditsModal';
import type { LorenciaNpcDef } from './three/muModels';

const SAFE_CENTER = 480;
const SAFE_RADIUS = 135;
const WORLD_SIZE = 1400;

function spawnMonstersForMap(mapId: MapId, mods: ModPackage[]): LiveMonster[] {
  const modTemplates: MonsterTemplate[] = mods
    .filter((m) => m.enabled)
    .flatMap((m) =>
      m.customMonsters.map((cm) => ({
        id: cm.id,
        name: cm.name,
        mapId: cm.mapId,
        level: cm.level,
        maxHp: cm.maxHp,
        attack: cm.attack,
        defense: cm.defense,
        expReward: cm.expReward,
        zenReward: cm.zenReward,
        speed: 68,
        radius: cm.isBoss ? 28 : 18,
        color: cm.color,
        isBoss: cm.isBoss,
        guaranteedJewel: cm.isBoss,
      }))
    );

  const templates = [...BASE_MONSTERS, ...modTemplates].filter((m) => m.mapId === mapId);
  if (templates.length === 0) {
    templates.push(...BASE_MONSTERS.filter((m) => m.mapId === 'lorencia'));
  }

  const list: LiveMonster[] = [];
  for (const tpl of templates) {
    const count = tpl.isBoss ? 1 : 4;
    for (let i = 0; i < count; i++) {
      let x = 200;
      let y = 200;
      for (let attempt = 0; attempt < 20; attempt++) {
        x = 120 + Math.random() * (WORLD_SIZE - 240);
        y = 120 + Math.random() * (WORLD_SIZE - 240);
        if (Math.hypot(x - SAFE_CENTER, y - SAFE_CENTER) > SAFE_RADIUS + 110) break;
      }
      list.push({
        uid: `${tpl.id}-${i}-${Math.random().toString(36).slice(2, 6)}`,
        templateId: tpl.id,
        name: tpl.name,
        mapId,
        level: tpl.level,
        hp: tpl.maxHp,
        maxHp: tpl.maxHp,
        attack: tpl.attack,
        defense: tpl.defense,
        expReward: tpl.expReward,
        zenReward: tpl.zenReward,
        speed: tpl.speed,
        radius: tpl.radius,
        color: tpl.color,
        isBoss: !!tpl.isBoss,
        guaranteedJewel: tpl.guaranteedJewel,
        x,
        y,
        homeX: x,
        homeY: y,
        respawnAt: 0,
        hitFlashUntil: 0,
        attackCooldownUntil: 0,
      });
    }
  }
  return list;
}

export default function App() {
  const [accountId, setAccountId] = useState<string | null>(() => getActiveSessionAccount());
  const [heroes, setHeroes] = useState<HeroSave[]>([]);
  const [mods, setMods] = useState<ModPackage[]>([]);
  const [activeHero, setActiveHero] = useState<HeroSave | null>(null);
  const [ready, setReady] = useState(false);

  // Modals & Orientation
  const [showCharModal, setShowCharModal] = useState(false);
  const [showInvModal, setShowInvModal] = useState(false);
  const [showModsModal, setShowModsModal] = useState(false);
  const [showLanModal, setShowLanModal] = useState(false);
  const [showCreditsModal, setShowCreditsModal] = useState(false);
  const [activeNpc, setActiveNpc] = useState<LorenciaNpcDef | null>(null);
  const [isPortraitMobile, setIsPortraitMobile] = useState(
    () => typeof window !== 'undefined' && window.innerHeight > window.innerWidth && window.innerWidth < 860
  );
  const [dismissedRotatePrompt, setDismissedRotatePrompt] = useState(false);

  // Gameplay World State
  const [monsters, setMonsters] = useState<LiveMonster[]>([]);
  const [drops, setDrops] = useState<GroundDrop[]>([]);
  const [floatingTexts, setFloatingTexts] = useState<FloatingText[]>([]);
  const [vfxList, setVfxList] = useState<SkillVfx[]>([]);
  const [skillCooldowns, setSkillCooldowns] = useState<Record<string, number>>({});
  const [autoHunt, setAutoHunt] = useState(false);
  const [muted, setMuted] = useState(false);

  // Wi-Fi LAN Multiplayer, Events & Trade State
  const [connectedRoom, setConnectedRoom] = useState<string | null>(null);
  const [lanStatus, setLanStatus] = useState<'disconnected' | 'connecting' | 'connected' | 'error'>('disconnected');
  const [lanError, setLanError] = useState<string | null>(null);
  const [lanPlayers, setLanPlayers] = useState<LanPlayer[]>([]);
  const [lanBosses, setLanBosses] = useState<LanBossState[]>([]);
  const [activeEvent, setActiveEvent] = useState<LanEventState | null>(null);
  const [incomingTrade, setIncomingTrade] = useState<IncomingTradeRequest | null>(null);
  const [tradeSession, setTradeSession] = useState<TradeSession | null>(null);
  const [chatMessages, setChatMessages] = useState<LanChatMessage[]>([]);
  const [gamepadName, setGamepadName] = useState<string | null>(null);

  const joystickRef = useRef({ x: 0, y: 0 });
  const wsRef = useRef<WebSocket | null>(null);
  const heroRef = useRef<HeroSave | null>(null);
  const monstersRef = useRef<LiveMonster[]>([]);
  const dropsRef = useRef<GroundDrop[]>([]);
  const autoHuntRef = useRef(false);
  const lastAttackAtRef = useRef(0);
  const lastLanSyncRef = useRef(0);

  heroRef.current = activeHero;
  monstersRef.current = monsters;
  dropsRef.current = drops;
  autoHuntRef.current = autoHunt;

  // Ctrl+F8 Credits Shortcut & Mobile Portrait Detection
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.key === 'F8') {
        e.preventDefault();
        setShowCreditsModal((v) => !v);
      }
    };
    const checkOrientation = () => {
      const portrait = window.innerHeight > window.innerWidth && window.innerWidth < 860;
      setIsPortraitMobile(portrait);
      if (!portrait) setDismissedRotatePrompt(false);
    };
    window.addEventListener('keydown', onKey);
    window.addEventListener('resize', checkOrientation);
    window.addEventListener('orientationchange', checkOrientation);
    return () => {
      window.removeEventListener('keydown', onKey);
      window.removeEventListener('resize', checkOrientation);
      window.removeEventListener('orientationchange', checkOrientation);
    };
  }, []);

  // Boot Mods & Heroes for current account
  useEffect(() => {
    Promise.all([
      accountId ? loadSavedHeroes(accountId) : Promise.resolve([]),
      loadMods(),
    ]).then(([loadedHeroes, loadedMods]) => {
      setHeroes(loadedHeroes);
      setMods(loadedMods);
      setReady(true);
    });
  }, [accountId]);

  // Combined Active Mod Multipliers
  const activeModRates = useMemo(() => {
    const enabled = mods.filter((m) => m.enabled);
    if (enabled.length === 0) {
      return { expMul: 1, zenMul: 1, dropMul: 1, chaosBonus: 0, resetLevel: 400 };
    }
    const expMul = enabled.reduce((acc, m) => acc * m.expMultiplier, 1);
    const zenMul = enabled.reduce((acc, m) => acc * m.zenMultiplier, 1);
    const dropMul = enabled.reduce((acc, m) => acc * m.dropRateMultiplier, 1);
    const chaosBonus = enabled.reduce((acc, m) => acc + m.chaosSuccessBonus, 0);
    const resetLevel = Math.min(...enabled.map((m) => m.resetLevel));
    return {
      expMul: Math.max(0.1, Math.min(250, expMul)),
      zenMul: Math.max(0.1, Math.min(250, zenMul)),
      dropMul: Math.max(0.2, Math.min(10, dropMul)),
      chaosBonus,
      resetLevel,
    };
  }, [mods]);

  const pushFloatingText = useCallback((x: number, y: number, text: string, color: string, isCrit = false) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
    setFloatingTexts((prev) => [...prev.slice(-24), { id, x, y, text, color, isCrit, createdAt: performance.now() }]);
  }, []);

  const pushVfx = useCallback((skillId: string, x: number, y: number, targetX: number, targetY: number, color: string, radius: number) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
    setVfxList((prev) => [...prev.slice(-16), { id, skillId, x, y, targetX, targetY, color, radius, createdAt: performance.now(), durationMs: 360 }]);
  }, []);

  // Persist active hero periodically offline
  useEffect(() => {
    if (!activeHero) return;
    const timer = setInterval(() => {
      if (heroRef.current) {
        saveHero(heroRef.current);
      }
    }, 3500);
    return () => clearInterval(timer);
  }, [activeHero?.id]);

  // Spawn monsters whenever hero enters or switches map
  useEffect(() => {
    if (!activeHero) return;
    setMonsters(spawnMonstersForMap(activeHero.mapId, mods));
    setDrops([]);
  }, [activeHero?.id, activeHero?.mapId, mods]);

  const addItemToHero = useCallback((hero: HeroSave, item: Item): HeroSave => {
    const inv = [...hero.inventory];
    if (item.slot === 'potion' || item.slot === 'jewel') {
      const idx = inv.findIndex((i) => i.templateId === item.templateId);
      if (idx >= 0) {
        inv[idx] = { ...inv[idx], quantity: (inv[idx].quantity || 1) + (item.quantity || 1) };
        return { ...hero, inventory: inv };
      }
    }
    if (inv.length >= 24) return hero;
    return { ...hero, inventory: [...inv, item] };
  }, []);

  const applyRewards = useCallback(
    (hero: HeroSave, gainedExp: number, gainedZen: number): { updated: HeroSave; leveledUp: boolean } => {
      let exp = hero.exp + gainedExp;
      let level = hero.level;
      let expToNext = hero.expToNext;
      let statPoints = hero.statPoints;
      let leveledUp = false;

      while (exp >= expToNext && level < 400) {
        exp -= expToNext;
        level += 1;
        statPoints += 5;
        expToNext = Math.round(120 * Math.pow(level, 1.38));
        leveledUp = true;
      }

      const nextHero: HeroSave = {
        ...hero,
        level,
        exp,
        expToNext,
        statPoints,
        zen: hero.zen + gainedZen,
        kills: hero.kills + 1,
      };
      const derived = computeDerivedHeroStats(nextHero);
      nextHero.maxHp = derived.maxHp;
      nextHero.maxMp = derived.maxMp;
      if (leveledUp) {
        nextHero.hp = derived.maxHp;
        nextHero.mp = derived.maxMp;
      }
      return { updated: nextHero, leveledUp };
    },
    []
  );

  const spawnDropAt = useCallback(
    (x: number, y: number, monster: LiveMonster) => {
      const zenAmount = Math.round(monster.zenReward * activeModRates.zenMul);
      const newDrops: GroundDrop[] = [
        {
          uid: `zen-${Date.now()}-${Math.random().toString(36).slice(2, 5)}`,
          x: x - 14,
          y: y + 8,
          zen: zenAmount,
          createdAt: Date.now(),
        },
      ];

      const modItems: Omit<Item, 'id'>[] = mods
        .filter((m) => m.enabled)
        .flatMap((m) =>
          m.customItems.map((ci) => ({
            templateId: ci.templateId,
            name: ci.name,
            slot: ci.slot,
            rarity: ci.rarity,
            plus: ci.basePlus,
            attack: ci.attack,
            defense: ci.defense,
            magic: ci.magic,
            hpBonus: ci.hpBonus,
            speedBonus: ci.speedBonus,
            classReq: ci.classReq,
            colorHex: ci.colorHex,
            valueZen: ci.valueZen,
          }))
        );

      const dropRoll = Math.random();
      const baseChance = monster.guaranteedJewel ? 1 : Math.min(0.9, 0.34 * activeModRates.dropMul);

      if (dropRoll <= baseChance) {
        const pool = [...DROP_ITEM_POOL, ...modItems];
        let picked = pool[Math.floor(Math.random() * pool.length)];
        if (monster.guaranteedJewel) {
          const jewelsAndMyths = pool.filter((p) => p.slot === 'jewel' || p.rarity === 'mythic' || p.slot === 'wings');
          picked = jewelsAndMyths[Math.floor(Math.random() * jewelsAndMyths.length)] || picked;
        }
        const item: Item = {
          ...picked,
          id: `item-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
          quantity: 1,
        };
        if (item.slot === 'jewel') soundFx.playJewelDrop();
        newDrops.push({
          uid: `drop-${Date.now()}-${Math.random().toString(36).slice(2, 5)}`,
          x: x + 16,
          y: y - 6,
          item,
          createdAt: Date.now(),
        });
      }

      setDrops((prev) => [...prev.slice(-30), ...newDrops]);
    },
    [activeModRates.dropMul, activeModRates.zenMul, mods]
  );

  const executeCombat = useCallback(
    (skillId?: string) => {
      const hero = heroRef.current;
      if (!hero) return;
      const nowMs = Date.now();
      const derived = computeDerivedHeroStats(hero);
      const classMeta = CLASS_CATALOG[hero.classId];

      let dmgMultiplier = 1;
      let range = classMeta.attackRange;
      let aoeRadius = 45;
      let mpCost = 0;
      let vfxColor = classMeta.accentColor;

      if (skillId) {
        const skill = SKILLS_BY_CLASS[hero.classId].find((s) => s.id === skillId);
        if (!skill) return;
        if ((skillCooldowns[skill.id] || 0) > nowMs) return;
        if (hero.mp < skill.mpCost) {
          pushFloatingText(hero.x, hero.y - 22, 'Sin Mana', '#60A5FA');
          return;
        }
        mpCost = skill.mpCost;
        dmgMultiplier = skill.damageMultiplier;
        range = Math.max(range, skill.range);
        aoeRadius = skill.aoeRadius;
        vfxColor = skill.color;
        setSkillCooldowns((prev) => ({ ...prev, [skill.id]: nowMs + skill.cooldownMs }));
        soundFx.playSkill();

        if (skill.id === 'heal-aura') {
          const healAmt = Math.round(derived.maxHp * 0.42);
          setActiveHero((prev) =>
            prev ? { ...prev, mp: prev.mp - mpCost, hp: Math.min(derived.maxHp, prev.hp + healAmt) } : prev
          );
          pushFloatingText(hero.x, hero.y - 20, `+${healAmt} HP`, '#34D399', true);
          pushVfx(skill.id, hero.x, hero.y, hero.x, hero.y, skill.color, skill.aoeRadius);
          return;
        }
      } else {
        if (nowMs - lastAttackAtRef.current < 320) return;
        lastAttackAtRef.current = nowMs;
        soundFx.playAttack(hero.classId);
      }

      const alive = monstersRef.current.filter((m) => m.hp > 0);
      let primary: LiveMonster | null = null;
      let bestDist = Infinity;
      for (const m of alive) {
        const d = Math.hypot(m.x - hero.x, m.y - hero.y);
        if (d <= range + m.radius && d < bestDist) {
          bestDist = d;
          primary = m;
        }
      }

      const targetX = primary ? primary.x : hero.x;
      const targetY = primary ? primary.y : hero.y;
      pushVfx(skillId || 'basic', hero.x, hero.y, targetX, targetY, vfxColor, aoeRadius);

      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(
          JSON.stringify({
            type: 'player:skill',
            skillId: skillId || 'basic',
            x: hero.x,
            y: hero.y,
            targetX,
            targetY,
          })
        );
      }

      for (const lb of lanBosses) {
        if (lb.mapId === hero.mapId && lb.hp > 0 && Math.hypot(lb.x - targetX, lb.y - targetY) <= aoeRadius + 30) {
          const dmg = Math.max(10, Math.round(derived.attackPower * dmgMultiplier));
          wsRef.current?.send(JSON.stringify({ type: 'boss:hit', bossId: lb.id, damage: dmg }));
          pushFloatingText(lb.x, lb.y - 24, `-${dmg}`, '#38BDF8', true);
        }
      }

      if (!primary) {
        if (mpCost > 0) {
          setActiveHero((prev) => (prev ? { ...prev, mp: Math.max(0, prev.mp - mpCost) } : prev));
        }
        return;
      }

      let totalExp = 0;
      let totalZen = 0;
      const killed: LiveMonster[] = [];

      setMonsters((prevMonsters) =>
        prevMonsters.map((m) => {
          if (m.hp <= 0) return m;
          const distToImpact = Math.hypot(m.x - targetX, m.y - targetY);
          const hit = m.uid === primary!.uid || (skillId && distToImpact <= aoeRadius + m.radius);
          if (!hit) return m;

          const isCrit = Math.random() < derived.critChance;
          const rawDmg = derived.attackPower * dmgMultiplier - m.defense * 0.45;
          const finalDmg = Math.max(4, Math.round(rawDmg * (isCrit ? 1.65 : 0.92 + Math.random() * 0.16)));
          const newHp = Math.max(0, m.hp - finalDmg);
          pushFloatingText(
            m.x,
            m.y - m.radius,
            isCrit ? `${finalDmg}!` : `${finalDmg}`,
            isCrit ? '#38BDF8' : '#FBBF24',
            isCrit
          );

          if (newHp === 0) {
            totalExp += Math.round(m.expReward * activeModRates.expMul);
            totalZen += Math.round(m.zenReward * 0.35 * activeModRates.zenMul);
            killed.push(m);
            return { ...m, hp: 0, respawnAt: Date.now() + (m.isBoss ? 18000 : 6500) };
          }
          return { ...m, hp: newHp, hitFlashUntil: performance.now() + 110 };
        })
      );

      for (const km of killed) {
        spawnDropAt(km.x, km.y, km);
      }

      setActiveHero((prev) => {
        if (!prev) return prev;
        let next: HeroSave = { ...prev, mp: Math.max(0, prev.mp - mpCost) };
        if (totalExp > 0 || totalZen > 0) {
          const res = applyRewards(next, totalExp, totalZen);
          next = res.updated;
          if (res.leveledUp) {
            soundFx.playLevelUp();
            pushFloatingText(next.x, next.y - 36, `LEVEL UP! Nv ${next.level}`, '#FDE68A', true);
          }
        }
        return next;
      });
    },
    [
      activeModRates.expMul,
      activeModRates.zenMul,
      applyRewards,
      lanBosses,
      pushFloatingText,
      pushVfx,
      skillCooldowns,
      spawnDropAt,
    ]
  );

  // World Simulation Tick
  useEffect(() => {
    if (!activeHero) return;
    const tick = setInterval(() => {
      const hero = heroRef.current;
      if (!hero) return;
      const now = Date.now();
      const derived = computeDerivedHeroStats(hero);
      const inSafeZone = Math.hypot(hero.x - SAFE_CENTER, hero.y - SAFE_CENTER) <= SAFE_RADIUS;

      const collectedDrops: GroundDrop[] = [];
      const remainingDrops: GroundDrop[] = [];
      for (const d of dropsRef.current) {
        if (Math.hypot(d.x - hero.x, d.y - hero.y) <= 44) {
          collectedDrops.push(d);
        } else if (now - d.createdAt < 45000) {
          remainingDrops.push(d);
        }
      }
      if (collectedDrops.length > 0) {
        setDrops(remainingDrops);
        setActiveHero((prev) => {
          if (!prev) return prev;
          let updated = { ...prev };
          for (const d of collectedDrops) {
            if (d.zen) {
              updated.zen += d.zen;
              pushFloatingText(updated.x, updated.y - 26, `+${d.zen} Zen`, '#FBBF24');
            }
            if (d.item) {
              updated = addItemToHero(updated, d.item);
              pushFloatingText(updated.x, updated.y - 34, `+${d.item.name}`, d.item.colorHex);
            }
          }
          return updated;
        });
      }

      let damageToHero = 0;
      setMonsters((prev) =>
        prev.map((m) => {
          if (m.hp <= 0) {
            if (m.respawnAt > 0 && now >= m.respawnAt) {
              return { ...m, hp: m.maxHp, x: m.homeX, y: m.homeY, respawnAt: 0 };
            }
            return m;
          }

          const distHero = Math.hypot(hero.x - m.x, hero.y - m.y);
          let nx = m.x;
          let ny = m.y;

          if (!inSafeZone && distHero < 250 && distHero > m.radius + 20) {
            const step = m.speed * 0.1;
            nx += ((hero.x - m.x) / distHero) * step;
            ny += ((hero.y - m.y) / distHero) * step;
            const distSafe = Math.hypot(nx - SAFE_CENTER, ny - SAFE_CENTER);
            if (distSafe < SAFE_RADIUS + m.radius) {
              nx = m.x;
              ny = m.y;
            }
          } else if (inSafeZone || distHero >= 250) {
            const distHome = Math.hypot(m.homeX - m.x, m.homeY - m.y);
            if (distHome > 12) {
              nx += ((m.homeX - m.x) / distHome) * 4;
              ny += ((m.homeY - m.y) / distHome) * 4;
            }
          }

          let nextAtkCd = m.attackCooldownUntil;
          if (!inSafeZone && distHero <= m.radius + 26 && now >= m.attackCooldownUntil) {
            const mitigated = Math.max(1, Math.round(m.attack - derived.defense * 0.45));
            damageToHero += mitigated;
            nextAtkCd = now + 1150;
          }

          return { ...m, x: nx, y: ny, attackCooldownUntil: nextAtkCd };
        })
      );

      setActiveHero((prev) => {
        if (!prev) return prev;
        let hp = prev.hp - damageToHero;
        let mp = Math.min(derived.maxMp, prev.mp + (inSafeZone ? 8 : 2));
        let x = prev.x;
        let y = prev.y;

        if (damageToHero > 0) {
          pushFloatingText(prev.x, prev.y - 22, `-${damageToHero}`, '#EF4444');
        }
        if (inSafeZone) {
          hp = Math.min(derived.maxHp, hp + 12);
        }
        if (hp <= 0) {
          hp = derived.maxHp;
          mp = derived.maxMp;
          x = SAFE_CENTER;
          y = SAFE_CENTER;
          pushFloatingText(SAFE_CENTER, SAFE_CENTER - 30, 'Regresaste a Lorencia Segura', '#FBBF24');
        }
        return { ...prev, hp, mp, maxHp: derived.maxHp, maxMp: derived.maxMp, x, y };
      });

      if (autoHuntRef.current) {
        const targetDrop = dropsRef.current[0];
        const alive = monstersRef.current.filter((m) => m.hp > 0);
        let nearest: LiveMonster | null = null;
        let minD = Infinity;
        for (const m of alive) {
          const d = Math.hypot(m.x - hero.x, m.y - hero.y);
          if (d < minD) {
            minD = d;
            nearest = m;
          }
        }

        const goal = targetDrop && Math.hypot(targetDrop.x - hero.x, targetDrop.y - hero.y) < 160 ? targetDrop : nearest;
        if (goal) {
          const dist = Math.hypot(goal.x - hero.x, goal.y - hero.y);
          const atkRange = CLASS_CATALOG[hero.classId].attackRange;
          if (nearest && Math.hypot(nearest.x - hero.x, nearest.y - hero.y) <= atkRange) {
            const firstSkill = SKILLS_BY_CLASS[hero.classId][0];
            if (hero.mp >= firstSkill.mpCost) executeCombat(firstSkill.id);
            else executeCombat();
          } else if (dist > 14) {
            const step = derived.moveSpeed * 0.1;
            const nx = Math.max(24, Math.min(WORLD_SIZE - 24, hero.x + ((goal.x - hero.x) / dist) * step));
            const ny = Math.max(24, Math.min(WORLD_SIZE - 24, hero.y + ((goal.y - hero.y) / dist) * step));
            setActiveHero((prev) => (prev ? { ...prev, x: nx, y: ny } : prev));
          }
        }
      }
    }, 100);

    return () => clearInterval(tick);
  }, [activeHero?.id, addItemToHero, executeCombat, pushFloatingText]);

  useEffect(() => {
    if (!activeHero) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName || '')) return;
      const skills = SKILLS_BY_CLASS[activeHero.classId];
      if (e.code === 'Space') {
        e.preventDefault();
        executeCombat();
      } else if (e.key === '1' && skills[0]) {
        executeCombat(skills[0].id);
      } else if (e.key === '2' && skills[1]) {
        executeCombat(skills[1].id);
      } else if (e.key === '3' && skills[2]) {
        executeCombat(skills[2].id);
      } else if (e.key.toLowerCase() === 'q') {
        handleUsePotion('hp');
      } else if (e.key.toLowerCase() === 'e') {
        handleUsePotion('mp');
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [activeHero, executeCombat]);

  const handleMoveDelta = useCallback((dxNorm: number, dyNorm: number) => {
    setActiveHero((prev) => {
      if (!prev) return prev;
      const derived = computeDerivedHeroStats(prev);
      const nx = Math.max(20, Math.min(WORLD_SIZE - 20, prev.x + dxNorm * derived.moveSpeed));
      const ny = Math.max(20, Math.min(WORLD_SIZE - 20, prev.y + dyNorm * derived.moveSpeed));

      const now = Date.now();
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN && now - lastLanSyncRef.current > 85) {
        lastLanSyncRef.current = now;
        wsRef.current.send(
          JSON.stringify({
            type: 'player:move',
            x: nx,
            y: ny,
            dir: Math.atan2(dyNorm, dxNorm),
            mapId: prev.mapId,
            level: prev.level,
            resets: prev.resets,
            wingsTier: prev.equipment.wings ? (prev.equipment.wings.rarity === 'mythic' ? 2 : 1) : 0,
            weaponPlus: prev.equipment.weapon?.plus || 0,
            hp: prev.hp,
            maxHp: prev.maxHp,
          })
        );
      }

      return { ...prev, x: nx, y: ny };
    });
  }, []);

  const handleTapWorld = useCallback(
    (wx: number, wy: number) => {
      const hero = heroRef.current;
      if (!hero) return;
      const clickedMonster = monstersRef.current.find(
        (m) => m.hp > 0 && Math.hypot(m.x - wx, m.y - wy) <= m.radius + 26
      );
      if (clickedMonster && Math.hypot(clickedMonster.x - hero.x, clickedMonster.y - hero.y) <= CLASS_CATALOG[hero.classId].attackRange + 30) {
        executeCombat();
        return;
      }
      const dist = Math.hypot(wx - hero.x, wy - hero.y);
      if (dist > 8) {
        const step = Math.min(dist, 54);
        const nx = Math.max(20, Math.min(WORLD_SIZE - 20, hero.x + ((wx - hero.x) / dist) * step));
        const ny = Math.max(20, Math.min(WORLD_SIZE - 20, hero.y + ((wy - hero.y) / dist) * step));
        setActiveHero((prev) => (prev ? { ...prev, x: nx, y: ny } : prev));
      }
    },
    [executeCombat]
  );

  const handleUsePotion = (kind: 'hp' | 'mp') => {
    setActiveHero((prev) => {
      if (!prev) return prev;
      const idx = prev.inventory.findIndex((i) => i.potionType === kind);
      if (idx < 0) return prev;
      soundFx.playPotion();
      const inv = [...prev.inventory];
      const pot = inv[idx];
      if ((pot.quantity || 1) > 1) {
        inv[idx] = { ...pot, quantity: (pot.quantity || 1) - 1 };
      } else {
        inv.splice(idx, 1);
      }
      if (kind === 'hp') {
        const heal = Math.round(prev.maxHp * 0.45);
        pushFloatingText(prev.x, prev.y - 24, `+${heal} HP`, '#34D399');
        return { ...prev, inventory: inv, hp: Math.min(prev.maxHp, prev.hp + heal) };
      }
      const mana = Math.round(prev.maxMp * 0.5);
      pushFloatingText(prev.x, prev.y - 24, `+${mana} MP`, '#60A5FA');
      return { ...prev, inventory: inv, mp: Math.min(prev.maxMp, prev.mp + mana) };
    });
  };

  // Wi-Fi LAN WebSocket Connection (with Invite Room Code, Events & P2P Trade)
  const handleJoinLanRoom = useCallback((roomCode: string) => {
    const hero = heroRef.current || heroes[0];
    if (!hero) {
      setLanError('Primero crea o selecciona un personaje para unirte a la sala LAN.');
      return;
    }

    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }

    setLanStatus('connecting');
    setLanError(null);
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const ws = new WebSocket(`${proto}//${window.location.host}/ws-lan`);
    wsRef.current = ws;

    ws.onopen = () => {
      ws.send(
        JSON.stringify({
          type: 'room:join',
          roomCode,
          player: {
            id: hero.id,
            name: hero.name,
            classId: hero.classId,
            level: hero.level,
            resets: hero.resets,
            mapId: hero.mapId,
            x: hero.x,
            y: hero.y,
            dir: 0,
            wingsTier: hero.equipment.wings ? 1 : 0,
            weaponPlus: hero.equipment.weapon?.plus || 0,
            hp: hero.hp,
            maxHp: hero.maxHp,
          },
        })
      );
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        if (msg.type === 'room:state') {
          setConnectedRoom(msg.roomCode);
          setLanStatus('connected');
          setLanPlayers(msg.players || []);
          setLanBosses(msg.bosses || []);
          setActiveEvent(msg.activeEvent || null);
        } else if (msg.type === 'player:joined') {
          setLanPlayers((prev) => (prev.some((p) => p.id === msg.player.id) ? prev : [...prev, msg.player]));
        } else if (msg.type === 'player:updated') {
          setLanPlayers((prev) => prev.map((p) => (p.id === msg.player.id ? msg.player : p)));
        } else if (msg.type === 'player:left') {
          setLanPlayers((prev) => prev.filter((p) => p.id !== msg.playerId));
        } else if (msg.type === 'player:cast') {
          if (heroRef.current && msg.mapId === heroRef.current.mapId) {
            pushVfx(msg.skillId, msg.x, msg.y, msg.targetX, msg.targetY, '#38BDF8', 80);
          }
        } else if (msg.type === 'event:started') {
          setActiveEvent(msg.event);
          pushFloatingText(SAFE_CENTER, SAFE_CENTER - 42, `Evento: ${msg.event.name}`, '#FBBF24', true);
        } else if (msg.type === 'boss:updated') {
          setLanBosses((prev) => prev.map((b) => (b.id === msg.bossId ? { ...b, hp: msg.hp } : b)));
        } else if (msg.type === 'boss:killed') {
          setLanBosses((prev) => prev.map((b) => (b.id === msg.bossId ? { ...b, hp: 0 } : b)));
          setActiveHero((prev) => {
            if (!prev) return prev;
            const bonusZen = 5000;
            const next = { ...prev, zen: prev.zen + bonusZen };
            saveHero(next);
            return next;
          });
          soundFx.playJewelDrop();
          pushFloatingText(SAFE_CENTER, SAFE_CENTER - 40, `${msg.killerName} derrotó al Jefe LAN (+5,000 Zen)!`, '#FBBF24', true);
        } else if (msg.type === 'boss:respawned') {
          setLanBosses((prev) => prev.map((b) => (b.id === msg.boss.id ? msg.boss : b)));
        } else if (msg.type === 'trade:incoming') {
          setIncomingTrade({
            tradeId: msg.tradeId,
            fromPlayerId: msg.fromPlayerId,
            fromPlayerName: msg.fromPlayerName,
          });
        } else if (msg.type === 'trade:started') {
          setIncomingTrade(null);
          setTradeSession({
            tradeId: msg.tradeId,
            partnerId: msg.partnerId,
            partnerName: msg.partnerName,
            myItems: [],
            myZen: 0,
            myConfirmed: false,
            partnerItems: [],
            partnerZen: 0,
            partnerConfirmed: false,
          });
        } else if (msg.type === 'trade:partner_updated') {
          setTradeSession((prev) =>
            prev && prev.tradeId === msg.tradeId
              ? {
                  ...prev,
                  partnerItems: msg.items || [],
                  partnerZen: msg.zen || 0,
                  myConfirmed: false,
                  partnerConfirmed: false,
                }
              : prev
          );
        } else if (msg.type === 'trade:partner_confirmed') {
          setTradeSession((prev) =>
            prev && prev.tradeId === msg.tradeId ? { ...prev, partnerConfirmed: true } : prev
          );
        } else if (msg.type === 'trade:cancelled') {
          setIncomingTrade(null);
          setTradeSession(null);
        } else if (msg.type === 'trade:completed') {
          const receivedItems = (msg.receivedItems || []) as Item[];
          const givenItems = (msg.givenItems || []) as Item[];
          const receivedZen = Number(msg.receivedZen || 0);
          const givenZen = Number(msg.givenZen || 0);
          const givenIds = new Set(givenItems.map((g) => g.id));

          setActiveHero((prev) => {
            if (!prev) return prev;
            let next: HeroSave = {
              ...prev,
              zen: Math.max(0, prev.zen - givenZen + receivedZen),
              inventory: prev.inventory.filter((it) => !givenIds.has(it.id)),
            };
            for (const rec of receivedItems) {
              next = addItemToHero(next, {
                ...rec,
                id: `tr-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
              });
            }
            saveHero(next);
            return next;
          });
          soundFx.playChaosSuccess();
          setTradeSession(null);
          if (heroRef.current) {
            pushFloatingText(heroRef.current.x, heroRef.current.y - 30, '¡Intercambio Completado!', '#34D399', true);
          }
        } else if (msg.type === 'chat:message') {
          setChatMessages((prev) => [...prev.slice(-30), msg]);
        }
      } catch {
        // ignore invalid frames
      }
    };

    ws.onerror = () => {
      setLanStatus('error');
      setLanError('No se pudo conectar al servidor LAN local (modo sin conexión activo).');
    };

    ws.onclose = () => {
      setConnectedRoom(null);
      setLanStatus('disconnected');
    };
  }, [addItemToHero, heroes, pushFloatingText, pushVfx]);

  // Auto-join invited ?room=CODE when entering game
  useEffect(() => {
    if (!activeHero || connectedRoom) return;
    const params = new URLSearchParams(window.location.search);
    const invitedRoom = params.get('room');
    if (invitedRoom) {
      handleJoinLanRoom(invitedRoom);
    }
  }, [activeHero?.id]);

  const handleLeaveLanRoom = () => {
    wsRef.current?.send(JSON.stringify({ type: 'room:leave' }));
    wsRef.current?.close();
    wsRef.current = null;
    setConnectedRoom(null);
    setLanPlayers([]);
    setLanBosses([]);
    setActiveEvent(null);
    setLanStatus('disconnected');
  };

  const handleAddStat = (stat: keyof HeroStats, amount: number) => {
    setActiveHero((prev) => {
      if (!prev || prev.statPoints < amount) return prev;
      const next: HeroSave = {
        ...prev,
        statPoints: prev.statPoints - amount,
        stats: { ...prev.stats, [stat]: prev.stats[stat] + amount },
      };
      const derived = computeDerivedHeroStats(next);
      next.maxHp = derived.maxHp;
      next.maxMp = derived.maxMp;
      saveHero(next);
      return next;
    });
  };

  const handleResetHero = () => {
    setActiveHero((prev) => {
      if (!prev || prev.level < activeModRates.resetLevel) return prev;
      const meta = CLASS_CATALOG[prev.classId];
      const nextResets = prev.resets + 1;
      const next: HeroSave = {
        ...prev,
        level: 1,
        resets: nextResets,
        exp: 0,
        expToNext: 120,
        statPoints: nextResets * 150,
        stats: { ...meta.baseStats },
        mapId: 'lorencia',
        x: SAFE_CENTER,
        y: SAFE_CENTER,
      };
      const derived = computeDerivedHeroStats(next);
      next.maxHp = derived.maxHp;
      next.hp = derived.maxHp;
      next.maxMp = derived.maxMp;
      next.mp = derived.maxMp;
      soundFx.playLevelUp();
      saveHero(next);
      return next;
    });
  };

  const handleEquipItem = (item: Item) => {
    if (item.slot === 'jewel' || item.slot === 'potion') return;
    setActiveHero((prev) => {
      if (!prev) return prev;
      const slotKey = item.slot as keyof EquipSlots;
      const currentEquipped = prev.equipment[slotKey];
      const nextInv = prev.inventory.filter((i) => i.id !== item.id);
      if (currentEquipped) nextInv.push(currentEquipped);
      const next: HeroSave = {
        ...prev,
        equipment: { ...prev.equipment, [slotKey]: item },
        inventory: nextInv,
      };
      const derived = computeDerivedHeroStats(next);
      next.maxHp = derived.maxHp;
      next.maxMp = derived.maxMp;
      saveHero(next);
      return next;
    });
  };

  const handleUnequipSlot = (slot: keyof EquipSlots) => {
    setActiveHero((prev) => {
      if (!prev) return prev;
      const eq = prev.equipment[slot];
      if (!eq || prev.inventory.length >= 24) return prev;
      const next: HeroSave = {
        ...prev,
        equipment: { ...prev.equipment, [slot]: null },
        inventory: [...prev.inventory, eq],
      };
      const derived = computeDerivedHeroStats(next);
      next.maxHp = derived.maxHp;
      next.maxMp = derived.maxMp;
      saveHero(next);
      return next;
    });
  };

  const consumeOneJewel = (inv: Item[], jewelType: 'bless' | 'soul' | 'chaos'): Item[] | null => {
    const idx = inv.findIndex((i) => i.jewelType === jewelType);
    if (idx < 0) return null;
    const copy = [...inv];
    const target = copy[idx];
    if ((target.quantity || 1) > 1) {
      copy[idx] = { ...target, quantity: (target.quantity || 1) - 1 };
    } else {
      copy.splice(idx, 1);
    }
    return copy;
  };

  const handleUpgradeWithJewel = (targetItemId: string, jewelType: 'bless' | 'soul'): string => {
    const hero = heroRef.current;
    if (!hero) return 'Error interno';
    const nextInv = consumeOneJewel(hero.inventory, jewelType);
    if (!nextInv) return `Necesitas 1 Jewel of ${jewelType === 'bless' ? 'Bless' : 'Soul'}.`;

    const success = jewelType === 'bless' ? true : Math.random() < 0.72;
    let upgradedName = '';
    let newPlus = 0;

    const applyTo = (item: Item): Item => {
      if (item.id !== targetItemId) return item;
      upgradedName = item.name;
      newPlus = success ? Math.min(9, item.plus + 1) : Math.max(0, item.plus - 1);
      return { ...item, plus: newPlus };
    };

    const nextEq: EquipSlots = {
      weapon: hero.equipment.weapon ? applyTo(hero.equipment.weapon) : null,
      offhand: hero.equipment.offhand ? applyTo(hero.equipment.offhand) : null,
      armor: hero.equipment.armor ? applyTo(hero.equipment.armor) : null,
      helm: hero.equipment.helm ? applyTo(hero.equipment.helm) : null,
      wings: hero.equipment.wings ? applyTo(hero.equipment.wings) : null,
      pendant: hero.equipment.pendant ? applyTo(hero.equipment.pendant) : null,
    };
    const finalInv = nextInv.map(applyTo);

    if (success) soundFx.playChaosSuccess();
    else soundFx.playChaosFail();

    const nextHero: HeroSave = { ...hero, equipment: nextEq, inventory: finalInv };
    setActiveHero(nextHero);
    saveHero(nextHero);
    return success
      ? `¡Éxito! ${upgradedName} subió a +${newPlus}.`
      : `Falló la joya: ${upgradedName} bajó a +${newPlus}.`;
  };

  const handleChaosUpgrade = (targetItemId: string): { ok: boolean; message: string } => {
    const hero = heroRef.current;
    if (!hero || hero.zen < 3000) return { ok: false, message: 'Necesitas 3,000 Zen.' };
    const afterChaos = consumeOneJewel(hero.inventory, 'chaos');
    if (!afterChaos) return { ok: false, message: 'Falta Jewel of Chaos.' };
    const afterSoul = consumeOneJewel(afterChaos, 'soul');
    if (!afterSoul) return { ok: false, message: 'Falta Jewel of Soul.' };

    let currentPlus = 9;
    let itemName = '';
    const findItem = (it: Item | null) => {
      if (it && it.id === targetItemId) {
        currentPlus = it.plus;
        itemName = it.name;
      }
    };
    Object.values(hero.equipment).forEach(findItem);
    afterSoul.forEach(findItem);

    const rate = Math.min(95, Math.max(35, 75 - (currentPlus - 9) * 10 + activeModRates.chaosBonus)) / 100;
    const ok = Math.random() < rate;
    const newPlus = ok ? Math.min(13, currentPlus + 1) : Math.max(0, currentPlus - 2);

    const mutateItem = (it: Item): Item => (it.id === targetItemId ? { ...it, plus: newPlus } : it);
    const nextEq: EquipSlots = {
      weapon: hero.equipment.weapon ? mutateItem(hero.equipment.weapon) : null,
      offhand: hero.equipment.offhand ? mutateItem(hero.equipment.offhand) : null,
      armor: hero.equipment.armor ? mutateItem(hero.equipment.armor) : null,
      helm: hero.equipment.helm ? mutateItem(hero.equipment.helm) : null,
      wings: hero.equipment.wings ? mutateItem(hero.equipment.wings) : null,
      pendant: hero.equipment.pendant ? mutateItem(hero.equipment.pendant) : null,
    };

    if (ok) soundFx.playChaosSuccess();
    else soundFx.playChaosFail();

    const nextHero: HeroSave = {
      ...hero,
      zen: hero.zen - 3000,
      equipment: nextEq,
      inventory: afterSoul.map(mutateItem),
    };
    setActiveHero(nextHero);
    saveHero(nextHero);
    return {
      ok,
      message: ok
        ? `¡Chaos Machine Exitosa! ${itemName} ahora es +${newPlus}.`
        : `Combinación fallida: ${itemName} bajó a +${newPlus}.`,
    };
  };

  const handleCraftWings = (): { ok: boolean; message: string } => {
    const hero = heroRef.current;
    if (!hero || hero.zen < 5000) return { ok: false, message: 'Necesitas 5,000 Zen.' };
    const afterChaos = consumeOneJewel(hero.inventory, 'chaos');
    if (!afterChaos) return { ok: false, message: 'Necesitas 1 Jewel of Chaos.' };
    const afterBless = consumeOneJewel(afterChaos, 'bless');
    if (!afterBless) return { ok: false, message: 'Necesitas 1 Jewel of Bless.' };

    const rate = Math.min(95, 70 + activeModRates.chaosBonus) / 100;
    const ok = Math.random() < rate;
    let nextHero: HeroSave = { ...hero, zen: hero.zen - 5000, inventory: afterBless };

    if (ok) {
      soundFx.playChaosSuccess();
      const wingsNames: Record<HeroSave['classId'], string> = {
        dk: 'Wings of Dragon',
        dw: 'Wings of Soul',
        elf: 'Wings of Spirits',
      };
      const newWings: Item = {
        id: `wings-${Date.now()}`,
        templateId: 'crafted-wings',
        name: wingsNames[hero.classId],
        slot: 'wings',
        rarity: 'excellent',
        plus: 3,
        attack: 34,
        magic: 34,
        defense: 28,
        hpBonus: 140,
        speedBonus: 20,
        classReq: 'all',
        colorHex: '#FBBF24',
        valueZen: 24000,
      };
      nextHero = addItemToHero(nextHero, newWings);
      setActiveHero(nextHero);
      saveHero(nextHero);
      return { ok: true, message: `¡Forjaste ${newWings.name} +3! Revisa tu mochila para equiparlas.` };
    }

    soundFx.playChaosFail();
    setActiveHero(nextHero);
    saveHero(nextHero);
    return { ok: false, message: 'La Chaos Machine falló al forjar las alas.' };
  };

  const handleSellItem = (itemId: string) => {
    setActiveHero((prev) => {
      if (!prev) return prev;
      const item = prev.inventory.find((i) => i.id === itemId);
      if (!item) return prev;
      const next: HeroSave = {
        ...prev,
        zen: prev.zen + item.valueZen * (item.quantity || 1),
        inventory: prev.inventory.filter((i) => i.id !== itemId),
      };
      saveHero(next);
      return next;
    });
  };

  const handleToggleLandscape = useCallback(async () => {
    try {
      const docEl = document.documentElement as HTMLElement & {
        webkitRequestFullscreen?: () => Promise<void>;
      };
      if (!document.fullscreenElement) {
        if (docEl.requestFullscreen) await docEl.requestFullscreen();
        else if (docEl.webkitRequestFullscreen) await docEl.webkitRequestFullscreen();
      } else if (document.exitFullscreen) {
        await document.exitFullscreen();
        return;
      }
    } catch {
      // Fullscreen may be blocked by browser policy
    }
    try {
      const ori = screen.orientation as ScreenOrientation & { lock?: (mode: string) => Promise<void> };
      if (ori?.lock) {
        await ori.lock('landscape');
      }
    } catch {
      // Orientation lock not supported on all browsers
    }
  }, []);

  const handleBuyItem = useCallback(
    (itemTemplate: Omit<Item, 'id'>, costZen: number): string => {
      const cur = heroRef.current;
      if (!cur) return 'No hay héroe activo.';
      if (cur.zen < costZen) return 'No tienes suficiente Zen.';
      if (cur.inventory.length >= 24 && itemTemplate.slot !== 'potion' && itemTemplate.slot !== 'jewel') {
        return 'Tu mochila está llena (máx. 24 espacios).';
      }
      const newItem: Item = {
        ...itemTemplate,
        id: `shop-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
        quantity: itemTemplate.slot === 'potion' ? 5 : 1,
      };
      setActiveHero((prev) => {
        if (!prev || prev.zen < costZen) return prev;
        const withItem = addItemToHero({ ...prev, zen: prev.zen - costZen }, newItem);
        saveHero(withItem);
        return withItem;
      });
      soundFx.playJewelDrop();
      return `Compraste ${itemTemplate.name} por ${costZen.toLocaleString()} Zen.`;
    },
    [addItemToHero]
  );

  const handleToggleMod = (modId: string) => {
    setMods((prev) => {
      const next = prev.map((m) => (m.id === modId ? { ...m, enabled: !m.enabled } : m));
      saveMods(next);
      return next;
    });
  };

  const handleAddMod = (mod: ModPackage) => {
    setMods((prev) => {
      const next = [mod, ...prev];
      saveMods(next);
      return next;
    });
  };

  const handleDeleteMod = (modId: string) => {
    setMods((prev) => {
      const next = prev.filter((m) => m.id !== modId);
      saveMods(next);
      return next;
    });
  };

  const handleGamepadUpdate = useCallback(
    (name: string | null, attackPressed: boolean, skillIdx: number | null) => {
      setGamepadName((prev) => (prev === name ? prev : name));
      if (attackPressed) executeCombat();
      if (skillIdx !== null && heroRef.current) {
        const sk = SKILLS_BY_CLASS[heroRef.current.classId][skillIdx];
        if (sk) executeCombat(sk.id);
      }
    },
    [executeCombat]
  );

  if (!ready) {
    return (
      <div className="h-full w-full flex items-center justify-center bg-[#0B0D13] text-amber-300 font-display text-lg">
        Cargando Mu Offline Mobile...
      </div>
    );
  }

  return (
    <div className="h-full w-full relative overflow-hidden bg-[#0B0D13]">
      {!accountId ? (
        <AuthScreen
          onAuthenticated={(id) => {
            setAccountId(id);
          }}
        />
      ) : !activeHero ? (
        <TitleScreen
          accountId={accountId}
          heroes={heroes}
          mods={mods}
          onSelectHero={(hero) => setActiveHero(hero)}
          onCreateHero={(hero) => {
            saveHero(hero);
            setHeroes((prev) => [hero, ...prev]);
            setActiveHero(hero);
          }}
          onDeleteHero={(heroId) => {
            deleteHeroFromDb(heroId);
            setHeroes((prev) => prev.filter((h) => h.id !== heroId));
          }}
          onOpenMods={() => setShowModsModal(true)}
          onOpenLanHelp={() => setShowLanModal(true)}
          onOpenCredits={() => setShowCreditsModal(true)}
          onLogoutAccount={() => {
            logoutAccount();
            setActiveHero(null);
            setAccountId(null);
          }}
        />
      ) : (
        <>
          <GameCanvas
            hero={activeHero}
            monsters={monsters}
            drops={drops}
            floatingTexts={floatingTexts}
            vfxList={vfxList}
            lanPlayers={lanPlayers}
            lanBosses={lanBosses}
            joystickRef={joystickRef}
            onTapWorld={handleTapWorld}
            onInteractNpc={(npc) => setActiveNpc(npc)}
            onMoveDelta={handleMoveDelta}
            onGamepadUpdate={handleGamepadUpdate}
          />

          <MobileHUD
            hero={activeHero}
            autoHunt={autoHunt}
            muted={muted}
            connectedRoom={connectedRoom}
            lanPlayerCount={lanPlayers.length}
            activeEvent={activeEvent}
            incomingTrade={incomingTrade}
            chatMessages={chatMessages}
            skillCooldowns={skillCooldowns}
            joystickRef={joystickRef}
            onToggleAutoHunt={() => setAutoHunt((v) => !v)}
            onToggleMute={() => {
              soundFx.muted = !muted;
              setMuted(!muted);
            }}
            onToggleLandscape={handleToggleLandscape}
            onChangeMap={(mapId) => {
              setActiveHero((prev) => {
                if (!prev) return prev;
                const next: HeroSave = { ...prev, mapId, x: SAFE_CENTER, y: SAFE_CENTER };
                saveHero(next);
                return next;
              });
            }}
            onInteractNpc={(npc) => setActiveNpc(npc)}
            onAttack={() => executeCombat()}
            onCastSkill={(skillId) => executeCombat(skillId)}
            onUsePotion={handleUsePotion}
            onOpenCharacter={() => setShowCharModal(true)}
            onOpenInventory={() => setShowInvModal(true)}
            onOpenMods={() => setShowModsModal(true)}
            onOpenLan={() => setShowLanModal(true)}
            onAcceptTrade={() => {
              if (!incomingTrade) return;
              wsRef.current?.send(JSON.stringify({ type: 'trade:accept', tradeId: incomingTrade.tradeId }));
            }}
            onDeclineTrade={() => {
              if (!incomingTrade) return;
              wsRef.current?.send(JSON.stringify({ type: 'trade:decline', tradeId: incomingTrade.tradeId }));
              setIncomingTrade(null);
            }}
            onSendChat={(text) => wsRef.current?.send(JSON.stringify({ type: 'chat:send', text }))}
            onExitToTitle={() => {
              if (activeHero) saveHero(activeHero);
              setHeroes((prev) => {
                if (!activeHero) return prev;
                return [activeHero, ...prev.filter((h) => h.id !== activeHero.id)];
              });
              setAutoHunt(false);
              setActiveNpc(null);
              setActiveHero(null);
            }}
          />

          {isPortraitMobile && !dismissedRotatePrompt && (
            <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/80 p-4">
              <div className="w-full max-w-xs rounded-2xl border-2 border-amber-500/60 bg-[#0E111A] p-5 text-center shadow-2xl space-y-4">
                <div className="mx-auto w-14 h-14 rounded-2xl border border-amber-500/40 bg-amber-500/10 flex items-center justify-center">
                  <Smartphone className="w-7 h-7 text-amber-400 rotate-90" />
                </div>
                <div className="space-y-1">
                  <h3 className="font-display text-base font-bold text-amber-200">
                    Juega en Horizontal
                  </h3>
                  <p className="text-xs text-slate-300 leading-relaxed">
                    Voltea tu teléfono de lado (como Free Fire o PUBG Mobile) para ver el mapa completo de Lorencia y usar ambos pulgares cómodamente.
                  </p>
                </div>
                <div className="space-y-2">
                  <button
                    onClick={handleToggleLandscape}
                    className="w-full min-h-[44px] rounded-xl bg-amber-500 text-slate-950 font-bold text-xs hover:bg-amber-400"
                  >
                    Activar Pantalla Horizontal
                  </button>
                  <button
                    onClick={() => setDismissedRotatePrompt(true)}
                    className="w-full min-h-[38px] rounded-xl border border-slate-700 bg-slate-900 text-slate-300 text-xs flex items-center justify-center gap-1"
                  >
                    <X className="w-3.5 h-3.5" />
                    <span>Continuar en vertical</span>
                  </button>
                </div>
              </div>
            </div>
          )}

          {showCharModal && (
            <CharacterModal
              hero={activeHero}
              resetLevelReq={activeModRates.resetLevel}
              onAddStat={handleAddStat}
              onResetHero={handleResetHero}
              onClose={() => setShowCharModal(false)}
            />
          )}

          {showInvModal && (
            <InventoryChaosModal
              hero={activeHero}
              chaosBonus={activeModRates.chaosBonus}
              onEquipItem={handleEquipItem}
              onUnequipSlot={handleUnequipSlot}
              onUpgradeWithJewel={handleUpgradeWithJewel}
              onChaosUpgrade={handleChaosUpgrade}
              onCraftWings={handleCraftWings}
              onSellItem={handleSellItem}
              onClose={() => setShowInvModal(false)}
            />
          )}

          {activeNpc && (
            <NpcShopModal
              hero={activeHero}
              npcName={activeNpc.name}
              onBuyItem={handleBuyItem}
              onOpenChaosMachine={() => setShowInvModal(true)}
              onClose={() => setActiveNpc(null)}
            />
          )}

          {tradeSession && (
            <TradeModal
              hero={activeHero}
              trade={tradeSession}
              onUpdateOffer={(items, zen) => {
                setTradeSession((prev) =>
                  prev ? { ...prev, myItems: items, myZen: zen, myConfirmed: false, partnerConfirmed: false } : prev
                );
                wsRef.current?.send(
                  JSON.stringify({ type: 'trade:update', tradeId: tradeSession.tradeId, items, zen })
                );
              }}
              onConfirm={() => {
                setTradeSession((prev) => (prev ? { ...prev, myConfirmed: true } : prev));
                wsRef.current?.send(JSON.stringify({ type: 'trade:confirm', tradeId: tradeSession.tradeId }));
              }}
              onCancel={() => {
                wsRef.current?.send(JSON.stringify({ type: 'trade:cancel', tradeId: tradeSession.tradeId }));
                setTradeSession(null);
              }}
            />
          )}
        </>
      )}

      {showModsModal && (
        <ModsModal
          mods={mods}
          onToggleMod={handleToggleMod}
          onAddMod={handleAddMod}
          onDeleteMod={handleDeleteMod}
          onClose={() => setShowModsModal(false)}
        />
      )}

      {showLanModal && (
        <LanModal
          localHeroId={activeHero?.id || null}
          connectedRoom={connectedRoom}
          lanPlayers={lanPlayers}
          activeEvent={activeEvent}
          lanStatus={lanStatus}
          lanError={lanError}
          gamepadName={gamepadName}
          onJoinRoom={handleJoinLanRoom}
          onLeaveRoom={handleLeaveLanRoom}
          onRequestTrade={(targetPlayerId) => {
            wsRef.current?.send(JSON.stringify({ type: 'trade:request', targetPlayerId }));
          }}
          onStartEvent={(eventType) => {
            wsRef.current?.send(JSON.stringify({ type: 'event:start', eventType }));
          }}
          onClose={() => setShowLanModal(false)}
        />
      )}

      {showCreditsModal && <CreditsModal onClose={() => setShowCreditsModal(false)} />}
    </div>
  );
}
