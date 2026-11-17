import React, { useState } from 'react';
import { KeyRound, Shield, UserPlus } from 'lucide-react';
import { loginAccount, registerAccount } from '../utils/storage';
import { OfflineStatusText, PWAInstallButton } from './PWAInstallButton';

interface AuthScreenProps {
  onAuthenticated: (accountId: string) => void;
}

export const AuthScreen: React.FC<AuthScreenProps> = ({ onAuthenticated }) => {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [accountId, setAccountId] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [crestError, setCrestError] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (mode === 'register') {
        if (password !== confirmPassword) {
          setError('Las contraseñas no coinciden.');
          setLoading(false);
          return;
        }
        const res = await registerAccount(accountId, password);
        if (!res.ok || !res.accountId) {
          setError(res.error || 'No se pudo registrar la cuenta.');
        } else {
          onAuthenticated(res.accountId);
        }
      } else {
        const res = await loginAccount(accountId, password);
        if (!res.ok || !res.accountId) {
          setError(res.error || 'No se pudo iniciar sesión.');
        } else {
          onAuthenticated(res.accountId);
        }
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-full w-full overflow-y-auto bg-[#0B0D13] text-slate-100 flex flex-col">
      <header className="flex items-center justify-between gap-4 border-b border-slate-800/80 px-4 sm:px-6 h-14 shrink-0">
        <div className="flex items-center gap-2.5">
          <Shield className="w-5 h-5 text-amber-400" />
          <span className="font-display text-base sm:text-lg font-bold text-amber-200 whitespace-nowrap">
            Mu Offline Mobile
          </span>
        </div>
        <div className="flex items-center gap-3">
          <OfflineStatusText />
          <PWAInstallButton compact />
        </div>
      </header>

      <main className="flex-1 flex items-center justify-center p-4 sm:p-6">
        <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-[#121622] p-6 sm:p-7 shadow-2xl space-y-6">
          <div className="flex flex-col items-center text-center space-y-3">
            <div className="w-20 h-20 rounded-2xl overflow-hidden border border-amber-500/40 bg-slate-950 flex items-center justify-center">
              {!crestError ? (
                <img
                  src="/src/assets/images/mu_crest_emblem_1794870013988.jpg"
                  alt="Cresta Mu Offline"
                  referrerPolicy="no-referrer"
                  onError={() => setCrestError(true)}
                  className="w-full h-full object-cover"
                />
              ) : (
                <Shield className="w-9 h-9 text-amber-400" />
              )}
            </div>
            <div>
              <h1 className="font-display text-2xl font-bold text-slate-50">
                Portal de Lorencia
              </h1>
              <p className="text-xs text-slate-400 mt-1">
                Cuentas guardadas localmente en tu teléfono para jugar 100% Offline o en Sala Wi-Fi LAN.
              </p>
            </div>
          </div>

          {/* Mode Tabs */}
          <div className="grid grid-cols-2 gap-1.5 p-1 rounded-xl bg-slate-950 border border-slate-800">
            <button
              type="button"
              onClick={() => {
                setMode('login');
                setError(null);
              }}
              className={`min-h-[44px] rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1.5 ${
                mode === 'login' ? 'bg-amber-500 text-slate-950' : 'text-slate-300 hover:text-white'
              }`}
            >
              <KeyRound className="w-4 h-4" />
              <span>Ingresar</span>
            </button>
            <button
              type="button"
              onClick={() => {
                setMode('register');
                setError(null);
              }}
              className={`min-h-[44px] rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1.5 ${
                mode === 'register' ? 'bg-amber-500 text-slate-950' : 'text-slate-300 hover:text-white'
              }`}
            >
              <UserPlus className="w-4 h-4" />
              <span>Registrarse</span>
            </button>
          </div>

          {error && (
            <div className="rounded-xl border border-red-500/40 bg-red-500/10 px-3.5 py-2.5 text-xs text-red-200">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="auth-id" className="block text-xs font-medium text-slate-300 mb-1.5">
                ID de Cuenta
              </label>
              <input
                id="auth-id"
                type="text"
                autoComplete="username"
                required
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                placeholder="Ej. zoey_dk"
                className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3.5 text-sm text-white placeholder:text-slate-500 focus:border-amber-500 focus:outline-none"
              />
            </div>

            <div>
              <label htmlFor="auth-pass" className="block text-xs font-medium text-slate-300 mb-1.5">
                Contraseña
              </label>
              <input
                id="auth-pass"
                type="password"
                autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Mínimo 4 caracteres"
                className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3.5 text-sm text-white placeholder:text-slate-500 focus:border-amber-500 focus:outline-none"
              />
            </div>

            {mode === 'register' && (
              <div>
                <label htmlFor="auth-pass2" className="block text-xs font-medium text-slate-300 mb-1.5">
                  Confirmar Contraseña
                </label>
                <input
                  id="auth-pass2"
                  type="password"
                  autoComplete="new-password"
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Repite tu contraseña"
                  className="w-full min-h-[44px] rounded-xl border border-slate-700 bg-slate-950 px-3.5 text-sm text-white placeholder:text-slate-500 focus:border-amber-500 focus:outline-none"
                />
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full min-h-[48px] rounded-xl bg-amber-500 text-slate-950 font-bold text-sm hover:bg-amber-400 active:scale-[0.98] transition-all disabled:opacity-50"
            >
              {loading
                ? 'Verificando...'
                : mode === 'register'
                ? 'Crear Cuenta Offline'
                : 'Entrar al Juego'}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
};
