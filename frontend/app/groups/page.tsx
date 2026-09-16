"use client";

import { ListRowSkeleton } from "@/components/list-row-skeleton";
import { Button } from "@/components/ui/button";
import { useGroups } from "@/hooks/useGroups";
import Link from "next/link";
import { toast } from "sonner";
import { useEffect, useRef } from "react";

export default function GroupsPage() {
  const { data: groups, isLoading, isError } = useGroups();
  const hasToastedError = useRef(false);

  useEffect(() => {
    if (isError && !hasToastedError.current) {
      hasToastedError.current = true;
      toast.error("Gruplar yüklenirken bir hata oluştu");
    }
  }, [isError]);

  return (
    <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
      <div className="mb-6 flex items-center justify-between gap-4">
        <h1 className="font-heading text-2xl font-bold">Gruplarım</h1>
        {groups && groups.length > 0 && (
          <Button asChild>
            <Link href="/groups/new">Yeni Grup</Link>
          </Button>
        )}
      </div>

      {isLoading && (
        <div>
          <ListRowSkeleton />
          <ListRowSkeleton />
          <ListRowSkeleton />
        </div>
      )}

      {groups && groups.length === 0 && (
        <div className="flex flex-col items-center justify-center gap-4 py-20 text-center">
          <p className="text-muted-foreground">Henüz bir gruba üye değilsin.</p>
          <Button size="lg" className="h-12 px-8 text-base" asChild>
            <Link href="/groups/new">İlk Grubunu Oluştur</Link>
          </Button>
        </div>
      )}

      {groups && groups.length > 0 && (
        <ul className="divide-y divide-border">
          {groups.map((group) => (
            <li key={group.id}>
              <Link
                href={`/groups/${group.id}`}
                className="-mx-2 flex items-center justify-between gap-4 rounded-md px-2 py-4 transition-colors not-disabled:hover:bg-muted/50"
              >
                <span className="font-medium">{group.name}</span>
                <span className="shrink-0 text-sm tabular-nums text-muted-foreground">{group.memberCount} üye</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
