// Endpoint functions for Invites, layered over the single HTTP seam in
// transport.ts: an Admin issues one for a Family, and a Persona redeems a
// code. A pasted code is normalized before the request so a sloppy paste still
// joins the Family (spec #14, "Invite UX").
import type { components } from "./schema";
import { request } from "./transport";

type RedemptionResponse = components["schemas"]["RedemptionResponse"];
type InviteResponse = components["schemas"]["InviteResponse"];

/** What redeeming an Invite gives back: the Family joined and the accepted Membership. */
export type Redemption = {
  familyId: string;
  personaId: string;
  status: string;
};

/** An Invite issued by an Admin: the six-character code and when it stops working. */
export type IssuedInvite = {
  code: string;
  familyId: string;
  /** ISO-8601 instant; the workspace renders it in the browser's local time. */
  expiresAt: string;
};

/** A 201 whose body is not a redemption response: a broken response, not a network outage. */
export class InvalidRedemptionError extends TypeError {}

/** A 201 whose body is not an invite response: a broken response, not a network outage. */
export class InvalidInviteError extends TypeError {}

/** Uppercases and trims the code; the backend's lookup is case-insensitive but the URL stays canonical. */
export function normalizeInviteCode(code: string): string {
  return code.trim().toUpperCase();
}

/**
 * Issues an Invite for a Family. Only the Admin may call this (the backend
 * answers `forbidden` otherwise), so the workspace shows the action to no one
 * else (spec #14, "Invite UX").
 */
export async function issueInvite(familyId: string): Promise<IssuedInvite> {
  const payload = await request<InviteResponse>({
    method: "POST",
    path: `/families/${encodeURIComponent(familyId)}/invites`,
    authorized: true,
  });
  if (
    typeof payload.code !== "string" ||
    typeof payload.familyId !== "string" ||
    typeof payload.expiresAt !== "string" ||
    Number.isNaN(Date.parse(payload.expiresAt))
  ) {
    throw new InvalidInviteError("The invite response is missing required fields");
  }
  return { code: payload.code, familyId: payload.familyId, expiresAt: payload.expiresAt };
}

export async function redeemInvite(code: string): Promise<Redemption> {
  const payload = await request<RedemptionResponse>({
    method: "POST",
    path: `/invites/${encodeURIComponent(normalizeInviteCode(code))}/redemption`,
    authorized: true,
  });
  if (
    typeof payload.familyId !== "string" ||
    typeof payload.personaId !== "string" ||
    typeof payload.status !== "string"
  ) {
    throw new InvalidRedemptionError("The redemption response is missing required fields");
  }
  return { familyId: payload.familyId, personaId: payload.personaId, status: payload.status };
}
