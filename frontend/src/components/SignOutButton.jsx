import { LogOut, LoaderCircle, ArrowUpRight } from "lucide-react";

export default function SignOutButton({ busy, onLogout }) {
  return (
    <button
      className="session-exit"
      disabled={busy}
      onClick={onLogout}
      aria-label={busy ? "Signing out" : "Sign out"}
      aria-busy={busy}
    >
      <span className="session-exit-icon">
        {busy ? (
          <LoaderCircle className="spin" size={19} />
        ) : (
          <LogOut size={19} />
        )}
      </span>
      <span className="session-exit-copy">
        <strong>{busy ? "Signing out…" : "Sign out"}</strong>
        <small>{busy ? "Just a moment" : "See you next time"}</small>
      </span>
      <ArrowUpRight className="session-exit-arrow" size={16} />
    </button>
  );
}
