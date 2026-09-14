import { apiFetch } from "@/lib/api";
import type { GroupSummary } from "@/types/groups";
import { useQuery } from "@tanstack/react-query";

export function useGroups() {
  return useQuery({
    queryKey: ["groups"],
    queryFn: () => apiFetch<GroupSummary[]>("/api/groups"),
  });
}
