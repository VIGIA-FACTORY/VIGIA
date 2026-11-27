#!/usr/bin/env bash
# Starts or updates the MVP on this server (ADR-052). Run from deploy/mvp after filling in .env.
set -euo pipefail
cd "$(dirname "$0")"

[ -f .env ] || { echo "Missing .env: cp .env.example .env and fill it in" >&2; exit 1; }
# shellcheck source=/dev/null
set -a; . ./.env; set +a
for name in VIGIA_DOMAIN ACME_EMAIL S09_DIGEST S01_DIGEST POSTGRES_PASSWORD IDENTITY_DB_PASSWORD \
  COMMUNITY_DB_PASSWORD KEYCLOAK_DB_PASSWORD N8N_DB_PASSWORD RABBITMQ_PASSWORD KEYCLOAK_ADMIN_PASSWORD \
  VIGIA_S09_CLIENT_SECRET VIGIA_S01_CLIENT_SECRET VIGIA_N8N_CLIENT_SECRET N8N_ENCRYPTION_KEY; do
  [ -n "${!name:-}" ] || { echo "Set $name in .env" >&2; exit 1; }
done
case "$S09_DIGEST$S01_DIGEST" in *latest*) echo "Use digests, never latest" >&2; exit 1 ;; esac

# Admin web client: built in a Node container from apps/admin (no Node needed on the server), then the
# runtime settings of this environment go into config.js, so the same build serves QA and production.
mkdir -p admin
if [ "${SKIP_ADMIN_BUILD:-false}" != "true" ]; then
  docker run --rm -v "$(cd ../.. && pwd)":/repo -w /repo node:24-alpine sh -c     "corepack enable && pnpm install --frozen-lockfile --filter @vigia/admin... && pnpm --filter @vigia/admin build"
  rm -rf admin && cp -r ../../apps/admin/dist admin
fi
cat > admin/config.js <<CONFIG
window.VIGIA_CONFIG = {
  authority: "https://auth.${VIGIA_DOMAIN}/realms/vigia",
  clientId: "vigia-admin",
  apiUrl: "https://api.${VIGIA_DOMAIN}",
};
CONFIG

docker compose config --quiet
docker compose pull
docker compose up -d --remove-orphans
docker compose ps
echo "Open https://admin.${VIGIA_DOMAIN} (identity: https://auth.${VIGIA_DOMAIN}, n8n: https://n8n.${VIGIA_DOMAIN})"
