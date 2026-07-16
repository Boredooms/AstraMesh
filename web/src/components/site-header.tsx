import Link from "next/link";
import { Button } from "@/components/ui/button";

const NAV_LINKS = [
  { href: "#problem", label: "Problem" },
  { href: "#architecture", label: "Architecture" },
  { href: "#workflow", label: "How it works" },
  { href: "#stack", label: "Stack" },
];

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-astra-border/60 bg-astra-black/80 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
        <Link href="/" className="flex items-center gap-2 font-medium tracking-tight">
          <span className="inline-block h-2 w-2 rounded-full bg-astra-accent" />
          AstraMesh
        </Link>
        <nav className="hidden items-center gap-8 text-sm text-astra-text-secondary md:flex">
          {NAV_LINKS.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="transition-colors hover:text-astra-text-primary"
            >
              {link.label}
            </a>
          ))}
        </nav>
        <Button asChild size="sm" variant="outline">
          <a href="#download">Download APK</a>
        </Button>
      </div>
    </header>
  );
}
