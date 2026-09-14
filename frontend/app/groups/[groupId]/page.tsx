"use client";

import { useGroup } from "@/hooks/useGroup";
import { useGroupBalances } from "@/hooks/useGroupBalances";
import { useGroupExpenses } from "@/hooks/useGroupExpenses";
import { ApiError } from "@/lib/api";
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
      <div className="flex flex-1 flex-col items-center justify-center">
        <p>Yükleniyor...</p>
      </div>
    );
  }

  if (groupQuery.isError) {
    const notAMember = groupQuery.error instanceof ApiError && groupQuery.error.status === 403;
    return (
      <div className="flex flex-1 flex-col items-center justify-center">
        <p className="text-red-600">
          {notAMember ? "Bu gruba erişim yetkin yok." : "Grup yüklenirken bir hata oluştu."}
        </p>
      </div>
    );
  }

  const group = groupQuery.data;

  return (
    <div className="mx-auto w-full max-w-2xl flex-1 px-6 py-10">
      <h1 className="mb-8 text-2xl font-semibold">{group?.name}</h1>

      <section className="mb-8">
        <h2 className="mb-3 text-lg font-medium">Masraflar</h2>
        {expensesQuery.isLoading && <p>Yükleniyor...</p>}
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
        <h2 className="mb-3 text-lg font-medium">Bakiyeler</h2>
        {balancesQuery.isLoading && <p>Yükleniyor...</p>}
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
