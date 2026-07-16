# AstraMesh Design System

## 1. Design Goal

AstraMesh must look like a real product, not a template or AI-generated mockup.

The design should feel:
- calm
- technical
- resilient
- premium
- readable under stress
- credible for a disaster communication tool

The visual language should support the core idea:
- phone-first offline mesh communication
- optional desktop companion
- separate promotional website
- end-to-end demo clarity

---

## 2. Final Design Stack Decision

### Mobile app
Use:
- **Jetpack Compose**
- **Material 3**
- **custom design tokens**
- **system typography and spacing scale**

Reason:
- the main product is Android-first
- Compose is the cleanest path for a modern native mobile UI
- Material 3 gives accessibility, theming, and platform consistency
- a custom token layer prevents the app from looking generic

### Web app / promotional site
Use:
- **Next.js**
- **Tailwind CSS**
- **shadcn/ui**
- **Radix UI primitives**
- **Motion** for animations
- **Lenis** for smooth scrolling
- **GSAP selectively** for high-impact hero or story sequences

Reason:
- shadcn/ui is built around copy-and-paste, customizable components rather than a fixed black-box library
- Radix provides accessible low-level primitives
- Motion is a production-grade React animation library with motion components and a small footprint
- Lenis is lightweight smooth scrolling for polished websites
- GSAP is best used sparingly for complex or expressive sequences, not as the default for everything

### Final decision
- **Mobile:** Compose + Material 3 + custom token system
- **Website:** Next.js + Tailwind + shadcn/ui + Radix + Motion + Lenis
- **GSAP:** only for standout sections, not the base UI layer

---

## 3. Design Principles

1. **Monochrome first**  
   Primary palette is black, white, and grayscale.

2. **Minimal accent color**  
   Use one restrained accent only for states such as active, success, warning, or critical.

3. **High contrast**  
   Text must remain readable in low-light and emergency scenarios.

4. **No visual noise**  
   Avoid clutter, glowing gradients, overdone shadows, and random decorative effects.

5. **Functional hierarchy**  
   Important actions and statuses must be obvious immediately.

6. **Responsive and resilient**  
   The UI must work across small phones, large phones, tablets, and desktop screens.

7. **Agent-friendly structure**  
   Components, tokens, and patterns should be easy for Claude Code to generate, extend, and reuse.

---

## 4. Visual Identity

### Brand mood
AstraMesh should feel like:
- a rescue-grade communication system
- a secure distributed network tool
- a mission-control interface
- a trustworthy offline infrastructure product

### Avoid
- playful gradients
- neon overload
- random illustration packs
- generic SaaS blue dashboards
- stock “AI futuristic” clichés
- template-like landing page patterns

### Preferred look
- black background
- off-white typography
- muted borders
- subtle glass or panel separation only where needed
- clear cards
- soft motion
- strong spacing

---

## 5. Color System

### Base palette
- `#000000` — true black background
- `#0B0B0B` — elevated surface
- `#121212` — cards and panels
- `#1A1A1A` — borders / separators
- `#EDEDED` — primary text
- `#A8A8A8` — secondary text
- `#6B6B6B` — disabled / helper text

### Accent palette
Use one accent family only.
Recommended:
- muted cyan or steel blue for web highlights
- restrained green for success states
- amber for warnings
- red only for critical failures

### Rule
The accent must never dominate the UI. The product should still look good if the accent color is removed.

---

## 6. Typography

### Mobile
Use the platform/system font or a close clean sans-serif.

### Web
Use a modern sans-serif system stack or a neutral geometric UI font.

### Typographic hierarchy
- Hero title: large, bold, compressed enough to feel confident
- Section titles: clear and medium-weight
- Body: readable and calm
- Labels: compact and low-noise
- Status text: highly legible and short

### Rule
Typography should do most of the design work, not decoration.

---

## 7. Layout System

### Core spacing scale
Use a simple spacing scale such as:
- 4
- 8
- 12
- 16
- 24
- 32
- 48
- 64

### Layout style
- grid-based on web
- stacked and card-driven on mobile
- strong alignment
- generous whitespace
- limited nested depth

### Card strategy
Cards should separate:
- peer lists
- chat threads
- file transfers
- broadcasts
- diagnostics
- technical explanation blocks

---

## 8. Mobile UI Direction

### Mobile goals
- one-hand usability
- fast message access
- strong state visibility
- low friction for discovery and sending

### Recommended mobile screens
- welcome / permission screen
- nearby peers
- chat list
- chat thread
- message compose sheet
- file sharing screen
- broadcast screen
- pending queue
- diagnostics
- settings

### Mobile interaction rules
- primary action must be obvious
- destructive actions must be rare and clearly labeled
- delivery state should be visible inline
- discovery state should be obvious at the top of the app
- empty states must explain what to do next

### Mobile visual style
- mostly monochrome
- compact cards
- clear top bars
- rounded but not bubbly corners
- subtle motion on state changes

---

## 9. Web UI Direction

### Web goals
- explain the project clearly
- show the architecture without confusion
- feel premium and technical
- help judges understand the demo fast

