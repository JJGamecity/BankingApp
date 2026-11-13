import test from "node:test";
import assert from "node:assert/strict";
import { projectSavings } from "./savings.js";

test("first month deposit earns no interest before arrival", () => {
  assert.deepEqual(projectSavings(250, 1), {
    contributions: 250,
    interest: 0,
    total: 250,
  });
});
test("month two earns interest only on the first deposit", () => {
  assert.ok(Math.abs(projectSavings(300, 2).total - 601) < 1e-9);
});
test("24-month maximum agrees with the annuity formula", () => {
  const result = projectSavings(1500, 24);
  const expected = (1500 * ((1 + 0.04 / 12) ** 24 - 1)) / (0.04 / 12);
  assert.ok(Math.abs(result.total - expected) < 1e-7);
  assert.equal(result.contributions, 36000);
  assert.ok(result.interest > 0);
});
test("zero contributions remain zero", () => {
  assert.deepEqual(projectSavings(0, 12), {
    contributions: 0,
    interest: 0,
    total: 0,
  });
});
