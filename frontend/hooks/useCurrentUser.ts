import { apiFetch } from "@/lib/api";
import type { User } from "@/types/auth";
import { useQuery } from "@tanstack/react-query";

// Takes `token` as a parameter instead of reading it via useAuth(): this
// hook is called from inside AuthProvider itself (to derive its own
// "authenticated" status), and useAuth() only works for components rendered
// BELOW AuthProvider - calling it here would throw the same "must be used
// within an AuthProvider" error the hook is designed to guard against.
export function useCurrentUser(token: string | null) {
  return useQuery({
    queryKey: ["currentUser"],
    queryFn: () => apiFetch<User>("/api/users/me"),
    enabled: !!token,
  });
}
