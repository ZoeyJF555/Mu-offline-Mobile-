import React, { useRef, useState } from 'react';
import {
  ArrowLeftRight,
  Backpack,
  Compass,
  Crosshair,
  Flame,
  LogOut,
  Maximize2,
  MessageSquare,
  Package,
  Send,
  Shield,
  Store,
  Swords,
  Volume2,
  VolumeX,
  Wifi,
} from 'lucide-react';
import { MAP_CATALOG, SKILLS_BY_CLASS } from '../data/muCatalog';
import { LORENCIA_NPCS, type LorenciaNpcDef } from '../three/muModels';
import type { HeroSave, IncomingTradeRequest, LanChatMessage, LanEventState, MapId } from '../types/game';
import { OfflineStatusText, PWAInstallButton } from './PWAInstallButton';

interface MobileHUDProps {
  hero: HeroSave;
  autoHunt: boolean;
  muted: boolean;
  connectedRoom: string | null;
  lanPlayerCount: number;
  activeEvent: LanEventState | null;
  incomingTrade: IncomingTradeRequest | null;
  chatMessages: LanChatMessage[];
  skillCooldowns: Record<string, number>;
  joystickRef: React.MutableRefObject<{ x: number; y: number }>;
  onToggleAutoHunt: () => void;
  onToggleMute: () => void;
  onToggleLandscape: () => void;
  onChangeMap: (mapId: MapId) => void;
  onInteractNpc: (npc: LorenciaNpcDef) => void;
  onAttack: () => void;
  onCastSkill: (skillId: string) => void;
  onUsePotion: (kind: 'hp' | 'mp') => void;
  onOpenCharacter: () => void;
  onOpenInventory: () => void;
  onOpenMods: () => void;
  onOpenLan: () => void;
  onAcceptTrade: () => void;
  onDeclineTrade: () => void;
  onSendChat: (text: string) => void;
  onExitToTitle: () => void;
}

