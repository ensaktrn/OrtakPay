import { apiFetch } from "@/lib/api";
import type { CreateGroupRequest, Group } from "@/types/groups";
import { useMutation, useQueryClient } from "@tanstack/react-query";

export function useCreateGroup() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateGroupRequest) =>
      apiFetch<Group>("/api/groups", {
        method: "POST",
        body: JSON.stringify(request),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["groups"] });
    },
  });
}
