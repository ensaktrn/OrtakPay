"use client";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { useCreateSettlement } from "@/hooks/useCreateSettlement";
import { useGroup } from "@/hooks/useGroup";
import { extractErrorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { Group } from "@/types/groups";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { use } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
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
      <div className="mx-auto w-full max-w-lg flex-1 px-6 py-10">
        <Skeleton className="mb-6 h-8 w-1/2" />
        <Skeleton className="mb-4 h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    );
  }

  if (groupQuery.isError || !groupQuery.data) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center">
        <p className="text-muted-foreground">Grup yüklenirken bir hata oluştu.</p>
      </div>
    );
  }

  return <NewSettlementForm groupId={groupId} group={groupQuery.data} />;
}

function NewSettlementForm({ groupId, group }: { groupId: string; group: Group }) {
  const router = useRouter();
  const { user } = useAuth();
  const createSettlement = useCreateSettlement(groupId);
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
    try {
      await createSettlement.mutateAsync({
        toUserId: values.toUserId,
        amount: Number(values.amount),
      });
      toast.success("Ödeme kaydedildi");
      router.push(`/groups/${groupId}`);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  return (
    <div className="mx-auto w-full max-w-lg flex-1 px-6 py-10">
      <h1 className="mb-6 font-heading text-2xl font-bold">Ödeme Kaydet</h1>
      {/* Deliberately no "you owe X" suggestion here: Balance only tracks a
          net position, not who-owes-whom pairwise, so any such suggestion
          would assert a certainty the backend doesn't actually guarantee.
          Just who and how much. */}
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="toUserId">Kime ödedin?</Label>
          <select
            id="toUserId"
            className="h-8 w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-1 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 md:text-sm"
            {...register("toUserId")}
          >
            {otherMembers.map((member) => (
              <option key={member.userId} value={member.userId}>
                {member.displayName ?? member.email}
              </option>
            ))}
          </select>
          {errors.toUserId && <p className="text-sm text-destructive">{errors.toUserId.message}</p>}
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="amount">Ne kadar?</Label>
          <Input id="amount" type="number" step="0.01" {...register("amount")} />
          {errors.amount && <p className="text-sm text-destructive">{errors.amount.message}</p>}
        </div>

        <Button type="submit" loading={createSettlement.isPending} className="mt-2">
          Ödemeyi Kaydet
        </Button>
      </form>
    </div>
  );
}