export const MobileHUD: React.FC<MobileHUDProps> = ({
  hero,
  autoHunt,
  muted,
  connectedRoom,
  lanPlayerCount,
  activeEvent,
  incomingTrade,
  chatMessages,
  skillCooldowns,
  joystickRef,
  onToggleAutoHunt,
  onToggleMute,
  onToggleLandscape,
  onChangeMap,
  onInteractNpc,
  onAttack,
  onCastSkill,
  onUsePotion,
  onOpenCharacter,
  onOpenInventory,
  onOpenMods,
  onOpenLan,
  onAcceptTrade,
  onDeclineTrade,
  onSendChat,
  onExitToTitle,
}) => {
  const [knobOffset, setKnobOffset] = useState({ x: 0, y: 0 });
  const [showMapPicker, setShowMapPicker] = useState(false);
  const [showChat, setShowChat] = useState(false);
  const [chatInput, setChatInput] = useState('');
  const joyBaseRef = useRef<HTMLDivElement | null>(null);
  const activePointerId = useRef<number | null>(null);

  const skills = SKILLS_BY_CLASS[hero.classId];
  const hpPotions = hero.inventory
    .filter((i) => i.potionType === 'hp')
    .reduce((acc, i) => acc + (i.quantity || 1), 0);
  const mpPotions = hero.inventory
    .filter((i) => i.potionType === 'mp')
    .reduce((acc, i) => acc + (i.quantity || 1), 0);

  const updateJoystickFromPointer = (clientX: number, clientY: number) => {
    const el = joyBaseRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    const maxR = rect.width * 0.38;
    const dx = clientX - cx;
    const dy = clientY - cy;
    const dist = Math.hypot(dx, dy);
    const clampedDist = Math.min(maxR, dist);
    const angle = Math.atan2(dy, dx);
    const nx = dist > 2 ? (Math.cos(angle) * clampedDist) / maxR : 0;
    const ny = dist > 2 ? (Math.sin(angle) * clampedDist) / maxR : 0;
    joystickRef.current = { x: nx, y: ny };
    setKnobOffset({ x: nx * maxR, y: ny * maxR });
  };

  const resetJoystick = () => {
    activePointerId.current = null;
    joystickRef.current = { x: 0, y: 0 };
    setKnobOffset({ x: 0, y: 0 });
  };

  const hpPct = Math.max(0, Math.min(100, (hero.hp / hero.maxHp) * 100));
  const mpPct = Math.max(0, Math.min(100, (hero.mp / hero.maxMp) * 100));
  const expPct = Math.max(0, Math.min(100, (hero.exp / hero.expToNext) * 100));
  const muCoordX = Math.round(hero.x / 5.5);
  const muCoordY = Math.round(hero.y / 5.5);

  const availableNpcs =
    hero.mapId === 'lorencia'
      ? LORENCIA_NPCS
      : LORENCIA_NPCS.filter((n) => n.id === 'chaos' || n.id === 'barmaid');
  const nearbyNpc = availableNpcs.find((n) => Math.hypot(hero.x - n.wx, hero.y - n.wy) < 70) || null;

  return (
    <div className="pointer-events-none fixed inset-0 z-20 flex flex-col justify-between pt-safe pb-safe">
      {/* Classic MU Gothic Gold-Trimmed Top Bar (compact for horizontal gameplay) */}
      <div className="pointer-events-auto flex items-center justify-between gap-2 bg-[#0B0D14]/92 border-b border-amber-600/40 px-2 sm:px-4 h-11 shadow-lg">
        <div className="flex items-center gap-1.5 sm:gap-2 min-w-0">
          <button
            onClick={onOpenCharacter}
            className="min-h-[36px] px-2 rounded-lg bg-[#16131c] border border-amber-500/45 flex items-center gap-1.5 text-left hover:border-amber-400"
          >
            <Shield className="w-3.5 h-3.5 text-amber-400 shrink-0" />
            <div className="leading-tight min-w-0">
              <span className="block text-[11px] font-bold text-amber-100 truncate max-w-[84px]">
                {hero.name}
              </span>
              <span className="block text-[9px] font-mono-tabular text-amber-400">
                Nv {hero.level} · R{hero.resets}
              </span>
            </div>
          </button>

          <button
            onClick={() => setShowMapPicker((v) => !v)}
            className="min-h-[36px] px-2 rounded-lg bg-[#141722] border border-amber-500/35 flex items-center gap-1.5 text-[11px] font-semibold text-amber-100 hover:border-amber-400 whitespace-nowrap shrink-0"
          >
            <Compass className="w-3.5 h-3.5 text-amber-400" />
            <span>{MAP_CATALOG[hero.mapId].name}</span>
            <span className="hidden sm:inline font-mono-tabular text-[10px] text-amber-300/80">
              ({muCoordX},{muCoordY})
            </span>
          </button>

          <OfflineStatusText />
        </div>

        <div className="flex items-center gap-1 sm:gap-1.5 shrink-0">
          <PWAInstallButton compact />
          <button
            onClick={onOpenInventory}
            aria-label="Mochila y Chaos Machine"
            className="min-h-[36px] min-w-[36px] px-2 rounded-lg bg-[#16131c] border border-amber-500/35 text-[11px] font-semibold text-amber-100 hover:border-amber-400 flex items-center gap-1.5"
          >
            <Backpack className="w-3.5 h-3.5 text-amber-400" />
            <span className="hidden lg:inline">Mochila</span>
          </button>
          <button
            onClick={onOpenMods}
            aria-label="Mods"
            className="min-h-[36px] min-w-[36px] px-2 rounded-lg bg-[#16131c] border border-purple-500/40 text-[11px] font-semibold text-purple-100 hover:border-purple-400 flex items-center gap-1.5"
          >
            <Package className="w-3.5 h-3.5 text-purple-400" />
            <span className="hidden lg:inline">Mods</span>
          </button>
          <button
            onClick={onOpenLan}
            aria-label="Wi-Fi LAN"
            className={`min-h-[36px] min-w-[36px] px-2 rounded-lg border text-[11px] font-semibold flex items-center gap-1.5 ${
              connectedRoom
                ? 'bg-sky-950/90 border-sky-400/70 text-sky-200'
                : 'bg-[#16131c] border-sky-500/35 text-slate-200 hover:border-sky-400'
            }`}
          >
            <Wifi className="w-3.5 h-3.5 text-sky-400" />
            <span className="hidden lg:inline">
              {connectedRoom ? `LAN (${lanPlayerCount})` : 'LAN'}
            </span>
          </button>
          <button
            onClick={onToggleLandscape}
            title="Pantalla Completa Horizontal"
            aria-label="Pantalla Completa Horizontal"
            className="min-h-[36px] min-w-[36px] px-2 rounded-lg bg-[#16131c] border border-amber-500/40 text-amber-300 hover:text-amber-200 flex items-center justify-center gap-1"
          >
            <Maximize2 className="w-3.5 h-3.5" />
            <span className="hidden md:inline text-[11px] font-semibold">Horizontal</span>
          </button>
          <button
            onClick={onToggleMute}
            aria-label={muted ? 'Activar sonido' : 'Silenciar'}
            className="min-h-[36px] min-w-[36px] rounded-lg bg-[#16131c] border border-slate-700 text-slate-300 flex items-center justify-center"
          >
            {muted ? <VolumeX className="w-3.5 h-3.5" /> : <Volume2 className="w-3.5 h-3.5" />}
          </button>
          <button
            onClick={onExitToTitle}
            aria-label="Salir al menú"
            className="min-h-[36px] min-w-[36px] rounded-lg bg-[#16131c] border border-slate-700 text-slate-400 hover:text-white flex items-center justify-center"
          >
            <LogOut className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Map Warp Window (M) */}
      {showMapPicker && (
        <div className="pointer-events-auto absolute top-14 left-3 z-30 w-64 rounded-xl border-2 border-amber-600/60 bg-[#0D1018]/95 p-2 shadow-2xl space-y-1">
          <div className="px-2 py-1 text-[11px] font-display font-bold text-amber-400 border-b border-amber-500/20">
            WARP DE MAPAS (M)
          </div>
          {(Object.keys(MAP_CATALOG) as MapId[]).map((mid) => {
            const m = MAP_CATALOG[mid];
            const locked = hero.level < m.minLevel;
            return (
              <button
                key={mid}
                disabled={locked}
                onClick={() => {
                  onChangeMap(mid);
                  setShowMapPicker(false);
                }}
                className={`w-full min-h-[42px] flex items-center justify-between rounded-lg px-3 text-left text-xs transition-colors ${
                  hero.mapId === mid
                    ? 'bg-amber-500 text-slate-950 font-bold'
                    : 'text-slate-200 hover:bg-slate-800 disabled:opacity-35'
                }`}
              >
                <span>{m.name}</span>
                <span className="font-mono-tabular text-[11px]">
                  {locked ? `Requiere Nv ${m.minLevel}` : `Nv ${m.minLevel}+`}
                </span>
              </button>
            );
          })}
        </div>
      )}

      {/* Cooperative LAN Event Banner */}
      {activeEvent?.active && activeEvent.endsAt > Date.now() && (
        <div className="pointer-events-auto mx-auto mt-1.5 flex items-center gap-3 rounded-xl border border-amber-500/60 bg-[#14111a]/95 px-3.5 py-1.5 text-xs shadow-lg">
          <Flame className="w-4 h-4 text-amber-400 shrink-0" />
          <span className="font-semibold text-amber-200">
            {activeEvent.name} ({MAP_CATALOG[activeEvent.mapId].name})
          </span>
          {hero.mapId !== activeEvent.mapId && (
            <button
              onClick={() => onChangeMap(activeEvent.mapId)}
              className="min-h-[32px] px-2.5 rounded-lg bg-amber-500 text-slate-950 font-semibold text-[11px]"
            >
              Ir al Evento
            </button>
          )}
        </div>
      )}

      {/* Incoming Trade Request Banner */}
      {incomingTrade && (
        <div className="pointer-events-auto mx-auto mt-1.5 flex items-center gap-3 rounded-xl border border-sky-500/70 bg-[#121622]/95 px-4 py-2 text-xs shadow-xl">
          <ArrowLeftRight className="w-4 h-4 text-sky-400 shrink-0" />
          <span className="text-slate-100">
            <strong>{incomingTrade.fromPlayerName}</strong> quiere intercambiar ítems contigo
          </span>
          <button
            onClick={onAcceptTrade}
            className="min-h-[36px] px-3 rounded-lg bg-emerald-500 text-slate-950 font-semibold"
          >
            Aceptar
          </button>
          <button
            onClick={onDeclineTrade}
            className="min-h-[36px] px-3 rounded-lg bg-slate-800 text-slate-300"
          >
            Rechazar
          </button>
        </div>
      )}

      {/* Middle Floating LAN Chat Drawer */}
      {connectedRoom && (
        <div className="pointer-events-auto px-3 my-auto self-start">
          {showChat ? (
            <div className="w-72 rounded-xl border border-amber-500/40 bg-[#0B0D14]/95 p-3 space-y-2 shadow-xl">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-sky-300 font-mono-tabular">Chat LAN · {connectedRoom}</span>
                <button
                  onClick={() => setShowChat(false)}
                  className="text-slate-400 hover:text-white px-2 py-1"
                >
                  Ocultar
                </button>
              </div>
              <div className="max-h-28 overflow-y-auto space-y-1 text-[11px]">
                {chatMessages.length === 0 ? (
                  <p className="text-slate-500">Sin mensajes aún en esta sala.</p>
                ) : (
                  chatMessages.slice(-8).map((m) => (
                    <p key={m.id} className="text-slate-200">
                      <strong className="text-sky-300">{m.senderName}:</strong> {m.text}
                    </p>
                  ))
                )}
              </div>
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  if (!chatInput.trim()) return;
                  onSendChat(chatInput);
                  setChatInput('');
                }}
                className="flex gap-1.5"
              >
                <input
                  type="text"
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  placeholder="Mensaje..."
                  className="flex-1 min-h-[36px] rounded-lg border border-slate-700 bg-slate-950 px-2.5 text-xs text-white focus:outline-none focus:border-sky-500"
                />
                <button
                  type="submit"
                  className="min-h-[36px] min-w-[36px] rounded-lg bg-sky-500 text-slate-950 flex items-center justify-center"
                >
                  <Send className="w-3.5 h-3.5" />
                </button>
              </form>
            </div>
          ) : (
            <button
              onClick={() => setShowChat(true)}
              className="min-h-[40px] px-3 rounded-xl border border-sky-500/40 bg-[#0B0D14]/90 text-xs font-medium text-sky-200 flex items-center gap-1.5"
            >
              <MessageSquare className="w-3.5 h-3.5" />
              <span>Chat LAN</span>
            </button>
          )}
        </div>
      )}

      {/* Nearby NPC Interaction Prompt */}
      {nearbyNpc && (
        <div className="pointer-events-auto mx-auto mb-1">
          <button
            onClick={() => onInteractNpc(nearbyNpc)}
            className="min-h-[38px] px-4 rounded-xl border-2 border-amber-400 bg-[#14101c]/95 text-xs font-bold text-amber-200 shadow-2xl flex items-center gap-2 hover:bg-amber-500 hover:text-slate-950 transition-colors"
          >
            <Store className="w-4 h-4 text-amber-400" />
            <span>Hablar con {nearbyNpc.name} ({nearbyNpc.subtitle})</span>
          </button>
        </div>
      )}

      {/* Bottom Controls + Classic MU Online HP/MP Globes & EXP Bar */}
      <div className="flex items-end justify-between gap-2 px-2.5 sm:px-5 pb-2 sm:pb-3">
        {/* Left Thumb: Virtual Analog Joystick + Auto Hunt */}
        <div className="pointer-events-auto flex flex-col items-start gap-1.5">
          <button
            onClick={onToggleAutoHunt}
            className={`min-h-[36px] px-2.5 rounded-xl border text-[11px] font-semibold flex items-center gap-1.5 transition-colors ${
              autoHunt
                ? 'bg-emerald-500 border-emerald-300 text-slate-950'
                : 'bg-[#0B0D14]/90 border-amber-500/40 text-amber-100'
            }`}
          >
            <Crosshair className="w-3.5 h-3.5" />
            <span>{autoHunt ? 'MU Helper ON' : 'MU Helper'}</span>
          </button>

          <div
            ref={joyBaseRef}
            onPointerDown={(e) => {
              activePointerId.current = e.pointerId;
              e.currentTarget.setPointerCapture(e.pointerId);
              updateJoystickFromPointer(e.clientX, e.clientY);
            }}
            onPointerMove={(e) => {
              if (activePointerId.current !== e.pointerId) return;
              updateJoystickFromPointer(e.clientX, e.clientY);
            }}
            onPointerUp={(e) => {
              if (activePointerId.current === e.pointerId) resetJoystick();
            }}
            onPointerCancel={resetJoystick}
            className="relative w-20 h-20 sm:w-24 sm:h-24 rounded-full border-2 border-amber-500/50 bg-[#0B0D14]/75 flex items-center justify-center touch-none-game shadow-xl"
          >
            <div
              style={{
                transform: `translate(${knobOffset.x}px, ${knobOffset.y}px)`,
              }}
              className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-amber-400 border-2 border-amber-100 shadow-lg pointer-events-none"
            />
          </div>
        </div>

        {/* Classic MU Online Bottom Bar: Red HP Globe + Center EXP/Zen Bar + Blue MP Globe */}
        <div className="pointer-events-auto hidden sm:flex items-end gap-1.5 md:gap-2 bg-[#0D1018]/95 border-2 border-amber-600/60 rounded-2xl px-2.5 md:px-3 py-1.5 shadow-2xl">
          {/* Red HP Globe */}
          <div className="relative w-11 h-11 md:w-13 md:h-13 rounded-full border-2 border-amber-500/70 bg-slate-950 overflow-hidden shrink-0 shadow-inner">
            <div
              className="absolute bottom-0 inset-x-0 bg-gradient-to-t from-red-700 via-red-500 to-rose-400 transition-all"
              style={{ height: `${hpPct}%` }}
            />
            <div className="relative z-10 h-full w-full flex flex-col items-center justify-center text-[9px] md:text-[10px] font-mono-tabular font-bold text-white drop-shadow">
              <span>HP</span>
              <span>{Math.round(hero.hp)}</span>
            </div>
          </div>

          {/* Center Gothic MU Bar */}
          <div className="w-40 md:w-56 space-y-1">
            <div className="flex items-center justify-between text-[10px] md:text-[11px] font-mono-tabular text-amber-200">
              <span>Nv {hero.level} · R{hero.resets}</span>
              <span className="text-amber-400 font-bold">{hero.zen.toLocaleString()} Zen</span>
            </div>
            {/* Segmented MU EXP Bar */}
            <div className="grid grid-cols-10 gap-0.5 h-2 p-0.5 rounded bg-slate-950 border border-amber-700/60">
              {Array.from({ length: 10 }).map((_, idx) => {
                const segStart = idx * 10;
                const filled = Math.max(0, Math.min(100, (expPct - segStart) * 10));
                return (
                  <div key={idx} className="bg-slate-900 overflow-hidden rounded-[1px]">
                    <div className="h-full bg-amber-400" style={{ width: `${filled}%` }} />
                  </div>
                );
              })}
            </div>
            <div className="flex items-center justify-between text-[9px] md:text-[10px] font-mono-tabular text-slate-300">
              <span>EXP {Math.round(expPct)}%</span>
              <div className="flex gap-1">
                <button
                  onClick={onOpenCharacter}
                  className="px-1.5 py-0.5 rounded border border-amber-500/40 bg-slate-900 text-amber-200 hover:border-amber-400"
                >
                  Stats
                </button>
                <button
                  onClick={onOpenInventory}
                  className="px-1.5 py-0.5 rounded border border-amber-500/40 bg-slate-900 text-amber-200 hover:border-amber-400"
                >
                  Chaos
                </button>
              </div>
            </div>
          </div>

          {/* Blue Mana Globe */}
          <div className="relative w-11 h-11 md:w-13 md:h-13 rounded-full border-2 border-amber-500/70 bg-slate-950 overflow-hidden shrink-0 shadow-inner">
            <div
              className="absolute bottom-0 inset-x-0 bg-gradient-to-t from-blue-800 via-blue-500 to-sky-400 transition-all"
              style={{ height: `${mpPct}%` }}
            />
            <div className="relative z-10 h-full w-full flex flex-col items-center justify-center text-[9px] md:text-[10px] font-mono-tabular font-bold text-white drop-shadow">
              <span>MP</span>
              <span>{Math.round(hero.mp)}</span>
            </div>
          </div>
        </div>

        {/* Right Thumb: Mobile Mini HP/MP Orbs + Potions + 3 Class Skills + Primary Attack */}
        <div className="pointer-events-auto flex flex-col items-end gap-1.5">
          {/* Compact Portrait-Only HP/MP Orbs + EXP */}
          <div className="sm:hidden flex items-center gap-1.5 rounded-xl border border-amber-500/50 bg-[#0B0D14]/92 px-2 py-1 text-[10px] font-mono-tabular">
            <span className="inline-flex items-center gap-1 text-red-300 font-bold">
              <span className="w-2 h-2 rounded-full bg-red-500 inline-block" />
              {Math.round(hpPct)}%
            </span>
            <span className="text-slate-600">·</span>
            <span className="inline-flex items-center gap-1 text-sky-300 font-bold">
              <span className="w-2 h-2 rounded-full bg-blue-500 inline-block" />
              {Math.round(mpPct)}%
            </span>
            <span className="text-slate-600">·</span>
            <span className="text-amber-300 font-bold">EXP {Math.round(expPct)}%</span>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={() => onUsePotion('hp')}
              disabled={hpPotions < 1}
              className="min-h-[36px] min-w-[42px] px-2 rounded-xl border border-red-500/50 bg-red-950/85 text-[10px] font-mono-tabular font-bold text-red-200 disabled:opacity-35 active:scale-95"
            >
              HP {hpPotions}
            </button>
            <button
              onClick={() => onUsePotion('mp')}
              disabled={mpPotions < 1}
              className="min-h-[36px] min-w-[42px] px-2 rounded-xl border border-sky-500/50 bg-sky-950/85 text-[10px] font-mono-tabular font-bold text-sky-200 disabled:opacity-35 active:scale-95"
            >
              MP {mpPotions}
            </button>
          </div>

          <div className="flex items-end gap-1.5">
            {skills.map((sk) => {
              const cdLeft = Math.max(0, (skillCooldowns[sk.id] || 0) - Date.now());
              const onCd = cdLeft > 0;
              return (
                <button
                  key={sk.id}
                  onClick={() => onCastSkill(sk.id)}
                  className="relative min-h-[44px] min-w-[44px] px-1.5 rounded-2xl border-2 border-amber-600/40 bg-[#12141e]/95 text-[10px] font-semibold text-slate-100 flex flex-col items-center justify-center active:scale-95 overflow-hidden shadow-lg"
                  style={{ borderColor: onCd ? undefined : sk.color }}
                >
                  <span className="truncate max-w-[48px]">{sk.name.split(' ')[0]}</span>
                  <span className="text-[9px] font-mono-tabular text-sky-300">
                    {onCd ? `${(cdLeft / 1000).toFixed(1)}s` : `${sk.mpCost}mp`}
                  </span>
                </button>
              );
            })}

            <button
              onClick={onAttack}
              aria-label="Atacar"
              className="min-h-[54px] min-w-[54px] rounded-full bg-gradient-to-b from-amber-400 to-amber-600 border-2 border-amber-200 text-slate-950 shadow-xl flex items-center justify-center active:scale-90 transition-transform"
            >
              <Swords className="w-6 h-6" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
