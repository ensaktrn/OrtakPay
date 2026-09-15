"use client"; // Error boundaries must be Client Components

import { useEffect } from "react";

// Next.js 16.3 stabilized a newer `retry` prop (re-fetches and re-renders the
// segment) and now recommends it over `reset` for most cases (checked
// node_modules/next/dist/docs/.../error.md rather than assuming from older
// training data) - `reset` just clears the error boundary's state without
// re-fetching. Kept as `reset` here since nothing on this page depends on
// fresh server data; it's still a fully supported prop, not deprecated.
export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
      <h1 className="text-2xl font-semibold">Bir şeyler ters gitti</h1>
      <p className="text-zinc-600 dark:text-zinc-400">Sayfa yüklenirken beklenmeyen bir hata oluştu.</p>
      <button onClick={reset} className="mt-2 rounded bg-foreground px-4 py-2 text-sm text-background">
        Tekrar Dene
      </button>
    </div>
  );
}
