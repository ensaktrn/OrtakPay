import { Skeleton } from "@/components/ui/skeleton";

export function ListRowSkeleton() {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-border py-4 last:border-b-0">
      <Skeleton className="h-4 w-2/5" />
      <Skeleton className="h-4 w-16" />
    </div>
  );
}
