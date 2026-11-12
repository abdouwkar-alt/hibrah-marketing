---
id: HIB-GOV-MIGRATION-PLAN
version: 0.1
status: DRAFT — Founder review required
owner: Founder
prepared_by: Founding Strategic & Operating Partner
prepared: 2026-11-12
baseline_branch: main
baseline_commit: 745beb4345b94395b6f057ee878af9c69716fb84
depends_on:
  - DECISION_LOG.md
  - OPERATING_WORKFLOW.md
  - ROLE_PREFLIGHT.md
  - roles/README.md
supersedes: null
---

# HIBRAH — Repository Governance Migration Plan

## 1. Purpose

Establish a safe repository model in which `main` contains only current Founder-approved operating documents and approved project records, while unapproved drafts remain available for review without being mistaken for active policy.

This plan is a proposal. It does not approve, move, delete, rename, or activate any project document. No repository migration is authorized by this file alone.

## 2. Desired repository model

### 2.1 `main` — approved baseline

- Default branch and source of truth for currently approved documents.
- Contains only approved/current operating instructions, approved role charters, approved policies, and authoritative decision records.
- A file's presence on `main` must mean that its current content has passed the required Founder approval.
- Decision and status registers may record proposals or unknowns, provided each entry is explicitly status-labelled. Their presence is approval of the recordkeeping mechanism, not approval of every entry.
- No draft role charter or provisional operating policy is promoted merely to make a path or index complete.

### 2.2 `drafts` — working and review baseline

- Long-lived, non-default branch for draft, provisional, proposed, and under-review project documents.
- Holds the current working versions of documents that are not yet approved.
- Draft PRs target `drafts`, not `main`.
- Draft files may retain their canonical paths on this branch (for example, `OPERATING_WORKFLOW.md` and `roles/R1/R1_charter.md`) to avoid needless path changes and broken internal references.
- `drafts` is not an approval signal and must never be used as the production/default source of truth.
- Changes to `main` should be periodically synchronized into `drafts` without overwriting newer draft work.

### 2.3 Archive checkpoint

Before any migration write to `main`, create a read-only preservation branch such as `archive/pre-migration-main-2026-11-12` pointing to the exact pre-migration `main` commit. This is a recovery checkpoint, not a replacement for Git history.

## 3. Promotion rule: drafts to main

Do not merge the long-lived `drafts` branch wholesale into `main`.

For each document or coherent package approved by the Founder:

1. Create a short-lived promotion branch from the latest `main`.
2. Copy or cherry-pick only the specifically approved file changes from `drafts`.
3. Update document metadata to the approved status/version and record the approval evidence.
4. Update the relevant decision record and changelog.
5. Open a PR targeting `main`, listing exact paths and approval scope.
6. Merge only after Founder review and confirmation.

A package may contain both approved and unapproved components. Promote only the approved components; keep the rest on `drafts`.

## 4. Initial migration inventory

The following items were verified on the current repository baseline or the existing R1 draft PR. This is a working inventory, not yet a complete recursive repository audit.

| Path | Current observed status | Proposed handling |
|---|---|---|
| `OPERATING_WORKFLOW.md` | PROVISIONAL — Founder review required | Keep on `drafts`; remove from `main` until explicitly approved |
| `ROLE_PREFLIGHT.md` | DRAFT operating control | Keep on `drafts`; remove from `main` until explicitly approved |
| `roles/README.md` | Role index containing proposed roles and draft governance references | Keep working index on `drafts`; replace on `main` only if a minimal approved index is genuinely needed |
| `roles/R1/R1_charter.md` | DRAFT; present on R1 PR branch, not current `main` | Keep on `drafts`; do not promote until substantive charter approval |
| `roles/R1/R1_operating_parameters.md` | DRAFT; present on R1 PR branch | Keep on `drafts`; proposed values remain inactive |
| `roles/R1/R1_decision_register.md` | Working register; present on R1 PR branch | Keep working R1 register on `drafts`; promote only approved records if needed |
| `roles/R1/R1_30day_mandate.md` | DRAFT; present on R1 PR branch | Keep on `drafts`; activate only after Founder approves the mandate |
| `roles/R1/README.md` | DRAFT guide; present on R1 PR branch | Keep on `drafts` |
| `CHANGELOG.md` | Draft file currently records an unmerged proposal; present on R1 PR branch | Maintain working changelog on `drafts`; `main` changelog records merged changes only |
| `DECISION_LOG.md` | Working Founder decision register; current main contains confirmed entries and status vocabulary | Retain on `main` as an authoritative decision record; promote the confirmed HIB-2026-004 entry without promoting draft role documents |
| `README.md` | Minimal repository landing page | Retain; update only if needed to explain branch roles and source-of-truth rules |

Before implementation, perform a complete recursive tree inventory and inspect every file's own status, references, and current use. Do not infer approval from filenames, directory location, commit history, or the fact that a file is currently on `main`.

