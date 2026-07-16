import { Button } from "@/components/ui/button";
import { Reveal } from "@/components/reveal";
import { Card, CardContent, CardDescription, CardTitle } from "@/components/ui/card";
import { Download, GitPullRequestArrow, ShieldCheck } from "lucide-react";

const REPO_URL = "https://github.com/Boredooms/AstraMesh";

export function DownloadSection() {
  return (
    <section id="download" className="py-24 sm:py-32">
      <div className="mx-auto max-w-6xl px-6">
        <Reveal>
          <h2 className="max-w-lg text-3xl font-medium tracking-[-0.02em] text-astra-text-primary sm:text-4xl">
            Get the app on your phone.
          </h2>
          <p className="mt-4 max-w-xl text-astra-text-secondary">
            The APK is debug-signed by Android&apos;s default keystore, so it installs directly
            after enabling &ldquo;install from unknown sources&rdquo; — no separate signing
            step. minSdk 26, targetSdk 35.
          </p>
        </Reveal>

        <div className="mt-12 grid gap-6 sm:grid-cols-2">
          <Reveal>
            <Card className="h-full border-astra-accent/25 bg-astra-accent/5">
              <CardContent className="flex h-full flex-col pt-6">
                <Download className="size-5 text-astra-accent" />
                <CardTitle className="mt-4">Latest release</CardTitle>
                <CardDescription className="mt-2">
                  The permanent download path. Every tagged version (v0.1.0 and up) publishes
                  an installable APK as a GitHub Release asset.
                </CardDescription>
                <Button asChild className="mt-6 w-fit" variant="accent">
                  <a href={`${REPO_URL}/releases/latest`} target="_blank" rel="noreferrer">
                    Open latest release
                  </a>
                </Button>
              </CardContent>
            </Card>
          </Reveal>

          <Reveal delay={0.08}>
            <Card className="h-full">
              <CardContent className="flex h-full flex-col pt-6">
                <GitPullRequestArrow className="size-5 text-astra-text-secondary" />
                <CardTitle className="mt-4">Latest build (CI artifact)</CardTitle>
                <CardDescription className="mt-2">
                  Every push to <code className="text-astra-text-secondary">main</code> builds
                  and tests fresh. Grab the <code>astramesh-debug-apk</code> artifact from the
                  most recent successful run — expires after 90 days.
                </CardDescription>
                <Button asChild className="mt-6 w-fit" variant="outline">
                  <a href={`${REPO_URL}/actions`} target="_blank" rel="noreferrer">
                    Open GitHub Actions
                  </a>
                </Button>
              </CardContent>
            </Card>
          </Reveal>
        </div>

        <Reveal delay={0.12}>
          <div className="mt-8 flex items-start gap-3 rounded-xl border border-astra-border bg-astra-panel/40 p-4 text-sm text-astra-text-secondary">
            <ShieldCheck className="mt-0.5 size-4 shrink-0 text-astra-text-disabled" />
            <p>
              Bluetooth LE requires <code className="text-astra-text-secondary">BLUETOOTH_SCAN</code>,{" "}
              <code className="text-astra-text-secondary">BLUETOOTH_ADVERTISE</code>, and{" "}
              <code className="text-astra-text-secondary">BLUETOOTH_CONNECT</code> at runtime on
              Android 12+. Grant them when prompted — the mesh can&apos;t discover peers
              without them.
            </p>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
