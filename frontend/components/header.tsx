"use client";

import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth-context";
import Link from "next/link";

export function Header() {
  const { status, user, logout } = useAuth();
  const isAuthenticated = status === "authenticated";

  return (
    <header className="flex items-center justify-between gap-3 border-b border-border px-4 py-3 sm:px-6 sm:py-4">
      <Link href="/groups" className="font-heading text-lg font-bold shrink-0">
        OrtakPay
      </Link>
      {isAuthenticated ? (
        <div className="flex min-w-0 items-center gap-3">
          <span className="hidden min-w-0 truncate text-sm text-muted-foreground sm:inline">
            {user?.displayName}
          </span>
          <Button variant="ghost" size="sm" onClick={logout}>
            Çıkış Yap
          </Button>
        </div>
      ) : (
        <Button variant="ghost" size="sm" asChild>
          <Link href="/login">Giriş Yap</Link>
        </Button>
      )}
    </header>
  );
}
