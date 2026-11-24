// RFC 9457 ProblemDetail shaping: the backend answers every failure with
// `application/problem+json` carrying `title`, `status`, `detail` plus its own
// `code`, and an `errors` array for validation failures.

export type FieldError = { field: string; message: string };

export type ProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: FieldError[];
};

export class ApiError extends Error {
  readonly status: number;
  readonly problem?: ProblemDetail;

  constructor(status: number, problem?: ProblemDetail) {
    super(problem?.detail ?? `HTTP ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.problem = problem;
  }

  get code(): string | undefined {
    return this.problem?.code;
  }

  static async fromResponse(response: Response): Promise<ApiError> {
    return new ApiError(response.status, await problemFrom(response));
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

async function problemFrom(response: Response): Promise<ProblemDetail | undefined> {
  try {
    return parseProblem(await response.json());
  } catch {
    return undefined;
  }
}

/** Lenient reader: returns a ProblemDetail only when the payload is an object. */
export function parseProblem(payload: unknown): ProblemDetail | undefined {
  if (typeof payload !== "object" || payload === null || Array.isArray(payload)) {
    return undefined;
  }
  const record = payload as Record<string, unknown>;
  const problem: ProblemDetail = {};
  for (const key of ["type", "title", "detail", "instance", "code"] as const) {
    const value = record[key];
    if (typeof value === "string") {
      problem[key] = value;
    }
  }
  if (typeof record.status === "number") {
    problem.status = record.status;
  }
  if (Array.isArray(record.errors)) {
    problem.errors = record.errors.filter(isFieldError);
  }
  return problem;
}

function isFieldError(value: unknown): value is FieldError {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as Record<string, unknown>).field === "string" &&
    typeof (value as Record<string, unknown>).message === "string"
  );
}

export type NormalizedFieldErrors = {
  fieldErrors: FieldError[];
  generalMessages: string[];
};

/**
 * The backend qualifies validation fields ("register.username") or reports
 * method-level problems as "request": strip the prefixes so errors can be
 * attached to form fields, and turn "request" entries into general messages.
 */
export function normalizeFieldErrors(errors: readonly FieldError[]): NormalizedFieldErrors {
  const fieldErrors: FieldError[] = [];
  const generalMessages: string[] = [];
  for (const { field, message } of errors) {
    const unqualified = field.includes(".") ? field.slice(field.indexOf(".") + 1) : field;
    if (unqualified === "request" || unqualified === "") {
      generalMessages.push(message);
    } else {
      fieldErrors.push({ field: unqualified, message });
    }
  }
  return { fieldErrors, generalMessages };
}
