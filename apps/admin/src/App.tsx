import { User, UserManager, WebStorageStateStore } from "oidc-client-ts";
import { useEffect, useMemo, useState } from "react";
import { createApi, type Me } from "./api";
import type { VigiaConfig } from "./config";
import { PeoplePage } from "./PeoplePage";
import { VehiclesPage } from "./VehiclesPage";

type Tab = "people" | "vehicles";

/** Sign-in with Keycloak (authorization code with PKCE, ADR-050), then the screens of S-01. */
export function App({ config }: { config: VigiaConfig }) {
  const manager = useMemo(
    () =>
      new UserManager({
        authority: config.authority,
        client_id: config.clientId,
        redirect_uri: `${window.location.origin}/`,
        post_logout_redirect_uri: `${window.location.origin}/`,
        response_type: "code",
        scope: "openid profile email",
        userStore: new WebStorageStateStore({ store: window.sessionStorage }),
      }),
    [config],
  );
  const [user, setUser] = useState<User | null>(null);
  const [me, setMe] = useState<Me | null>(null);
  const [tab, setTab] = useState<Tab>("people");
  const [error, setError] = useState<string | null>(null);
  const api = useMemo(() => createApi(config.apiUrl, () => user?.access_token), [config, user]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const signIn = params.has("code")
      ? manager.signinRedirectCallback().then((signedIn) => {
          window.history.replaceState({}, "", "/");
          return signedIn;
        })
      : manager.getUser();
    signIn.then(setUser).catch((failure: Error) => setError(failure.message));
  }, [manager]);

  useEffect(() => {
    if (user && !user.expired) {
      api.me().then(setMe).catch((failure: Error) => setError(failure.message));
    }
  }, [api, user]);

  if (!user || user.expired) {
    return (
      <main className="center">
        <h1>VIGIA</h1>
        <p>Control de acceso vehicular del campus</p>
        <button onClick={() => void manager.signinRedirect()}>Iniciar sesión</button>
        {error && <p className="error">{error}</p>}
      </main>
    );
  }

  const isAdmin = me?.roles.includes("ADMIN") ?? false;
  return (
    <div>
      <header>
        <strong>VIGIA · Administración</strong>
        <nav>
          <button className={tab === "people" ? "active" : ""} onClick={() => setTab("people")}>
            Personas
          </button>
          <button className={tab === "vehicles" ? "active" : ""} onClick={() => setTab("vehicles")}>
            Vehículos
          </button>
        </nav>
        <span>
          {me ? `${me.displayName ?? me.username} (${me.roles.join(", ") || "sin rol"})` : "…"}
          <button className="link" onClick={() => void manager.signoutRedirect()}>
            Salir
          </button>
        </span>
      </header>
      <main>
        {error && <p className="error">{error}</p>}
        {tab === "people" ? <PeoplePage api={api} canWrite={isAdmin} /> : <VehiclesPage api={api} canWrite={isAdmin} />}
      </main>
    </div>
  );
}
