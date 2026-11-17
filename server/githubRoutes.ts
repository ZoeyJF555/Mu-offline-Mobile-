import { Router, type Request } from 'express';
import { getSessionToken } from './cryptoSession.ts';

export const githubRouter = Router();

interface GitHubRequestOptions {
  method?: string;
  body?: unknown;
  requireAuth?: boolean;
}

async function githubFetch<T>(
  req: Request,
  path: string,
  options: GitHubRequestOptions = {}
): Promise<{ status: number; data: T }> {
  const token = getSessionToken(req);

  if (options.requireAuth && !token) {
    return {
      status: 401,
      data: {
        message: 'Debes iniciar sesión con GitHub OAuth para realizar esta acción.',
      } as T,
    };
  }

  const headers: Record<string, string> = {
    Accept: 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28',
    'User-Agent': 'GitForge-Studio',
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`https://api.github.com${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  const text = await response.text();
  let parsed: unknown = {};
  if (text) {
    try {
      parsed = JSON.parse(text);
    } catch {
      parsed = { message: text };
    }
  }

  return {
    status: response.status,
    data: parsed as T,
  };
}

function encodePathSegments(filePath: string): string {
  return filePath
    .split('/')
    .filter(Boolean)
    .map((segment) => encodeURIComponent(segment))
    .join('/');
}

// List authenticated user's repositories
githubRouter.get('/api/github/repos', async (req, res) => {
  const token = getSessionToken(req);
  if (!token) {
    res.status(401).json({ error: 'No autenticado con GitHub OAuth.' });
    return;
  }

  const result = await githubFetch<unknown[]>(
    req,
    '/user/repos?sort=updated&per_page=100&affiliation=owner,collaborator,organization_member'
  );

  if (result.status >= 400) {
    res.status(result.status).json(result.data);
    return;
  }

  res.json(result.data);
});

// Get single repository metadata (works for public repos even without OAuth)
githubRouter.get('/api/github/repos/:owner/:repo', async (req, res) => {
  const { owner, repo } = req.params;
  const result = await githubFetch(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}`
  );
  res.status(result.status).json(result.data);
});

// List branches
githubRouter.get('/api/github/repos/:owner/:repo/branches', async (req, res) => {
  const { owner, repo } = req.params;
  const result = await githubFetch(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/branches?per_page=100`
  );
  res.status(result.status).json(result.data);
});

// Get recursive file tree for branch
githubRouter.get('/api/github/repos/:owner/:repo/tree', async (req, res) => {
  const { owner, repo } = req.params;
  const branch = typeof req.query.branch === 'string' ? req.query.branch : 'main';

  const branchRes = await githubFetch<{ commit?: { sha: string } }>(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/branches/${encodeURIComponent(branch)}`
  );

  if (branchRes.status >= 400) {
    res.status(branchRes.status).json(branchRes.data);
    return;
  }

  const commitSha = branchRes.data?.commit?.sha || branch;
  const treeRes = await githubFetch<{
    sha: string;
    truncated?: boolean;
    tree: Array<{ path: string; type: 'blob' | 'tree'; sha: string; size?: number }>;
  }>(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/git/trees/${encodeURIComponent(commitSha)}?recursive=1`
  );

  res.status(treeRes.status).json(treeRes.data);
});

// Read file content
githubRouter.get('/api/github/repos/:owner/:repo/file', async (req, res) => {
  const { owner, repo } = req.params;
  const filePath = typeof req.query.path === 'string' ? req.query.path : '';
  const ref = typeof req.query.ref === 'string' ? req.query.ref : '';

  if (!filePath) {
    res.status(400).json({ message: 'Falta el parámetro path.' });
    return;
  }

  const query = ref ? `?ref=${encodeURIComponent(ref)}` : '';
  const result = await githubFetch<{
    type?: string;
    encoding?: string;
    content?: string;
    sha?: string;
    size?: number;
    name?: string;
    path?: string;
    html_url?: string;
    message?: string;
  }>(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/contents/${encodePathSegments(filePath)}${query}`
  );

  if (result.status >= 400) {
    res.status(result.status).json(result.data);
    return;
  }

  const data = result.data;
  if (data.type !== 'file') {
    res.status(400).json({ message: 'La ruta seleccionada no es un archivo.' });
    return;
  }

  let decodedContent = '';
  let isBinary = false;

  if (data.encoding === 'base64' && typeof data.content === 'string') {
    const buffer = Buffer.from(data.content.replace(/\n/g, ''), 'base64');
    // Heuristic binary check: presence of null bytes
    if (buffer.includes(0)) {
      isBinary = true;
    } else {
      decodedContent = buffer.toString('utf8');
    }
  }

  res.json({
    name: data.name,
    path: data.path,
    sha: data.sha,
    size: data.size,
    htmlUrl: data.html_url,
    isBinary,
    content: decodedContent,
  });
});

