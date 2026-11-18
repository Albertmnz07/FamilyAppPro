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

// jsdom does not update its internal file list when user-event attaches files, so
// a required file input reports `valueMissing` even after a file is chosen, and
// native submission blocks. Real browsers compute this correctly; this shim only
// re-aligns the file input's validity with the files actually attached.
const validityDescriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "validity");
Object.defineProperty(HTMLInputElement.prototype, "validity", {
  configurable: true,
  get(this: HTMLInputElement) {
    const validity = validityDescriptor!.get!.call(this) as ValidityState;
    if (this.type !== "file") return validity;
    const missing = this.files === null || this.files.length === 0;
    const valueMissing = this.required && missing;
    const valid = !valueMissing && !validity.customError;
    return new Proxy(validity, {
      get(target, property) {
        if (property === "valueMissing") return valueMissing;
        if (property === "valid") return valid;
        return Reflect.get(target, property);
      },
    });
  },
});

// jsdom's native submission also ignores that shim, so the happy paths dispatch
// the submit event directly once the form is complete (as the user would).
function submitRegisterForm(): void {
  fireEvent.submit(document.querySelector("form")!);
}

// jsdom has no object-URL support; the preview only needs a stable src.
URL.createObjectURL = vi.fn(() => "blob:preview");
URL.revokeObjectURL = vi.fn();

