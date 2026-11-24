import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import { clearToken, setToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { FamiliesPage } from "./FamiliesPage";

function renderFamilies() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  let currentPath = "";
  function LocationProbe() {
    currentPath = useLocation().pathname;
    return null;
  }
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/families"]}>
        <LocationProbe />
        <Routes>
          <Route path="/families" element={<FamiliesPage />} />
          <Route path="/families/:familyId" element={<p>pantalla de la familia</p>} />
          <Route path="/invites/:code" element={<p>pantalla de la invitación</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return { currentPath: () => currentPath };
}

beforeEach(() => {
  setToken("token-1");
});

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("FamiliesPage", () => {
  it("shows a loading state and then the families, naming legacy rows 'Sin nombre'", async () => {
    let resolveFetch: (response: Response) => void = () => {};
    const fetchMock = vi.fn().mockImplementation(
      () =>
        new Promise<Response>((resolve) => {
          resolveFetch = resolve;
        }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const { currentPath } = renderFamilies();

    expect(screen.getByText("Cargando familias…")).toBeInTheDocument();

    resolveFetch(
      jsonResponse({
        content: [
          { id: "f1", name: "Rojas", adminPersonaId: "p1" },
          { id: "f2", name: null, adminPersonaId: "p2" },
        ],
        totalElements: 2,
        totalPages: 1,
        number: 0,
        size: 20,
      }),
    );

    const rojas = await screen.findByRole("link", { name: "Rojas" });
    expect(rojas).toHaveAttribute("href", "/families/f1");
    expect(screen.getByRole("link", { name: "Sin nombre" })).toHaveAttribute(
      "href",
      "/families/f2",
    );
    expect(screen.queryByText("Cargando familias…")).not.toBeInTheDocument();

    await userEvent.setup().click(rojas);
    expect(currentPath()).toBe("/families/f1");
    expect(screen.getByText("pantalla de la familia")).toBeInTheDocument();
  });

  it("offers creating a family or redeeming an invite when there are none", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          jsonResponse({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }),
        ),
    );
    renderFamilies();

    expect(await screen.findByText("Aún no perteneces a ninguna familia.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Crear una familia" })).toBeInTheDocument();
    expect(screen.getByLabelText("Código de invitación")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Canjear invitación" })).toBeInTheDocument();
    expect(screen.getByRole("form", { name: "Canjear una invitación" })).toBeInTheDocument();
    expect(screen.getByRole("form", { name: "Nueva familia" })).toBeInTheDocument();
  });

  it("normalizes a pasted invite code and opens its redemption route", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          jsonResponse({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }),
        ),
    );
    const { currentPath } = renderFamilies();
    const user = userEvent.setup();

    await user.type(await screen.findByLabelText("Código de invitación"), " ab12cd ");
    await user.click(screen.getByRole("button", { name: "Canjear invitación" }));

    expect(currentPath()).toBe("/invites/ab12cd");
    expect(screen.getByText("pantalla de la invitación")).toBeInTheDocument();
  });

  it("pages the list at 20 with prev/next driven by the Page metadata", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) => {
      const number = Number(new URL(url).searchParams.get("page") ?? 0);
      return Promise.resolve(
        jsonResponse({
          content: [{ id: `f${number}`, name: `Familia ${number}`, adminPersonaId: "p1" }],
          totalElements: 45,
          totalPages: 3,
          number,
          size: 20,
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderFamilies();

    expect(await screen.findByText("Página 1 de 3")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Siguiente" })).toBeEnabled();

    await user.click(screen.getByRole("button", { name: "Siguiente" }));
    expect(await screen.findByText("Página 2 de 3")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Familia 1" })).toBeInTheDocument();
    expect(fetchMock.mock.calls[1][0]).toBe(
      "http://localhost:8070/api/v1/me/families?page=1&size=20",
    );

    await user.click(screen.getByRole("button", { name: "Siguiente" }));
    expect(await screen.findByText("Página 3 de 3")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Siguiente" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Anterior" }));
    expect(await screen.findByText("Página 2 de 3")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Anterior" })).toBeEnabled();
  });

  it("validates the new family's name natively, creates it and shows it in the list", async () => {
    let created = false;
    const fetchMock = vi.fn().mockImplementation((_url: string, init?: RequestInit) => {
      if (init?.method === "POST") {
        created = true;
        return Promise.resolve(
          jsonResponse({ id: "f3", name: "Nueva familia", adminPersonaId: "p1" }, 201),
        );
      }
      const content = [{ id: "f1", name: "Rojas", adminPersonaId: "p1" }];
      if (created) content.push({ id: "f3", name: "Nueva familia", adminPersonaId: "p1" });
      return Promise.resolve(
        jsonResponse({
          content,
          totalElements: content.length,
          totalPages: 1,
          number: 0,
          size: 20,
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderFamilies();

    const name = (await screen.findByRole("textbox", {
      name: "Nombre de la familia",
    })) as HTMLInputElement;
    expect(name).toBeRequired();
    expect(name).toHaveAttribute("maxlength", "100");

    await user.click(screen.getByRole("button", { name: "Crear familia" }));
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(name).toHaveAttribute("aria-invalid", "true");
    expect(name).toHaveAttribute("aria-describedby", "family-name-error");
    expect(screen.getByText("Escribe un nombre de hasta 100 caracteres.")).toBeInTheDocument();

    await user.type(name, "Nueva familia");
    await user.click(screen.getByRole("button", { name: "Crear familia" }));

    expect(await screen.findByRole("link", { name: "Nueva familia" })).toBeInTheDocument();
    expect(name).toHaveValue("");
    const postCall = fetchMock.mock.calls.find(([, init]) => init?.method === "POST");
    expect(postCall?.[0]).toBe("http://localhost:8070/api/v1/families");
    expect(postCall?.[1].body).toBe(JSON.stringify({ name: "Nueva familia" }));
    expect(postCall?.[1].headers.get("Authorization")).toBe("Bearer token-1");
  });

  it("marks the name field for a server validation failure and clears it on edit", async () => {
    let created = false;
    const fetchMock = vi.fn().mockImplementation((_url: string, init?: RequestInit) => {
      if (init?.method === "POST") {
        if (created) {
          return Promise.resolve(
            jsonResponse({ id: "f3", name: "Rojas", adminPersonaId: "p1" }, 201),
          );
        }
        return Promise.resolve(
          jsonResponse(
            {
              title: "Validation failed",
              status: 400,
              code: "validation-failed",
              errors: [{ field: "name", message: "must not be blank" }],
            },
            400,
            "application/problem+json",
          ),
        );
      }
      const content = created ? [{ id: "f3", name: "Rojas", adminPersonaId: "p1" }] : [];
      return Promise.resolve(
        jsonResponse({
          content,
          totalElements: content.length,
          totalPages: 1,
          number: 0,
          size: 20,
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderFamilies();

    const name = (await screen.findByLabelText("Nombre de la familia")) as HTMLInputElement;
    await user.type(name, "   ");
    await user.click(screen.getByRole("button", { name: "Crear familia" }));

    await waitFor(() => expect(name).toHaveAttribute("aria-invalid", "true"));
    expect(name).toHaveFocus();
    expect(name).toHaveAttribute("aria-describedby", "family-name-error");
    expect(screen.getByText("Escribe un nombre de hasta 100 caracteres.")).toBeInTheDocument();
    expect(screen.queryByText("Revisa los campos marcados.")).not.toBeInTheDocument();
    expect(screen.queryByText("must not be blank")).not.toBeInTheDocument();

    await user.clear(name);
    await user.type(name, "Rojas");
    await waitFor(() => expect(name).toHaveAttribute("aria-invalid", "false"));
    created = true;
    await user.click(screen.getByRole("button", { name: "Crear familia" }));

    expect(await screen.findByRole("link", { name: "Rojas" })).toBeInTheDocument();
  });

  it("explains a connection failure when creating a family", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockImplementation((_url: string, init?: RequestInit) =>
          init?.method === "POST"
            ? Promise.reject(new TypeError("Failed to fetch"))
            : Promise.resolve(
                jsonResponse({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }),
              ),
        ),
    );
    const user = userEvent.setup();
    renderFamilies();

    await user.type(await screen.findByLabelText("Nombre de la familia"), "Rojas");
    await user.click(screen.getByRole("button", { name: "Crear familia" }));

    expect(
      await screen.findByText("No se pudo conectar con el servidor. Inténtalo de nuevo."),
    ).toBeInTheDocument();
  });

  it("explains a malformed list response without blaming the network", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse({ unexpected: true })));
    renderFamilies();

    expect(
      await screen.findByText("La respuesta del servidor no es válida. Inténtalo de nuevo."),
    ).toBeInTheDocument();
  });

  it("bridges both forms with one polite live region and marks invite errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          jsonResponse({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }),
        ),
    );
    const { currentPath } = renderFamilies();
    const user = userEvent.setup();

    const code = await screen.findByLabelText("Código de invitación");
    await user.click(screen.getByRole("button", { name: "Canjear invitación" }));

    expect(code).toHaveAttribute("aria-invalid", "true");
    expect(code).toHaveAttribute("aria-describedby", "invite-code-error");
    expect(screen.getByText("Escribe el código de invitación.")).toBeInTheDocument();
    expect(currentPath()).toBe("/families");
    expect(document.querySelectorAll('[aria-live="polite"]')).toHaveLength(1);
  });

  it("explains a failed list request in Spanish and can retry it", async () => {
    let fail = true;
    const fetchMock = vi.fn().mockImplementation(() => {
      if (fail) {
        return Promise.resolve(
          jsonResponse(
            { title: "Forbidden", status: 403, code: "forbidden" },
            403,
            "application/problem+json",
          ),
        );
      }
      return Promise.resolve(
        jsonResponse({
          content: [{ id: "f1", name: "Rojas", adminPersonaId: "p1" }],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 20,
        }),
      );
    });
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderFamilies();

    expect(await screen.findByText("No tienes permiso para hacer eso.")).toBeInTheDocument();
    fail = false;
    await user.click(screen.getByRole("button", { name: "Reintentar" }));
    expect(await screen.findByRole("link", { name: "Rojas" })).toBeInTheDocument();
  });
});
