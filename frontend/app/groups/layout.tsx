"use client";

import { Skeleton } from "@/components/skeleton";
import { useAuth } from "@/lib/auth-context";
import { useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";

export default function GroupsLayout({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const router = useRouter();

  // useEffect burada gerekli çünkü yönlendirme (router.replace) bir yan
  // etkidir - render fonksiyonu sırasında çağrılırsa "component render
  // ederken başka bir component'i güncelleme" hatası verir. "idle"/"error"
  // durumunda (token yok ya da geçersiz çıktı) çalışır; "loading" sırasında
  // henüz karar verilmediği için bekler.
  useEffect(() => {
    if (status === "idle" || status === "error") {
      router.replace("/login");
    }
  }, [status, router]);

  // "loading" means a token was found and /me is being verified (see
  // lib/auth-context.tsx) - a skeleton here avoids a blank flash on every
  // hard refresh of a protected page while that check is in flight.
  if (status === "loading") {
    return (
      <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
        <Skeleton className="mb-6 h-8 w-40" />
        <Skeleton className="h-20 w-full" />
      </div>
    );
  }

  if (status !== "authenticated") {
    return null;
  }

  return <>{children}</>;
}
