import crypto from 'node:crypto';
import type { Request, Response } from 'express';

const COOKIE_NAME = 'gf_session';
const STATE_COOKIE_NAME = 'gf_oauth_state';

function getEncryptionKey(): Buffer {
  const rawSecret =
    process.env.SESSION_SECRET ||
    process.env.GITHUB_CLIENT_SECRET ||
    `${process.env.APP_URL || 'local-dev'}-gitforge-studio-ephemeral-key`;
  return crypto.createHash('sha256').update(rawSecret).digest();
}

export function encryptToken(token: string): string {
  const key = getEncryptionKey();
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
  const encrypted = Buffer.concat([cipher.update(token, 'utf8'), cipher.final()]);
  const authTag = cipher.getAuthTag();
  return Buffer.concat([iv, authTag, encrypted]).toString('base64url');
}

export function decryptToken(payload: string): string | null {
  try {
    const buf = Buffer.from(payload, 'base64url');
    if (buf.length < 28) return null;
    const iv = buf.subarray(0, 12);
    const authTag = buf.subarray(12, 28);
    const encrypted = buf.subarray(28);
    const key = getEncryptionKey();
    const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
    decipher.setAuthTag(authTag);
    const decrypted = Buffer.concat([decipher.update(encrypted), decipher.final()]);
    return decrypted.toString('utf8');
  } catch {
    return null;
  }
}

export function getSessionToken(req: Request): string | null {
  const raw = req.cookies?.[COOKIE_NAME];
  if (!raw || typeof raw !== 'string') return null;
  return decryptToken(raw);
}

export function setSessionCookie(res: Response, token: string): void {
  const encrypted = encryptToken(token);
  res.cookie(COOKIE_NAME, encrypted, {
    httpOnly: true,
    secure: true,
    sameSite: 'none',
    maxAge: 7 * 24 * 60 * 60 * 1000,
    path: '/',
  });
}

export function clearSessionCookie(res: Response): void {
  res.clearCookie(COOKIE_NAME, {
    httpOnly: true,
    secure: true,
    sameSite: 'none',
    path: '/',
  });
}

export function setOAuthStateCookie(res: Response, state: string): void {
  res.cookie(STATE_COOKIE_NAME, state, {
    httpOnly: true,
    secure: true,
    sameSite: 'none',
    maxAge: 10 * 60 * 1000,
    path: '/',
  });
}

export function getOAuthStateCookie(req: Request): string | null {
  const val = req.cookies?.[STATE_COOKIE_NAME];
  return typeof val === 'string' ? val : null;
}

export function clearOAuthStateCookie(res: Response): void {
  res.clearCookie(STATE_COOKIE_NAME, {
    httpOnly: true,
    secure: true,
    sameSite: 'none',
    path: '/',
  });
}