## 5. Proposed implementation sequence

### Phase 0 — Freeze and verify

- Re-fetch the current default branch, commit SHA, open PRs, and complete repository tree.
- Confirm that the baseline has not moved since this plan was prepared.
- Record all current file paths, statuses, references, and external dependencies.
- Identify any automation, project instructions, or connected workflows that currently read from `main` or expect the provisional files at their present paths.
- Do not proceed if a dependency or required source is unknown and could break active work.

### Phase 1 — Preserve the baseline

- Create the archive checkpoint branch from the exact verified pre-migration `main` SHA.
- Verify that the checkpoint points to that exact SHA.
- Do not rewrite Git history or force-push any existing branch.

### Phase 2 — Establish the drafts branch

- Create `drafts` from the verified pre-migration baseline.
- Bring the existing R1 draft package into `drafts` through review; do not merge it into `main`.
- Keep the provisional workflow, preflight, role index, and all other unapproved working documents available on `drafts`.
- Retarget or replace the existing R1 PR only after confirming the new base branch and exact diff. Preserve the original PR/branch history and clearly mark any superseded PR.

### Phase 3 — Prepare the clean main baseline

- Create a migration branch from the unchanged `main`.
- Remove from the current `main` tree only files verified as unapproved and safely preserved on `drafts`.
- Keep `DECISION_LOG.md` and its confirmed decision records. Add HIB-2026-004 only if it is absent and its scope remains exactly the approved documentation architecture.
- Keep a minimal repository README with clear source-of-truth guidance.
- Do not add a replacement operating workflow or role policy unless the Founder has separately approved its content.
- Open a migration PR with a path-by-path removal/addition list and explicit preservation evidence.

### Phase 4 — Verify before merge

Required checks:
- Every removed file exists on `drafts` with matching content and history context.
- The archive branch points to the exact pre-migration `main` SHA.
- No confirmed Founder decision is lost from `DECISION_LOG.md`.
- No approved current file is removed.
- No broken internal links or references remain in the cleaned `main` tree.
- The default branch remains `main`.
- The PR does not include R1 charter approval or role activation.
- The resulting `main` tree contains no unapproved policy or role document.

### Phase 5 — Operate the two-branch model

- New drafts and revisions are developed on `drafts` or short-lived branches based on it.
- Draft PRs target `drafts`.
- Founder-approved changes are selectively promoted to `main` through a promotion PR.
- Never merge `drafts` wholesale into `main`.
- Keep branch protections and required review settings aligned with this model where repository settings permit.
- Periodically sync the latest `main` baseline into `drafts`, resolving conflicts deliberately.

## 6. Treatment of existing R1 PR #1

PR #1 currently targets `main` and contains eight commits: six added files and two updated files. It remains unmerged and its substantive R1 content remains DRAFT.

Proposed handling after Phase 2:
- Retarget PR #1 to `drafts` if GitHub permits and the resulting diff is exactly the intended R1 package; or
- Close it as superseded only after an equivalent draft-preserving PR is open and verified.

Do not merge PR #1 into `main` under the current approval statuses.

## 7. Risks and controls

| Risk | Control |
|---|---|
| Losing draft content during cleanup | Archive checkpoint + verified copy on `drafts` before any removal |
| Accidentally approving a draft by merging it | Separate `drafts` and `main`; selective promotion only |
| Breaking links or project workflows | Full tree/reference/dependency audit before changes |
| Losing Founder decisions | Preserve `DECISION_LOG.md`; compare entries before and after |
| Draft branch drifting from approved baseline | Periodic deliberate sync from `main` to `drafts` |
| Confusing branch presence with approval | Explicit branch policy and per-file status metadata |
| Overcomplicating routine work | Use one long-lived `drafts` branch and short-lived promotion branches only |

## 8. Decisions required before implementation

Founder confirmation is required for:
1. Adopting the `main` + `drafts` branch model described above.
2. Authorizing creation of the archive checkpoint and `drafts` branch.
3. Authorizing the migration PR that removes unapproved documents from the current `main` tree after preservation and verification.
4. Confirming that `DECISION_LOG.md` remains on `main` as an approved recordkeeping register even though individual entries retain different statuses.

No role charter, workflow, preflight control, or proposed operating parameter is approved by confirming this migration plan.

## 9. Completion criteria

Migration is complete only when:
- The archive checkpoint is verified.
- `drafts` contains the complete working set and the R1 draft package.
- `main` contains only approved/current policy documents and approved recordkeeping mechanisms.
- The default branch is still `main`.
- All draft PRs target `drafts`; all promotion PRs target `main`.
- The R1 package remains DRAFT until separately approved.
- A repository audit confirms no missing files, broken references, or unrecorded decisions.

## Change control

This plan remains DRAFT until the Founder approves the migration approach. Any implementation must be performed through separately reviewed PRs. No force-push, history rewrite, or destructive deletion is permitted.
