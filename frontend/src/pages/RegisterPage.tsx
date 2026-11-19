import { useMutation } from "@tanstack/react-query";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router";
import { errorMessages, messageForFailure } from "../api/messages";
import { register, type Registration } from "../api/personas";
import { isApiError, normalizeFieldErrors } from "../api/problem";
import { useUserInvalidBridge } from "../forms/useUserInvalidBridge";

export const registrationSuccessNotice = "Cuenta creada. Ya puedes iniciar sesión.";

/** The three inputs, in the order focus should reach them after a failed submit. */
const fieldOrder = ["username", "dni", "password"] as const;
type FieldName = (typeof fieldOrder)[number];

/**
 * The visible Spanish error text of each field, and the fallback used when the
 * server names a field without a message we can show. The DNI number has no
 * format rule: any non-blank value is accepted (spec #26). The browser's native
 * bubble is not shown: the bridge links this text through aria-describedby.
 */
const fieldMessages: Record<FieldName, string> = {
  username:
    "El nombre de usuario debe tener 3-30 caracteres: letras, dígitos, puntos, guiones bajos o guiones.",
  dni: "Escribe tu número de DNI.",
  password: "La contraseña debe tener 8-100 caracteres.",
};

/** The one field each duplicate-`code` points at. */
const duplicateFieldByCode: Record<string, FieldName> = {
  "duplicate-username": "username",
  "duplicate-dni": "dni",
};

function isFieldName(field: string): field is FieldName {
  return (fieldOrder as readonly string[]).includes(field);
}

export function RegisterPage() {
  const navigate = useNavigate();
  const formErrorRef = useRef<HTMLParagraphElement | null>(null);
  const refs = useRef<Record<FieldName, HTMLInputElement | null>>({
    username: null,
    dni: null,
    password: null,
  });
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

  useEffect(() => {
    if (formError) formErrorRef.current?.focus();
  }, [formError]);

  function messageFor(field: FieldName): string {
    return serverErrors[field] ?? fieldMessages[field];
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
      }
    }
    setFormError(messageForFailure(error));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();
    setFormError(null);
    const data = new FormData(event.currentTarget);
    registerMutation.mutate({
      username: String(data.get("username") ?? ""),
      dni: String(data.get("dni") ?? ""),
      password: String(data.get("password") ?? ""),
    });
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
            ref={(element) => {
              refs.current.dni = element;
            }}
            data-error="register-dni-error"
            onInputCapture={() => clearFieldError("dni")}
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
            enterKeyHint="done"
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

        <button type="submit" disabled={registerMutation.isPending}>
          {registerMutation.isPending ? "Creando cuenta…" : "Crear cuenta"}
        </button>
      </form>
      <p className="form-switch">
        ¿Ya tienes cuenta? <Link to="/login">Iniciar sesión</Link>
      </p>
    </section>
  );
}
