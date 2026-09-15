import { NotFoundMessage } from "@/components/not-found-message";

// Per Next.js's not-found.js convention (checked
// node_modules/next/dist/docs/.../not-found.md): a root app/not-found.tsx
// handles any URL that doesn't match a route at all - no client hooks
// needed here, so this stays a Server Component.
export default function NotFound() {
  return <NotFoundMessage title="Sayfa bulunamadı" description="Aradığın sayfa mevcut değil." />;
}
