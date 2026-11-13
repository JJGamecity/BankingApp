import { useState } from "react";
import { Compass, ArrowUpRight, EyeOff } from "lucide-react";
import { money, accountName } from "../lib/api.js";
import { accountAllocation } from "../lib/compass.js";
import "./money-compass.css";

const colors = [
  "#147d80",
  "#51b7aa",
  "#77a9dc",
  "#426e9d",
  "#a299c6",
  "#c79262",
];

export default function MoneyCompass({ accounts, hidden, onSelect }) {
  const [selectedId, setSelectedId] = useState(null);
  const { rows, positiveTotal, incomplete, hasNegative } =
    accountAllocation(accounts);
  const selected = rows.find((row) => row.id === selectedId);
  let cursor = 0;
  const segments = rows.map((row, index) => {
    const start = cursor;
    cursor += row.share * 100;
    return `${colors[index % colors.length]} ${start}% ${cursor}%`;
  });
  return (
    <section className="money-compass" aria-labelledby="compass-title">
      <header className="compass-heading">
        <div className="compass-icon">
          <Compass size={23} />
        </div>
        <div>
          <p className="eyebrow">A NEW PERSPECTIVE</p>
          <h2 id="compass-title">Your Money Compass</h2>
        </div>
        <span className="compass-badge">YOUR MONEY, MAPPED</span>
      </header>
      <p className="compass-intro">
        A little clarity on where your money lives. Choose an account to bring
        it into focus.
      </p>
      {!rows.length ? (
        <div className="compass-empty">
          <Compass size={38} />
          <h3>Your next chapter starts here.</h3>
          <p>
            Your money map will appear once a representative opens your first
            account.
          </p>
        </div>
      ) : (
        <div className="compass-layout">
          <div className="compass-visual">
            <div
              className="compass-ring"
              aria-hidden="true"
              style={{
                background:
                  hidden || !positiveTotal
                    ? "#dce6ef"
                    : `conic-gradient(${segments.join(",")})`,
              }}
            >
              <div className="compass-center">
                {hidden ? (
                  <>
                    <EyeOff size={25} />
                    <strong>Private view</strong>
                    <span>Balances are hidden</span>
                  </>
                ) : (
                  <>
                    <span>
                      {selected
                        ? accountName(selected.accountType)
                        : "Positive balances"}
                    </span>
                    <strong>
                      {money(selected ? selected.balance : positiveTotal)}
                    </strong>
                    <span>
                      {selected
                        ? `${(selected.share * 100).toFixed(1)}% of mapped funds`
                        : `${rows.length} connected ${rows.length === 1 ? "account" : "accounts"}`}
                    </span>
                  </>
                )}
              </div>
            </div>
            <span className="compass-visual-caption">
              A snapshot of your available balances
            </span>
          </div>
          <div className="compass-accounts">
            <div className="compass-list-heading">
              <span>ACCOUNT BREAKDOWN</span>
              <button
                type="button"
                disabled={!selected}
                onClick={() => setSelectedId(null)}
              >
                Reset view
              </button>
            </div>
            {rows.map((row, index) => (
              <button
                type="button"
                className="compass-account"
                key={row.id}
                aria-pressed={selectedId === row.id}
                onClick={() =>
                  setSelectedId(selectedId === row.id ? null : row.id)
                }
              >
                <span
                  className="compass-swatch"
                  style={{ background: colors[index % colors.length] }}
                />
                <span className="compass-account-name">
                  <strong>{accountName(row.accountType)}</strong>
                  <small>Ending {String(row.accountNumber).slice(-4)}</small>
                </span>
                <span className="compass-account-value">
                  <strong>{hidden ? "••••••" : money(row.balance)}</strong>
                  <small>
                    {hidden
                      ? "Hidden"
                      : row.balance === null
                        ? "Unavailable"
                        : `${(row.share * 100).toFixed(1)}%`}
                  </small>
                </span>
              </button>
            ))}
            <div className="compass-selection" aria-live="polite">
              {selected ? (
                <>
                  <p>
                    {accountName(selected.accountType)} ending{" "}
                    {String(selected.accountNumber).slice(-4)} selected.
                  </p>
                  <button
                    className="text-button"
                    onClick={() =>
                      onSelect(
                        accounts.find((account) => account.id === selected.id),
                      )
                    }
                  >
                    View account details <ArrowUpRight size={16} />
                  </button>
                </>
              ) : (
                <p>
                  Choose any account above to explore its place in your money
                  map.
                </p>
              )}
            </div>
          </div>
        </div>
      )}
      <footer className="compass-footnote">
        {hidden
          ? "Privacy view is on. Amounts and allocation proportions are hidden."
          : "The map shows each account’s share of positive available balances."}
        {!hidden &&
          hasNegative &&
          " Negative balances are listed but excluded from the ring."}
        {!hidden &&
          incomplete &&
          " Some balances are unavailable; the map is partial."}{" "}
        No money is moved.
      </footer>
    </section>
  );
}
