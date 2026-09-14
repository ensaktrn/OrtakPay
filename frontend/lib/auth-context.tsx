"use client";

import { apiFetch, ApiError, TOKEN_STORAGE_KEY } from "@/lib/api";
import type { AuthResponse, LoginRequest, RegisterRequest, User } from "@/types/auth";
import { createContext, useContext, useEffect, useReducer, type ReactNode } from "react";

interface AuthState {
  user: User | null;
  token: string | null;
  status: "idle" | "loading" | "authenticated" | "error";
}

type AuthAction =
  | { type: "LOGIN_START" }
  | { type: "LOGIN_SUCCESS"; payload: { user: User; token: string } }
  | { type: "LOGIN_ERROR" }
  | { type: "LOGOUT" };

// useReducer instead of a few useState calls: user/token/status always change
// together (a login either sets all three or none of them), and a reducer
// makes those transitions explicit named cases instead of several setters
// that could be called out of sync with each other.
function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case "LOGIN_START":
      return { ...state, status: "loading" };
    case "LOGIN_SUCCESS":
      return { user: action.payload.user, token: action.payload.token, status: "authenticated" };
    case "LOGIN_ERROR":
      return { user: null, token: null, status: "error" };
    case "LOGOUT":
      return { user: null, token: null, status: "idle" };
    default:
      return state;
  }
}

// Reads a token synchronously (during render, not an effect) so that on a
// hard refresh of a protected page, the very first render already knows
// "there might be a session" instead of momentarily reporting "idle" - a
// route guard reading stale "idle" on that first render would redirect to
// /login before the effect below ever gets a chance to confirm the token.
function initAuthState(): AuthState {
  if (typeof window === "undefined") {
    return { user: null, token: null, status: "idle" };
  }
  const token = window.localStorage.getItem(TOKEN_STORAGE_KEY);
  return token ? { user: null, token, status: "loading" } : { user: null, token: null, status: "idle" };
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, undefined, initAuthState);

  // useEffect burada localStorage senkronizasyonu için gerekli, çünkü
  // GET /api/users/me bir ağ isteği (yan etki) - render sırasında değil,
  // component mount olduktan sonra tetiklenmeli. Bağımlılık dizisi boş:
  // bu sadece uygulama ilk açıldığında (ör. sayfa yenilendiğinde), yukarıdaki
  // initAuthState'in bulduğu token'ı doğrulamak için bir kere çalışır.
  useEffect(() => {
    if (!state.token) return;

    let cancelled = false;
    apiFetch<User>("/api/users/me")
      .then((user) => {
        if (!cancelled) {
          dispatch({ type: "LOGIN_SUCCESS", payload: { user, token: state.token! } });
        }
      })
      .catch(() => {
        if (!cancelled) {
          window.localStorage.removeItem(TOKEN_STORAGE_KEY);
          dispatch({ type: "LOGOUT" });
        }
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function login(email: string, password: string) {
    dispatch({ type: "LOGIN_START" });
    try {
      const body: LoginRequest = { email, password };
      const { token } = await apiFetch<AuthResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(body),
      });
      window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
      const user = await apiFetch<User>("/api/users/me");
      dispatch({ type: "LOGIN_SUCCESS", payload: { user, token } });
    } catch (err) {
      dispatch({ type: "LOGIN_ERROR" });
      throw err;
    }
  }

  async function register(email: string, password: string, displayName: string) {
    dispatch({ type: "LOGIN_START" });
    try {
      const body: RegisterRequest = { email, password, displayName };
      await apiFetch<User>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify(body),
      });
    } catch (err) {
      dispatch({ type: "LOGIN_ERROR" });
      throw err;
    }
    // Registration only creates the account, it doesn't hand back a token -
    // log the new user in right away so register ends in the same
    // authenticated state login does.
    await login(email, password);
  }

  function logout() {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    dispatch({ type: "LOGOUT" });
  }

  return <AuthContext.Provider value={{ ...state, login, register, logout }}>{children}</AuthContext.Provider>;
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

export function extractErrorMessage(err: unknown): string {
  return err instanceof ApiError ? (err.detail ?? err.title) : "Beklenmeyen bir hata oluştu";
}
