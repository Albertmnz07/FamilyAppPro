// The one place that talks HTTP: every request the front-end makes goes through
// this module (spec #14, "One seam: the HTTP boundary"). Endpoint functions in
// src/api call `request`; nothing else imports `fetch`.
import { getToken } from "../auth/token";
import { ApiError } from "./problem";

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8070/api/v1";

/**
 * Absolute API base URL, overridable per environment (ADR-0006).
 *
 * Development bakes an absolute `http://localhost:8070/api/v1`. The production
 * image bakes the same-origin relative `/api/v1`, which is resolved against the
 * page origin so one build works from any host or LAN device.
 */
export const apiBaseUrl = (
  configuredBaseUrl.startsWith("/")
    ? new URL(configuredBaseUrl, window.location.origin).toString()
    : configuredBaseUrl
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

/**
 * Resolves a server-relative URL taken from a DTO (e.g. a DNI photo URL)
 * against the API origin — the page origin itself in the same-origin production
 * build, a separate host in development.
 */
export function resolveApiUrl(path: string): string {
  return new URL(path, `${new URL(apiBaseUrl).origin}/`).toString();
}

export type RequestInit = {
  method: "GET" | "POST" | "PUT" | "DELETE";
  path: string;
  query?: QueryParams;
  /** JSON body; omitted for GET/DELETE. */
  body?: unknown;
  /**
   * multipart/form-data body (registration). The browser sets its own
   * Content-Type with the generated boundary, so this omits the JSON header.
   */
  form?: FormData;
  /** Authenticated calls carry `Authorization: Bearer`; login and registration omit it. */
  authorized?: boolean;
  signal?: AbortSignal;
  /** Photo bytes are binary; never send them through the JSON parser. */
  responseType?: "blob";
};

export async function request<T>(options: RequestInit): Promise<T> {
  const headers = new Headers({ Accept: "application/json" });
  if (options.body !== undefined) headers.set("Content-Type", "application/json");
  if (options.authorized) {
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const body = options.form ?? (options.body === undefined ? undefined : JSON.stringify(options.body));
  const response = await fetch(buildUrl(options.path, options.query), {
    method: options.method,
    headers,
    body,
    signal: options.signal,
  });
  if (!response.ok) {
    throw await ApiError.fromResponse(response);
  }
  if (options.responseType === "blob") {
    return (await response.blob()) as T;
  }
  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}
