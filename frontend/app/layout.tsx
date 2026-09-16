import { Header } from "@/components/header";
import { Toaster } from "@/components/ui/sonner";
import { AuthProvider } from "@/lib/auth-context";
import { QueryProvider } from "@/lib/query-client";
import type { Metadata } from "next";
import { Fraunces, Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const fraunces = Fraunces({
  variable: "--font-fraunces",
  subsets: ["latin"],
  weight: ["600", "700"],
});

export const metadata: Metadata = {
  title: "OrtakPay",
  description: "Masraf paylaşım platformu",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} ${fraunces.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        {/* QueryClientProvider wraps AuthProvider (not the reverse): AuthProvider
            calls useCurrentUser() - a useQuery hook - internally, which requires
            a QueryClientProvider ancestor. It's also the right long-term shape:
            public, non-authenticated queries can use TanStack Query without
            ever needing to sit inside AuthProvider. */}
        <QueryProvider>
          <AuthProvider>
            <Header />
            {children}
          </AuthProvider>
        </QueryProvider>
        <Toaster position="bottom-right" />
      </body>
    </html>
  );
}
