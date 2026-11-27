# Keycloak realm as code

The realm `vigia` (clients, Entra ID identity provider, role mappers) is exported here as JSON by US-219
(SCRUM-95, ADR-050). The local profile `identity` of `docker-compose.yml` imports every `*.json` file of
this folder when Keycloak starts.
