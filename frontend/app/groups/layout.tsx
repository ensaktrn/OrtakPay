"use client";

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

  if (status !== "authenticated") {
    return null;
  }

  return <>{children}</>;
}
