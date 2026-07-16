"use client";

import { useEffect, useRef } from "react";

/**
 * A live, canvas-drawn mesh graph: nodes drift slowly, edges connect nodes within range, and
 * one packet at a time animates hop-by-hop across a random path -- a literal, honest picture
 * of epidemic relay (docs/routing.md §2-3), not a decorative gradient blob.
 */
export function MeshVisual({ className }: { className?: string }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)"
    ).matches;

    let width = 0;
    let height = 0;
    let dpr = 1;

    const NODE_COUNT = 14;
    const RANGE = 150;

    type Node = { x: number; y: number; vx: number; vy: number };
    const nodes: Node[] = [];

    function resize() {
      const rect = canvas!.getBoundingClientRect();
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = rect.width;
      height = rect.height;
      canvas!.width = width * dpr;
      canvas!.height = height * dpr;
      ctx!.scale(dpr, dpr);
    }

    function seed() {
      nodes.length = 0;
      for (let i = 0; i < NODE_COUNT; i++) {
        nodes.push({
          x: Math.random() * width,
          y: Math.random() * height,
          vx: (Math.random() - 0.5) * 0.15,
          vy: (Math.random() - 0.5) * 0.15,
        });
      }
    }

    resize();
    seed();

    let hopPath: number[] = [];
    let hopIndex = 0;
    let hopProgress = 0;

    function pickNewPath() {
      const edges: [number, number][] = [];
      for (let i = 0; i < nodes.length; i++) {
        for (let j = i + 1; j < nodes.length; j++) {
          const dx = nodes[i].x - nodes[j].x;
          const dy = nodes[i].y - nodes[j].y;
          if (Math.hypot(dx, dy) < RANGE) edges.push([i, j]);
        }
      }
      if (edges.length === 0) return;
      const start = Math.floor(Math.random() * nodes.length);
      const path = [start];
      let current = start;
      const maxHops = 4;
      for (let h = 0; h < maxHops; h++) {
        const neighbors = edges
          .filter(([a, b]) => a === current || b === current)
          .map(([a, b]) => (a === current ? b : a))
          .filter((n) => !path.includes(n));
        if (neighbors.length === 0) break;
        const next = neighbors[Math.floor(Math.random() * neighbors.length)];
        path.push(next);
        current = next;
      }
      if (path.length > 1) {
        hopPath = path;
        hopIndex = 0;
        hopProgress = 0;
      }
    }

    let raf = 0;
    let last = performance.now();

    function frame(now: number) {
      const dt = Math.min(now - last, 40);
      last = now;

      ctx!.clearRect(0, 0, width, height);

      if (!prefersReducedMotion) {
        for (const n of nodes) {
          n.x += n.vx * dt;
          n.y += n.vy * dt;
          if (n.x < 0 || n.x > width) n.vx *= -1;
          if (n.y < 0 || n.y > height) n.vy *= -1;
        }
      }

      // Edges within relay range.
      ctx!.lineWidth = 1;
      for (let i = 0; i < nodes.length; i++) {
        for (let j = i + 1; j < nodes.length; j++) {
          const dx = nodes[i].x - nodes[j].x;
          const dy = nodes[i].y - nodes[j].y;
          const dist = Math.hypot(dx, dy);
          if (dist < RANGE) {
            const alpha = 0.16 * (1 - dist / RANGE);
            ctx!.strokeStyle = `rgba(168,168,168,${alpha})`;
            ctx!.beginPath();
            ctx!.moveTo(nodes[i].x, nodes[i].y);
            ctx!.lineTo(nodes[j].x, nodes[j].y);
            ctx!.stroke();
          }
        }
      }

      // Nodes.
      for (const n of nodes) {
        ctx!.beginPath();
        ctx!.arc(n.x, n.y, 2.5, 0, Math.PI * 2);
        ctx!.fillStyle = "rgba(237,237,237,0.55)";
        ctx!.fill();
      }

      // Traveling packet, hopping across a live relay path.
      if (!prefersReducedMotion) {
        if (hopPath.length < 2) pickNewPath();
        if (hopPath.length >= 2) {
          hopProgress += dt / 650;
          if (hopProgress >= 1) {
            hopProgress = 0;
            hopIndex++;
            if (hopIndex >= hopPath.length - 1) {
              pickNewPath();
            }
          }
          const a = nodes[hopPath[hopIndex]];
          const b = nodes[hopPath[hopIndex + 1]];
          if (a && b) {
            const px = a.x + (b.x - a.x) * hopProgress;
            const py = a.y + (b.y - a.y) * hopProgress;

            ctx!.beginPath();
            ctx!.moveTo(a.x, a.y);
            ctx!.lineTo(b.x, b.y);
            ctx!.strokeStyle = "rgba(79,176,198,0.55)";
            ctx!.lineWidth = 1.5;
            ctx!.stroke();

            ctx!.beginPath();
            ctx!.arc(px, py, 3.5, 0, Math.PI * 2);
            ctx!.fillStyle = "#4fb0c6";
            ctx!.shadowColor = "#4fb0c6";
            ctx!.shadowBlur = 8;
            ctx!.fill();
            ctx!.shadowBlur = 0;
          }
        }
      }

      raf = requestAnimationFrame(frame);
    }

    raf = requestAnimationFrame(frame);

    const onResize = () => {
      resize();
      seed();
      hopPath = [];
    };
    window.addEventListener("resize", onResize);

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", onResize);
    };
  }, []);

  return <canvas ref={canvasRef} className={className} aria-hidden="true" />;
}
