"use client";

import { useAuth } from "@/lib/auth-context";
import Link from "next/link";

export function Header() {
  const { status, logout } = useAuth();
  const isAuthenticated = status === "authenticated";

  return (
    <header className="flex items-center justify-between border-b border-black/10 px-6 py-4 dark:border-white/10">
      <Link href="/" className="font-semibold">
        OrtakPay
      </Link>
      {isAuthenticated ? (
        <button onClick={logout} className="text-sm font-medium underline">
          Çıkış Yap
        </button>
      ) : (
        <Link href="/login" className="text-sm font-medium underline">
          Giriş Yap
        </Link>
      )}
    </header>
  );
}
