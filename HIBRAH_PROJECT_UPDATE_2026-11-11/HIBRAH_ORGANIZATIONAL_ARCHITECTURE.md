---
Approval status: NOT APPROVED
Confidentiality: INTERNAL
Document ID: HIBRAH-ORG-001
Document title: HIBRAH ORGANIZATIONAL AND FUNCTIONAL ARCHITECTURE
Prepared: 2026-11-09
Status: DRAFT --- FOR FOUNDER REVIEW
Version: 0.1
---

# HIBRAH ORGANIZATIONAL AND FUNCTIONAL ARCHITECTURE

## 1. Design purpose

This architecture defines the functions HIBRAH needs to operate and
develop. It is a functional accountability design, not a recommendation
to hire eight employees immediately.

**Founder authority:** the Founder remains the final decision-maker.
Functional owners prepare, execute, monitor, and recommend within
approved boundaries.

**Design principle:** separate accountability even when one person
temporarily performs multiple functions.

## 2. Proposed structure

``` text
Founder / Managing Owner
│
├── Marketing & Growth
├── Content & Creative
├── Business Operations & Administration
├── Sales & Customer Relations
├── Operations & Customer Experience
├── Finance & Business Control
├── AI Engineering & Automation
└── Independent Strategy & Quality Review
```

The eight functions report to the Founder for governance. In a larger
organization, some functions may report operationally to a general
manager, but no such layer is justified at the current stage.

## 3. Functional independence and safeguards

-   Marketing owns demand strategy and acquisition; Sales owns lead
    conversion and customer relationships.
-   Content owns editorial/creative quality; Administration owns
    calendar, records, and publishing coordination.
-   Operations owns fulfillment and service consistency; Sales cannot
    promise what Operations has not confirmed.
-   Finance owns financial analysis; it does not unilaterally set market
    positioning or product policy.
-   AI Engineering owns technical evaluation and implementation; it does
    not choose technology without a defined business case.
-   Independent Review challenges all functions and cannot approve its
    own work.
-   Founder approves strategy, budgets, prices, brand decisions,
    customer policies, and major commitments.

## 4. Role specifications

### Role 1 --- Marketing & Growth Director

**Purpose:** create evidence-led demand and sustainable customer
acquisition aligned with HIBRAH's position, operational capacity, and
cash constraints.

**Scope and core duties** - research customers, competitors, market
categories, and buying behaviour; - define segments, positioning
hypotheses, and go-to-market priorities; - design acquisition and
retention strategy; - plan organic and paid channel experiments; -
analyze inquiry quality, conversion, CAC, and campaign economics; -
provide market evidence for pricing discussions; - evaluate marketing
outputs and recommend changes.

**Deliverables** - market/competitor research with sources and
confidence levels; - segment and positioning hypotheses with validation
plan; - channel strategy and campaign briefs; - experiment plans with
budgets, success measures, and stop conditions; - weekly/monthly growth
reports; - customer acquisition and retention analysis.

**KPIs (baseline first; no targets assigned)** - qualified inquiries by
channel; - consultation-to-quote and quote-to-order conversion; - CAC
per new paying customer; - customer acquisition contribution margin; -
referral and repeat-order share; - experiment learning rate and evidence
quality.

**Inputs required:** customer/lead records, order outcomes, price list,
unit economics, workshop capacity, service geography, brand rules,
channel insights.

**Decision authority:** recommend segments, channels, campaign designs,
tests, and allocation within an approved budget.

**Limits:** cannot approve budget increases, final prices, claims,
customer promises, service changes, or capacity assumptions. Cannot
treat reach/engagement as sales.

**Dependencies:** Content, Sales, Finance, Operations, Administration,
Founder.

**Reporting:** reports to Founder; receives performance data from
Administration and Sales.

**Evaluation criteria:** evidence quality, qualified demand, efficient
learning, conversion contribution, alignment with capacity and cash.

**Failure modes:** vanity metrics; generic segments; untested competitor
claims; increasing spend before operational readiness; conflating
Marketing with Sales.

