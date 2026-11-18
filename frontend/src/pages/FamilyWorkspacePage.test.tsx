import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router";
import { clearToken, setPersonaId, setToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { FamilyWorkspacePage } from "./FamilyWorkspacePage";

const familyUrl = "http://localhost:8080/api/v1/families/f1";

function renderWorkspace() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/families/f1"]}>
        <Routes>
          <Route path="/families/:familyId" element={<FamilyWorkspacePage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function memberPage(content: unknown[], overrides: Record<string, number> = {}) {
  return jsonResponse({
    content,
    totalElements: content.length,
    totalPages: 1,
    number: 0,
    size: 20,
    ...overrides,
  });
}

function familyResponse(overrides: Record<string, unknown> = {}) {
  return jsonResponse({ id: "f1", name: "Rojas", adminPersonaId: "p1", ...overrides });
}

/**
 * The workspace reads the Family (for its Admin) and its members; this routes
 * both so a single stubbed `fetch` answers each call with the right shape.
 */
function routed(members: (url: string, init?: RequestInit) => Response | Promise<Response>) {
  return (url: string, init?: RequestInit) =>
    Promise.resolve(String(url) === familyUrl ? familyResponse() : members(String(url), init));
}

const ana = {
  personaId: "p1",
  username: "ana",
  dni: "12345678Z",
  admin: true,
  status: "ACCEPTED",
  consentInForce: true,
};

function membersCall(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.find(([url]) => String(url).includes("/members?"));
}

let createObjectURL: ReturnType<typeof vi.fn>;
let revokeObjectURL: ReturnType<typeof vi.fn>;

beforeEach(() => {
  setToken("token-1");
  createObjectURL = vi
    .fn()
    .mockReturnValueOnce("blob:front")
    .mockReturnValueOnce("blob:back");
  revokeObjectURL = vi.fn();
  Object.defineProperty(URL, "createObjectURL", { configurable: true, value: createObjectURL });
  Object.defineProperty(URL, "revokeObjectURL", { configurable: true, value: revokeObjectURL });
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  Reflect.deleteProperty(URL, "createObjectURL");
  Reflect.deleteProperty(URL, "revokeObjectURL");
  Reflect.deleteProperty(navigator, "clipboard");
  clearToken();
});

describe("FamilyWorkspacePage", () => {
  it("lists members with their admin badge and masks those without Consent", async () => {
    const fetchMock = vi.fn().mockImplementation(
      routed(() =>
        memberPage([
          ana,
          {
            personaId: "p2",
            username: "beto",
            dni: null,
            admin: false,
            status: "ACCEPTED",
            consentInForce: false,
          },
        ]),
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();

    const anaRow = (await screen.findByRole("rowheader", { name: /ana/ })).closest("tr");
    expect(anaRow).not.toBeNull();
    expect(within(anaRow!).getByText("Admin")).toBeInTheDocument();
    expect(within(anaRow!).getByText("12345678Z")).toBeInTheDocument();
    expect(within(anaRow!).getByRole("button", { name: "Ver DNI de ana" })).toBeInTheDocument();

    const betoRow = screen.getByRole("rowheader", { name: /beto/ }).closest("tr");
    expect(betoRow).not.toBeNull();
    expect(within(betoRow!).getByText("Sin consentimiento")).toBeInTheDocument();
    expect(within(betoRow!).queryByRole("button")).not.toBeInTheDocument();

    const [url, init] = membersCall(fetchMock)!;
    expect(url).toBe("http://localhost:8080/api/v1/families/f1/members?page=0&size=20");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });

  it("shows the DNI number and both photos fetched from their relative URLs with the bearer token", async () => {
    const frontPhotoUrl = "/api/v1/families/f1/members/p1/dni/front";
    const backPhotoUrl = "/api/v1/families/f1/members/p1/dni/back";
    const photoResponse = () =>
      Promise.resolve(new Response(new Blob(["photo"], { type: "image/png" }), { status: 200 }));
    const fetchMock = vi.fn().mockImplementation(
      routed((url) => {
        if (url.endsWith("/dni/front") || url.endsWith("/dni/back")) {
          return photoResponse();
        }
        if (url.endsWith("/dni")) {
          return jsonResponse({
            personaId: "p1",
            number: "12345678Z",
            frontPhotoUrl,
            backPhotoUrl,
          });
        }
        return memberPage([ana]);
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Ver DNI de ana" }));

    const dniView = await screen.findByRole("region", { name: "DNI de ana" });
    expect(within(dniView).getByRole("heading", { name: "DNI de ana" })).toBeInTheDocument();
    expect(within(dniView).getByText("12345678Z")).toBeInTheDocument();
    expect(
      await within(dniView).findByRole("img", { name: "Foto delantera del DNI de ana" }),
    ).toHaveAttribute("src", "blob:front");
    expect(
      await within(dniView).findByRole("img", { name: "Foto trasera del DNI de ana" }),
    ).toHaveAttribute("src", "blob:back");
    const frontCall = fetchMock.mock.calls.find(([url]) => String(url).endsWith("/dni/front"));
    const backCall = fetchMock.mock.calls.find(([url]) => String(url).endsWith("/dni/back"));
    expect(frontCall?.[0]).toBe("http://localhost:8080/api/v1/families/f1/members/p1/dni/front");
    expect(backCall?.[0]).toBe("http://localhost:8080/api/v1/families/f1/members/p1/dni/back");
    expect(frontCall?.[1].headers.get("Authorization")).toBe("Bearer token-1");
    expect(backCall?.[1].headers.get("Authorization")).toBe("Bearer token-1");
    expect(createObjectURL).toHaveBeenCalledTimes(2);
  });

  it.each([
    [403, "forbidden", "Esta persona no tiene el consentimiento vigente en esta familia."],
    [404, "not-found", "Esta persona ya no es miembro de la familia."],
  ])("explains a %i on the DNI view without confusing the two causes", async (status, code, message) => {
    const fetchMock = vi.fn().mockImplementation(
      routed((url) => {
        if (url.endsWith("/dni")) {
          return jsonResponse({ title: "Problem", status, code }, status, "application/problem+json");
        }
        return memberPage([ana]);
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();

    await userEvent.setup().click(await screen.findByRole("button", { name: "Ver DNI de ana" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it("pages the member list at 20 using the Page metadata", async () => {
    const fetchMock = vi.fn().mockImplementation(
      routed((url) => {
        const number = Number(new URL(url).searchParams.get("page") ?? 0);
        return memberPage(
          [
            {
              personaId: `p${number}`,
              username: `miembro${number}`,
              dni: null,
              admin: false,
              status: "ACCEPTED",
              consentInForce: false,
            },
          ],
          { number, totalElements: 35, totalPages: 2 },
        );
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    expect(await screen.findByText("Página 1 de 2")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "Siguiente" }));

    expect(await screen.findByText("Página 2 de 2")).toBeInTheDocument();
    expect(screen.getByRole("rowheader", { name: /miembro1/ })).toBeInTheDocument();
    const secondPageCall = fetchMock.mock.calls.find(([url]) => String(url).includes("page=1"));
    expect(secondPageCall?.[0]).toBe(
      "http://localhost:8080/api/v1/families/f1/members?page=1&size=20",
    );
    expect(screen.getByRole("button", { name: "Siguiente" })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "Anterior" }));
    expect(await screen.findByText("Página 1 de 2")).toBeInTheDocument();
  });
});

describe("FamilyWorkspacePage invite issuance", () => {
  it("lets the Admin issue an Invite and shows the code with its expiry in local time", async () => {
    setPersonaId("p1");
    const expiresAt = "2026-11-25T10:30:00Z";
    const fetchMock = vi.fn().mockImplementation(
      routed((_url, init) => {
        if (init?.method === "POST") {
          return jsonResponse({ code: "AB12CD", familyId: "f1", expiresAt }, 201);
        }
        return memberPage([ana]);
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Emitir invitación" }));

    expect(await screen.findByText("AB12CD")).toBeInTheDocument();
    const expiry = document.querySelector("time");
    expect(expiry).not.toBeNull();
    expect(expiry).toHaveAttribute("datetime", expiresAt);
    expect(expiry!.textContent).not.toBe("");
    expect(expiry!.textContent).not.toBe(expiresAt);

    const postCall = fetchMock.mock.calls.find(([, init]) => init?.method === "POST");
    expect(postCall?.[0]).toBe("http://localhost:8080/api/v1/families/f1/invites");
    expect(postCall?.[1].headers.get("Authorization")).toBe("Bearer token-1");
  });

  it("copies the code with one tap and shows feedback", async () => {
    setPersonaId("p1");
    const fetchMock = vi.fn().mockImplementation(
      routed((_url, init) =>
        init?.method === "POST"
          ? jsonResponse({ code: "AB12CD", familyId: "f1", expiresAt: "2026-11-25T10:30:00Z" }, 201)
          : memberPage([ana]),
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Emitir invitación" }));
    await user.click(await screen.findByRole("button", { name: "Copiar código" }));

    await expect(navigator.clipboard.readText()).resolves.toBe("AB12CD");
    expect(await screen.findByText("Código copiado.")).toBeInTheDocument();
  });

  it("tells the Admin to copy by hand when the browser refuses the clipboard", async () => {
    setPersonaId("p1");
    const fetchMock = vi.fn().mockImplementation(
      routed((_url, init) =>
        init?.method === "POST"
          ? jsonResponse({ code: "AB12CD", familyId: "f1", expiresAt: "2026-11-25T10:30:00Z" }, 201)
          : memberPage([ana]),
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Emitir invitación" }));
    vi.spyOn(navigator.clipboard, "writeText").mockRejectedValue(new Error("denied"));
    await user.click(await screen.findByRole("button", { name: "Copiar código" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No se pudo copiar el código. Cópialo a mano.",
    );
  });

  it("hides the action from members who are not the Family's Admin", async () => {
    setPersonaId("p2");
    vi.stubGlobal("fetch", vi.fn().mockImplementation(routed(() => memberPage([ana]))));
    renderWorkspace();

    await screen.findByRole("rowheader", { name: /ana/ });
    expect(screen.queryByRole("button", { name: "Emitir invitación" })).not.toBeInTheDocument();
  });

  it("explains a forbidden response when a non-admin races the check", async () => {
    setPersonaId("p1");
    const fetchMock = vi.fn().mockImplementation(
      routed((_url, init) =>
        init?.method === "POST"
          ? jsonResponse({ title: "Forbidden", status: 403, code: "forbidden" }, 403, "application/problem+json")
          : memberPage([ana]),
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Emitir invitación" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("No tienes permiso para hacer eso.");
  });
});
