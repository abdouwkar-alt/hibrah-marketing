# HIBRAH — Mandatory Role Preflight

Status: DRAFT operating control — test before treating as effective.
Applies to: All HIBRAH roles and the Founding Strategic & Operating Partner.
Owner: Founder retains approval authority.

## Rule
Before substantive work begins, the role must inspect the relevant accessible project sources and retrieve relevant decisions. A role must not assume it can see every other conversation. If a relevant source is inaccessible, say so explicitly and request the missing material only when it affects correctness.

## Required preflight record
Complete this in the response or task handoff:

**ROLE PREFLIGHT**
- Role / role ID:
- Task:
- Date:
- Current project/workstream:

**1. Sources actually reviewed**
For each source, record filename, version/date if available, and relevant section. Do not list a source that was not opened and read.

**2. Relevant Founder decisions**
Record decision ID, exact status, and the effect on this task. Check DECISION_LOG.md and relevant approved project files.

**3. Relevant prior discussion**
- Discussion or decision retrieved:
- Where it was found:
- What was not accessible:
Do not imply full conversation-history access unless demonstrated.

**4. Conflicts / unknowns**
List conflicting statements, missing approvals, uncertain facts, and assumptions. Do not silently reconcile material conflicts.

**5. Scope and approval boundary**
State what can be done now and what requires Founder approval.

**6. Readiness**
- READY / BLOCKED
- Reason:

## Minimum checks by task type
- Content/marketing: brand context, current content direction, current sequence, approved wording, held topics, claims and publication restrictions.
- Creative/design: approved content brief, brand visual rules, actual reference assets, image-generation limitations, no invented product evidence.
- Operations/sales: current service and payment practice, unresolved policies, no unapproved promises.
- Finance: source data, period, assumptions, and explicit separation of actuals from estimates.
- AI/automation: source access, permission boundaries, tested behavior, failure modes, and no claims of integration without evidence.
- Independent review: inspect primary source material independently; do not approve work merely because another role says it is approved.

## Context retrieval test (acceptance test)
1. Founder places a harmless test decision in DECISION_LOG.md with a unique ID and status.
2. Start a fresh conversation for a different role in the same intended Project environment.
3. Ask the role to report the test ID, exact status, and source filename without pasting the decision into the prompt.
4. Ask it to distinguish a second item that exists only in an earlier conversation and is absent from all shared files.
5. Update the shared file and repeat to test whether the role sees the latest version.

**Pass criteria**
- Correctly retrieves the seeded decision and status.
- Names the actual source it read.
- Does not invent or claim knowledge of the conversation-only item.
- Reports stale/missing access honestly.

**Failure handling**
- Do not describe the system as having shared memory.
- Use an explicit handoff containing the missing context.
- Investigate Project Sources availability, connector permissions, and retrieval behavior before proposing paid integrations.

## Revision history
- 2026-11-12: Initial draft created for Founder review.
