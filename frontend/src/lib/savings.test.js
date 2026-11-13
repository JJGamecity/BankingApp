import test from "node:test";
import assert from "node:assert/strict";
import { projectSavings } from "./savings.js";

test("month-end deposit earns no interest in the first month", () => {
  assert.deepEqual(projectSavings(250, 1), {
    contributions: 250,
    total: 250,
    interest: 0,
  });
});
test("only the first deposit earns interest during month two", () => {
  assert.ok(Math.abs(projectSavings(300, 2).total - 601) < 1e-9);
});
test("24-month projection matches the annuity formula", () => {
  const result = projectSavings(1500, 24);
  const expected = (1500 * ((1 + 0.04 / 12) ** 24 - 1)) / (0.04 / 12);
  assert.ok(Math.abs(result.total - expected) < 1e-7);
  assert.equal(result.contributions, 36000);
  assert.ok(result.interest > 0);
});
