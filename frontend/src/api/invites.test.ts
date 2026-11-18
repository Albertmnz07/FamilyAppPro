import { afterEach, describe, expect, it, vi } from "vitest";
import { clearToken, setToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { InvalidRedemptionError, normalizeInviteCode, redeemInvite } from "./invites";

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("normalizeInviteCode", () => {
  it("uppercases and trims the pasted code so a sloppy paste still works", () => {
    expect(normalizeInviteCode(" ab12cd ")).toBe("AB12CD");
    expect(normalizeInviteCode("AB12CD")).toBe("AB12CD");
  });
});

describe("redeemInvite", () => {
  it("posts the normalized code with the bearer token and returns the joined family", async () => {
    setToken("token-1");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ familyId: "f1", personaId: "p1", status: "ACCEPTED" }, 201),
    );
    vi.stubGlobal("fetch", fetchMock);

    const redemption = await redeemInvite(" ab12cd ");

    expect(redemption).toEqual({ familyId: "f1", personaId: "p1", status: "ACCEPTED" });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/invites/AB12CD/redemption");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });

  it("treats a malformed redemption response as a broken response, not a network outage", async () => {
    setToken("token-1");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse({ unexpected: true }, 201)));

    await expect(redeemInvite("AB12CD")).rejects.toBeInstanceOf(InvalidRedemptionError);
  });
});
