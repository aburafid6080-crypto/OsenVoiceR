import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "OSEN BOOSTVOX RX | Admin Panel",
  description: "OSEN BOOSTVOX RX private Admin Panel for Website Studio and Android App Studio.",
  applicationName: "OSEN BOOSTVOX RX Admin Panel",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
