import { fromPartial } from "@total-typescript/shoehorn";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, type ProblemDetail } from "../api/problem";
import { clearToken, getToken, setToken } from "./token";
import { handleUnauthorizedResponse, onUnauthorized, SESSION_EXPIRED_NOTICE } from "./session";

afterEach(() => {
  clearToken();
});

describe("handleUnauthorizedResponse", () => {
  it("clears the token and notifies the handler on a 401 while identified", () => {
    setToken("token-1");
    const handler = vi.fn();
    const unregister = onUnauthorized(handler);

    handleUnauthorizedResponse(new ApiError(401, fromPartial<ProblemDetail>({ code: "unauthorized" })));

    expect(handler).toHaveBeenCalledOnce();
    expect(getToken()).toBeNull();
    unregister();
  });

  it("ignores a 401 when no token is held, as with wrong credentials at login", () => {
    const handler = vi.fn();
    const unregister = onUnauthorized(handler);

    handleUnauthorizedResponse(new ApiError(401, fromPartial<ProblemDetail>({ code: "unauthorized" })));

    expect(handler).not.toHaveBeenCalled();
    unregister();
  });

  it("ignores failures that are not 401s", () => {
    setToken("token-1");
    const handler = vi.fn();
    const unregister = onUnauthorized(handler);

    handleUnauthorizedResponse(new ApiError(409, fromPartial<ProblemDetail>({ code: "cannot-remove-admin" })));
    handleUnauthorizedResponse(new TypeError("Failed to fetch"));

    expect(handler).not.toHaveBeenCalled();
    expect(getToken()).toBe("token-1");
    unregister();
  });

  it("stops notifying once unregistered", () => {
    setToken("token-1");
    const handler = vi.fn();
    const unregister = onUnauthorized(handler);
    unregister();

    handleUnauthorizedResponse(new ApiError(401, fromPartial<ProblemDetail>({ code: "unauthorized" })));

    expect(handler).not.toHaveBeenCalled();
    expect(getToken()).toBeNull();
  });

  it("exposes the session-expired notice for the /login redirect", () => {
    expect(SESSION_EXPIRED_NOTICE).toBe("Tu sesión ha finalizado. Vuelve a iniciar sesión.");
  });
});
