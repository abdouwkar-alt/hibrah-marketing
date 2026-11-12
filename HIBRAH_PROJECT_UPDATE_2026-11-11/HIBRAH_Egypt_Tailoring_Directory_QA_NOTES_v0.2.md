# HIBRAH Egypt Tailoring Directory — QA Notes

**Status:** DRAFT v0.2 — NOT APPROVED  
**Source:** `HIBRAH — Egypt Tailoring Directory.csv` (2026-11-11)  
**Handling:** Original fields and values are preserved. Two QA columns are appended.

## Column semantics
- `search_query_area`: the area used in the Google Maps search query; it is not verified as the business's physical location.
- `address_as_listed`: address text returned/listed by Google Maps; it still requires verification.
- `qa_status`: all rows begin as `NOT_VERIFIED`; rows with obvious area/address discrepancies are marked `VERIFY_ADDRESS_AND_AREA`.
- `qa_notes`: records the reason for a manual verification flag.

## Snapshot audit
- 123 rows.
- 98 distinct listed business names.
- 27 distinct search-area labels.
- 123 distinct Maps place IDs.
- 5 rows lack rating and review-count values.
- Repeated names may represent branches, not duplicate records or distinct companies.
- The CSV is a discovery directory, not a verified census or a direct-competitor-only list.

## Flagged examples
- Catalonia: query area Banha; address text points to Shibin El Kom, Menoufia.
- Vialle: query area Kafr El Sheikh; address text appears internally inconsistent.
- TiE HOUSE: two entries have apparent search-area/address mismatches (Fayoum/Cairo and Hurghada/Luxor).
- Maurice Hanna: query area Fayoum; address text points to Heliopolis, Cairo.

Do not delete, merge or geographically reassign any row until the original Maps listing is checked.
