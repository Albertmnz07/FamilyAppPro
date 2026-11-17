// Endpoint functions of the src/api client module, one per backend resource.
import type { components } from "./schema";
import { request } from "./transport";

export type LoginRequest = components["schemas"]["LoginRequest"];
export type LoginResponse = components["schemas"]["LoginResponse"];

/** The session the login call grants, with the fields the contract always sends. */
export type Session = {
  token: string;
  personaId: string;
  username: string;
};

export async function login(credentials: LoginRequest, signal?: AbortSignal): Promise<Session> {
  const data = await request<LoginResponse>({
    method: "POST",
    path: "/auth/login",
    body: credentials,
    authorized: false,
    signal,
  });
  if (!data.token || !data.personaId || !data.username) {
    throw new Error("The login response is missing required fields");
  }
  return { token: data.token, personaId: data.personaId, username: data.username };
}
