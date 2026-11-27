# deploy

GitOps desired state read by Argo CD (ADR-012).

| Folder | Content |
| --- | --- |
| `charts/service/` | Common Helm chart |
| `platform/` | Istio, KEDA, Kyverno, External Secrets, Argo CD, Strimzi, RabbitMQ operator, Keycloak Operator, kube-prometheus-stack, cloudflared |
| `qa/` | Per-service values for QA, updated by CI after a merge to `qa` (D-79) |
| `prd/` | Per-service values for production, changed only by the pull request from `qa` to `main` (D-79) |