// Commit file update or creation (with optional new branch creation)
githubRouter.put('/api/github/repos/:owner/:repo/file', async (req, res) => {
  const { owner, repo } = req.params;
  const { path, content, message, sha, branch, createNewBranch, baseBranch } = req.body as {
    path?: string;
    content?: string;
    message?: string;
    sha?: string;
    branch?: string;
    createNewBranch?: string;
    baseBranch?: string;
  };

  if (!path || typeof content !== 'string' || !message?.trim()) {
    res.status(400).json({ message: 'Se requiere path, content y mensaje de commit.' });
    return;
  }

  let targetBranch = branch || 'main';

  // If user requested creating a new branch before committing
  if (createNewBranch && createNewBranch.trim()) {
    const newBranchName = createNewBranch.trim();
    const sourceBranch = baseBranch || targetBranch;

    const refRes = await githubFetch<{ object?: { sha: string }; message?: string }>(
      req,
      `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/git/ref/heads/${encodeURIComponent(sourceBranch)}`,
      { requireAuth: true }
    );

    if (refRes.status >= 400 || !refRes.data?.object?.sha) {
      res.status(refRes.status || 400).json({
        message: refRes.data?.message || `No se pudo leer la rama base ${sourceBranch}.`,
      });
      return;
    }

    const createRefRes = await githubFetch<{ message?: string }>(
      req,
      `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/git/refs`,
      {
        method: 'POST',
        requireAuth: true,
        body: {
          ref: `refs/heads/${newBranchName}`,
          sha: refRes.data.object.sha,
        },
      }
    );

    if (createRefRes.status >= 400) {
      res.status(createRefRes.status).json(createRefRes.data);
      return;
    }

    targetBranch = newBranchName;
  }

  const encodedContent = Buffer.from(content, 'utf8').toString('base64');
  const payload: Record<string, string> = {
    message: message.trim(),
    content: encodedContent,
    branch: targetBranch,
  };
  if (sha) {
    payload.sha = sha;
  }

  const commitRes = await githubFetch<{
    content?: { sha: string; path: string; html_url: string };
    commit?: { sha: string; html_url: string; message: string };
    message?: string;
  }>(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/contents/${encodePathSegments(path)}`,
    {
      method: 'PUT',
      requireAuth: true,
      body: payload,
    }
  );

  if (commitRes.status >= 400) {
    res.status(commitRes.status).json(commitRes.data);
    return;
  }

  res.json({
    branch: targetBranch,
    fileSha: commitRes.data.content?.sha,
    commitSha: commitRes.data.commit?.sha,
    commitUrl: commitRes.data.commit?.html_url,
    message: commitRes.data.commit?.message,
  });
});

// List Issues (excluding PRs)
githubRouter.get('/api/github/repos/:owner/:repo/issues', async (req, res) => {
  const { owner, repo } = req.params;
  const state = typeof req.query.state === 'string' ? req.query.state : 'open';
  const result = await githubFetch<Array<Record<string, unknown>>>(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues?state=${encodeURIComponent(state)}&per_page=50`
  );

  if (result.status >= 400) {
    res.status(result.status).json(result.data);
    return;
  }

  const issuesOnly = Array.isArray(result.data)
    ? result.data.filter((item) => !item.pull_request)
    : [];
  res.json(issuesOnly);
});

