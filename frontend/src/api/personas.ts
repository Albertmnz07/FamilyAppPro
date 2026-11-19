// Endpoint functions for the Personas resource. Registration is the only public
// Persona write: a multipart form carrying the credentials as fields. DNI photos
// are optional and uploaded later (spec #26).
import type { components } from "./schema";
import { request } from "./transport";

type PersonaResponse = components["schemas"]["PersonaResponse"];

/** The two sides of a DNI, in the order the upload presents them. */
export type DniSide = "front" | "back";

export type Registration = {
  username: string;
  dni: string;
  password: string;
};

/** The identity the registration call creates, with the fields the contract always sends. */
export type RegisteredPersona = {
  id: string;
  username: string;
  dni: string;
};

/** A 200 with a body that is not a Persona response: a broken response, not a network outage. */
export class InvalidPersonaError extends TypeError {}

/** The identity every Persona response must carry; throws on a malformed body. */
function personaIdentity(data: PersonaResponse): { id: string; username: string } {
  if (typeof data.id !== "string" || typeof data.username !== "string") {
    throw new InvalidPersonaError("The persona response is missing required fields");
  }
  return { id: data.id, username: data.username };
}

export async function register(input: Registration, signal?: AbortSignal): Promise<RegisteredPersona> {
  const form = new FormData();
  form.set("username", input.username);
  form.set("dni", input.dni);
  form.set("password", input.password);

  const data = await request<PersonaResponse>({
    method: "POST",
    path: "/personas",
    form,
    authorized: false,
    signal,
  });
  const { id, username } = personaIdentity(data);
  return { id, username, dni: data.dni ?? "" };
}

/** The acting Persona's own data; photo URLs are null until a side is uploaded (spec #26). */
export type Profile = {
  id: string;
  username: string;
  dni: string;
  frontPhotoUrl: string | null;
  backPhotoUrl: string | null;
};

function toProfile(data: PersonaResponse): Profile {
  const { id, username } = personaIdentity(data);
  return {
    id,
    username,
    dni: data.dni ?? "",
    frontPhotoUrl: typeof data.frontPhotoUrl === "string" ? data.frontPhotoUrl : null,
    backPhotoUrl: typeof data.backPhotoUrl === "string" ? data.backPhotoUrl : null,
  };
}

/** The acting Persona's read-only profile, shown in the personal menu. */
export async function getMe(signal?: AbortSignal): Promise<Profile> {
  const data = await request<PersonaResponse>({
    method: "GET",
    path: "/personas/me",
    authorized: true,
    signal,
  });
  return toProfile(data);
}

/** Each side is independent: an omitted side leaves the stored photo untouched. */
export type DniPhotoUpload = {
  frontPhoto?: File;
  backPhoto?: File;
};

/** Uploads or replaces the acting Persona's optional DNI photos (multipart PUT). */
export async function uploadDniPhotos(input: DniPhotoUpload, signal?: AbortSignal): Promise<Profile> {
  const form = new FormData();
  if (input.frontPhoto) form.set("frontPhoto", input.frontPhoto);
  if (input.backPhoto) form.set("backPhoto", input.backPhoto);
  const data = await request<PersonaResponse>({
    method: "PUT",
    path: "/personas/me/dni/photos",
    form,
    authorized: true,
    signal,
  });
  return toProfile(data);
}