**Founder approval required:** positioning changes, campaign budget,
public offer, price communication policy, market entry, material
targeting changes.

### Role 2 --- Content & Creative Director

**Purpose:** translate approved brand direction and audience needs into
accurate, useful, distinctive content and creative assets.

**Scope and core duties** - content/editorial strategy within approved
positioning; - editorial calendar and topic selection; - research and
verification of technical claims; - copywriting, reels, scripts,
storytelling, and visual direction; - campaign concepts and production
briefs; - creative quality control and content performance learning; -
maintain topic/hook/format continuity.

**Deliverables** - content briefs and calendars; - complete post
packages following the Content Writer Brief; - scripts and shot lists; -
design/photography briefs; - research notes and source register; -
content review scores and handoffs.

**KPIs** - factual accuracy and correction rate; - content
usefulness/quality review scores; - on-time approved deliverables; -
content-to-qualified-inquiry contribution where attributable; -
production feasibility and reuse efficiency.

**Inputs:** approved brand context, customer questions, current product
facts, real assets, editorial calendar, channel data.

**Decision authority:** editorial angle, draft copy, creative concept,
format proposal within the approved brand.

**Limits:** cannot change positioning, product claims, public prices,
publishing policy, or publish/schedule without approval. Cannot invent
real process evidence.

**Dependencies:** Marketing, Sales, Operations, Administration, Founder.

**Reporting:** to Founder; coordinates with Marketing for campaign
priorities and Administration for calendar/assets.

**Evaluation:** accuracy, usefulness, brand consistency, execution
feasibility, audience response.

**Failure modes:** generic AI copy; unsupported fabric claims; aesthetic
polish without commercial purpose; repeated hooks; treating draft as
approved.

**Founder approval required:** new brand voice, claims, campaign concept
materially affecting identity, publication.

### Role 3 --- Business Operations & Administration Manager

**Purpose:** keep business information, records, calendars, and
coordination reliable and retrievable.

**Scope and core duties** - administer business records and document
control; - maintain CRM/lead/order records and data quality; - manage
task/content calendars and publishing coordination; - maintain
Excel/Google Sheets and reporting inputs; - organize inquiries for
handoff to Sales; - track deadlines, approvals, document versions, and
action items; - produce routine operational reports.

**Deliverables** - clean CRM and order register; - content/task
calendars; - weekly status and exception reports; - version-controlled
files and decision logs; - data quality checks; - administrative SOPs
and records.

**KPIs** - record completeness and error rate; - timeliness of
reporting; - missing-field rate; - overdue task/approval rate; -
successful retrieval/version accuracy; - inquiry handoff time and
completeness.

**Inputs:** records from Sales, Operations, Finance, Marketing, Content,
and Founder.

**Decision authority:** administrative formats, record hygiene,
reminders, and workflow coordination within approved policy.

**Limits:** cannot qualify or convert leads in place of Sales; cannot
change customer commitments, prices, policies, or source facts.

**Dependencies:** all functions; especially Sales and Operations.

**Reporting:** to Founder; acts as system coordinator, not commercial
owner.

**Evaluation:** reliable data, low friction, timely handoffs, traceable
decisions.

**Failure modes:** data entry without validation; becoming an
unaccountable inbox owner; duplicate records; undocumented policy
changes.

**Founder approval required:** CRM policy, access permissions, retention
rules, major workflow changes, external communications policy.

### Role 4 --- Sales & Customer Relations Manager

**Purpose:** convert suitable inquiries into correctly specified orders
and build trust through accurate communication and follow-through.

**Scope and core duties** - respond to inquiries under an approved
service standard; - qualify need, use case, location, fit requirements,
and buying readiness; - conduct consultation and explain actual
options; - prepare quotes using current approved price data; - manage
follow-up, objections, and conversion; - maintain customer relationships
and feedback; - record loss reasons, referrals, and repeat demand.

**Deliverables** - qualified lead pipeline; - consultation and quote
records; - follow-up queue; - objection/loss analysis; - order handoff
package; - customer feedback and relationship report.

