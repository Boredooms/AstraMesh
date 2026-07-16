import { Reveal } from "@/components/reveal";

const GROUPS = [
  {
    name: "Mobile app",
    items: ["Kotlin", "Jetpack Compose", "Material 3", "Coroutines & Flow", "Hilt", "Room", "Kotlin Serialization"],
  },
  {
    name: "Networking",
    items: ["Bluetooth LE advertising & scanning", "GATT sessions", "Transport-agnostic packet envelope"],
  },
  {
    name: "Security",
    items: ["ECDH (secp256r1)", "AES-256-GCM", "Per-peer session keys", "Local-only private keys"],
  },
  {
    name: "This website",
    items: ["Next.js", "Tailwind CSS", "shadcn/ui", "Radix primitives", "Motion", "Lenis"],
  },
];

export function Stack() {
  return (
    <section id="stack" className="border-b border-astra-border/60 py-24 sm:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <Reveal>
          <h2 className="max-w-lg text-3xl font-medium tracking-[-0.02em] text-astra-text-primary sm:text-4xl">
            Built on stable, boring technology.
          </h2>
          <p className="mt-4 max-w-xl text-astra-text-secondary">
            No experimental frameworks in the parts that need to just work in a blackout.
          </p>
        </Reveal>

        <div className="mt-14 grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
          {GROUPS.map((group, i) => (
            <Reveal key={group.name} delay={i * 0.06}>
              <h3 className="text-sm font-medium text-astra-text-primary">{group.name}</h3>
              <ul className="mt-3 flex flex-col gap-2">
                {group.items.map((item) => (
                  <li
                    key={item}
                    className="text-sm text-astra-text-secondary"
                  >
                    {item}
                  </li>
                ))}
              </ul>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
