// Endpoint function for redeeming an Invite, layered over the single HTTP seam
// in transport.ts. A pasted code is normalized before the request so a sloppy
// paste still joins the Family (spec #14, "Invite UX").
import type { components } from "./schema";
import { request } from "./transport";

type RedemptionResponse = components["schemas"]["RedemptionResponse"];

/** What redeeming an Invite gives back: the Family joined and the accepted Membership. */
export type Redemption = {
  familyId: string;
  personaId: string;
  status: string;
};

/** A 201 whose body is not a redemption response: a broken response, not a network outage. */
export class InvalidRedemptionError extends TypeError {}

/** Uppercases and trims the code; the backend's lookup is case-insensitive but the URL stays canonical. */
export function normalizeInviteCode(code: string): string {
  return code.trim().toUpperCase();
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
