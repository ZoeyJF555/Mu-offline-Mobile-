import crypto from 'node:crypto';
import { Router } from 'express';
import {
  clearOAuthStateCookie,
  clearSessionCookie,
  getOAuthStateCookie,
  getSessionToken,
  setOAuthStateCookie,
  setSessionCookie,
} from './cryptoSession.ts';

export const authRouter = Router();

function getAppBaseUrl(): string {
  const appUrl = process.env.APP_URL?.trim();
  if (appUrl && appUrl !== 'MY_APP_URL') {
    return appUrl.replace(/\/+$/, '');
  }
  return 'http://localhost:3000';
}

function getRedirectUri(): string {
  return `${getAppBaseUrl()}/auth/callback`;
}

authRouter.get('/api/auth/status', async (req, res) => {
  const clientId = process.env.GITHUB_CLIENT_ID?.trim();
  const clientSecret = process.env.GITHUB_CLIENT_SECRET?.trim();
  const configured = Boolean(
    clientId &&
      clientSecret &&
      clientId !== 'YOUR_GITHUB_CLIENT_ID' &&
      clientSecret !== 'YOUR_GITHUB_CLIENT_SECRET'
  );

  const callbackUrl = getRedirectUri();
  const token = getSessionToken(req);

  if (!token) {
    res.json({
      configured,
      authenticated: false,
      user: null,
      callbackUrl,
    });
    return;
  }

  try {
    const ghRes = await fetch('https://api.github.com/user', {
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
        'User-Agent': 'GitForge-Studio',
      },
    });

    if (!ghRes.ok) {
      clearSessionCookie(res);
      res.json({
        configured,
        authenticated: false,
        user: null,
        callbackUrl,
      });
      return;
    }

    const userData = (await ghRes.json()) as {
      login: string;
      name: string | null;
      avatar_url: string;
      html_url: string;
      public_repos: number;
      total_private_repos?: number;
    };

    res.json({
      configured,
      authenticated: true,
      user: {
        login: userData.login,
        name: userData.name,
        avatarUrl: userData.avatar_url,
        htmlUrl: userData.html_url,
        publicRepos: userData.public_repos,
        privateRepos: userData.total_private_repos ?? 0,
      },
      callbackUrl,
    });
  } catch {
    res.json({
      configured,
      authenticated: false,
      user: null,
      callbackUrl,
    });
  }
});

authRouter.get('/api/auth/url', (req, res) => {
  const clientId = process.env.GITHUB_CLIENT_ID?.trim();
  const clientSecret = process.env.GITHUB_CLIENT_SECRET?.trim();

  if (
    !clientId ||
    !clientSecret ||
    clientId === 'YOUR_GITHUB_CLIENT_ID' ||
    clientSecret === 'YOUR_GITHUB_CLIENT_SECRET'
  ) {
    res.status(400).json({
      error:
        'GitHub OAuth no está configurado todavía. Agrega GITHUB_CLIENT_ID y GITHUB_CLIENT_SECRET en los Secrets.',
      callbackUrl: getRedirectUri(),
    });
    return;
  }

  const state = crypto.randomBytes(16).toString('hex');
  setOAuthStateCookie(res, state);

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: getRedirectUri(),
    scope: 'repo read:user user:email',
    state,
  });

  const url = `https://github.com/login/oauth/authorize?${params.toString()}`;
  res.json({ url });
});

authRouter.get(['/auth/callback', '/auth/callback/'], async (req, res) => {
  const code = typeof req.query.code === 'string' ? req.query.code : null;
  const state = typeof req.query.state === 'string' ? req.query.state : null;
  const errorParam = typeof req.query.error === 'string' ? req.query.error : null;

  if (errorParam) {
    res.status(400).send(renderCallbackHtml(false, `Autorización cancelada: ${errorParam}`));
    return;
  }

  if (!code) {
    res.status(400).send(renderCallbackHtml(false, 'No se recibió código de autorización de GitHub.'));
    return;
  }

  const savedState = getOAuthStateCookie(req);
  clearOAuthStateCookie(res);
  if (savedState && state && savedState !== state) {
    res.status(400).send(renderCallbackHtml(false, 'Estado OAuth inválido. Intenta iniciar sesión nuevamente.'));
    return;
  }

  const clientId = process.env.GITHUB_CLIENT_ID?.trim();
  const clientSecret = process.env.GITHUB_CLIENT_SECRET?.trim();
  if (!clientId || !clientSecret) {
    res.status(500).send(renderCallbackHtml(false, 'Faltan credenciales OAuth en el servidor.'));
    return;
  }

  try {
    const tokenRes = await fetch('https://github.com/login/oauth/access_token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify({
        client_id: clientId,
        client_secret: clientSecret,
        code,
        redirect_uri: getRedirectUri(),
      }),
    });

    const tokenData = (await tokenRes.json()) as {
      access_token?: string;
      error?: string;
      error_description?: string;
    };

    if (!tokenData.access_token) {
      const msg = tokenData.error_description || tokenData.error || 'No se pudo obtener el token de GitHub.';
      res.status(400).send(renderCallbackHtml(false, msg));
      return;
    }

    setSessionCookie(res, tokenData.access_token);
    res.send(renderCallbackHtml(true, 'Autenticación completada.'));
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Error de red al conectar con GitHub.';
    res.status(500).send(renderCallbackHtml(false, msg));
  }
});

authRouter.post('/api/auth/logout', (_req, res) => {
  clearSessionCookie(res);
  res.json({ ok: true });
});

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function renderCallbackHtml(success: boolean, message: string): string {
  const payload = JSON.stringify(
    success ? { type: 'OAUTH_AUTH_SUCCESS' } : { type: 'OAUTH_AUTH_ERROR', error: message }
  );

  return `<!doctype html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <title>GitHub OAuth — GitForge Studio</title>
    <style>
      body {
        margin: 0;
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #0b0f17;
        color: #f1f5f9;
        font-family: system-ui, -apple-system, sans-serif;
        text-align: center;
        padding: 24px;
      }
      .box {
        max-width: 380px;
        border: 1px solid #1f2937;
        background: #111827;
        border-radius: 10px;
        padding: 24px;
      }
      h1 { font-size: 16px; margin: 0 0 8px; }
      p { font-size: 13px; color: #94a3b8; margin: 0; }
    </style>
  </head>
  <body>
    <div class="box">
      <h1>${success ? 'Conectado a GitHub' : 'Error de autenticación'}</h1>
      <p>${escapeHtml(message)}</p>
    </div>
    <script>
      if (window.opener) {
        window.opener.postMessage(${payload}, '*');
        setTimeout(function () { window.close(); }, 350);
      } else {
        window.location.href = '/';
      }
    </script>
  </body>
</html>`;
}
