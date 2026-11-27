# Service template

Every VIGIA service starts from this folder (05.01 §2.2, ADR-037 v1.3). The folder itself is a working
Spring Boot 4.1 / Java 21 service in the hexagonal style, built and tested with the rest of the monorepo
so the template never rots (QAS-26).

## Generate a service

```bash
./gradlew generateService -Pservice=s-01-community -Pstyle=hexagonal
pnpm install
./gradlew :services:s-01-community:check
```

`-Pstyle` may be left out: the generator takes the primary style of the service from ADR-037 v1.3 and
rejects a different value, so the code never drifts from the decision record.

| Style | Services (ADR-037) | Packages | ArchUnit rules |
| --- | --- | --- | --- |
| `hexagonal` | S-01, S-03, S-04, S-06, S-07, S-08, S-13 | `domain`, `application`, `application.port`, `adapter.in`, `adapter.out` | Domain free of Spring and adapters; application free of adapters; ports with at most 5 methods (DP-04, DP-05) |
| `layered` | S-02, S-09 | `web`, `service`, `repository`, `model` | Layers only call downward; controllers never call repositories |
| `event-driven` | S-05, S-10, S-11, S-12 | `listener`, `handler`, `domain`, `publisher` | Domain free of Spring; handlers do not depend on listeners |

The package names of the `layered` and `event-driven` styles are a convention introduced here; 05.01 §2.2
names only the hexagonal packages.

## What every generated service gets

| Concern | Where |
| --- | --- |
| Health probes `/actuator/health/liveness` and `/readiness` (ADR-033), Prometheus metrics, OpenTelemetry tracing | `application.yml` |
| Test that starts the service against PostgreSQL in Testcontainers and checks both probes | `ApplicationHealthTest` |
| Flyway migrations, schema validated by Hibernate | `src/main/resources/db/migration/` |
| Structured JSON logs on stdout (ADR-024) | `logback-spring.xml` |
| Container image (non-root, Java 21 JRE) | `Dockerfile` |
| Values for the common Helm chart | `helm/values.yaml` |
| Local run with its own database and user (ADR-003) | `local/compose.yaml`, `local/create-database.sql` |
| Scripts that let Turborepo run its Gradle tasks (ADR-011) | `package.json` |

The name must match 05.01 §2.1 (`s-NN-<context>`). From `s-03-access-control` the generator derives the
Gradle project `:services:s-03-access-control`, the package `ec.edu.uce.vigia.accesscontrol`, the class
`AccessControlApplication`, the local database and user `vigia_access_control` and the local port `8103`.

The template uses PostgreSQL; a service whose store is DynamoDB or Redis (03.08.03) changes its
dependencies in its first story. `generator.gradle.kts`, `styles/` and this README are not copied.
