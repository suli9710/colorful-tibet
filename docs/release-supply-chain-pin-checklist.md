# Release Supply-chain Pin Checklist

This checklist records the CT-OPS-008 supply-chain pins and the checks required
before a production release. Do not invent or backfill hashes from an offline
environment; regenerate image evidence from the official registry whenever pins
are rotated.

## Required Commands

Run these checks from the repository root before a production release:

```powershell
npm run test:ops
node scripts/resolve-docker-image-digests.mjs --evidence --verifier=<name-or-email> --target-platform=multi-platform-index --output=docker-digest-evidence.json
node scripts/check-supply-chain-pins.mjs --evidence-only --evidence docker-digest-evidence.json
node scripts/resolve-docker-image-digests.mjs --markdown
npm run check:supply-chain-pins
docker compose --env-file .env.example -f docker-compose.yml config --quiet
docker compose --env-file .env.example -f docker-compose.prod.yml config --quiet
```

`npm run check:supply-chain-pins` must pass before release. Treat any failure as
the release stop sign, not as a test flake.

## Completed Pins

The GitHub Actions refs in `.github/workflows/ci.yml` were replaced with full
40-character SHAs verified from the official GitHub repositories via
`git ls-remote` on 2026-06-09 +08:00:

| Action tag | Pinned SHA |
| --- | --- |
| `actions/checkout@v4` | `34e114876b0b11c390a56381ad16ebd13914f8d5` |
| `actions/setup-python@v5` | `a26af69be951a213d495a4c3e4e4022e16d87065` |
| `gitleaks/gitleaks-action@v2` | `dcedce43c6f43de0b836d1fe38946645c9c638dc` |
| `actions/setup-java@v4` | `c1e323688fd81a25caa38c78aa6df2d33d3e20d9` |
| `aquasecurity/trivy-action@v0.36.0` | `a9c7b0f06e461e9d4b4d1711f154ee024b8d7ab8` |
| `actions/setup-node@v4` | `49933ea5288caeca8642d1e84afbd3f7d6820020` |
| `docker/setup-buildx-action@v3` | `8d2750c68a42422c14e847fe6c8ac0403b4cbd6f` |
| `actions/upload-artifact@v4` | `ea165f8d65b6e75b540449e92b4886f43607fa02` |

Scrapler Python dependencies are exact-pinned and hash-locked in
`scrapler/requirements.txt` and `scrapler/requirements-dev.txt`. CI and the
Scrapler Dockerfile now install them with `pip --require-hashes`; the dev lock
resolves the former `pytest>=8.3,<9.0` range to `pytest==8.4.2`.

Docker image references in Dockerfiles, Compose files, and workflow `docker run`
commands were replaced with `name:tag@sha256:<digest>` pins verified through the
official Docker Registry HTTP API v2. The evidence and moving image tags were
refreshed on 2026-07-28 +08:00, including the workflow MySQL service image. The
source-matched evidence is checked in as `docker-digest-evidence.json` and
validated by `scripts/check-supply-chain-pins.mjs`.

## Pin Rotation

- Run `node scripts/resolve-docker-image-digests.mjs --markdown` from a
  networked release workstation that can reach `auth.docker.io` and
  `registry-1.docker.io`.
- Also run `node scripts/resolve-docker-image-digests.mjs --evidence
  --verifier=<name-or-email> --target-platform=multi-platform-index
  --output=docker-digest-evidence.json` and attach that JSON file to the release
  record. `--output` writes a synced temporary file and atomically replaces the
  prior evidence only after every required registry lookup succeeds. Do not use
  shell redirection for this file, because the shell truncates the destination
  before the resolver can fail closed.
- On the release workstation, or later in an offline review environment, run
  `node scripts/check-supply-chain-pins.mjs --evidence-only --evidence docker-digest-evidence.json`.
  This does not contact Docker Hub; it verifies that the evidence has a valid
  schema, names the verifier and target platform/manifest scope, includes every
  required source/image pair, uses sha256 digest format, keeps `pinned`
  consistent with `image@digest`, and does not assign conflicting digests to the
  same image tag.
- Replace every Dockerfile `FROM` image with `name:tag@sha256:<digest>`.
- Replace every Compose `image:` tag with `name:tag@sha256:<digest>`.
- Replace workflow `docker run` and service image tags with
  `name:tag@sha256:<digest>`.
- Rerun `npm run check:supply-chain-pins`, `npm run test:ops`, and both Compose
  config checks after changing any pin.

## Evidence to Record

For each action SHA, image digest, and pip hash, record the lookup source, the
lookup timestamp, target platform where relevant, and the person who verified
it. Acceptable evidence includes registry digest output, GitHub action commit
lookup, and the reviewed `pip-compile --generate-hashes` output from a trusted
networked environment.

For Docker image digests, prefer the resolver evidence JSON over screenshots or
manually copied tables. The JSON is still not a substitute for writing the
verified `@sha256:` references back to Dockerfiles, Compose files, and workflow
commands; it is the audit trail used to explain where those values came from.

## Acceptance

The release may proceed only when `npm run check:supply-chain-pins` exits 0.
That command checks `docker-digest-evidence.json` by default, so digest pins and
their operator evidence must advance together. The two `docker compose
config --quiet` commands must still parse, and the release record must contain
the verification evidence above.
