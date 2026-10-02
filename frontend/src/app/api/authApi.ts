import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

const API_URL: string = import.meta.env.VITE_API_URL || '/api';

export type UserRole = 'CUSTOMER' | 'ADMIN';

/** Public user shape returned by the backend (never contains credentials). */
export interface AuthUser {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: AuthUser;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

/** Registration returns the created user; the access token comes from login. */
export type RegisterResponse = AuthUser;

/**
 * Response of a silent token refresh. Mirrors the backend
 * `RefreshTokenResponse` record exactly: the refresh token itself is never in
 * the JSON body, only rotated in the HttpOnly cookie.
 */
export interface RefreshResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: AuthUser;
}

/**
 * Authentication API slice (Phase 7). Extends the existing RTK Query approach
 * used by {@link apiSlice} instead of introducing a second state library.
 * `credentials: 'include'` lets the backend's HttpOnly refresh cookie travel
 * with login/refresh/logout calls. The returned access token is kept in the
 * Redux auth slice — see `src/app/authSlice.ts`.
 */
export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({ baseUrl: API_URL, credentials: 'include' }),
  endpoints: (builder) => ({
    login: builder.mutation<LoginResponse, LoginRequest>({
      query: (body) => ({ url: '/auth/login', method: 'POST', body }),
    }),
    register: builder.mutation<RegisterResponse, RegisterRequest>({
      query: (body) => ({ url: '/auth/register', method: 'POST', body }),
    }),
    logout: builder.mutation<void, void>({
      query: () => ({ url: '/auth/logout', method: 'POST' }),
    }),
    /**
     * Silent re-authentication. The backend reads the HttpOnly `refresh_token`
     * cookie (rotated on use) and mints a fresh access token, so the body needs
     * no arguments. The cookie is scoped to `/api/auth` and the base query
     * already sends `credentials: 'include'`.
     */
    refresh: builder.mutation<RefreshResponse, void>({
      query: () => ({ url: '/auth/refresh', method: 'POST' }),
    }),
  }),
});

export const {
  useLoginMutation,
  useRegisterMutation,
  useLogoutMutation,
  useRefreshMutation,
} = authApi;

/** Server-side validation messages, keyed by field name (may be empty). */
export function readFieldErrors(error: unknown): Record<string, string> {
  const data = errorData(error);
  if (data && typeof data.fieldErrors === 'object' && data.fieldErrors !== null) {
    const entries = Object.entries(data.fieldErrors as Record<string, unknown>)
      .filter(([, value]) => typeof value === 'string' && value.length > 0)
      .map(([field, value]) => [field, value as string]);
    return Object.fromEntries(entries);
  }
  return {};
}

/**
 * Converts an RTK Query error into a message a person can act on: it prefers
 * the backend's error envelope (`message`, then `fieldErrors`, then `error`)
 * and falls back to a caller-supplied sentence for network failures.
 */
export function describeApiError(error: unknown, fallback: string): string {
  const data = errorData(error);

  if (data) {
    if (typeof data.message === 'string' && data.message.trim().length > 0) {
      return data.message;
    }
    const fieldErrors = readFieldErrors(error);
    const firstFieldError = Object.values(fieldErrors)[0];
    if (firstFieldError) {
      return firstFieldError;
    }
    if (typeof data.error === 'string' && data.error.trim().length > 0) {
      return data.error;
    }
  }

  if (typeof error === 'string' && error.trim().length > 0) {
    return error;
  }

  return fallback;
}

function errorData(error: unknown): Record<string, unknown> | null {
  if (typeof error !== 'object' || error === null) {
    return null;
  }
  const candidate = (error as { data?: unknown }).data;
  if (typeof candidate === 'object' && candidate !== null) {
    return candidate as Record<string, unknown>;
  }
  if (typeof candidate === 'string') {
    return { message: candidate };
  }
  return null;
}