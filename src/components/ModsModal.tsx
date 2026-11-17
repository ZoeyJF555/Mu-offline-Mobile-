import React, { useRef, useState } from 'react';
import { Download, Package, Plus, Trash2, Upload, X } from 'lucide-react';
import type { ModPackage } from '../types/game';

interface ModsModalProps {
  mods: ModPackage[];
  onToggleMod: (modId: string) => void;
  onAddMod: (mod: ModPackage) => void;
  onDeleteMod: (modId: string) => void;
  onClose: () => void;
}

export const ModsModal: React.FC<ModsModalProps> = ({
  mods,
  onToggleMod,
  onAddMod,
  onDeleteMod,
  onClose,
}) => {
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [author, setAuthor] = useState('');
  const [expMul, setExpMul] = useState(5);
  const [zenMul, setZenMul] = useState(5);
  const [dropMul, setDropMul] = useState(1.5);
  const [customWeaponName, setCustomWeaponName] = useState('');
  const [customWeaponAtk, setCustomWeaponAtk] = useState(85);
  const [customBossName, setCustomBossName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleCreateMod = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Escribe un nombre para tu Mod.');
      return;
    }
    const id = `custom-mod-${Date.now()}`;
    const newMod: ModPackage = {
      id,
      name: name.trim(),
      author: author.trim() || 'Jugador Local',
      version: '1.0.0',
      description: `Mod personalizado (EXP x${expMul}, Zen x${zenMul}, Drop x${dropMul}).`,
      enabled: true,
      builtIn: false,
      expMultiplier: Math.max(0.1, Number(expMul) || 1),
      zenMultiplier: Math.max(0.1, Number(zenMul) || 1),
      dropRateMultiplier: Math.max(0.2, Number(dropMul) || 1),
      chaosSuccessBonus: 10,
      resetLevel: 120,
      customItems: customWeaponName.trim()
        ? [
            {
              templateId: `${id}-item`,
              name: customWeaponName.trim(),
              slot: 'weapon',
              rarity: 'mythic',
              basePlus: 7,
              attack: Math.max(10, Number(customWeaponAtk) || 60),
              magic: Math.max(10, Number(customWeaponAtk) || 60),
              classReq: 'all',
              colorHex: '#F43F5E',
              valueZen: 25000,
            },
          ]
        : [],
      customMonsters: customBossName.trim()
        ? [
            {
              id: `${id}-boss`,
              name: customBossName.trim(),
              mapId: 'custom',
              level: 45,
              maxHp: 3200,
              attack: 75,
              defense: 25,
              expReward: 2200,
              zenReward: 8500,
              color: '#F43F5E',
              isBoss: true,
            },
          ]
        : [],
    };
    onAddMod(newMod);
    setCreating(false);
    setName('');
    setCustomWeaponName('');
    setCustomBossName('');
    setError(null);
  };

  const handleExport = (mod: ModPackage) => {
    const blob = new Blob([JSON.stringify(mod, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${mod.name.toLowerCase().replace(/[^a-z0-9]+/g, '-')}.mu-mod.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleImportFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const text = await file.text();
      const parsed = JSON.parse(text) as Partial<ModPackage>;
      if (!parsed.name) throw new Error('Archivo inválido');
      const imported: ModPackage = {
        id: `imported-${Date.now()}`,
        name: String(parsed.name),
        author: String(parsed.author || 'Importado'),
        version: String(parsed.version || '1.0.0'),
        description: String(parsed.description || 'Mod importado localmente.'),
        enabled: true,
        builtIn: false,
        expMultiplier: Number(parsed.expMultiplier ?? 1),
        zenMultiplier: Number(parsed.zenMultiplier ?? 1),
        dropRateMultiplier: Number(parsed.dropRateMultiplier ?? 1),
        chaosSuccessBonus: Number(parsed.chaosSuccessBonus ?? 0),
        resetLevel: Number(parsed.resetLevel ?? 400),
        customItems: Array.isArray(parsed.customItems) ? parsed.customItems : [],
        customMonsters: Array.isArray(parsed.customMonsters) ? parsed.customMonsters : [],
      };
      onAddMod(imported);
      setError(null);
    } catch {
      setError('No se pudo leer el archivo .json del Mod. Verifica el formato.');
    } finally {
      e.target.value = '';
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/75 backdrop-blur-sm sm:p-4">
      <div className="w-full max-w-2xl rounded-t-3xl sm:rounded-2xl border border-slate-700 bg-[#111520] max-h-[90vh] flex flex-col shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <Package className="w-5 h-5 text-amber-400" />
            <div>
              <h2 className="font-display text-lg font-bold text-amber-200">Gestor de Mods Offline</h2>
              <p className="text-xs text-slate-400">
                Activa paquetes de tasas, armas míticas y jefes o importa archivos .json sin internet.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            aria-label="Cerrar mods"
            className="min-h-[44px] min-w-[44px] rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-300 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Actions */}
        <div className="flex flex-wrap items-center gap-2 px-5 py-3 border-b border-slate-800/80 bg-[#0D1018]">
          <button
            onClick={() => setCreating((v) => !v)}
            className="min-h-[40px] px-3.5 rounded-xl bg-amber-500 text-slate-950 text-xs font-semibold hover:bg-amber-400 flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" />
            <span>{creating ? 'Volver a la lista' : 'Crear Mod Propio'}</span>
          </button>
          <button
            onClick={() => fileInputRef.current?.click()}
            className="min-h-[40px] px-3.5 rounded-xl border border-slate-700 bg-slate-900 text-xs font-semibold text-slate-200 hover:border-amber-500/40 flex items-center gap-1.5"
          >
            <Upload className="w-4 h-4 text-sky-400" />
            <span>Importar .json</span>
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".json,application/json"
            onChange={handleImportFile}
            className="hidden"
          />
        </div>

        {error && (
          <div className="mx-5 mt-3 rounded-xl border border-red-500/40 bg-red-500/10 px-3.5 py-2 text-xs text-red-200">
            {error}
          </div>
        )}

        <div className="overflow-y-auto p-5 space-y-3 flex-1">
          {creating ? (
            <form onSubmit={handleCreateMod} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-300 mb-1">Nombre del Mod</label>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Ej. Mu Season 6 Custom"
                    className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3 text-sm text-white focus:border-amber-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-300 mb-1">Autor</label>
                  <input
                    type="text"
                    value={author}
                    onChange={(e) => setAuthor(e.target.value)}
                    placeholder="Tu nombre"
                    className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3 text-sm text-white focus:border-amber-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs text-slate-300 mb-1">EXP (x)</label>
                  <input
                    type="number"
                    step="0.5"
                    min="0.1"
                    max="200"
                    value={expMul}
                    onChange={(e) => setExpMul(Number(e.target.value))}
                    className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3 text-sm font-mono-tabular text-white focus:border-amber-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-300 mb-1">Zen (x)</label>
                  <input
                    type="number"
                    step="0.5"
                    min="0.1"
                    max="200"
                    value={zenMul}
                    onChange={(e) => setZenMul(Number(e.target.value))}
                    className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3 text-sm font-mono-tabular text-white focus:border-amber-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-300 mb-1">Drop (x)</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0.2"
                    max="10"
                    value={dropMul}
                    onChange={(e) => setDropMul(Number(e.target.value))}
                    className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3 text-sm font-mono-tabular text-white focus:border-amber-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-slate-800">
                <div>
                  <label className="block text-xs text-slate-300 mb-1">Arma Custom en Drops (opcional)</label>
                  <input
                    type="text"
                    value={customWeaponName}
                    onChange={(e) => setCustomWeaponName(e.target.value)}
                    placeholder="Ej. Espada del Caos"
                    className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3 text-sm text-white focus:border-amber-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-300 mb-1">Ataque Base del Arma</label>
                  <input
                    type="number"
                    min="10"
                    max="500"
                    value={customWeaponAtk}
                    onChange={(e) => setCustomWeaponAtk(Number(e.target.value))}
                    className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3 text-sm font-mono-tabular text-white focus:border-amber-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-300 mb-1">Jefe Custom en Arena Mod (opcional)</label>
                <input
                  type="text"
                  value={customBossName}
                  onChange={(e) => setCustomBossName(e.target.value)}
                  placeholder="Ej. Kundun Ancestral"
                  className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3 text-sm text-white focus:border-amber-500 focus:outline-none"
                />
              </div>

              <button
                type="submit"
                className="w-full min-h-[44px] rounded-xl bg-amber-500 text-slate-950 font-semibold text-sm hover:bg-amber-400"
              >
                Guardar y Activar Mod Offline
              </button>
            </form>
          ) : (
            mods.map((mod) => (
              <div
                key={mod.id}
                className={`rounded-xl border p-3.5 transition-colors ${
                  mod.enabled
                    ? 'border-amber-500/40 bg-[#161B2A]'
                    : 'border-slate-800 bg-[#0D1018]'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-baseline gap-2">
                      <h3 className="text-sm font-bold text-slate-100">{mod.name}</h3>
                      <span className="text-[11px] font-mono-tabular text-slate-400">
                        v{mod.version} · por {mod.author}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-1">{mod.description}</p>
                    <p className="text-[11px] font-mono-tabular text-amber-300/90 mt-1.5">
                      EXP x{mod.expMultiplier} · Zen x{mod.zenMultiplier} · Drop x{mod.dropRateMultiplier}
                      {mod.customItems.length > 0 ? ` · +${mod.customItems.length} ítems` : ''}
                      {mod.customMonsters.length > 0 ? ` · +${mod.customMonsters.length} jefes` : ''}
                    </p>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0">
                    <button
                      onClick={() => handleExport(mod)}
                      title="Exportar .json para compartir"
                      className="min-h-[40px] min-w-[40px] rounded-xl border border-slate-700 bg-slate-900 text-slate-300 hover:text-amber-300 flex items-center justify-center"
                    >
                      <Download className="w-4 h-4" />
                    </button>
                    {!mod.builtIn && (
                      <button
                        onClick={() => onDeleteMod(mod.id)}
                        title="Eliminar mod"
                        className="min-h-[40px] min-w-[40px] rounded-xl border border-slate-700 bg-slate-900 text-slate-400 hover:text-red-400 flex items-center justify-center"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                    <button
                      onClick={() => onToggleMod(mod.id)}
                      className={`min-h-[40px] px-3.5 rounded-xl text-xs font-semibold transition-colors ${
                        mod.enabled
                          ? 'bg-emerald-500 text-slate-950 hover:bg-emerald-400'
                          : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                      }`}
                    >
                      {mod.enabled ? 'Activo' : 'Activar'}
                    </button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
