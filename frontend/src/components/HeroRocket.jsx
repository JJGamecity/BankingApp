import { useEffect, useRef } from "react";

export default function HeroRocket() {
  const layer = useRef(null);
  const canvas = useRef(null);
  const sprite = useRef(null);
  useEffect(() => {
    const root = layer.current;
    const hero = root.parentElement.querySelector(".home-hero");
    const context = canvas.current.getContext("2d");
    if (!context) return;
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
    const mouse = window.matchMedia("(hover: hover) and (pointer: fine)");
    let frame = 0,
      lastTime = 0;
    let active = false;
    let x = 0,
      y = 0,
      targetX = 0,
      targetY = 0,
      angle = -Math.PI / 2;
    let particles = [];
    let width = 0,
      height = 0;
    function stop() {
      cancelAnimationFrame(frame);
      frame = 0;
      lastTime = 0;
      active = false;
      particles = [];
      root.dataset.flying = "false";
      context.clearRect(0, 0, width, height);
    }
    function resize() {
      stop();
      width = window.innerWidth;
      height = window.innerHeight;
      const ratio = Math.min(window.devicePixelRatio || 1, 2);
      canvas.current.width = width * ratio;
      canvas.current.height = height * ratio;
      context.setTransform(ratio, 0, 0, ratio, 0, 0);
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
        const desiredAngle = Math.atan2(dy, dx);
        const turn = Math.atan2(
          Math.sin(desiredAngle - angle),
          Math.cos(desiredAngle - angle),
        );
        angle += turn * (1 - Math.exp(-dt / 70));
        x += (dx / distance) * travel;
        y += (dy / distance) * travel;
      }
      sprite.current.style.transform = `translate3d(${x}px, ${y}px, 0) translate(-50%, -50%) rotate(${angle + Math.PI / 2}rad)`;
      if (travel > 0.15) {
        const amount = Math.min(4, Math.max(1, Math.ceil(travel / 5)));
        for (let i = 0; i < amount; i++) {
          particles.push({
            x: x - Math.cos(angle) * (17 + i * 3),
            y: y - Math.sin(angle) * (17 + i * 3),
            vx: -Math.cos(angle) * 0.035 + (Math.random() - 0.5) * 0.022,
            vy: -Math.sin(angle) * 0.035 + (Math.random() - 0.5) * 0.022,
            age: 0,
            life: 450 + Math.random() * 250,
            radius: 1.8 + Math.random() * 1.5,
          });
        }
      }
      particles = particles.filter((p) => p.age < p.life).slice(-100);
      context.clearRect(0, 0, width, height);
      for (const p of particles) {
        p.age += dt;
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        const fade = Math.max(0, 1 - p.age / p.life);
        context.beginPath();
        context.arc(p.x, p.y, p.radius + p.age / 180, 0, Math.PI * 2);
        context.fillStyle = `rgba(255,255,255,${fade * 0.65})`;
        context.fill();
      }
      if (active && (distance > 25 || particles.length))
        frame = requestAnimationFrame(draw);
      else lastTime = 0;
    }
    function move(event) {
      if (reduced.matches || !mouse.matches || event.pointerType !== "mouse")
        return;
      const bounds = hero.getBoundingClientRect();
      if (event.clientY > bounds.bottom || bounds.bottom < 0) {
        stop();
        return;
      }
      targetX = event.clientX;
      targetY = event.clientY;
      if (!active) {
        x = targetX - 50;
        y = targetY + 35;
        active = true;
        root.dataset.flying = "true";
      }
      if (!frame) frame = requestAnimationFrame(draw);
    }
    resize();
    window.addEventListener("pointermove", move, { passive: true });
    document.documentElement.addEventListener("pointerleave", stop);
    window.addEventListener("scroll", stop, { passive: true });
    window.addEventListener("resize", resize);
    window.addEventListener("blur", stop);
    document.addEventListener("visibilitychange", stop);
    reduced.addEventListener("change", stop);
    mouse.addEventListener("change", stop);
    return () => {
      stop();
      window.removeEventListener("pointermove", move);
      document.documentElement.removeEventListener("pointerleave", stop);
      window.removeEventListener("scroll", stop);
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
        src="/images/blue-rocket.svg?v=white-exhaust"
        alt=""
        width="42"
        height="48"
        draggable="false"
      />
    </div>
  );
}
