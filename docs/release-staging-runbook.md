# Release and Staging Runbook

This runbook is the offline operator path for staging and production release
checks. It is intentionally conservative: do not write Docker digests unless a
networked release workstation has produced verifier-owned evidence, and do not
enable PII backfill unless there is an approved change window.

## 1. Inputs

Prepare these files before running the preflight:

- `.env.staging` or `.env.production`, copied from `.env.example` and filled
  with deployment-specific values.
- `docker-digest-evidence.json`, generated on a networked release workstation
  with:

```powershell
node scripts/resolve-docker-image-digests.mjs --evidence --verifier=<name-or-email> --target-platform=multi-platform-index > docker-digest-evidence.json
```

Do not hand-edit or invent `sha256:` values in an offline environment. If the
evidence is missing, expired by policy, or owned by the wrong verifier, stop and
regenerate it from the registry.

## 2. Offline Preflight

Run from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/deploy-preflight.ps1 -Stage staging -EnvFile .env.staging -DockerDigestEvidence docker-digest-evidence.json
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/deploy-preflight.ps1 -Stage release -EnvFile .env.production -DockerDigestEvidence docker-digest-evidence.json
```

The script is read-only. It checks production posture, PII key shape,
`PII_MIGRATION_ENABLED`, the admin audit migration file, Docker digest evidence,
and `docker compose config --quiet`.

If Docker is not installed on the review workstation, keep the rest of the gate
and skip only Compose parsing:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/deploy-preflight.ps1 -Stage staging -EnvFile .env.staging -SkipComposeConfig
```

## 3. PII Backfill Guard

Default release posture is:

```dotenv
PII_MIGRATION_ENABLED=false
```

Only set `PII_MIGRATION_ENABLED=true` during the approved one-window backfill.
The preflight must then include the ticket/change ID:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/deploy-preflight.ps1 -Stage release -EnvFile .env.production -AllowPiiBackfill -PiiBackfillTicket CHG-1234
```

Before opening that window, record:

- Snapshot/backup ID for MySQL and Redis.
- Active `PII_ACTIVE_KID` and full `PII_KEYS` kid list, without copying secret
  material into the ticket.
- Expected affected tables and validation query owner.
- Rollback owner and the exact time when `PII_MIGRATION_ENABLED` will be set
  back to `false`.

After the backfill finishes, deploy or restart with `PII_MIGRATION_ENABLED=false`
and rerun the preflight without `-AllowPiiBackfill`.

## 4. Admin Audit Migration

The release must include Flyway migration:

```text
backend/src/main/resources/db/migration/V24__create_admin_audit_logs.sql
```

Preflight requires:

- `SPRING_FLYWAY_ENABLED=true`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
- the migration file above present in the release tree

Before promotion, confirm the deployed database has accepted the migration via
Flyway history and that `admin_audit_logs` exists with the expected indexes. Do
not bypass Flyway with Hibernate DDL auto-update in staging or release.

## 5. Supply-chain Evidence

Run the existing offline supply-chain gates:

```powershell
npm run test:ops
node scripts/check-supply-chain-pins.mjs --evidence-only --evidence docker-digest-evidence.json
npm run check:supply-chain-pins
```

`npm run check:supply-chain-pins` is allowed to fail only while mutable Docker
image references remain intentionally unpinned before release. Treat that as a
release stop sign, not a flaky test.

## 6. Rollback Checklist

Before promotion:

- Confirm the previous release artifact, image tags/digests, and environment
  file are available.
- Confirm database backup/snapshot restore instructions have been rehearsed.
- Confirm `PII_MIGRATION_ENABLED=false` is the rollback default.
- Confirm admin access still requires TOTP and `SUPER_ADMIN_TOTP_SECRET` is not
  a placeholder.
- Confirm `PAYMENT_MOCK_CALLBACK_ENABLED=false`, `SEED_DEMO_USERS=false`, and
  `SEED_CONTENT_ENABLED=false`.

During rollback:

- Stop new traffic at the reverse proxy or load balancer.
- Restore the previous release artifact and previous environment file.
- Restore database only if the incident owner confirms data corruption or a bad
  irreversible migration; otherwise keep forward data and roll back code only.
- Re-run `scripts/deploy-preflight.ps1` against the rollback env file before
  bringing traffic back.
- Record the exact artifact, env file checksum, snapshot ID, and operator names
  in the incident/release record.

## 7. Handoff Record

Attach these outputs to the release record:

- `scripts/deploy-preflight.ps1` command line and pass/fail result.
- `npm run test:ops` result.
- `node scripts/check-supply-chain-pins.mjs --evidence-only --evidence docker-digest-evidence.json` result.
- `npm run check:supply-chain-pins` result.
- `docker compose --env-file <env-file> -f docker-compose.prod.yml config --quiet` result, unless explicitly skipped due to a Docker-free review host.
- Docker digest evidence JSON generated by the named verifier.
