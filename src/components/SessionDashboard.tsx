import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ChevronRight,
  ExternalLink,
  FileCode2,
  Folder,
  GitBranch,
  Loader2,
  Lock,
  LogOut,
  RefreshCw,
  Search,
  ShieldCheck,
  Star,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import type { GitHubRepo, RepoEntry, RepoFile } from '../types/auth';
import type { ActiveTab } from './TopBar';
import { OAuthSetupPanel } from './OAuthSetupPanel';

interface SessionDashboardProps {
  activeTab: ActiveTab;
  onSelectTab: (tab: ActiveTab) => void;
}

function formatDate(isoOrMs: string | number): string {
  const date = new Date(isoOrMs);
  if (Number.isNaN(date.getTime())) return 'Unknown';
  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function UserAvatar({ url, name }: { url: string; name: string }) {
  const [failed, setFailed] = useState(false);
  const initials = name
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('');

  if (failed || !url) {
    return (
      <div className="w-12 h-12 rounded-lg bg-slate-800 border border-slate-700 flex items-center justify-center text-sm font-semibold text-slate-200 shrink-0">
        {initials || 'GH'}
      </div>
    );
  }

  return (
    <img
      src={url}
      alt={`${name} avatar`}
      referrerPolicy="no-referrer"
      onError={() => setFailed(true)}
      className="w-12 h-12 rounded-lg border border-slate-700 object-cover shrink-0 bg-slate-800"
    />
  );
}

export function SessionDashboard({
  activeTab,
  onSelectTab,
}: SessionDashboardProps) {
  const { user, session, logout, refreshSession } = useAuth();
  const [repos, setRepos] = useState<GitHubRepo[]>([]);
  const [reposLoading, setReposLoading] = useState(true);
  const [reposError, setReposError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [visibilityFilter, setVisibilityFilter] = useState<
    'all' | 'public' | 'private'
  >('all');

  const [selectedRepo, setSelectedRepo] = useState<GitHubRepo | null>(null);
  const [currentPath, setCurrentPath] = useState('');
  const [entries, setEntries] = useState<RepoEntry[]>([]);
  const [openFile, setOpenFile] = useState<RepoFile | null>(null);
  const [contentsLoading, setContentsLoading] = useState(false);
  const [contentsError, setContentsError] = useState<string | null>(null);
  const [refreshingSession, setRefreshingSession] = useState(false);

  const loadRepos = useCallback(async () => {
    setReposLoading(true);
    setReposError(null);
    try {
      const res = await fetch('/api/github/repos', {
        credentials: 'include',
      });
      const data = (await res.json()) as {
        repos?: GitHubRepo[];
        error?: string;
      };
      if (!res.ok || !data.repos) {
        throw new Error(data.error || 'Failed to load repositories.');
      }
      setRepos(data.repos);
      if (data.repos.length > 0) {
        setSelectedRepo((prev) => prev ?? data.repos![0]);
      }
    } catch (err) {
      setReposError(
        err instanceof Error ? err.message : 'Unable to load repositories.'
      );
    } finally {
      setReposLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadRepos();
  }, [loadRepos]);

  const loadPath = useCallback(
    async (repo: GitHubRepo, path: string) => {
      setContentsLoading(true);
      setContentsError(null);
      try {
        const params = new URLSearchParams();
        if (path) params.set('path', path);
        params.set('ref', repo.default_branch);

        const res = await fetch(
          `/api/github/repos/${encodeURIComponent(repo.owner.login)}/${encodeURIComponent(repo.name)}/contents?${params.toString()}`,
          { credentials: 'include' }
        );
        const data = (await res.json()) as {
          kind?: 'dir' | 'file';
          entries?: RepoEntry[];
          file?: RepoFile;
          error?: string;
        };

        if (!res.ok) {
          throw new Error(data.error || 'Failed to load repository path.');
        }

        if (data.kind === 'dir' && data.entries) {
          setEntries(data.entries);
          setOpenFile(null);
          setCurrentPath(path);
        } else if (data.kind === 'file' && data.file) {
          setOpenFile(data.file);
          setCurrentPath(path);
        }
      } catch (err) {
        setContentsError(
          err instanceof Error ? err.message : 'Error reading repository.'
        );
      } finally {
        setContentsLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    if (selectedRepo) {
      void loadPath(selectedRepo, '');
    }
  }, [selectedRepo, loadPath]);

  const filteredRepos = useMemo(() => {
    const query = search.trim().toLowerCase();
    return repos.filter((repo) => {
      if (visibilityFilter === 'public' && repo.private) return false;
      if (visibilityFilter === 'private' && !repo.private) return false;
      if (!query) return true;
      return (
        repo.full_name.toLowerCase().includes(query) ||
        (repo.description ?? '').toLowerCase().includes(query) ||
        (repo.language ?? '').toLowerCase().includes(query)
      );
    });
  }, [repos, search, visibilityFilter]);

  const handleManualRefresh = async () => {
    setRefreshingSession(true);
    try {
      await refreshSession();
    } finally {
      setRefreshingSession(false);
    }
  };

  if (!user) return null;

  if (activeTab === 'setup') {
    return (
      <main className="max-w-4xl mx-auto px-6 py-10 space-y-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-slate-100">
              OAuth Configuration
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Active GitHub OAuth credentials and session security settings.
            </p>
          </div>
          <button
            type="button"
            onClick={() => onSelectTab('overview')}
            className="px-3.5 py-2 text-xs font-medium text-slate-200 bg-slate-900 border border-slate-800 rounded-lg hover:bg-slate-800 cursor-pointer whitespace-nowrap shrink-0"
          >
            Back to session
          </button>
        </div>
        <OAuthSetupPanel />
      </main>
    );
  }

  const breadcrumbs = currentPath ? currentPath.split('/') : [];

  return (
    <main className="max-w-[1440px] mx-auto px-6 py-8 space-y-8">
      {/* Authenticated User Header & 4-Column Tabular KPI Strip */}
      {activeTab === 'overview' && (
        <section className="space-y-6">
          <div className="bg-[#111827] border border-slate-800 rounded-xl p-6 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
            <div className="flex items-start sm:items-center gap-4">
              <UserAvatar url={user.avatar_url} name={user.name || user.login} />
              <div className="space-y-1 min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="text-xl font-semibold text-slate-50 truncate">
                    {user.name}
                  </h1>
                  <span className="text-xs font-mono text-slate-400">
                    @{user.login}
                  </span>
                  <span className="text-xs text-emerald-400 inline-flex items-center gap-1 font-medium">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    Authenticated
                  </span>
                </div>
                <div className="text-xs text-slate-400 flex flex-wrap items-center gap-2">
                  {user.email && <span>{user.email}</span>}
                  {user.email && (user.company || user.location) && (
                    <span aria-hidden="true">·</span>
                  )}
                  {user.company && <span>{user.company}</span>}
                  {user.company && user.location && (
                    <span aria-hidden="true">·</span>
                  )}
                  {user.location && <span>{user.location}</span>}
                  {session?.createdAt && (
                    <>
                      <span aria-hidden="true">·</span>
                      <span>Session started {formatDate(session.createdAt)}</span>
                    </>
                  )}
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3 shrink-0">
              <a
                href={user.html_url}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-medium text-slate-300 bg-[#0B0F17] border border-slate-800 rounded-lg hover:text-white hover:border-slate-700 transition-colors whitespace-nowrap shrink-0"
              >
                GitHub profile
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
              <button
                type="button"
                onClick={() => void handleManualRefresh()}
                disabled={refreshingSession}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-medium text-slate-300 bg-[#0B0F17] border border-slate-800 rounded-lg hover:text-white hover:border-slate-700 transition-colors whitespace-nowrap shrink-0 cursor-pointer"
              >
                <RefreshCw
                  className={`w-3.5 h-3.5 ${
                    refreshingSession ? 'animate-spin' : ''
                  }`}
                />
                Refresh session
              </button>
              <button
                type="button"
                onClick={() => void logout()}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-500 transition-colors whitespace-nowrap shrink-0 cursor-pointer"
              >
                <LogOut className="w-3.5 h-3.5" />
                Sign out
              </button>
            </div>
          </div>

          <div className="grid grid-cols-2 lg:grid-cols-4 border border-slate-800 rounded-xl bg-[#111827] divide-y lg:divide-y-0 lg:divide-x divide-slate-800">
            <div className="p-5">
              <div className="text-xs text-slate-400">Public Repositories</div>
              <div className="text-2xl font-semibold font-mono tabular-nums text-slate-50 mt-1.5">
                {user.public_repos}
              </div>
            </div>
            <div className="p-5">
              <div className="text-xs text-slate-400">Private Repositories</div>
              <div className="text-2xl font-semibold font-mono tabular-nums text-slate-50 mt-1.5">
                {user.total_private_repos}
              </div>
            </div>
            <div className="p-5">
              <div className="text-xs text-slate-400">Followers</div>
              <div className="text-2xl font-semibold font-mono tabular-nums text-slate-50 mt-1.5">
                {user.followers}
              </div>
            </div>
            <div className="p-5">
              <div className="text-xs text-slate-400">Granted OAuth Scopes</div>
              <div className="text-sm font-mono text-slate-200 mt-2.5 truncate">
                {session?.scopes.length
                  ? session.scopes.join(' · ')
                  : 'read:user'}
              </div>
            </div>
          </div>
        </section>
      )}

      {/* Repository List + File Inspector */}
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Repository List Column */}
        <div className="lg:col-span-5 bg-[#111827] border border-slate-800 rounded-xl overflow-hidden">
          <div className="p-4 border-b border-slate-800 space-y-3">
            <div className="flex items-center justify-between gap-2">
              <h2 className="text-sm font-semibold text-slate-100">
                Your Repositories
              </h2>
              <span className="text-xs font-mono tabular-nums text-slate-400">
                {filteredRepos.length} shown
              </span>
            </div>

            <div className="flex flex-col sm:flex-row gap-2">
              <div className="relative flex-1">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Filter repositories..."
                  className="w-full bg-[#0B0F17] border border-slate-800 rounded-lg pl-8 pr-3 py-1.5 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="flex items-center gap-1 p-1 bg-[#0B0F17] border border-slate-800 rounded-lg shrink-0">
                {(['all', 'public', 'private'] as const).map((tab) => (
                  <button
                    key={tab}
                    type="button"
                    onClick={() => setVisibilityFilter(tab)}
                    className={`px-2.5 py-1 text-xs font-medium rounded transition-colors capitalize cursor-pointer whitespace-nowrap ${
                      visibilityFilter === tab
                        ? 'bg-slate-800 text-slate-100'
                        : 'text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {tab}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="max-h-[540px] overflow-y-auto divide-y divide-slate-800/70">
            {reposLoading ? (
              <div className="p-6 flex items-center gap-2 text-xs text-slate-400">
                <Loader2 className="w-4 h-4 animate-spin" />
                Loading repositories from GitHub...
              </div>
            ) : reposError ? (
              <div className="p-6 space-y-3">
                <p className="text-xs text-red-300">{reposError}</p>
                <button
                  type="button"
                  onClick={() => void loadRepos()}
                  className="px-3 py-1.5 text-xs font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-500 cursor-pointer"
                >
                  Retry
                </button>
              </div>
            ) : filteredRepos.length === 0 ? (
              <div className="p-6 text-xs text-slate-400">
                No repositories match the current filter.
              </div>
            ) : (
              filteredRepos.map((repo) => {
                const isSelected = selectedRepo?.id === repo.id;
                return (
                  <button
                    key={repo.id}
                    type="button"
                    onClick={() => setSelectedRepo(repo)}
                    className={`w-full text-left px-4 py-3 transition-colors cursor-pointer flex items-start justify-between gap-3 ${
                      isSelected
                        ? 'bg-blue-600/10'
                        : 'hover:bg-slate-800/40'
                    }`}
                  >
                    <div className="min-w-0 space-y-1">
                      <div className="flex items-center gap-2">
                        <span
                          className={`text-sm font-medium truncate ${
                            isSelected ? 'text-blue-400' : 'text-slate-100'
                          }`}
                        >
                          {repo.full_name}
                        </span>
                      </div>
                      {repo.description && (
                        <p className="text-xs text-slate-400 line-clamp-1">
                          {repo.description}
                        </p>
                      )}
                      <div className="text-xs text-slate-500 flex items-center gap-2">
                        <span className="inline-flex items-center gap-1">
                          {repo.private && <Lock className="w-3 h-3" />}
                          {repo.private ? 'Private' : 'Public'}
                        </span>
                        {repo.language && (
                          <>
                            <span aria-hidden="true">·</span>
                            <span>{repo.language}</span>
                          </>
                        )}
                        <span aria-hidden="true">·</span>
                        <span className="font-mono">{repo.default_branch}</span>
                      </div>
                    </div>
                    <div className="inline-flex items-center gap-1 text-xs font-mono tabular-nums text-slate-400 shrink-0 pt-0.5">
                      <Star className="w-3.5 h-3.5" />
                      {repo.stargazers_count}
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>

        {/* Repository File Browser & Code Viewer */}
        <div className="lg:col-span-7 bg-[#111827] border border-slate-800 rounded-xl overflow-hidden min-h-[460px] flex flex-col">
          {selectedRepo ? (
            <>
              <div className="px-4 py-3.5 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-1.5 text-xs font-mono text-slate-300 min-w-0 flex-wrap">
                  <button
                    type="button"
                    onClick={() => void loadPath(selectedRepo, '')}
                    className="text-blue-400 hover:text-blue-300 cursor-pointer font-medium"
                  >
                    {selectedRepo.name}
                  </button>
                  {breadcrumbs.map((segment, index) => {
                    const pathUpToHere = breadcrumbs
                      .slice(0, index + 1)
                      .join('/');
                    return (
                      <span
                        key={pathUpToHere}
                        className="inline-flex items-center gap-1.5"
                      >
                        <ChevronRight className="w-3.5 h-3.5 text-slate-500" />
                        <button
                          type="button"
                          onClick={() =>
                            void loadPath(selectedRepo, pathUpToHere)
                          }
                          className="hover:text-white cursor-pointer"
                        >
                          {segment}
                        </button>
                      </span>
                    );
                  })}
                </div>

                <div className="flex items-center gap-3 text-xs text-slate-400 shrink-0">
                  <span className="inline-flex items-center gap-1 font-mono">
                    <GitBranch className="w-3.5 h-3.5" />
                    {selectedRepo.default_branch}
                  </span>
                  <a
                    href={openFile?.html_url || selectedRepo.html_url}
                    target="_blank"
                    rel="noreferrer"
                    className="text-slate-300 hover:text-white inline-flex items-center gap-1"
                  >
                    Open on GitHub
                    <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                </div>
              </div>

              <div className="flex-1 overflow-auto max-h-[540px]">
                {contentsLoading ? (
                  <div className="p-6 flex items-center gap-2 text-xs text-slate-400">
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Loading repository contents...
                  </div>
                ) : contentsError ? (
                  <div className="p-6 text-xs text-red-300">{contentsError}</div>
                ) : openFile ? (
                  <div className="bg-[#0B0F17]">
                    <div className="px-4 py-2 border-b border-slate-800 flex items-center justify-between text-xs text-slate-400">
                      <span className="font-mono text-slate-300">
                        {openFile.path}
                      </span>
                      <span className="font-mono tabular-nums">
                        {openFile.size} bytes
                      </span>
                    </div>
                    <pre className="p-4 text-xs font-mono leading-relaxed text-slate-200 overflow-x-auto">
                      {openFile.content.split('\n').map((line, i) => (
                        <div key={i} className="flex gap-4">
                          <span className="w-10 text-right select-none text-slate-600 tabular-nums shrink-0">
                            {i + 1}
                          </span>
                          <span className="whitespace-pre">{line}</span>
                        </div>
                      ))}
                    </pre>
                  </div>
                ) : entries.length === 0 ? (
                  <div className="p-6 text-xs text-slate-400">
                    This directory is empty.
                  </div>
                ) : (
                  <div className="divide-y divide-slate-800/70">
                    {currentPath && (
                      <button
                        type="button"
                        onClick={() => {
                          const parent = breadcrumbs.slice(0, -1).join('/');
                          void loadPath(selectedRepo, parent);
                        }}
                        className="w-full px-4 py-2.5 text-left text-xs font-mono text-slate-400 hover:bg-slate-800/40 hover:text-slate-200 flex items-center gap-2.5 cursor-pointer"
                      >
                        <Folder className="w-4 h-4 text-slate-500" />
                        ..
                      </button>
                    )}
                    {entries.map((entry) => (
                      <button
                        key={entry.sha + entry.path}
                        type="button"
                        onClick={() => void loadPath(selectedRepo, entry.path)}
                        className="w-full px-4 py-2.5 text-left hover:bg-slate-800/40 transition-colors flex items-center justify-between gap-4 cursor-pointer"
                      >
                        <span className="flex items-center gap-2.5 text-xs font-mono text-slate-200 min-w-0 truncate">
                          {entry.type === 'dir' ? (
                            <Folder className="w-4 h-4 text-blue-400 shrink-0" />
                          ) : (
                            <FileCode2 className="w-4 h-4 text-slate-400 shrink-0" />
                          )}
                          <span className="truncate">{entry.name}</span>
                        </span>
                        <span className="text-xs font-mono tabular-nums text-slate-500 shrink-0">
                          {entry.type === 'dir' ? 'Folder' : `${entry.size} B`}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="p-8 text-sm text-slate-400 flex-1 flex items-center justify-center">
              Select a repository from the list to inspect its files.
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
