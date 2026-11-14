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
} from "lucide-react";
import { Brand } from "../components/UI.jsx";
import BankingShowcase from "../components/BankingShowcase.jsx";
import HeroRocket from "../components/HeroRocket.jsx";
import SiteFooter from "../components/SiteFooter.jsx";
import { followLink } from "../lib/navigation.js";
import { projectSavings, SAVINGS_RATE_LABEL } from "../lib/savings.js";
import "./welcome.css";

const accounts = [
  {
    name: "Checking",
    icon: Wallet,
    tag: "FOR THE EVERYDAY",
    title: "Life happens. Stay in step.",
    text: "Give your everyday money a place that is easy to keep track of. Check your available balance, find your account details, and see how your checking account fits alongside your savings—all from one organized dashboard.",
    image: "/images/everyday-banking.svg?v=jjbanking",
    alt: "Light blue banking card and payment terminal on a pale blue background",
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
    text: "Whether you are planning your next adventure or setting money aside for the unexpected, give those plans a dedicated place in your financial picture. Explore the planner below to see how regular deposits and illustrative interest could add up over time.",
    image: "/images/green-architecture.jpg",
    alt: "Green planted balconies on a modern building",
    features: [
      `${SAVINGS_RATE_LABEL} annual interest in our savings illustration`,
      "An overview of your balances",
      "More clarity for your future plans",
    ],
  },
  {
    name: "Money market",
    icon: Layers3,
    tag: "FOR THE BIGGER PICTURE",
    title: "Make room for more.",
    text: "Bring another part of your financial picture into focus with a money market account. View it alongside your other accounts, then speak with a bank representative about eligibility, rates, and account terms to decide whether it fits your plans.",
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
  const projection = projectSavings(monthly, months);
  const milestones = [
    ...new Set(
      [0.25, 0.5, 0.75, 1].map((fraction) =>
        Math.max(1, Math.round(months * fraction)),
      ),
    ),
  ];
  return (
    <div className="bank-home">
      <HeroRocket />
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
              A new perspective
              <br />
              <em>on your money.</em>
            </h1>
            <p>
              Bring your everyday balances and future plans into one clearer
              picture. Explore your accounts, see where your money sits, and try
              out savings scenarios—all in a space designed to make your next
              step easier to understand.
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
          <BankingShowcase />
        </section>
        <section className="home-feature-strip" aria-label="Banking features">
          {[
            [
              Wallet,
              "All together.",
              "See your available balances side by side, then open an account to find the details you need.",
            ],
            [
              ShieldCheck,
              "Feel at home.",
              "Keep your profile and accounts within reach, with a balance-hiding control for a little more privacy on your screen.",
            ],
            [
              Sprout,
              "Look ahead.",
              "Turn a savings idea into an illustration. Adjust your monthly deposit and timeframe to explore how the numbers change.",
            ],
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
              Everyday spending, money for later, or a broader financial plan:
              explore each account type and find out what to discuss with a bank
              representative before opening one.
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
                Creating a profile is your first step. A bank representative can
                then help you open an account and explain its terms.
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
          <div className="planner-intro">
            <div>
              <span className="home-eyebrow">FROM SOMEDAY TO A PLAN</span>
              <h2>
                Your next chapter. <em>In numbers.</em>
              </h2>
              <p>
                Start with an amount you could put aside each month, then choose
                how long you want to save. The planner separates your deposits
                from estimated interest, helping you see both what you
                contribute and the total you could build under the illustrated
                rate.
              </p>
            </div>
            <img
              className="planner-blue-photo"
              src="/images/blue-city.jpg"
              alt="Blue glass architecture reflecting a clear sky"
              loading="lazy"
              width="180"
              height="140"
            />
          </div>
          <div className="home-planner-controls">
            <div className="planner-inputs">
              <span className="home-eyebrow">YOUR POSSIBILITY PLANNER</span>
              <h3>See what adds up.</h3>
              <p>
                Move either slider to explore a different plan. Your estimated
                interest, total balance, and growth chart update together, so
                you can compare a smaller monthly habit with a longer saving
                period.
              </p>
              <div className="savings-rate">
                <Sprout size={23} />
                <div>
                  <strong>{SAVINGS_RATE_LABEL}</strong>
                  <span>Illustrative annual interest rate</span>
                </div>
                <small>
                  Compounded
                  <br />
                  monthly
                </small>
              </div>
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
                aria-valuetext={`${money(monthly)} per month`}
                onChange={(event) => setMonthly(Number(event.target.value))}
              />
              <div className="home-range-labels">
                <span>$25</span>
                <span>$1,500</span>
              </div>
              <label className="savings-month-label" htmlFor="saving-months">
                Give it a little time{" "}
                <strong>
                  {months} {months === 1 ? "month" : "months"}
                </strong>
              </label>
              <input
                id="saving-months"
                type="range"
                min="1"
                max="24"
                step="1"
                value={months}
                aria-valuetext={`${months} ${months === 1 ? "month" : "months"}`}
                onChange={(event) => setMonths(Number(event.target.value))}
              />
              <div className="home-range-labels">
                <span>1 month</span>
                <span>24 months · max</span>
              </div>
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
            </div>
            <div className="planner-results">
              <div className="home-projection" aria-live="polite">
                <span>You could end up earning</span>
                <strong>
                  {money(projection.interest)}
                  <Sprout size={30} />
                </strong>
                <p className="savings-result-caption">
                  Estimated interest over {months}{" "}
                  {months === 1 ? "month" : "months"}
                </p>
                <div className="savings-breakdown">
                  <span>
                    You put in{" "}
                    <strong>{money(projection.contributions)}</strong>
                  </span>
                  <span>
                    You could end up with{" "}
                    <strong>{money(projection.total)}</strong>
                  </span>
                </div>
              </div>
              <div
                className="savings-growth"
                aria-label="Estimated balance over time"
              >
                <div className="savings-growth-heading">
                  <span>YOUR SAVINGS, TAKING SHAPE</span>
                  <span>Deposits + interest</span>
                </div>
                <div
                  className="savings-growth-bars"
                  style={{
                    gridTemplateColumns: `repeat(${milestones.length}, minmax(0, 1fr))`,
                  }}
                >
                  {milestones.map((month) => (
                    <div key={month}>
                      <strong>
                        {money(projectSavings(monthly, month).total)}
                      </strong>
                      <div className="savings-growth-track">
                        <span
                          style={{
                            height: `${(projectSavings(monthly, month).total / projection.total) * 100}%`,
                          }}
                        />
                      </div>
                      <small>Month {month}</small>
                    </div>
                  ))}
                </div>
              </div>
              <small>
                Illustration assumes a fixed {SAVINGS_RATE_LABEL} annual
                interest rate, compounded monthly, with deposits at each month’s
                end and no starting balance. Values rounded to whole dollars;
                fees and taxes excluded. This is not a confirmed account offer
                and does not move money.
              </small>
            </div>
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
            <p className="home-cta-description">
              Create your customer profile to start exploring your banking
              workspace. Once an account is opened, your balances, account
              details, and Money Compass come together in one place.
            </p>
          </div>
          <AppLink className="home-button" to="/signup">
            Let's get started <ArrowUpRight size={20} />
          </AppLink>
        </section>
      </main>
      <SiteFooter landing />
    </div>
  );
}
