# MVP deployment profile (QA and production)

Interim runtime of increment 1 (ADR-052, SCRUM-99): Keycloak, S-09, S-01, Kafka, RabbitMQ, PostgreSQL, n8n and
the admin web client on **one EC2 instance per environment** with Docker Compose, behind Caddy with HTTPS. The
services, contracts and tokens are the same as in the documented architecture; EKS, Aurora, Istio, API Gateway,
the hub-and-spoke network and the standby region come back after the delivery.

| Host | Goes to |
| --- | --- |
| `auth.<domain>` | Keycloak, realm `vigia` (ADR-050); `/admin` is blocked, use an SSH tunnel |
| `api.<domain>` | `/v1/me` to S-09, `/v1/people` and `/v1/vehicles` to S-01 |
| `admin.<domain>` | Admin web client (`./admin`, built from `apps/admin`) |
| `n8n.<domain>` | n8n editor |

## 1. Images (once per release)

Merging a `release/` branch into `qa` runs the **QA images** workflow, which pushes and signs
`ghcr.io/vigia-factory/s-09-identity-config` and `s-01-community`. Copy each `sha256:...` digest from the
workflow log or the package page on GitHub into `S09_DIGEST` and `S01_DIGEST`. Production uses the same digests
that passed QA.

## 2. Server (once per environment, in its Learner Lab account)

1. EC2 console, region `us-east-1`: **Launch instance**, Amazon Linux 2023, `t3.large`, 30 GB gp3, the lab key
   pair, instance profile `LabInstanceProfile`, and in *Advanced details > User data* the content of
   `bootstrap-ec2.sh`.
2. Security group: inbound `22` from your IP only, `80` and `443` from anywhere.
3. **Elastic IPs > Allocate address**, then **Associate** it with the instance. This is the IP for the domain.
4. Connect: `ssh -i labsuser.pem ec2-user@<elastic-ip>`.
5. Log in to GHCR with a GitHub token that has only `read:packages`:
   `echo <token> | docker login ghcr.io -u <github-user> --password-stdin`.
6. Get the code: `git clone https://github.com/VIGIA-FACTORY/VIGIA.git && cd VIGIA && git checkout qa`
   (production: `main`).

## 3. Domain

- **Best:** the instructor delegates the domain of the environment to Cloudflare (NS records, ADR-041); then
  create the `A` records `auth`, `api`, `admin` and `n8n` pointing to the Elastic IP, with the proxy off while
  Caddy gets the certificates.
- **If only A records are possible:** send the Elastic IP; it does not change between lab sessions.
- **Until the domain exists:** `VIGIA_DOMAIN=<ip-with-dashes>.sslip.io`, for example `3-90-12-45.sslip.io`.

## 4. Start or update

```bash
cd deploy/mvp
cp .env.example .env && nano .env   # secrets with: openssl rand -hex 24
./deploy.sh
```

Create the test accounts through an SSH tunnel to the Keycloak port (passwords from the environment, never
from Git):

```bash
ssh -i labsuser.pem -L 8081:127.0.0.1:8081 ec2-user@<elastic-ip>   # on your computer, keep it open
KEYCLOAK_URL=http://localhost:8081 KEYCLOAK_ADMIN_PASSWORD=... VIGIA_TEST_USER_PASSWORD=... \
  infra/identity/create-test-users.sh
```

In the Keycloak console (`http://localhost:8081/admin`, same tunnel) give the administrator account the role
`ADMIN` of the client `vigia-api`.

## 5. n8n workflow

Open `https://n8n.<domain>`, create the owner account and **Import from file**
`n8n/w-mvp-community-notification.json`. Set the RabbitMQ credential (host `rabbitmq`, user `vigia`, password
`RABBITMQ_PASSWORD`) and an SMTP credential, change the two email addresses and **activate** the workflow.

## 6. Smoke test

```bash
curl -fsS https://auth.<domain>/realms/vigia/.well-known/openid-configuration > /dev/null && echo identity ok
curl -s -o /dev/null -w '%{http_code}\n' https://api.<domain>/v1/me    # 401 without a token
```

Sign in at `https://admin.<domain>`, register a person and a vehicle, and check the email sent by n8n.

## Cost and shutdown

About USD 0.09 per hour with the instance on. Stop the instance at the end of each session; the Elastic IP and
the disk cost about USD 0.15 per day while it is stopped. Data stays in the Docker volumes on the disk.
