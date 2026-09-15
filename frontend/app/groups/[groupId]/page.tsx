"use client";

import { NotFoundMessage } from "@/components/not-found-message";
import { Skeleton } from "@/components/skeleton";
import { useGroup } from "@/hooks/useGroup";
import { useGroupBalances } from "@/hooks/useGroupBalances";
import { useGroupExpenses } from "@/hooks/useGroupExpenses";
import { ApiError } from "@/lib/api";
import Link from "next/link";
import { use } from "react";

// Next.js 15+ made `params` a Promise, including in Client Component pages
// (checked node_modules/next/dist/docs/01-app/03-api-reference/03-file-conventions/dynamic-routes.md
// rather than trusting training data, which mostly predates this and still
// expects the old synchronous `{ params: { groupId } }` shape). This
// component can't be an async function (it needs hooks), so `await` isn't
// an option - React's `use()` is the documented way to unwrap a promise
// prop in a Client Component instead.
export default function GroupDetailPage(props: PageProps<"/groups/[groupId]">) {
  const { groupId } = use(props.params);

  const groupQuery = useGroup(groupId);
  const expensesQuery = useGroupExpenses(groupId);
  const balancesQuery = useGroupBalances(groupId);

  if (groupQuery.isLoading) {
    return (
      <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
        <Skeleton className="mb-8 h-8 w-1/2" />
        <Skeleton className="mb-3 h-6 w-24" />
        <Skeleton className="mb-2 h-16 w-full" />
        <Skeleton className="mb-8 h-16 w-full" />
        <Skeleton className="mb-3 h-6 w-24" />
        <Skeleton className="h-16 w-full" />
      </div>
    );
  }

  if (groupQuery.isError) {
    const notAMember = groupQuery.error instanceof ApiError && groupQuery.error.status === 403;
    if (notAMember) {
      return (
        <div className="flex flex-1 flex-col items-center justify-center">
          <p className="text-red-600">Bu gruba erişim yetkin yok.</p>
        </div>
      );
    }
    // Everything else (a real 404, or - a backend quirk - a malformed
    // groupId coming back as 401 instead of 400, since @PathVariable UUID
    // conversion failures aren't routed through GlobalExceptionHandler) is
    // treated as "this group doesn't exist" rather than a generic error,
    // since from the user's perspective both look the same: there's nothing
    // here to show.
    return <NotFoundMessage title="Grup bulunamadı" description="Aradığın grup mevcut değil ya da kaldırılmış." />;
  }

  const group = groupQuery.data;

  return (
    <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
      <div className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-semibold">{group?.name}</h1>
        <Link
          href={`/groups/${groupId}/expenses/new`}
          className="shrink-0 self-start rounded bg-foreground px-4 py-2 text-sm whitespace-nowrap text-background sm:self-auto"
        >
          Yeni Masraf Ekle
        </Link>
      </div>

      <section className="mb-8">
        <h2 className="mb-3 text-lg font-medium">Masraflar</h2>
        {expensesQuery.isLoading && (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        )}
        {expensesQuery.isError && <p className="text-red-600">Masraflar yüklenirken bir hata oluştu.</p>}
        {expensesQuery.data?.content?.length === 0 && (
          <p className="text-zinc-600 dark:text-zinc-400">Henüz masraf yok.</p>
        )}
        <ul className="flex flex-col gap-2">
          {expensesQuery.data?.content?.map((expense) => (
            <li key={expense.id} className="rounded border border-black/10 px-4 py-3 dark:border-white/10">
              <p className="font-medium">{expense.description}</p>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">
                {expense.paidByDisplayName} ödedi · {expense.amount?.toFixed(2)} TL
              </p>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-medium">Bakiyeler</h2>
          <Link
            href={`/groups/${groupId}/settlements/new`}
            className="rounded bg-foreground px-4 py-2 text-sm text-background"
          >
            Ödeme Kaydet
          </Link>
        </div>
        {balancesQuery.isLoading && (
          <div className="flex flex-col gap-2">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        )}
        {balancesQuery.isError && <p className="text-red-600">Bakiyeler yüklenirken bir hata oluştu.</p>}
        {balancesQuery.data?.length === 0 && <p className="text-zinc-600 dark:text-zinc-400">Henüz bakiye yok.</p>}
        <ul className="flex flex-col gap-2">
          {balancesQuery.data?.map((balance) => {
            const net = balance.netAmount ?? 0;
            const label = net > 0 ? "alacaklı" : net < 0 ? "borçlu" : "eşit";
            const colorClass = net > 0 ? "text-green-600" : net < 0 ? "text-red-600" : "text-zinc-600";
            return (
              <li
                key={balance.userId}
                className="flex items-center justify-between rounded border border-black/10 px-4 py-3 dark:border-white/10"
              >
                <span>{balance.displayName}</span>
                <span className={colorClass}>
                  {label} · {net.toFixed(2)} TL
                </span>
              </li>
            );
          })}
        </ul>
      </section>
    </div>
  );
}
