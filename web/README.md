# AstraMesh — promotional website

The `astramesh.dev` marketing site: hero, problem/solution, architecture overview, message
lifecycle, screenshots, tech stack, and the APK download section. Deliberately kept separate
from the Android app — see [`docs/architecture.md`](../docs/architecture.md) §19.

## Stack

- [Next.js](https://nextjs.org) (App Router) + TypeScript
- Tailwind CSS v4
- shadcn/ui-style components (hand-authored in `src/components/ui`, Radix primitives underneath)
- [Motion](https://motion.dev) for scroll-reveal animation
- [Lenis](https://lenis.darkroom.engineering) for smooth scrolling
- Design tokens shared with the mobile app's `core-ui` module (`src/app/globals.css`) — see
  [`docs/design.md`](../docs/design.md) §5

## Develop

```bash
npm install
npm run dev      # http://localhost:3000
npm run build    # production build
npm run lint
```

## Structure

```
src/
├─ app/                 # App Router entry (layout, page, globals.css)
├─ components/
│  ├─ ui/                # hand-authored shadcn-style primitives (button, card, tabs, dialog...)
│  ├─ sections/           # page sections (hero, problem, solution, architecture, ...)
│  ├─ mesh-visual.tsx      # live canvas mesh/relay animation
│  ├─ reveal.tsx           # scroll-triggered fade+rise wrapper (Motion)
│  └─ smooth-scroll-provider.tsx  # Lenis setup
└─ lib/utils.ts          # cn() class merge helper
```

CI: [`.github/workflows/website.yml`](../.github/workflows/website.yml) builds this project on
every push that touches `web/**`.
