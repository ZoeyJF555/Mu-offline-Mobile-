import { AlertCircle, Loader2, Lock, RefreshCw, ShieldOff, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { GitHubIcon } from './GitHubIcon';
import { OAuthSetupPanel } from './OAuthSetupPanel';

import type { ActiveTab } from './TopBar';

interface LoginViewProps {
  activeTab: ActiveTab;
  onSelectTab: (tab: ActiveTab) => void;
}

export function LoginView({ activeTab, onSelectTab }: LoginViewProps) {
  const {
    status,
    error,
    isAuthenticating,
    configured,
    login,
    refreshSession,
    clearError,
  } = useAuth();

  if (activeTab === 'setup') {
    return (
      <main className="max-w-4xl mx-auto px-6 py-10 space-y-6 w-full">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-slate-100">
              GitHub OAuth Setup
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Register these callback URLs in your GitHub OAuth App settings.
            </p>
          </div>
          <button
            type="button"
            onClick={() => onSelectTab('overview')}
            className="px-3.5 py-2 text-xs font-medium text-slate-200 bg-slate-900 border border-slate-800 rounded-lg hover:bg-slate-800 cursor-pointer whitespace-nowrap shrink-0"
          >
            Back to sign in
          </button>
        </div>
        <OAuthSetupPanel />
      </main>
    );
  }

  return (
    <main className="max-w-[1440px] mx-auto px-6 py-10 lg:py-16 w-full">
      {activeTab === 'repositories' && (
        <div className="mb-6 bg-[#111827] border border-slate-800 rounded-xl px-5 py-3.5 flex flex-wrap items-center justify-between gap-4">
          <p className="text-xs text-slate-300">
            Sign in with GitHub to load your public and private repositories.
          </p>
          <button
            type="button"
            onClick={() => void login()}
            disabled={isAuthenticating}
            className="text-xs font-medium text-blue-400 hover:text-blue-300 cursor-pointer whitespace-nowrap"
          >
            Connect account now
          </button>
        </div>
      )}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
        {/* Left Column: Primary Auth Entry & Session Readout */}
        <section className="lg:col-span-7 bg-[#111827] border border-slate-800 rounded-xl p-8 flex flex-col justify-between gap-8">
          <div className="space-y-6">
            <div className="flex items-center gap-2 text-xs text-slate-400">
              <ShieldOff className="w-3.5 h-3.5 text-slate-400" />
              <span>Session status: Signed out</span>
              <span aria-hidden="true">·</span>
              <span>GitHub OAuth 2.0</span>
            </div>

            <div className="space-y-3 max-w-xl">
              <h1
                className="text-3xl sm:text-4xl font-semibold tracking-tight text-slate-50 leading-tight"
                style={{ textWrap: 'balance' }}
              >
                Authenticate with GitHub to access your repositories
              </h1>
              <p className="text-sm sm:text-base text-slate-400 leading-relaxed">
                Sign in with your GitHub account through a secure OAuth popup.
                Your access token stays encrypted inside an HTTP-only server
                session cookie and is never stored in browser storage.
              </p>
            </div>

            {error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 bg-red-950/40 border border-red-900/80 rounded-lg px-4 py-3 text-xs text-red-200"
              >
                <div className="flex items-start gap-2.5">
                  <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                  <span className="leading-relaxed">{error}</span>
                </div>
                <button
                  type="button"
                  onClick={clearError}
                  aria-label="Dismiss error"
                  className="text-red-300 hover:text-white p-0.5 rounded cursor-pointer shrink-0"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>
            )}

            <div className="flex flex-wrap items-center gap-4 pt-2">
              <button
                type="button"
                onClick={() => void login()}
                disabled={isAuthenticating || status === 'loading'}
                className="inline-flex items-center justify-center gap-2.5 px-5 py-3 min-h-[44px] text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-500 disabled:opacity-60 disabled:cursor-not-allowed transition-colors whitespace-nowrap shrink-0 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-400"
              >
                {isAuthenticating ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Waiting for GitHub authorization...
                  </>
                ) : (
                  <>
                    <GitHubIcon className="w-4 h-4" />
                    Sign in with GitHub
                  </>
                )}
              </button>

              <button
                type="button"
                onClick={() => void refreshSession()}
                disabled={status === 'loading'}
                className="inline-flex items-center gap-2 px-4 py-3 min-h-[44px] text-sm font-medium text-slate-300 bg-[#0B0F17] border border-slate-800 rounded-lg hover:text-white hover:border-slate-700 transition-colors whitespace-nowrap shrink-0 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              >
                <RefreshCw
                  className={`w-4 h-4 ${
                    status === 'loading' ? 'animate-spin' : ''
                  }`}
                />
                Verify session
              </button>
            </div>
          </div>

          <div className="border-t border-slate-800 pt-5 grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
            <div>
              <div className="text-slate-400">OAuth Flow</div>
              <div className="text-slate-200 font-medium mt-1">
                Direct provider popup
              </div>
            </div>
            <div>
              <div className="text-slate-400">Token Custody</div>
              <div className="text-slate-200 font-medium mt-1 flex items-center gap-1.5">
                <Lock className="w-3.5 h-3.5 text-slate-400" />
                Server-side AES-256-GCM
              </div>
            </div>
            <div>
              <div className="text-slate-400">Provider Status</div>
              <div className="text-slate-200 font-medium mt-1">
                {configured ? 'Ready for sign-in' : 'Awaiting client credentials'}
              </div>
            </div>
          </div>
        </section>

        {/* Right Column: OAuth Config & Callback Inspector */}
        <div className="lg:col-span-5 flex">
          <OAuthSetupPanel />
        </div>
      </div>
    </main>
  );
}
