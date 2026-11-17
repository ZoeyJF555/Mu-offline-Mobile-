import React from 'react';
import { Plus, RotateCcw, X } from 'lucide-react';
import { CLASS_CATALOG, computeDerivedHeroStats } from '../data/muCatalog';
import type { HeroSave, HeroStats } from '../types/game';

interface CharacterModalProps {
  hero: HeroSave;
  resetLevelReq: number;
  onAddStat: (stat: keyof HeroStats, amount: number) => void;
  onResetHero: () => void;
  onClose: () => void;
}

export const CharacterModal: React.FC<CharacterModalProps> = ({
  hero,
  resetLevelReq,
  onAddStat,
  onResetHero,
  onClose,
}) => {
  const derived = computeDerivedHeroStats(hero);
  const meta = CLASS_CATALOG[hero.classId];
  const canReset = hero.level >= resetLevelReq;

  const statRows: { key: keyof HeroStats; label: string; desc: string }[] = [
    { key: 'str', label: 'Fuerza (STR)', desc: 'Aumenta daño físico de espadas y arcos' },
    { key: 'agi', label: 'Agilidad (AGI)', desc: 'Defensa, velocidad de ataque y crítico' },
    { key: 'vit', label: 'Vitalidad (VIT)', desc: 'Aumenta tu vida máxima (+6 HP por punto)' },
    { key: 'ene', label: 'Energía (ENE)', desc: 'Daño mágico de Dark Wizard y Mana máximo' },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/75 backdrop-blur-sm sm:p-4">
      <div className="w-full max-w-lg rounded-t-3xl sm:rounded-2xl border border-slate-700 bg-[#111520] max-h-[88vh] flex flex-col shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <div>
            <h2 className="font-display text-lg font-bold text-amber-200">{hero.name}</h2>
            <p className="text-xs text-slate-400">
              {meta.name} · Nivel <span className="font-mono-tabular text-amber-300">{hero.level}</span> · Resets <span className="font-mono-tabular text-sky-300">{hero.resets}</span>
            </p>
          </div>
          <button
            onClick={onClose}
            aria-label="Cerrar personaje"
            className="min-h-[44px] min-w-[44px] rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-300 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="overflow-y-auto p-5 space-y-5">
          {/* Derived Summary */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 rounded-xl border border-slate-800 bg-[#0B0D13] p-3.5">
            <div>
              <span className="text-[11px] text-slate-400 block">Ataque</span>
              <span className="font-mono-tabular text-base font-bold text-amber-300">{derived.attackPower}</span>
            </div>
            <div>
              <span className="text-[11px] text-slate-400 block">Defensa</span>
              <span className="font-mono-tabular text-base font-bold text-sky-300">{derived.defense}</span>
            </div>
            <div>
              <span className="text-[11px] text-slate-400 block">Crítico</span>
              <span className="font-mono-tabular text-base font-bold text-emerald-300">{Math.round(derived.critChance * 100)}%</span>
            </div>
            <div>
              <span className="text-[11px] text-slate-400 block">Bajas</span>
              <span className="font-mono-tabular text-base font-bold text-slate-200">{hero.kills}</span>
            </div>
          </div>

          {/* Points Allocation */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-semibold text-slate-200">Atributos Base</h3>
              <span className="text-xs font-mono-tabular text-amber-300">
                Puntos disponibles: <strong>{hero.statPoints}</strong>
              </span>
            </div>

            <div className="space-y-2.5">
              {statRows.map((row) => (
                <div
                  key={row.key}
                  className="flex items-center justify-between gap-3 rounded-xl border border-slate-800 bg-[#161B2A] px-3.5 py-2.5"
                >
                  <div className="min-w-0">
                    <div className="flex items-baseline gap-2">
                      <span className="text-sm font-semibold text-slate-100">{row.label}</span>
                      <span className="font-mono-tabular text-sm font-bold text-amber-300">
                        {hero.stats[row.key]}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-400 truncate">{row.desc}</p>
                  </div>
                  <div className="flex items-center gap-1.5 shrink-0">
                    <button
                      disabled={hero.statPoints < 1}
                      onClick={() => onAddStat(row.key, 1)}
                      className="min-h-[44px] min-w-[44px] rounded-xl bg-slate-800 text-xs font-semibold text-slate-100 hover:bg-amber-500 hover:text-slate-950 disabled:opacity-35 transition-colors flex items-center justify-center"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                    <button
                      disabled={hero.statPoints < 10}
                      onClick={() => onAddStat(row.key, 10)}
                      className="min-h-[44px] px-2.5 rounded-xl bg-slate-800 text-xs font-mono-tabular font-semibold text-slate-100 hover:bg-amber-500 hover:text-slate-950 disabled:opacity-35 transition-colors"
                    >
                      +10
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Master Reset */}
          <div className="rounded-xl border border-slate-800 bg-[#0B0D13] p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h4 className="text-sm font-semibold text-slate-100">Master Reset</h4>
              <p className="text-xs text-slate-400 mt-0.5">
                Al llegar al nivel <span className="font-mono-tabular text-amber-300">{resetLevelReq}</span> reinicias a Nivel 1 conservando equipo y ganando +150 puntos extra por Reset.
              </p>
            </div>
            <button
              disabled={!canReset}
              onClick={onResetHero}
              className="min-h-[44px] px-4 rounded-xl bg-sky-500 text-slate-950 font-semibold text-xs hover:bg-sky-400 disabled:opacity-35 transition-colors flex items-center justify-center gap-1.5 whitespace-nowrap shrink-0"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Hacer Reset</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
