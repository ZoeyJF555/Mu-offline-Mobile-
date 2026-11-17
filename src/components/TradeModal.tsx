import React, { useState } from 'react';
import { ArrowLeftRight, Check, X } from 'lucide-react';
import type { HeroSave, Item, TradeSession } from '../types/game';

interface TradeModalProps {
  hero: HeroSave;
  trade: TradeSession;
  onUpdateOffer: (items: Item[], zen: number) => void;
  onConfirm: () => void;
  onCancel: () => void;
}

export const TradeModal: React.FC<TradeModalProps> = ({
  hero,
  trade,
  onUpdateOffer,
  onConfirm,
  onCancel,
}) => {
  const [zenInput, setZenInput] = useState(trade.myZen);

  const offeredIds = new Set(trade.myItems.map((i) => i.id));
  const tradeableItems = hero.inventory.filter((i) => i.slot !== 'potion');

  const toggleItemInOffer = (item: Item) => {
    const nextItems = offeredIds.has(item.id)
      ? trade.myItems.filter((i) => i.id !== item.id)
      : [...trade.myItems, item];
    onUpdateOffer(nextItems, zenInput);
  };

  const handleZenChange = (raw: string) => {
    const val = Math.max(0, Math.min(hero.zen, Number(raw) || 0));
    setZenInput(val);
    onUpdateOffer(trade.myItems, val);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/75 backdrop-blur-sm sm:p-4">
      <div className="w-full max-w-2xl rounded-t-3xl sm:rounded-2xl border border-slate-700 bg-[#111520] max-h-[90vh] flex flex-col shadow-2xl">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <ArrowLeftRight className="w-5 h-5 text-amber-400" />
            <div>
              <h2 className="font-display text-lg font-bold text-amber-200">
                Intercambio con {trade.partnerName}
              </h2>
              <p className="text-xs text-slate-400">
                Selecciona objetos y Zen. Ambos deben confirmar para completar el intercambio.
              </p>
            </div>
          </div>
          <button
            onClick={onCancel}
            aria-label="Cancelar intercambio"
            className="min-h-[44px] min-w-[44px] rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-300 hover:text-white"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="overflow-y-auto p-5 space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* My Offer */}
            <div className="rounded-xl border border-slate-800 bg-[#161B2A] p-4 space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-slate-100">Tu Oferta</h3>
                <span className={`text-xs font-semibold ${trade.myConfirmed ? 'text-emerald-300' : 'text-slate-400'}`}>
                  {trade.myConfirmed ? 'Confirmado' : 'Editando'}
                </span>
              </div>

              <div>
                <label className="block text-[11px] text-slate-400 mb-1">
                  Zen a entregar (Tienes {hero.zen.toLocaleString()})
                </label>
                <input
                  type="number"
                  min={0}
                  max={hero.zen}
                  value={zenInput}
                  onChange={(e) => handleZenChange(e.target.value)}
                  className="w-full min-h-[40px] rounded-lg border border-slate-700 bg-slate-950 px-3 text-sm font-mono-tabular text-amber-300 focus:border-amber-500 focus:outline-none"
                />
              </div>

              <div className="space-y-1.5 max-h-44 overflow-y-auto">
                {tradeableItems.length === 0 ? (
                  <p className="text-xs text-slate-500">Sin joyas o equipo disponible para intercambiar.</p>
                ) : (
                  tradeableItems.map((item) => {
                    const selected = offeredIds.has(item.id);
                    return (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => toggleItemInOffer(item)}
                        className={`w-full min-h-[40px] flex items-center justify-between rounded-lg border px-3 text-left text-xs transition-colors ${
                          selected
                            ? 'border-amber-500 bg-amber-500/15'
                            : 'border-slate-800 bg-[#0D1018] hover:border-slate-700'
                        }`}
                      >
                        <span className="font-semibold truncate" style={{ color: item.colorHex }}>
                          {item.name} {item.plus > 0 ? `+${item.plus}` : ''}
                        </span>
                        <span className="text-[11px] font-mono-tabular text-slate-400 shrink-0">
                          {selected ? 'En oferta' : item.quantity && item.quantity > 1 ? `x${item.quantity}` : 'Añadir'}
                        </span>
                      </button>
                    );
                  })
                )}
              </div>
            </div>

            {/* Partner Offer */}
            <div className="rounded-xl border border-slate-800 bg-[#0D1018] p-4 space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold text-sky-200">Oferta de {trade.partnerName}</h3>
                <span className={`text-xs font-semibold ${trade.partnerConfirmed ? 'text-emerald-300' : 'text-slate-400'}`}>
                  {trade.partnerConfirmed ? 'Confirmado' : 'Esperando'}
                </span>
              </div>

              <div className="rounded-lg bg-slate-900/90 px-3 py-2 text-xs font-mono-tabular text-amber-300">
                Zen ofrecido: {trade.partnerZen.toLocaleString()} Zen
              </div>

              <div className="space-y-1.5 max-h-44 overflow-y-auto">
                {trade.partnerItems.length === 0 ? (
                  <p className="text-xs text-slate-500">Ningún objeto ofrecido aún.</p>
                ) : (
                  trade.partnerItems.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between rounded-lg border border-slate-800 bg-[#161B2A] px-3 py-2 text-xs"
                    >
                      <span className="font-semibold" style={{ color: item.colorHex }}>
                        {item.name} {item.plus > 0 ? `+${item.plus}` : ''}
                      </span>
                      <span className="font-mono-tabular text-slate-400">
                        {item.quantity && item.quantity > 1 ? `x${item.quantity}` : item.slot}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-2 border-t border-slate-800">
            <button
              onClick={onCancel}
              className="min-h-[44px] px-4 rounded-xl border border-slate-700 bg-slate-900 text-xs font-semibold text-slate-300 hover:text-white"
            >
              Cancelar
            </button>
            <button
              disabled={trade.myConfirmed}
              onClick={onConfirm}
              className="min-h-[44px] px-5 rounded-xl bg-emerald-500 text-slate-950 font-semibold text-xs hover:bg-emerald-400 disabled:opacity-50 flex items-center gap-1.5"
            >
              <Check className="w-4 h-4" />
              <span>{trade.myConfirmed ? 'Esperando al otro jugador...' : 'Confirmar Intercambio'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
