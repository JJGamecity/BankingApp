import { useEffect, useState } from "react";
import {
  ArrowRight,
  ArrowUpRight,
  Eye,
  EyeOff,
  Layers3,
  Sprout,
} from "lucide-react";
import { Brand, Field, Notice, SecurityNote } from "../components/UI.jsx";
import { API_URL, request, resetCsrf } from "../lib/api.js";
import { navigate, followLink } from "../lib/navigation.js";
import EditorialGallery from "../components/EditorialGallery.jsx";
import SiteFooter from "../components/SiteFooter.jsx";

export default function AuthPage({ onSession, initialMessage, mode }) {
  const setMode = (value) =>
    navigate(value === "signup" ? "/signup" : "/login");
  const [message, setMessage] = useState(initialMessage);
  const [success, setSuccess] = useState(false);
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [password, setPassword] = useState("");
  const [capsLock, setCapsLock] = useState(false);
  const [googleAvailable, setGoogleAvailable] = useState(null);
  const [googleError, setGoogleError] = useState("");
  const signup = mode === "signup";

  useEffect(() => {
    const controller = new AbortController();
    request("/auth/options", { signal: controller.signal })
      .then((options) => setGoogleAvailable(options.googleSignInEnabled === true))
      .catch((error) => {
        if (error.name !== "AbortError") {
          setGoogleAvailable(false);
          setGoogleError("We could not check Google sign-in availability. Refresh the page to try again.");
        }
      });
    return () => controller.abort();
  }, []);

  async function submit(event) {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    setBusy(true);
    setMessage("");
    setSuccess(false);
    try {
      if (signup) {
        await request("/auth/signup", { method: "POST", body: data });
        setMode("login");
        setSuccess(true);
        setMessage(
          "Your profile is ready. Sign in to start your next chapter.",
        );
      } else {
        await request("/login", { method: "POST", body: data, form: true });
        resetCsrf();
        await onSession();
      }
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-story">
        <a
          href="/"
          onClick={(event) => followLink(event, "/")}
          aria-label="Back to banking home"
          style={{ textDecoration: "none", color: "inherit" }}
        >
          <Brand />
        </a>
        <div className="story-copy">
          <span className="pill">
            <span className="status-dot" /> MADE FOR YOUR EVERYDAY
          </span>
          <h1>
            A little more clarity.
            <br />
            <em>A lot more possibility.</em>
          </h1>
          <p>
            From everyday essentials to whatever comes next. Keep your money
            close and your possibilities open.
          </p>
        </div>
        <EditorialGallery blue />
        <div className="banking-art legacy-art" aria-hidden="true">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <div className="art-note">
            <Sprout size={22} />
            <div>
              Small steps.<strong>Brighter tomorrows.</strong>
            </div>
          </div>
          <div className="presentation-card">
            <div className="card-top">
              <span>banking.</span>
              <Layers3 />
            </div>
            <div className="chip" />
            <div className="card-number">
              •••• &nbsp; •••• &nbsp; •••• &nbsp; ••••
            </div>
            <div className="card-top">
              <span>EVERYDAY POSSIBILITIES</span>
              <ArrowUpRight />
            </div>
          </div>
          <span className="art-caption">
            A clearer view of your financial world.
          </span>
        </div>
        <div className="story-footer">
          <span>Thoughtfully simple. Refreshingly yours.</span>
          <span>01 / A fresh perspective</span>
        </div>
      </section>

      <section className="auth-content">
        <div className="auth-topline">
          <span>YOUR NEXT CHAPTER STARTS HERE</span>
          <span>↗</span>
        </div>
        <div className="auth-form-wrap">
          <span className="small-logo">
            <LandmarkIcon />
          </span>
          <p className="eyebrow">WELCOME TO YOUR BANKING</p>
          <h2>{signup ? "Let’s make it yours." : "Welcome back."}</h2>
          <p className="muted">
            {signup
              ? "A few details, a fresh beginning."
              : "Your everyday banking. A little more effortless."}
          </p>
          <div className="auth-tabs" aria-label="Sign in or create profile">
            {["login", "signup"].map((value) => (
              <button
                type="button"
                key={value}
                disabled={busy}
                aria-pressed={mode === value}
                className={mode === value ? "active" : ""}
                onClick={() => {
                  setMode(value);
                  setMessage("");
                  setShowPassword(false);
                }}
              >
                {value === "login" ? "Sign in" : "Create profile"}
              </button>
            ))}
          </div>
          <Notice success={success}>{message}</Notice>
          <form key={mode} onSubmit={submit}>
            <fieldset disabled={busy}>
              {signup && (
                <>
                  <div className="form-row">
                    <Field
                      label="First name"
                      name="firstName"
                      autoComplete="given-name"
                    />
                    <Field
                      label="Last name"
                      name="lastName"
                      autoComplete="family-name"
                    />
                  </div>
                  <Field
                    label="Email address"
                    name="email"
                    type="email"
                    autoComplete="email"
                  />
                  <Field
                    label="Date of birth"
                    name="dateOfBirth"
                    type="date"
                    autoComplete="bday"
                  />
                  <p className="form-help">
                    You must be at least 18 to create a profile.
                  </p>
                </>
              )}
              <Field
                label="Username"
                name="username"
                autoComplete="username"
                placeholder="Enter your username"
                minLength={signup ? 3 : undefined}
                maxLength={signup ? 15 : undefined}
                pattern={signup ? "[A-Za-z0-9._-]+" : undefined}
              />
              <label className="field">
                <span>Password</span>
                <div className="password-field">
                  <input
                    required
                    name="password"
                    type={showPassword ? "text" : "password"}
                    autoComplete={signup ? "new-password" : "current-password"}
                    placeholder="Enter your password"
                    onChange={(event) => setPassword(event.target.value)}
                    onKeyUp={(event) =>
                      setCapsLock(event.getModifierState("CapsLock"))
                    }
                    onBlur={() => setCapsLock(false)}
                  />
                  <button
                    type="button"
                    aria-label={
                      showPassword ? "Hide password" : "Show password"
                    }
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </label>
              {capsLock && (
                <p className="caps-lock" role="status">
                  Caps Lock is on.
                </p>
              )}
              {signup && (
                <ul
                  className="password-checklist"
                  aria-label="Password guidance"
                >
                  <li
                    className={Array.from(password).length >= 10 ? "met" : ""}
                  >
                    {Array.from(password).length >= 10 ? "✓" : "○"} At least 10
                    characters
                  </li>
                  <li className={/[\p{P}\p{S}]/u.test(password) ? "met" : ""}>
                    {/[\p{P}\p{S}]/u.test(password) ? "✓" : "○"} A special
                    character
                  </li>
                </ul>
              )}
              {signup && (
                <p className="form-help">
                  10+ characters, a special character, and no three-character
                  letter or number sequences (even separated by spaces). Maximum
                  72 UTF-8 bytes.
                </p>
              )}
              <button
                className="button button-primary full-width"
                disabled={busy}
              >
                {busy
                  ? "Just a moment…"
                  : signup
                    ? "Create my profile"
                    : "Sign in to my account"}
                <ArrowRight size={18} />
              </button>
            </fieldset>
          </form>
          <div className="divider">
            <span>or continue with</span>
          </div>
          <button
            type="button"
            className="button button-google full-width"
            disabled={busy || googleAvailable !== true}
            onClick={() => window.location.assign(`${API_URL}/oauth2/authorization/google`)}
          >
            <img
              className="google-logo"
              src="/images/google-g.png"
              alt=""
              width="20"
              height="20"
            />
            <span>
              {signup ? "Sign up with Google" : "Sign in with Google"}
            </span>
          </button>
          {googleAvailable !== true && (
            <p className="google-help" role="status">
              {googleAvailable === null
                ? "Checking Google sign-in availability…"
                : googleError || "Google sign-in is currently unavailable. Please use your username and password."}
            </p>
          )}
          <p className="google-help">
            Already signed in with Google?{" "}
            <button className="text-button" disabled={busy} onClick={onSession}>
              Check my session
            </button>
          </p>
          <SecurityNote />
          <div className="auth-questions">
            <details>
              <summary>New here? What happens after signup?</summary>
              <p>
                Signup creates your customer profile. Sign in next, then contact
                a bank representative to open a checking, savings, or money
                market account.
              </p>
            </details>
            <details>
              <summary>How does Google sign-in work?</summary>
              <p>
                Use your Google account when the bank has enabled this option.
                New customers complete a short profile form. After the Google
                flow, return here and choose “Check my session.”
              </p>
            </details>
          </div>
        </div>
        <SiteFooter compact />
      </section>
    </main>
  );
}

function LandmarkIcon() {
  return <Layers3 size={24} />;
}
