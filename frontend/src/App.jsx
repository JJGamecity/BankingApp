import { useCallback, useEffect, useState } from "react";
import { request, resetCsrf, API_URL } from "./lib/api.js";
import { Loading, Notice } from "./components/UI.jsx";
import Layout from "./components/Layout.jsx";
import AuthPage from "./pages/AuthPage.jsx";
import CustomerPage from "./pages/CustomerPage.jsx";
import StaffPage from "./pages/StaffPage.jsx";
import WelcomePage from "./pages/WelcomePage.jsx";
import { navigate, paths, usePath } from "./lib/navigation.js";

export default function App() {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const path = usePath();
  const page =
    Object.entries(paths).find(([, value]) => value === path)?.[0] ||
    "overview";
  const setPage = (next) =>
    navigate(
      next === "overview" && session && !session.roles.includes("CUSTOMER")
        ? paths.customers
        : paths[next],
    );
  const [busy, setBusy] = useState(false);

  const expired = useCallback(() => {
    resetCsrf();
    setMessage("Your session has expired. Please sign in again.");
    setSession(null);
    navigate("/login", true);
  }, []);

  const checkSession = useCallback(async () => {
    setLoading(true);
    try {
      setSession(await request("/auth/me"));
      setMessage("");
    } catch (error) {
      setSession(null);
      setMessage(error.status === 401 ? "" : error.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkSession();
  }, [checkSession]);

  useEffect(() => {
    if (loading) return;
    const publicPaths = ["/", "/login", "/signup"];
    if (!session) {
      if (!publicPaths.includes(path)) {
        if (Object.values(paths).includes(path))
          sessionStorage.setItem("banking:returnTo", path);
        navigate("/login", true);
      }
      return;
    }
    if (session.signupRequired) return;
    const customer = session.roles.includes("CUSTOMER");
    const allowed = customer
      ? ["/overview", "/accounts", "/profile"]
      : [
          "/customers",
          "/accounts/new",
          ...(session.roles.includes("ADMIN") ? ["/staff/new"] : []),
        ];
    if (!allowed.includes(path)) {
      const returnTo = sessionStorage.getItem("banking:returnTo");
      sessionStorage.removeItem("banking:returnTo");
      navigate(allowed.includes(returnTo) ? returnTo : allowed[0], true);
    }
  }, [loading, session, path]);

  useEffect(() => {
    const names = {
      "/login": "Login",
      "/signup": "Sign up",
      "/overview": "Overview",
      "/accounts": "My accounts",
      "/profile": "My profile",
      "/customers": "Customers",
      "/accounts/new": "Open an account",
      "/staff/new": "Create staff",
    };
    document.title = `${names[path] || "Welcome"} | BankingApp`;
  }, [path]);

  async function logout() {
    setBusy(true);
    setMessage("");
    try {
      await request("/logout", { method: "POST" });
      resetCsrf();
      setSession(null);
      sessionStorage.removeItem("banking:returnTo");
      navigate("/login", true);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  if (path === "/" && !session) return <WelcomePage />;
  if (loading)
    return (
      <div className="initial-loading">
        <Loading />
      </div>
    );
  if (!session)
    return (
      <AuthPage
        onSession={checkSession}
        initialMessage={message}
        mode={path === "/signup" ? "signup" : "login"}
      />
    );
  if (session.signupRequired)
    return (
      <div className="pending-signup panel">
        <h1>One more step.</h1>
        <p>Complete your Google profile before accessing your accounts.</p>
        <a className="button button-primary" href={`${API_URL}/signup/google`}>
          Complete my profile
        </a>
        <button className="button button-secondary" onClick={checkSession}>
          Check my session
        </button>
        <Notice>{message}</Notice>
        <button className="text-button" disabled={busy} onClick={logout}>
          Sign out
        </button>
      </div>
    );

  return (
    <Layout
      session={session}
      page={page === "customers" ? "overview" : page}
      onNavigate={(next) => {
        setPage(next);
        setMessage("");
      }}
      onLogout={logout}
      busy={busy}
    >
      <Notice>{message}</Notice>
      {session.roles.includes("CUSTOMER") ? (
        <CustomerPage
          session={session}
          page={page}
          onNavigate={setPage}
          onExpired={expired}
        />
      ) : (
        <StaffPage
          key={page}
          page={page === "customers" ? "overview" : page}
          onExpired={expired}
        />
      )}
    </Layout>
  );
}
