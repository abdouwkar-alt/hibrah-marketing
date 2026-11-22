# App UX Revision — 2026-11-22

Status: Approved direction / pending implementation review by founder.
Branch: `salati-app`

## 1. Combine Qada with the Calculator

- Remove the standalone top-level `القضاء` destination.
- Keep one top-level destination: `الحاسبة`.
- Inside the calculator destination, provide two clear internal views/tabs:
  1. `حساب الفوائت`: current birth date / puberty age calculation and estimated initial qada balance.
  2. `متابعة القضاء`: current per-prayer and fasting qada counters and progress.
- The calculator's “اعتماد النتيجة وبدء المتابعة” action should switch to the internal `متابعة القضاء` view, not navigate to a separate top-level destination.
- Any link from the Prayer screen that starts or reviews the qada calculation should open the appropriate internal calculator view.
- Preserve existing saved balances and per-prayer progress; do not reset user data during this navigation refactor.

## 2. Prophetic Duas: Curated Library, Two at a Time

- Keep the two existing prophetic duas as the initial seed entries:
  - `اللهم أعني على ذكرك وشكرك وحسن عبادتك` — Abu Dawud 1522; al-Albani graded it authentic.
  - `اللهم إني أسألك الهدى والتقى والعفاف والغنى` — Sahih Muslim 2721.
- Expand the internal library with as many reliably sourced prophetic supplications as can be verified. Do not include weak/uncertain reports as authentic; retain exact wording, narrator where useful, source, hadith number, and grading.
- Display only two dua cards at a time.
- Add a clearly labeled refresh action (e.g. `دعاءان آخران`) that selects another pair from the verified library. Avoid showing the same pair immediately again when alternatives exist.
- Each card hierarchy:
  1. Short descriptive title (optional and visually secondary).
  2. The supplication itself in bold / prominent typography.
  3. The complete relevant hadith text underneath, in smaller readable typography.
  4. Source, hadith number, and authenticity grade.
- Do not overwhelm the user with the full library at once.

## 3. Quranic Duas: Two at a Time

- Keep a separate Quranic-dua collection, independent from prophetic supplications.
- Display only two Quranic duas at a time.
- Add a refresh action to show another pair, avoiding immediate repeats when possible.
- Show the Quranic supplication prominently, followed by surah name and ayah number.
- Preserve exact Quranic wording and verify references.

## 4. Encourage Small, Consistent Learning

- Add a brief, calm note in the dua area encouraging the user to learn one dua and live with it for a week rather than trying to memorize many at once.
- Support the note with the authentic hadith wording: `أحب الأعمال إلى الله أدومها وإن قل` — Sahih Muslim 783; also Sahih al-Bukhari 6464 with a longer wording.
- Suggested explanatory copy: `يمكنك أن تكتفي بتعلّم دعاء واحد هذا الأسبوع؛ القليل الذي تداوم عليه خير من الكثير الذي تنقطع عنه.`
- Keep this as gentle guidance, not a target, streak, competition, or notification pressure.

## 5. Design Principle

The worship app must not feel like an information dump. Prioritize calmness, progressive disclosure, and one small useful action at a time. Large verified libraries should remain behind refresh / next-pair interaction, not appear as long scrolling lists.

## 6. Implementation / Review Notes

- This file records the requested product direction; it is not evidence that the UI changes have shipped.
- Verify hadith text, references, and grading before adding each new prophetic dua.
- Preserve local user data during all navigation changes.
- Build and inspect the APK after implementation; do not distribute a new APK until the exact latest workflow run succeeds.
