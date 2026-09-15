import Link from "next/link";

export function NotFoundMessage({ title, description }: { title: string; description: string }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 text-center">
      <h1 className="text-2xl font-semibold">{title}</h1>
      <p className="text-zinc-600 dark:text-zinc-400">{description}</p>
      <Link href="/" className="mt-2 rounded bg-foreground px-4 py-2 text-sm text-background">
        Ana Sayfaya Dön
      </Link>
    </div>
  );
}
