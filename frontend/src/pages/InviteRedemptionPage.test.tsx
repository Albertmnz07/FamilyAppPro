import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router";
import { clearToken, setToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { InviteRedemptionPage } from "./InviteRedemptionPage";

function renderRedemption(path: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/invites/:code" element={<InviteRedemptionPage />} />
          <Route path="/families/:familyId" element={<p>pantalla de la familia</p>} />
          <Route path="/families" element={<p>lista de familias</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  setToken("token-1");
});

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("InviteRedemptionPage", () => {
  it("normalizes a pasted code, redeems it in one request and offers the joined Family", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ familyId: "f1", personaId: "p1", status: "ACCEPTED" }, 201));
    vi.stubGlobal("fetch", fetchMock);
    renderRedemption("/invites/%20ab12cd%20");

    expect(await screen.findByText("Te has unido a la familia.")).toBeInTheDocument();

    const workspace = screen.getByRole("link", { name: "Abrir la familia" });
    expect(workspace).toHaveAttribute("href", "/families/f1");
    expect(screen.getByRole("link", { name: "Ver mis familias" })).toHaveAttribute("href", "/families");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8070/api/v1/invites/AB12CD/redemption");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });

  it.each([
    ["invite-expired", "La invitación ha expirado. Pide una nueva."],
    ["invite-already-redeemed", "La invitación ya se ha utilizado."],
    ["not-found", "No se encontró lo que buscabas."],
  ])("explains the %s failure with its own Spanish message", async (code, message) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        jsonResponse({ title: "Problem", status: 409, code }, 409, "application/problem+json"),
      ),
    );
    renderRedemption("/invites/AB12CD");

    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.queryByRole("link", { name: "Abrir la familia" })).not.toBeInTheDocument();
  });

  it("offers a retry when the connection fails, then confirms the join", async () => {
    let fail = true;
    const fetchMock = vi.fn().mockImplementation(() => {
      if (fail) return Promise.reject(new TypeError("Failed to fetch"));
      return Promise.resolve(jsonResponse({ familyId: "f1", personaId: "p1", status: "ACCEPTED" }, 201));
    });
    vi.stubGlobal("fetch", fetchMock);
    renderRedemption("/invites/AB12CD");
    const user = userEvent.setup();

    expect(
      await screen.findByText("No se pudo conectar con el servidor. Inténtalo de nuevo."),
    ).toBeInTheDocument();

    fail = false;
    await user.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(await screen.findByText("Te has unido a la familia.")).toBeInTheDocument();
  });

  it("does not blame the network when the redemption response is malformed", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse({ unexpected: true }, 201)));
    renderRedemption("/invites/AB12CD");

    expect(
      await screen.findByRole("alert"),
    ).toHaveTextContent("La respuesta del servidor no es válida. Inténtalo de nuevo.");
  });

  it("explains a link that carries no code without asking the server", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    renderRedemption("/invites/%20");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "El enlace no incluye ningún código de invitación.",
    );
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
