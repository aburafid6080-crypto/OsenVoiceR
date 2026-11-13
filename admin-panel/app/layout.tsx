import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "OSEN NEXUS | Private Owner Console",
  description: "OSEN NEXUS — your private owner console for Website Studio and Android App Studio.",
  applicationName: "OSEN NEXUS",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
