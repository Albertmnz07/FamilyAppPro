// Endpoint functions for the Personas resource. Registration is the only public
// Persona write: a multipart form carrying the credentials as fields and the two
// DNI photos as parts (spec #14, "Photos"), so it bypasses the JSON body path.
import type { components } from "./schema";
import { request } from "./transport";

type PersonaResponse = components["schemas"]["PersonaResponse"];

export type Registration = {
  username: string;
  dni: string;
  password: string;
  frontPhoto: File;
  backPhoto: File;
};

/** The identity the registration call creates, with the fields the contract always sends. */
export type RegisteredPersona = {
  id: string;
  username: string;
  dni: string;
};

export async function register(input: Registration, signal?: AbortSignal): Promise<RegisteredPersona> {
  const form = new FormData();
  form.set("username", input.username);
  form.set("dni", input.dni);
  form.set("password", input.password);
  form.set("frontPhoto", input.frontPhoto);
  form.set("backPhoto", input.backPhoto);

  const data = await request<PersonaResponse>({
    method: "POST",
    path: "/personas",
    form,
    authorized: false,
    signal,
  });
  if (typeof data.id !== "string" || typeof data.username !== "string") {
    throw new Error("The registration response is missing required fields");
  }
  return { id: data.id, username: data.username, dni: data.dni ?? "" };
}
