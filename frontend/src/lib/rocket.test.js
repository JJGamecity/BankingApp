import test from "node:test";
import assert from "node:assert/strict";
import { aimRocket } from "./rocket.js";

const near = (actual, expected) =>
  assert.ok(
    Math.abs(actual - expected) < 1e-9,
    `${actual} should match ${expected}`,
  );
for (const [name, tx, ty, angle, nx, ny] of [
  ["up", 100, 0, -Math.PI / 2, 100, 112.6],
  ["right", 200, 100, 0, 87.4, 100],
  ["down", 100, 200, Math.PI / 2, 100, 87.4],
  ["left", 0, 100, Math.PI, 112.6, 100],
]) {
  test(`rocket faces ${name} and exhaust starts at the opposite nozzle`, () => {
    const pose = aimRocket(100, 100, tx, ty);
    near(pose.angle, angle);
    near(pose.nozzle.x, nx);
    near(pose.nozzle.y, ny);
  });
}
test("diagonal heading aligns the exhaust with the body's axis", () => {
  const pose = aimRocket(100, 100, 140, 60);
  near(pose.angle, -Math.PI / 4);
  near(pose.nozzle.x, 100 - 12.6 / Math.sqrt(2));
  near(pose.nozzle.y, 100 + 12.6 / Math.sqrt(2));
});
test("rocket holds its heading when cursor is exactly at its center", () => {
  near(aimRocket(100, 100, 100, 100, 1.25).angle, 1.25);
});
