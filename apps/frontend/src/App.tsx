import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from "react-router";
import { onUnauthorized, SESSION_EXPIRED_NOTICE } from "./auth/session";
import { queryClient } from "./queryClient";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { FamiliesPage } from "./pages/FamiliesPage";
import { FamilyWorkspacePage } from "./pages/FamilyWorkspacePage";
import { InviteRedemptionPage } from "./pages/InviteRedemptionPage";
import { ProfilePage } from "./pages/ProfilePage";
import { RequireToken } from "./routing/RequireToken";
import { AppShell } from "./shell/AppShell";

function AppRoutes() {
  const navigate = useNavigate();

  useEffect(
    () =>
      onUnauthorized(() => {
        navigate("/login", { replace: true, state: { notice: SESSION_EXPIRED_NOTICE } });
      }),
    [navigate],
  );

  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/families"
          element={
            <RequireToken>
              <FamiliesPage />
            </RequireToken>
          }
        />
        <Route
          path="/families/:familyId"
          element={
            <RequireToken>
              <FamilyWorkspacePage />
            </RequireToken>
          }
        />
        <Route
          path="/invites/:code"
          element={
            <RequireToken>
              <InviteRedemptionPage />
            </RequireToken>
          }
        />
        <Route
          path="/mi-perfil"
          element={
            <RequireToken>
              <ProfilePage />
            </RequireToken>
          }
        />
        <Route path="*" element={<Navigate to="/families" replace />} />
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </QueryClientProvider>
  );
}
