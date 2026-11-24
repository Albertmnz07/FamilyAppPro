// Endpoint functions for a Family's Members and their DNI. Like the other
// resources, this is layered over the one HTTP seam in transport.ts and
// normalizes the generated schema's optional fields into strict screen types.
import type { components } from "./schema";
import { InvalidPageError, parsePage, type Page } from "./page";
import { request, resolveApiUrl } from "./transport";

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
  /** Relative URL served by the backend; null until that side is uploaded (spec #26). */
  frontPhotoUrl: string | null;
  backPhotoUrl: string | null;
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
  if (typeof payload.number !== "string") {
    throw new InvalidDniError("The DNI response is missing required fields");
  }
  return {
    number: payload.number,
    frontPhotoUrl: toPhotoUrl(payload.frontPhotoUrl),
    backPhotoUrl: toPhotoUrl(payload.backPhotoUrl),
  };
}

/** A photo URL is a relative string or explicit null; anything else is a broken response. */
function toPhotoUrl(value: unknown): string | null {
  if (value === null || value === undefined) return null;
  if (typeof value === "string") return value;
  throw new InvalidDniError("The DNI response has an invalid photo URL");
}

/**
 * The DTO's relative URL is used as-is: it is resolved against the API origin
 * and fetched through the bearer-aware seam, then rendered as a blob URL.
 */
export async function getDniPhoto(photoUrl: string, signal?: AbortSignal): Promise<Blob> {
  return request<Blob>({
    method: "GET",
    path: resolveApiUrl(photoUrl),
    authorized: true,
    responseType: "blob",
    signal,
  });
}

/**
 * A Persona revokes its own Consent in a Family, ending Visibility of its DNI;
 * the backend answers 204 and refuses anyone else's Consent with `forbidden`
 * (spec #14, "Consent UX").
 */
export async function revokeConsent(familyId: string, personaId: string): Promise<void> {
  await request<void>({
    method: "DELETE",
    path: `/families/${encodeURIComponent(familyId)}/members/${encodeURIComponent(personaId)}/consent`,
    authorized: true,
  });
}

/**
 * The Admin removes an accepted Member; the backend answers 204, refuses a
 * non-admin with `forbidden` and the Admin itself with `cannot-remove-admin`.
 */
export async function removeMember(familyId: string, personaId: string): Promise<void> {
  await request<void>({
    method: "DELETE",
    path: `/families/${encodeURIComponent(familyId)}/members/${encodeURIComponent(personaId)}`,
    authorized: true,
  });
}
