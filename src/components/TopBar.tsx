import { Loader2, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { GitHubIcon } from './GitHubIcon';

export type ActiveTab = 'overview' | 'repositories' | 'setup';

interface TopBarProps {
  activeTab: ActiveTab;
  onSelectTab: (tab: ActiveTab) => void;
}

export function TopBar({ activeTab, onSelectTab }: TopBarProps) {
  const { status, user, isAuthenticating, login, logout } = useAuth();

  return (
    <header className="h-14 border-b border-slate-800 bg-[#0B0F17]/95 backdrop-blur-sm sticky top-0 z-30">
      <div className="max-w-[1440px] mx-auto h-full px-6 flex items-center justify-between gap-8">
        {/* Zone 1: Single-line Brand Wordmark */}
        <button
          type="button"
          onClick={() => onSelectTab('overview')}
          className="text-base font-semibold tracking-tight text-slate-100 whitespace-nowrap shrink-0 hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 rounded"
        >
          GitForge
        </button>

        {/* Zone 2: Concise single-line navigation links */}
        <nav
          aria-label="Primary navigation"
          className="hidden sm:flex items-center gap-6 text-sm font-medium"
        >
          <button
            type="button"
            onClick={() => onSelectTab('overview')}
            className={`whitespace-nowrap shrink-0 py-1 border-b-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${
              activeTab === 'overview'
                ? 'border-blue-500 text-slate-100'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Session
          </button>
          <button
            type="button"
            onClick={() => onSelectTab('repositories')}
            className={`whitespace-nowrap shrink-0 py-1 border-b-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${
              activeTab === 'repositories'
                ? 'border-blue-500 text-slate-100'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Repositories
          </button>
          <button
            type="button"
            onClick={() => onSelectTab('setup')}
            className={`whitespace-nowrap shrink-0 py-1 border-b-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${
              activeTab === 'setup'
                ? 'border-blue-500 text-slate-100'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            OAuth Setup
          </button>
        </nav>

        {/* Zone 3: Primary Action */}
        <div className="flex items-center gap-4 shrink-0">
          {status === 'loading' ? (
            <span className="inline-flex items-center gap-2 text-xs text-slate-400 whitespace-nowrap shrink-0">
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
              Checking session
            </span>
          ) : status === 'authenticated' && user ? (
            <div className="flex items-center gap-3 shrink-0">
              <span className="hidden md:inline text-xs font-mono text-slate-300 whitespace-nowrap shrink-0">
                @{user.login}
              </span>
              <button
                type="button"
                onClick={() => void logout()}
                className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-medium text-slate-200 bg-slate-900 border border-slate-700 rounded-lg hover:bg-slate-800 hover:text-white transition-colors whitespace-nowrap shrink-0 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              >
                <LogOut className="w-3.5 h-3.5" />
                Sign out
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => void login()}
              disabled={isAuthenticating}
              className="inline-flex items-center gap-2 px-4 py-2 text-xs font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-500 disabled:opacity-60 disabled:cursor-not-allowed transition-colors whitespace-nowrap shrink-0 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-400"
            >
              {isAuthenticating ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <GitHubIcon className="w-3.5 h-3.5" />
              )}
              {isAuthenticating ? 'Connecting...' : 'Sign in with GitHub'}
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
