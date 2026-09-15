"use client";

import { apiFetch, TOKEN_STORAGE_KEY } from "@/lib/api";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import type { AuthResponse, LoginRequest, RegisterRequest, User } from "@/types/auth";
import { useQueryClient } from "@tanstack/react-query";
import { createContext, useContext, useEffect, useState, type ReactNode } from "react";

type AuthStatus = "idle" | "loading" | "authenticated" | "error";

interface AuthContextValue {
  user: User | undefined;
  token: string | null;
  status: AuthStatus;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// Reads a token synchronously (during render, not an effect) so that on a
// hard refresh of a protected page, the very first render already knows
// "there might be a session" instead of momentarily reporting "idle" - a
// route guard reading stale "idle" on that first render would redirect to
// /login before useCurrentUser's query ever gets a chance to confirm the
// token.
function initToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  // useState instead of useReducer: token is now the only piece of state
  // AuthProvider itself owns (user/status are derived below), and a single
  // primitive value doesn't need a reducer's action/case machinery. Passing
  // initToken itself (not initToken()) makes this a lazy initializer, run
  // synchronously during the first render rather than in an effect after -
  // see initToken's comment for why that timing matters.
  const [token, setToken] = useState<string | null>(initToken);

  const queryClient = useQueryClient();

  // Calling useCurrentUser() (a useQuery hook) here, inside AuthProvider's
  // own body, only works because QueryClientProvider wraps AuthProvider in
  // app/layout.tsx (not the other way around) - a hook that reads query
  // context has to be a descendant of the provider that supplies it. If
  // AuthProvider were the outer provider instead, this call would throw
  // "No QueryClient set" before ever reaching a render of QueryClientProvider.
  const { data: user, isLoading, isError } = useCurrentUser(token);

  // A previously-valid token that /api/users/me now rejects (expired,
  // revoked) needs to be dropped - otherwise the app would keep sending a
  // dead token on every request and never let the user log back in cleanly.
  useEffect(() => {
    if (isError) {
      window.localStorage.removeItem(TOKEN_STORAGE_KEY);
      // This isn't derived state (which the rule below is meant to catch):
      // it's a one-time reaction to an async query settling into an error
      // state, which can't be computed during render.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setToken(null);
    }
  }, [isError]);

  const status: AuthStatus = !token ? "idle" : isError ? "error" : isLoading || !user ? "loading" : "authenticated";

  async function login(email: string, password: string) {
    const body: LoginRequest = { email, password };
    const { token: newToken } = await apiFetch<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(body),
    });
    if (!newToken) throw new Error("Login response did not include a token");
    window.localStorage.setItem(TOKEN_STORAGE_KEY, newToken);
    setToken(newToken);
  }

  async function register(email: string, password: string, displayName: string) {
    const body: RegisterRequest = { email, password, displayName };
    await apiFetch<User>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(body),
    });
    // Registration only creates the account, it doesn't hand back a token -
    // log the new user in right away so register ends in the same
    // authenticated state login does.
    await login(email, password);
  }

  function logout() {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    setToken(null);
    // Otherwise the next login would briefly render the PREVIOUS user's
    // cached data before the new /me fetch resolves.
    queryClient.removeQueries({ queryKey: ["currentUser"] });
  }

  return (
    <AuthContext.Provider value={{ user, token, status, login, register, logout }}>{children}</AuthContext.Provider>
  );
}

// Custom hook instead of exporting AuthContext directly: it centralizes the
// "used outside a Provider" check in one place (useContext would otherwise
// silently return null and defer the crash to wherever `.user` first gets
// read) and gives every caller a properly narrowed, non-null value.
export function useAuth(): AuthContextValue {
  // useContext reads whatever value the nearest AuthProvider ancestor last
  // rendered with - this is what lets login()/logout() update state in one
  // component and have every other component reading useAuth() re-render.
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
