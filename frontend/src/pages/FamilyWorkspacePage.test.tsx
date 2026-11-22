import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router";
import { clearToken, setPersonaId, setToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { FamilyWorkspacePage } from "./FamilyWorkspacePage";

const familyUrl = "http://localhost:8070/api/v1/families/f1";

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
    .mockReturnValueOnce("blob:back")
    .mockReturnValue("blob:photo");
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
    expect(url).toBe("http://localhost:8070/api/v1/families/f1/members?page=0&size=20");
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
    expect(frontCall?.[0]).toBe("http://localhost:8070/api/v1/families/f1/members/p1/dni/front");
    expect(backCall?.[0]).toBe("http://localhost:8070/api/v1/families/f1/members/p1/dni/back");
    expect(frontCall?.[1].headers.get("Authorization")).toBe("Bearer token-1");
    expect(backCall?.[1].headers.get("Authorization")).toBe("Bearer token-1");
    expect(createObjectURL).toHaveBeenCalledTimes(2);
  });

  it.each([
    [403, "forbidden", "Esta persona no tiene el consentimiento vigente en esta familia."],
    [404, "not-found", "Esta persona ya no es miembro de la familia."],
  ])(
    "explains a %i on the DNI view without confusing the two causes",
    async (status, code, message) => {
      const fetchMock = vi.fn().mockImplementation(
        routed((url) => {
          if (url.endsWith("/dni")) {
            return jsonResponse(
              { title: "Problem", status, code },
              status,
              "application/problem+json",
            );
          }
          return memberPage([ana]);
        }),
      );
      vi.stubGlobal("fetch", fetchMock);
      renderWorkspace();

      await userEvent.setup().click(await screen.findByRole("button", { name: "Ver DNI de ana" }));

      expect(await screen.findByRole("alert")).toHaveTextContent(message);
      expect(screen.queryByRole("img")).not.toBeInTheDocument();
    },
  );

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
      "http://localhost:8070/api/v1/families/f1/members?page=1&size=20",
    );
    expect(screen.getByRole("button", { name: "Siguiente" })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "Anterior" }));
    expect(await screen.findByText("Página 1 de 2")).toBeInTheDocument();
  });

  it("shows an empty state when the family has no members", async () => {
    setPersonaId("p1");
    vi.stubGlobal("fetch", vi.fn().mockImplementation(routed(() => memberPage([]))));
    renderWorkspace();

    expect(await screen.findByText("Esta familia todavía no tiene miembros.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("does not call a family empty when a removal just emptied a later page", async () => {
    setPersonaId("p1");
    const fetchMock = vi.fn().mockImplementation(
      routed((url) => {
        const number = Number(new URL(url).searchParams.get("page") ?? 0);
        // Page 1 is out of range after the last removal shrank the total, so the
        // backend answers it with an empty content array and a smaller totalPages.
        if (number > 0) return memberPage([], { number, totalElements: 20, totalPages: 1 });
        return memberPage([ana], { number, totalElements: 21, totalPages: 2 });
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await screen.findByText("Página 1 de 2");
    await user.click(screen.getByRole("button", { name: "Siguiente" }));

    const requestedPages = () =>
      fetchMock.mock.calls
        .filter(([url]) => String(url).includes("/members?"))
        .map(([url]) => new URL(String(url)).searchParams.get("page"));
    await waitFor(() => expect(requestedPages()).toEqual(["0", "1", "0"]));
    expect(screen.queryByText("Esta familia todavía no tiene miembros.")).not.toBeInTheDocument();
    expect(await screen.findByRole("rowheader", { name: /ana/ })).toBeInTheDocument();
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
    expect(postCall?.[0]).toBe("http://localhost:8070/api/v1/families/f1/invites");
    expect(postCall?.[1].headers.get("Authorization")).toBe("Bearer token-1");
  });

  it("copies the code with one tap and shows feedback", async () => {
    setPersonaId("p1");
    const fetchMock = vi
      .fn()
      .mockImplementation(
        routed((_url, init) =>
          init?.method === "POST"
            ? jsonResponse(
                { code: "AB12CD", familyId: "f1", expiresAt: "2026-11-25T10:30:00Z" },
                201,
              )
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
    const fetchMock = vi
      .fn()
      .mockImplementation(
        routed((_url, init) =>
          init?.method === "POST"
            ? jsonResponse(
                { code: "AB12CD", familyId: "f1", expiresAt: "2026-11-25T10:30:00Z" },
                201,
              )
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
    const fetchMock = vi
      .fn()
      .mockImplementation(
        routed((_url, init) =>
          init?.method === "POST"
            ? jsonResponse(
                { title: "Forbidden", status: 403, code: "forbidden" },
                403,
                "application/problem+json",
              )
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

type MemberRecord = {
  personaId: string;
  username: string;
  dni: string | null;
  admin: boolean;
  status: string;
  consentInForce: boolean;
};

function memberRecord(
  overrides: Partial<MemberRecord> & { personaId: string; username: string },
): MemberRecord {
  return {
    dni: null,
    admin: false,
    status: "ACCEPTED",
    consentInForce: false,
    ...overrides,
  };
}

function rowFor(username: RegExp) {
  const row = screen.getByRole("rowheader", { name: username }).closest("tr");
  if (!row) throw new Error(`No row for ${username}`);
  return within(row);
}

async function findRow(username: RegExp) {
  const row = (await screen.findByRole("rowheader", { name: username })).closest("tr");
  if (!row) throw new Error(`No row for ${username}`);
  return within(row);
}

/**
 * A stateful workspace stub: GETs report the current members, and a DELETE
 * either applies the removal to that state (the happy path) or answers with a
 * problem the screen must explain.
 */
function statefulWorkspace(
  members: MemberRecord[],
  deleteResponse?: () => Response,
): ReturnType<typeof vi.fn> {
  return vi.fn().mockImplementation((url: string, init?: RequestInit) => {
    const href = String(url);
    if (href === familyUrl) return Promise.resolve(familyResponse());
    if (init?.method === "DELETE") {
      if (deleteResponse) return Promise.resolve(deleteResponse());
      const isConsent = href.endsWith("/consent");
      const personaId = href.split("/members/")[1].replace("/consent", "");
      const target = members.find((member) => member.personaId === personaId);
      if (target) {
        if (isConsent) {
          target.consentInForce = false;
          target.dni = null;
        } else {
          members.splice(members.indexOf(target), 1);
        }
      }
      return Promise.resolve(new Response(null, { status: 204 }));
    }
    return Promise.resolve(memberPage(members));
  });
}

function deleteCall(fetchMock: ReturnType<typeof vi.fn>, suffix: string) {
  return fetchMock.mock.calls.find(
    ([url, init]) => String(url).endsWith(suffix) && init?.method === "DELETE",
  );
}

describe("FamilyWorkspacePage consent revocation", () => {
  it("revokes the Persona's own Consent only after confirming, then masks the row", async () => {
    setPersonaId("p1");
    const members = [
      memberRecord({
        personaId: "p1",
        username: "ana",
        dni: "12345678Z",
        admin: true,
        consentInForce: true,
      }),
      memberRecord({ personaId: "p2", username: "beto" }),
    ];
    const fetchMock = statefulWorkspace(members);
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    const anaRow = await findRow(/ana/);
    expect(anaRow.getByText("12345678Z")).toBeInTheDocument();
    expect(
      rowFor(/beto/).queryByRole("button", { name: "Revocar consentimiento" }),
    ).not.toBeInTheDocument();

    await user.click(anaRow.getByRole("button", { name: "Revocar consentimiento" }));

    expect(deleteCall(fetchMock, "/consent")).toBeUndefined();
    const dialog = await screen.findByRole("dialog");
    expect(
      within(dialog).getByRole("heading", { name: "¿Revocar tu consentimiento?" }),
    ).toBeInTheDocument();

    await user.click(within(dialog).getByRole("button", { name: "Confirmar revocación" }));

    await waitFor(() => {
      const call = deleteCall(fetchMock, "/members/p1/consent");
      expect(call?.[0]).toBe("http://localhost:8070/api/v1/families/f1/members/p1/consent");
      expect(call?.[1].headers.get("Authorization")).toBe("Bearer token-1");
    });
    expect(await rowFor(/ana/).findByText("Sin consentimiento")).toBeInTheDocument();
    expect(rowFor(/ana/).queryByText("12345678Z")).not.toBeInTheDocument();
  });

  it("leaves the Consent untouched when the Persona cancels the confirmation", async () => {
    setPersonaId("p1");
    const members = [
      memberRecord({
        personaId: "p1",
        username: "ana",
        dni: "12345678Z",
        admin: true,
        consentInForce: true,
      }),
    ];
    const fetchMock = statefulWorkspace(members);
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click(await screen.findByRole("button", { name: "Revocar consentimiento" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Cancelar" }));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(deleteCall(fetchMock, "/consent")).toBeUndefined();
    expect(rowFor(/ana/).getByText("12345678Z")).toBeInTheDocument();
  });
});

describe("FamilyWorkspacePage member removal", () => {
  const members = () => [
    memberRecord({
      personaId: "p1",
      username: "ana",
      dni: "12345678Z",
      admin: true,
      consentInForce: true,
    }),
    memberRecord({ personaId: "p3", username: "carla", consentInForce: true }),
  ];

  it("removes a member only after confirming, then drops the row", async () => {
    setPersonaId("p1");
    const fetchMock = statefulWorkspace(members());
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click((await findRow(/carla/)).getByRole("button", { name: "Retirar a carla" }));

    expect(deleteCall(fetchMock, "/members/p3")).toBeUndefined();
    const dialog = await screen.findByRole("dialog");
    expect(within(dialog).getByRole("heading", { name: "¿Retirar a carla?" })).toBeInTheDocument();

    await user.click(within(dialog).getByRole("button", { name: "Confirmar retirada" }));

    await waitFor(() => {
      expect(deleteCall(fetchMock, "/members/p3")?.[0]).toBe(
        "http://localhost:8070/api/v1/families/f1/members/p3",
      );
    });
    await waitFor(() =>
      expect(screen.queryByRole("rowheader", { name: /carla/ })).not.toBeInTheDocument(),
    );
  });

  it("offers the Admin no way to remove its own row", async () => {
    setPersonaId("p1");
    vi.stubGlobal("fetch", statefulWorkspace(members()));
    renderWorkspace();

    await screen.findByRole("rowheader", { name: /ana/ });
    expect(rowFor(/ana/).queryByRole("button", { name: /Retirar/ })).not.toBeInTheDocument();
    expect(rowFor(/carla/).getByRole("button", { name: "Retirar a carla" })).toBeInTheDocument();
  });

  it("explains a cannot-remove-admin refusal in its own words", async () => {
    setPersonaId("p1");
    const fetchMock = statefulWorkspace(members(), () =>
      jsonResponse(
        { title: "Conflict", status: 409, code: "cannot-remove-admin" },
        409,
        "application/problem+json",
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click((await findRow(/carla/)).getByRole("button", { name: "Retirar a carla" }));
    await user.click(
      within(await screen.findByRole("dialog")).getByRole("button", { name: "Confirmar retirada" }),
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "El administrador de la familia no puede ser retirado.",
    );
  });

  it("explains a forbidden refusal with a different message", async () => {
    setPersonaId("p1");
    const fetchMock = statefulWorkspace(members(), () =>
      jsonResponse(
        { title: "Forbidden", status: 403, code: "forbidden" },
        403,
        "application/problem+json",
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click((await findRow(/carla/)).getByRole("button", { name: "Retirar a carla" }));
    await user.click(
      within(await screen.findByRole("dialog")).getByRole("button", { name: "Confirmar retirada" }),
    );

    expect(await screen.findByRole("alert")).toHaveTextContent("No tienes permiso para hacer eso.");
  });

  it("clears a refusal message once a later action succeeds", async () => {
    setPersonaId("p1");
    const records = members();
    let deletes = 0;
    const fetchMock = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      const href = String(url);
      if (href === familyUrl) return Promise.resolve(familyResponse());
      if (init?.method === "DELETE") {
        deletes += 1;
        if (deletes === 1) {
          return Promise.resolve(
            jsonResponse(
              { title: "Conflict", status: 409, code: "cannot-remove-admin" },
              409,
              "application/problem+json",
            ),
          );
        }
        const personaId = href.split("/members/")[1].replace("/consent", "");
        const target = records.find((member) => member.personaId === personaId);
        if (target) {
          target.consentInForce = false;
          target.dni = null;
        }
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      return Promise.resolve(memberPage(records));
    });
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    await user.click((await findRow(/carla/)).getByRole("button", { name: "Retirar a carla" }));
    await user.click(
      within(await screen.findByRole("dialog")).getByRole("button", { name: "Confirmar retirada" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "El administrador de la familia no puede ser retirado.",
    );

    await user.click(rowFor(/ana/).getByRole("button", { name: "Revocar consentimiento" }));
    await user.click(
      within(await screen.findByRole("dialog")).getByRole("button", {
        name: "Confirmar revocación",
      }),
    );

    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
  });
});

describe("FamilyWorkspacePage DNI photos", () => {
  function workspaceWithDni(dni: Record<string, unknown>) {
    return vi.fn().mockImplementation(
      routed((url) => {
        if (url.endsWith("/dni/front") || url.endsWith("/dni/back")) {
          return new Response(new Blob(["photo"], { type: "image/png" }), { status: 200 });
        }
        if (url.endsWith("/dni")) {
          return jsonResponse({ personaId: "p1", ...dni });
        }
        return memberPage([ana]);
      }),
    );
  }

  function photoCalls(fetchMock: ReturnType<typeof vi.fn>) {
    return fetchMock.mock.calls.filter(([url]) => /\/dni\/(front|back)$/.test(String(url)));
  }

  async function openDni() {
    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Ver DNI de ana" }));
    return user;
  }

  it("enlarges a photo in a modal and closes it with the cross", async () => {
    const fetchMock = workspaceWithDni({
      number: "12345678Z",
      frontPhotoUrl: "/api/v1/families/f1/members/p1/dni/front",
      backPhotoUrl: "/api/v1/families/f1/members/p1/dni/back",
    });
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = await openDni();

    await user.click(await screen.findByRole("img", { name: "Foto delantera del DNI de ana" }));

    const dialog = await screen.findByRole("dialog");
    expect(
      within(dialog).getByRole("img", { name: "Foto delantera del DNI de ana" }),
    ).toBeInTheDocument();
    expect(dialog).toHaveAttribute("closedby", "any");

    await user.click(within(dialog).getByRole("button", { name: "Cerrar" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("closes the enlarged photo when the area outside it is tapped", async () => {
    const fetchMock = workspaceWithDni({
      number: "12345678Z",
      frontPhotoUrl: "/api/v1/families/f1/members/p1/dni/front",
      backPhotoUrl: null,
    });
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = await openDni();

    await user.click(await screen.findByRole("img", { name: "Foto delantera del DNI de ana" }));
    const dialog = await screen.findByRole("dialog");
    fireEvent.click(dialog);

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("closes the enlarged photo on Escape", async () => {
    const fetchMock = workspaceWithDni({
      number: "12345678Z",
      frontPhotoUrl: "/api/v1/families/f1/members/p1/dni/front",
      backPhotoUrl: null,
    });
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = await openDni();

    await user.click(await screen.findByRole("img", { name: "Foto delantera del DNI de ana" }));
    const dialog = await screen.findByRole("dialog");
    fireEvent(dialog, new Event("cancel"));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });

  it("shows the number and 'Sin fotos del DNI' without requesting any photo", async () => {
    const fetchMock = workspaceWithDni({
      number: "12345678Z",
      frontPhotoUrl: null,
      backPhotoUrl: null,
    });
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    await openDni();

    const dniView = await screen.findByRole("region", { name: "DNI de ana" });
    expect(within(dniView).getByText("Sin fotos del DNI")).toBeInTheDocument();
    expect(within(dniView).queryByRole("img")).not.toBeInTheDocument();
    expect(within(dniView).queryByRole("button", { name: /Ampliar/ })).not.toBeInTheDocument();
    expect(photoCalls(fetchMock)).toHaveLength(0);
  });

  it("shows and enlarges only the side that has a photo", async () => {
    const fetchMock = workspaceWithDni({
      number: "12345678Z",
      frontPhotoUrl: "/api/v1/families/f1/members/p1/dni/front",
      backPhotoUrl: null,
    });
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    await openDni();

    expect(
      await screen.findByRole("img", { name: "Foto delantera del DNI de ana" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("img", { name: "Foto trasera del DNI de ana" }),
    ).not.toBeInTheDocument();
    expect(photoCalls(fetchMock)).toHaveLength(1);
    expect(String(photoCalls(fetchMock)[0][0])).toContain("/dni/front");
  });
});
