import React, { useState } from 'react';
import { Award, LogOut, Package, Play, Plus, Shield, Trash2, Wifi } from 'lucide-react';
import { CLASS_CATALOG, createStarterHero } from '../data/muCatalog';
import type { ClassId, HeroSave, ModPackage } from '../types/game';
import { PWAInstallButton, OfflineStatusText } from './PWAInstallButton';

interface TitleScreenProps {
  accountId: string;
  heroes: HeroSave[];
  mods: ModPackage[];
  onSelectHero: (hero: HeroSave) => void;
  onCreateHero: (hero: HeroSave) => void;
  onDeleteHero: (heroId: string) => void;
  onOpenMods: () => void;
  onOpenLanHelp: () => void;
  onOpenCredits: () => void;
  onLogoutAccount: () => void;
}

export const TitleScreen: React.FC<TitleScreenProps> = ({
  accountId,
  heroes,
  mods,
  onSelectHero,
  onCreateHero,
  onDeleteHero,
  onOpenMods,
  onOpenLanHelp,
  onOpenCredits,
  onLogoutAccount,
}) => {
  const [creating, setCreating] = useState(heroes.length === 0);
  const [selectedClass, setSelectedClass] = useState<ClassId>('dk');
  const [heroName, setHeroName] = useState('');
  const [imgErrors, setImgErrors] = useState<Record<string, boolean>>({});

  const enabledModsCount = mods.filter((m) => m.enabled).length;

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    const hero = { ...createStarterHero(heroName, selectedClass), accountId };
    onCreateHero(hero);
  };

  return (
    <div className="h-full w-full overflow-y-auto bg-[#0B0D13] text-slate-100">
      <header className="sticky top-0 z-20 flex items-center justify-between gap-4 border-b border-slate-800/80 bg-[#0B0D13]/90 backdrop-blur-md px-4 sm:px-6 h-14">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-8 h-8 rounded-lg overflow-hidden border border-amber-500/40 bg-slate-900 shrink-0 flex items-center justify-center">
            {!imgErrors.crest ? (
              <img
                src="/src/assets/images/mu_crest_emblem_1794870013988.jpg"
                alt="Mu Offline Crest"
                referrerPolicy="no-referrer"
                onError={() => setImgErrors((p) => ({ ...p, crest: true }))}
                className="w-full h-full object-cover"
              />
            ) : (
              <Shield className="w-4 h-4 text-amber-400" />
            )}
          </div>
          <span className="font-display text-base sm:text-lg font-bold tracking-wide text-amber-200 whitespace-nowrap shrink-0">
            Mu Offline Mobile
          </span>
        </div>

        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <OfflineStatusText />
          <button
            onClick={onOpenMods}
            className="min-h-[44px] px-3 rounded-xl border border-slate-700 bg-slate-900/90 text-xs font-medium text-slate-200 hover:border-amber-500/50 hover:text-amber-200 transition-colors flex items-center gap-1.5 whitespace-nowrap shrink-0"
          >
            <Package className="w-4 h-4 text-amber-400" />
            <span>Mods ({enabledModsCount})</span>
          </button>
          <button
            onClick={onOpenLanHelp}
            className="min-h-[44px] px-3 rounded-xl border border-slate-700 bg-slate-900/90 text-xs font-medium text-slate-200 hover:border-sky-500/50 hover:text-sky-200 transition-colors flex items-center gap-1.5 whitespace-nowrap shrink-0"
          >
            <Wifi className="w-4 h-4 text-sky-400" />
            <span className="hidden sm:inline">Servidor LAN</span>
          </button>
          <button
            onClick={onOpenCredits}
            title="Créditos (Ctrl+F8)"
            className="min-h-[44px] px-3 rounded-xl border border-slate-700 bg-slate-900/90 text-xs font-medium text-slate-300 hover:border-amber-500/50 hover:text-amber-200 transition-colors flex items-center gap-1.5 whitespace-nowrap shrink-0"
          >
            <Award className="w-4 h-4 text-amber-400" />
            <span className="hidden md:inline">Créditos</span>
          </button>
          <PWAInstallButton compact />
          <button
            onClick={onLogoutAccount}
            title="Cambiar cuenta"
            className="min-h-[44px] px-3 rounded-xl border border-slate-700 bg-slate-900/90 text-xs font-medium text-slate-300 hover:text-white flex items-center gap-1.5 whitespace-nowrap shrink-0"
          >
            <LogOut className="w-4 h-4" />
            <span className="hidden md:inline font-mono-tabular">{accountId}</span>
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 sm:px-6 py-6 sm:py-10 pb-20">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-slate-800/80 pb-6">
          <div>
            <p className="text-xs font-medium text-amber-400 tracking-wide">
              Cuenta: <span className="font-mono-tabular">{accountId}</span> · Guardado Offline · Servidor Privado LAN
            </p>
            <h1 className="font-display mt-1 text-2xl sm:text-4xl font-bold text-slate-50 text-balance">
              Elige tu Héroe de Lorencia
            </h1>
          </div>
          {heroes.length > 0 && (
            <button
              onClick={() => setCreating((v) => !v)}
              className="min-h-[44px] px-4 rounded-xl border border-amber-500/40 bg-amber-500/10 text-sm font-semibold text-amber-300 hover:bg-amber-500/20 transition-colors flex items-center justify-center gap-2 whitespace-nowrap shrink-0"
            >
              <Plus className="w-4 h-4" />
              <span>{creating ? 'Ver Héroes Guardados' : 'Crear Nuevo Héroe'}</span>
            </button>
          )}
        </div>

        {!creating && heroes.length > 0 && (
          <section className="mt-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {heroes.map((hero) => {
              const meta = CLASS_CATALOG[hero.classId];
              return (
                <div
                  key={hero.id}
                  className="group relative flex flex-col justify-between rounded-2xl border border-slate-800 bg-[#121622] p-4 hover:border-amber-500/40 transition-colors"
                >
                  <div className="flex items-start gap-4">
                    <div className="w-20 h-20 rounded-xl overflow-hidden border border-slate-700 bg-slate-900 shrink-0 flex items-center justify-center">
                      {!imgErrors[hero.classId] ? (
                        <img
                          src={meta.portrait}
                          alt={meta.name}
                          referrerPolicy="no-referrer"
                          onError={() => setImgErrors((p) => ({ ...p, [hero.classId]: true }))}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <Shield className="w-8 h-8 text-amber-400" />
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <h2 className="font-display text-lg font-bold text-slate-50 truncate">
                        {hero.name}
                      </h2>
                      <p className="text-xs text-slate-400 mt-0.5">
                        {meta.name} · Nivel <span className="font-mono-tabular text-amber-300">{hero.level}</span> · Resets <span className="font-mono-tabular text-sky-300">{hero.resets}</span>
                      </p>
                      <p className="text-xs text-slate-400 mt-1 font-mono-tabular">
                        Mapa: {hero.mapId.toUpperCase()} · {hero.zen.toLocaleString()} Zen
                      </p>
                      <p className="text-xs text-slate-500 mt-1 font-mono-tabular">
                        STR {hero.stats.str} · AGI {hero.stats.agi} · VIT {hero.stats.vit} · ENE {hero.stats.ene}
                      </p>
                    </div>
                  </div>

                  <div className="mt-4 flex items-center gap-2 pt-3 border-t border-slate-800/80">
                    <button
                      onClick={() => onSelectHero(hero)}
                      className="flex-1 min-h-[44px] rounded-xl bg-amber-500 text-slate-950 font-semibold text-sm hover:bg-amber-400 active:scale-[0.98] transition-all flex items-center justify-center gap-2"
                    >
                      <Play className="w-4 h-4 fill-current" />
                      <span>Entrar al Mundo</span>
                    </button>
                    <button
                      onClick={() => onDeleteHero(hero.id)}
                      aria-label={`Eliminar ${hero.name}`}
                      className="min-h-[44px] min-w-[44px] rounded-xl border border-slate-800 bg-slate-900 text-slate-400 hover:text-red-400 hover:border-red-500/40 transition-colors flex items-center justify-center"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </section>
        )}

        {creating && (
          <form onSubmit={handleCreate} className="mt-6 space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {(Object.keys(CLASS_CATALOG) as ClassId[]).map((cid) => {
                const meta = CLASS_CATALOG[cid];
                const isSelected = selectedClass === cid;
                return (
                  <button
                    type="button"
                    key={cid}
                    onClick={() => setSelectedClass(cid)}
                    className={`text-left rounded-2xl border p-4 transition-all ${
                      isSelected
                        ? 'border-amber-500 bg-[#171C2B] shadow-lg shadow-amber-500/10'
                        : 'border-slate-800 bg-[#121622] hover:border-slate-700'
                    }`}
                  >
                    <div className="aspect-square w-full rounded-xl overflow-hidden border border-slate-800 bg-slate-950 mb-4 flex items-center justify-center">
                      {!imgErrors[cid] ? (
                        <img
                          src={meta.portrait}
                          alt={meta.name}
                          referrerPolicy="no-referrer"
                          onError={() => setImgErrors((p) => ({ ...p, [cid]: true }))}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <Shield className="w-12 h-12 text-amber-400" />
                      )}
                    </div>
                    <div className="flex items-baseline justify-between gap-2">
                      <h3 className="font-display text-lg font-bold text-slate-50">{meta.name}</h3>
                      <span className="text-xs font-medium text-amber-400">{meta.title}</span>
                    </div>
                    <p className="mt-1 text-xs text-slate-400">{meta.role}</p>
                    <p className="mt-2 text-xs font-mono-tabular text-slate-500">
                      STR {meta.baseStats.str} · AGI {meta.baseStats.agi} · VIT {meta.baseStats.vit} · ENE {meta.baseStats.ene}
                    </p>
                  </button>
                );
              })}
            </div>

            <div className="rounded-2xl border border-slate-800 bg-[#121622] p-4 sm:p-5 flex flex-col sm:flex-row items-stretch sm:items-end gap-4">
              <div className="flex-1">
                <label htmlFor="hero-name" className="block text-xs font-medium text-slate-300 mb-1.5">
                  Nombre del Personaje (guardado en la cuenta {accountId})
                </label>
                <input
                  id="hero-name"
                  type="text"
                  maxLength={16}
                  value={heroName}
                  onChange={(e) => setHeroName(e.target.value)}
                  placeholder={`Ej. ${CLASS_CATALOG[selectedClass].name}`}
                  className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3.5 text-sm text-slate-100 placeholder:text-slate-500 focus:border-amber-500 focus:outline-none"
                />
              </div>
              <button
                type="submit"
                className="min-h-[44px] px-6 rounded-xl bg-amber-500 text-slate-950 font-semibold text-sm hover:bg-amber-400 active:scale-[0.98] transition-all flex items-center justify-center gap-2 whitespace-nowrap shrink-0"
              >
                <Play className="w-4 h-4 fill-current" />
                <span>Comenzar Aventura Offline</span>
              </button>
            </div>
          </form>
        )}
      </main>
    </div>
  );
};
