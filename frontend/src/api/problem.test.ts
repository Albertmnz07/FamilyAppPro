import { fromPartial } from "@total-typescript/shoehorn";
import { describe, expect, it } from "vitest";
import { ApiError, normalizeFieldErrors, parseProblem, type ProblemDetail } from "./problem";

const problemResponse = (status: number, body: unknown): Response =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/problem+json" } });

describe("ApiError.fromResponse", () => {
  it("carries the status, code and detail of a ProblemDetail body", async () => {
    const body = fromPartial<ProblemDetail>({
      type: "about:blank",
      title: "Unauthorized",
      status: 401,
      detail: "Invalid credentials",
      code: "unauthorized",
    });
    const error = await ApiError.fromResponse(problemResponse(401, body));

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(401);
    expect(error.code).toBe("unauthorized");
    expect(error.problem?.detail).toBe("Invalid credentials");
  });

  it("carries only the HTTP status when the body is not a ProblemDetail", async () => {
    const error = await ApiError.fromResponse(new Response("<html>boom</html>", { status: 502 }));

    expect(error.status).toBe(502);
    expect(error.code).toBeUndefined();
    expect(error.problem).toBeUndefined();
  });
});

describe("parseProblem", () => {
  it("returns undefined for payloads that are not problem objects", () => {
    expect(parseProblem(["not", "an", "object"])).toBeUndefined();
    expect(parseProblem("unauthorized")).toBeUndefined();
  });
});

describe("normalizeFieldErrors", () => {
  it("strips known field prefixes and moves request-level entries to general messages", () => {
    const normalized = normalizeFieldErrors([
      { field: "register.username", message: "must not be blank" },
      { field: "request", message: "Malformed request" },
      { field: "password", message: "must not be blank" },
    ]);

    expect(normalized.fieldErrors).toEqual([
      { field: "username", message: "must not be blank" },
      { field: "password", message: "must not be blank" },
    ]);
    expect(normalized.generalMessages).toEqual(["Malformed request"]);
  });
});
