import { afterEach, describe, expect, it, vi } from "vitest";
import { buildUrl, request, resolveApiUrl } from "./transport";
import { clearToken, setToken } from "../auth/token";
import { jsonResponse } from "../test/http";

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("request", () => {
  it("sends the bearer token on an authorized call and returns the parsed body", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: "abc" }));
    vi.stubGlobal("fetch", fetchMock);
    setToken("token-123");

    const result = await request<{ id: string }>({ method: "GET", path: "/personas/me", authorized: true });

    expect(result).toEqual({ id: "abc" });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/personas/me");
    expect(init.method).toBe("GET");
    expect(init.headers.get("Authorization")).toBe("Bearer token-123");
  });

  it("omits the Authorization header when the call is not authorized", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ token: "t" }));
    vi.stubGlobal("fetch", fetchMock);

    await request({ method: "POST", path: "/auth/login", body: { username: "ana", password: "secret" }, authorized: false });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/auth/login");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Authorization")).toBeNull();
    expect(init.headers.get("Content-Type")).toBe("application/json");
    expect(init.body).toBe(JSON.stringify({ username: "ana", password: "secret" }));
  });

  it("omits the Authorization header when there is no token yet", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({}));
    vi.stubGlobal("fetch", fetchMock);

    await request({ method: "GET", path: "/me/families", authorized: true });

    const [, init] = fetchMock.mock.calls[0];
    expect(init.headers.get("Authorization")).toBeNull();
  });

  it("sends query parameters and accepts without a body", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ content: [] }));
    vi.stubGlobal("fetch", fetchMock);

    await request({ method: "GET", path: "/me/families", query: { page: 1, size: 20 }, authorized: true });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/me/families?page=1&size=20");
    expect(init.body).toBeUndefined();
  });

  it("returns binary responses unparsed, with the bearer token", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(new Blob(["photo"], { type: "image/png" }), {
        status: 200,
        headers: { "Content-Type": "image/png" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const blob = await request<Blob>({
      method: "GET",
      path: "/families/f1/members/p1/dni/front",
      authorized: true,
      responseType: "blob",
    });

    expect(blob).toBeInstanceOf(Blob);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/families/f1/members/p1/dni/front");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });

  it("sends a FormData body and lets the browser set the multipart Content-Type", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: "p1" }));
    vi.stubGlobal("fetch", fetchMock);
    const form = new FormData();
    form.set("username", "ana");

    await request({ method: "POST", path: "/personas", form, authorized: false });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/personas");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Content-Type")).toBeNull();
    expect(init.headers.get("Authorization")).toBeNull();
    expect(init.body).toBe(form);
  });
});

describe("buildUrl", () => {
  it("joins the default base URL and the path without double slashes", () => {
    expect(buildUrl("/auth/login")).toBe("http://localhost:8070/api/v1/auth/login");
  });

  it("appends defined query parameters and skips undefined ones", () => {
    expect(buildUrl("/me/families", { page: 0, size: 20, sort: undefined })).toBe(
      "http://localhost:8070/api/v1/me/families?page=0&size=20",
    );
  });

  it("encodes characters that are not allowed raw in query strings", () => {
    // URLSearchParams uses application/x-www-form-urlencoded encoding: space is `+`.
    expect(buildUrl("/thing", { q: "a b&c" })).toBe("http://localhost:8070/api/v1/thing?q=a+b%26c");
  });
});

describe("resolveApiUrl", () => {
  it("resolves a relative URL from a DTO against the API origin, not the app origin", () => {
    expect(resolveApiUrl("/api/v1/families/f1/members/p1/dni/front")).toBe(
      "http://localhost:8070/api/v1/families/f1/members/p1/dni/front",
    );
  });

  it("leaves an already absolute URL alone", () => {
    expect(resolveApiUrl("http://localhost:8070/api/v1/photos/front")).toBe(
      "http://localhost:8070/api/v1/photos/front",
    );
  });
});
