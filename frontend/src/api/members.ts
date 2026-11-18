// Endpoint functions for a Family's Members and their DNI. Like the other
// resources, this is layered over the one HTTP seam in transport.ts and
// normalizes the generated schema's optional fields into strict screen types.
import type { components } from "./schema";
import { InvalidPageError, parsePage, type Page } from "./page";
import { request } from "./transport";

type MemberResponse = components["schemas"]["MemberResponse"];
type DniResponse = components["schemas"]["DniResponse"];

export type Member = {
  personaId: string;
  username: string;
  /** Null whenever Consent is not in force; the screen shows "Sin consentimiento". */
  dni: string | null;
  admin: boolean;
  consentInForce: boolean;
};

export type Dni = {
  number: string;
  /** Relative URL served by the backend; used as-is (spec #14, "Photos"). */
  frontPhotoUrl: string;
  backPhotoUrl: string;
};

export const MEMBER_PAGE_SIZE = 20;

/** A 200 with a body that is not a DNI response: a broken response, not a network outage. */
export class InvalidDniError extends TypeError {}

function toMember(payload: MemberResponse): Member {
  if (
    typeof payload.personaId !== "string" ||
    typeof payload.username !== "string" ||
    typeof payload.admin !== "boolean" ||
    typeof payload.consentInForce !== "boolean"
  ) {
    throw new InvalidPageError("The member response is missing required fields");
  }
  return {
    personaId: payload.personaId,
    username: payload.username,
    dni: typeof payload.dni === "string" ? payload.dni : null,
    admin: payload.admin,
    consentInForce: payload.consentInForce,
  };
}

export async function listFamilyMembers(
  familyId: string,
  page: number,
  signal?: AbortSignal,
): Promise<Page<Member>> {
  const payload = await request<unknown>({
    method: "GET",
    path: `/families/${encodeURIComponent(familyId)}/members`,
    query: { page, size: MEMBER_PAGE_SIZE },
    authorized: true,
    signal,
  });
  const springPage = parsePage<MemberResponse>(payload);
  return { ...springPage, content: springPage.content.map(toMember) };
}

export async function getMemberDni(
  familyId: string,
  personaId: string,
  signal?: AbortSignal,
): Promise<Dni> {
  const payload = await request<DniResponse>({
    method: "GET",
    path: `/families/${encodeURIComponent(familyId)}/members/${encodeURIComponent(personaId)}/dni`,
    authorized: true,
    signal,
  });
  if (
    typeof payload.number !== "string" ||
    typeof payload.frontPhotoUrl !== "string" ||
    typeof payload.backPhotoUrl !== "string"
  ) {
    throw new InvalidDniError("The DNI response is missing required fields");
  }
  return {
    number: payload.number,
    frontPhotoUrl: payload.frontPhotoUrl,
    backPhotoUrl: payload.backPhotoUrl,
  };
}
