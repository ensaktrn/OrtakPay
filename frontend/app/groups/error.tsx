"use client"; // Error boundaries must be Client Components

import { Button } from "@/components/ui/button";
import { useEffect } from "react";

export default function GroupsError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
      <h1 className="font-heading text-2xl font-bold">Bir şeyler ters gitti</h1>
      <p className="text-muted-foreground">Gruplar yüklenirken beklenmeyen bir hata oluştu.</p>
      <Button className="mt-2" onClick={reset}>
        Tekrar Dene
      </Button>
    </div>
  );
}
