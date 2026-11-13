import { useState } from "react";
import {
  ArrowUpRight,
  ArrowRight,
  Wallet,
  Sprout,
  Layers3,
  ShieldCheck,
  Menu,
  X,
  Check,
  Wifi,
} from "lucide-react";
import { Brand } from "../components/UI.jsx";
import { followLink } from "../lib/navigation.js";
import "./welcome.css";

const accounts = [
  {
    name: "Checking",
    icon: Wallet,
    tag: "FOR THE EVERYDAY",
    title: "Life happens. Stay in step.",
    text: "Keep everyday money in focus with a clear view of your balance and account details.",
    image: "/images/everyday-banking.svg",
    alt: "Illustration of a banking card and a contactless payment terminal",
    features: [
      "Your everyday balance at a glance",
      "Account details in one place",
      "A simple, connected dashboard",
    ],
  },
  {
    name: "Savings",
    icon: Sprout,
    tag: "FOR YOUR NEXT CHAPTER",
    title: "Small steps. Bigger possibilities.",
    text: "Give your next adventure, a new home, or a little peace of mind a place to grow.",
    image: "/images/green-architecture.jpg",
    alt: "Green planted balconies on a modern building",
    features: [
      "A dedicated space for savings",
      "An overview of your balances",
      "More clarity for your future plans",
    ],
  },
  {
    name: "Money market",
    icon: Layers3,
    tag: "FOR THE BIGGER PICTURE",
    title: "Make room for more.",
    text: "Explore another way to organize your money. A bank representative can help you find your fit.",
    image: "/images/alpine-lake.jpg",
    alt: "Mountain lake framed by green alpine forests",
    features: [
      "Explore your account options",
      "Keep your finances together",
      "Find a fit with a representative",
    ],
  },
];
const money = (value) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
  }).format(value);
function AppLink({ to, children, className = "" }) {
  return (
    <a
      className={className}
      href={to}
      onClick={(event) => followLink(event, to)}
    >
      {children}
    </a>
  );
}

