# HIBRAH — Founder Decision Log

Status: Authoritative Founder decision register. The recordkeeping mechanism was confirmed by the Founder on 2026-11-12; each entry retains its own status and scope.
Owner: Founder
Last updated: 2026-11-12

## Status vocabulary
- CONFIRMED: explicitly approved by the Founder.
- CURRENT PRACTICE: currently followed, but not necessarily a permanent policy.
- PROVISIONAL: working assumption; reconfirm before external commitment.
- HYPOTHESIS: unvalidated idea.
- RECOMMENDATION: proposed by an HIBRAH role; not approved.
- ON HOLD: intentionally paused; do not reactivate without Founder direction.
- REJECTED: explicitly declined.
- OUTDATED: superseded; retain for history, do not use as current direction.
- UNKNOWN: not established in accessible sources.

## Decision records

### HIB-2026-001 — Content editorial direction
- Date: 2026-11-05 (recorded from prior confirmed project context)
- Status: CONFIRMED
- Decision: Fabric is the central content subject; the suit is the product built around it. Commercial editorial emphasis is wool, including English and Italian wool. Turkish fabric remains available but is not the permanent editorial centre; it may be featured for specific price-led moments.
- Scope: Content strategy and editorial prioritisation.
- Source: Founder-confirmed project context / prior project conversation summary.
- Supersedes: Earlier styling-first content direction, where conflicting.

### HIB-2026-002 — Eight-post content sequence
- Date: 2026-11-12
- Status: CONFIRMED for topics/order only; not approval to design, publish, or schedule.
- Sequence:
  1. Navy versus a grey shade — wording requires correction; exact shade name UNKNOWN. Do not call it charcoal/الفحمي without Founder confirmation.
  2. Jacket design details — lapel, shoulder, jacket length, sleeve length. Avoid implying jacket details are chosen before fabric.
  3. Fabric first, then the suit.
  4. One suit, two looks.
  5. Same suit, three ties. Use the term "كرافته".
  6. How to assess jacket fit.
  7. Trouser length — proposed topic; approval status must be checked against the latest Founder message before production.
  8. Wool suit care — proposed topic; approval status must be checked against the latest Founder message before production.
- On hold: Previous topic "How to assess suit fabric" and previous topic "The last jacket button". Do not reintroduce without Founder direction.
- Source: Founder-confirmed sequence recorded in project conversation on 2026-11-12.
- Constraint: Topic/order approval is not copy/design/publication approval.

### HIB-2026-003 — Role system status
- Date: 2026-11-12
- Status: CONFIRMED
- Decision: The eight functional role documents remain DRAFT until explicitly approved by the Founder. Role activation does not imply hiring or staffing. Founder retains approval authority.
- Source: HIBRAH project operating instructions.

### HIB-2026-004 — R1 documentation architecture
- Date: 2026-11-12
- Status: CONFIRMED
- Decision: Adopt a modular R1 documentation package consisting of a stable role charter, adjustable operating parameters, a centralized decision/unknown register, a time-bound 30-day mandate, a README, and CHANGELOG. Use stable rule IDs, front matter metadata, and branch/PR review. The approval covers the documentation architecture only; it does not approve the substantive R1 role charter, its Proposed rules, or activate R1 authority.
- Scope: R1 documentation structure and change-control method.
- Source: Founder approval in the R1 role-establishment conversation on 2026-11-12.
- Supersedes: None.
- Owner: Founder.
- Follow-up required: The documentation package is preserved on `drafts`; separately review and approve the substantive R1 charter and any Proposed operating rules.

### HIB-2026-005 — Repository governance and migration
- Date: 2026-11-12
- Status: CONFIRMED
- Decision: Adopt `main` as the approved/current baseline and `drafts` as the long-lived working branch for draft, provisional, proposed, and under-review material. Preserve the exact pre-migration `main` commit in an archive checkpoint before cleanup. Draft PRs target `drafts`; approved changes are selectively promoted through PRs to `main`. Never merge `drafts` wholesale into `main`. Retain this `DECISION_LOG.md` on `main` as the authoritative status-labelled Founder decision register.
- Scope: Repository governance, branch roles, preservation, and selective promotion. This decision does not approve the substantive content of any draft document or activate any role.
- Source: Founder approval in the HIBRAH project conversation on 2026-11-12.
- Supersedes: None.
- Owner: Founder.
- Follow-up required: Review and merge the repository migration PR after preservation and path-by-path verification.


