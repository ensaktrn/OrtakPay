// Type aliases into the OpenAPI-generated schema (types/api-generated.d.ts,
// regenerated via `npm run generate:types` - see docs/adr/0012). If a backend
// DTO field is renamed or removed, these aliases pick up the new shape as
// soon as types are regenerated, and every mismatched usage in the frontend
// becomes a compile error instead of a silent runtime bug.
import type { components } from "@/types/api-generated";

export type RegisterRequest = components["schemas"]["RegisterRequest"];
export type LoginRequest = components["schemas"]["LoginRequest"];
export type AuthResponse = components["schemas"]["AuthResponse"];
export type User = components["schemas"]["UserResponse"];
