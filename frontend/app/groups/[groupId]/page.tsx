"use client";

import { ListRowSkeleton } from "@/components/list-row-skeleton";
import { NotFoundMessage } from "@/components/not-found-message";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useGroup } from "@/hooks/useGroup";
import { useGroupBalances } from "@/hooks/useGroupBalances";
import { useGroupExpenses } from "@/hooks/useGroupExpenses";
import { ApiError } from "@/lib/api";
import Link from "next/link";
import { use, useEffect, useRef } from "react";
import { toast } from "sonner";

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

  const hasToastedExpensesError = useRef(false);
  const hasToastedBalancesError = useRef(false);

  useEffect(() => {
    if (expensesQuery.isError && !hasToastedExpensesError.current) {
      hasToastedExpensesError.current = true;
      toast.error("Masraflar yüklenirken bir hata oluştu");
    }
  }, [expensesQuery.isError]);

  useEffect(() => {
    if (balancesQuery.isError && !hasToastedBalancesError.current) {
      hasToastedBalancesError.current = true;
      toast.error("Bakiyeler yüklenirken bir hata oluştu");
    }
  }, [balancesQuery.isError]);

  if (groupQuery.isLoading) {
    return (
      <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
        <Skeleton className="mb-8 h-8 w-1/2" />
        <Skeleton className="mb-3 h-6 w-24" />
        <ListRowSkeleton />
        <ListRowSkeleton />
        <Skeleton className="mt-8 mb-3 h-6 w-24" />
        <ListRowSkeleton />
      </div>
    );
  }

  if (groupQuery.isError) {
    const notAMember = groupQuery.error instanceof ApiError && groupQuery.error.status === 403;
    if (notAMember) {
      return (
        <div className="flex flex-1 flex-col items-center justify-center">
          <p className="text-muted-foreground">Bu gruba erişim yetkin yok.</p>
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
        <h1 className="font-heading text-2xl font-bold">{group?.name}</h1>
        <Button asChild className="self-start sm:self-auto">
          <Link href={`/groups/${groupId}/expenses/new`}>Yeni Masraf Ekle</Link>
        </Button>
      </div>

      <section className="mb-8">
        <h2 className="mb-2 text-lg font-medium">Masraflar</h2>
        {expensesQuery.isLoading && (
          <div>
            <ListRowSkeleton />
            <ListRowSkeleton />
          </div>
        )}
        {expensesQuery.data?.content?.length === 0 && (
          <p className="py-4 text-sm text-muted-foreground">Henüz masraf yok.</p>
        )}
        <ul className="divide-y divide-border">
          {expensesQuery.data?.content?.map((expense) => (
            <li key={expense.id} className="flex items-center justify-between gap-4 py-4">
              <div className="min-w-0">
                <p className="truncate font-medium">{expense.description}</p>
                <p className="text-sm text-muted-foreground">{expense.paidByDisplayName} ödedi</p>
              </div>
              <span className="shrink-0 tabular-nums font-medium">{expense.amount?.toFixed(2)} TL</span>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-lg font-medium">Bakiyeler</h2>
          <Button variant="outline" size="sm" asChild>
            <Link href={`/groups/${groupId}/settlements/new`}>Ödeme Kaydet</Link>
          </Button>
        </div>
        {balancesQuery.isLoading && (
          <div>
            <ListRowSkeleton />
            <ListRowSkeleton />
          </div>
        )}
        {balancesQuery.data?.length === 0 && <p className="py-4 text-sm text-muted-foreground">Henüz bakiye yok.</p>}
        <ul className="divide-y divide-border">
          {balancesQuery.data?.map((balance) => {
            const net = balance.netAmount ?? 0;
            const label = net > 0 ? "alacaklı" : net < 0 ? "borçlu" : "eşit";
            const colorClass = net > 0 ? "text-credit" : net < 0 ? "text-debit" : "text-muted-foreground";
            return (
              <li key={balance.userId} className="flex items-center justify-between gap-4 py-4">
                <span className="font-medium">{balance.displayName}</span>
                <span className={`shrink-0 tabular-nums font-medium ${colorClass}`}>
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
