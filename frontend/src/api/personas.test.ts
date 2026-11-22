import { afterEach, describe, expect, it, vi } from "vitest";
import { clearToken, setToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { getMe, InvalidPersonaError, register, uploadDniPhotos } from "./personas";

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("register", () => {
  it("posts the credentials as multipart form data without any photo part", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ id: "p1", username: "ana", dni: "12345678Z" }, 201));
    vi.stubGlobal("fetch", fetchMock);

    const persona = await register({
      username: "ana",
      dni: "12345678Z",
      password: "secret-1",
    });

    expect(persona).toEqual({ id: "p1", username: "ana", dni: "12345678Z" });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/personas");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Authorization")).toBeNull();
    const form = init.body as FormData;
    expect(form).toBeInstanceOf(FormData);
    expect(form.get("username")).toBe("ana");
    expect(form.get("dni")).toBe("12345678Z");
    expect(form.get("password")).toBe("secret-1");
    expect(form.get("frontPhoto")).toBeNull();
    expect(form.get("backPhoto")).toBeNull();
  });
});

describe("getMe", () => {
  it("GETs the profile with the bearer token and null photo URLs when none are uploaded", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        id: "p1",
        username: "ana",
        dni: "12345678Z",
        frontPhotoUrl: null,
        backPhotoUrl: null,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const profile = await getMe();

    expect(profile).toEqual({
      id: "p1",
      username: "ana",
      dni: "12345678Z",
      frontPhotoUrl: null,
      backPhotoUrl: null,
    });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/personas/me");
    expect(init.method).toBe("GET");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });

  it("rejects a malformed profile response instead of treating it as an empty one", async () => {
    setToken("token-1");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse({ username: "ana" })));

    await expect(getMe()).rejects.toBeInstanceOf(InvalidPersonaError);
  });
});

describe("uploadDniPhotos", () => {
  it("PUTs only the supplied parts as multipart with the bearer token", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        id: "p1",
        username: "ana",
        dni: "12345678Z",
        frontPhotoUrl: "/api/v1/personas/me/photos/front",
        backPhotoUrl: null,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const frontPhoto = new File(["front"], "front.jpg", { type: "image/jpeg" });

    const profile = await uploadDniPhotos({ frontPhoto });

    expect(profile).toEqual({
      id: "p1",
      username: "ana",
      dni: "12345678Z",
      frontPhotoUrl: "/api/v1/personas/me/photos/front",
      backPhotoUrl: null,
    });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/personas/me/dni/photos");
    expect(init.method).toBe("PUT");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
    const form = init.body as FormData;
    expect(form).toBeInstanceOf(FormData);
    expect(form.get("frontPhoto")).toBe(frontPhoto);
    expect(form.get("backPhoto")).toBeNull();
  });

  it("sends both parts when both are supplied", async () => {
    setToken("token-1");
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ id: "p1", username: "ana", dni: "12345678Z" }));
    vi.stubGlobal("fetch", fetchMock);
    const frontPhoto = new File(["front"], "front.jpg", { type: "image/jpeg" });
    const backPhoto = new File(["back"], "back.png", { type: "image/png" });

    await uploadDniPhotos({ frontPhoto, backPhoto });

    const form = fetchMock.mock.calls[0][1].body as FormData;
    expect(form.get("frontPhoto")).toBe(frontPhoto);
    expect(form.get("backPhoto")).toBe(backPhoto);
  });
});
