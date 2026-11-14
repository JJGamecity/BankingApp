export default function BankingShowcase() {
  return (
    <div className="showcase">
      <div className="showcase-ribbons" aria-hidden="true">
        {Array.from({ length: 7 }, (_, i) => (
          <i key={i} style={{ "--i": i }} />
        ))}
      </div>
      <div className="showcase-screen">
        <header>
          <strong>JJBanking</strong>
          <span>ILLUSTRATIVE PREVIEW</span>
          <b>J</b>
        </header>
        <div className="showcase-body">
          <small>YOUR MONEY, IN PERSPECTIVE</small>
          <h2>A little more clarity.</h2>
          <div className="showcase-balance">
            <span>Total available balance</span>
            <strong>$12,480.00</strong>
            <small>Across 3 sample accounts</small>
          </div>
          <div className="showcase-details">
            <div>
              <span>Money Compass</span>
              <div className="showcase-ring">
                <b>
                  3<small>accounts</small>
                </b>
              </div>
            </div>
            <div className="showcase-accounts">
              {[
                ["Checking", "$6,240"],
                ["Savings", "$4,368"],
                ["Money market", "$1,872"],
              ].map(([name, value]) => (
                <p key={name}>
                  <span>{name}</span>
                  <strong>{value}</strong>
                </p>
              ))}
            </div>
          </div>
        </div>
      </div>
      <div className="showcase-note">
        <span>✦</span>
        <div>
          Your space. Your pace.<strong>Ready for what comes next.</strong>
        </div>
      </div>
      <p className="showcase-caption">
        Sample balances for illustration. Your real accounts stay private.
      </p>
    </div>
  );
}
