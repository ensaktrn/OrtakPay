import { apiFetch } from "@/lib/api";
import type { Balance } from "@/types/groups";
import { useQuery } from "@tanstack/react-query";

export function useGroupBalances(groupId: string) {
  return useQuery({
    queryKey: ["group", groupId, "balances"],
    queryFn: () => apiFetch<Balance[]>(`/api/groups/${groupId}/balances`),
    enabled: !!groupId,
  });
}
