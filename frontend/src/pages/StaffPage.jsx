import { useEffect, useState, useRef } from "react";
import { Search, ArrowRight } from "lucide-react";
import { request, accountName, money } from "../lib/api.js";
import {
  Field,
  Loading,
  Notice,
  PageHeading,
  DataRow,
} from "../components/UI.jsx";

export default function StaffPage({ page, onExpired }) {
  const [users, setUsers] = useState(null);
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState(null);
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);
  const [busy, setBusy] = useState(false);
  const [retry, setRetry] = useState(0);
  const selectionRequest = useRef(0);

  useEffect(() => {
    const controller = new AbortController();
    if (page === "overview") {
      setMessage("");
      setUsers(null);
      request("/users", { signal: controller.signal })
        .then(setUsers)
        .catch((error) => {
          if (error.name === "AbortError") return;
          if (error.status === 401) onExpired();
          else setMessage(error.message);
        });
    }
    return () => {
      controller.abort();
      selectionRequest.current++;
    };
  }, [page, retry, onExpired]);

  async function viewAccounts(user) {
    const version = ++selectionRequest.current;
    setSelected({ user, accounts: null });
    setMessage("");
    try {
      const accounts = await request(`/accounts/user-${user.id}`);
      if (version === selectionRequest.current) setSelected({ user, accounts });
    } catch (error) {
      if (version !== selectionRequest.current) return;
      if (error.status === 401) onExpired();
      else {
        setSelected(null);
        setMessage(error.message);
      }
    }
  }

  async function submit(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const body = Object.fromEntries(new FormData(form));
    if (page === "open") body.userId = Number(body.userId);
    setBusy(true);
    setMessage("");
    setSuccess(false);
    try {
      const result = await request(page === "staff" ? "/staff" : "/accounts", {
        method: "POST",
        body,
      });
      form.reset();
      setSuccess(true);
      setMessage(
        `${page === "staff" ? "Staff login" : "Account"} created. Reference #${result.id}.`,
      );
    } catch (error) {
      if (error.status === 401) onExpired();
      else setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  if (page === "overview") {
    const filtered =
      users?.filter((user) =>
        `${user.firstName} ${user.lastName} ${user.username} ${user.email}`
          .toLowerCase()
          .includes(query.toLowerCase()),
      ) || [];
    return (
      <>
        <PageHeading
          eyebrow="THE PEOPLE BEHIND THE ACCOUNTS"
          title="Customer workspace."
        >
          A personal touch starts with a clear picture.
        </PageHeading>
        <Notice>{message}</Notice>
        {!users ? (
          message ? (
            <button
              className="button button-secondary"
              onClick={() => setRetry(retry + 1)}
            >
              Try again
            </button>
          ) : (
            <Loading />
          )
        ) : (
          <section className="panel">
            <label className="search-field">
              <Search size={18} />
              <input
                type="search"
                aria-label="Search customers"
                placeholder="Search by name, username, or email"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
            </label>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Customer</th>
                    <th>Email</th>
                    <th>Credit score</th>
                    <th>Accounts</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((user) => (
                    <tr key={user.id}>
                      <td>
                        <strong>
                          {user.firstName} {user.lastName}
                        </strong>
                        <span className="table-subtitle">
                          {user.username} · #{user.id}
                        </span>
                      </td>
                      <td>{user.email}</td>
                      <td>{user.creditScore ?? "Not assessed"}</td>
                      <td>
                        <button
                          className="button button-secondary button-small"
                          onClick={() => viewAccounts(user)}
                        >
                          View <ArrowRight size={15} />
                        </button>
                      </td>
                    </tr>
                  ))}
                  {!filtered.length && (
                    <tr>
                      <td colSpan="4">No matching customers.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        )}
        {selected && (
          <section className="panel selected-customer">
            <h2>{selected.user.firstName}’s accounts</h2>
            {!selected.accounts ? (
              <Loading />
            ) : selected.accounts.length ? (
              selected.accounts.map((account) => (
                <DataRow
                  key={account.id}
                  label={`${accountName(account.accountType)} · ${account.accountNumber}`}
                  value={money(account.availableBalance)}
                />
              ))
            ) : (
              <p className="muted">No accounts have been opened yet.</p>
            )}
          </section>
        )}
      </>
    );
  }

  const staff = page === "staff";
  return (
    <>
      <PageHeading title={staff ? "Build your team." : "A new beginning."}>
        {staff
          ? "Create a login for an administrator or bank representative."
          : "Open a bank account for an existing customer."}
      </PageHeading>
      <section className="panel form-panel">
        <Notice success={success}>{message}</Notice>
        <h2>{staff ? "Staff details" : "Account details"}</h2>
        <form onSubmit={submit}>
          <fieldset disabled={busy}>
            {staff ? (
              <>
                <Field
                  label="Username"
                  name="username"
                  minLength="3"
                  maxLength="50"
                  pattern="[A-Za-z0-9._-]+"
                  autoComplete="off"
                />
                <Field
                  label="Password"
                  name="password"
                  type="password"
                  autoComplete="new-password"
                />
                <p className="form-help">
                  10+ characters, a special character, no three-character letter
                  or digit sequences (including spaced sequences), and at most
                  72 UTF-8 bytes.
                </p>
                <Field label="Role">
                  <select name="role">
                    <option value="BANK_REPRESENTATIVE">
                      Bank representative
                    </option>
                    <option value="ADMIN">Administrator</option>
                  </select>
                </Field>
              </>
            ) : (
              <>
                <Field
                  label="Customer ID"
                  name="userId"
                  type="number"
                  min="1"
                  step="1"
                />
                <Field
                  label="Account number"
                  name="accountNumber"
                  maxLength="255"
                />
                <Field label="Account type">
                  <select name="accountType">
                    <option value="CHECKING">Checking</option>
                    <option value="SAVINGS">Savings</option>
                    <option value="MONEY_MARKET">Money market</option>
                  </select>
                </Field>
                <p className="form-help">
                  New accounts start with a zero balance. The bank provides the
                  routing number.
                </p>
              </>
            )}
            <button className="button button-primary">
              {busy ? "Saving…" : staff ? "Create staff login" : "Open account"}
              <ArrowRight size={18} />
            </button>
          </fieldset>
        </form>
      </section>
    </>
  );
}
