export type AuthStatus = 'loading' | 'unauthenticated' | 'authenticated';

export interface GitHubUser {
  id: number;
  login: string;
  name: string;
  email: string | null;
  avatar_url: string;
  html_url: string;
  bio: string | null;
  company: string | null;
  location: string | null;
  public_repos: number;
  total_private_repos: number;
  followers: number;
  following: number;
  created_at: string;
}

export interface SessionInfo {
  scopes: string[];
  tokenType: string;
  createdAt: number;
}

export interface SessionApiResponse {
  authenticated: boolean;
  configured: boolean;
  callbackUrl: string;
  user: GitHubUser | null;
  session: SessionInfo | null;
  reason?: string;
}

export interface GitHubRepo {
  id: number;
  name: string;
  full_name: string;
  private: boolean;
  description: string | null;
  language: string | null;
  stargazers_count: number;
  default_branch: string;
  updated_at: string;
  html_url: string;
  owner: {
    login: string;
  };
}

export interface RepoEntry {
  name: string;
  path: string;
  type: 'dir' | 'file' | 'symlink' | 'submodule';
  size: number;
  sha: string;
}

export interface RepoFile {
  name: string;
  path: string;
  sha: string;
  size: number;
  content: string;
  html_url: string;
}
