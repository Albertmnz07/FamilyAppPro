import { describe, expect, it } from "vitest";
import { parsePage } from "./page";

type Family = { id: string; name: string | null; adminPersonaId: string };

// Representative body of a Spring Data Page as served by /me/families
// (shape asserted by the backend's ApiIntegrationTest).
const springPageBody = {
  content: [{ id: "f1", name: "Rojas", adminPersonaId: "p1" }],
  pageable: { pageNumber: 0, pageSize: 20, sort: { empty: true, sorted: false, unsorted: true }, offset: 0, paged: true, unpaged: false },
  last: true,
  totalElements: 1,
  totalPages: 1,
  size: 20,
  number: 0,
  sort: { empty: true, sorted: false, unsorted: true },
  first: true,
  numberOfElements: 1,
  empty: false,
};

describe("parsePage", () => {
  it("keeps the fields the front-end needs from a Spring Page", () => {
    expect(parsePage<Family>(springPageBody)).toEqual({
      content: [{ id: "f1", name: "Rojas", adminPersonaId: "p1" }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    });
  });

  it("rejects payloads without a content array", () => {
    expect(() => parsePage<Family>({ totalElements: 3 })).toThrow(TypeError);
    expect(() => parsePage<Family>(null)).toThrow(TypeError);
  });
});
