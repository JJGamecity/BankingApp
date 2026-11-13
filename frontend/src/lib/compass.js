// Negative and unavailable balances cannot be represented as donut slices.
export function accountAllocation(accounts) {
  const rows = accounts.map((account) => {
    const raw = account.availableBalance;
    const balance = raw == null || raw === "" ? null : Number(raw);
    return { ...account, balance: Number.isFinite(balance) ? balance : null };
  });
  const positiveTotal = rows.reduce(
    (sum, row) => sum + Math.max(0, row.balance ?? 0),
    0,
  );
  return {
    positiveTotal,
    incomplete: rows.some((row) => row.balance === null),
    hasNegative: rows.some((row) => row.balance < 0),
    rows: rows.map((row) => ({
      ...row,
      share:
        positiveTotal > 0 ? Math.max(0, row.balance ?? 0) / positiveTotal : 0,
    })),
  };
}
