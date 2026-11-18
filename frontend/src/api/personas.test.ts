import { afterEach, describe, expect, it, vi } from "vitest";
import { jsonResponse } from "../test/http";
import { register } from "./personas";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("register", () => {
  it("posts the credentials and both photos as multipart form data", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ id: "p1", username: "ana", dni: "12345678Z" }, 201),
    );
    vi.stubGlobal("fetch", fetchMock);
    const frontPhoto = new File(["front"], "front.jpg", { type: "image/jpeg" });
    const backPhoto = new File(["back"], "back.jpg", { type: "image/jpeg" });

    const persona = await register({
      username: "ana",
      dni: "12345678Z",
      password: "secret-1",
      frontPhoto,
      backPhoto,
    });

    expect(persona).toEqual({ id: "p1", username: "ana", dni: "12345678Z" });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/personas");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Authorization")).toBeNull();
    const form = init.body as FormData;
    expect(form).toBeInstanceOf(FormData);
    expect(form.get("username")).toBe("ana");
    expect(form.get("dni")).toBe("12345678Z");
    expect(form.get("password")).toBe("secret-1");
    expect(form.get("frontPhoto")).toBe(frontPhoto);
    expect(form.get("backPhoto")).toBe(backPhoto);
  });
});
