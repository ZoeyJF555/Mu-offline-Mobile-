import React, { useState } from 'react';
import { Download, Smartphone, WifiOff } from 'lucide-react';
import { useOnlineStatus, usePWAInstall } from '../utils/usePWAInstall';

export const PWAInstallButton: React.FC<{ compact?: boolean }> = ({ compact }) => {
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();
  const [showIOSGuide, setShowIOSGuide] = useState(false);

  if (isInstalled) return null;

  if (isInstallable) {
    return (
      <button
        onClick={install}
        className={`flex items-center justify-center gap-2 rounded-xl bg-amber-500 text-slate-950 font-semibold hover:bg-amber-400 active:scale-[0.98] transition-all whitespace-nowrap shrink-0 ${
          compact ? 'min-h-[44px] px-3 text-xs' : 'min-h-[44px] px-4 py-2 text-sm'
        }`}
      >
        <Download className="w-4 h-4 shrink-0" />
        <span>{compact ? 'Instalar' : 'Instalar App Offline'}</span>
      </button>
    );
  }

  if (isIOS) {
    return (
      <>
        <button
          onClick={() => setShowIOSGuide(true)}
          className="flex items-center justify-center gap-2 min-h-[44px] rounded-xl border border-amber-500/40 bg-amber-500/10 px-3.5 text-xs font-semibold text-amber-300 hover:bg-amber-500/20 transition-colors whitespace-nowrap shrink-0"
        >
          <Smartphone className="w-4 h-4 shrink-0" />
          <span>Instalar en iOS</span>
        </button>

        {showIOSGuide && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
            <div className="w-full max-w-sm rounded-2xl border border-slate-700 bg-[#121622] p-6 shadow-2xl">
              <h3 className="font-display text-lg font-bold text-amber-300">
                Instalar Mu Offline en iPhone / iPad
              </h3>
              <p className="mt-3 text-sm leading-relaxed text-slate-300">
                1. Toca el botón <strong>Compartir</strong> en la barra de Safari.<br />
                2. Desliza y selecciona <strong>Agregar a pantalla de inicio</strong>.<br />
                3. Ábrelo desde tu pantalla para jugar 100% sin internet.
              </p>
              <button
                onClick={() => setShowIOSGuide(false)}
                className="mt-5 w-full min-h-[44px] rounded-xl bg-slate-800 text-sm font-semibold text-slate-100 hover:bg-slate-700 transition-colors"
              >
                Entendido
              </button>
            </div>
          </div>
        )}
      </>
    );
  }

  return null;
};

export const OfflineStatusText: React.FC = () => {
  const isOnline = useOnlineStatus();
  if (isOnline) return null;

  return (
    <div className="flex items-center gap-1.5 text-xs font-medium text-amber-300 whitespace-nowrap shrink-0">
      <WifiOff className="w-3.5 h-3.5 shrink-0" />
      <span>Modo Offline Activo</span>
    </div>
  );
};