### HIB-2026-006 — Approval of HIBRAH's three foundations and current operating model
- Date: 2026-11-12
- Status: CONFIRMED / APPROVED
- Decision: Approve the current versions of `BRAND_FOUNDATION.md`, `OPERATING_WORKFLOW.md`, and `ORGANIZATIONAL_STRUCTURE.md` as HIBRAH's three foundational operating references.
- Brand Foundation: the common reference for brand identity, positioning, customer, product/service, values, voice and visual direction. Functions use it; they do not independently redefine it.
- Operating Workflow: the approved practical workflow for goal-setting, strategy approval, GM priority-setting, function-level execution, the Sales-owned customer journey through production coordination and delivery, cross-functional information exchange, Founder technical review, reporting and improvement.
- Organizational Structure: Founder; GM; Marketing; Creative; Sales & Customer Relations; Finance & Accounting; Development & R&D. The GM is currently performed by the marketer, while the GM and Marketing responsibilities remain distinct.
- Authority: the Founder retains final decision-making authority and currently performs direct technical review as a temporary control. Functions may produce and act within approved plans and responsibilities. The GM sets priorities within approved goals/plans and coordinates execution.
- Sales scope: includes all customer contact and the complete customer journey from inquiry through appointment, measurement, order, workshop/tailor coordination, fitting, adjustments, delivery and post-sale relationship. No separate Customer Service or Production Coordination function is established at this stage.
- Finance: financial review of pricing and discounts becomes operational when customer transactions and reliable cost/revenue data begin; Founder retains final authority unless delegation is explicitly recorded.
- Reporting: each function prepares its own report; GM collects and consolidates; Founder currently determines review-session timing. Automation may be introduced later when the workflow is stable.
- Scope limitation: approval applies to the three foundational documents and current functional model. It does not imply hiring, legal incorporation, verified technical quality, validated commercial results, automatic approval of the monthly content plan, or approval of legacy eight-role charters.
- Source: Founder instruction in the HIBRAH project conversation, 2026-11-12.
- Supersedes: Conflicting provisional workflow and proposed organizational/role architecture on the `drafts` branch, including the earlier eight-role model, wherever inconsistent with these approved foundations.
- Owner: Founder.
- Follow-up: Use these three documents as the baseline for immediate work; revise when actual operating issues emerge.

## New decision entry template
### HIB-YYYY-NNN — [Short title]
- Date:
- Status:
- Decision (exact, concise):
- Scope:
- Rationale (if supplied):
- Source / conversation / file:
- Supersedes:
- Owner:
- Follow-up required:


### HIB-2026-007 — Classic menswear image-library source shortlist and validation workflow
- Date: 2026-11-13
- Status: CONFIRMED
- Decision: Approve FIT SPARC Digital, The Metropolitan Museum of Art, Smithsonian Open Access, Wikimedia Commons, and the Library of Congress as the initial research-discovery source shortlist for the Classic Menswear Reference Library. Approve the staged validation workflow: search by category; verify that relevant items actually exist; review item-level rights and intended-use permissions; then run a small controlled retrieval test before scaling.
- Scope: Approval is for research discovery and controlled validation only. It does not approve every image, grant commercial-use rights, authorize bulk downloading, or establish that all five sources are technically automatable. FIT SPARC is primarily a specialist manual research source unless its access and rights are separately validated.
- Rationale: The shortlist has demonstrated relevant menswear/fashion holdings, unlike the previous generic search test whose downloaded results were mostly irrelevant.
- Source: Founder approval in project conversation, 2026-11-13; source examples and research presented in the same conversation.
- Supersedes: None.
- Owner: Creative for visual research and relevance review; Development & R&D for technical access, retrieval and automation tests. Founder retains final approval of system changes.
- Follow-up required: Validate five initial categories with specific relevant item records, source URLs, image availability, license/rights, and quality before implementing or scaling automated downloads.
