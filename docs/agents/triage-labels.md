# Triage Labels

This repo uses a security-review-oriented label vocabulary for agent triage and issue follow-up.

## Repository labels

| Label | Use |
| ----- | --- |
| `security` | Security finding, hardening task, or security regression |
| `bug` | Confirmed defect or behavior regression |
| `high` | High-severity or high-priority issue |
| `medium` | Medium-severity or medium-priority issue |
| `low` | Low-severity or low-priority issue |
| `validated` | Finding has been checked against the code or reproduced |

## Mapping from mattpocock/skills roles

The skills speak in terms of canonical triage roles. Use the repository labels above rather than creating duplicate workflow labels unless the user asks to introduce a separate issue-state vocabulary.

| Role in mattpocock/skills | Label guidance in this repo |
| ------------------------- | --------------------------- |
| `needs-triage` | Use `security` for security-review findings, plus an initial severity label when known |
| `needs-info` | Leave a comment requesting the missing information; do not apply a dedicated label unless one exists |
| `ready-for-agent` | Use `validated` once the finding is checked and ready for implementation |
| `ready-for-human` | Leave a comment explaining why human judgment is needed; apply severity if known |
| `wontfix` | Close with a comment; do not create a `wontfix` label unless the repo adds one |

For security findings, prefer the combination `security` + one of `high`, `medium`, or `low`; add `bug` when the finding is a confirmed implementation defect and `validated` after code or runtime verification.
