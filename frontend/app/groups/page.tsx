"use client";

import { Skeleton } from "@/components/skeleton";
import { useGroups } from "@/hooks/useGroups";
import Link from "next/link";

export default function GroupsPage() {
  const { data: groups, isLoading, isError } = useGroups();

  return (
    <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
      <h1 className="mb-6 text-2xl font-semibold">Gruplar</h1>

      {isLoading && (
        <ul className="flex flex-col gap-3">
          {[0, 1, 2].map((i) => (
            <li key={i} className="rounded border border-black/10 px-4 py-3 dark:border-white/10">
              <Skeleton className="mb-2 h-5 w-1/2" />
              <Skeleton className="h-4 w-1/4" />
            </li>
          ))}
        </ul>
      )}
      {isError && <p className="text-red-600">Gruplar yüklenirken bir hata oluştu.</p>}
      {groups && groups.length === 0 && <p className="text-zinc-600 dark:text-zinc-400">Henüz bir gruba üye değilsin.</p>}

      <ul className="flex flex-col gap-3">
        {groups?.map((group) => (
          <li key={group.id}>
            <Link
              href={`/groups/${group.id}`}
              className="block rounded border border-black/10 px-4 py-3 hover:bg-black/[.03] dark:border-white/10 dark:hover:bg-white/[.05]"
            >
              <p className="font-medium">{group.name}</p>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">{group.memberCount} üye</p>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
