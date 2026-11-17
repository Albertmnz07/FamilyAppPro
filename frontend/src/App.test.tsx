import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import App from "./App";
import { clearToken, setToken } from "./auth/token";

afterEach(() => {
  clearToken();
  window.history.replaceState(null, "", "/");
});

describe("App", () => {
  it("lands unidentified visitors on the login screen", () => {
    window.history.replaceState(null, "", "/");
    render(<App />);

    expect(screen.getByRole("heading", { name: "Iniciar sesión" })).toBeInTheDocument();
  });

  it("shows the families placeholder and the shell to an identified visitor", () => {
    setToken("token-1");
    window.history.replaceState(null, "", "/families");
    render(<App />);

    expect(screen.getByRole("heading", { name: "Familias" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Volver" })).toBeInTheDocument();
    expect(screen.getByText("FamilyApp")).toBeInTheDocument();
  });

  it("redirects unidentified visitors from protected routes to the login screen", () => {
    window.history.replaceState(null, "", "/families");
    render(<App />);

    expect(screen.getByRole("heading", { name: "Iniciar sesión" })).toBeInTheDocument();
  });
});
