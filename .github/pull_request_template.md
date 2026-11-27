<!-- Pull request from a task branch to dev (D-79). Title: type(scope): SCRUM-NN short subject -->

## Jira and traceability

- Jira: SCRUM-NN (link)
- Requirement or ADR: FR-..., NFR-..., ADR-...

## What changes and why

<!-- One paragraph per change. Say why, citing the Confluence page and section. -->

## How it was tested locally

| Check | Command | Result |
| --- | --- | --- |
| Unit and architecture tests | `./gradlew :<project>:check` | |
| Local run | `docker compose ... up` | |
| Screens per client and width (QAS-34) | | |

## Risks and rollback

<!-- What could break, and how to revert (commit to revert, migration notes). -->

## Checklist

- [ ] Conventional Commits with the Jira key and a body (what, why, how tested)
- [ ] Every commit builds and passes its tests
- [ ] No secrets, no `.env` files
- [ ] Confluence updated if the design changed (ADR, 01.04 Change Log)
