// Fetches the runtime OpenAPI document from the running backend and writes the
// versioned snapshot that the generated API types are built from (ADR-0005).
// The backend stays the source of truth; refreshing the snapshot turns contract
// drift into a reviewable diff.
import { writeFile } from "node:fs/promises";

const docsUrl = process.env.API_DOCS_URL ?? "http://localhost:8070/v3/api-docs";
const snapshotPath = new URL("../openapi.json", import.meta.url);

const response = await fetch(docsUrl, { headers: { Accept: "application/json" } });
if (!response.ok) {
  console.error(`Fetching ${docsUrl} failed: ${response.status} ${response.statusText}`);
  process.exit(1);
}

const spec = await response.json();
await writeFile(snapshotPath, `${JSON.stringify(spec, null, 2)}\n`);
console.log(`OpenAPI snapshot written from ${docsUrl} to ${snapshotPath.pathname}`);
