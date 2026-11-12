---
id: HIB-ROLE-R1-README
version: 0.1
status: DRAFT
owner: Founder
role_owner: R1
depends_on:
  - roles/R1/R1_charter.md
  - roles/R1/R1_operating_parameters.md
  - roles/R1/R1_decision_register.md
  - roles/R1/R1_30day_mandate.md
supersedes: null
---

# R1 Documentation Pack

## Read order

1. Read `R1_charter.md` for role purpose, responsibilities, authority, boundaries, and interfaces.
2. Read `R1_operating_parameters.md` before making decisions involving thresholds, budgets, timing, KPIs, or reporting cadence.
3. Read `R1_decision_register.md` to identify the exact status of decisions, proposals, validation items, and unknowns.
4. Read `R1_30day_mandate.md` for the current time-bound priorities.
5. Read the company-wide `OPERATING_WORKFLOW.md`, `ROLE_PREFLIGHT.md`, and `DECISION_LOG.md` for governance and source context.

## Status rules

- **Confirmed:** explicitly decided by the Founder. Check the source and scope; do not expand the decision beyond what was approved.
- **Proposed:** recommendation only. It is not an operating instruction or delegated authority.
- **To Validate:** evidence or a test is required. Do not treat it as established fact.
- **Unknown:** do not guess. Ask the Founder or responsible owner, or record the blocker.
- A document marked DRAFT or PROVISIONAL is not an approved role charter or policy, even if some individual statements inside it are Confirmed.

## Stable rule IDs

Every material rule has a permanent ID. Refer to rule IDs across documents instead of section numbers. Never reuse an ID for a different rule. If a rule is retired, preserve its ID and history.

## Change control

- Do not edit an approved file directly.
- Propose changes on a dedicated branch and open a Pull Request.
- Founder reviews and merges. No model or role may merge its own proposal.
- Update `CHANGELOG.md` and the decision register for material changes.
- Preserve approval evidence and the previous status when a rule changes.
- A decision or lesson not recorded in the shared project files must not be assumed to be known by another model.

## Repository governance transition

The Founder approved the target documentation architecture and branch/PR review convention. However, the current repository contains provisional/draft governance documents, including `OPERATING_WORKFLOW.md` and `ROLE_PREFLIGHT.md`. Therefore, the repository has not yet completed migration to a fully approved-only `main` baseline. Do not interpret the presence of a file on `main` as proof of approval until its own status and approval record have been checked. A separate repository-wide migration plan is required; this R1 package does not silently move or approve existing files.
