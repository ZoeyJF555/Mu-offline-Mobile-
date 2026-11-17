import { BUILT_IN_MODS } from '../data/muCatalog';
import type { AccountRecord, HeroSave, ModPackage } from '../types/game';

const DB_NAME = 'mu_offline_mobile_db';
const DB_VERSION = 2;
const STORE_HEROES = 'heroes';
const STORE_MODS = 'mods';
const STORE_ACCOUNTS = 'accounts';
const LOCAL_HEROES_KEY = 'mu_offline_heroes_v1';
const LOCAL_MODS_KEY = 'mu_offline_mods_v1';
const LOCAL_ACCOUNTS_KEY = 'mu_offline_accounts_v1';
const LOCAL_SESSION_ACCOUNT_KEY = 'mu_offline_session_account_v1';

function openDb(): Promise<IDBDatabase | null> {
  if (typeof indexedDB === 'undefined') return Promise.resolve(null);
  return new Promise((resolve) => {
    try {
      const req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onupgradeneeded = () => {
        const db = req.result;
        if (!db.objectStoreNames.contains(STORE_HEROES)) {
          db.createObjectStore(STORE_HEROES, { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains(STORE_MODS)) {
          db.createObjectStore(STORE_MODS, { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains(STORE_ACCOUNTS)) {
          db.createObjectStore(STORE_ACCOUNTS, { keyPath: 'accountId' });
        }
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => resolve(null);
    } catch {
      resolve(null);
    }
  });
}

async function hashPassword(accountId: string, password: string): Promise<string> {
  const input = `${accountId.toLowerCase()}::${password}`;
  if (typeof crypto !== 'undefined' && crypto.subtle) {
    try {
      const bytes = new TextEncoder().encode(input);
      const digest = await crypto.subtle.digest('SHA-256', bytes);
      return Array.from(new Uint8Array(digest))
        .map((b) => b.toString(16).padStart(2, '0'))
        .join('');
    } catch {
      // Fallback below for non-HTTPS LAN IPs
    }
  }
  let h1 = 0x811c9dc5;
  let h2 = 0x9e3779b9;
  for (let i = 0; i < input.length; i++) {
    const ch = input.charCodeAt(i);
    h1 ^= ch;
    h1 = Math.imul(h1, 0x01000193);
    h2 ^= ch + i;
    h2 = Math.imul(h2, 0x85ebca6b);
  }
  return `${(h1 >>> 0).toString(16).padStart(8, '0')}${(h2 >>> 0).toString(16).padStart(8, '0')}`;
}

async function getAllAccounts(): Promise<AccountRecord[]> {
  const db = await openDb();
  if (db) {
    try {
      const list = await new Promise<AccountRecord[]>((resolve) => {
        const tx = db.transaction(STORE_ACCOUNTS, 'readonly');
        const req = tx.objectStore(STORE_ACCOUNTS).getAll();
        req.onsuccess = () => resolve((req.result as AccountRecord[]) || []);
        req.onerror = () => resolve([]);
      });
      if (list.length > 0) return list;
    } catch {
      // fallback
    }
  }
  try {
    return JSON.parse(localStorage.getItem(LOCAL_ACCOUNTS_KEY) || '[]') as AccountRecord[];
  } catch {
    return [];
  }
}

async function putAccount(record: AccountRecord): Promise<void> {
  try {
    const existing = JSON.parse(localStorage.getItem(LOCAL_ACCOUNTS_KEY) || '[]') as AccountRecord[];
    const filtered = existing.filter((a) => a.accountId !== record.accountId);
    localStorage.setItem(LOCAL_ACCOUNTS_KEY, JSON.stringify([record, ...filtered]));
  } catch {
    // ignore
  }
  const db = await openDb();
  if (db) {
    try {
      await new Promise<void>((resolve) => {
        const tx = db.transaction(STORE_ACCOUNTS, 'readwrite');
        tx.objectStore(STORE_ACCOUNTS).put(record);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      });
    } catch {
      // ignore
    }
  }
}

export async function registerAccount(
  rawId: string,
  password: string
): Promise<{ ok: boolean; accountId?: string; error?: string }> {
  const accountId = rawId.trim().toLowerCase();
  if (accountId.length < 3 || !/^[a-z0-9_-]+$/.test(accountId)) {
    return { ok: false, error: 'El ID debe tener al menos 3 caracteres (solo letras, números, - o _).' };
  }
  if (password.length < 4) {
    return { ok: false, error: 'La contraseña debe tener al menos 4 caracteres.' };
  }
  const accounts = await getAllAccounts();
  if (accounts.some((a) => a.accountId === accountId)) {
    return { ok: false, error: 'Ese ID de cuenta ya existe en este dispositivo. Usa Ingresar.' };
  }

  const passwordHash = await hashPassword(accountId, password);
  await putAccount({ accountId, passwordHash, createdAt: Date.now() });
  localStorage.setItem(LOCAL_SESSION_ACCOUNT_KEY, accountId);

  // Assign any legacy unowned heroes to the first created account so progress is never lost
  const allHeroes = await loadAllHeroesRaw();
  for (const h of allHeroes) {
    if (!h.accountId) {
      await saveHero({ ...h, accountId });
    }
  }

  return { ok: true, accountId };
}

export async function loginAccount(
  rawId: string,
  password: string
): Promise<{ ok: boolean; accountId?: string; error?: string }> {
  const accountId = rawId.trim().toLowerCase();
  const accounts = await getAllAccounts();
  const found = accounts.find((a) => a.accountId === accountId);
  if (!found) {
    return { ok: false, error: 'Cuenta no encontrada en este dispositivo. Regístrate primero.' };
  }
  const passwordHash = await hashPassword(accountId, password);
  if (found.passwordHash !== passwordHash) {
    return { ok: false, error: 'Contraseña incorrecta.' };
  }
  localStorage.setItem(LOCAL_SESSION_ACCOUNT_KEY, accountId);
  return { ok: true, accountId };
}

export function getActiveSessionAccount(): string | null {
  try {
    return localStorage.getItem(LOCAL_SESSION_ACCOUNT_KEY);
  } catch {
    return null;
  }
}

export function logoutAccount(): void {
  try {
    localStorage.removeItem(LOCAL_SESSION_ACCOUNT_KEY);
  } catch {
    // ignore
  }
}

async function loadAllHeroesRaw(): Promise<HeroSave[]> {
  const db = await openDb();
  if (db) {
    try {
      const result = await new Promise<HeroSave[]>((resolve, reject) => {
        const tx = db.transaction(STORE_HEROES, 'readonly');
        const store = tx.objectStore(STORE_HEROES);
        const req = store.getAll();
        req.onsuccess = () => resolve((req.result as HeroSave[]) || []);
        req.onerror = () => reject(req.error);
      });
      if (result.length > 0) {
        return result.sort((a, b) => b.updatedAt - a.updatedAt);
      }
    } catch {
      // Fallback
    }
  }
  try {
    const raw = localStorage.getItem(LOCAL_HEROES_KEY);
    return raw ? (JSON.parse(raw) as HeroSave[]) : [];
  } catch {
    return [];
  }
}

export async function loadSavedHeroes(accountId?: string): Promise<HeroSave[]> {
  const all = await loadAllHeroesRaw();
  if (!accountId) return all;
  return all.filter((h) => h.accountId === accountId);
}

export async function saveHero(hero: HeroSave): Promise<void> {
  const updated = { ...hero, updatedAt: Date.now() };
  try {
    const existing = JSON.parse(localStorage.getItem(LOCAL_HEROES_KEY) || '[]') as HeroSave[];
    const filtered = existing.filter((h) => h.id !== updated.id);
    localStorage.setItem(LOCAL_HEROES_KEY, JSON.stringify([updated, ...filtered]));
  } catch {
    // ignore
  }

  const db = await openDb();
  if (db) {
    try {
      await new Promise<void>((resolve) => {
        const tx = db.transaction(STORE_HEROES, 'readwrite');
        tx.objectStore(STORE_HEROES).put(updated);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      });
    } catch {
      // ignore
    }
  }
}

export async function deleteHero(heroId: string): Promise<void> {
  try {
    const existing = JSON.parse(localStorage.getItem(LOCAL_HEROES_KEY) || '[]') as HeroSave[];
    localStorage.setItem(LOCAL_HEROES_KEY, JSON.stringify(existing.filter((h) => h.id !== heroId)));
  } catch {
    // ignore
  }
  const db = await openDb();
  if (db) {
    try {
      await new Promise<void>((resolve) => {
        const tx = db.transaction(STORE_HEROES, 'readwrite');
        tx.objectStore(STORE_HEROES).delete(heroId);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      });
    } catch {
      // ignore
    }
  }
}

export async function loadMods(): Promise<ModPackage[]> {
  let stored: ModPackage[] = [];
  const db = await openDb();
  if (db) {
    try {
      stored = await new Promise<ModPackage[]>((resolve) => {
        const tx = db.transaction(STORE_MODS, 'readonly');
        const req = tx.objectStore(STORE_MODS).getAll();
        req.onsuccess = () => resolve((req.result as ModPackage[]) || []);
        req.onerror = () => resolve([]);
      });
    } catch {
      stored = [];
    }
  }
  if (stored.length === 0) {
    try {
      const raw = localStorage.getItem(LOCAL_MODS_KEY);
      if (raw) stored = JSON.parse(raw) as ModPackage[];
    } catch {
      stored = [];
    }
  }

  const byId = new Map<string, ModPackage>();
  for (const mod of BUILT_IN_MODS) {
    byId.set(mod.id, mod);
  }
  for (const mod of stored) {
    const builtIn = byId.get(mod.id);
    if (builtIn) {
      byId.set(mod.id, { ...builtIn, enabled: mod.enabled });
    } else {
      byId.set(mod.id, mod);
    }
  }
  return Array.from(byId.values());
}

export async function saveMods(mods: ModPackage[]): Promise<void> {
  try {
    localStorage.setItem(LOCAL_MODS_KEY, JSON.stringify(mods));
  } catch {
    // ignore
  }
  const db = await openDb();
  if (db) {
    try {
      await new Promise<void>((resolve) => {
        const tx = db.transaction(STORE_MODS, 'readwrite');
        const store = tx.objectStore(STORE_MODS);
        store.clear();
        for (const m of mods) {
          store.put(m);
        }
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      });
    } catch {
      // ignore
    }
  }
}
