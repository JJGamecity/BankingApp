import {
  LayoutDashboard,
  Wallet,
  UserRound,
  Users,
  Plus,
  UserPlus,
  LogOut,
} from "lucide-react";
import { Brand } from "./UI.jsx";
import { paths, followLink } from "../lib/navigation.js";

export default function Layout({
  session,
  page,
  onNavigate,
  onLogout,
  busy,
  children,
}) {
  const customer = session.roles.includes("CUSTOMER");
  const navigation = customer
    ? [
        ["overview", LayoutDashboard, "Overview"],
        ["accounts", Wallet, "My accounts"],
        ["profile", UserRound, "My profile"],
      ]
    : [
        ["overview", Users, "Customers"],
        ["open", Plus, "Open an account"],
        ...(session.roles.includes("ADMIN")
          ? [["staff", UserPlus, "Create staff"]]
          : []),
      ];

  return (
    <div className="app-layout">
      <header className="sidebar portal-header">
        <Brand />
        <nav aria-label="Main navigation">
          {navigation.map(([key, Icon, label]) => (
            <a
              href={
                key === "overview" && !customer ? paths.customers : paths[key]
              }
              key={key}
              className={`nav-item ${page === key ? "active" : ""}`}
              aria-current={page === key ? "page" : undefined}
              onClick={(event) =>
                followLink(
                  event,
                  key === "overview" && !customer
                    ? paths.customers
                    : paths[key],
                )
              }
            >
              <Icon size={19} />
              {label}
              {page === key && <span className="nav-dot" />}
            </a>
          ))}
        </nav>
        <div className="sidebar-profile">
          <span className="avatar">{session.username?.[0]?.toUpperCase()}</span>
          <div>
            <strong>{session.username}</strong>
            <span>{customer ? "Personal banking" : "Staff workspace"}</span>
          </div>
        </div>
      </header>
      <div className="main-column">
        <header className="topbar">
          <div>
            <span className="breadcrumb">Workspace</span>
            <span className="breadcrumb-divider">/</span>
            <strong>{navigation.find(([key]) => key === page)?.[2]}</strong>
          </div>
          <button className="signout" disabled={busy} onClick={onLogout}>
            <LogOut size={17} />
            {busy ? "Signing out…" : "Sign out"}
          </button>
        </header>
        <main className="workspace">
          {children}
          <footer className="workspace-footer">
            <span>
              banking. <span className="muted">A little more possibility.</span>
            </span>
            <span>Made for your everyday ↗</span>
          </footer>
        </main>
      </div>
    </div>
  );
}
