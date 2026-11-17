import React from 'react';
import { Coins, ShoppingBag, Sparkles, X } from 'lucide-react';
import { DROP_ITEM_POOL } from '../data/muCatalog';
import type { HeroSave, Item } from '../types/game';

interface NpcShopModalProps {
  hero: HeroSave;
  npcName: string;
  onBuyItem: (itemTemplate: Omit<Item, 'id'>, costZen: number) => string;
  onOpenChaosMachine: () => void;
  onClose: () => void;
}

const SHOP_TEMPLATES = DROP_ITEM_POOL.filter((it) =>
  ['hp-potion', 'mp-potion', 'blade-of-archangel', 'dragon-armor', 'viper-staff', 'grand-soul-armor', 'viper-bow', 'guardian-armor', 'plate-shield'].includes(
    it.templateId
  )
);

export const NpcShopModal: React.FC<NpcShopModalProps> = ({
  hero,
  npcName,
  onBuyItem,
  onOpenChaosMachine,
  onClose,
}) => {
  const [feedback, setFeedback] = React.useState<string | null>(null);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-3 sm:p-4">
      <div className="w-full max-w-2xl max-h-[90vh] flex flex-col rounded-2xl border-2 border-amber-500/60 bg-[#0E111A] text-slate-100 shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between border-b border-amber-500/30 bg-[#15121d] px-4 py-3">
          <div className="flex items-center gap-2.5">
            <ShoppingBag className="w-5 h-5 text-amber-400" />
            <div>
              <h2 className="font-display text-base font-bold text-amber-200">{npcName}</h2>
              <p className="text-[11px] text-slate-400">Comercio de Lorencia · 100% Offline</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span className="flex items-center gap-1.5 text-xs font-mono-tabular font-bold text-amber-300 bg-slate-950 px-2.5 py-1 rounded-lg border border-amber-500/30">
              <Coins className="w-3.5 h-3.5" />
              {hero.zen.toLocaleString()} Zen
            </span>
            <button
              onClick={onClose}
              aria-label="Cerrar tienda"
              className="min-h-[38px] min-w-[38px] rounded-lg bg-slate-900 text-slate-300 hover:text-white flex items-center justify-center"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {feedback && (
            <div className="rounded-xl border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-xs text-amber-200">
              {feedback}
            </div>
          )}

          {npcName.toLowerCase().includes('chaos') && (
            <div className="rounded-xl border border-purple-500/50 bg-purple-950/30 p-3 flex items-center justify-between gap-3">
              <div className="text-xs">
                <p className="font-bold text-purple-200">Chaos Goblin — Combinación de Ítems y Alas</p>
                <p className="text-slate-400">Usa Joyas de Chaos, Bless y Soul para subir ítems a +13 o forjar Alas.</p>
              </div>
              <button
                onClick={() => {
                  onClose();
                  onOpenChaosMachine();
                }}
                className="min-h-[40px] px-3.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-semibold flex items-center gap-1.5 shrink-0"
              >
                <Sparkles className="w-4 h-4" />
                <span>Abrir Chaos Machine</span>
              </button>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {SHOP_TEMPLATES.map((tpl) => {
              const price = tpl.slot === 'potion' ? 250 : Math.round(tpl.valueZen * 1.4);
              const canAfford = hero.zen >= price;
              return (
                <div
                  key={tpl.templateId}
                  className="flex items-center justify-between gap-3 rounded-xl border border-slate-800 bg-slate-950/75 p-3"
                >
                  <div className="min-w-0">
                    <p className="text-xs font-bold truncate" style={{ color: tpl.colorHex }}>
                      {tpl.name} {tpl.plus > 0 ? `+${tpl.plus}` : ''}
                    </p>
                    <p className="text-[11px] text-slate-400">
                      {tpl.attack ? `ATK +${tpl.attack} ` : ''}
                      {tpl.magic ? `MAG +${tpl.magic} ` : ''}
                      {tpl.defense ? `DEF +${tpl.defense} ` : ''}
                      {tpl.potionType ? 'Restaura HP/MP al instante' : ''}
                    </p>
                  </div>
                  <button
                    disabled={!canAfford}
                    onClick={() => setFeedback(onBuyItem(tpl, price))}
                    className="min-h-[38px] px-3 rounded-lg bg-amber-500 text-slate-950 text-xs font-bold hover:bg-amber-400 disabled:opacity-35 shrink-0"
                  >
                    {price.toLocaleString()} Zen
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};