**KPIs** - first-response time; - qualified lead rate; - consultation
show rate; - quote-to-order conversion; - follow-up completion; - order
value and contribution by segment; - customer satisfaction/complaint
outcomes; - repeat and referral rates.

**Inputs:** inquiry records, approved service/price policy, operational
capacity, customer journey, product catalogue.

**Decision authority:** qualify leads, explain approved options, follow
up, and quote within the current approved price/policy.

**Limits:** cannot change prices, promise delivery, approve
exceptions/refunds/remakes, or confirm production capacity without
Operations/Founder approval.

**Dependencies:** Administration, Marketing, Content, Finance,
Operations.

**Reporting:** to Founder; administrative intake is provided by
Administration.

**Evaluation:** conversion quality, accuracy, relationship quality,
complete handoffs, low avoidable complaint rate.

**Failure modes:** treating all inquiries as qualified; overselling;
inconsistent follow-up; unrecorded discounts; promises beyond capacity.

**Founder approval required:** exceptions, discounts, new terms,
sensitive complaints, customer compensation, major commercial
commitments.

### Role 5 --- Operations & Customer Experience Manager

**Purpose:** turn confirmed customer orders into correctly produced,
inspected, fitted, and delivered garments while protecting service
consistency.

**Scope and core duties** - own customer journey after order
acceptance; - coordinate workshop job cards and production milestones; -
confirm operational feasibility and capacity; - track materials,
production, fitting, adjustments, and delivery; - inspect finished work
against agreed specifications; - manage rework, defects, complaints, and
service recovery; - evaluate workshop performance and operational
bottlenecks.

**Deliverables** - order production pack; - workshop coordination log; -
production status report; - fitting/adjustment records; - QC checklist
and defect/rework register; - delivery and service recovery records; -
capacity and supplier scorecards.

**KPIs** - on-time completion against internally agreed dates; -
first-pass QC acceptance; - defect/rework rate and cost; -
fit/adjustment incidence; - production cycle time; - workshop
reliability; - customer issue resolution time; - capacity utilization
(once measured).

**Inputs:** approved order specification, measurements, customer
commitments, workshop terms, material availability, cash authorization.

**Decision authority:** schedule work within approved capacity,
stop/flag nonconforming work, coordinate rework within approved policy.

**Limits:** cannot promise customer delivery dates or quality guarantees
without verified basis; cannot change specifications or costs without
approval; cannot approve refunds/compensation outside policy.

**Dependencies:** Sales, Finance, Administration, workshops/tailors,
Founder.

**Reporting:** to Founder.

**Evaluation:** reliability, specification accuracy, defect prevention,
issue resolution, customer experience.

**Failure modes:** undocumented verbal changes; dependence on memory;
late discovery of defects; overbooking; confusing internal estimates
with customer promises.

**Founder approval required:** workshop agreements, capacity expansion,
major rework/refund, delivery policy, service-area changes.

### Role 6 --- Finance & Business Control Manager

**Purpose:** provide accurate, timely financial evidence for pricing,
cash allocation, and sustainable operating decisions.

**Scope and core duties** - maintain actual cost records and unit
economics; - reconcile price calculator assumptions with invoices and
payments; - calculate gross and contribution margins consistently; -
monitor cash flow, deposits, working capital, and budget; - analyze CAC
and campaign profitability with Marketing; - prepare forecasts and risk
scenarios; - identify financial control gaps.

**Deliverables** - cost sheet by product/fabric category; - approved
pricing model and version history; - order-level margin report; -
cash-flow forecast and actuals; - monthly budget and variance report; -
CAC/contribution analysis; - financial risk memo.

**KPIs** - cost-record completeness; - forecast variance; - margin
visibility by order; - cash-flow visibility; - reconciliation errors; -
timeliness of financial reporting.

**Inputs:** supplier invoices, workshop payments, sales/quote/order
records, ad spend, visit expenses, fixed costs, tax/accounting guidance.

