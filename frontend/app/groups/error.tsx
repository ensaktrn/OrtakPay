"use client"; // Error boundaries must be Client Components

import { useEffect } from "react";

export default function GroupsError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
      <h1 className="text-2xl font-semibold">Bir şeyler ters gitti</h1>
      <p className="text-zinc-600 dark:text-zinc-400">Gruplar yüklenirken beklenmeyen bir hata oluştu.</p>
      <button onClick={reset} className="mt-2 rounded bg-foreground px-4 py-2 text-sm text-background">
        Tekrar Dene
      </button>
    </div>
  );
}
