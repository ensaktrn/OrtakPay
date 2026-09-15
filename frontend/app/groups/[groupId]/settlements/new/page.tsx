"use client";

import { useCreateSettlement } from "@/hooks/useCreateSettlement";
import { useGroup } from "@/hooks/useGroup";
import { extractErrorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { Group } from "@/types/groups";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { use, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

const settlementFormSchema = z.object({
  toUserId: z.string().min(1, "Kime ödediğini seç"),
  // Kept as a validated string, not z.coerce.number() - same reasoning as
  // the expense form's amount field (see its schema comment): avoids
  // useForm's single type parameter fighting the resolver's input/output
  // split that coercion introduces.
  amount: z
    .string()
    .min(1, "Tutar gerekli")
    .refine((val) => !Number.isNaN(Number(val)) && Number(val) > 0, "Tutar pozitif bir sayı olmalı"),
});

type SettlementFormValues = z.infer<typeof settlementFormSchema>;

// Next.js 15+ made `params` a Promise even in Client Component pages (see
// app/groups/[groupId]/page.tsx's comment - checked the bundled docs, not
// training data). Same use() pattern applies here.
export default function NewSettlementPage(props: PageProps<"/groups/[groupId]/settlements/new">) {
  const { groupId } = use(props.params);
  const groupQuery = useGroup(groupId);

  if (groupQuery.isLoading) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center">
        <p>Yükleniyor...</p>
      </div>
    );
  }

  if (groupQuery.isError || !groupQuery.data) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center">
        <p className="text-red-600">Grup yüklenirken bir hata oluştu.</p>
      </div>
    );
  }

  return <NewSettlementForm groupId={groupId} group={groupQuery.data} />;
}

function NewSettlementForm({ groupId, group }: { groupId: string; group: Group }) {
  const router = useRouter();
  const { user } = useAuth();
  const createSettlement = useCreateSettlement(groupId);
  const [formError, setFormError] = useState<string | null>(null);
  // Excluding yourself here is a UX default, not the real guard - the
  // backend rejects a self-settlement (InvalidSettlementException) on its
  // own regardless of what the client sends.
  const otherMembers = (group.members ?? []).filter((member) => member.userId !== user?.id);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SettlementFormValues>({
    resolver: zodResolver(settlementFormSchema),
    defaultValues: {
      toUserId: otherMembers[0]?.userId ?? "",
      amount: "",
    },
  });

  async function onSubmit(values: SettlementFormValues) {
    setFormError(null);
    try {
      await createSettlement.mutateAsync({
        toUserId: values.toUserId,
        amount: Number(values.amount),
      });
      router.push(`/groups/${groupId}`);
    } catch (err) {
      setFormError(extractErrorMessage(err));
    }
  }

  return (
    <div className="mx-auto w-full max-w-lg flex-1 px-6 py-10">
      <h1 className="mb-6 text-2xl font-semibold">Ödeme Kaydet</h1>
      {/* Deliberately no "you owe X" suggestion here: Balance only tracks a
          net position, not who-owes-whom pairwise, so any such suggestion
          would assert a certainty the backend doesn't actually guarantee.
          Just who and how much. */}
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <label htmlFor="toUserId" className="text-sm font-medium">
            Kime ödedin?
          </label>
          <select
            id="toUserId"
            className="rounded border border-black/20 px-3 py-2 dark:border-white/20"
            {...register("toUserId")}
          >
            {otherMembers.map((member) => (
              <option key={member.userId} value={member.userId}>
                {member.displayName ?? member.email}
              </option>
            ))}
          </select>
          {errors.toUserId && <p className="text-sm text-red-600">{errors.toUserId.message}</p>}
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="amount" className="text-sm font-medium">
            Ne kadar?
          </label>
          <input
            id="amount"
            type="number"
            step="0.01"
            className="rounded border border-black/20 px-3 py-2 dark:border-white/20"
            {...register("amount")}
          />
          {errors.amount && <p className="text-sm text-red-600">{errors.amount.message}</p>}
        </div>

        {formError && <p className="text-sm text-red-600">{formError}</p>}

        <button
          type="submit"
          disabled={createSettlement.isPending}
          className="rounded bg-foreground px-4 py-2 text-background disabled:opacity-50"
        >
          {createSettlement.isPending ? "Kaydediliyor..." : "Ödemeyi Kaydet"}
        </button>
      </form>
    </div>
  );
}
