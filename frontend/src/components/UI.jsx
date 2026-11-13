import {
  ArrowUpRight,
  Landmark,
  LoaderCircle,
  ShieldCheck,
} from "lucide-react";

export function Brand() {
  return (
    <div className="brand">
      <span className="brand-icon">
        <Landmark size={22} />
      </span>
      banking<span className="brand-dot">.</span>
    </div>
  );
}

export function Notice({ children, success = false }) {
  return children ? (
    <div className={`notice ${success ? "notice-success" : ""}`} role="status">
      {children}
    </div>
  ) : null;
}

export function Field({ label, name, children, ...props }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children || <input name={name} required {...props} />}
    </label>
  );
}

export function Loading() {
  return (
    <div className="loading" role="status">
      <LoaderCircle className="spin" /> Bringing everything together…
    </div>
  );
}

export function PageHeading({
  eyebrow = "YOUR EVERYDAY, IN FOCUS",
  title,
  children,
}) {
  return (
    <div className="page-heading">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="muted">{children}</p>
      </div>
      <span className="date-label">
        {new Date().toLocaleDateString("en-US", {
          month: "short",
          day: "numeric",
          year: "numeric",
        })}
      </span>
    </div>
  );
}

export function DataRow({ label, value }) {
  return (
    <div className="data-row">
      <span>{label}</span>
      <strong>{value ?? "Not available yet"}</strong>
    </div>
  );
}

export function EmptyAccounts() {
  return (
    <div className="empty-state">
      <span className="soft-icon">
        <Landmark />
      </span>
      <h3>A fresh start. A world of possibility.</h3>
      <p>
        Your profile is ready. A bank representative can help you open your
        first account.
      </p>
    </div>
  );
}

export function AccountCard({ account, onClick, formatMoney, formatName }) {
  return (
    <button
      className={`account-card account-${account.accountType?.toLowerCase()}`}
      onClick={onClick}
    >
      <div className="card-top">
        <span className="soft-icon">
          <Landmark size={20} />
        </span>
        <ArrowUpRight size={19} />
      </div>
      <h3>{formatName(account.accountType)}</h3>
      <p className="muted">•••• {String(account.accountNumber).slice(-4)}</p>
      <strong className="account-amount">
        {formatMoney(account.availableBalance)}
      </strong>
      <span className="caption">Available balance</span>
      <div className="card-bottom">
        <span>View account</span>
        <span className="status-dot" />
      </div>
    </button>
  );
}

export function SecurityNote() {
  return (
    <div className="security-note">
      <ShieldCheck size={17} />
      <span>Your account. Your private space.</span>
    </div>
  );
}
