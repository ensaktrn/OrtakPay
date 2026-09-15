const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;
export const TOKEN_STORAGE_KEY = "ortakpay_token";

// Shape of a Spring RFC 7807 ProblemDetail response (see
// GlobalExceptionHandler in core-service) - only the fields we actually use.
interface ProblemDetailBody {
  status?: number;
  title?: string;
  detail?: string;
}

export class ApiError extends Error {
  status: number;
  title: string;
  detail: string;

  constructor(status: number, title: string, detail: string) {
    super(detail || title);
    this.name = "ApiError";
    this.status = status;
    this.title = title;
    this.detail = detail;
  }
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = typeof window !== "undefined" ? window.localStorage.getItem(TOKEN_STORAGE_KEY) : null;

  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });

  if (!response.ok) {
    const problem: ProblemDetailBody = await response.json().catch(() => ({}));
    throw new ApiError(
      problem.status ?? response.status,
      problem.title ?? response.statusText,
      problem.detail ?? "Beklenmeyen bir hata oluştu",
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function extractErrorMessage(err: unknown): string {
  return err instanceof ApiError ? (err.detail ?? err.title ?? "Beklenmeyen bir hata oluştu") : "Beklenmeyen bir hata oluştu";
}
