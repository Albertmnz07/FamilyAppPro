// Every failure the API can return is explained in Spanish with consistent
// wording, mapped from the ProblemDetail `code` (spec #14, "Error mapping").
import type { ProblemDetail } from "./problem";

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

/** Wrong credentials are a 401 on the login call itself; the screen owns this wording. */
export const loginFailedMessage = "Usuario o contraseña incorrectos.";

export function messageForProblem(problem: ProblemDetail): string {
  return (problem.code !== undefined && errorMessages[problem.code]) || fallbackMessage;
}
