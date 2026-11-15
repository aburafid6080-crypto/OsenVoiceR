import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "OSEN AUREX | Admin Panel",
  description: "OSEN AUREX private Admin Panel for Website Studio and Android App Studio.",
  applicationName: "OSEN AUREX Admin Panel",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
