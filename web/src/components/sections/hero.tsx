import { Button } from "@/components/ui/button";
import { MeshVisual } from "@/components/mesh-visual";
import { ArrowRight, Code2 } from "lucide-react";

export function Hero() {
  return (
    <section className="relative overflow-hidden border-b border-astra-border/60">
      <MeshVisual className="absolute inset-0 h-full w-full opacity-70" />
      <div className="relative mx-auto flex max-w-6xl flex-col px-6 pt-28 pb-24 sm:pt-36 sm:pb-32">
        <span className="inline-flex w-fit items-center gap-2 rounded-full border border-astra-border bg-astra-surface/80 px-3 py-1 text-xs text-astra-text-secondary">
          <span className="h-1.5 w-1.5 rounded-full bg-astra-success" />
          No internet. No server. No account.
        </span>

        <h1
          className="mt-6 max-w-3xl text-[clamp(2.5rem,6vw,4.5rem)] font-medium leading-[1.05] tracking-[-0.03em] text-astra-text-primary"
          style={{ textWrap: "balance" }}
        >
          Every phone becomes a node in the network.
        </h1>

        <p className="mt-6 max-w-xl text-lg leading-relaxed text-astra-text-secondary">
          AstraMesh turns nearby Android devices into a self-organizing, encrypted mesh —
          discovering peers over Bluetooth LE, relaying messages hop by hop, and delivering
          them later if a recipient is temporarily offline. Built for disaster zones,
          blackouts, and anywhere the network you can&apos;t rely on is the one everyone else
          controls.
        </p>

        <div className="mt-10 flex flex-wrap items-center gap-4">
          <Button asChild size="lg" variant="accent">
            <a href="#download">
              Download APK <ArrowRight className="size-4" />
            </a>
          </Button>
          <Button asChild size="lg" variant="outline">
            <a
              href="https://github.com/Boredooms/AstraMesh"
              target="_blank"
              rel="noreferrer"
            >
              <Code2 className="size-4" /> View source
            </a>
          </Button>
        </div>

        <dl className="mt-16 grid max-w-2xl grid-cols-2 gap-8 sm:grid-cols-4">
          {[
            ["8", "max relay hops per message"],
            ["256-bit", "AES-GCM end-to-end encryption"],
            ["0", "servers required"],
            ["26+", "Android API minimum"],
          ].map(([value, label]) => (
            <div key={label}>
              <dt className="font-mono text-2xl text-astra-text-primary">{value}</dt>
              <dd className="mt-1 text-sm text-astra-text-secondary">{label}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
