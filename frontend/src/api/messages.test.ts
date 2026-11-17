import { fromPartial } from "@total-typescript/shoehorn";
import { describe, expect, it } from "vitest";
import { InvalidPageError } from "./page";
import { ApiError, type ProblemDetail } from "./problem";
import {
  errorMessages,
  loginFailedMessage,
  messageForFailure,
  messageForProblem,
  networkErrorMessage,
  unexpectedResponseMessage,
} from "./messages";

// The spec (#14) lists exactly these ProblemDetail codes; every one of them
// must resolve to its own Spanish message instead of the generic fallback.
const specCodes = [
  "unauthorized",
  "forbidden",
  "not-found",
  "duplicate-dni",
  "duplicate-username",
  "invite-expired",
  "invite-already-redeemed",
  "cannot-remove-admin",
  "unsupported-photo-type",
  "photo-too-large",
  "validation-failed",
  "bad-request",
] as const;

const fallbackMessage = "Ha ocurrido un error inesperado. Inténtalo de nuevo.";

describe("messageForProblem", () => {
  it("maps every ProblemDetail code the spec lists to a specific Spanish message", () => {
    for (const code of specCodes) {
      expect(messageForProblem(fromPartial<ProblemDetail>({ code }))).not.toBe(fallbackMessage);
    }
    expect(Object.keys(errorMessages).sort()).toEqual([...specCodes].sort());
  });

  it("spells out the wording for representative codes", () => {
    expect(messageForProblem(fromPartial<ProblemDetail>({ code: "duplicate-username" }))).toBe(
      "Ese nombre de usuario ya está en uso.",
    );
    expect(messageForProblem(fromPartial<ProblemDetail>({ code: "cannot-remove-admin" }))).toBe(
      "El administrador de la familia no puede ser retirado.",
    );
  });

  it("reads rejected credentials as the backend's unauthorized code on the login screen", () => {
    expect(messageForProblem(fromPartial<ProblemDetail>({ code: "unauthorized" }), "login")).toBe(
      loginFailedMessage,
    );
  });

  it("keeps the session-over reading of unauthorized outside the login screen", () => {
    expect(messageForProblem(fromPartial<ProblemDetail>({ code: "unauthorized" }))).toBe(
      "Necesitas iniciar sesión para continuar.",
    );
  });

  it("falls back to a generic Spanish message for unknown codes", () => {
    expect(messageForProblem(fromPartial<ProblemDetail>({ code: "something-new" }))).toBe(fallbackMessage);
    expect(messageForProblem({})).toBe(fallbackMessage);
  });

  it("offers a distinct message for connection failures", () => {
    expect(networkErrorMessage).toBe("No se pudo conectar con el servidor. Inténtalo de nuevo.");
  });
});

describe("messageForFailure", () => {
  it("reads the problem code for API failures, including the login context", () => {
    expect(messageForFailure(new ApiError(401, { code: "unauthorized" }), "login")).toBe(loginFailedMessage);
    expect(messageForFailure(new ApiError(403, { code: "forbidden" }))).toBe(
      "No tienes permiso para hacer eso.",
    );
  });

  it("does not blame the network for a malformed successful response", () => {
    expect(messageForFailure(new InvalidPageError("no content array"))).toBe(unexpectedResponseMessage);
    expect(messageForFailure(new TypeError("Failed to fetch"))).toBe(networkErrorMessage);
  });
});
