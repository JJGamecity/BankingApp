import {
  LayoutDashboard,
  Wallet,
  UserRound,
  Users,
  Plus,
  UserPlus,
} from "lucide-react";
import { Brand } from "./UI.jsx";
import SignOutButton from "./SignOutButton.jsx";
import SiteFooter from "./SiteFooter.jsx";
import CommandCenter from "./CommandCenter.jsx";
import { paths, followLink } from "../lib/navigation.js";

export default function Layout({
  session,
  page,
  onNavigate,
  onLogout,
  onAccountSelect,
  onExpired,
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
      <header className="portal-header">
        <Brand />
        <nav className="portal-tabs" aria-label="Main navigation">
          {navigation.map(([key, Icon, label]) => (
            <a
              href={
                key === "overview" && !customer ? paths.customers : paths[key]
              }
              key={key}
              className={`portal-tab ${page === key ? "active" : ""}`}
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
            </a>
          ))}
        </nav>
        <div className="portal-profile">
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
          <div className="topbar-actions">
            <CommandCenter
              session={session}
              onNavigate={onNavigate}
              onAccountSelect={onAccountSelect}
              onExpired={onExpired}
            />
            <SignOutButton busy={busy} onLogout={onLogout} />
          </div>
        </header>
        <main className="workspace">
          {children}
          <SiteFooter session={session} />
        </main>
      </div>
    </div>
  );
}
