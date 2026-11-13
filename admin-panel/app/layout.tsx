import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "OSEN NEXUS | Admin Panel",
  description: "OSEN NEXUS private Admin Panel for Website Studio and Android App Studio.",
  applicationName: "OSEN NEXUS Admin Panel",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
