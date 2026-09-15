import { apiFetch } from "@/lib/api";
import type { CreateSettlementRequest, Settlement } from "@/types/groups";
import { useMutation, useQueryClient } from "@tanstack/react-query";

export function useCreateSettlement(groupId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateSettlementRequest) =>
      apiFetch<Settlement>(`/api/groups/${groupId}/settlements`, {
        method: "POST",
        body: JSON.stringify(request),
      }),
    onSuccess: () => {
      // Same prefix-match pattern as useCreateExpense: invalidating
      // ['group', groupId] covers ['group', groupId, 'balances'] (and
      // 'expenses') in one call, since a settlement changes balances too.
      queryClient.invalidateQueries({ queryKey: ["group", groupId] });
    },
  });
}
