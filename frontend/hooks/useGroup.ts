import { apiFetch } from "@/lib/api";
import type { Group } from "@/types/groups";
import { useQuery } from "@tanstack/react-query";

export function useGroup(groupId: string) {
  return useQuery({
    queryKey: ["group", groupId],
    queryFn: () => apiFetch<Group>(`/api/groups/${groupId}`),
    enabled: !!groupId,
  });
}
