# HIBRAH — Role Handoff Log

Status: DRAFT operating control.
Last updated: 2026-11-12
Purpose: Preserve decisions, evidence, open questions, and next actions when work moves between roles.

## Handoff rules
1. The sender links the exact source files and decision IDs used.
2. Separate confirmed facts, current practice, provisional assumptions, hypotheses, recommendations, and unknowns.
3. Include rejected and on-hold directions when they could otherwise be accidentally revived.
4. The receiving role performs its own ROLE_PREFLIGHT. A handoff is not a substitute for source review.
5. The receiving role confirms what it received, identifies missing context, and does not silently alter Founder decisions.
6. Founder approval is required wherever the project rules reserve the decision to the Founder.
7. Record completion only after the receiving role acknowledges the handoff.

## Handoff entry template

### HO-YYYY-NNN — [Task / deliverable]
- Date:
- From role:
- To role:
- Project ID:
- Task objective:
- Requested output:
- Sources reviewed (exact paths and versions):
- Relevant Founder decision IDs and statuses:
- Confirmed facts:
- Current practice:
- Provisional assumptions / hypotheses:
- Recommendations (not approved):
- Rejected / on-hold directions:
- Work completed:
- Work not completed:
- Open questions / blockers:
- Approval required from Founder:
- Acceptance criteria:
- Receiver acknowledgement:
- Final status: DRAFT / ACCEPTED / BLOCKED / CLOSED

## Initial system issue record

### HO-2026-001 — Shared context reliability
- Date: 2026-11-12
- From: Founder / Founding Strategic & Operating Partner
- To: AI Engineer & Automation Architect (R7) — proposed role, not formally activated
- Status: OPEN — awaiting role review and Founder approval
- Problem: Role conversations do not reliably demonstrate retrieval of relevant prior discussions and decisions before work begins.
- Required investigation:
  1. Determine what each role conversation can actually access: current prompt, Project Sources, files, connectors, and prior conversations.
  2. Separate platform limitations from failure to inspect available sources.
  3. Run the acceptance test in ROLE_PREFLIGHT.md.
  4. Document observed results, not assumptions.
  5. Recommend the smallest viable fix; do not purchase, subscribe, or change integrations without Founder approval.
- Acceptance: Evidence-backed access matrix, test results, identified failure point, and a reversible implementation proposal.
