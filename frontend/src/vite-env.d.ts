/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Absolute base URL of the API, e.g. http://localhost:8070/api/v1 (ADR-0006). */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
