import {
  ArrowUpRight,
  Layers3,
  ShieldCheck,
  Sparkles,
  Wallet,
} from "lucide-react";

export default function BankingShowcase() {
  return (
    <div className="banking-showcase">
      <div className="showcase-ribbons" aria-hidden="true">
        {Array.from({ length: 7 }, (_, index) => (
          <span key={index} style={{ "--ribbon": index }} />
        ))}
      </div>
      <div
        className="showcase-window"
        role="img"
        aria-label="Illustrative banking dashboard with a sample total balance of $12,480 across three accounts. This is not live account data."
      >
        <div className="showcase-window-top">
          <span>
            <Layers3 size={15} /> banking.
          </span>
          <span className="showcase-example">ILLUSTRATIVE PREVIEW</span>
          <span className="showcase-avatar">J</span>
        </div>
        <div className="showcase-window-body">
          <div className="showcase-greeting">
            <span>YOUR MONEY, IN PERSPECTIVE</span>
            <strong>A little more clarity.</strong>
          </div>
          <div className="showcase-balance">
            <div>
              <span>Total available balance</span>
              <strong>
                $12,480<span>.00</span>
              </strong>
              <small>Across 3 sample accounts</small>
            </div>
            <span className="showcase-wallet">
              <Wallet size={25} />
            </span>
          </div>
          <div className="showcase-bottom">
            <div className="showcase-map">
              <span>
                Money Compass <ArrowUpRight size={12} />
              </span>
              <div className="showcase-donut">
                <span>
                  3<small>accounts</small>
                </span>
              </div>
            </div>
            <div className="showcase-account-list">
              <span>EVERYDAY, ORGANIZED</span>
              {[
                ["Checking", "$6,240", "#79bff4"],
                ["Savings", "$4,368", "#a49aef"],
                ["Money market", "$1,872", "#65c9bc"],
              ].map(([name, balance, color]) => (
                <div key={name}>
                  <i style={{ background: color }} />
                  <span>{name}</span>
                  <strong>{balance}</strong>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
      <div className="showcase-float">
        <span>
          <ShieldCheck size={23} />
        </span>
        <div>
          Your space. Your pace.<strong>Banking with a clearer view.</strong>
        </div>
        <Sparkles size={18} />
      </div>
      <p className="showcase-caption">
        Designed around your everyday. Ready for what’s next.
      </p>
    </div>
  );
}
