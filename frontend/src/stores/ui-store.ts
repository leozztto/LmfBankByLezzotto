import { create } from "zustand";
import { persist } from "zustand/middleware";

/**
 * Lightweight UI state that should survive a reload: which account the user is
 * working with. Persisted to localStorage; cleared on logout.
 */
interface UiState {
  selectedAccountId: number | null;
  setSelectedAccount: (id: number | null) => void;
  reset: () => void;
}

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      selectedAccountId: null,
      setSelectedAccount: (id) => set({ selectedAccountId: id }),
      reset: () => set({ selectedAccountId: null }),
    }),
    {
      name: "lmf-ui",
      // localStorage may be unavailable (SSR, private mode) — degrade quietly.
      storage: {
        getItem: (name) => {
          try {
            const v = globalThis.localStorage?.getItem(name);
            return v ? JSON.parse(v) : null;
          } catch {
            return null;
          }
        },
        setItem: (name, value) => {
          try {
            globalThis.localStorage?.setItem(name, JSON.stringify(value));
          } catch {
            /* ignore */
          }
        },
        removeItem: (name) => {
          try {
            globalThis.localStorage?.removeItem(name);
          } catch {
            /* ignore */
          }
        },
      },
    },
  ),
);
