import { Button } from "@/components/ui/button";
import Link from "next/link";

export function NotFoundMessage({ title, description }: { title: string; description: string }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
      <h1 className="font-heading text-2xl font-bold">{title}</h1>
      <p className="text-muted-foreground">{description}</p>
      <Button className="mt-2" asChild>
        <Link href="/">Ana Sayfaya Dön</Link>
      </Button>
    </div>
  );
}