### Recommended web sections
- hero section
- problem statement
- solution summary
- architecture overview
- workflow explanation
- feature cards
- screenshots
- demo / video section
- build and tech stack section
- footer with links

### Web style
- editorial layout
- black-first monochrome theme
- sharp hierarchy
- limited accent use
- large readable type
- smooth scrolling
- controlled motion
- polished reveal effects

---

## 10. Component Strategy

### Mobile components
Build reusable Compose components for:
- top app bars
- peer cards
- message bubbles
- status chips
- action sheets
- empty states
- file cards
- broadcast banners
- diagnostic rows

### Web components
Use shadcn/ui and Radix-based components for:
- buttons
- dialogs
- sheets
- tabs
- dropdowns
- cards
- badges
- inputs
- toast notifications
- command palette / search

### Rule
Prefer composition over heavy custom one-off UI.

---

## 11. Motion Strategy

### Mobile motion
Use subtle Compose animations for:
- screen transitions
- state changes
- message delivery updates
- peer discovery updates

### Web motion
Use:
- **Motion** for component-level animations and state transitions
- **Lenis** for smooth scrolling
- **GSAP** only for richer hero or storytelling moments

### Motion rules
- motion must support clarity, not distract from it
- keep movement short and readable
- avoid excessive parallax
- respect reduced-motion preferences

---

## 12. Desktop / PC Design Direction

PC support is optional, but when present it should look like a control and observability surface.

### Desktop goals
- show incoming messages clearly
- display peer health and relay state
- act like a local dashboard or relay console
- remain visually consistent with the mobile product

### Desktop layout suggestions
- left navigation rail
- peer status panel
- message stream panel
- diagnostics sidebar
- log/history drawer

### Desktop tone
- mission control
- operations dashboard
- technical and calm
- not flashy

---

## 13. Design Tokens

### Token categories
- color
- spacing
- radius
- elevation
- typography
- motion duration
- motion easing

### Example token intent
- radius small: 8
- radius medium: 12
- radius large: 16
- radius xl: 24

### Rule
Tokens must be shared as much as possible so the app and website feel like one family.

---

## 14. Design File System

Recommended design-related files:

```text
/docs
├─ design.md
├─ architecture.md
├─ workflow.md
├─ protocol.md
├─ routing.md
├─ implementation.md
└─ tasks.md

/web
├─ app/
├─ components/
├─ lib/
├─ styles/
├─ public/
└─ screenshots/

/app
├─ src/
│  ├─ design/
│  ├─ ui/
│  ├─ theme/
│  ├─ screens/
│  └─ components/
```

### Rule
Design tokens and components should not be scattered randomly. Keep them discoverable and reusable.

---

## 15. Accessibility Rules

- maintain strong contrast
- support keyboard navigation on web
- support focus states everywhere
- use semantic structure
- make touch targets large enough
- keep motion optional where possible
- ensure text is readable on bright and dark environments

Accessibility is not optional, especially for emergency communication software.

---

## 16. Promotional Website Direction

The website should not look like a generated landing page.

### It should instead feel like
- a serious product launch page
- a technical product story
- a polished hackathon submission page

### Website visual system
- black surfaces
- monochrome text
- carefully placed accent color
- strong grids
- restrained motion
- clean cards and sections

### Website motion pattern
- fade + rise on scroll
- hover states on cards and buttons
- subtle sticky header behavior
- elegant section transitions

---

## 17. Component Libraries Decision

### Use shadcn/ui when
- you need a polished website component you can own and customize
- you want a code-first, copy-and-paste component workflow
- you need a good base for buttons, dialogs, tabs, forms, and panels

### Use Radix when
- you need accessible behavior primitives
- you want custom styling control
- you need dialogs, tabs, dropdowns, sliders, and overlays that behave correctly

### Use Motion when
- you need React component animations
- you want layout transitions or scroll-linked effects
- you want production-grade motion with small footprint

### Use Lenis when
- you want smooth scrolling for the web story page
- you want a more polished, premium feel without heavy visual clutter

### Use GSAP when
- the hero section needs a standout sequence
- you need advanced controlled animation
- you want a high-impact but limited storytelling element

### Final rule
Do not use every library everywhere. Use the right tool for each layer.

---

## 18. Anti–AI-Slop Rules

- no random glows
- no meaningless gradients
- no overloaded dashboards
- no cluttered cards
- no decorative motion without purpose
- no template-like hero sections
- no generic stock UI patterns
- no inconsistent button styles
- no copied landing page layouts

Every element must justify its presence.

---

## 19. Implementation Priority

1. define tokens and theme
2. build mobile app shell
3. build web design system
4. create shared component patterns
5. add animation rules
6. build screenshots and story sections
7. refine with real device testing

---

## 20. Final Decision Summary

- **Android app:** Jetpack Compose + Material 3 + custom monochrome design tokens
- **Website:** Next.js + Tailwind + shadcn/ui + Radix + Motion + Lenis
- **GSAP:** selective use only for high-impact sections
- **Theme:** black-first monochrome with restrained accent
- **Style:** premium, technical, calm,