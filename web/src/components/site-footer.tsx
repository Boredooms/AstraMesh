import Link from "next/link";

export function SiteFooter() {
  return (
    <footer className="border-t border-astra-border/60 py-10">
      <div className="mx-auto flex max-w-6xl flex-col items-start justify-between gap-4 px-6 text-sm text-astra-text-disabled sm:flex-row sm:items-center">
        <p>AstraMesh — offline mesh communication. No servers, by design.</p>
        <div className="flex gap-6">
          <Link
            href="https://github.com/Boredooms/AstraMesh"
            className="transition-colors hover:text-astra-text-secondary"
          >
            GitHub
          </Link>
          <Link
            href="https://github.com/Boredooms/AstraMesh/tree/main/docs"
            className="transition-colors hover:text-astra-text-secondary"
          >
            Docs
          </Link>
        </div>
      </div>
    </footer>
  );
}
