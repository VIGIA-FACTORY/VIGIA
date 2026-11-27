# VIGIA

Vehicle entry and exit control for the university campus: 13 Spring Boot services (S-01 to S-13), web,
mobile and desktop clients, on AWS. The documentation lives in Confluence, space SCRUM; work is tracked in
Jira, project SCRUM.

## Requirements

| Tool | Version |
| --- | --- |
| Java | 21 (LTS) |
| Node.js | 24 (LTS) |
| pnpm | 12.23.0 (`corepack enable` or `npx pnpm@12.23.0`) |
| Docker Desktop | with Docker Compose |

Gradle is not installed separately: the wrapper `./gradlew` (Gradle 9.9.2) is used.

## Build and test

```bash
pnpm install
pnpm turbo run build     # TypeScript packages and, through their package.json scripts, the Gradle projects
pnpm turbo run test
./gradlew check          # Gradle only
```

## Local environment

```bash
docker compose up -d                       # PostgreSQL, Kafka, RabbitMQ
docker compose --profile cache up -d       # + Redis
docker compose --profile aws up -d         # + LocalStack (DynamoDB, S3)
docker compose --profile identity up -d    # + Keycloak (http://localhost:8180)
```

## New service

```bash
./gradlew generateService -Pservice=s-01-community -Pstyle=hexagonal
```

See `tools/service-template/README.md`.

## Branches and pull requests

Task branch `feat/`, `fix/` or `docs/` plus the Jira key, from `dev` → pull request to `dev` → `release/`
branch → pull request to `qa` (deploys QA-1) → pull request from `qa` to `main` at the close of the sprint,
approved by the course instructor (deploys PRD-1). Commits follow Conventional Commits with the Jira key and
a body that says what changed, why and how it was tested. Templates are in `.github/PULL_REQUEST_TEMPLATE/`.

## Pipeline

Pull requests to `dev` and `qa` run stages 0 to 8 of 05.01 §3.2 (`.github/workflows/pr-checks.yml`); a merge
to `qa` builds, scans, pushes and signs the service images (`qa-images.yml`). Details and the checks to run
before a push are in `.github/workflows/README.md`. To scan staged changes for secrets before each commit:

```bash
git config core.hooksPath tools/git-hooks
```

## Layout

The folder tree is the one of Confluence 05.01 §2.1; each top-level folder has a README.
