# HIBRAH — Master Content Batch Execution Prompt

**Status:** DRAFT reusable execution prompt  
**Repository:** `abdouwkar-alt/hibrah-marketing`  
**Functions involved:** Marketing and Creative  
**Founder:** Final decision-maker and approver

## YOUR MISSION

Execute the next HIBRAH content batch end-to-end through the Marketing and Creative functions. Do not merely advise. Read the repository, establish the controlling content plan, research genuine gaps, audit available assets, produce copy and creative outputs, save the work to GitHub and return exact links for Founder review.

The goal is to make HIBRAH publish consistently without requiring the Founder to manually coordinate every research, writing, asset and documentation step.

## STEP 1 — READ THE SOURCE OF TRUTH

Read from `main`:
- `README.md`
- `BRAND_FOUNDATION.md`
- `OPERATING_WORKFLOW.md`
- `ORGANIZATIONAL_STRUCTURE.md`
- `DECISION_LOG.md`

Then inspect `drafts`, current Project Sources and all relevant content files, including the monthly plan, approved sequence, prior caption batch, asset archive and any later Founder-confirmed decisions.

Use the established source hierarchy. Do not infer approval from file recency alone.

### Mandatory conflict check
The main-branch Decision Log currently records an eight-topic sequence, while earlier project work may contain a later Founder-approved 13-post plan and a 28-day story plan. The monthly plan on `drafts` is labelled DRAFT. Locate the latest explicit Founder confirmation and identify exactly what it approved: topics, copy, design, calendar, stories, publication or scheduling. These are separate approval scopes.

If the exact controlling plan cannot be verified:
- Do not claim that the eight-post draft or 13-post plan is approved.
- Do not invent a missing sequence.
- Record the conflict and ask one concise Founder question.
- Continue research, asset inventory, non-conflicting copy/creative preparation and reusable templates while awaiting that answer.

## STEP 2 — MARKETING EXECUTION

Marketing must:
1. Confirm the immediate objective for this batch and the primary audience.
2. Review existing market, customer and competitor evidence before new research.
3. Confirm content pillars, post sequence, content formats, cadence and channel roles.
4. Identify any topic overlap, unsupported claims or weak content angles.
5. Create a practical calendar and detailed creative brief for each post.
6. Define meaningful measures, prioritising qualified inquiries and commercial outcomes over vanity metrics.
7. Save Marketing outputs under `marketing/work/YYYY-MM-DD-content-batch/`.

Marketing owns the marketing brief and strategy. It does not take over Creative's production accountability.

## STEP 3 — CREATIVE EXECUTION

Creative must:
1. Audit the actual 21 Founder-supplied images in the Canva design / Project Sources / repository. Verify access; do not claim to have inspected an image if only its filename or thumbnail metadata is visible.
2. Match existing images to the posts where they genuinely fit.
3. Identify missing visuals and source suitable external assets.
4. Verify commercial-use permission/terms for every external image. Save the image, source URL, creator, licence/terms and access date in GitHub.
5. Use the official logo file only. Never recreate, redraw, approximate or AI-generate the logo.
6. Respect the no-AI-image-generation instruction unless the Founder explicitly changes it.
7. Produce the caption, on-image text, design specification and final visual files for each post where the tools and source assets allow.
8. Save Creative outputs under `creative/work/YYYY-MM-DD-content-batch/` and images under a structured `assets/content/YYYY-MM/` directory.

Do not stop if one image is inaccessible. Continue with the accessible images and all other content. State the precise blocker and produce the remaining publish-ready materials.

## STEP 4 — CROSS-FUNCTIONAL HANDOFF

- Marketing provides the approved strategic brief and calendar to Creative.
- Creative returns concepts, copy, visual assets, factual/technical questions and production status to Marketing.
- Marketing checks strategic alignment and channel purpose.
- Creative remains accountable for creative accuracy and quality.
- Marketing remains accountable for marketing strategy and measurement.
- The GM may coordinate dependencies if needed, but is not a mandatory gatekeeper for Founder-to-function work.
- Founder retains final approval for strategic decisions and customer-facing publication where required.

## STEP 5 — GITHUB DELIVERY

- Create a dated branch from `drafts`, e.g. `content/YYYY-MM-DD-batch-01`.
- Preserve existing approved files; do not overwrite them.
- Save all outputs, source images and asset registers in clear folders.
- Commit meaningful changes.
- Open a pull request targeting `drafts`, not `main`, unless the Founder explicitly directs a different target.
- Do not merge the PR.
- Do not write to `main` without explicit Founder approval and the established selective-promotion process.
- Return exact GitHub file links, branch and PR URL.

## STEP 6 — PUBLISHING BOUNDARY

**Do not publish or schedule anything.** Prepare the work for Founder review. Publication/scheduling is a separate action and requires explicit approval of the exact final asset, caption, account, date and time. Never interpret a general request to prepare a content plan as permission to publish.

Do not spend on paid ads without explicit approval and an approved budget.

## OUTPUT CHECKLIST

The batch is complete only when the repository contains, as applicable:
- Marketing brief and strategy.
- Content calendar and post-level creative briefs.
- Final captions and on-image copy.
- Asset inventory of the 21 existing images.
- External asset register with licence/permission details.
- Final export files for each completed post/slide.
- Measurement plan.
- Clear status for each post: BLOCKED / IN PROGRESS / READY FOR FOUNDER REVIEW / APPROVED.
- PR with all deliverables.

## FINAL REPORT FORMAT

Return a short, factual report:
1. **Plan used:** exact controlling file/decision and approval scope.
2. **Work completed:** deliverables and exact GitHub paths.
3. **Images:** existing assets used; external images added and their source/licence.
4. **Ready for review:** post IDs, captions and final visual paths.
5. **Blocked:** only specific blockers and why.
6. **Founder decisions needed:** maximum one or two genuinely essential decisions.
7. **GitHub:** branch, commit and PR URL.

No generic status essay. No fabricated completion. Execute and save the actual work.
