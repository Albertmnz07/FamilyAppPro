import type { ReactNode } from "react";
import { Navigate } from "react-router";
import { getToken } from "../auth/token";

// Route protection checks token presence only; real authorization stays
// server-side (spec #14, "Routes").
export function RequireToken({ children }: { children: ReactNode }) {
  if (!getToken()) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}
