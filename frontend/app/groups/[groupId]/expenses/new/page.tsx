"use client";

import { useCreateExpense } from "@/hooks/useCreateExpense";
import { useGroup } from "@/hooks/useGroup";
import { extractErrorMessage } from "@/lib/api";
import type { Group, ParticipantInput } from "@/types/groups";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { use, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

const participantFormSchema = z.object({
  userId: z.string(),
  selected: z.boolean(),
  value: z.string().optional(),
});

const expenseFormSchema = z
  .object({
    description: z.string().min(1, "Açıklama gerekli"),
    // Kept as a validated string (like participants[i].value below), parsed
    // manually where needed - z.coerce.number() would make this field's
    // input type `unknown`, which breaks useForm's single type parameter
    // (the form's raw values and the resolver's parsed output would no
    // longer match) without also threading z.input/z.output generics
    // through useForm just for this one field.
    amount: z
      .string()
      .min(1, "Tutar gerekli")
      .refine((val) => !Number.isNaN(Number(val)) && Number(val) > 0, "Tutar pozitif bir sayı olmalı"),
    splitType: z.enum(["EQUAL", "EXACT", "PERCENTAGE"]),
    paidBy: z.string().min(1, "Ödeyen seçilmeli"),
    participants: z.array(participantFormSchema),
  })
  // superRefine (not a plain .refine()) because these checks depend on each
  // other and on splitType - a single refine can only attach one message to
  // one path, superRefine lets each failure report its own specific message.
  .superRefine((data, ctx) => {
    const selected = data.participants.filter((p) => p.selected);
    if (selected.length === 0) {
      ctx.addIssue({ code: "custom", message: "En az bir katılımcı seçilmeli", path: ["participants"] });
      return;
    }

    if (data.splitType !== "EQUAL") {
      const hasMissingValue = selected.some(
        (p) => p.value === undefined || p.value.trim() === "" || Number.isNaN(Number(p.value)),
      );
      if (hasMissingValue) {
        ctx.addIssue({
          code: "custom",
          message: "Seçili katılımcıların hepsi için bir değer gir",
          path: ["participants"],
        });
        return;
      }
    }

    if (data.splitType === "EXACT") {
      const sum = selected.reduce((total, p) => total + Number(p.value), 0);
      const amount = Number(data.amount);
      if (Math.abs(sum - amount) > 0.01) {
        ctx.addIssue({
          code: "custom",
          message: `Tutarlar toplamı (${sum.toFixed(2)}) masraf tutarına (${amount.toFixed(2)}) eşit olmalı`,
          path: ["participants"],
        });
      }
    }

    if (data.splitType === "PERCENTAGE") {
      const sum = selected.reduce((total, p) => total + Number(p.value), 0);
      if (Math.abs(sum - 100) > 0.01) {
        ctx.addIssue({
          code: "custom",
          message: `Yüzdeler toplamı %${sum.toFixed(1)} (100 olmalı)`,
          path: ["participants"],
        });
      }
    }
  });

type ExpenseFormValues = z.infer<typeof expenseFormSchema>;

// Next.js 15+ made `params` a Promise even in Client Component pages (see
// app/groups/[groupId]/page.tsx's comment - checked the bundled docs, not
// training data). Same `use()` pattern applies here.
export default function NewExpensePage(props: PageProps<"/groups/[groupId]/expenses/new">) {
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

  return <NewExpenseForm groupId={groupId} group={groupQuery.data} />;
}

// Split out from the page component so useForm's defaultValues can be built
// straight from the group's member list - the form only ever mounts once
// that data already exists, so there's no "members loaded later" case to
// juggle with resets/effects.
function NewExpenseForm({ groupId, group }: { groupId: string; group: Group }) {
  const router = useRouter();
  const createExpense = useCreateExpense(groupId);
  const [formError, setFormError] = useState<string | null>(null);
  const members = group.members ?? [];

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<ExpenseFormValues>({
    resolver: zodResolver(expenseFormSchema),
    defaultValues: {
      description: "",
      amount: "",
      splitType: "EQUAL",
      paidBy: members[0]?.userId ?? "",
      participants: members.map((member) => ({
        userId: member.userId ?? "",
        selected: true,
        value: "",
      })),
    },
  });

  // useWatch instead of relying on the Zod resolver's own error state: the
  // running total needs to update on every keystroke as immediate feedback,
  // before the user ever submits - the resolver above only runs at submit
  // time and would just block a mismatched total, not show it live.
  const splitType = useWatch({ control, name: "splitType" });
  const amount = useWatch({ control, name: "amount" });
  const participants = useWatch({ control, name: "participants" });

  const selectedSum = participants
    .filter((p) => p.selected)
    .reduce((total, p) => total + (Number(p.value) || 0), 0);
  const amountValue = Number(amount) || 0;

  async function onSubmit(values: ExpenseFormValues) {
    setFormError(null);
    const selected = values.participants.filter((p) => p.selected);
    const participantsPayload: ParticipantInput[] = selected.map((p) => ({
      userId: p.userId,
      value: values.splitType === "EQUAL" ? undefined : Number(p.value),
    }));

    try {
      await createExpense.mutateAsync({
        paidBy: values.paidBy,
        amount: Number(values.amount),
        description: values.description,
        splitType: values.splitType,
        participants: participantsPayload,
      });
      router.push(`/groups/${groupId}`);
    } catch (err) {
      setFormError(extractErrorMessage(err));
    }
  }

  return (
    <div className="mx-auto w-full max-w-lg flex-1 px-6 py-10">
      <h1 className="mb-6 text-2xl font-semibold">Yeni Masraf Ekle</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <label htmlFor="description" className="text-sm font-medium">
            Açıklama
          </label>
          <input
            id="description"
            type="text"
            className="rounded border border-black/20 px-3 py-2 dark:border-white/20"
            {...register("description")}
          />
          {errors.description && <p className="text-sm text-red-600">{errors.description.message}</p>}
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="amount" className="text-sm font-medium">
            Tutar
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

        <div className="flex flex-col gap-1">
          <label htmlFor="paidBy" className="text-sm font-medium">
            Ödeyen
          </label>
          <select
            id="paidBy"
            className="rounded border border-black/20 px-3 py-2 dark:border-white/20"
            {...register("paidBy")}
          >
            {members.map((member) => (
              <option key={member.userId} value={member.userId}>
                {member.displayName ?? member.email}
              </option>
            ))}
          </select>
          {errors.paidBy && <p className="text-sm text-red-600">{errors.paidBy.message}</p>}
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="splitType" className="text-sm font-medium">
            Paylaşım Türü
          </label>
          <select
            id="splitType"
            className="rounded border border-black/20 px-3 py-2 dark:border-white/20"
            {...register("splitType")}
          >
            <option value="EQUAL">Eşit</option>
            <option value="EXACT">Tam Tutar</option>
            <option value="PERCENTAGE">Yüzde</option>
          </select>
        </div>

        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium">Katılımcılar</p>
          {members.map((member, index) => (
            <div key={member.userId} className="flex items-center gap-3">
              <input type="hidden" {...register(`participants.${index}.userId`)} />
              <input
                type="checkbox"
                id={`participant-${index}`}
                {...register(`participants.${index}.selected`)}
              />
              <label htmlFor={`participant-${index}`} className="flex-1">
                {member.displayName ?? member.email}
              </label>
              {/* Only for participants that are actually selected - a value
                  input for someone excluded from the split makes no sense
                  and would be confusing left on screen. */}
              {splitType !== "EQUAL" && participants[index]?.selected && (
                <input
                  id={`participant-value-${index}`}
                  type="number"
                  step="0.01"
                  placeholder={splitType === "PERCENTAGE" ? "%" : "tutar"}
                  className="w-24 rounded border border-black/20 px-2 py-1 dark:border-white/20"
                  {...register(`participants.${index}.value`)}
                />
              )}
            </div>
          ))}

          {splitType === "EXACT" && (
            <p
              className={Math.abs(selectedSum - amountValue) > 0.01 ? "text-sm text-red-600" : "text-sm text-green-600"}
            >
              Toplam: {selectedSum.toFixed(2)} / {amountValue.toFixed(2)} TL
            </p>
          )}
          {splitType === "PERCENTAGE" && (
            <p className={Math.abs(selectedSum - 100) > 0.01 ? "text-sm text-red-600" : "text-sm text-green-600"}>
              Toplam: %{selectedSum.toFixed(1)}
            </p>
          )}
          {/* react-hook-form nests an array-level custom error under `.root`
              (not directly on `.message`) whenever the array field also has
              registered per-item sub-fields, which ours does
              (participants.{index}.userId/selected/value) - confirmed by
              inspecting the live formState rather than guessing. */}
          {typeof errors.participants?.root?.message === "string" && (
            <p className="text-sm text-red-600">{errors.participants.root.message}</p>
          )}
        </div>

        {formError && <p className="text-sm text-red-600">{formError}</p>}

        <button
          type="submit"
          disabled={createExpense.isPending}
          className="rounded bg-foreground px-4 py-2 text-background disabled:opacity-50"
        >
          {createExpense.isPending ? "Kaydediliyor..." : "Masraf Ekle"}
        </button>
      </form>
    </div>
  );
}
