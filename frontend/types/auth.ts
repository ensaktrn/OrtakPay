// Mirrors core-service's auth/user DTOs field-for-field (RegisterRequest,
// LoginRequest, AuthResponse, UserResponse in backend/core-service/.../dto).
// Hand-written for now - Faz F3 generates these from the OpenAPI spec instead.

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
}

export interface User {
  id: string;
  email: string;
  displayName: string;
}
