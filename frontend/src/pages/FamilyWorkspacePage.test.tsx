import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router";
import { clearToken, setToken } from "../auth/token";
import { jsonResponse } from "../test/http";
import { FamilyWorkspacePage } from "./FamilyWorkspacePage";

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
  Reflect.deleteProperty(URL, "createObjectURL");
  Reflect.deleteProperty(URL, "revokeObjectURL");
  clearToken();
});

describe("FamilyWorkspacePage", () => {
  it("lists members with their admin badge and masks those without Consent", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      memberPage([
        {
          personaId: "p1",
          username: "ana",
          dni: "12345678Z",
          admin: true,
          status: "ACCEPTED",
          consentInForce: true,
        },
        {
          personaId: "p2",
          username: "beto",
          dni: null,
          admin: false,
          status: "ACCEPTED",
          consentInForce: false,
        },
      ]),
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

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/families/f1/members?page=0&size=20");
    expect(init.headers.get("Authorization")).toBe("Bearer token-1");
  });

  it("shows the DNI number and both photos fetched from their relative URLs with the bearer token", async () => {
    const frontPhotoUrl = "/api/v1/families/f1/members/p1/dni/front";
    const backPhotoUrl = "/api/v1/families/f1/members/p1/dni/back";
    const photoResponse = () =>
      Promise.resolve(new Response(new Blob(["photo"], { type: "image/png" }), { status: 200 }));
    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith("/dni/front") || url.endsWith("/dni/back")) {
        return photoResponse();
      }
      if (url.endsWith("/dni")) {
        return Promise.resolve(
          jsonResponse({
            personaId: "p1",
            number: "12345678Z",
            frontPhotoUrl,
            backPhotoUrl,
          }),
        );
      }
      return Promise.resolve(
        memberPage([
          {
            personaId: "p1",
            username: "ana",
            dni: "12345678Z",
            admin: true,
            status: "ACCEPTED",
            consentInForce: true,
          },
        ]),
      );
    });
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
    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith("/dni")) {
        return Promise.resolve(
          jsonResponse(
            { title: "Problem", status, code },
            status,
            "application/problem+json",
          ),
        );
      }
      return Promise.resolve(
        memberPage([
          {
            personaId: "p1",
            username: "ana",
            dni: "12345678Z",
            admin: true,
            status: "ACCEPTED",
            consentInForce: true,
          },
        ]),
      );
    });
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();

    await userEvent.setup().click(await screen.findByRole("button", { name: "Ver DNI de ana" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it("pages the member list at 20 using the Page metadata", async () => {
    const fetchMock = vi.fn().mockImplementation((url: string) => {
      const number = Number(new URL(url).searchParams.get("page") ?? 0);
      return Promise.resolve(
        memberPage(
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
        ),
      );
    });
    vi.stubGlobal("fetch", fetchMock);
    renderWorkspace();
    const user = userEvent.setup();

    expect(await screen.findByText("Página 1 de 2")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "Siguiente" }));

    expect(await screen.findByText("Página 2 de 2")).toBeInTheDocument();
    expect(screen.getByRole("rowheader", { name: /miembro1/ })).toBeInTheDocument();
    expect(fetchMock.mock.calls[1][0]).toBe(
      "http://localhost:8080/api/v1/families/f1/members?page=1&size=20",
    );
    expect(screen.getByRole("button", { name: "Siguiente" })).toBeDisabled();
    await user.click(screen.getByRole("button", { name: "Anterior" }));
    expect(await screen.findByText("Página 1 de 2")).toBeInTheDocument();
  });
});
