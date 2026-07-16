import { Reveal } from "@/components/reveal";

const STEPS = [
  {
    title: "Discover",
    body: "Devices advertise and scan over Bluetooth LE. No pairing screen, no PIN — just presence.",
  },
  {
    title: "Handshake",
    body: "On first contact, both sides exchange public keys and derive a shared session key locally via ECDH.",
  },
  {
    title: "Send",
    body: "A message is persisted as PENDING, sealed with AES-GCM, wrapped in a versioned packet, and handed to routing.",
  },
  {
    title: "Relay",
    body: "If the destination isn't a direct peer, the packet hops through relay-capable neighbors — deduplicated, TTL-bounded.",
  },
  {
    title: "Store, if needed",
    body: "No route right now? The packet queues, still encrypted, and retries the moment a peer reappears.",
  },
  {
    title: "Deliver + ACK",
    body: "The destination decrypts, persists the message, and sends an encrypted ACK back through the same mesh.",
  },
];

export function Workflow() {
  return (
    <section id="workflow" className="border-b border-astra-border/60 py-24 sm:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <Reveal>
          <h2 className="max-w-lg text-3xl font-medium tracking-[-0.02em] text-astra-text-primary sm:text-4xl">
            What happens between tapping send and seeing &ldquo;Delivered&rdquo;.
          </h2>
        </Reveal>

        <ol className="mt-14 grid gap-x-8 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
          {STEPS.map((step, i) => (
            <Reveal key={step.title} delay={(i % 3) * 0.07}>
              <li className="flex gap-4">
                <span className="mt-0.5 font-mono text-sm text-astra-accent">
                  {String(i + 1).padStart(2, "0")}
                </span>
                <div>
                  <h3 className="font-medium text-astra-text-primary">{step.title}</h3>
                  <p className="mt-1.5 text-sm leading-relaxed text-astra-text-secondary">
                    {step.body}
                  </p>
                </div>
              </li>
            </Reveal>
          ))}
        </ol>
      </div>
    </section>
  );
}
