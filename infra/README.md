# infra

Terraform, run from the developer's machine with the Learner Lab session credentials (ADR-013).
`terraform apply` and `destroy` are run only by the project owner.

| Folder | Content |
| --- | --- |
| `modules/` | network-hub, network-spoke, transit-gateway, bastion, eks, aurora, dynamodb, iot, edge-access, observability, datalake, beanstalk, fargate, ecr-cache |
| `identity/` | Keycloak realm as code (`realm/`) and the scripts that create the test users (ADR-050, 05.03 §1.3) |
| `envs/` | `qa-1/us-east-1`, `prd-1/us-east-1`, `prd-1/us-west-2`, `prd-2`, `prd-7` |
