#!/usr/bin/env bash
# Creates the Keycloak local test accounts of 05.03 §1.3 in the realm "vigia": 4 owners and 2 drivers
# (ADR-050). Staff test users live in Entra ID and are not created here. Safe to run again: existing users
# are skipped.
#
#   VIGIA_TEST_USER_PASSWORD=<password> infra/identity/create-test-users.sh
#
# Environment:
#   VIGIA_TEST_USER_PASSWORD  password of every test account (required; at least 12 characters, realm policy)
#   KEYCLOAK_URL              default http://localhost:8180 (docker compose profile "identity")
#   KEYCLOAK_ADMIN_PASSWORD   bootstrap admin password, default "admin" (local only)
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
PASSWORD="${VIGIA_TEST_USER_PASSWORD:?Set VIGIA_TEST_USER_PASSWORD (at least 12 characters)}"
REALM_API="${KEYCLOAK_URL}/admin/realms/vigia"

json() { python3 -c "import sys, json; print(json.load(sys.stdin)$1)"; }

token="$(curl -fsS -d grant_type=password -d client_id=admin-cli -d username=admin \
  --data-urlencode "password=${ADMIN_PASSWORD}" \
  "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" | json "['access_token']")"
auth=(-H "Authorization: Bearer ${token}" -H "Content-Type: application/json")
api_client="$(curl -fsS "${auth[@]}" "${REALM_API}/clients?clientId=vigia-api" | json "[0]['id']")"

# username, first name, last name, VIGIA role
users=(
  "owner1 Owner One OWNER"
  "owner2 Owner Two OWNER"
  "owner3 Owner Three OWNER"
  "owner4 Owner Four OWNER"
  "driver1 Driver One DRIVER"
  "driver2 Driver Two DRIVER"
)

for entry in "${users[@]}"; do
  read -r username first last role <<< "${entry}"
  existing="$(curl -fsS "${auth[@]}" "${REALM_API}/users?username=${username}&exact=true" | json ".__len__()")"
  if [[ "${existing}" != "0" ]]; then
    echo "skip   ${username} (exists)"
    continue
  fi
  body="$(USERNAME="${username}" FIRST="${first}" LAST="${last}" PASSWORD="${PASSWORD}" python3 -c '
import json, os
print(json.dumps({
    "username": os.environ["USERNAME"], "email": os.environ["USERNAME"] + "@vigia.test",
    "firstName": os.environ["FIRST"], "lastName": os.environ["LAST"],
    "enabled": True, "emailVerified": True,
    "credentials": [{"type": "password", "value": os.environ["PASSWORD"], "temporary": False}],
}))')"
  curl -fsS "${auth[@]}" -X POST "${REALM_API}/users" -d "${body}"
  user_id="$(curl -fsS "${auth[@]}" "${REALM_API}/users?username=${username}&exact=true" | json "[0]['id']")"
  role_json="$(curl -fsS "${auth[@]}" "${REALM_API}/clients/${api_client}/roles/${role}")"
  curl -fsS "${auth[@]}" -X POST "${REALM_API}/users/${user_id}/role-mappings/clients/${api_client}" -d "[${role_json}]"
  echo "create ${username} (${role})"
done
