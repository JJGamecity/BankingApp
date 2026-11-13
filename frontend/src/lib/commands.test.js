import test from "node:test";
import assert from "node:assert/strict";
import { bankingCommands, searchCommands } from "./commands.js";

test("customer shortcuts exclude staff actions", () => {
  assert.deepEqual(
    bankingCommands(["CUSTOMER"]).map((item) => item.id),
    ["overview", "accounts", "profile"],
  );
});
test("staff registration appears only for admins", () => {
  assert.ok(!bankingCommands(["STAFF"]).some((item) => item.id === "staff"));
  assert.ok(bankingCommands(["ADMIN"]).some((item) => item.id === "staff"));
});
test("account search exposes only masked identifiers and excludes balances", () => {
  const commands = bankingCommands(
    ["CUSTOMER"],
    [
      {
        id: 3,
        accountType: "SAVINGS",
        accountNumber: "1234567890",
        availableBalance: 98765,
      },
    ],
  );
  const matches = searchCommands(commands, "SAVINGS 7890");
  assert.equal(matches.length, 1);
  assert.equal(matches[0].accountId, 3);
  assert.ok(!JSON.stringify(commands).includes("1234567890"));
  assert.ok(!JSON.stringify(commands).includes("98765"));
});
test("search supports whitespace, multiple words, and no results", () => {
  const commands = bankingCommands(["CUSTOMER"]);
  assert.equal(searchCommands(commands, "  credit   SCORE ").length, 2);
  assert.equal(searchCommands(commands, "unmatched").length, 0);
  assert.equal(searchCommands(commands, "  ").length, commands.length);
});
test("staff command lists never include customer account data", () => {
  assert.ok(
    bankingCommands(["ADMIN"], [{ id: 3, accountNumber: "1234" }]).every(
      (item) => item.accountId === undefined,
    ),
  );
});
