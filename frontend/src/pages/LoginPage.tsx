import { useMutation } from "@tanstack/react-query";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useLocation, useNavigate } from "react-router";
import { login, type LoginRequest } from "../api/auth";
import { messageForProblem, networkErrorMessage } from "../api/messages";
import { isApiError } from "../api/problem";
import { setToken } from "../auth/token";
import { useUserInvalidBridge } from "../forms/useUserInvalidBridge";

function readNotice(state: unknown): string | null {
  if (typeof state === "object" && state !== null && "notice" in state) {
    const notice = (state as { notice: unknown }).notice;
    if (typeof notice === "string") return notice;
  }
  return null;
}

function loginErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    // On the login screen the backend's `code` decides the message; its
    // "unauthorized" here means rejected credentials, not a lost session.
    return messageForProblem(error.problem ?? {}, "login");
  }
  return networkErrorMessage;
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [formError, setFormError] = useState<string | null>(null);
  const formErrorRef = useRef<HTMLParagraphElement | null>(null);
  const formRef = useUserInvalidBridge();

  const loginMutation = useMutation({
    mutationFn: (credentials: LoginRequest) => login(credentials),
    onSuccess: (session) => {
      setToken(session.token);
      navigate("/families", { replace: true });
    },
    onError: (error) => setFormError(loginErrorMessage(error)),
  });

  const notice = readNotice(location.state);

  useEffect(() => {
    if (formError) formErrorRef.current?.focus();
  }, [formError]);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    const data = new FormData(event.currentTarget);
    loginMutation.mutate({
      username: String(data.get("username") ?? ""),
      password: String(data.get("password") ?? ""),
    });
  }

  return (
    <section className="page">
      <h1>Iniciar sesión</h1>
      {notice && (
        <p className="notice" role="status">
          {notice}
        </p>
      )}
      {formError && (
        <p className="form-error" tabIndex={-1} ref={formErrorRef}>
          {formError}
        </p>
      )}
      <form ref={formRef} onSubmit={handleSubmit} className="form">
        <div className="field">
          <label htmlFor="login-username">Nombre de usuario</label>
          <input
            id="login-username"
            name="username"
            type="text"
            autoComplete="username"
            autoCapitalize="none"
            autoCorrect="off"
            spellCheck={false}
            enterKeyHint="done"
            required
            data-error="login-username-error"
          />
          <span id="login-username-error" className="field-error">
            Escribe tu nombre de usuario.
          </span>
        </div>
        <div className="field">
          <label htmlFor="login-password">Contraseña</label>
          <input
            id="login-password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            data-error="login-password-error"
          />
          <span id="login-password-error" className="field-error">
            Escribe tu contraseña.
          </span>
        </div>
        <button type="submit" disabled={loginMutation.isPending}>
          {loginMutation.isPending ? "Entrando…" : "Iniciar sesión"}
        </button>
      </form>
    </section>
  );
}
