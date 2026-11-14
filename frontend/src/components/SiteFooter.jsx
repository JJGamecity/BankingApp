import { ArrowUp, ShieldCheck, Sprout } from "lucide-react";
import { Brand } from "./UI.jsx";
import { followLink } from "../lib/navigation.js";
import { SAVINGS_RATE_LABEL } from "../lib/savings.js";
import "./site-footer.css";

export default function SiteFooter({ session, compact = false, landing = false }) {
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
            A clearer view of your everyday money, with a little more room for
            the plans ahead. Explore your accounts and take your next step at
            your own pace.
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
        {!compact && <div className="bank-footer-explore">
          <h2>{session ? "Make the most of your space" : "Explore JJBanking"}</h2>
          {!session ? <>
            <a href={landing ? "#accounts" : "/#accounts"}>Compare account types</a>
            <a href={landing ? "#possibilities" : "/#possibilities"}>Try the savings planner</a>
            <p>Start with a profile. A representative can then explain account options and help you open an account.</p>
          </> : <p>{customer ? "Use Money Compass to explore your account balances, and open an account card to see its details." : "Use the workspace tabs to find customer records and access the forms available to your role."}</p>}
        </div>}
        <div className="bank-footer-help">
          <h2>A little guidance</h2>
          <details>
            <summary>How do I open an account?</summary>
            <p>
              Create your customer profile first, then contact a bank
              representative to discuss account options and terms. Creating a
              profile does not automatically open a bank account.
            </p>
          </details>
          <details>
            <summary>Keep your space private</summary>
            <p>
              Use the dashboard’s privacy controls to hide balances on your
              screen. Keep your login details private and sign out when you
              finish using a shared device.
            </p>
          </details>
          <details><summary>How does the savings estimate work?</summary><p>The planner uses an illustrative {SAVINGS_RATE_LABEL} annual interest rate, monthly compounding, and deposits at the end of each month. Fees and taxes are excluded. It does not move money or confirm an account rate.</p></details>
        </div>
      </div>
      {!compact && <div className="bank-footer-note"><ShieldCheck size={17} /><p>Have a question about your account or profile? Contact a bank representative. Account terms and eligibility should be confirmed before opening an account.</p></div>}
      <div className="bank-footer-bottom">
        <span>© {new Date().getFullYear()} JJBanking</span>
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
