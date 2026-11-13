import test from "node:test";
import assert from "node:assert/strict";
import { accountAllocation } from "./compass.js";

test("allocates positive balances including decimal strings", () => {
  const result = accountAllocation([
    { availableBalance: "75.50" },
    { availableBalance: "24.50" },
  ]);
  assert.equal(result.positiveTotal, 100);
  assert.deepEqual(
    result.rows.map((row) => row.share),
    [0.755, 0.245],
  );
  assert.equal(result.incomplete, false);
});
test("preserves negative balances but excludes them from the ring", () => {
  const result = accountAllocation([
    { availableBalance: -40 },
    { availableBalance: 80 },
  ]);
  assert.equal(result.positiveTotal, 80);
  assert.equal(result.hasNegative, true);
  assert.equal(result.rows[0].balance, -40);
  assert.deepEqual(
    result.rows.map((row) => row.share),
    [0, 1],
  );
});
test("marks missing and malformed balances as unavailable", () => {
  const result = accountAllocation(
    [null, undefined, "", "invalid", Infinity].map((availableBalance) => ({
      availableBalance,
    })),
  );
  assert.equal(result.incomplete, true);
  assert.equal(result.positiveTotal, 0);
  assert.ok(
    result.rows.every((row) => row.balance === null && row.share === 0),
  );
});
test("empty and zero-only accounts never divide by zero", () => {
  assert.deepEqual(accountAllocation([]), {
    positiveTotal: 0,
    incomplete: false,
    hasNegative: false,
    rows: [],
  });
  assert.equal(accountAllocation([{ availableBalance: 0 }]).rows[0].share, 0);
});
