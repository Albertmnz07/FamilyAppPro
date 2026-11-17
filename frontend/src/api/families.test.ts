import { afterEach, describe, expect, it, vi } from "vitest";
import { setToken, clearToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { createFamily, listMyFamilies } from "./families";

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("listMyFamilies", () => {
  it("requests page 0 at 20 per page with the bearer token and reads the Spring Page", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        content: [
          { id: "f1", name: "Rojas", adminPersonaId: "p1" },
          { id: "f2", name: null, adminPersonaId: "p2" },
        ],
        totalElements: 2,
        totalPages: 1,
        number: 0,
        size: 20,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const page = await listMyFamilies(0);

    expect(page).toEqual({
      content: [
        { id: "f1", name: "Rojas", adminPersonaId: "p1" },
        { id: "f2", name: null, adminPersonaId: "p2" },
      ],
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 20,
    });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/me/families?page=0&size=20");
    expect(init.method).toBe("GET");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });
});

describe("createFamily", () => {
  it("posts the name with the bearer token and returns the created family", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ id: "f3", name: "Nueva familia", adminPersonaId: "p1" }, 201),
    );
    vi.stubGlobal("fetch", fetchMock);

    const family = await createFamily("Nueva familia");

    expect(family).toEqual({ id: "f3", name: "Nueva familia", adminPersonaId: "p1" });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/families");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
    expect(init.headers.get("Content-Type")).toBe("application/json");
    expect(init.body).toBe(JSON.stringify({ name: "Nueva familia" }));
  });
});
