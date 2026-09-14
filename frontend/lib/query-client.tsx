"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { useState, type ReactNode } from "react";

export function QueryProvider({ children }: { children: ReactNode }) {
  // useState(() => new QueryClient(...)) instead of a plain `const client =
  // new QueryClient(...)`: a plain const would build a brand new
  // QueryClient (and lose its whole cache) on every re-render of this
  // component. The lazy initializer runs exactly once, on mount.
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 1,
          },
          mutations: {
            // Financial actions (creating an expense, recording a
            // settlement) must not be retried automatically - a network
            // blip retried silently could double-submit money. The user
            // should see the failure and choose to resubmit.
            retry: false,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {process.env.NODE_ENV === "development" && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
}
