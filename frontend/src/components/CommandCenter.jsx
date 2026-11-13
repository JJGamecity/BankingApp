import { useEffect, useRef, useState } from "react";
import {
  Search,
  Command,
  ArrowUpRight,
  Wallet,
  X,
  ShieldCheck,
  LoaderCircle,
} from "lucide-react";
import { request } from "../lib/api.js";
import { bankingCommands, searchCommands } from "../lib/commands.js";
import "./command-center.css";

export default function CommandCenter({
  session,
  onNavigate,
  onAccountSelect,
  onExpired,
}) {
  const [open, setOpen] = useState(false);
  useEffect(() => {
    function shortcut(event) {
      if (
        (event.ctrlKey || event.metaKey) &&
        event.key.toLowerCase() === "k" &&
        !event.altKey
      ) {
        event.preventDefault();
        setOpen((value) => !value);
      }
    }
    window.addEventListener("keydown", shortcut);
    return () => window.removeEventListener("keydown", shortcut);
  }, []);
  return (
    <>
      <button
        className="command-launcher"
        type="button"
        onClick={() => setOpen(true)}
        aria-label="Open banking command center"
        aria-haspopup="dialog"
      >
        <Search size={17} />
        <span>Find anything</span>
        <kbd>⌘ / Ctrl K</kbd>
      </button>
      {open && (
        <CommandDialog
          session={session}
          onClose={() => setOpen(false)}
          onNavigate={onNavigate}
          onAccountSelect={onAccountSelect}
          onExpired={onExpired}
        />
      )}
    </>
  );
}

function CommandDialog({
  session,
  onClose,
  onNavigate,
  onAccountSelect,
  onExpired,
}) {
  const dialog = useRef(null);
  const input = useRef(null);
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const [accounts, setAccounts] = useState([]);
  const [status, setStatus] = useState(
    session.roles.includes("CUSTOMER") ? "loading" : "ready",
  );
  const [attempt, setAttempt] = useState(0);
  const customer = session.roles.includes("CUSTOMER");
  const results = searchCommands(
    bankingCommands(session.roles, accounts),
    query,
  );
  const activeIndex = Math.min(active, Math.max(0, results.length - 1));

  useEffect(() => {
    const previous = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    const element = dialog.current;
    element.showModal();
    input.current.focus();
    document.body.style.overflow = "hidden";
    return () => {
      element.close();
      document.body.style.overflow = previousOverflow;
      if (previous?.isConnected) previous.focus();
    };
  }, []);

  useEffect(() => {
    if (!customer) return;
    const controller = new AbortController();
    setStatus("loading");
    request("/accounts/me", { signal: controller.signal })
      .then((data) => {
        if (controller.signal.aborted) return;
        // Keep only the fields needed for search in this short-lived dialog.
        setAccounts(
          data.map(({ id, accountType, accountNumber }) => ({
            id,
            accountType,
            accountNumber: String(accountNumber).slice(-4),
          })),
        );
        setStatus("ready");
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        if (error.status === 401) onExpired();
        else setStatus("error");
      });
    return () => controller.abort();
  }, [customer, attempt, onExpired]);

  useEffect(() => {
    document
      .getElementById(`bank-command-${activeIndex}`)
      ?.scrollIntoView({ block: "nearest" });
  }, [activeIndex]);

  function choose(result) {
    if (!result) return;
    onClose();
    if (result.accountId !== undefined) onAccountSelect(result.accountId);
    else onNavigate(result.page);
  }

  return (
    <dialog
      ref={dialog}
      className="command-dialog"
      aria-labelledby="command-title"
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        if (event.target !== dialog.current) return;
        const bounds = dialog.current.getBoundingClientRect();
        if (
          event.clientX < bounds.left ||
          event.clientX > bounds.right ||
          event.clientY < bounds.top ||
          event.clientY > bounds.bottom
        )
          onClose();
      }}
    >
      <header className="command-header">
        <span className="command-emblem">
          <Command size={21} />
        </span>
        <div>
          <p>LESS SEARCHING. MORE DOING.</p>
          <h2 id="command-title">Your banking, one shortcut away.</h2>
        </div>
        <button
          type="button"
          aria-label="Close command center"
          onClick={onClose}
        >
          <X size={20} />
        </button>
      </header>
      <div className="command-search">
        <Search size={21} />
        <input
          ref={input}
          value={query}
          placeholder={
            customer
              ? "Try savings, credit score, or the last 4 digits…"
              : "Try customers or open an account…"
          }
          aria-label="Search banking pages and accounts"
          role="combobox"
          aria-autocomplete="list"
          aria-expanded="true"
          aria-controls="bank-command-results"
          aria-activedescendant={
            results.length ? `bank-command-${activeIndex}` : undefined
          }
          onChange={(event) => {
            setQuery(event.target.value);
            setActive(0);
          }}
          onKeyDown={(event) => {
            if (event.key === "ArrowDown" || event.key === "ArrowUp") {
              event.preventDefault();
              if (results.length)
                setActive(
                  (activeIndex +
                    (event.key === "ArrowDown" ? 1 : -1) +
                    results.length) %
                    results.length,
                );
            } else if (event.key === "Enter") {
              event.preventDefault();
              choose(results[activeIndex]);
            }
          }}
        />
        <kbd>ESC</kbd>
      </div>
      <div className="command-meta">
        <span>{query ? "SEARCH RESULTS" : "QUICK JUMPS"}</span>
        <span aria-live="polite">
          {results.length} {results.length === 1 ? "result" : "results"}
        </span>
      </div>
      <div
        id="bank-command-results"
        className="command-results"
        role="listbox"
        aria-label="Banking destinations"
      >
        {results.map((result, index) => (
          <div
            id={`bank-command-${index}`}
            className="command-result"
            key={result.id}
            role="option"
            aria-selected={activeIndex === index}
            onMouseMove={() => setActive(index)}
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => choose(result)}
          >
            <span className="command-result-icon">
              {result.accountId !== undefined ? (
                <Wallet size={20} />
              ) : (
                <ArrowUpRight size={20} />
              )}
            </span>
            <span>
              <strong>{result.title}</strong>
              <small>{result.detail}</small>
            </span>
            <span className="command-result-group">{result.group}</span>
            <ArrowUpRight className="command-result-arrow" size={16} />
          </div>
        ))}
      </div>
      {!results.length && (
        <div className="command-empty">
          <Search size={27} />
          <h3>No matches yet.</h3>
          <p>
            Try a page name
            {customer
              ? ", account type, or the last four account digits"
              : " or a task"}
            .
          </p>
          <button
            onClick={() => {
              setQuery("");
              setActive(0);
              input.current.focus();
            }}
          >
            Clear search
          </button>
        </div>
      )}
      {status === "loading" && (
        <p className="command-status" role="status">
          <LoaderCircle size={14} className="spin" /> Loading your accounts.
          Page shortcuts are ready.
        </p>
      )}
      {status === "error" && (
        <p className="command-status" role="status">
          Account search is unavailable.{" "}
          <button onClick={() => setAttempt((value) => value + 1)}>
            Try again
          </button>
        </p>
      )}
      {customer && status === "ready" && accounts.length === 0 && (
        <p className="command-status">
          No accounts yet. Your workspace shortcuts are ready.
        </p>
      )}
      <footer className="command-footer">
        <span>
          <ShieldCheck size={15} /> Balances stay out of search.
        </span>
        <span>
          <kbd>↑</kbd>
          <kbd>↓</kbd> to browse <kbd>Enter</kbd> to open
        </span>
      </footer>
    </dialog>
  );
}
