# Identity (ADR-050)

| Path | Content |
| --- | --- |
| `realm/` | Realm `vigia` as code; see `realm/README.md` |
| `create-test-users.sh` | Keycloak local test accounts of 05.03 §1.3: 4 owners and 2 drivers. Staff test users live in Entra ID |

Local run:

```bash
docker compose --profile identity up -d keycloak
VIGIA_TEST_USER_PASSWORD='<at least 12 characters>' infra/identity/create-test-users.sh
```
