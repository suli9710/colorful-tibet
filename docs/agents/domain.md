# Domain Docs

How engineering skills should consume this repo's domain documentation when exploring the codebase.

## Layout

This is a single-context repo.

- Root domain context: `CONTEXT.md`
- Architecture decisions: `docs/adr/`
- Agent configuration docs: `docs/agents/`

## Before exploring, read these

- Read `CONTEXT.md` at the repo root if it exists.
- Read relevant ADRs under `docs/adr/` if they exist and touch the area being changed.
- If these files do not exist, proceed silently. Do not block implementation just because the domain docs have not been created yet.

## Use the glossary's vocabulary

When output names a domain concept in an issue title, refactor proposal, hypothesis, or test name, use the term as defined in `CONTEXT.md`.

If the concept is missing from the glossary, either avoid inventing new terminology or note that `/grill-with-docs` should fill the gap.

## Flag ADR conflicts

If a proposed change contradicts an existing ADR, surface the conflict explicitly before implementing it.
