import { afterEach, describe, expect, it, vi } from "vitest";
import { clearToken } from "../auth/token";
import { login } from "./auth";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("login", () => {
  it("posts the credentials to /auth/login without a bearer token", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ token: "t", personaId: "p", username: "ana" }));
    vi.stubGlobal("fetch", fetchMock);

    const response = await login({ username: "ana", password: "secret" });

    expect(response).toEqual({ token: "t", personaId: "p", username: "ana" });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/auth/login");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Authorization")).toBeNull();
    expect(init.headers.get("Content-Type")).toBe("application/json");
    expect(init.body).toBe(JSON.stringify({ username: "ana", password: "secret" }));
  });
});
