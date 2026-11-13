import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = { title: "OSEN NEXUS • Owner Console", description: "Private owner control center for OSEN TURBOPLAY" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="en"><body>{children}</body></html>; }
