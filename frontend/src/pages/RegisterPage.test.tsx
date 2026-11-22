import { fromPartial } from "@total-typescript/shoehorn";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import type { components } from "../api/schema";
import type { ProblemDetail } from "../api/problem";
import { clearToken, getToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { LoginPage } from "./LoginPage";
import { RegisterPage, registrationSuccessNotice } from "./RegisterPage";

// The happy paths dispatch the submit event directly once the form is complete
// (as the user would).
function submitRegisterForm(): void {
  fireEvent.submit(document.querySelector("form")!);
}

// Fixtures typed against the generated contract so drift shows up as a type error.
const personaResponseBody = fromPartial<components["schemas"]["PersonaResponse"]>({
  id: "p1",
  username: "ana",
  dni: "12345678Z",
});
const loginResponseBody = fromPartial<components["schemas"]["LoginResponse"]>({
  token: "token-1",
  personaId: "p1",
  username: "ana",
});
const duplicateUsernameBody = fromPartial<ProblemDetail>({
  title: "Conflict",
  status: 409,
  code: "duplicate-username",
});
const duplicateDniBody = fromPartial<ProblemDetail>({
  title: "Conflict",
  status: 409,
  code: "duplicate-dni",
});
const validationFailedBody = fromPartial<ProblemDetail>({
  title: "Validation failed",
  status: 400,
  code: "validation-failed",
  errors: [{ field: "register.username", message: "must be 3-30 characters" }],
});
const requestLevelFailureBody = fromPartial<ProblemDetail>({
  title: "Validation failed",
  status: 400,
  code: "validation-failed",
  errors: [{ field: "request", message: "Invalid request" }],
});

function renderRegister() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  let currentPath = "";
  function LocationProbe() {
    const location = useLocation();
    currentPath = location.pathname;
    return null;
  }
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/register"]}>
        <LocationProbe />
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/families" element={<p>pantalla de familias</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return { currentPath: () => currentPath };
}

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Nombre de usuario"), "ana");
  await user.type(screen.getByLabelText("DNI"), "12345678Z");
  await user.type(screen.getByLabelText("Contraseña"), "secret-1");
}

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("RegisterPage", () => {
  it("renders the registration form in Spanish with autofill hints and no photo inputs", () => {
    renderRegister();

    expect(screen.getByRole("heading", { name: "Crear cuenta" })).toBeInTheDocument();
    expect(screen.getByLabelText("Nombre de usuario")).toHaveAttribute("autocomplete", "username");
    expect(screen.getByLabelText("DNI")).toHaveAttribute("autocomplete", "off");
    expect(screen.getByLabelText("DNI")).not.toHaveAttribute("pattern");
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("autocomplete", "new-password");
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("type", "password");
    expect(document.querySelectorAll('input[type="file"]')).toHaveLength(0);
    expect(screen.getByRole("button", { name: "Crear cuenta" })).toBeInTheDocument();
  });

  it("submits the registration as multipart form data and lands on login with a notice", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(personaResponseBody, 201));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    const { currentPath } = renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    // findByText instead of getByText: the notice renders when LoginPage mounts
    // after the navigation, which can land a tick later than the path probe.
    await screen.findByText(registrationSuccessNotice);
    expect(currentPath()).toBe("/login");

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/personas");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Authorization")).toBeNull();
    const body = init.body as FormData;
    expect(body).toBeInstanceOf(FormData);
    expect(body.get("username")).toBe("ana");
    expect(body.get("dni")).toBe("12345678Z");
    expect(body.get("password")).toBe("secret-1");
    expect(body.get("frontPhoto")).toBeNull();
    expect(body.get("backPhoto")).toBeNull();
  });

  it("lets the new Persona log in right after registering", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(personaResponseBody, 201))
      .mockResolvedValueOnce(jsonResponse(loginResponseBody));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    const { currentPath } = renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    await screen.findByRole("heading", { name: "Iniciar sesión" });
    await user.type(screen.getByLabelText("Nombre de usuario"), "ana");
    await user.type(screen.getByLabelText("Contraseña"), "secret-1");
    await user.click(screen.getByRole("button", { name: "Iniciar sesión" }));

    await waitFor(() => expect(currentPath()).toBe("/families"));
    expect(getToken()).toBe("token-1");
  });

  it("explains a duplicate username in Spanish on the username field", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(duplicateUsernameBody, 409)));
    const user = userEvent.setup();
    renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    expect(await screen.findByText("Ese nombre de usuario ya está en uso.")).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByLabelText("Nombre de usuario")).toHaveAttribute("aria-invalid", "true"),
    );
  });

  it("explains a duplicate DNI in Spanish and focuses the DNI field", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(duplicateDniBody, 409)));
    const user = userEvent.setup();
    renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    expect(
      await screen.findByText("Ya existe una persona registrada con ese DNI."),
    ).toBeInTheDocument();
    await waitFor(() => expect(screen.getByLabelText("DNI")).toHaveFocus());
  });

  it("maps a validation-failed field error onto the offending input", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(jsonResponse(validationFailedBody, 400, "application/problem+json")),
    );
    const user = userEvent.setup();
    renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    await waitFor(() =>
      expect(screen.getByLabelText("Nombre de usuario")).toHaveAttribute("aria-invalid", "true"),
    );
  });

  it("accepts a DNI without the Spanish format", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(personaResponseBody, 201));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderRegister();

    const dni = screen.getByLabelText("DNI");
    await user.type(dni, "DNI-SIN-FORMATO");
    await user.tab();
    expect(dni).toHaveAttribute("aria-invalid", "false");

    await user.type(screen.getByLabelText("Nombre de usuario"), "ana");
    await user.type(screen.getByLabelText("Contraseña"), "secret-1");
    submitRegisterForm();

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
  });

  it("accepts a password of three characters", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(personaResponseBody, 201));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderRegister();

    await user.type(screen.getByLabelText("Nombre de usuario"), "ana");
    await user.type(screen.getByLabelText("DNI"), "12345678Z");
    await user.type(screen.getByLabelText("Contraseña"), "abc");
    await user.tab();
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("aria-invalid", "false");

    submitRegisterForm();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
  });

  it("shows a general message for a request-level validation failure", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(requestLevelFailureBody, 400)));
    const user = userEvent.setup();
    renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    expect(await screen.findByText("Revisa los campos marcados.")).toBeInTheDocument();
  });

  it("blocks an empty submit with native validation", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderRegister();

    await user.click(screen.getByRole("button", { name: "Crear cuenta" }));

    expect(fetchMock).not.toHaveBeenCalled();
    expect(screen.getByLabelText("Nombre de usuario")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText("DNI")).toHaveAttribute("aria-invalid", "true");
  });

  it("announces the error of a field left empty through one polite live region", async () => {
    const user = userEvent.setup();
    renderRegister();

    const username = screen.getByLabelText("Nombre de usuario");
    await user.type(username, "a{backspace}");
    await user.tab();

    expect(username).toHaveAttribute("aria-invalid", "true");
    await waitFor(() =>
      expect(document.querySelector('[aria-live="polite"]')).toHaveTextContent(
        "Nombre de usuario: El nombre de usuario debe tener 3-30 caracteres",
      ),
    );
  });

  it("shows the connection-failure message when the network is down", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));
    const user = userEvent.setup();
    renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    expect(
      await screen.findByText("No se pudo conectar con el servidor. Inténtalo de nuevo."),
    ).toBeInTheDocument();
  });

  it("offers a way back to the login screen and navigates to /login", async () => {
    const user = userEvent.setup();
    const { currentPath } = renderRegister();

    const link = screen.getByRole("link", { name: "Iniciar sesión" });
    expect(link).toHaveAttribute("href", "/login");

    await user.click(link);

    await waitFor(() => expect(currentPath()).toBe("/login"));
  });
});
