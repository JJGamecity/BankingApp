export const SAVINGS_ANNUAL_RATE = 0.04;
export const SAVINGS_RATE_LABEL = `${(SAVINGS_ANNUAL_RATE * 100).toFixed(1)}%`;

// Fixed nominal annual rate, monthly compounding, deposits at month end.
export function projectSavings(monthlyContribution, months) {
  let total = 0;
  for (let month = 0; month < months; month += 1) {
    total = total * (1 + SAVINGS_ANNUAL_RATE / 12) + monthlyContribution;
  }
  const contributions = monthlyContribution * months;
  return { contributions, total, interest: total - contributions };
}
