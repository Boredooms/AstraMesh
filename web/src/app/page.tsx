import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
import { Hero } from "@/components/sections/hero";
import { Problem } from "@/components/sections/problem";
import { Solution } from "@/components/sections/solution";
import { Architecture } from "@/components/sections/architecture";
import { Workflow } from "@/components/sections/workflow";
import { Screenshots } from "@/components/sections/screenshots";
import { Stack } from "@/components/sections/stack";
import { DownloadSection } from "@/components/sections/download";

export default function Home() {
  return (
    <div className="flex flex-1 flex-col">
      <SiteHeader />
      <main className="flex-1">
        <Hero />
        <Problem />
        <Solution />
        <Architecture />
        <Workflow />
        <Screenshots />
        <Stack />
        <DownloadSection />
      </main>
      <SiteFooter />
    </div>
  );
}