export default function WelcomePage() {
  const [active, setActive] = useState(0);
  const [monthly, setMonthly] = useState(250);
  const [months, setMonths] = useState(12);
  const [menu, setMenu] = useState(false);
  const account = accounts[active];
  return (
    <div className="bank-home">
      <header className="home-header">
        <a href="#" className="home-brand" aria-label="Banking home">
          <Brand />
        </a>
        <button
          className="home-menu"
          aria-label={menu ? "Close navigation" : "Open navigation"}
          aria-expanded={menu}
          aria-controls="home-nav"
          onClick={() => setMenu(!menu)}
        >
          {menu ? <X /> : <Menu />}
        </button>
        <nav
          id="home-nav"
          className={menu ? "is-open" : ""}
          aria-label="Welcome navigation"
        >
          <a href="#accounts" onClick={() => setMenu(false)}>
            Our accounts
          </a>
          <a href="#possibilities" onClick={() => setMenu(false)}>
            Your possibilities
          </a>
          <AppLink to="/login">Sign in</AppLink>
          <AppLink className="home-button home-button-dark" to="/signup">
            Get started <ArrowUpRight size={17} />
          </AppLink>
        </nav>
      </header>
      <main>
        <section className="home-hero">
          <div className="home-hero-copy">
            <span className="home-eyebrow">
              <i /> A FRESH PERSPECTIVE ON BANKING
            </span>
            <h1>
              Your money.
              <br />
              Your life.
              <br />
              <em>More possibility.</em>
            </h1>
            <p>
              For the everyday moments and the someday plans. A simpler way to
              feel at home with your money.
            </p>
            <div className="home-actions">
              <AppLink className="home-button home-button-dark" to="/signup">
                Find your fresh start <ArrowUpRight size={19} />
              </AppLink>
              <a className="home-text-link" href="#accounts">
                Explore accounts <ArrowRight size={17} />
              </a>
            </div>
            <div className="home-assurance">
              <ShieldCheck size={20} />
              <span>Your own space. A clearer financial picture.</span>
            </div>
          </div>
          <div className="home-hero-art">
            <img
              className="home-hero-photo"
              src="/images/green-architecture.jpg"
              alt="Sunlit architecture with greenery, representing room to grow"
              fetchPriority="high"
            />
            <div className="home-photo-label">
              A LITTLE ROOM TO GROW <ArrowUpRight size={17} />
            </div>
            <div className="home-orbit" />
            <div className="home-bank-card">
              <div>
                <strong>banking.</strong>
                <Wifi size={25} />
              </div>
              <span className="home-card-chip" />
              <p>•••• &nbsp; •••• &nbsp; •••• &nbsp; 2048</p>
              <div>
                <span>EVERYDAY POSSIBILITIES</span>
                <span className="home-card-circles">◯◯</span>
              </div>
            </div>
            <div className="home-art-note">
              <span>
                <Sprout size={24} />
              </span>
              <div>
                Built around you.<strong>And what comes next.</strong>
              </div>
            </div>
            <span className="home-art-caption">
              Card illustration · A fresh outlook
            </span>
          </div>
        </section>
        <section className="home-feature-strip" aria-label="Banking features">
          {[
            [Wallet, "All together.", "Your accounts, in one clear view."],
            [
              ShieldCheck,
              "Feel at home.",
              "A personal space for your banking.",
            ],
            [Sprout, "Look ahead.", "More room for your next chapter."],
          ].map(([Icon, title, text]) => (
            <div key={title}>
              <Icon size={25} />
              <div>
                <h2>{title}</h2>
                <p>{text}</p>
              </div>
            </div>
          ))}
        </section>
        <section id="accounts" className="home-accounts home-section">
          <div className="home-section-heading">
            <div>
              <span className="home-eyebrow">
                GOOD THINGS START WITH A LITTLE CLARITY
              </span>
              <h2>
                An account for
                <br />
                <em>your kind of everyday.</em>
              </h2>
            </div>
            <p>
              Different plans. The same thoughtful approach.
              <br />
              Find a place for what matters to you.
            </p>
          </div>
          <div
            className="home-account-tabs"
            role="tablist"
            aria-label="Explore account types"
          >
            {accounts.map((item, index) => (
              <button
                key={item.name}
                id={`account-tab-${index}`}
                role="tab"
                aria-selected={active === index}
                aria-controls="account-preview"
                tabIndex={active === index ? 0 : -1}
                onKeyDown={(event) => {
                  const next = {
                    ArrowRight: (index + 1) % accounts.length,
                    ArrowLeft: (index + accounts.length - 1) % accounts.length,
                    Home: 0,
                    End: accounts.length - 1,
                  }[event.key];
                  if (next === undefined) return;
                  event.preventDefault();
                  setActive(next);
                  document.getElementById(`account-tab-${next}`)?.focus();
                }}
                onClick={() => setActive(index)}
                className={active === index ? "selected" : ""}
              >
                <item.icon size={19} />
                {item.name}
                <ArrowUpRight size={16} />
              </button>
            ))}
          </div>
          <div
            className="home-account-preview"
            id="account-preview"
            role="tabpanel"
            tabIndex={0}
            aria-labelledby={`account-tab-${active}`}
          >
            <div className="home-account-copy">
              <span className="home-eyebrow">{account.tag}</span>
              <h3>{account.title}</h3>
              <p>{account.text}</p>
              <ul>
                {account.features.map((feature) => (
                  <li key={feature}>
                    <Check size={17} />
                    {feature}
                  </li>
                ))}
              </ul>
              <AppLink className="home-text-link" to="/signup">
                Create your profile <ArrowUpRight size={18} />
              </AppLink>
              <small>
                Contact a bank representative to open an account after signup.
              </small>
            </div>
            <img
              key={account.image}
              src={account.image}
              alt={account.alt}
              loading="lazy"
            />
          </div>
        </section>
        <section id="possibilities" className="home-planner home-section">
          <div className="home-planner-story">
            <img
              src="/images/alpine-lake.jpg"
              alt="A quiet mountain lake, inspiration for your next adventure"
              loading="lazy"
            />
            <div>
              <span className="home-eyebrow">FROM SOMEDAY TO A PLAN</span>
              <h2>
                Big dreams.
                <br />
                <em>Little by little.</em>
              </h2>
              <p>That next adventure starts with a little intention.</p>
            </div>
          </div>
          <div className="home-planner-controls">
            <span className="home-eyebrow">YOUR POSSIBILITY PLANNER</span>
            <h3>See what adds up.</h3>
            <p>A small habit today. Something to look forward to.</p>
            <label htmlFor="monthly-saving">
              Put aside each month <strong>{money(monthly)}</strong>
            </label>
            <input
              id="monthly-saving"
              type="range"
              min="25"
              max="1500"
              step="25"
              value={monthly}
              onChange={(event) => setMonthly(Number(event.target.value))}
            />
            <div className="home-range-labels">
              <span>$25</span>
              <span>$1,500</span>
            </div>
            <span className="home-period-label">Give it a little time</span>
            <div className="home-periods">
              {[6, 12, 24].map((value) => (
                <button
                  key={value}
                  aria-pressed={months === value}
                  onClick={() => setMonths(value)}
                >
                  {value} months
                </button>
              ))}
            </div>
            <div className="home-projection" aria-live="polite">
              <span>You could set aside</span>
              <strong>
                {money(monthly * months)}
                <Sprout size={30} />
              </strong>
            </div>
            <small>
              Planning illustration only. Monthly contributions × months; no
              interest, fees, or account terms included. This does not move
              money.
            </small>
          </div>
        </section>
        <section className="home-cta">
          <div>
            <span className="home-eyebrow">HERE'S TO WHAT COMES NEXT</span>
            <h2>
              A fresh start.
              <br />
              <em>A little more you.</em>
            </h2>
          </div>
          <AppLink className="home-button" to="/signup">
            Let's get started <ArrowUpRight size={20} />
          </AppLink>
        </section>
      </main>
      <footer className="home-footer">
        <Brand />
        <span>A simpler relationship with your money.</span>
        <a href="#">Back to top ↑</a>
      </footer>
    </div>
  );
}
