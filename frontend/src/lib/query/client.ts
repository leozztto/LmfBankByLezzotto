import {
  MutationCache,
  QueryCache,
  QueryClient,
} from "@tanstack/react-query";

import { ApiError, ValidationError } from "@/lib/api/errors";
import { notify } from "@/lib/notify";
import { useAuthStore } from "@/stores/auth-store";
import { useUiStore } from "@/stores/ui-store";

/**
 * One global error path for every query/mutation:
 *  - 401 -> the session lapsed: clear it and hard-navigate to /login
 *    (hard nav re-runs the middleware and drops the cache);
 *  - ValidationError -> the form shows it, stay quiet here;
 *  - anything else -> a toast.
 */
export function handleGlobalError(error: unknown) {
  if (error instanceof ValidationError) return;

  if (error instanceof ApiError && error.status === 401) {
    useAuthStore.getState().clearSession();
    useUiStore.getState().reset();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    return;
  }

  notify.error(error instanceof Error ? error.message : "Erro inesperado");
}

export function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        gcTime: 5 * 60_000,
        refetchOnWindowFocus: false,
        retry: (failureCount, error) =>
          !(error instanceof ApiError) && failureCount < 2,
      },
      mutations: {
        retry: 0,
      },
    },
    queryCache: new QueryCache({ onError: handleGlobalError }),
    mutationCache: new MutationCache({ onError: handleGlobalError }),
  });
}
