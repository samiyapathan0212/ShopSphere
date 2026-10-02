import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { AuthUser } from './api/authApi';

const STORAGE_KEY = 'shopsphere.auth';

export interface AuthState {
  /** Short-lived JWT access token; `null` when signed out. */
  accessToken: string | null;
  user: AuthUser | null;
}

const EMPTY_STATE: AuthState = {
  accessToken: null,
  user: null,
};

/**
 * Loads the persisted session so a page reload keeps the signed-in navbar.
 * Every access is guarded: a disabled or unreadable localStorage must never
 * break application start-up.
 */
function loadPersistedState(): AuthState {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return EMPTY_STATE;
    }
    const parsed: unknown = JSON.parse(raw);
    if (typeof parsed !== 'object' || parsed === null) {
      return EMPTY_STATE;
    }
    const candidate = parsed as { accessToken?: unknown; user?: unknown };
    return {
      accessToken: typeof candidate.accessToken === 'string' ? candidate.accessToken : null,
      user: (candidate.user as AuthUser | null) ?? null,
    };
  } catch {
    return EMPTY_STATE;
  }
}

/** Mirrors the current session into localStorage (best effort). */
export function persistAuthState(state: AuthState): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // Ignore quota/privacy-mode failures: the in-memory session still works.
  }
}

/** Clears the persisted session (called on logout). */
export function clearPersistedAuthState(): void {
  try {
    window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore: nothing to clean up if storage is unavailable.
  }
}

/**
 * Authentication state (Phase 7). Deliberately a small slice on top of the
 * existing `configureStore` setup: it holds the access token and the signed-in
 * user for the UI (navbar, later guarded routes) and leaves all server
 * communication to the RTK Query slices.
 */
const authSlice = createSlice({
  name: 'auth',
  initialState: loadPersistedState() as AuthState,
  reducers: {
    credentialsReceived(state, action: PayloadAction<{ accessToken: string; user: AuthUser }>) {
      state.accessToken = action.payload.accessToken;
      state.user = action.payload.user;
    },
    loggedOut(state) {
      state.accessToken = null;
      state.user = null;
    },
  },
});

export const { credentialsReceived, loggedOut } = authSlice.actions;

export default authSlice.reducer;