# Quran reader and worship-content update — 2026-11-23

## Quran reader repair
- The reader no longer requires `numberOfAyahs` to exist in every surah object returned by the Quran data endpoint.
- When that optional field is absent, the parser derives the expected per-surah count from the returned `ayahs` array.
- Structural validation remains in place: 114 surahs, sequential surah and ayah numbering, non-empty ayah texts, and 6,236 ayahs in total.
- Unexpected JSON/parser messages are replaced with an Arabic user-facing recovery message instead of exposing raw implementation errors.
- The source response is still cached locally after a successful validation; this change does not alter Quran text.

## Daily Quran goal
- Replaced the previous page-based goal with a fixed suggested goal of 10 ayahs per day.
- The user enters the number of ayahs actually read and records them in one action.
- Daily progress resets on the date change.
- New preference keys are used for ayah-based progress so old page counts are not misinterpreted as ayah counts.
- The app explicitly describes 10 ayahs as a suggested personal habit goal, not a binding religious quota or a claim that a fixed daily ten-ayah amount is prescribed.

## Prayer Sunnahs and Fajr
The Fajr section now includes:
- The virtue of the two Sunnah rak'ahs: Sahih Muslim 725.
- Their light/brief performance: Sahih al-Bukhari 1165 and Sahih Muslim 724.
- The recitation of al-Kafirun and al-Ikhlas: Sahih Muslim 726.
- Lying on the right side after the two Fajr Sunnah rak'ahs: Sahih al-Bukhari 1160 and Sahih Muslim 736; presented as an established reported action, with a note that juristic details of recommendation differ.
- The light supplication in the context of leaving for prayer after night worship: Sahih Muslim 763l; a related wording is in Sahih al-Bukhari 6316. It is not presented as a required post-Fajr formula.

Two general prayer practices are displayed alongside prayer-specific guidance:
- Raising the hands at the opening takbir, before bowing, and after rising from bowing: Sahih al-Bukhari 735. The UI notes the recognized madhhab differences.
- Making abundant supplication in prostration: Sahih Muslim 482.

The post-prayer section now also includes: `اللهم أعني على ذكرك وشكرك وحسن عبادتك` — Sunan Abi Dawud 1522 (authenticated by al-Albani).

## Dua library expansion
The app continues to show two entries at a time and now has 12 Prophetic supplications and 15 Quranic supplications.

New Prophetic entries and references:
- Seeking forgiveness for one's wrongdoing: Sahih al-Bukhari 834.
- Seeking the soul's piety and refuge from unbeneficial knowledge: Sahih Muslim 2722.
- Seeking protection from the loss of blessings and sudden punishment: Sahih Muslim 2739.
- Asking for pardon: Jami' al-Tirmidhi 3513 (al-Tirmidhi graded it Hasan Sahih).
- Asking for guidance and rightness: Sahih Muslim 2725.
- The light supplication when going out for prayer: Sahih Muslim 763l.

New Quranic entries include:
- Al-Furqan 25:74.
- Taha 20:25–28.
- Al-Qasas 28:24.
- Al-Kahf 18:10.
- Ibrahim 14:40.
- Al-Anbiya 21:87.
- Taha 20:114.
- Hud 11:47.
- Ghafir 40:7.
- Al-Baqarah 2:286.
- Al-A'raf 7:126.

## Editorial safeguards
- Quranic texts are displayed with their surah and ayah references.
- Hadith entries include the collection and number, and grading where it is relevant and available.
- The Fajr light supplication is contextualized in the hadith rather than described as an obligatory or fixed post-Fajr dhikr.
- The 10-ayah daily goal is a suggested app setting, not a claim about a universally fixed Sunnah count.
- This is a curated expansion, not a claim that the dua and Sunnah collections are exhaustive. Additional entries should only be added after checking the wording, context, source, and grading.
