# Salati App — Data, Quran Content, and Release Architecture
**Decision record:** 22 November 2026  
**Repository branch:** `salati-app`

## Executive decision

Keep the first public version offline-first and privacy-first:

1. App code, source files, reviewed static content, build workflows, and version history belong in GitHub.
2. Each person's prayer records, qada progress, dhikr counters, and Quran reflection notes remain private to that person's phone by default.
3. Provide a user-controlled JSON export and restore flow. The system file picker can offer Google Drive or another document provider when installed; this is a file backup chosen by the user, not automatic synchronization.
4. Do not use a public GitHub repository as a database for user records. A repository is for versioned project files, not private per-user storage.
5. Defer cloud accounts and cross-device synchronization until there is a clear consent, authentication, deletion, export, and support plan.
6. Use GitHub Releases for persistent APK distribution after a stable signing key is configured. GitHub Actions artifacts remain build/test artifacts, not the app's permanent public download channel.

## What the app stores today

The inspected code uses Android SharedPreferences for small local records and settings. Examples include prayer statuses and counts, separate qada counters, notification preferences, the current Quran reading position, the daily page target/progress, and Quran reflection sessions (currently represented as a JSON string).

This is local app data, not a central database. It stays on the same installation through normal app updates, but app-specific data and files are removed when the app is uninstalled. Android's own documentation describes the app-specific storage and uninstall behavior:
- https://developer.android.com/training/data-storage/app-specific
- https://developer.android.com/training/data-storage

### Backup and restore implementation in this revision

The Settings screen now includes:
- Create a JSON backup through Android's `ACTION_CREATE_DOCUMENT` file picker.
- Restore from a JSON file selected through Android's document picker.
- Package ID and schema version validation before restore.
- A confirmation dialog because restore replaces the current saved preferences.
- Preservation of the current phone's location coordinates; `lat` and `lon` are intentionally excluded from the backup.
- Typed serialization for strings, integers, longs, floats, booleans, and string sets.

The JSON is a plain-text file and may contain personal reflection notes. Users should keep it private. It is not encrypted by the app in this revision. Google Drive can appear as a save location through Android's document-provider picker, but no Google Drive API integration or background sync is implemented.

## Local database decision

### Current phase

Keep SharedPreferences for small settings and counters while the app is still in early development. The explicit backup/restore feature protects portability without requiring users to create an account or sending their private activity to a server.

### Next internal storage phase

When the journal and prayer history grow, migrate record-shaped information to Room:
- Quran reflection sessions as individual rows.
- Prayer history as dated prayer records.
- Qada estimates and completion history as explicit records.
- Any future bookmarks and reading plans as structured records.

Keep notification toggles, simple preferences, and a few small counters in Preferences. Migrate from the existing SharedPreferences keys through a one-time, idempotent migration; do not delete the old values until row counts and sample records have been checked. This phased migration reduces the risk of losing the founder's existing data.

Android recommends a local database as the source of truth for offline-first apps and identifies Room as the local relational storage option:
- https://developer.android.com/topic/architecture/data-layer
- https://developer.android.com/topic/architecture/data-layer/offline-first
- https://developer.android.com/training/data-storage

## Cloud database options — research

No cloud database is connected in this revision.

### Firebase Firestore — preferred candidate for a later opt-in sync phase

The no-cost Spark plan currently includes a Firestore quota of 1 GiB stored data, 50,000 document reads/day, 20,000 writes/day, 20,000 deletes/day, and 10 GiB outbound transfer/month. The free quotas are hard limits: after a product quota is exceeded, that product is shut off for the rest of the month unless the project is upgraded. Firebase Authentication's common sign-in options and Cloud Messaging are listed as no-cost products. See:
- https://firebase.google.com/docs/firestore/quotas
- https://firebase.google.com/docs/projects/billing/firebase-pricing-plans

Why it is the preferred candidate if/when sync is approved:
- It has an established Android SDK and managed authentication.
- The Spark plan can be started without adding a payment method for the no-cost services.
- A per-user data model can be protected with authenticated user IDs and Firestore Security Rules.

Required before launch of cloud sync:
- Require a recoverable sign-in method; anonymous authentication alone is not enough for device-loss recovery.
- Put every user record beneath that user's authenticated UID.
- Enforce ownership in Security Rules; never rely only on client-side filtering.
- Add account deletion, data export, and conflict-resolution behavior.
- Make sync explicitly optional and explain what is uploaded.
- Define a recovery/backup plan. Firestore's free quota does not include every backup/restore feature.
- Monitor quota usage and make no promise of unlimited free storage.

### Supabase Free — useful alternative, not selected for the first sync implementation

The current Free plan lists 500 MB database size, 5 GB uncached egress, 5 GB cached egress, 50,000 monthly active users, and two active projects. Free projects can pause after a week of low activity, and automatic database backups are not included in the Free plan. See:
- https://supabase.com/pricing
- https://supabase.com/docs/guides/platform/free-project-pausing
- https://supabase.com/docs/guides/platform/billing-on-supabase

