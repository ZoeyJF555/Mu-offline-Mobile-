import { useState } from 'react';
import { Check, Copy, ExternalLink, KeyRound, ShieldCheck, AlertTriangle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export function OAuthSetupPanel() {
  const { configured, callbackUrl } = useAuth();
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(callbackUrl);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  return (
    <section className="bg-[#111827] border border-slate-800 rounded-xl p-6 flex flex-col justify-between gap-6">
      <div className="space-y-5">
        <div className="flex items-start justify-between gap-4 border-b border-slate-800 pb-4">
          <div>
            <h2 className="text-base font-semibold text-slate-100">
              OAuth Configuration &amp; Session Policy
            </h2>
            <p className="text-xs text-slate-400 mt-1">
              Popup authorization flow with AES-256-GCM encrypted server cookies.
            </p>
          </div>
          <span
            className={`inline-flex items-center gap-1.5 text-xs font-medium whitespace-nowrap shrink-0 ${
              configured ? 'text-emerald-400' : 'text-amber-400'
            }`}
          >
            {configured ? (
              <>
                <ShieldCheck className="w-4 h-4" />
                Credentials active
              </>
            ) : (
              <>
                <AlertTriangle className="w-4 h-4" />
                Setup required
              </>
            )}
          </span>
        </div>

        <div className="space-y-4 text-sm">
          <div>
            <div className="text-xs font-medium text-slate-400 mb-1.5">
              Authorized Callback URL
            </div>
            <div className="flex items-center gap-2 bg-[#0B0F17] border border-slate-800 rounded-lg px-3 py-2">
              <code className="text-xs font-mono text-slate-200 truncate flex-1">
                {callbackUrl}
              </code>
              <button
                type="button"
                onClick={() => void handleCopy()}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-slate-300 hover:text-white bg-slate-800/80 hover:bg-slate-700 rounded transition-colors shrink-0 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
              >
                {copied ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                    Copied
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    Copy
                  </>
                )}
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
            <div className="border-t border-slate-800/80 pt-3">
              <div className="text-xs text-slate-400">Requested Scopes</div>
              <div className="text-xs font-mono text-slate-200 mt-1">
                repo · read:user · user:email
              </div>
            </div>
            <div className="border-t border-slate-800/80 pt-3">
              <div className="text-xs text-slate-400">Cookie Security</div>
              <div className="text-xs font-mono text-slate-200 mt-1">
                HttpOnly · SameSite=None · Secure
              </div>
            </div>
          </div>

          <div className="border-t border-slate-800/80 pt-4 space-y-2.5">
            <div className="flex items-center gap-2 text-xs font-medium text-slate-200">
              <KeyRound className="w-3.5 h-3.5 text-blue-400" />
              GitHub OAuth App Setup Steps
            </div>
            <ol className="text-xs text-slate-400 space-y-1.5 list-decimal list-inside leading-relaxed">
              <li>
                Open{' '}
                <a
                  href="https://github.com/settings/developers"
                  target="_blank"
                  rel="noreferrer"
                  className="text-blue-400 hover:text-blue-300 inline-flex items-center gap-1 underline underline-offset-2"
                >
                  GitHub Developer Settings
                  <ExternalLink className="w-3 h-3" />
                </a>{' '}
                and create or select an OAuth App.
              </li>
              <li>
                Set the Authorization callback URL to the exact URI shown above.
              </li>
              <li>
                Add <code className="font-mono text-slate-200">GITHUB_CLIENT_ID</code>{' '}
                and{' '}
                <code className="font-mono text-slate-200">
                  GITHUB_CLIENT_SECRET
                </code>{' '}
                in your AI Studio Secrets panel.
              </li>
            </ol>
          </div>
        </div>
      </div>
    </section>
  );
}
