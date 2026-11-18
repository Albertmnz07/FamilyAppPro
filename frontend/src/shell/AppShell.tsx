import { Outlet, useLocation, useNavigate } from "react-router";

// Mobile shell (spec #14, "Routes"): a top bar with the app name and a back
// affordance; no side menu. Deep links without history fall back to the
// families home.
export function AppShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const historyState = window.history.state as { idx?: number } | null;
  const canGoBack = typeof historyState?.idx === "number" && historyState.idx > 0;

  return (
    <div className="shell">
      <header className="top-bar">
        {location.pathname !== "/login" && (
          <button
            type="button"
            className="back-button"
            aria-label="Volver"
            onClick={() => {
              if (canGoBack) navigate(-1);
              else navigate("/families");
            }}
          >
            <span aria-hidden="true">←</span>
          </button>
        )}
        <span className="app-name">FamilyApp</span>
      </header>
      <main className="shell-main">
        <Outlet />
      </main>
    </div>
  );
}
