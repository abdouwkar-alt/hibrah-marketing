# HIBRAH Content Execution Prompts

**Status:** DRAFT — reusable working prompts; not a new policy or role appointment.  
**Branch:** `drafts`  
**Purpose:** Let Claude execute the Marketing and Creative workstreams against the current repository, prepare publish-ready content, and save all outputs back to GitHub for Founder review.

## Files

1. [Marketing Role Prompt](MARKETING_ROLE_PROMPT.md) — research, audience, objectives, content strategy, calendar, performance measures and content briefs.
2. [Creative Role Prompt](CREATIVE_ROLE_PROMPT.md) — creative research, asset audit, copy development, design briefs, production, quality control and asset archiving.
3. [Master Execution Prompt](MASTER_EXECUTION_PROMPT.md) — runs both functions in the correct order and coordinates their handoff.

## How to use

- Start Claude with repository access to `abdouwkar-alt/hibrah-marketing`.
- Paste the Master Execution Prompt, or use one role prompt when assigning a single function.
- Tell Claude the immediate task, for example: `Execute the next publishable HIBRAH content batch. Start with the next post due today.`
- Claude must read the current approved documents on `main`, inspect working material on `drafts`, and inspect the actual available image assets before drafting.
- Claude saves its work to a clearly named working folder/branch and opens a PR targeting `drafts` unless the Founder explicitly instructs otherwise.
- Founder reviews and approves customer-facing copy and final creative before anything is published or scheduled.

## Important source-control rule

The current `main` Decision Log records an eight-topic confirmed sequence and a separate eight-post monthly plan on `drafts` marked DRAFT. Earlier project work may contain a later 13-post plan. Claude must locate and compare the actual dated Founder-confirmed source before deciding which plan is current. It must not silently treat a draft as approved, silently downgrade a later Founder decision, or invent missing posts. If the controlling source cannot be identified, it should report the exact conflict and proceed with non-conflicting research and production preparation while asking one concise decision question.

## Execution standard

Do the work, save the deliverables, report exact GitHub paths and PR links, and identify only genuine blockers. Do not return a generic strategy essay or a list of tasks for the Founder to perform.
