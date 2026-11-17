// The one place that talks HTTP: every request the front-end makes goes through
// this module (spec #14, "One seam: the HTTP boundary"). Endpoint functions in
// src/api call `request`; nothing else imports `fetch`.
import { getToken } from "../auth/token";
import { ApiError } from "./problem";

/** Absolute API base URL, overridable per environment (ADR-0006). */
export const apiBaseUrl = (
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1"
).replace(/\/+$/, "");

export type QueryParams = Record<string, string | number | boolean | undefined>;

export function buildUrl(path: string, query?: QueryParams): string {
  const relativePath = path.replace(/^\/+/, "");
  const url = new URL(relativePath, `${apiBaseUrl}/`);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined) url.searchParams.set(key, String(value));
    }
  }
  return url.toString();
}

export type RequestInit = {
  method: "GET" | "POST" | "DELETE";
  path: string;
  query?: QueryParams;
  /** JSON body; omitted for GET/DELETE. */
  body?: unknown;
  /** Authenticated calls carry `Authorization: Bearer`; login and registration omit it. */
  authorized?: boolean;
  signal?: AbortSignal;
};

export async function request<T>(options: RequestInit): Promise<T> {
  const headers = new Headers({ Accept: "application/json" });
  if (options.body !== undefined) headers.set("Content-Type", "application/json");
  if (options.authorized) {
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(buildUrl(options.path, options.query), {
    method: options.method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal,
  });
  if (!response.ok) {
    throw await ApiError.fromResponse(response);
  }
  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}