const maxPhotoBytes = 1_048_576;

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
const photoTooLargeBody = fromPartial<ProblemDetail>({
  title: "Payload too large",
  status: 413,
  code: "photo-too-large",
});
const unsupportedPhotoTypeBody = fromPartial<ProblemDetail>({
  title: "Unsupported media type",
  status: 415,
  code: "unsupported-photo-type",
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

function validPhoto(name: string): File {
  return new File([new Uint8Array(16)], name, { type: "image/jpeg" });
}

function oversizePhoto(): File {
  return new File([new Uint8Array(maxPhotoBytes + 1)], "front.jpg", { type: "image/jpeg" });
}

function frontPhotoInput(): HTMLInputElement {
  return screen.getByLabelText("Foto delantera del DNI") as HTMLInputElement;
}

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Nombre de usuario"), "ana");
  await user.type(screen.getByLabelText("DNI"), "12345678Z");
  await user.type(screen.getByLabelText("Contraseña"), "secret-1");
  await user.upload(frontPhotoInput(), validPhoto("front.jpg"));
  await user.upload(screen.getByLabelText("Foto trasera del DNI"), validPhoto("back.jpg"));
}

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe("RegisterPage", () => {
  it("renders the registration form in Spanish with autofill hints and photo inputs", () => {
    renderRegister();

    expect(screen.getByRole("heading", { name: "Crear cuenta" })).toBeInTheDocument();
    expect(screen.getByLabelText("Nombre de usuario")).toHaveAttribute("autocomplete", "username");
    expect(screen.getByLabelText("DNI")).toHaveAttribute("autocomplete", "off");
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("autocomplete", "new-password");
    expect(screen.getByLabelText("Contraseña")).toHaveAttribute("type", "password");
    expect(frontPhotoInput()).toHaveAttribute("type", "file");
    expect(frontPhotoInput()).toHaveAttribute("accept", "image/jpeg,image/png");
    expect(frontPhotoInput()).toBeRequired();
    expect(screen.getByLabelText("Foto trasera del DNI")).toBeRequired();
    expect(screen.getByRole("button", { name: "Crear cuenta" })).toBeInTheDocument();
  });

  it("rejects an oversize photo before submitting and points at the field", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    renderRegister();

    await user.upload(frontPhotoInput(), oversizePhoto());

    expect(fetchMock).not.toHaveBeenCalled();
    expect(screen.getByText("Cada foto puede pesar 1 MB como máximo.")).toBeInTheDocument();
    expect(frontPhotoInput()).toHaveAttribute("aria-invalid", "true");
    expect(screen.queryByAltText("Vista previa de la foto delantera del DNI")).not.toBeInTheDocument();
  });

  it("rejects a photo that is not JPEG or PNG before submitting", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup({ applyAccept: false });
    renderRegister();

    await user.upload(frontPhotoInput(), new File(["x"], "notes.pdf", { type: "application/pdf" }));

    expect(fetchMock).not.toHaveBeenCalled();
    expect(screen.getByText("Las fotos deben ser JPEG o PNG.")).toBeInTheDocument();
    expect(frontPhotoInput()).toHaveAttribute("aria-invalid", "true");
  });

  it("shows a local preview for a valid photo", async () => {
    const user = userEvent.setup();
    renderRegister();

    await user.upload(frontPhotoInput(), validPhoto("front.jpg"));

    const preview = await screen.findByAltText("Vista previa de la foto delantera del DNI");
    expect(preview).toHaveAttribute("src", "blob:preview");
    expect(frontPhotoInput()).toHaveAttribute("aria-invalid", "false");
  });

  it("submits the registration as multipart form data and lands on login with a notice", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(personaResponseBody, 201));
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    const { currentPath } = renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    await waitFor(() => expect(currentPath()).toBe("/login"));
    expect(screen.getByText(registrationSuccessNotice)).toBeInTheDocument();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/v1/personas");
    expect(init.method).toBe("POST");
    expect(init.headers.get("Authorization")).toBeNull();
    const body = init.body as FormData;
    expect(body).toBeInstanceOf(FormData);
    expect(body.get("username")).toBe("ana");
    expect(body.get("dni")).toBe("12345678Z");
    expect(body.get("password")).toBe("secret-1");
    expect(body.get("frontPhoto")).toBeInstanceOf(File);
    expect(body.get("backPhoto")).toBeInstanceOf(File);
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

    expect(await screen.findByText("Ya existe una persona registrada con ese DNI.")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByLabelText("DNI")).toHaveFocus());
  });

  it("maps a validation-failed field error onto the offending input", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(jsonResponse(validationFailedBody, 400, "application/problem+json")),
    );
    const user = userEvent.setup();
    renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    await waitFor(() =>
      expect(screen.getByLabelText("Nombre de usuario")).toHaveAttribute("aria-invalid", "true"),
    );
  });

  it("maps a photo-too-large response onto the photo inputs", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(photoTooLargeBody, 413)));
    const user = userEvent.setup();
    renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    expect(await screen.findAllByText("Cada foto puede pesar 1 MB como máximo.")).toHaveLength(2);
    await waitFor(() => expect(frontPhotoInput()).toHaveAttribute("aria-invalid", "true"));
    expect(screen.getByLabelText("Foto trasera del DNI")).toHaveAttribute("aria-invalid", "true");
  });

  it("maps an unsupported-photo-type response onto the photo inputs", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(unsupportedPhotoTypeBody, 415)));
    const user = userEvent.setup();
    renderRegister();

    await fillValidForm(user);
    submitRegisterForm();

    expect(await screen.findAllByText("Las fotos deben ser JPEG o PNG.")).toHaveLength(2);
    await waitFor(() => expect(frontPhotoInput()).toHaveAttribute("aria-invalid", "true"));
    expect(screen.getByLabelText("Foto trasera del DNI")).toHaveAttribute("aria-invalid", "true");
  });

  it("recovers after a rejected photo once a new photo is chosen", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(photoTooLargeBody, 413)));
    const user = userEvent.setup();
    renderRegister();

    await fillValidForm(user);
    submitRegisterForm();
    await waitFor(() => expect(frontPhotoInput()).toHaveAttribute("aria-invalid", "true"));

    await user.upload(frontPhotoInput(), validPhoto("front-fixed.jpg"));

    await waitFor(() => expect(frontPhotoInput()).toHaveAttribute("aria-invalid", "false"));
    // The sibling photo is unblocked too, so the next submit is not silently refused.
    const back = screen.getByLabelText("Foto trasera del DNI") as HTMLInputElement;
    expect(back.validity.customError).toBe(false);
  });

  it("rejects a DNI whose check letter is wrong, on blur", async () => {
    const user = userEvent.setup();
    renderRegister();

    const dni = screen.getByLabelText("DNI");
    await user.type(dni, "12345678A");
    await user.tab();
    expect(dni).toHaveAttribute("aria-invalid", "true");

    await user.clear(dni);
    await user.type(dni, "12345678Z");
    await user.tab();
    await waitFor(() => expect(dni).toHaveAttribute("aria-invalid", "false"));
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
    expect(frontPhotoInput()).toHaveAttribute("aria-invalid", "true");
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

    expect(await screen.findByText("No se pudo conectar con el servidor. Inténtalo de nuevo.")).toBeInTheDocument();
  });
});
