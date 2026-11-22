import { afterEach, describe, expect, it, vi } from "vitest";
import { clearToken, setToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import {
  getDniPhoto,
  getMemberDni,
  InvalidDniError,
  listFamilyMembers,
  removeMember,
  revokeConsent,
} from "./members";

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("listFamilyMembers", () => {
  it("requests the requested page at 20 per page with the bearer token and reads the Spring Page", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        content: [
          {
            personaId: "p1",
            username: "ana",
            dni: "12345678Z",
            admin: true,
            status: "ACCEPTED",
            consentInForce: true,
          },
          {
            personaId: "p2",
            username: "beto",
            dni: null,
            admin: false,
            status: "ACCEPTED",
            consentInForce: false,
          },
        ],
        totalElements: 21,
        totalPages: 2,
        number: 1,
        size: 20,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const page = await listFamilyMembers("f1", 1);

    expect(page).toEqual({
      content: [
        {
          personaId: "p1",
          username: "ana",
          dni: "12345678Z",
          admin: true,
          consentInForce: true,
        },
        {
          personaId: "p2",
          username: "beto",
          dni: null,
          admin: false,
          consentInForce: false,
        },
      ],
      totalElements: 21,
      totalPages: 2,
      number: 1,
      size: 20,
    });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/families/f1/members?page=1&size=20");
    expect(init.method).toBe("GET");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });
});

describe("getMemberDni", () => {
  it("reads the number and the relative photo URLs with the bearer token", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        personaId: "p1",
        number: "12345678Z",
        frontPhotoUrl: "/api/v1/families/f1/members/p1/dni/front",
        backPhotoUrl: "/api/v1/families/f1/members/p1/dni/back",
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const dni = await getMemberDni("f1", "p1");

    expect(dni).toEqual({
      number: "12345678Z",
      frontPhotoUrl: "/api/v1/families/f1/members/p1/dni/front",
      backPhotoUrl: "/api/v1/families/f1/members/p1/dni/back",
    });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/families/f1/members/p1/dni");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });

  it("reads null photo URLs when the Persona has uploaded no photos", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        personaId: "p1",
        number: "12345678Z",
        frontPhotoUrl: null,
        backPhotoUrl: null,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const dni = await getMemberDni("f1", "p1");

    expect(dni).toEqual({ number: "12345678Z", frontPhotoUrl: null, backPhotoUrl: null });
  });

  it("rejects a DNI response whose photo URL is not a string", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        personaId: "p1",
        number: "12345678Z",
        frontPhotoUrl: 42,
        backPhotoUrl: null,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getMemberDni("f1", "p1")).rejects.toBeInstanceOf(InvalidDniError);
  });
});

describe("revokeConsent", () => {
  it("DELETEs the member's consent with the bearer token and reads the empty 204", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(revokeConsent("f1", "p1")).resolves.toBeUndefined();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/families/f1/members/p1/consent");
    expect(init.method).toBe("DELETE");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });
});

describe("removeMember", () => {
  it("DELETEs the membership with the bearer token and reads the empty 204", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(removeMember("f1", "p2")).resolves.toBeUndefined();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/families/f1/members/p2");
    expect(init.method).toBe("DELETE");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });
});

describe("getDniPhoto", () => {
  it("requests the DTO's relative photo URL against the API origin with the bearer token", async () => {
    setToken("token-1");
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(new Blob(["photo"], { type: "image/png" }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    const photo = await getDniPhoto("/api/v1/families/f1/members/p1/dni/front");

    expect(photo).toBeInstanceOf(Blob);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/families/f1/members/p1/dni/front");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });
});