Supabase is attractive where direct SQL/Postgres control is important. Its free inactivity pause and backup limitations make it a less attractive first choice for an app whose users may expect their saved worship records to always be available.

### Google Drive

Use Drive as a user-selected destination for backup files first, through Android's built-in document picker. That avoids asking every user to sign into a service controlled by the app. Automatic Drive sync would require a separate Google authorization and conflict/retry design; it is not part of this revision.

## Quran text design

The reader added in this revision retrieves the Uthmani Quran edition from:
`https://api.alquran.cloud/v1/quran/quran-uthmani`

The API documentation identifies the complete-Quran and per-surah endpoints:
- https://legacy.alquran.cloud/api

AlQuran Cloud's terms, last updated 13 November 2026, say the Quran text may be used, stored, and shown, but must remain exactly as provided with its diacritics and spelling; they ask for source credit and link to the Credits page. Its Credits page names Tanzil among the Quran-text sources:
- https://alquran.cloud/terms-and-conditions
- https://alquran.cloud/credits

The app's reader:
- Downloads the complete text once on first use.
- Checks for 114 surahs, the expected 6,236 ayahs, ordered surah numbers, verse counts, sequential verse numbers, and non-empty verse text.
- Saves the unmodified API response in app-private persistent files after validation.
- Reads from the local file on subsequent use, including offline use after a successful first download.
- Shows attribution to Tanzil and AlQuran Cloud and links to the text license.

Important limitations:
- The first download requires internet. If the device is offline before the text has been cached, the reader shows a retry message instead of inventing or partially displaying verses.
- The checks above are structural integrity checks, not an independent scholarly certification of the Quran text. The publisher's no-warranty statement still applies.
- No translation, tafsir, recitation, page-image scans, or audio has been bundled or republished here.

## App updates and distribution

### Development builds

GitHub Actions builds a debug APK for verification. The current workflow explicitly sets uploaded APK artifact retention to 14 days (`retention-days: 14`). GitHub's general default is 90 days, but this repository overrides it. Artifacts are therefore temporary build outputs, not the permanent download channel:
- https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts
- https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository

### Public distribution without a monthly fee

Use public GitHub Releases as the initial free distribution channel once release signing is ready. Keep release notes, version number, APK asset, and SHA-256 checksum together. The app may later check the public latest-release metadata and show an update notice; downloading and installing an APK outside Google Play still requires the user to approve the Android installer flow.

### Signing is a release blocker

Android only accepts an in-place update when the new APK matches the installed app's signing certificate. The same private release key must be used for every release:
- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/guide/app-bundle/faq

The current GitHub Actions job builds a debug APK and does not use a persistent release keystore. Do not distribute debug builds as the public upgrade path. Before making stable GitHub Releases:
1. Create one release keystore locally.
2. Keep a secure offline copy and password record.
3. Store signing material only in GitHub Actions Secrets (never in the repository).
4. Add a release build workflow that signs with that same keystore for every version.
5. Verify the APK certificate and SHA-256 before publishing.
6. Increase `versionCode` with every release.

Google Play is the better long-term update channel because it supports managed app updates and in-app update flows. Its developer account currently has a one-time USD 25 registration fee:
- https://developer.android.com/guide/playcore/in-app-updates
- https://support.google.com/googleplay/android-developer/answer/6112435

No silent APK self-installation is promised. A code change generally requires an app update; remotely hosted content can be updated independently only when the app has explicitly been designed to load that content safely.

## Implementation status in this revision

- [x] Quran reader UI with searchable surah selection and verse selection.
- [x] Download-once, local Quran text cache and structural validation.
- [x] Tanzil / AlQuran Cloud attribution link in the reader.
- [x] Full app preferences backup and restore through Android's file picker.
- [x] Exclude exact stored latitude/longitude from exported backup.
- [x] Keep the user's data local; no central database or login introduced.
- [x] Bump Android version to 1.1.0 / versionCode 2.
- [ ] Build and inspect this exact commit.
- [ ] Test first-run Quran download, offline cache read, failed download retry, backup, and restore on a real Android device.
- [ ] Review the Quran reflection UX and Settings UX with the founder before a final public release.
- [ ] Create and protect a stable release keystore before public distribution.
- [ ] Only then create the first signed GitHub Release and update notification flow.

## Non-negotiable data safety rules

- Never commit a user's personal notes, prayer records, location, or backup files into the public GitHub repository.
- Never treat a debug APK as a production release.
- Never overwrite user data during an app update or migration without a backup path.
- Never add cloud sync without explicit consent, account recovery, access rules, export, and deletion.
- Keep Quran text verbatim; do not normalize, strip diacritics, or rewrite Quranic text.
