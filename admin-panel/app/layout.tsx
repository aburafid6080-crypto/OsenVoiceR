import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "OCEAN ADMIN PANEL",
  description: "OCEAN ADMIN PANEL — private owner dashboard for separate Website and Android workspaces.",
  applicationName: "OCEAN ADMIN PANEL",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
