import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import type {
  AuthStatus,
  GitHubUser,
  SessionApiResponse,
  SessionInfo,
} from '../types/auth';

interface AuthContextValue {
  status: AuthStatus;
  user: GitHubUser | null;
  session: SessionInfo | null;
  configured: boolean;
  callbackUrl: string;
  error: string | null;
  isAuthenticating: boolean;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<void>;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<GitHubUser | null>(null);
  const [session, setSession] = useState<SessionInfo | null>(null);
  const [configured, setConfigured] = useState<boolean>(false);
  const [callbackUrl, setCallbackUrl] = useState<string>(
    `${window.location.origin}/auth/callback`
  );
  const [error, setError] = useState<string | null>(null);
  const [isAuthenticating, setIsAuthenticating] = useState<boolean>(false);

  const popupRef = useRef<Window | null>(null);
  const pollTimerRef = useRef<number | null>(null);

  const stopPopupPolling = useCallback(() => {
    if (pollTimerRef.current !== null) {
      window.clearInterval(pollTimerRef.current);
      pollTimerRef.current = null;
    }
    popupRef.current = null;
  }, []);

  const refreshSession = useCallback(async () => {
    try {
      const response = await fetch('/api/auth/session', {
        credentials: 'include',
      });
      if (!response.ok) {
        throw new Error('Unable to verify user session with server.');
      }

      const data = (await response.json()) as SessionApiResponse;
      setConfigured(data.configured);
      if (data.callbackUrl) {
        setCallbackUrl(data.callbackUrl);
      }

      if (data.authenticated && data.user) {
        setUser(data.user);
        setSession(data.session);
        setStatus('authenticated');
      } else {
        setUser(null);
        setSession(null);
        setStatus('unauthenticated');
        if (data.reason) {
          setError(data.reason);
        }
      }
    } catch (err) {
      setUser(null);
      setSession(null);
      setStatus('unauthenticated');
      setError(
        err instanceof Error ? err.message : 'Failed to load session state.'
      );
    }
  }, []);

  useEffect(() => {
    void refreshSession();
  }, [refreshSession]);

  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const origin = event.origin;
      const isTrustedOrigin =
        origin === window.location.origin ||
        origin.endsWith('.run.app') ||
        origin.includes('localhost');

      if (!isTrustedOrigin) {
        return;
      }

      if (event.data?.type === 'OAUTH_AUTH_SUCCESS') {
        stopPopupPolling();
        setIsAuthenticating(false);
        setError(null);
        void refreshSession();
      } else if (event.data?.type === 'OAUTH_AUTH_ERROR') {
        stopPopupPolling();
        setIsAuthenticating(false);
        setError(
          typeof event.data?.error === 'string'
            ? event.data.error
            : 'GitHub authentication failed.'
        );
      }
    };

    window.addEventListener('message', handleMessage);
    return () => {
      window.removeEventListener('message', handleMessage);
      stopPopupPolling();
    };
  }, [refreshSession, stopPopupPolling]);

  const login = useCallback(async () => {
    setError(null);
    setIsAuthenticating(true);

    try {
      const response = await fetch('/api/auth/url', {
        credentials: 'include',
      });
      const data = (await response.json()) as {
        url?: string;
        error?: string;
        callbackUrl?: string;
      };

      if (data.callbackUrl) {
        setCallbackUrl(data.callbackUrl);
      }

      if (!response.ok || !data.url) {
        setIsAuthenticating(false);
        setError(
          data.error ||
            'Unable to initiate GitHub OAuth flow. Check server configuration.'
        );
        return;
      }

      const width = 600;
      const height = 720;
      const left = window.screenX + (window.outerWidth - width) / 2;
      const top = window.screenY + (window.outerHeight - height) / 2;

      const authWindow = window.open(
        data.url,
        'github_oauth_popup',
        `width=${width},height=${height},left=${Math.max(0, left)},top=${Math.max(0, top)},resizable=yes,scrollbars=yes`
      );

      if (!authWindow) {
        setIsAuthenticating(false);
        setError(
          'Your browser blocked the GitHub sign-in popup. Please allow popups for this site and try again.'
        );
        return;
      }

      popupRef.current = authWindow;
      stopPopupPolling();
      popupRef.current = authWindow;

      pollTimerRef.current = window.setInterval(() => {
        if (!popupRef.current || popupRef.current.closed) {
          stopPopupPolling();
          setIsAuthenticating(false);
        }
      }, 400);
    } catch (err) {
      setIsAuthenticating(false);
      setError(
        err instanceof Error
          ? err.message
          : 'Network error while initiating GitHub login.'
      );
    }
  }, [stopPopupPolling]);

  const logout = useCallback(async () => {
    setError(null);
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
      });
    } finally {
      setUser(null);
      setSession(null);
      setStatus('unauthenticated');
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  return (
    <AuthContext.Provider
      value={{
        status,
        user,
        session,
        configured,
        callbackUrl,
        error,
        isAuthenticating,
        login,
        logout,
        refreshSession,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
