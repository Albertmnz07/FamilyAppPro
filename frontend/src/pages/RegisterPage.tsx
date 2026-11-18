import { useMutation } from "@tanstack/react-query";
import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react";
import { useNavigate } from "react-router";
import { errorMessages, messageForFailure } from "../api/messages";
import { register, type Registration } from "../api/personas";
import { isApiError, normalizeFieldErrors } from "../api/problem";
import { useUserInvalidBridge } from "../forms/useUserInvalidBridge";

export const registrationSuccessNotice = "Cuenta creada. Ya puedes iniciar sesión.";

const allowedPhotoTypes = ["image/jpeg", "image/png"];
const maxPhotoBytes = 1_048_576;
/** The `accept` attribute restricted to the same types the backend allows. */
const photoAccept = allowedPhotoTypes.join(",");

/** The five inputs, in the order focus should reach them after a failed submit. */
const fieldOrder = ["username", "dni", "password", "frontPhoto", "backPhoto"] as const;
type FieldName = (typeof fieldOrder)[number];

/**
 * The visible Spanish error text of each field, and the fallback used when the
 * server names a field without a message we can show. Photo type/size problems
 * reuse the `code` wording from messages.ts, so client and server rejections of
 * the same rule read identically. The browser's native bubble is not shown: the
 * bridge links this text through aria-describedby.
 */
const fieldMessages: Record<FieldName, string> = {
  username:
    "El nombre de usuario debe tener 3-30 caracteres: letras, dígitos, puntos, guiones bajos o guiones.",
  dni: "Escribe un DNI válido: 8 dígitos y su letra.",
  password: "La contraseña debe tener 8-100 caracteres.",
  frontPhoto: "Elige una foto del DNI.",
  backPhoto: "Elige una foto del DNI.",
};

/** The one field each duplicate-`code` points at. */
const duplicateFieldByCode: Record<string, FieldName> = {
  "duplicate-username": "username",
  "duplicate-dni": "dni",
};

function isFieldName(field: string): field is FieldName {
  return (fieldOrder as readonly string[]).includes(field);
}

/** The client-side check that mirrors the backend: JPEG/PNG, at most 1 MB per photo. */
function photoProblem(file: File): string | null {
  if (!allowedPhotoTypes.includes(file.type)) return errorMessages["unsupported-photo-type"];
  if (file.size > maxPhotoBytes) return errorMessages["photo-too-large"];
  return null;
}

// The same check-letter rule as the backend's DniValidator, so a DNI is rejected
// on blur instead of only after a round trip. An empty value is left to `required`.
const dniCheckLetters = "TRWAGMYFPDXBNJZSQVHLCKE";

function dniProblem(value: string): string | null {
  const normalized = value.trim().toUpperCase();
  if (normalized === "") return null;
  if (!/^\d{8}[A-Z]$/.test(normalized)) return fieldMessages.dni;
  const letter = dniCheckLetters[Number(normalized.slice(0, 8)) % 23];
  return normalized.charAt(8) === letter ? null : fieldMessages.dni;
}

