import { Reveal } from "@/components/reveal";
import { Badge } from "@/components/ui/badge";
import { Check, Radio, Wifi } from "lucide-react";

/**
 * A faithful recreation of the real Compose screens (feature-chat, feature-discovery), built
 * from the same design tokens as the mobile app -- not a stock screenshot, not a generic phone
 * mockup. Every string and state shown here matches an actual DeliveryState /
 * MeshCoordinator behavior described in docs/workflow.md.
 */
export function Screenshots() {
  return (
    <section id="screenshots" className="border-b border-astra-border/60 py-24 sm:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <Reveal>
          <h2 className="max-w-lg text-3xl font-medium tracking-[-0.02em] text-astra-text-primary sm:text-4xl">
            The interface, in the same black.
          </h2>
          <p className="mt-4 max-w-xl text-astra-text-secondary">
            One monochrome system across the app and this page — built with Jetpack Compose on
            device, and the same color tokens here.
          </p>
        </Reveal>

        <div className="mt-14 grid gap-6 lg:grid-cols-2">
          <Reveal>
            <PhoneFrame label="Nearby">
              <div className="flex items-center justify-between border-b border-astra-border px-4 py-3">
                <span className="text-sm font-medium">Nearby Nodes</span>
                <Wifi className="size-4 text-astra-text-secondary" />
              </div>
              <div className="flex items-center justify-between px-4 py-3">
                <div>
                  <p className="text-sm font-medium">Scanning</p>
                  <p className="text-xs text-astra-text-secondary">3 peers nearby</p>
                </div>
                <Badge variant="accent">Live</Badge>
              </div>
              <div className="flex flex-col gap-2 p-3">
                {[
                  { name: "node-b3f1", state: "ACTIVE", signal: "strong" },
                  { name: "node-7a02", state: "HANDSHAKING", signal: "medium" },
                  { name: "node-e9c4", state: "DISCOVERED", signal: "weak" },
                ].map((peer) => (
                  <div
                    key={peer.name}
                    className="flex items-center justify-between rounded-lg bg-astra-surface px-3 py-2.5"
                  >
                    <div className="flex items-center gap-2.5">
                      <span
                        className={`size-2 rounded-full ${
                          peer.signal === "strong"
                            ? "bg-astra-accent"
                            : peer.signal === "medium"
                              ? "bg-astra-text-secondary"
                              : "bg-astra-text-disabled"
                        }`}
                      />
                      <span className="font-mono text-xs">{peer.name}</span>
                    </div>
                    <span className="rounded bg-astra-panel px-1.5 py-0.5 text-[10px] text-astra-text-secondary">
                      {peer.state}
                    </span>
                  </div>
                ))}
              </div>
            </PhoneFrame>
          </Reveal>

          <Reveal delay={0.08}>
            <PhoneFrame label="Chat thread">
              <div className="flex flex-col border-b border-astra-border px-4 py-3">
                <span className="text-sm font-medium">node-b3f1</span>
                <span className="flex items-center gap-1.5 text-xs text-astra-success">
                  <span className="size-1.5 rounded-full bg-astra-success" /> Connected
                </span>
              </div>
              <div className="flex flex-col gap-3 p-4">
                <div className="ml-auto max-w-[75%] rounded-2xl rounded-br-md bg-astra-panel px-3 py-2 text-sm">
                  Reached the shelter, all four of us are safe.
                </div>
                <div className="ml-auto flex items-center gap-1.5 text-[10px] text-astra-text-disabled">
                  10:42 <Check className="size-3 text-astra-success" /> Delivered
                </div>
                <div className="mr-auto max-w-[75%] rounded-2xl rounded-bl-md bg-astra-surface px-3 py-2 text-sm">
                  Good. Sending water coordinates now.
                </div>
                <div className="mr-auto text-[10px] text-astra-text-disabled">
                  10:43 · relayed ×2
                </div>
              </div>
            </PhoneFrame>
          </Reveal>

          <Reveal delay={0.12}>
            <PhoneFrame label="Emergency broadcast">
              <div className="flex items-center gap-2 border-b border-astra-border px-4 py-3">
                <Radio className="size-4 text-astra-critical" />
                <span className="text-sm font-medium">Broadcast</span>
              </div>
              <div className="flex flex-col gap-3 p-4">
                <div className="rounded-xl border border-astra-critical/30 bg-astra-critical/10 p-3">
                  <p className="text-xs font-medium text-astra-critical">CRITICAL</p>
                  <p className="mt-1 text-sm">
                    Bridge on Route 9 unstable. Do not cross. Rescue staged at north lot.
                  </p>
                  <p className="mt-2 text-[10px] text-astra-text-disabled">
                    Reached 11 nodes · relayed ×3
                  </p>
                </div>
              </div>
            </PhoneFrame>
          </Reveal>

          <Reveal delay={0.16}>
            <PhoneFrame label="Diagnostics">
              <div className="border-b border-astra-border px-4 py-3">
                <span className="text-sm font-medium">Diagnostics</span>
              </div>
              <div className="grid grid-cols-2 gap-px bg-astra-border">
                {[
                  ["Chat sent", "128"],
                  ["Chat received", "112"],
                  ["Relayed", "47"],
                  ["Dedup cache", "203"],
                ].map(([label, value]) => (
                  <div key={label} className="bg-astra-panel px-3 py-3">
                    <p className="text-[10px] text-astra-text-secondary">{label}</p>
                    <p className="mt-1 font-mono text-lg">{value}</p>
                  </div>
                ))}
              </div>
            </PhoneFrame>
          </Reveal>
        </div>
      </div>
    </section>
  );
}

function PhoneFrame({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <p className="mb-2 text-xs text-astra-text-disabled">{label}</p>
      <div className="overflow-hidden rounded-2xl border border-astra-border bg-astra-black shadow-[0_0_0_1px_rgba(255,255,255,0.03)]">
        {children}
      </div>
    </div>
  );
}
