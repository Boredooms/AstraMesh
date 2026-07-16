import { Reveal } from "@/components/reveal";
import { Card, CardContent, CardDescription, CardTitle } from "@/components/ui/card";
import {
  Bluetooth,
  KeyRound,
  Radio,
  RefreshCw,
  ShieldCheck,
  Split,
} from "lucide-react";

const FEATURES = [
  {
    icon: Bluetooth,
    title: "Bluetooth LE discovery",
    body: "Every device advertises and scans simultaneously — there's no fixed access point, so any two nearby phones can find each other directly.",
  },
  {
    icon: KeyRound,
    title: "ECDH handshake",
    body: "Phones exchange public keys on first contact (HELLO / HELLO_ACK) and derive a shared AES-256 session key locally. The private key never leaves the device.",
  },
  {
    icon: ShieldCheck,
    title: "Encrypted, always",
    body: "Every chat payload is sealed with AES-GCM under that session key before it touches the radio. A relay node forwards ciphertext it cannot read.",
  },
  {
    icon: Split,
    title: "Epidemic relay",
    body: "Messages hop through intermediate devices with deduplication and a bounded TTL, so delivery doesn't need a direct link — just a path.",
  },
  {
    icon: RefreshCw,
    title: "Store-and-forward",
    body: "If the next hop is offline, the packet is queued locally — still encrypted — and retried automatically the moment that peer reappears.",
  },
  {
    icon: Radio,
    title: "Delivery you can see",
    body: "Pending, sent, relayed, delivered, or failed: every message shows its real state, derived from an actual ACK round-trip through the mesh.",
  },
];

export function Solution() {
  return (
    <section className="border-b border-astra-border/60 py-24 sm:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <Reveal>
          <h2 className="max-w-lg text-3xl font-medium tracking-[-0.02em] text-astra-text-primary sm:text-4xl">
            A protocol built for hops, not hosts.
          </h2>
        </Reveal>

        <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, body }, i) => (
            <Reveal key={title} delay={(i % 3) * 0.06}>
              <Card className="h-full">
                <CardContent className="pt-6">
                  <Icon className="size-5 text-astra-accent" />
                  <CardTitle className="mt-4">{title}</CardTitle>
                  <CardDescription className="mt-2">{body}</CardDescription>
                </CardContent>
              </Card>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
