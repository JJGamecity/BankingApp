import { useEffect, useState } from "react";
import {
  Fingerprint,
  Eye,
  EyeOff,
  RefreshCw,
  ArrowUpRight,
} from "lucide-react";
import { request } from "../lib/api.js";

export default function CreditScoreCard({ profile, onExpired, onProfile }) {
  const [result, setResult] = useState({
    loading: !profile,
    score: null,
    error: "",
  });
  const [retry, setRetry] = useState(0);
  const [hidden, setHidden] = useState(false);
  useEffect(() => {
    if (profile) return;
    const controller = new AbortController();
    setResult({ loading: true, score: null, error: "" });
    request("/users/me", { signal: controller.signal })
      .then((data) => {
        if (!controller.signal.aborted)
          setResult({ loading: false, score: data.creditScore, error: "" });
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        if (error.status === 401) onExpired();
        else
          setResult({
            loading: false,
            score: null,
            error: "Your score could not be loaded.",
          });
      });
    return () => controller.abort();
  }, [profile, retry, onExpired]);
  const state = profile
    ? { loading: false, score: profile.creditScore, error: "" }
    : result;
  const available =
    typeof state.score === "number" && Number.isFinite(state.score);
  return (
    <section className="credit-card" aria-label="Your credit score">
      <div className="credit-card-content">
        <span className="credit-eyebrow">
          <Fingerprint size={18} /> YOUR FINANCIAL PICTURE
        </span>
        <h2>
          A little more insight.
          <br />
          <em>Your credit, in focus.</em>
        </h2>
        <p>See the credit score currently recorded on your customer profile.</p>
        <div className="credit-card-actions">
          <button
            className="credit-privacy"
            type="button"
            aria-pressed={hidden}
            onClick={() => setHidden(!hidden)}
          >
            {hidden ? <Eye size={15} /> : <EyeOff size={15} />}
            {hidden ? "Show score" : "Hide score"}
          </button>
          {onProfile && (
            <button className="credit-profile" onClick={onProfile}>
              My profile <ArrowUpRight size={16} />
            </button>
          )}
        </div>
      </div>
      <div className="credit-score-display">
        <div className="credit-score-orbit">
          <div className="credit-score-inner" aria-live="polite">
            <span>CREDIT SCORE</span>
            <strong>
              {hidden
                ? "•••"
                : state.loading
                  ? "…"
                  : available
                    ? state.score
                    : "—"}
            </strong>
            <span>
              {hidden
                ? "Hidden from view"
                : state.loading
                  ? "Loading your score"
                  : state.error
                    ? "Temporarily unavailable"
                    : available
                      ? "On your profile"
                      : "Not assessed yet"}
            </span>
          </div>
        </div>
      </div>
      <div className="credit-card-note">
        {state.error ? (
          <>
            <span role="status">{state.error}</span>
            <button onClick={() => setRetry((value) => value + 1)}>
              <RefreshCw size={13} /> Try again
            </button>
          </>
        ) : (
          <span>
            {available
              ? "Score provider, scoring model, and assessment date are not supplied by the bank."
              : "When a score is added to your profile, it will appear here."}{" "}
            Viewing this card does not request a new credit check.
          </span>
        )}
      </div>
    </section>
  );
}
