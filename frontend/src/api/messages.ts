// Every failure the API can return is explained in Spanish with consistent
// wording, mapped from the ProblemDetail `code` (spec #14, "Error mapping").
import { InvalidDniError } from "./members";
import { InvalidPageError } from "./page";
import { isApiError, type ProblemDetail } from "./problem";

export const errorMessages: Record<string, string> = {
  unauthorized: "Necesitas iniciar sesión para continuar.",
  forbidden: "No tienes permiso para hacer eso.",
  "not-found": "No se encontró lo que buscabas.",
  "duplicate-dni": "Ya existe una persona registrada con ese DNI.",
  "duplicate-username": "Ese nombre de usuario ya está en uso.",
  "invite-expired": "La invitación ha expirado. Pide una nueva.",
  "invite-already-redeemed": "La invitación ya se ha utilizado.",
  "cannot-remove-admin": "El administrador de la familia no puede ser retirado.",
  "unsupported-photo-type": "Las fotos deben ser JPEG o PNG.",
  "photo-too-large": "Cada foto puede pesar 1 MB como máximo.",
  "validation-failed": "Revisa los campos marcados.",
  "bad-request": "La petición no es válida.",
};

const fallbackMessage = "Ha ocurrido un error inesperado. Inténtalo de nuevo.";

export const networkErrorMessage = "No se pudo conectar con el servidor. Inténtalo de nuevo.";

/** On the login screen the same 401 code means rejected credentials, not a lost session. */
export const loginFailedMessage = "Usuario o contraseña incorrectos.";

export function messageForProblem(problem: ProblemDetail, context?: "login"): string {
  if (context === "login" && problem.code === "unauthorized") return loginFailedMessage;
  return (problem.code !== undefined && errorMessages[problem.code]) || fallbackMessage;
}

export const unexpectedResponseMessage = "La respuesta del servidor no es válida. Inténtalo de nuevo.";

export const dniWithoutConsentMessage =
  "Esta persona no tiene el consentimiento vigente en esta familia.";
export const dniNotMemberMessage = "Esta persona ya no es miembro de la familia.";

/** One place that turns any failed request into the Spanish message the screens show. */
export function messageForFailure(error: unknown, context?: "login"): string {
  if (isApiError(error)) return messageForProblem(error.problem ?? {}, context);
  if (error instanceof InvalidPageError || error instanceof InvalidDniError) {
    return unexpectedResponseMessage;
  }
  return networkErrorMessage;
}

/**
 * On the DNI view the reason matters more than the generic code: a missing
 * Consent is a privacy rule, while a 404 means the Persona is no longer a
 * Member of the Family.
 */
export function messageForDniFailure(error: unknown): string {
  if (isApiError(error) && error.status === 403) return dniWithoutConsentMessage;
  if (isApiError(error) && error.status === 404) return dniNotMemberMessage;
  return messageForFailure(error);
}
