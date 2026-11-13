import { ArrowUp, ShieldCheck, Sprout } from "lucide-react";
import { Brand } from "./UI.jsx";
import { followLink } from "../lib/navigation.js";

export default function SiteFooter({ session, compact = false }) {
  const customer = session?.roles.includes("CUSTOMER");
  const links = session
    ? customer
      ? [
          ["Overview", "/overview"],
          ["My accounts", "/accounts"],
          ["My profile", "/profile"],
        ]
      : [
          ["Customers", "/customers"],
          ["Open an account", "/accounts/new"],
          ...(session.roles.includes("ADMIN")
            ? [["Create staff", "/staff/new"]]
            : []),
        ]
    : [
        ["Welcome", "/"],
        ["Sign in", "/login"],
        ["Create profile", "/signup"],
      ];
  return (
    <footer className={`bank-footer ${compact ? "bank-footer-compact" : ""}`}>
      <div className="bank-footer-top">
        <div className="bank-footer-brand">
          <Brand />
          <p>
            A little clarity today.
            <br />
            More possibility tomorrow.
          </p>
          <span>
            <Sprout size={14} /> Made for your everyday.
          </span>
        </div>
        <nav aria-label="Footer navigation">
          <h2>Your banking</h2>
          {links.map(([label, path]) => (
            <a
              key={path}
              href={path}
              onClick={(event) => followLink(event, path)}
            >
              {label}
            </a>
          ))}
        </nav>
        <div className="bank-footer-help">
          <h2>A little guidance</h2>
          <details>
            <summary>Need a hand?</summary>
            <p>
              Contact a bank representative for help with account opening or
              changes to your profile.
            </p>
          </details>
          <details>
            <summary>Keep your space private</summary>
            <p>
              Use the dashboard’s privacy controls to hide balances and your
              credit score. Sign out when you finish using a shared device.
            </p>
          </details>
        </div>
      </div>
      <div className="bank-footer-bottom">
        <span>© {new Date().getFullYear()} BankingApp</span>
        <span>
          <ShieldCheck size={14} /> Your money deserves a little care.
        </span>
        <button
          type="button"
          onClick={() =>
            window.scrollTo({
              top: 0,
              behavior: window.matchMedia("(prefers-reduced-motion: reduce)")
                .matches
                ? "instant"
                : "smooth",
            })
          }
        >
          Back to top <ArrowUp size={14} />
        </button>
      </div>
    </footer>
  );
}
