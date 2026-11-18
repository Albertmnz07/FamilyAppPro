import { fromPartial } from "@total-typescript/shoehorn";
import { QueryClientProvider, useMutation, useQuery } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, type ProblemDetail } from "./api/problem";
import { onUnauthorized } from "./auth/session";
import { clearToken, getToken, setToken } from "./auth/token";
import { queryClient } from "./queryClient";

const unauthorized = () => new ApiError(401, fromPartial<ProblemDetail>({ code: "unauthorized" }));

function Wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

afterEach(() => {
  clearToken();
  queryClient.clear();
});

describe("the global 401 handler wired into TanStack Query", () => {
  it("runs when a query fails with 401 while a token is held", async () => {
    setToken("token-1");
    const handler = vi.fn();
    const unregister = onUnauthorized(handler);
    const { result } = renderHook(
      () => useQuery({ queryKey: ["probe", "unauthorized"], queryFn: () => Promise.reject(unauthorized()) }),
      { wrapper: Wrapper },
    );

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(handler).toHaveBeenCalledOnce();
    expect(getToken()).toBeNull();
    unregister();
  });

  it("runs when a mutation fails with 401 while a token is held", async () => {
    setToken("token-1");
    const handler = vi.fn();
    const unregister = onUnauthorized(handler);
    const { result } = renderHook(() => useMutation({ mutationFn: () => Promise.reject(unauthorized()) }), {
      wrapper: Wrapper,
    });

    act(() => result.current.mutate());
    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(handler).toHaveBeenCalledOnce();
    expect(getToken()).toBeNull();
    unregister();
  });

  it("keeps the session when a query fails with anything else", async () => {
    setToken("token-1");
    const handler = vi.fn();
    const unregister = onUnauthorized(handler);
    const { result } = renderHook(
      () => useQuery({ queryKey: ["probe", "conflict"], queryFn: () => Promise.reject(new ApiError(409)) }),
      { wrapper: Wrapper },
    );

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(handler).not.toHaveBeenCalled();
    expect(getToken()).toBe("token-1");
    unregister();
  });

  it("does not retry HTTP-level failures", async () => {
    const failing = vi.fn(() => Promise.reject(new ApiError(404)));
    const { result } = renderHook(() => useQuery({ queryKey: ["probe", "no-retry"], queryFn: failing }), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(failing).toHaveBeenCalledTimes(1);
  });
});
