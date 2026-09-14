import { apiFetch } from "@/lib/api";
import type { ExpensePage } from "@/types/groups";
import { useQuery } from "@tanstack/react-query";

export function useGroupExpenses(groupId: string) {
  return useQuery({
    queryKey: ["group", groupId, "expenses"],
    queryFn: () => apiFetch<ExpensePage>(`/api/groups/${groupId}/expenses`),
    enabled: !!groupId,
  });
}
