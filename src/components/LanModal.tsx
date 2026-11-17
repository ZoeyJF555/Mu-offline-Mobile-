import React, { useEffect, useState } from 'react';
import {
  ArrowLeftRight,
  Bluetooth,
  Copy,
  Flame,
  Gamepad2,
  LogOut,
  RefreshCw,
  Share2,
  Users,
  Wifi,
  X,
} from 'lucide-react';
import type { LanEventState, LanPlayer } from '../types/game';

interface LanModalProps {
  localHeroId: string | null;
  connectedRoom: string | null;
  lanPlayers: LanPlayer[];
  activeEvent: LanEventState | null;
  lanStatus: 'disconnected' | 'connecting' | 'connected' | 'error';
  lanError: string | null;
  gamepadName: string | null;
  onJoinRoom: (roomCode: string) => void;
  onLeaveRoom: () => void;
  onRequestTrade: (targetPlayerId: string) => void;
  onStartEvent: (eventType: 'golden-invasion' | 'blood-castle') => void;
  onClose: () => void;
}

interface LanInfoResponse {
  port: number;
  lanUrls: string[];
  activeRoomCount: number;
}

interface ActiveRoomSummary {
  code: string;
  hostName: string;
  playerCount: number;
}

export const LanModal: React.FC<LanModalProps> = ({
  localHeroId,
  connectedRoom,
  lanPlayers,
  activeEvent,
  lanStatus,
  lanError,
  gamepadName,
  onJoinRoom,
  onLeaveRoom,
  onRequestTrade,
  onStartEvent,
  onClose,
}) => {
  const [roomInput, setRoomInput] = useState(connectedRoom || 'LORENCIA-01');
  const [lanInfo, setLanInfo] = useState<LanInfoResponse | null>(null);
  const [rooms, setRooms] = useState<ActiveRoomSummary[]>([]);
  const [copiedUrl, setCopiedUrl] = useState<string | null>(null);
  const [bleDeviceName, setBleDeviceName] = useState<string | null>(null);
  const [bleMessage, setBleMessage] = useState<string | null>(null);

  const fetchLanStatus = async () => {
    try {
      const [infoRes, roomsRes] = await Promise.all([
        fetch('/api/lan/info'),
        fetch('/api/lan/rooms'),
      ]);
      if (infoRes.ok) setLanInfo((await infoRes.json()) as LanInfoResponse);
      if (roomsRes.ok) {
        const data = (await roomsRes.json()) as { rooms: ActiveRoomSummary[] };
        setRooms(data.rooms || []);
      }
    } catch {
      // Offline mode
    }
  };

  useEffect(() => {
    fetchLanStatus();
  }, [connectedRoom]);

  const copyText = (value: string) => {
    navigator.clipboard?.writeText(value).catch(() => {});
    setCopiedUrl(value);
    setTimeout(() => setCopiedUrl(null), 1800);
  };

  const shareInvite = () => {
    const code = connectedRoom || roomInput || 'LORENCIA-01';
    const inviteUrl = `${window.location.origin}/?room=${encodeURIComponent(code)}`;
    const text = `Únete a mi servidor privado de Mu Offline Mobile! Sala: ${code} · Enlace: ${inviteUrl}`;
    if (navigator.share) {
      navigator.share({ title: 'Mu Offline Mobile - Servidor Privado', text, url: inviteUrl }).catch(() => {
        copyText(text);
      });
    } else {
      copyText(text);
    }
  };

  const scanBluetoothDevice = async () => {
    const nav = navigator as unknown as {
      bluetooth?: { requestDevice: (opts: { acceptAllDevices: boolean }) => Promise<{ name?: string }> };
    };
    if (!nav.bluetooth) {
      setBleMessage('Este navegador/iframe bloquea Web Bluetooth; usa Hotspot Wi-Fi + Sala LAN para conectar teléfonos.');
      return;
    }
    try {
      const device = await nav.bluetooth.requestDevice({ acceptAllDevices: true });
      setBleDeviceName(device.name || 'Dispositivo BLE emparejado');
      setBleMessage('Dispositivo Bluetooth detectado. Recuerda que los eventos y comercio entre teléfonos usan Wi-Fi LAN.');
    } catch {
      setBleMessage('Búsqueda Bluetooth cancelada o no disponible en este dispositivo.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/75 backdrop-blur-sm sm:p-4">
      <div className="w-full max-w-xl rounded-t-3xl sm:rounded-2xl border border-slate-700 bg-[#111520] max-h-[90vh] flex flex-col shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <Wifi className="w-5 h-5 text-sky-400" />
            <div>
              <h2 className="font-display text-lg font-bold text-slate-100">
                Servidor Privado · Wi-Fi LAN y Bluetooth
              </h2>
              <p className="text-xs text-slate-400">
                Invita amigos, ayuda en eventos cooperativos e intercambia ítems y Zen.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            aria-label="Cerrar servidor privado"
            className="min-h-[44px] min-w-[44px] rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-300 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="overflow-y-auto p-5 space-y-5">
          {/* Room Join & Invite */}
          <div className="rounded-xl border border-slate-800 bg-[#161B2A] p-4 space-y-3">
            <div className="flex items-center justify-between gap-2">
              <h3 className="text-sm font-semibold text-slate-100">Sala del Servidor Privado</h3>
              <div className="flex items-center gap-2">
                <button
                  onClick={shareInvite}
                  className="min-h-[36px] px-2.5 rounded-lg bg-amber-500 text-slate-950 text-xs font-semibold hover:bg-amber-400 flex items-center gap-1"
                >
                  <Share2 className="w-3.5 h-3.5" />
                  <span>{copiedUrl ? 'Invitación copiada' : 'Invitar'}</span>
                </button>
                <button
                  onClick={fetchLanStatus}
                  className="min-h-[36px] px-2.5 rounded-lg bg-slate-900 border border-slate-700 text-xs text-slate-300 hover:text-white flex items-center gap-1"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {connectedRoom ? (
              <div className="space-y-3">
                <div className="flex items-center justify-between gap-2 rounded-xl border border-emerald-500/40 bg-emerald-500/10 px-3.5 py-2.5">
                  <div>
                    <span className="text-xs font-semibold text-emerald-300 block">
                      Sala Activa: <span className="font-mono-tabular">{connectedRoom}</span>
                    </span>
                    <span className="text-[11px] text-slate-300">
                      {lanPlayers.length} jugador(es) conectados
                    </span>
                  </div>
                  <button
                    onClick={onLeaveRoom}
                    className="min-h-[40px] px-3 rounded-lg bg-slate-900 border border-slate-700 text-xs font-semibold text-red-300 hover:border-red-500/40 flex items-center gap-1.5"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>Salir</span>
                  </button>
                </div>

                {/* Players + Trade Button */}
                <div className="space-y-1.5">
                  <span className="text-[11px] text-slate-400 block">Jugadores en el servidor privado:</span>
                  {lanPlayers.map((p) => {
                    const isMe = p.id === localHeroId;
                    return (
                      <div
                        key={p.id}
                        className="flex items-center justify-between gap-2 rounded-lg bg-[#0D1018] px-3 py-2 text-xs"
                      >
                        <div>
                          <span className="font-semibold text-slate-100">
                            {p.name} {isMe ? '(Tú)' : ''}
                          </span>
                          <span className="block text-[11px] font-mono-tabular text-slate-400">
                            {p.classId.toUpperCase()} · Nv {p.level} · {p.mapId}
                          </span>
                        </div>
                        {!isMe && (
                          <button
                            onClick={() => {
                              onRequestTrade(p.id);
                              onClose();
                            }}
                            className="min-h-[38px] px-3 rounded-lg border border-amber-500/40 bg-amber-500/10 text-xs font-semibold text-amber-300 hover:bg-amber-500/20 flex items-center gap-1.5"
                          >
                            <ArrowLeftRight className="w-3.5 h-3.5" />
                            <span>Intercambiar</span>
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Cooperative Private Server Events */}
                <div className="pt-3 border-t border-slate-800 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-amber-200 flex items-center gap-1.5">
                      <Flame className="w-4 h-4 text-amber-400" />
                      <span>Eventos Cooperativos del Servidor</span>
                    </span>
                    {activeEvent?.active && (
                      <span className="text-[11px] font-mono-tabular text-emerald-300">
                        Activo: {activeEvent.name}
                      </span>
                    )}
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <button
                      onClick={() => {
                        onStartEvent('golden-invasion');
                        onClose();
                      }}
                      className="min-h-[42px] rounded-xl border border-amber-500/40 bg-amber-500/10 px-3 text-xs font-semibold text-amber-200 hover:bg-amber-500/20"
                    >
                      Lanzar Invasión Dorada (Lorencia)
                    </button>
                    <button
                      onClick={() => {
                        onStartEvent('blood-castle');
                        onClose();
                      }}
                      className="min-h-[42px] rounded-xl border border-purple-500/40 bg-purple-500/10 px-3 text-xs font-semibold text-purple-200 hover:bg-purple-500/20"
                    >
                      Lanzar Blood Castle (Dungeon)
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  onJoinRoom(roomInput);
                }}
                className="flex flex-col sm:flex-row gap-2.5"
              >
                <input
                  type="text"
                  value={roomInput}
                  onChange={(e) => setRoomInput(e.target.value.toUpperCase())}
                  placeholder="Código de sala (ej. LORENCIA-01)"
                  className="flex-1 min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3.5 text-sm font-mono-tabular text-white focus:border-sky-500 focus:outline-none"
                />
                <button
                  type="submit"
                  disabled={lanStatus === 'connecting'}
                  className="min-h-[44px] px-5 rounded-xl bg-sky-500 text-slate-950 font-semibold text-sm hover:bg-sky-400 whitespace-nowrap shrink-0"
                >
                  {lanStatus === 'connecting' ? 'Conectando...' : 'Unirse / Crear Sala'}
                </button>
              </form>
            )}

            {lanError && <p className="text-xs text-amber-300">{lanError}</p>}

            {!connectedRoom && rooms.length > 0 && (
              <div className="pt-2 border-t border-slate-800 space-y-1.5">
                <span className="text-[11px] font-medium text-slate-400 block">Salas activas en tu red:</span>
                {rooms.map((r) => (
                  <button
                    key={r.code}
                    type="button"
                    onClick={() => onJoinRoom(r.code)}
                    className="w-full min-h-[44px] flex items-center justify-between rounded-xl border border-slate-800 bg-[#0D1018] px-3 text-xs hover:border-sky-500/40"
                  >
                    <span className="font-mono-tabular font-semibold text-sky-300">{r.code}</span>
                    <span className="text-slate-400 flex items-center gap-1.5">
                      <Users className="w-3.5 h-3.5" />
                      {r.playerCount} · Host: {r.hostName}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Local LAN IPs */}
          {lanInfo && lanInfo.lanUrls.length > 0 && (
            <div className="rounded-xl border border-slate-800 bg-[#0D1018] p-4 space-y-2">
              <h4 className="text-xs font-semibold text-slate-300">
                Enlaces Wi-Fi LAN para abrir desde otros teléfonos en la misma red:
              </h4>
              {lanInfo.lanUrls.map((url) => {
                const fullUrl = `${url}/?room=${encodeURIComponent(connectedRoom || roomInput || 'LORENCIA-01')}`;
                return (
                  <div key={url} className="flex items-center justify-between gap-2 rounded-lg bg-slate-900 px-3 py-2">
                    <span className="text-xs font-mono-tabular text-sky-300 truncate">{fullUrl}</span>
                    <button
                      onClick={() => copyText(fullUrl)}
                      className="min-h-[36px] px-2.5 rounded-lg bg-slate-800 text-xs text-slate-200 hover:text-white flex items-center gap-1 shrink-0"
                    >
                      <Copy className="w-3.5 h-3.5" />
                      <span>{copiedUrl === fullUrl ? 'Copiado' : 'Copiar'}</span>
                    </button>
                  </div>
                );
              })}
            </div>
          )}

          {/* Bluetooth Scanner + Gamepad + Hotspot Guide */}
          <div className="rounded-xl border border-slate-800 bg-[#0D1018] p-4 space-y-3">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <Bluetooth className="w-5 h-5 text-sky-400 shrink-0 mt-0.5" />
                <div>
                  <h4 className="text-sm font-semibold text-slate-100">
                    Bluetooth y Wi-Fi Hotspot del Dispositivo
                  </h4>
                  <p className="text-xs leading-relaxed text-slate-400 mt-0.5">
                    Usa el <strong>Hotspot Wi-Fi</strong> de un teléfono + la misma <strong>Sala LAN</strong> para invitar amigos, ayudarse en eventos e intercambiar ítems sin internet. Bluetooth en navegador permite detectar mandos y dispositivos BLE cercanos.
                  </p>
                </div>
              </div>
              <button
                onClick={scanBluetoothDevice}
                className="min-h-[38px] px-3 rounded-lg border border-sky-500/40 bg-sky-500/10 text-xs font-semibold text-sky-200 hover:bg-sky-500/20 shrink-0"
              >
                Buscar Bluetooth
              </button>
            </div>

            {(bleDeviceName || bleMessage) && (
              <p className="text-xs text-sky-300">
                {bleDeviceName ? `Emparejado: ${bleDeviceName}. ` : ''}
                {bleMessage}
              </p>
            )}

            <div className="flex items-center justify-between gap-2 pt-2 border-t border-slate-800">
              <div className="flex items-center gap-2 text-xs">
                <Gamepad2 className="w-4 h-4 text-amber-400" />
                <span className="text-slate-300">Mando Bluetooth conectado:</span>
              </div>
              <span className="text-xs font-mono-tabular text-slate-200">
                {gamepadName ? gamepadName : 'Ninguno detectado'}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
