// Endpoint functions for the Families resource, layered over the single HTTP
// seam in transport.ts. The generated schema marks response fields optional and
// cannot express a legacy `null` family name, so these functions normalize the
// wire shape into the strict type the screens consume.
import type { components } from "./schema";
import { parsePage, type Page } from "./page";
import { request } from "./transport";

type FamilyResponse = components["schemas"]["FamilyResponse"];
type CreateFamilyRequest = components["schemas"]["CreateFamilyRequest"];

export type Family = {
  id: string;
  /** Legacy families can have a null name; the screen renders "Sin nombre". */
  name: string | null;
  adminPersonaId: string;
};

export const FAMILY_PAGE_SIZE = 20;

function toFamily(payload: FamilyResponse): Family {
  if (typeof payload.id !== "string" || typeof payload.adminPersonaId !== "string") {
    throw new Error("The family response is missing required fields");
  }
  return {
    id: payload.id,
    name: typeof payload.name === "string" ? payload.name : null,
    adminPersonaId: payload.adminPersonaId,
  };
}

export async function listMyFamilies(page: number, signal?: AbortSignal): Promise<Page<Family>> {
  const payload = await request<unknown>({
    method: "GET",
    path: "/me/families",
    query: { page, size: FAMILY_PAGE_SIZE },
    authorized: true,
    signal,
  });
  const springPage = parsePage<FamilyResponse>(payload);
  return { ...springPage, content: springPage.content.map(toFamily) };
}

export async function createFamily(name: string, signal?: AbortSignal): Promise<Family> {
  const payload = await request<FamilyResponse>({
    method: "POST",
    path: "/families",
    body: { name } satisfies CreateFamilyRequest,
    authorized: true,
    signal,
  });
  return toFamily(payload);
}
