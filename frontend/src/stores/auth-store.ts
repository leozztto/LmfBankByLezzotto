import { create } from "zustand";

import type { Session } from "@/lib/schemas/auth";

/**
 * Session state for the UI only. NOT persisted — the httpOnly cookie is the
 * source of truth; this is hydrated on every load by `(app)/layout.tsx` (server)
 * and revalidated by `useSessionQuery`. The real gate is `middleware.ts` + the
 * backend 401 (ADR 0007).
 */
interface AuthState {
  session: Session | null;
  setSession: (session: Session | null) => void;
  clearSession: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  session: null,
  setSession: (session) => set({ session }),
  clearSession: () => set({ session: null }),
}));

export const selectIsAuthenticated = (s: AuthState) =>
  s.session?.authenticated === true;
export const selectUsername = (s: AuthState) => s.session?.username;