export function RegisterPage() {
  const navigate = useNavigate();
  const formErrorRef = useRef<HTMLParagraphElement | null>(null);
  const refs = useRef<Record<FieldName, HTMLInputElement | null>>({
    username: null,
    dni: null,
    password: null,
    frontPhoto: null,
    backPhoto: null,
  });
  // The object URLs are tracked in a ref (to revoke them on replacement and
  // unmount) and mirrored in state (to render the previews).
  const previews = useRef<Partial<Record<FieldName, string>>>({});
  const [previewUrls, setPreviewUrls] = useState<Partial<Record<FieldName, string>>>({});
  const [serverErrors, setServerErrors] = useState<Partial<Record<FieldName, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const formRef = useUserInvalidBridge<HTMLFormElement>();

  const registerMutation = useMutation({
    mutationFn: (registration: Registration) => register(registration),
    onSuccess: () => {
      navigate("/login", { replace: true, state: { notice: registrationSuccessNotice } });
    },
    onError: (error) => applyServerError(error),
  });

  // Object URLs are released when the page unmounts (spec #14, "Photos": local previews).
  useEffect(() => {
    const urls = previews.current;
    return () => {
      Object.values(urls).forEach((url) => url && URL.revokeObjectURL(url));
    };
  }, []);

  useEffect(() => {
    if (formError) formErrorRef.current?.focus();
  }, [formError]);

  function messageFor(field: FieldName): string {
    return serverErrors[field] ?? fieldMessages[field];
  }

  function clearPreview(field: FieldName): void {
    const url = previews.current[field];
    if (url) URL.revokeObjectURL(url);
    delete previews.current[field];
    setPreviewUrls((current) => {
      if (!(field in current)) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  /**
   * Mark a field invalid with a Spanish message. The custom validity makes the
   * bridge set aria-invalid and link the message, exactly like a native failure.
   */
  function showFieldError(field: FieldName, message: string): void {
    setServerErrors((current) => ({ ...current, [field]: message }));
    const input = refs.current[field];
    if (input) {
      input.setCustomValidity(message);
      input.checkValidity();
    }
  }

  function clearFieldError(field: FieldName): void {
    const input = refs.current[field];
    if (input) input.setCustomValidity("");
    setServerErrors((current) => {
      if (!(field in current)) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  function focusFirstInvalid(fields: FieldName[]): void {
    const first = fieldOrder.find((field) => fields.includes(field));
    if (first) refs.current[first]?.focus();
  }

  function applyServerError(error: unknown): void {
    if (isApiError(error)) {
      const code = error.code;
      if (code === "validation-failed") {
        const { fieldErrors, generalMessages } = normalizeFieldErrors(error.problem?.errors ?? []);
        const named = fieldErrors.map(({ field }) => field).filter(isFieldName);
        named.forEach((field) => showFieldError(field, messageFor(field)));
        if (named.length > 0) focusFirstInvalid(named);
        if (generalMessages.length > 0) setFormError(errorMessages["validation-failed"]);
        if (named.length > 0 || generalMessages.length > 0) return;
      } else if (code !== undefined && code in duplicateFieldByCode) {
        const field = duplicateFieldByCode[code];
        showFieldError(field, errorMessages[code]);
        focusFirstInvalid([field]);
        return;
      } else if (code === "photo-too-large" || code === "unsupported-photo-type") {
        // The response cannot say which photo it rejected, so mark both.
        showFieldError("frontPhoto", errorMessages[code]);
        showFieldError("backPhoto", errorMessages[code]);
        focusFirstInvalid(["frontPhoto"]);
        return;
      }
    }
    setFormError(messageForFailure(error));
  }

  /** Validate the check letter on blur, once the user has finished with the field. */
  function validateDni(): void {
    const dni = refs.current.dni;
    if (!dni) return;
    const problem = dniProblem(dni.value);
    if (problem) showFieldError("dni", problem);
    else clearFieldError("dni");
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    setFormError(null);
    const data = new FormData(event.currentTarget);
    const frontPhoto = data.get("frontPhoto");
    const backPhoto = data.get("backPhoto");
    if (!(frontPhoto instanceof File)) {
      showFieldError("frontPhoto", fieldMessages.frontPhoto);
      return;
    }
    if (!(backPhoto instanceof File)) {
      showFieldError("backPhoto", fieldMessages.backPhoto);
      return;
    }
    registerMutation.mutate({
      username: String(data.get("username") ?? ""),
      dni: String(data.get("dni") ?? ""),
      password: String(data.get("password") ?? ""),
      frontPhoto,
      backPhoto,
    });
  }

  /** A photo rejection is reported against both photos, so both clear together. */
  function clearPhotoErrors(): void {
    clearFieldError("frontPhoto");
    clearFieldError("backPhoto");
  }

  function handlePhotoChange(field: "frontPhoto" | "backPhoto") {
    return (event: ChangeEvent<HTMLInputElement>): void => {
      const input = event.currentTarget;
      clearPhotoErrors();
      clearPreview(field);
      const file = input.files?.[0];
      if (!file) return;
      const problem = photoProblem(file);
      if (problem) {
        showFieldError(field, problem);
        return;
      }
      const url = URL.createObjectURL(file);
      previews.current[field] = url;
      setPreviewUrls((current) => ({ ...current, [field]: url }));
    };
  }

  return (
    <section className="page">
      <h1>Crear cuenta</h1>
      {formError && (
        <p className="form-error" tabIndex={-1} ref={formErrorRef}>
          {formError}
        </p>
      )}
      <form ref={formRef} onSubmit={handleSubmit} className="form">
        <div className="field">
          <label htmlFor="register-username">Nombre de usuario</label>
          <input
            id="register-username"
            name="username"
            type="text"
            autoComplete="username"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            enterKeyHint="next"
            required
            minLength={3}
            maxLength={30}
            pattern="[A-Za-z0-9._-]+"
            ref={(element) => {
              refs.current.username = element;
            }}
            data-error="register-username-error"
            onInputCapture={() => clearFieldError("username")}
          />
          <span id="register-username-error" className="field-error">
            {messageFor("username")}
          </span>
        </div>

        <div className="field">
          <label htmlFor="register-dni">DNI</label>
          <input
            id="register-dni"
            name="dni"
            type="text"
            autoComplete="off"
            autoCapitalize="characters"
            autoCorrect="off"
            spellCheck={false}
            enterKeyHint="next"
            required
            pattern="[0-9]{8}[A-Za-z]"
            ref={(element) => {
              refs.current.dni = element;
            }}
            data-error="register-dni-error"
            onInputCapture={() => clearFieldError("dni")}
            onBlur={validateDni}
          />
          <span id="register-dni-error" className="field-error">
            {messageFor("dni")}
          </span>
        </div>

        <div className="field">
          <label htmlFor="register-password">Contraseña</label>
          <input
            id="register-password"
            name="password"
            type="password"
            autoComplete="new-password"
            enterKeyHint="next"
            required
            minLength={8}
            maxLength={100}
            ref={(element) => {
              refs.current.password = element;
            }}
            data-error="register-password-error"
            onInputCapture={() => clearFieldError("password")}
          />
          <span id="register-password-error" className="field-error">
            {messageFor("password")}
          </span>
        </div>

        <div className="field">
          <label htmlFor="register-front-photo">Foto delantera del DNI</label>
          <input
            id="register-front-photo"
            name="frontPhoto"
            type="file"
            accept={photoAccept}
            required
            ref={(element) => {
              refs.current.frontPhoto = element;
            }}
            data-error="register-front-photo-error"
            onInputCapture={clearPhotoErrors}
            onChange={handlePhotoChange("frontPhoto")}
          />
          <span id="register-front-photo-error" className="field-error">
            {messageFor("frontPhoto")}
          </span>
          {previewUrls.frontPhoto && (
            <img
              className="photo-preview"
              src={previewUrls.frontPhoto}
              alt="Vista previa de la foto delantera del DNI"
            />
          )}
        </div>

        <div className="field">
          <label htmlFor="register-back-photo">Foto trasera del DNI</label>
          <input
            id="register-back-photo"
            name="backPhoto"
            type="file"
            accept={photoAccept}
            required
            ref={(element) => {
              refs.current.backPhoto = element;
            }}
            data-error="register-back-photo-error"
            onInputCapture={clearPhotoErrors}
            onChange={handlePhotoChange("backPhoto")}
          />
          <span id="register-back-photo-error" className="field-error">
            {messageFor("backPhoto")}
          </span>
          {previewUrls.backPhoto && (
            <img
              className="photo-preview"
              src={previewUrls.backPhoto}
              alt="Vista previa de la foto trasera del DNI"
            />
          )}
        </div>

        <button type="submit" disabled={registerMutation.isPending}>
          {registerMutation.isPending ? "Creando cuenta…" : "Crear cuenta"}
        </button>
      </form>
    </section>
  );
}
