import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { SmoothScrollProvider } from "@/components/smooth-scroll-provider";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const SITE_URL = "https://astramesh.dev";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "AstraMesh — offline mesh communication",
    template: "%s · AstraMesh",
  },
  description:
    "A phone-first, offline mesh communication system. Every Android device becomes a relay node for encrypted chat, file sharing, and emergency coordination — no internet, no server.",
  keywords: [
    "mesh network",
    "offline messaging",
    "bluetooth mesh",
    "disaster communication",
    "emergency broadcast",
    "peer to peer",
    "Android",
  ],
  openGraph: {
    title: "AstraMesh — offline mesh communication",
    description:
      "Every device becomes a node. Encrypted chat, file sharing, and emergency broadcast with no internet and no central server.",
    url: SITE_URL,
    siteName: "AstraMesh",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "AstraMesh — offline mesh communication",
    description:
      "Every device becomes a node. Encrypted chat, file sharing, and emergency broadcast with no internet and no central server.",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased dark`}
    >
      <body className="min-h-full flex flex-col bg-astra-black text-astra-text-primary">
        <SmoothScrollProvider>{children}</SmoothScrollProvider>
      </body>
    </html>
  );
}
