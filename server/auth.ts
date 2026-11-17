import crypto from 'crypto';
import type { CookieOptions } from 'express';

export const SESSION_COOKIE_NAME = 'gh_session';
export const OAUTH_STATE_COOKIE_NAME = 'gh_oauth_state';
const SESSION_MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000;
const OAUTH_STATE_MAX_AGE_MS = 10 * 60 * 1000;

export interface SessionPayload {
  accessToken: string;
  scope: string;
  tokenType: string;
  createdAt: number;
}

// Short-lived in-memory state store as a resilient fallback alongside the state cookie
const pendingStates = new Map<string, number>();

function getEncryptionKey(): Buffer {
  const rawSecret =
    process.env.SESSION_SECRET ||
    process.env.GITHUB_CLIENT_SECRET ||
    'gitforge-ephemeral-dev-session-secret';
  return crypto.createHash('sha256').update(rawSecret).digest();
}

export function getAppBaseUrl(): string {
  const envUrl = process.env.APP_URL?.trim();
  if (envUrl && envUrl !== 'MY_APP_URL') {
    return envUrl.replace(/\/+$/, '');
  }
  return 'http://localhost:3000';
}

export function getCallbackUrl(): string {
  return `${getAppBaseUrl()}/auth/callback`;
}

export function isGitHubOAuthConfigured(): boolean {
  return Boolean(
    process.env.GITHUB_CLIENT_ID?.trim() &&
      process.env.GITHUB_CLIENT_SECRET?.trim()
  );
}

export function getSessionCookieOptions(): CookieOptions {
  return {
    httpOnly: true,
    secure: true,
    sameSite: 'none',
    path: '/',
    maxAge: SESSION_MAX_AGE_MS,
  };
}

export function getClearCookieOptions(): CookieOptions {
  return {
    httpOnly: true,
    secure: true,
    sameSite: 'none',
    path: '/',
  };
}

export function getStateCookieOptions(): CookieOptions {
  return {
    httpOnly: true,
    secure: true,
    sameSite: 'none',
    path: '/',
    maxAge: OAUTH_STATE_MAX_AGE_MS,
  };
}

export function createOAuthState(): string {
  const now = Date.now();
  for (const [key, expiresAt] of pendingStates.entries()) {
    if (expiresAt <= now) {
      pendingStates.delete(key);
    }
  }
  const state = crypto.randomBytes(24).toString('hex');
  pendingStates.set(state, now + OAUTH_STATE_MAX_AGE_MS);
  return state;
}

export function verifyAndConsumeOAuthState(
  queryState: string | undefined,
  cookieState: string | undefined
): boolean {
  if (!queryState) return false;

  const expiresAt = pendingStates.get(queryState);
  if (expiresAt && expiresAt > Date.now()) {
    pendingStates.delete(queryState);
    return true;
  }

  if (cookieState && cookieState.length === queryState.length) {
    return crypto.timingSafeEqual(
      Buffer.from(cookieState),
      Buffer.from(queryState)
    );
  }

  return false;
}

export function encryptSession(payload: SessionPayload): string {
  const key = getEncryptionKey();
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
  const plaintext = JSON.stringify(payload);
  const encrypted = Buffer.concat([
    cipher.update(plaintext, 'utf8'),
    cipher.final(),
  ]);
  const authTag = cipher.getAuthTag();
  return `${iv.toString('base64url')}.${authTag.toString('base64url')}.${encrypted.toString('base64url')}`;
}

export function decryptSession(token: string | undefined): SessionPayload | null {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;

  try {
    const [ivB64, tagB64, dataB64] = parts;
    const key = getEncryptionKey();
    const iv = Buffer.from(ivB64, 'base64url');
    const authTag = Buffer.from(tagB64, 'base64url');
    const encrypted = Buffer.from(dataB64, 'base64url');

    const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
    decipher.setAuthTag(authTag);
    const decrypted = Buffer.concat([
      decipher.update(encrypted),
      decipher.final(),
    ]);

    const parsed = JSON.parse(decrypted.toString('utf8')) as SessionPayload;
    if (!parsed?.accessToken) return null;
    if (Date.now() - parsed.createdAt > SESSION_MAX_AGE_MS) return null;
    return parsed;
  } catch {
    return null;
  }
}