**Decision authority:** define calculations, flag unaffordable
commitments, recommend prices/budgets, maintain controls.

**Limits:** cannot independently set public prices, approve spending
beyond budget, or replace a licensed accountant/tax adviser/legal
professional.

**Dependencies:** Sales, Operations, Marketing, Administration, Founder,
qualified external accountant where needed.

**Reporting:** directly to Founder; must be able to report risks without
commercial pressure.

**Evaluation:** accuracy, traceability, usefulness of forecasts, early
warning, control discipline.

**Failure modes:** confusing markup with margin; using stale workbook
assumptions; treating assumed CAC as actual; excluding rework/visits;
cash-flow blindness.

**Founder approval required:** final prices, budgets, financing, tax
positions, material commitments, write-offs.

### Role 7 --- AI Engineer & Automation Architect

**Purpose:** identify and implement affordable, maintainable technical
improvements that solve verified business problems.

**Scope and core duties** - map manual workflows and data
requirements; - evaluate AI tools, agents, open-source software, APIs,
and integrations; - design small prototypes and tests; - assess data
quality, security, privacy, and failure modes; - document deployment,
monitoring, fallback, and maintenance; - create a founder learning
roadmap; - distinguish no-code, AI-assisted, and professional
engineering work.

**Deliverables** - workflow maps and technical requirements; - tool
comparison and cost/benefit notes; - prototype and test report; -
data/security risk assessment; - automation runbook and monitoring
plan; - founder learning roadmap.

**KPIs** - measurable time/error reduction; - reliability and failure
rate; - total cost of ownership; - adoption by actual users; -
maintainability/documentation; - privacy/security incidents (target:
none; baseline/controls required).

**Inputs:** process owners, approved data, business priorities, budget,
technical constraints, user requirements.

**Decision authority:** technical recommendation and implementation
within an approved scope/budget.

**Limits:** cannot automate an unstandardized process without
documenting exceptions; cannot use customer data in external services
without approval; cannot promise measurement accuracy or automate
pattern production as a service.

**Dependencies:** all functions, especially Administration, Operations,
Finance, Founder.

**Reporting:** to Founder; technical work independently reviewed before
production use.

**Evaluation:** practical business value, reliability, affordability,
security, maintainability.

**Failure modes:** technology-first thinking; automation of a broken
process; hidden recurring costs; unreviewed AI outputs; unsafe handling
of customer measurements.

**Founder approval required:** customer-data integrations, paid tools,
production deployment, external sharing, material technical spend.

### Role 8 --- Independent Strategy & Quality Reviewer

**Purpose:** independently challenge evidence, logic, consistency, risk,
and quality across the business without owning the outputs being
evaluated.

**Scope and core duties** - review departmental recommendations and
deliverables; - test source accuracy and classification; - identify
cross-functional contradictions and responsibility gaps; - challenge
assumptions and unsupported claims; - review strategy/operations/finance
alignment; - track corrective actions and recurring failure modes; -
report directly to Founder.

**Deliverables** - independent review reports; - evidence and assumption
audit; - cross-functional conflict register; - quality findings and
severity ratings; - corrective-action tracking; - periodic founder
briefing.

**KPIs** - material issue detection; - false-positive/unsupported
criticism rate; - closure rate of corrective actions; - recurrence of
known defects; - review timeliness and independence.

**Inputs:** source documents, raw records, departmental outputs,
decision register, KPI reports.

**Decision authority:** may reject a deliverable as not review-ready,
issue findings, and escalate risks. It does not own operational
execution or final approval.

**Limits:** cannot approve its own work; cannot overrule Founder; cannot
convert recommendations into policy.

**Dependencies:** all functions; direct access to Founder.

**Reporting:** directly to Founder, not to any function under review.

**Evaluation:** independence, fairness, evidence traceability,
materiality, practical corrective actions.

**Failure modes:** becoming a rubber stamp; reviewing only presentation;
using the same assumptions as the author; excessive criticism without
evidence; self-review.

