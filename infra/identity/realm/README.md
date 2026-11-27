# Keycloak realm as code

`vigia-realm.json` is the realm `vigia` of ADR-050 (US-219, SCRUM-95). Docker compose (profile `identity`)
and the Keycloak Operator import it on start, so a rebuilt environment needs no manual step (K4).

| Part | Content |
| --- | --- |
| Roles | `GUARD`, `SUPERVISOR`, `OWNER`, `DRIVER`, `ADMIN`, `FINANCE_ADMIN` as roles of the client `vigia-api` (03.08.04.01). Being client roles, they are the only values the `vigia:role` claim can carry |
| Claims | Every client maps `vigia:role` (list), `vigia:gatehouse` (user attribute `gatehouse`) and the audience `vigia-api` into the access token |
| Tokens | RS256 (the API Gateway JWT authorizer needs RSA keys); access token 15 minutes |
| Sessions | Realm maximum 30 days (owners and drivers); `vigia-admin` 8 h and `vigia-gate`, `vigia-desktop` 12 h (03.08.04.01 §2) |
| User clients | `vigia-admin`, `vigia-gate`, `vigia-portal`, `vigia-mobile`, `vigia-desktop`: public, authorization code with PKCE S256, no password grant |
| Machine clients | `vigia-s-09` (Keycloak Admin API: identity providers and users), `vigia-s-01`, `vigia-n8n`: client credentials. Scopes per operation come with US-023 |
| Staff | OIDC identity provider `entra-id`, disabled until the Entra ID tenant exists (OI-09). S-09 writes its group mappers (US-021, US-210) |

## Values read from the environment

The file holds no secret. Keycloak replaces each `${NAME}` with the environment variable of the same name
when it imports the realm. Docker compose sets local defaults; in QA and production they come from Secrets
Manager.

| Variable | Use |
| --- | --- |
| `VIGIA_ADMIN_URL`, `VIGIA_GATE_URL`, `VIGIA_PORTAL_URL` | Redirect URIs and web origins of the web clients |
| `VIGIA_S09_CLIENT_SECRET`, `VIGIA_S01_CLIENT_SECRET`, `VIGIA_N8N_CLIENT_SECRET` | Machine client secrets |
| `VIGIA_ENTRA_TENANT_ID`, `VIGIA_ENTRA_CLIENT_ID`, `VIGIA_ENTRA_CLIENT_SECRET` | Entra ID identity provider |

## Test

`./gradlew :libs:security:integrationTest` starts Keycloak 26.8 with this file (Testcontainers) and checks the
discovery document, the RS256 key, a guard token with `vigia:role`, `vigia:gatehouse`, audience and a
15-minute lifetime, machine client secrets and PKCE on the public clients.