// Create Issue
githubRouter.post('/api/github/repos/:owner/:repo/issues', async (req, res) => {
  const { owner, repo } = req.params;
  const { title, body, labels } = req.body as {
    title?: string;
    body?: string;
    labels?: string[];
  };

  if (!title?.trim()) {
    res.status(400).json({ message: 'El título del issue es obligatorio.' });
    return;
  }

  const result = await githubFetch(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues`,
    {
      method: 'POST',
      requireAuth: true,
      body: {
        title: title.trim(),
        body: body?.trim() || '',
        labels: Array.isArray(labels) ? labels : undefined,
      },
    }
  );

  res.status(result.status).json(result.data);
});

// Get single Issue + Comments
githubRouter.get('/api/github/repos/:owner/:repo/issues/:number', async (req, res) => {
  const { owner, repo, number } = req.params;
  const [issueRes, commentsRes] = await Promise.all([
    githubFetch(
      req,
      `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues/${encodeURIComponent(number)}`
    ),
    githubFetch(
      req,
      `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues/${encodeURIComponent(number)}/comments?per_page=50`
    ),
  ]);

  if (issueRes.status >= 400) {
    res.status(issueRes.status).json(issueRes.data);
    return;
  }

  res.json({
    issue: issueRes.data,
    comments: Array.isArray(commentsRes.data) ? commentsRes.data : [],
  });
});

// Update Issue state (open / closed)
githubRouter.patch('/api/github/repos/:owner/:repo/issues/:number', async (req, res) => {
  const { owner, repo, number } = req.params;
  const { state, title, body } = req.body as {
    state?: 'open' | 'closed';
    title?: string;
    body?: string;
  };

  const result = await githubFetch(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues/${encodeURIComponent(number)}`,
    {
      method: 'PATCH',
      requireAuth: true,
      body: {
        ...(state ? { state } : {}),
        ...(title !== undefined ? { title } : {}),
        ...(body !== undefined ? { body } : {}),
      },
    }
  );

  res.status(result.status).json(result.data);
});

// Add comment to Issue or Pull Request
githubRouter.post('/api/github/repos/:owner/:repo/issues/:number/comments', async (req, res) => {
  const { owner, repo, number } = req.params;
  const { body } = req.body as { body?: string };

  if (!body?.trim()) {
    res.status(400).json({ message: 'El comentario no puede estar vacío.' });
    return;
  }

  const result = await githubFetch(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues/${encodeURIComponent(number)}/comments`,
    {
      method: 'POST',
      requireAuth: true,
      body: { body: body.trim() },
    }
  );

  res.status(result.status).json(result.data);
});

// List Pull Requests
githubRouter.get('/api/github/repos/:owner/:repo/pulls', async (req, res) => {
  const { owner, repo } = req.params;
  const state = typeof req.query.state === 'string' ? req.query.state : 'open';
  const result = await githubFetch(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/pulls?state=${encodeURIComponent(state)}&per_page=50`
  );
  res.status(result.status).json(result.data);
});

// Get single Pull Request + files + comments
githubRouter.get('/api/github/repos/:owner/:repo/pulls/:number', async (req, res) => {
  const { owner, repo, number } = req.params;
  const [prRes, filesRes, commentsRes] = await Promise.all([
    githubFetch(
      req,
      `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/pulls/${encodeURIComponent(number)}`
    ),
    githubFetch(
      req,
      `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/pulls/${encodeURIComponent(number)}/files?per_page=50`
    ),
    githubFetch(
      req,
      `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues/${encodeURIComponent(number)}/comments?per_page=50`
    ),
  ]);

  if (prRes.status >= 400) {
    res.status(prRes.status).json(prRes.data);
    return;
  }

  res.json({
    pull: prRes.data,
    files: Array.isArray(filesRes.data) ? filesRes.data : [],
    comments: Array.isArray(commentsRes.data) ? commentsRes.data : [],
  });
});

// Merge Pull Request
githubRouter.put('/api/github/repos/:owner/:repo/pulls/:number/merge', async (req, res) => {
  const { owner, repo, number } = req.params;
  const { merge_method, commit_title, commit_message } = req.body as {
    merge_method?: 'merge' | 'squash' | 'rebase';
    commit_title?: string;
    commit_message?: string;
  };

  const result = await githubFetch(
    req,
    `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/pulls/${encodeURIComponent(number)}/merge`,
    {
      method: 'PUT',
      requireAuth: true,
      body: {
        merge_method: merge_method || 'merge',
        ...(commit_title ? { commit_title } : {}),
        ...(commit_message ? { commit_message } : {}),
      },
    }
  );

  res.status(result.status).json(result.data);
});
