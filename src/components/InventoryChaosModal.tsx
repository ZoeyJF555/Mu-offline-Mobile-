import React, { useState } from 'react';
import { Flame, Gem, Shield, Sparkles, Swords, X } from 'lucide-react';
import type { EquipSlots, HeroSave, Item } from '../types/game';

interface InventoryChaosModalProps {
  hero: HeroSave;
  chaosBonus: number;
  onEquipItem: (item: Item) => void;
  onUnequipSlot: (slot: keyof EquipSlots) => void;
  onUpgradeWithJewel: (targetItemId: string, jewelType: 'bless' | 'soul') => string;
  onChaosUpgrade: (targetItemId: string) => { ok: boolean; message: string };
  onCraftWings: () => { ok: boolean; message: string };
  onSellItem: (itemId: string) => void;
  onClose: () => void;
}

const SLOT_LABELS: Record<keyof EquipSlots, string> = {
  weapon: 'Arma',
  offhand: 'Escudo',
  armor: 'Armadura',
  helm: 'Casco',
  wings: 'Alas',
  pendant: 'Colgante',
};

export const InventoryChaosModal: React.FC<InventoryChaosModalProps> = ({
  hero,
  chaosBonus,
  onEquipItem,
  onUnequipSlot,
  onUpgradeWithJewel,
  onChaosUpgrade,
  onCraftWings,
  onSellItem,
  onClose,
}) => {
  const [tab, setTab] = useState<'bag' | 'chaos'>('bag');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const blessCount = hero.inventory
    .filter((i) => i.jewelType === 'bless')
    .reduce((acc, i) => acc + (i.quantity || 1), 0);
  const soulCount = hero.inventory
    .filter((i) => i.jewelType === 'soul')
    .reduce((acc, i) => acc + (i.quantity || 1), 0);
  const chaosCount = hero.inventory
    .filter((i) => i.jewelType === 'chaos')
    .reduce((acc, i) => acc + (i.quantity || 1), 0);

  const selectedItem =
    hero.inventory.find((i) => i.id === selectedId) ||
    Object.values(hero.equipment).find((i) => i && i.id === selectedId) ||
    null;

  const isEquipped = selectedItem
    ? Object.values(hero.equipment).some((eq) => eq?.id === selectedItem.id)
    : false;

  const gearCandidates = [
    ...Object.values(hero.equipment).filter((x): x is Item => !!x),
    ...hero.inventory.filter(
      (i) => i.slot !== 'jewel' && i.slot !== 'potion'
    ),
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/75 backdrop-blur-sm sm:p-4">
      <div className="w-full max-w-3xl rounded-t-3xl sm:rounded-2xl border border-slate-700 bg-[#111520] max-h-[90vh] flex flex-col shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-slate-800">
          <div className="flex items-center gap-4">
            <h2 className="font-display text-lg font-bold text-amber-200">Mochila y Forja</h2>
            <span className="text-xs font-mono-tabular text-amber-300">
              {hero.zen.toLocaleString()} Zen
            </span>
          </div>
          <button
            onClick={onClose}
            aria-label="Cerrar mochila"
            className="min-h-[44px] min-w-[44px] rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-300 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Segmented Tabs + Jewel Totals */}
        <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3 border-b border-slate-800/80 bg-[#0D1018]">
          <div className="flex items-center gap-1 p-1 rounded-xl bg-slate-900 border border-slate-800">
            <button
              onClick={() => setTab('bag')}
              className={`min-h-[38px] px-3.5 rounded-lg text-xs font-semibold transition-colors ${
                tab === 'bag' ? 'bg-amber-500 text-slate-950' : 'text-slate-300 hover:text-white'
              }`}
            >
              Equipo y Mochila
            </button>
            <button
              onClick={() => setTab('chaos')}
              className={`min-h-[38px] px-3.5 rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5 ${
                tab === 'chaos' ? 'bg-amber-500 text-slate-950' : 'text-slate-300 hover:text-white'
              }`}
            >
              <Flame className="w-3.5 h-3.5" />
              <span>Chaos Machine</span>
            </button>
          </div>

          <div className="flex items-center gap-3 text-xs font-mono-tabular text-slate-300">
            <span>Bless: <strong className="text-purple-300">{blessCount}</strong></span>
            <span>·</span>
            <span>Soul: <strong className="text-pink-300">{soulCount}</strong></span>
            <span>·</span>
            <span>Chaos: <strong className="text-amber-300">{chaosCount}</strong></span>
          </div>
        </div>

        {feedback && (
          <div className="mx-5 mt-3 rounded-xl border border-amber-500/40 bg-amber-500/10 px-3.5 py-2 text-xs font-medium text-amber-200">
            {feedback}
          </div>
        )}

        <div className="overflow-y-auto p-5 flex-1">
          {tab === 'bag' ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              {/* Equipment Slots */}
              <div>
                <h3 className="text-xs font-semibold text-slate-400 mb-2.5">Equipo Activo</h3>
                <div className="grid grid-cols-2 gap-2.5">
                  {(Object.keys(SLOT_LABELS) as (keyof EquipSlots)[]).map((slot) => {
                    const eq = hero.equipment[slot];
                    const selected = eq && eq.id === selectedId;
                    return (
                      <button
                        key={slot}
                        onClick={() => eq && setSelectedId(eq.id)}
                        className={`min-h-[60px] text-left rounded-xl border p-2.5 transition-colors ${
                          selected
                            ? 'border-amber-500 bg-amber-500/10'
                            : 'border-slate-800 bg-[#161B2A] hover:border-slate-700'
                        }`}
                      >
                        <span className="block text-[10px] text-slate-400">{SLOT_LABELS[slot]}</span>
                        {eq ? (
                          <span className="block text-xs font-semibold truncate mt-0.5" style={{ color: eq.colorHex }}>
                            {eq.name} {eq.plus > 0 ? `+${eq.plus}` : ''}
                          </span>
                        ) : (
                          <span className="block text-xs text-slate-600 mt-0.5">Vacío</span>
                        )}
                      </button>
                    );
                  })}
                </div>

                {/* Selected Item Inspector */}
                {selectedItem && (
                  <div className="mt-4 rounded-xl border border-slate-700 bg-[#0B0D13] p-3.5 space-y-2.5">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h4 className="text-sm font-bold" style={{ color: selectedItem.colorHex }}>
                          {selectedItem.name} {selectedItem.plus > 0 ? `+${selectedItem.plus}` : ''}
                        </h4>
                        <p className="text-[11px] text-slate-400">
                          {selectedItem.rarity.toUpperCase()} · Valor {selectedItem.valueZen} Zen
                        </p>
                      </div>
                    </div>

                    <div className="flex flex-wrap gap-3 text-xs font-mono-tabular text-slate-300">
                      {selectedItem.attack ? <span>ATK +{Math.round(selectedItem.attack * (1 + selectedItem.plus * 0.14))}</span> : null}
                      {selectedItem.magic ? <span>MAG +{Math.round(selectedItem.magic * (1 + selectedItem.plus * 0.14))}</span> : null}
                      {selectedItem.defense ? <span>DEF +{Math.round(selectedItem.defense * (1 + selectedItem.plus * 0.14))}</span> : null}
                      {selectedItem.hpBonus ? <span>HP +{selectedItem.hpBonus}</span> : null}
                    </div>

                    <div className="flex flex-wrap gap-2 pt-1">
                      {selectedItem.slot !== 'jewel' && selectedItem.slot !== 'potion' && (
                        isEquipped ? (
                          <button
                            onClick={() => onUnequipSlot(selectedItem.slot as keyof EquipSlots)}
                            className="min-h-[40px] px-3 rounded-lg bg-slate-800 text-xs font-semibold text-slate-200 hover:bg-slate-700"
                          >
                            Quitar
                          </button>
                        ) : (
                          <button
                            onClick={() => {
                              onEquipItem(selectedItem);
                              setFeedback(`Equipaste ${selectedItem.name}.`);
                            }}
                            className="min-h-[40px] px-3 rounded-lg bg-amber-500 text-slate-950 text-xs font-semibold hover:bg-amber-400"
                          >
                            Equipar
                          </button>
                        )
                      )}

                      {selectedItem.slot !== 'jewel' && selectedItem.slot !== 'potion' && selectedItem.plus < 6 && (
                        <button
                          disabled={blessCount < 1}
                          onClick={() => setFeedback(onUpgradeWithJewel(selectedItem.id, 'bless'))}
                          className="min-h-[40px] px-3 rounded-lg border border-purple-500/40 bg-purple-500/10 text-xs font-semibold text-purple-200 disabled:opacity-35"
                        >
                          +1 con Bless
                        </button>
                      )}

                      {selectedItem.slot !== 'jewel' && selectedItem.slot !== 'potion' && selectedItem.plus >= 6 && selectedItem.plus < 9 && (
                        <button
                          disabled={soulCount < 1}
                          onClick={() => setFeedback(onUpgradeWithJewel(selectedItem.id, 'soul'))}
                          className="min-h-[40px] px-3 rounded-lg border border-pink-500/40 bg-pink-500/10 text-xs font-semibold text-pink-200 disabled:opacity-35"
                        >
                          +1 con Soul
                        </button>
                      )}

                      {!isEquipped && (
                        <button
                          onClick={() => {
                            onSellItem(selectedItem.id);
                            setSelectedId(null);
                            setFeedback(`Vendiste ${selectedItem.name} por ${selectedItem.valueZen} Zen.`);
                          }}
                          className="min-h-[40px] px-3 rounded-lg border border-slate-700 bg-slate-900 text-xs font-medium text-slate-300 hover:text-amber-300"
                        >
                          Vender
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* Backpack */}
              <div>
                <h3 className="text-xs font-semibold text-slate-400 mb-2.5">
                  Mochila ({hero.inventory.length}/24)
                </h3>
                {hero.inventory.length === 0 ? (
                  <div className="rounded-xl border border-slate-800 bg-[#0B0D13] p-6 text-center text-xs text-slate-500">
                    Tu mochila está vacía. Derrota monstruos para recoger objetos y joyas.
                  </div>
                ) : (
                  <div className="grid grid-cols-1 gap-2">
                    {hero.inventory.map((item) => {
                      const active = item.id === selectedId;
                      return (
                        <button
                          key={item.id}
                          onClick={() => setSelectedId(item.id)}
                          className={`min-h-[48px] flex items-center justify-between gap-3 rounded-xl border px-3 py-2 text-left transition-colors ${
                            active
                              ? 'border-amber-500 bg-amber-500/10'
                              : 'border-slate-800 bg-[#161B2A] hover:border-slate-700'
                          }`}
                        >
                          <div className="flex items-center gap-2.5 min-w-0">
                            {item.slot === 'jewel' ? (
                              <Gem className="w-4 h-4 shrink-0" style={{ color: item.colorHex }} />
                            ) : item.slot === 'weapon' ? (
                              <Swords className="w-4 h-4 shrink-0" style={{ color: item.colorHex }} />
                            ) : (
                              <Shield className="w-4 h-4 shrink-0" style={{ color: item.colorHex }} />
                            )}
                            <span className="text-xs font-semibold truncate" style={{ color: item.colorHex }}>
                              {item.name} {item.plus > 0 ? `+${item.plus}` : ''}
                            </span>
                          </div>
                          <span className="text-[11px] font-mono-tabular text-slate-400 shrink-0">
                            {item.quantity && item.quantity > 1 ? `x${item.quantity}` : item.slot}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="space-y-5">
              {/* Craft Wings */}
              <div className="rounded-xl border border-amber-500/30 bg-[#171B2A] p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h3 className="font-display text-base font-bold text-amber-300 flex items-center gap-2">
                    <Sparkles className="w-4 h-4" />
                    <span>Forjar Alas de Nivel 2 (Wings)</span>
                  </h3>
                  <p className="text-xs text-slate-300 mt-1">
                    Requiere: 1 Jewel of Chaos + 1 Jewel of Bless + 5,000 Zen · Éxito base {Math.min(95, 70 + chaosBonus)}%
                  </p>
                </div>
                <button
                  disabled={chaosCount < 1 || blessCount < 1 || hero.zen < 5000}
                  onClick={() => setFeedback(onCraftWings().message)}
                  className="min-h-[44px] px-4 rounded-xl bg-amber-500 text-slate-950 font-semibold text-xs hover:bg-amber-400 disabled:opacity-35 whitespace-nowrap shrink-0"
                >
                  Combinar Alas
                </button>
              </div>

              {/* +10 to +13 Combination */}
              <div>
                <h3 className="text-xs font-semibold text-slate-400 mb-2.5">
                  Combinar Equipo a +10 / +11 / +12 / +13 (Requiere 1 Jewel of Chaos + 1 Soul + 3,000 Zen)
                </h3>
                <div className="space-y-2">
                  {gearCandidates.filter((i) => i.plus >= 9 && i.plus < 13).length === 0 ? (
                    <div className="rounded-xl border border-slate-800 bg-[#0B0D13] p-5 text-center text-xs text-slate-400">
                      Sube primero cualquier arma, armadura o alas a +9 usando Jewel of Bless y Jewel of Soul para desbloquear la combinación hasta +13.
                    </div>
                  ) : (
                    gearCandidates
                      .filter((i) => i.plus >= 9 && i.plus < 13)
                      .map((item) => {
                        const rate = Math.min(95, Math.max(35, 75 - (item.plus - 9) * 10 + chaosBonus));
                        return (
                          <div
                            key={item.id}
                            className="flex items-center justify-between gap-3 rounded-xl border border-slate-800 bg-[#161B2A] p-3"
                          >
                            <div>
                              <span className="text-xs font-bold" style={{ color: item.colorHex }}>
                                {item.name} +{item.plus} → +{item.plus + 1}
                              </span>
                              <p className="text-[11px] font-mono-tabular text-slate-400">
                                Probabilidad de éxito: {rate}%
                              </p>
                            </div>
                            <button
                              disabled={chaosCount < 1 || soulCount < 1 || hero.zen < 3000}
                              onClick={() => setFeedback(onChaosUpgrade(item.id).message)}
                              className="min-h-[44px] px-4 rounded-xl bg-amber-500 text-slate-950 text-xs font-semibold hover:bg-amber-400 disabled:opacity-35"
                            >
                              Forjar +{item.plus + 1}
                            </button>
                          </div>
                        );
                      })
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
