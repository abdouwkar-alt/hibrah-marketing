# HIBRAH Marketing — Approved Baseline

This repository uses a two-branch governance model confirmed by the Founder on 2026-11-12.

## Branch roles

- **`main`** — approved/current project documents and authoritative, status-labelled decision records.
- **`drafts`** — working material: drafts, provisional documents, proposals, research under review, and role packages not yet approved.
- **`archive/pre-migration-main-2026-11-12`** — recovery checkpoint for the exact pre-migration `main` baseline. The branch is not currently protected; do not move its reference, and configure branch protection manually if repository settings are to enforce this.

A document's presence on `drafts` is not approval. A status label inside a decision register controls the scope and meaning of that individual entry.

## Current approved records on `main`

- [Founder Decision Log](DECISION_LOG.md) — authoritative register; each entry retains its own approval status.
- [Confirmed Content Sequence — 2026-11-12](content/2026-11-12-approved-content-sequence.md) — approved topic/order decisions only. It does not approve copy, design, publication, or scheduling.

## Current working references on `drafts`

These documents remain unapproved or provisional and must not be treated as active policy:

- [Operating Workflow](https://github.com/abdouwkar-alt/hibrah-marketing/blob/drafts/OPERATING_WORKFLOW.md) — PROVISIONAL; Founder review required.
- [Role Preflight](https://github.com/abdouwkar-alt/hibrah-marketing/blob/drafts/ROLE_PREFLIGHT.md) — DRAFT operating control.
- [HIBRAH Brand & Business Context](https://github.com/abdouwkar-alt/hibrah-marketing/blob/drafts/HIBRAH_CONTEXT.md) — working reference.
- [Role Charter Index](https://github.com/abdouwkar-alt/hibrah-marketing/blob/drafts/roles/README.md) — role charters remain DRAFT unless separately approved.
- [R1 Documentation Package](https://github.com/abdouwkar-alt/hibrah-marketing/tree/drafts/roles/R1) — documentation architecture approved; substantive charter and proposed rules remain DRAFT.
- [Monthly Content Plan](https://github.com/abdouwkar-alt/hibrah-marketing/blob/drafts/content/HIBRAH_Monthly_Content_Plan_2026-11-12.md) — DRAFT; not scheduled or published.
- [Repository Migration Plan](https://github.com/abdouwkar-alt/hibrah-marketing/blob/drafts/REPOSITORY_MIGRATION_PLAN.md) — Founder-approved implementation plan; repository cleanup remains subject to the migration PR review.

The full working project update and research package is also retained under `HIBRAH_PROJECT_UPDATE_2026-11-11/` on `drafts`.

## Change and promotion rules

1. Develop draft work on `drafts` or a short-lived branch based on it; open draft PRs to `drafts`.
2. When the Founder approves a specific document or package, create a short-lived promotion branch from the latest `main`.
3. Promote only the exact approved files and scope; update status/version metadata and the decision record.
4. Open a PR to `main` for Founder review.
5. Never merge `drafts` wholesale into `main`.

## Approval boundary

This repository structure does not approve any operating workflow, role charter, proposed operating parameter, market-research conclusion, or content plan unless that material has its own explicit approval record.
