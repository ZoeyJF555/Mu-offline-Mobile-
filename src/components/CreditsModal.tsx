import React from 'react';
import { Award, X } from 'lucide-react';

interface CreditsModalProps {
  onClose: () => void;
}

export const CreditsModal: React.FC<CreditsModalProps> = ({ onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4">
      <div className="w-full max-w-lg rounded-2xl border-2 border-amber-500/50 bg-[#0E111A] p-5 text-slate-100 shadow-2xl space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <Award className="w-5 h-5 text-amber-400" />
            <h2 className="font-display text-lg font-bold text-amber-200">
              Créditos y Recursos MU (Ctrl+F8)
            </h2>
          </div>
          <button
            onClick={onClose}
            aria-label="Cerrar"
            className="min-h-[38px] min-w-[38px] rounded-lg bg-slate-900 text-slate-300 hover:text-white flex items-center justify-center"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="space-y-3 text-xs text-slate-300 leading-relaxed">
          <p>
            Los modelos 3D originales <span className="font-mono-tabular text-amber-300">.bmd</span>,
            texturas y especificaciones de personajes, NPCs, monstruos y mapa de Lorencia provienen de los
            proyectos de código abierto de la comunidad MU:
          </p>
          <ul className="space-y-1.5 rounded-xl border border-slate-800 bg-slate-950/70 p-3">
            <li>
              <strong className="text-amber-300">Ignies / OpenMu-Client-Babylon</strong> — Modelos BMD,
              texturas y referencias de protocolo (Licencia MIT).
            </li>
            <li>
              <strong className="text-sky-300">afrokick / muonlinejs</strong> — Base original de cliente
              web MU en JS/TS.
            </li>
            <li>
              <strong className="text-emerald-300">xulek / muonline-bmd-viewer</strong> — Documentación del
              formato binario BMD.
            </li>
            <li>
              <strong className="text-purple-300">MUnique / OpenMU</strong> — Tablas de monstruos, NPCs y
              habilidades Season 6.
            </li>
          </ul>
          <p className="text-[11px] text-slate-500">
            Atajo clásico: presiona <kbd className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-200">Ctrl+F8</kbd> en
            cualquier momento para ver estos créditos.
          </p>
        </div>
      </div>
    </div>
  );
};
