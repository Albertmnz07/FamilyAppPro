import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearToken, setToken } from "../auth/token";
import { cameraUnavailableMessage } from "../camera/CameraCapture";
import { jsonResponse } from "../test/http";
import { ProfilePage, profileSavedNotice } from "./ProfilePage";

const maxPhotoBytes = 10 * 1024 * 1024;

const profileBody = (overrides: Record<string, unknown> = {}) => ({
  id: "p1",
  username: "ana",
  dni: "12345678Z",
  frontPhotoUrl: null,
  backPhotoUrl: null,
  ...overrides,
});

function renderProfile() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <ProfilePage />
    </QueryClientProvider>,
  );
}

/** Routes the profile GET and the photo PUT so refetches after a mutation still answer. */
function profileFetch(putResponse?: () => Response) {
  return vi.fn().mockImplementation((_url: string, init?: RequestInit) => {
    if (init?.method === "PUT") {
      return Promise.resolve(
        putResponse
          ? putResponse()
          : jsonResponse(profileBody({ frontPhotoUrl: "/api/v1/personas/me/photos/front" })),
      );
    }
    return Promise.resolve(jsonResponse(profileBody()));
  });
}

function stubCamera(getUserMedia: ReturnType<typeof vi.fn>) {
  Object.defineProperty(navigator, "mediaDevices", { configurable: true, value: { getUserMedia } });
  vi.spyOn(window.HTMLMediaElement.prototype, "play").mockResolvedValue(undefined);
  HTMLCanvasElement.prototype.getContext = vi.fn(() => ({ drawImage: vi.fn() })) as unknown as typeof HTMLCanvasElement.prototype.getContext;
  HTMLCanvasElement.prototype.toBlob = vi.fn((callback: BlobCallback) =>
    callback(new Blob(["frame"], { type: "image/jpeg" })),
  ) as unknown as typeof HTMLCanvasElement.prototype.toBlob;
}

function workingCamera() {
  const stream = { getTracks: () => [{ stop: vi.fn() }] } as unknown as MediaStream;
  const getUserMedia = vi.fn().mockResolvedValue(stream);
  stubCamera(getUserMedia);
  return getUserMedia;
}

function putCall(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.find(([, init]) => init?.method === "PUT");
}

beforeEach(() => {
  setToken("token-1");
  Object.defineProperty(URL, "createObjectURL", { configurable: true, value: vi.fn(() => "blob:capture") });
  Object.defineProperty(URL, "revokeObjectURL", { configurable: true, value: vi.fn() });
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  Reflect.deleteProperty(URL, "createObjectURL");
  Reflect.deleteProperty(URL, "revokeObjectURL");
  Reflect.deleteProperty(navigator, "mediaDevices");
  Reflect.deleteProperty(window, "isSecureContext");
  clearToken();
});

