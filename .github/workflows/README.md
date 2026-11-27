# GitHub Actions workflows

The CI pipeline of 05.01 §3.2 (US-003, SCRUM-62). No workflow uses AWS credentials (ADR-012); every action
is pinned by commit SHA and Renovate keeps the pins current.

## `pr-checks.yml`: every pull request to `dev` and `qa`

| Job | Stage | What fails it |
| --- | --- | --- |
| 0 Conventions (commitlint) | 0 | A commit without Conventional Commits format or without a `SCRUM-NN` key |
| 1 Lint and types | 1 | Spotless, Checkstyle, ESLint or `tsc` errors |
| 2 Unit and architecture | 2 | A failing JUnit or ArchUnit test, or less than 80 % line coverage in `*.domain` |
| 4 Secrets (Gitleaks) | 4 | A secret in the commits of the pull request |
| 5 SAST (Semgrep) | 5 | An ERROR finding of `p/java`, `p/typescript` or `p/owasp-top-ten` |
| 5 Quality gate (SonarQube) | 5 | The SonarCloud quality gate; skipped with a notice while `SONAR_TOKEN` is not set |
| 6 Dependencies (Trivy, SBOM) | 6 | A fixable CRITICAL vulnerability in a dependency; the SBOM is kept as an artifact |
| 7 IaC and workflows | 7 | zizmor, Checkov or Trivy config findings; TFLint once `infra/**/*.tf` exists |
| 8 Integration (Testcontainers) | 8 | A failing test tagged `integration` |

Stage 3 (contracts: buf, Pact, JSON Schema) is added with the first contract in `packages/contracts`.

## `qa-images.yml`: every merge to `qa`

Stage 9: for each folder in `services/`, Jib builds the image, Trivy scans it (CRITICAL fails), Jib pushes it
to `ghcr.io/vigia-factory/<service>` tagged with the commit SHA, and cosign signs it by digest (keyless).
Stage 10, writing the digests into `deploy/qa` for Argo CD, comes with US-004 (SCRUM-69).

## Checks before a push

```bash
./gradlew check                    # Spotless, Checkstyle, unit, ArchUnit, JaCoCo and integration tests
./gradlew spotlessApply            # fixes the Java formatting
pnpm turbo run lint typecheck test
```
