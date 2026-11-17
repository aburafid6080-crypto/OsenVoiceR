import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = { title: "OSEN BOOSTVOX RX | Power Beyond Limits", description: "Discover OSEN BOOSTVOX RX — a gaming companion concept bringing voice tools and game utilities together.", metadataBase: new URL("https://example.com") };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="en"><body>{children}</body></html>; }
