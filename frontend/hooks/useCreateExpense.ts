import { apiFetch } from "@/lib/api";
import type { CreateExpenseRequest, Expense } from "@/types/groups";
import { useMutation, useQueryClient } from "@tanstack/react-query";

export function useCreateExpense(groupId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateExpenseRequest) =>
      apiFetch<Expense>(`/api/groups/${groupId}/expenses`, {
        method: "POST",
        body: JSON.stringify(request),
      }),
    onSuccess: () => {
      // Prefix match, not an exact key: this invalidates ['group', groupId]
      // itself AND every query key that starts with it -
      // ['group', groupId, 'expenses'] and ['group', groupId, 'balances'] -
      // in one call, since a new expense changes both.
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
    },
  });
}