describe("ProfilePage", () => {
  it("shows Username and DNI number as read-only text with no edit or logout", async () => {
    vi.stubGlobal("fetch", profileFetch());
    renderProfile();

    expect(await screen.findByText("ana")).toBeInTheDocument();
    expect(screen.getByText("12345678Z")).toBeInTheDocument();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /cerrar sesión/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /guardar cambios/i })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Guardar fotos" })).toBeDisabled();
  });

  it("uploads the chosen front photo through the multipart PUT with the bearer token", async () => {
    const fetchMock = profileFetch();
    vi.stubGlobal("fetch", fetchMock);
    renderProfile();
    const user = userEvent.setup();
    const front = new File([new Uint8Array(16)], "front.jpg", { type: "image/jpeg" });

    await user.upload(await screen.findByLabelText("Foto delantera del DNI"), front);
    await user.click(screen.getByRole("button", { name: "Guardar fotos" }));

    expect(await screen.findByText(profileSavedNotice)).toBeInTheDocument();
    const call = putCall(fetchMock);
    expect(call?.[0]).toBe("http://localhost:8080/api/v1/personas/me/dni/photos");
    expect(call?.[1].headers.get("Authorization")).toBe("Bearer token-1");
    const form = call?.[1].body as FormData;
    expect(form.get("frontPhoto")).toBe(front);
    expect(form.get("backPhoto")).toBeNull();
  });

  it("uploads only the back photo when that is the only side supplied", async () => {
    const fetchMock = profileFetch();
    vi.stubGlobal("fetch", fetchMock);
    renderProfile();
    const user = userEvent.setup();
    const back = new File([new Uint8Array(16)], "back.png", { type: "image/png" });

    await user.upload(await screen.findByLabelText("Foto trasera del DNI"), back);
    await user.click(screen.getByRole("button", { name: "Guardar fotos" }));

    await screen.findByText(profileSavedNotice);
    const form = putCall(fetchMock)?.[1].body as FormData;
    expect(form.get("frontPhoto")).toBeNull();
    expect(form.get("backPhoto")).toBe(back);
  });

  it("captures a photo with the camera, accepts it and uploads that frame", async () => {
    workingCamera();
    const fetchMock = profileFetch();
    vi.stubGlobal("fetch", fetchMock);
    renderProfile();
    const user = userEvent.setup();
    await screen.findByText("ana");

    await user.click(screen.getAllByRole("button", { name: "Usar cámara" })[0]);
    const capturar = await screen.findByRole("button", { name: "Capturar" });
    await waitFor(() => expect(capturar).toBeEnabled());
    await user.click(capturar);
    await user.click(await screen.findByRole("button", { name: "Aceptar" }));

    await screen.findByText(profileSavedNotice);
    const form = putCall(fetchMock)?.[1].body as FormData;
    expect(form.get("frontPhoto")).toBeInstanceOf(File);
    expect(form.get("backPhoto")).toBeNull();
  });

  it("reopens the camera when the Persona repeats the shot", async () => {
    const getUserMedia = workingCamera();
    vi.stubGlobal("fetch", profileFetch());
    renderProfile();
    const user = userEvent.setup();
    await screen.findByText("ana");

    await user.click(screen.getAllByRole("button", { name: "Usar cámara" })[0]);
    const capturar = await screen.findByRole("button", { name: "Capturar" });
    await waitFor(() => expect(capturar).toBeEnabled());
    await user.click(capturar);
    await user.click(await screen.findByRole("button", { name: "Repetir" }));

    await waitFor(() => expect(getUserMedia).toHaveBeenCalledTimes(2));
  });

  it("falls back to the file picker when the camera is refused", async () => {
    stubCamera(vi.fn().mockRejectedValue(new Error("denied")));
    vi.stubGlobal("fetch", profileFetch());
    renderProfile();
    const user = userEvent.setup();
    await screen.findByText("ana");

    await user.click(screen.getAllByRole("button", { name: "Usar cámara" })[0]);

    expect(await screen.findByRole("alert")).toHaveTextContent(cameraUnavailableMessage);
    expect(screen.getByLabelText("Foto delantera del DNI")).toBeInTheDocument();
  });

  it("falls back to the file picker when the camera is missing", async () => {
    vi.stubGlobal("fetch", profileFetch());
    renderProfile();
    const user = userEvent.setup();
    await screen.findByText("ana");

    await user.click(screen.getAllByRole("button", { name: "Usar cámara" })[0]);

    expect(await screen.findByRole("alert")).toHaveTextContent(cameraUnavailableMessage);
    expect(screen.getByLabelText("Foto delantera del DNI")).toBeInTheDocument();
  });

  it("falls back to the file picker in an insecure context", async () => {
    Object.defineProperty(window, "isSecureContext", { configurable: true, value: false });
    workingCamera();
    vi.stubGlobal("fetch", profileFetch());
    renderProfile();
    const user = userEvent.setup();
    await screen.findByText("ana");

    await user.click(screen.getAllByRole("button", { name: "Usar cámara" })[0]);

    expect(await screen.findByRole("alert")).toHaveTextContent(cameraUnavailableMessage);
    expect(screen.getByLabelText("Foto delantera del DNI")).toBeInTheDocument();
  });

  it("releases the camera stream when the capture is cancelled before permission is granted", async () => {
    const stop = vi.fn();
    const stream = { getTracks: () => [{ stop }] } as unknown as MediaStream;
    let grant: ((value: MediaStream) => void) | undefined;
    stubCamera(
      vi.fn(
        () =>
          new Promise<MediaStream>((resolve) => {
            grant = resolve;
          }),
      ),
    );
    vi.stubGlobal("fetch", profileFetch());
    renderProfile();
    const user = userEvent.setup();
    await screen.findByText("ana");

    await user.click(screen.getAllByRole("button", { name: "Usar cámara" })[0]);
    await user.click(await screen.findByRole("button", { name: "Cancelar" }));

    grant?.(stream);
    await waitFor(() => expect(stop).toHaveBeenCalled());
  });

  it("explains a refused upload in Spanish", async () => {
    const fetchMock = profileFetch(() =>
      jsonResponse(
        { title: "Payload too large", status: 413, code: "photo-too-large" },
        413,
        "application/problem+json",
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    renderProfile();
    const user = userEvent.setup();
    const front = new File([new Uint8Array(16)], "front.jpg", { type: "image/jpeg" });

    await user.upload(await screen.findByLabelText("Foto delantera del DNI"), front);
    await user.click(screen.getByRole("button", { name: "Guardar fotos" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Cada foto puede pesar 10 MB como máximo.",
    );
  });

  it("rejects an oversize file before uploading", async () => {
    const fetchMock = profileFetch();
    vi.stubGlobal("fetch", fetchMock);
    renderProfile();
    const user = userEvent.setup();
    const tooBig = new File([new Uint8Array(maxPhotoBytes + 1)], "big.jpg", { type: "image/jpeg" });

    await user.upload(await screen.findByLabelText("Foto delantera del DNI"), tooBig);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Cada foto puede pesar 10 MB como máximo.",
    );
    expect(putCall(fetchMock)).toBeUndefined();
  });
});
