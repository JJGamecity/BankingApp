export const SAVINGS_ANNUAL_RATE = 0.04;
export const SAVINGS_RATE_LABEL = `${(SAVINGS_ANNUAL_RATE * 100).toFixed(1)}%`;

// Nominal annual rate, compounded monthly. Deposits arrive at month end.
export function projectSavings(monthly, months) {
  let total = 0;
  for (let month = 0; month < months; month += 1) {
    total = total * (1 + SAVINGS_ANNUAL_RATE / 12) + monthly;
  }
  const contributions = monthly * months;
  return { contributions, interest: total - contributions, total };
}
