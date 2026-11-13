import { useEffect, useState } from "react";
import EditorialGallery from "../components/EditorialGallery.jsx";
import MoneyCompass from "../components/MoneyCompass.jsx";
import CreditScoreCard from "../components/CreditScoreCard.jsx";
import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  Wallet,
  Sprout,
  ShieldCheck,
  Eye,
  EyeOff,
} from "lucide-react";
import { request, money, accountName } from "../lib/api.js";
import {
  AccountCard,
  DataRow,
  EmptyAccounts,
  Loading,
  Notice,
  PageHeading,
} from "../components/UI.jsx";

export default function CustomerPage({
  session,
  page,
  onNavigate,
  onExpired,
  targetAccountId,
  onAccountTargetConsumed,
}) {
  const [data, setData] = useState(null);
  const [dataPage, setDataPage] = useState(null);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [selected, setSelected] = useState(null);
  const [hideBalances, setHideBalances] = useState(false);
  const formatBalance = (value) => (hideBalances ? "••••••" : money(value));
  const privacyControl = (
    <div className="balance-privacy-toolbar">
      <button
        type="button"
        aria-pressed={hideBalances}
        onClick={() => setHideBalances(!hideBalances)}
      >
        {hideBalances ? <Eye size={16} /> : <EyeOff size={16} />}
        {hideBalances ? "Show balances" : "Hide balances"}
      </button>
    </div>
  );

  useEffect(() => {
    const controller = new AbortController();
    setData(null);
    setDataPage(null);
    setError("");
    setSelected(null);
    request(page === "profile" ? "/users/me" : "/accounts/me", {
      signal: controller.signal,
    })
      .then((result) => {
        if (controller.signal.aborted) return;
        setData(result);
        setDataPage(page);
      })
      .catch((failure) => {
        if (failure.name === "AbortError") return;
        if (failure.status === 401) onExpired();
        else setError(failure.message);
      });
    return () => controller.abort();
  }, [page, retry, onExpired]);

  useEffect(() => {
    if (
      page !== "accounts" ||
      dataPage !== page ||
      !Array.isArray(data) ||
      targetAccountId == null
    )
      return;
    const account = data.find((item) => item.id === targetAccountId);
    if (account) setSelected(account);
    onAccountTargetConsumed();
  }, [page, dataPage, data, targetAccountId, onAccountTargetConsumed]);

  if (error)
    return (
      <>
        <Notice>{error}</Notice>
        <button
          className="button button-secondary"
          onClick={() => setRetry(retry + 1)}
        >
          Try again
        </button>
      </>
    );
  if (!data) return <Loading />;
  if (page === "profile")
    return (
      <>
        <PageHeading title="A little about you.">
          Your details, all in one place.
        </PageHeading>
        <section className="profile-panel panel">
          <div className="profile-intro">
            <span className="avatar avatar-large">{data.firstName?.[0]}</span>
            <h2>
              {data.firstName} {data.lastName}
            </h2>
            <span className="pill pill-light">PERSONAL BANKING</span>
          </div>
          <div>
            <DataRow label="Username" value={data.username} />
            <DataRow label="Email address" value={data.email} />
            <DataRow label="Customer ID" value={data.id} />
            <p className="form-help">
              Need to change your details? Contact a bank representative.
            </p>
          </div>
        </section>
        <CreditScoreCard profile={data} onExpired={onExpired} />
      </>
    );
  if (selected)
    return (
      <>
        <button
          className="text-button back-button"
          onClick={() => setSelected(null)}
        >
          <ArrowLeft size={17} /> Back to accounts
        </button>
        <PageHeading title={accountName(selected.accountType)}>
          The details behind your everyday banking.
        </PageHeading>
        <div className="detail-grid">
          <section className="balance-card">
            <Wallet size={26} />
            <p className="eyebrow">AVAILABLE BALANCE</p>
            {privacyControl}
            <h2>{formatBalance(selected.availableBalance)}</h2>
            <span>
              Account ending {String(selected.accountNumber).slice(-4)}
            </span>
          </section>
          <section className="panel">
            <h2>Account details</h2>
            <DataRow label="Account number" value={selected.accountNumber} />
            <DataRow label="Routing number" value={selected.routingNumber} />
            <DataRow label="Account ID" value={selected.id} />
          </section>
        </div>
      </>
    );

  const total = data.every((account) => account.availableBalance != null)
    ? data.reduce((sum, account) => sum + Number(account.availableBalance), 0)
    : null;
  return (
    <>
      <PageHeading
        title={
          page === "overview"
            ? `Looking good, ${session.username}.`
            : "Your accounts."
        }
      >
        {page === "overview"
          ? "A fresh perspective on your financial world."
          : "Everything you need, right where you need it."}
      </PageHeading>
      {privacyControl}
      {page === "overview" && (
        <div className="overview-grid">
          <section className="balance-card">
            <div className="card-top">
              <span className="eyebrow">YOUR TOTAL BALANCE</span>
              <Wallet size={23} />
            </div>
            <h2>{formatBalance(total)}</h2>
            <p>
              Available across {data.length}{" "}
              {data.length === 1 ? "account" : "accounts"}
            </p>
            <div className="balance-bottom">
              <span>
                <span className="status-dot" /> Your money, at a glance
              </span>
              <button
                onClick={() => onNavigate("accounts")}
                aria-label="View all accounts"
              >
                <ArrowUpRight size={22} />
              </button>
            </div>
          </section>
          <section className="possibility-card">
            <Sprout size={32} />
            <span className="eyebrow">SPACE FOR YOUR NEXT CHAPTER</span>
            <h2>
              Little steps.
              <br />
              <em>Big possibilities.</em>
            </h2>
            <p>Your next chapter starts with knowing where you stand.</p>
            <button
              className="text-button"
              onClick={() => onNavigate("profile")}
            >
              View my profile <ArrowRight size={16} />
            </button>
          </section>
        </div>
      )}
      {page === "overview" && (
        <MoneyCompass
          accounts={data}
          hidden={hideBalances}
          onSelect={setSelected}
        />
      )}
      {page === "overview" && (
        <CreditScoreCard
          onExpired={onExpired}
          onProfile={() => onNavigate("profile")}
        />
      )}
      <div className="section-heading">
        <div>
          <h2>Made for your everyday</h2>
          <p>Your accounts, simply organized.</p>
        </div>
        <span className="count-label">
          {data.length} {data.length === 1 ? "ACCOUNT" : "ACCOUNTS"}
        </span>
      </div>
      <div className="account-grid">
        {data.length ? (
          data.map((account) => (
            <AccountCard
              key={account.id}
              account={account}
              formatMoney={formatBalance}
              formatName={accountName}
              onClick={() => setSelected(account)}
            />
          ))
        ) : (
          <EmptyAccounts />
        )}
      </div>
      {page === "overview" && <EditorialGallery compact />}
      <div className="bottom-note">
        <ShieldCheck size={22} />
        <div>
          <strong>A clearer view. A calmer everyday.</strong>
          <p>
            Keep your account details private and sign out when you’re finished.
          </p>
        </div>
      </div>
    </>
  );
}
