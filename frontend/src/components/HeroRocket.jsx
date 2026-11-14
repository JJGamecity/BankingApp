import { useEffect, useRef } from "react";
import { aimRocket, ROCKET_WIDTH, ROCKET_HEIGHT } from "../lib/rocket.js";

export default function HeroRocket() {
  const layer = useRef(null);
  const canvas = useRef(null);
  const sprite = useRef(null);
  useEffect(() => {
    const root = layer.current;
    const context = canvas.current.getContext("2d");
    if (!context) return;
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
    const mouse = window.matchMedia("(hover: hover) and (pointer: fine)");
    let frame = 0,
      lastTime = 0;
    let active = false;
    let fadeTimer = null;
    let pointer = null;
    let x = 0,
      y = 0,
      targetX = 0,
      targetY = 0,
      angle = -Math.PI / 2;
    let particles = [];
    let emissionElapsed = 0;
    let emissionDistance = 0;
    const emissionInterval = 300;
    let width = 0,
      height = 0;
    function clampTarget() {
      const maxY = Math.max(
        30,
        Math.min(420, window.innerHeight * 0.55, height - 30),
      );
      targetX = Math.max(30, Math.min(width - 30, targetX));
      targetY = Math.max(30, Math.min(maxY, targetY));
      x = Math.max(30, Math.min(width - 30, x));
      y = Math.max(30, Math.min(maxY, y));
    }
    function stop() {
      clearTimeout(fadeTimer);
      fadeTimer = null;
      cancelAnimationFrame(frame);
      frame = 0;
      lastTime = 0;
      active = false;
      particles = [];
      emissionElapsed = 0;
      emissionDistance = 0;
      root.dataset.flying = "false";
      context.clearRect(0, 0, width, height);
    }
    function fadeOut() {
      if (root.dataset.flying !== "true") return;
      cancelAnimationFrame(frame);
      frame = 0;
      lastTime = 0;
      root.dataset.flying = "false";
      fadeTimer = setTimeout(stop, 400);
    }
    function leave() {
      pointer = null;
      fadeOut();
    }
    function onScroll() {
      if (pointer) move(pointer);
    }
    function resize() {
      width = root.clientWidth;
      height = root.clientHeight;
      clampTarget();
      const ratio = Math.min(window.devicePixelRatio || 1, 2);
      canvas.current.width = width * ratio;
      canvas.current.height = height * ratio;
      context.setTransform(ratio, 0, 0, ratio, 0, 0);
      // Resizing should not dismiss an already active decoration either.
      if (active && !frame) frame = requestAnimationFrame(draw);
    }
    function draw(time) {
      frame = 0;
      const dt = lastTime ? Math.min(time - lastTime, 40) : 16;
      lastTime = time;
      const dx = targetX - x,
        dy = targetY - y;
      const distance = Math.hypot(dx, dy);
      const travel = Math.max(0, distance - 24) * (1 - Math.exp(-dt / 115));
      if (distance > 25) {
        x += (dx / distance) * travel;
        y += (dy / distance) * travel;
      }
      const pose = aimRocket(x, y, targetX, targetY, angle);
      angle = pose.angle;
      sprite.current.style.transform = `translate3d(${x}px, ${y}px, 0) translate(-50%, -50%) rotate(${pose.rotation}rad)`;
      emissionElapsed = Math.min(emissionInterval, emissionElapsed + dt);
      if (distance > 25) emissionDistance += travel;
      // Widely spaced dots: at most one per 300ms and 24px of movement.
      if (
        distance > 25 &&
        travel > 0.05 &&
        emissionElapsed >= emissionInterval &&
        emissionDistance >= 24
      ) {
        particles.push({
          x: pose.nozzle.x,
          y: pose.nozzle.y,
          vx: -Math.cos(angle) * 0.015,
          vy: -Math.sin(angle) * 0.015,
          age: 0,
          life: 1500 + Math.random() * 300,
          radius: 2.3 + Math.random() * 0.5,
        });
        emissionElapsed = 0;
        emissionDistance = 0;
      }
      particles = particles.filter((p) => p.age < p.life).slice(-40);
      context.clearRect(0, 0, width, height);
      for (const p of particles) {
        p.age += dt;
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        // Hold the bright dot for half its life, then fade without becoming smoke.
        const fade = Math.max(0, Math.min(1, (1 - p.age / p.life) * 2));
        context.beginPath();
        context.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        context.shadowColor = `rgba(255,255,255,${fade * 0.5})`;
        context.shadowBlur = 4;
        context.fillStyle = `rgba(255,255,255,${fade * 0.95})`;
        context.fill();
      }
      if (active && (distance > 25 || particles.length))
        frame = requestAnimationFrame(draw);
      else lastTime = 0;
    }
    function move(event) {
      if (reduced.matches || !mouse.matches || event.pointerType !== "mouse")
        return;
      // Use coordinates within the page's top decoration, not the viewport.
      // Fade at the edge instead of leaving the decoration visible indefinitely.
      pointer = {
        clientX: event.clientX,
        clientY: event.clientY,
        pointerType: event.pointerType,
      };
      const bounds = root.getBoundingClientRect();
      if (
        event.clientX < bounds.left ||
        event.clientX > bounds.right ||
        event.clientY < bounds.top ||
        event.clientY > bounds.bottom
      ) {
        fadeOut();
        return;
      }
      clearTimeout(fadeTimer);
      fadeTimer = null;
      targetX = event.clientX - bounds.left;
      targetY = event.clientY - bounds.top;
      clampTarget();
      if (!active) {
        x = targetX - 50;
        y = targetY + 35;
        clampTarget();
        active = true;
      }
      root.dataset.flying = "true";
      if (!frame) frame = requestAnimationFrame(draw);
    }
    resize();
    window.addEventListener("pointermove", move, { passive: true });
    document.documentElement.addEventListener("pointerleave", leave);
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", resize);
    window.addEventListener("blur", stop);
    document.addEventListener("visibilitychange", stop);
    reduced.addEventListener("change", stop);
    mouse.addEventListener("change", stop);
    return () => {
      stop();
      window.removeEventListener("pointermove", move);
      document.documentElement.removeEventListener("pointerleave", leave);
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", resize);
      window.removeEventListener("blur", stop);
      document.removeEventListener("visibilitychange", stop);
      reduced.removeEventListener("change", stop);
      mouse.removeEventListener("change", stop);
    };
  }, []);
  return (
    <div ref={layer} className="rocket-flight-layer" aria-hidden="true">
      <canvas ref={canvas} />
      <img
        ref={sprite}
        className="rocket-flight-sprite"
        src="/images/blue-rocket.svg?v=nozzle-dots"
        alt=""
        width={ROCKET_WIDTH}
        height={ROCKET_HEIGHT}
        style={{ width: ROCKET_WIDTH, height: ROCKET_HEIGHT }}
        draggable="false"
      />
    </div>
  );
}
