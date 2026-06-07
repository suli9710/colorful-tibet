# Security Policy

## Supported Versions

Security fixes are applied to the default branch and to the latest tagged release when releases are available. Older snapshots, experimental branches, and local deployment examples are not supported unless a maintainer explicitly marks them as maintained.

## Reporting a Vulnerability

Do not open a public issue with exploit details, credentials, tokens, private URLs, logs containing secrets, or proof-of-concept payloads.

Preferred reporting path:

1. Use GitHub's private vulnerability reporting or Security Advisory flow for this repository.
2. If private reporting is unavailable, open a minimal public issue that only asks for a private contact channel. Do not include technical details in that issue.

Please include, in the private report:

- Affected component, route, endpoint, workflow, or deployment file.
- Reproduction steps and expected versus actual behavior.
- Impact assessment, including whether authentication is required.
- Any relevant version, commit SHA, configuration profile, and environment details.
- Whether secrets, personal data, payment data, or production infrastructure may be exposed.

## Response Targets

These are best-effort targets for maintainers:

- Initial acknowledgement: within 3 business days.
- Triage and severity assessment: within 7 business days.
- Remediation plan for confirmed high-impact issues: within 14 business days.
- Public disclosure: after a fix or mitigation is available, coordinated with the reporter when possible.

## Secret Exposure

If a token, password, API key, private key, payment secret, or signing secret is exposed:

1. Revoke or rotate the secret immediately.
2. Remove the secret from the repository and deployment environment.
3. Audit recent access logs for suspicious use.
4. Treat Git history as compromised unless it has been scrubbed and all affected secrets have been rotated.

## Dependency Vulnerabilities

Dependency issues should include the package name, ecosystem, installed version, fixed version, CVE or advisory link, and whether the vulnerable code path is reachable in this project. Automated dependency PRs should pass the backend, frontend, Scrapler, container, and security scanning checks before merge.

## Deployment Safety

Production deployments must use the production profile and must not enable local-only settings such as insecure cookies, weak secret mode, mock payment callbacks, or unauthenticated internal services. If a production startup safety check fails, treat it as a deployment blocker rather than bypassing it.
