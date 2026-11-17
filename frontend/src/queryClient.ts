import { MutationCache, QueryCache, QueryClient } from "@tanstack/react-query";
import { handleUnauthorizedResponse } from "./auth/session";
import { isApiError } from "./api/problem";

// Server state lives in TanStack Query (spec #14, "Server state"): caching,
// loading and error states, mutation invalidation and the single global 401
// handler live here. HTTP-level failures are never retried; network failures
// get one retry.
export const queryClient = new QueryClient({
  queryCache: new QueryCache({ onError: handleUnauthorizedResponse }),
  mutationCache: new MutationCache({ onError: handleUnauthorizedResponse }),
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => !isApiError(error) && failureCount < 2,
    },
  },
});
