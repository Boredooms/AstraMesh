import { Reveal } from "@/components/reveal";

const LAYERS = [
  {
    name: "App",
    modules: ["app", "feature-chat", "feature-discovery", "feature-files", "feature-broadcast", "feature-settings"],
    note: "Compose UI + ViewModels. Never talks to Bluetooth or crypto directly.",
  },
  {
    name: "Mesh",
    modules: ["core-mesh", "core-ui"],
    note: "MeshCoordinator ties transport, routing, security, and persistence together.",
  },
  {
    name: "Core engines",
    modules: ["core-routing", "core-security", "core-transport", "core-persistence"],
    note: "Pure Kotlin where possible — routing and security are unit-tested with zero Android dependency.",
  },
  {
    name: "Protocol",
    modules: ["core-domain", "core-protocol"],
    note: "Entities, packet envelope, and typed payloads. Everything else is built on this.",
  },
];

export function Architecture() {
  return (
    <section id="architecture" className="border-b border-astra-border/60 py-24 sm:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <Reveal>
          <h2 className="max-w-lg text-3xl font-medium tracking-[-0.02em] text-astra-text-primary sm:text-4xl">
            Fourteen modules, one direction of dependency.
          </h2>
          <p className="mt-4 max-w-xl text-astra-text-secondary">
            The protocol layer knows nothing about Android. The routing engine knows nothing
            about Bluetooth. Every layer above only depends on the interfaces of the layer
            below it — which is what makes the whole mesh pipeline testable on the JVM, no
            emulator required.
          </p>
        </Reveal>

        <Reveal delay={0.1}>
          <div className="mt-14 flex flex-col gap-3">
            {LAYERS.map((layer, i) => (
              <div
                key={layer.name}
                className="grid grid-cols-1 gap-4 rounded-xl border border-astra-border bg-astra-panel/40 p-5 sm:grid-cols-[140px_1fr_1fr] sm:items-center"
              >
                <div className="flex items-center gap-2 font-mono text-xs text-astra-text-secondary">
                  <span className="rounded border border-astra-border px-1.5 py-0.5">
                    L{LAYERS.length - i}
                  </span>
                  {layer.name}
                </div>
                <div className="flex flex-wrap gap-2">
                  {layer.modules.map((m) => (
                    <code
                      key={m}
                      className="rounded-md border border-astra-border bg-astra-surface px-2 py-1 font-mono text-xs text-astra-text-primary"
                    >
                      {m}
                    </code>
                  ))}
                </div>
                <p className="text-sm text-astra-text-secondary">{layer.note}</p>
              </div>
            ))}
          </div>
        </Reveal>

        <Reveal delay={0.15}>
          <p className="mt-6 text-sm text-astra-text-disabled">
            Full dependency graph and design rationale: see{" "}
            <code className="text-astra-text-secondary">docs/architecture.md</code> in the
            repository.
          </p>
        </Reveal>
      </div>
    </section>
  );
}
