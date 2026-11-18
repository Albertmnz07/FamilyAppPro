import { fromPartial } from "@total-typescript/shoehorn";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import type { components } from "../api/schema";
import type { ProblemDetail } from "../api/problem";
import { clearToken, getPersonaId, getToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { LoginPage } from "./LoginPage";

const sessionExpiredNotice = "Tu sesión ha finalizado. Vuelve a iniciar sesión.";

// Fixtures typed against the generated contract so drift shows up as a type error.
const loginResponseBody = fromPartial<components["schemas"]["LoginResponse"]>({
  token: "token-1",
  personaId: "p1",
  username: "ana",
});
const unauthorizedBody = fromPartial<ProblemDetail>({
  title: "Unauthorized",
  status: 401,
  detail: "Invalid credentials",
  code: "unauthorized",
});

function renderLogin(options?: { state?: unknown }) {
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
      <MemoryRouter initialEntries={[{ pathname: "/login", state: options?.state }]}>
        <LocationProbe />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/families" element={<p>pantalla de familias</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return { currentPath: () => currentPath };
}

async function fillCredentials(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Nombre de usuario"), "ana");
  await user.type(screen.getByLabelText("Contraseña"), "secret-1");
  await user.click(screen.getByRole("button", { name: "Iniciar sesión" }));
}

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("LoginPage", () => {
  it("renders the login form in Spanish with autofill hints", () => {
    renderLogin();

    expect(screen.getByRole("heading", { name: "Iniciar sesión" })).toBeInTheDocument();
    expect(screen.getByLabelText("Nombre de usuario")).toHaveAttribute("autocomplete", "username");
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("autocomplete", "current-password");
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("type", "password");
    expect(screen.getByRole("button", { name: "Iniciar sesión" })).toBeInTheDocument();
  });

  it("logs in with valid credentials and lands on /families", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(loginResponseBody));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    const { currentPath } = renderLogin();

    await fillCredentials(user);

    await waitFor(() => expect(currentPath()).toBe("/families"));
    expect(screen.getByText("pantalla de familias")).toBeInTheDocument();
    expect(getToken()).toBe("token-1");
    expect(getPersonaId()).toBe("p1");
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/auth/login");
    expect(init.headers.get("Authorization")).toBeNull();
  });

  it("explains wrong credentials in Spanish and focuses the message", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(unauthorizedBody, 401, "application/problem+json"));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    const { currentPath } = renderLogin();

    await fillCredentials(user);

    const message = await screen.findByText("Usuario o contraseña incorrectos.");
    await waitFor(() => expect(message).toHaveFocus());
    expect(currentPath()).toBe("/login");
    expect(getToken()).toBeNull();
  });

  it("blocks an empty submit with native validation and explains the empty fields", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderLogin();

    await user.click(screen.getByRole("button", { name: "Iniciar sesión" }));

    expect(fetchMock).not.toHaveBeenCalled();
    const username = screen.getByLabelText("Nombre de usuario");
    expect(username).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByText("Escribe tu nombre de usuario.")).toBeInTheDocument();
  });

  it("announces the error of a field left empty through one polite live region", async () => {
    const user = userEvent.setup();
    renderLogin();

    // Edit and leave the field empty: that is when the error appears.
    const username = screen.getByLabelText("Nombre de usuario");
    await user.type(username, "a{backspace}");
    await user.tab();

    expect(username).toHaveAttribute("aria-invalid", "true");
    await waitFor(() =>
      expect(document.querySelector('[aria-live="polite"]')).toHaveTextContent(
        "Nombre de usuario: Escribe tu nombre de usuario.",
      ),
    );
  });

  it("clears the field error again once the user fixes it", async () => {
    const user = userEvent.setup();
    renderLogin();

    const username = screen.getByLabelText("Nombre de usuario");
    await user.type(username, "a{backspace}");
    await user.tab();
    await waitFor(() => expect(username).toHaveAttribute("aria-invalid", "true"));

    await user.type(username, "a");
    expect(username).toHaveAttribute("aria-invalid", "false");
  });

  it("shows the connection-failure message when the network is down", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));
    const user = userEvent.setup();
    renderLogin();

    await fillCredentials(user);

    expect(await screen.findByText("No se pudo conectar con el servidor. Inténtalo de nuevo.")).toBeInTheDocument();
  });

  it("shows the session-expired notice carried in the router state", () => {
    renderLogin({ state: { notice: sessionExpiredNotice } });

    expect(screen.getByText(sessionExpiredNotice)).toBeInTheDocument();
  });
});