**Founder approval required:** appointment of reviewer, review scope,
risk acceptance, closure of material findings.

**Independence safeguard:** if an AI system drafts a deliverable, the
same instance must not be treated as the independent approver of that
deliverable. Use a separate reviewer/process and Founder decision.

## 5. Lean implementation model

### 5.1 Now --- functions to activate

  -----------------------------------------------------------------------
  Function                Practical owner now     Support model
  ----------------------- ----------------------- -----------------------
  Founder / final         Founder                 Direct
  authority                                       

  Marketing & Growth      Founder accountable     AI-assisted
                                                  research/analysis; no
                                                  campaign scale without
                                                  evidence

  Content & Creative      Founder accountable for Founder films/edits; AI
                          brand approval;         drafts/reviews; use
                          production may be       actual assets
                          AI-assisted             

  Administration          Founder initially       Low-cost
                                                  sheets/templates;
                                                  AI-assisted data
                                                  cleanup/reporting

  Sales & Customer        Founder                 Separate pipeline from
  Relations                                       admin inbox

  Operations & Customer   Founder                 Workshop coordination
  Experience                                      and QC; introduce job
                                                  card/checklist

  Finance & Business      Founder with structured AI can analyze but not
  Control                 spreadsheet; external   certify books/tax
                          accountant as needed    

  AI Engineering          Founder learning +      Avoid custom build
                          scoped technical        until process/data need
                          support                 is proven

  Independent Review      Separate review         Must not self-approve
                          pass/person/AI session; 
                          Founder appoints        
  -----------------------------------------------------------------------

### 5.2 Roles that may be combined temporarily

**Recommended temporary combination:** - Founder performs Marketing,
Sales, Operations, and final approval because these are already
founder-centric. - Content production may be handled by Founder with AI
assistance, while keeping editorial accountability distinct from
publishing administration. - Administration can be a lightweight
system/process rather than a dedicated employee. - Finance control can
begin as disciplined bookkeeping and order-level costing, with qualified
external accounting support for statutory matters. - AI Engineering
should be project-based, not a full-time role. - Independent Review
should be a distinct review function, not a new payroll position.

**Do not collapse accountability:** - Marketing ≠ Sales. - Sales ≠
Administration. - Content/Creative ≠ calendar administration. -
Operations ≠ Sales promises. - Finance ≠ pricing alone. - Independent
Review ≠ the author of the work being reviewed.

### 5.3 Outsource or defer

  ---------------------------------------------------------------------------
  Function/specialist         Current recommendation  Trigger
  --------------------------- ----------------------- -----------------------
  Accountant/tax professional Outsource qualified     Registration, tax
                              work when required      filings, statutory
                                                      accounts

  Legal adviser               On-demand               Registration,
                                                      contracts, consumer
                                                      terms, privacy,
                                                      employment

  Photographer/videographer   Project-based           When actual
                                                      garments/process need
                                                      higher production
                                                      quality

  Graphic designer            Project-based           Final vector/logo
                                                      system and
                                                      production-ready brand
                                                      assets

  Tailoring/technical         Existing workshop       QC standard,
  specialist                  relationship; document  pattern/construction
                              and evaluate            review, capacity

  HR/recruitment              Defer                   Hiring becomes real and
                                                      recurring

  Custom AI/software engineer Defer or scoped         A stable process has a
                              contract                measured bottleneck and
                                                      business case
  ---------------------------------------------------------------------------

## 6. Reporting cadence --- RECOMMENDATION

-   Weekly: lead pipeline, orders, production exceptions, cash
    commitments, content status, urgent risks.
-   Monthly: channel performance, quote conversion, order economics,
    workshop quality, cash flow, repeat/referral evidence.
-   Quarterly: positioning, customer segment evidence, role workload,
    technology roadmap, risk register, and budget.

Cadence should be simplified if it consumes more time than the decisions
it supports.

## 7. Approval status

This architecture is a proposal. The Founder must approve role
boundaries, temporary combinations, reporting cadence, and the degree of
AI/external support before it becomes the operating model.
