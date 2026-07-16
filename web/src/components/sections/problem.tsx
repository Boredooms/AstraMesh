import { Reveal } from "@/components/reveal";
import { CloudOff, ServerCrash, SignalZero } from "lucide-react";

const POINTS = [
  {
    icon: SignalZero,
    title: "Cellular and Wi-Fi both fail together",
    body: "Disasters, blackouts, and remote terrain take out the cell towers and the Wi-Fi at the same time — the two networks most chat apps assume will always be there.",
  },
  {
    icon: ServerCrash,
    title: "A central server is a single point of failure",
    body: "If the app needs a backend to relay a message between two phones sitting next to each other, the app doesn't work exactly when it's needed most.",
  },
  {
    icon: CloudOff,
    title: "Store-and-forward isn't optional, it's the point",
    body: "A message sent while someone is briefly out of range shouldn't just fail. It should wait, and arrive the moment a route reappears.",
  },
];

export function Problem() {
  return (
    <section id="problem" className="border-b border-astra-border/60 py-24 sm:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <Reveal>
          <h2 className="max-w-lg text-3xl font-medium tracking-[-0.02em] text-astra-text-primary sm:text-4xl">
            Most chat apps assume the internet is always there.
          </h2>
          <p className="mt-4 max-w-xl text-astra-text-secondary">
            AstraMesh doesn&apos;t. It was built on the opposite assumption: the network you can
            trust is the one made of the people standing near you.
          </p>
        </Reveal>

        <div className="mt-16 grid gap-8 sm:grid-cols-3">
          {POINTS.map(({ icon: Icon, title, body }, i) => (
            <Reveal key={title} delay={i * 0.08}>
              <Icon className="size-5 text-astra-text-secondary" />
              <h3 className="mt-4 font-medium text-astra-text-primary">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-astra-text-secondary">
                {body}
              </p>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
