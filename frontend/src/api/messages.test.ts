import { fromPartial } from "@total-typescript/shoehorn";
import { describe, expect, it } from "vitest";
import type { ProblemDetail } from "./problem";
import { loginFailedMessage, messageForProblem, networkErrorMessage } from "./messages";

describe("messageForProblem", () => {
  it("maps the backend's ProblemDetail codes to Spanish messages", () => {
    expect(messageForProblem(fromPartial<ProblemDetail>({ code: "duplicate-username" }))).toBe(
      "Ese nombre de usuario ya está en uso.",
    );
    expect(messageForProblem(fromPartial<ProblemDetail>({ code: "cannot-remove-admin" }))).toBe(
      "El administrador de la familia no puede ser retirado.",
    );
  });

  it("falls back to a generic Spanish message for unknown codes", () => {
    expect(messageForProblem(fromPartial<ProblemDetail>({ code: "something-new" }))).toBe(
      "Ha ocurrido un error inesperado. Inténtalo de nuevo.",
    );
    expect(messageForProblem({})).toBe("Ha ocurrido un error inesperado. Inténtalo de nuevo.");
  });

  it("offers a distinct message for connection failures", () => {
    expect(networkErrorMessage).toBe("No se pudo conectar con el servidor. Inténtalo de nuevo.");
    expect(loginFailedMessage).toBe("Usuario o contraseña incorrectos.");
  });
});
