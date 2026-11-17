// Spring Data serves paged listings in its own Page JSON shape (content,
// pageable, totalElements, ...). The front-end normalizes it to the fields it
// needs; extra keys such as pageable/sort are server concerns and are dropped.

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  /** Zero-based index of the current page. */
  number: number;
  size: number;
};

/** A 200 whose body is not a Spring Page: a broken response, not a network outage. */
export class InvalidPageError extends TypeError {}

export function parsePage<T>(payload: unknown): Page<T> {
  const content = (payload as { content?: unknown } | null)?.content;
  if (!Array.isArray(content)) {
    throw new InvalidPageError("Expected a Spring Page response with a content array");
  }
  const record = payload as Record<string, unknown>;
  return {
    content: content as T[],
    totalElements: countOf(record.totalElements) ?? content.length,
    totalPages: countOf(record.totalPages) ?? 0,
    number: countOf(record.number) ?? 0,
    size: countOf(record.size) ?? content.length,
  };
}

function countOf(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}
