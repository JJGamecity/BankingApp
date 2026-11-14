// Match blue-rocket.svg's 160 x 180 viewBox and its nozzle at (80, 138).
export const ROCKET_WIDTH = 42;
export const ROCKET_HEIGHT = (ROCKET_WIDTH * 180) / 160;
const NOZZLE_OFFSET = ((138 - 90) * ROCKET_HEIGHT) / 180;

export function aimRocket(
  x,
  y,
  targetX,
  targetY,
  previousAngle = -Math.PI / 2,
) {
  const dx = targetX - x;
  const dy = targetY - y;
  const angle = Math.hypot(dx, dy) > 0.01 ? Math.atan2(dy, dx) : previousAngle;
  // The SVG points up. Its rotated nozzle must use the same transform as its nose.
  const rotation = angle + Math.PI / 2;
  return {
    angle,
    rotation,
    nozzle: {
      x: x - Math.sin(rotation) * NOZZLE_OFFSET,
      y: y + Math.cos(rotation) * NOZZLE_OFFSET,
    },
  };
}
