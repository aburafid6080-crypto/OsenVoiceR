import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = { title: "OSEN TURBOPLAY • Admin", description: "Owner-only OSEN TURBOPLAY control panel" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="en"><body>{children}</body></html>; }
