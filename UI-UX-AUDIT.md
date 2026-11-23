# UI/UX Audit — الصلاة جامعة
Date: 2026-11-23
Branch reviewed: salati-app
Source reviewed: app/src/main/java/com/salati/app/MainActivity.kt
Scope: static source review of Jetpack Compose UI. No APK/device visual inspection was available, so layout behavior must be verified on device.

## Product constraints
- The app is free: no subscriptions, no Premium tier, and no paid feature gates.
- Account sign-in and cloud sync are deferred until the core app is complete and stable.
- Keep worship records local-first in the current release.

## Initial independent UI/UX findings

### P0 — Navigation and information architecture
1. The top-level navigation is a horizontally scrolling row of 13 FilterChips (Home, Prayer, Sunnah, Qiyam, Fasting, Qada, Statistics, Adhkar, Duas, Wudu fiqh, Prayer fiqh, Quran reflection, Settings). This is difficult to discover, easy to overlook, and gives every destination equal visual priority.
2. The requested core product areas should be the primary navigation: Prayer, Sunnah, Qiyam al-Layl, and Fasting. Qada can be a clear sub-area under Prayer/Fasting or a secondary destination; learning/reference areas should move into a separate “Learn” or “More” area.
3. The home page currently renders five separate large cards (Prayer, Sunnah, Qiyam, Fasting, Qada) plus a large header. This is not yet the compact “three main areas” dashboard described in the product brief, and should be reconciled with the intended home hierarchy.

### P1 — Screen hierarchy and interaction
4. MainActivity.kt combines activity lifecycle, location permission handling, local persistence, business rules, and a very large Compose screen. This makes visual iteration and UI testing harder. Split into screen-level composables and a shared design system without changing behavior.
5. The app repeats the product title in the global header and the Home hero card. Consider one persistent identity header and use the hero for the next prayer/countdown.
6. Prayer times, status logging, qada, Sunnah, fasting, statistics, Quran, and fiqh content create long scroll surfaces. Each screen needs clear section headings, concise cards, and a visible current task.
7. The daily Sunnah tracker uses a useful per-prayer breakdown, but the UI should explain the 12-rak'ah target and distinguish Sunnah mu'akkadah from other voluntary prayers without implying every madhhab classifies every detail identically.
8. Fasting screen includes multiple categories and a recent-log list; make progress visible first, place secondary educational content below, and ensure Shawwal progress resets/filters correctly by Hijri month (functional review needed).

### P1 — Accessibility and mobile ergonomics
9. Check tap target sizes, text scaling, contrast, RTL alignment, and screen-reader labels. Many controls are TextButtons/FilterChips; test with large font settings and narrow Android screens.
10. The 13-item horizontal navigation has no obvious selected-destination context when users scroll away from the start. Replace with a stable bottom navigation for core destinations plus a secondary drawer/menu or dedicated “More” page.
11. Add empty states, undo feedback, and confirmation for destructive actions consistently. Avoid making “أستغفر الله” appear as an unexplained global text row; use a brief, localized Snackbar/toast near the action and do not treat it as a replacement for confirmation.

### P2 — Visual consistency
12. Centralize spacing, corner radius, typography, colors, card elevation, and button variants into a small design system. Current screen code repeats raw colors and padding values.
13. Use consistent screen titles, supporting descriptions, section spacing, and progress indicators. Avoid overusing white cards on cream backgrounds without enough grouping.
14. Verify Arabic typography, diacritics, numeral formatting, and long Arabic labels on real devices.

## Recommended implementation sequence
1. Confirm primary navigation and home hierarchy against the product brief.
2. Extract reusable UI components and a theme/design-token layer.
3. Redesign the Home, Prayer, Sunnah, Qiyam, and Fasting screens first.
4. Move secondary reference content (fiqh, adhkar, duas, Quran reflection, settings) into a secondary navigation area.
5. Run device previews and accessibility checks at narrow, standard, and large font settings.
6. Only after visual review, refactor deeper state/business logic; do not change religious calculations or hadith content as part of a visual-only pass.

## Gemini review request
Please review the linked MainActivity.kt as a senior Android UI/UX and Jetpack Compose reviewer. Focus on information architecture, RTL, Arabic readability, navigation discoverability, screen density, touch targets, accessibility, component reuse, and responsive layouts. Do not change business logic, prayer calculations, hadith text, local persistence keys, or permissions. Return:
- P0/P1/P2 findings with exact code locations or snippets
- a proposed navigation map
- a proposed home-screen hierarchy
- concrete composable-level refactoring recommendations
- risks/regressions to test
- a phased redesign proposal
Keep the product fully free; do not introduce subscriptions, paid tiers, analytics, ads, or cloud accounts/sync in this phase.
