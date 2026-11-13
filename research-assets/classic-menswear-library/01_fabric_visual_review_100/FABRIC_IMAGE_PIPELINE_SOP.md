# HIBRAH — Fabric Image Collection & Canva Review Pipeline

**Purpose:** Repeatable process for collecting, screening, downloading, organising, and handing over visual reference images for Founder review in Canva.

**Standard batch size:** 100 images.  
**Current batch scope:** Wool and classic men's suiting fabrics.  
**Repository destination:** `research-assets/classic-menswear-library/01_fabric_visual_review_100/`  
**Canva destination:** `HIBRAH — Fabric Reference Review — 100 Images`

## Operating sequence

### 1. Search and source selection
- Search specialist clothmakers, fabric mills, and established suiting-fabric retailers first.
- Prioritise product-specific fabric images, swatches, close-ups, and bunch/sample-book imagery.
- Record the original product/source page and direct image URL for every candidate.
- Avoid generic stock images, unrelated textiles, casualwear, and non-suiting products.

### 2. Visual screening
Review each candidate image before treating it as accepted. Record:
- Short visual description and visible colours.
- Category and pattern/weave where visually supportable.
- Image quality: GOOD / MEDIUM / POOR.
- Logo or watermark: YES / NO / UNCLEAR.
- Duplicate/similarity: UNIQUE / POSSIBLE DUPLICATE / DUPLICATE.
- Decision: ACCEPT / REJECT / REVIEW.

Do not infer fibre composition, mill, origin, weight, or technical properties from pixels alone. Those details must be recorded as source-page metadata and distinguished from visual observations.

### 3. Build the batch manifest
For every candidate, record a stable sequence ID, filename, source brand, product title, source page, direct image URL, alt text, image dimensions, byte size, SHA-256, visual review fields, rights status, and intended use.

Only ACCEPT images proceed to the final download set. REJECT images are excluded. REVIEW items are held out until resolved or replaced.

### 4. GitHub Actions download
- The batch workflow runs from the repository's `main` branch.
- It downloads accepted image URLs into the batch's `images/` directory.
- It validates image MIME type, decodability, minimum dimensions, maximum file size, and SHA-256 uniqueness.
- It creates a JSON manifest, CSV index, and numbered contact sheets.
- It commits the resulting image files and indexes to `main`.
- A run is successful only when the target count is reached and all validation checks pass.

### 5. Canva upload
After the GitHub download/commit is confirmed:
- Upload each accepted image using its public raw GitHub image URL.
- Use the stable image ID and concise subject name as the Canva asset name.
- Move every uploaded asset into the designated HIBRAH batch folder.
- If a direct URL import fails, retry using the original source URL or another verified direct image URL. Replace the candidate if it remains unavailable.
- Do not report completion until Canva confirms all 100 uploads and the destination folder count is verified.

### 6. Founder handover
Provide:
- Canva folder link.
- GitHub batch folder and manifest links.
- Total images downloaded and uploaded.
- Any missing, rejected, or unresolved records.
- Confirmation that the Canva folder contains the complete batch.

The Founder performs the final visual selection/review in Canva.

## Exception handling

| Problem | Required response |
|---|---|
| Source page unavailable | Move to the next approved specialist source; record the failed source. |
| Image URL blocked or invalid | Try the product page's alternate image URL; otherwise replace the candidate. |
| Image too small or corrupt | Reject and replace it. |
| Duplicate detected by URL or SHA-256 | Skip it and collect a different candidate. |
| Similar-looking images | Flag for visual review; do not assume they are unique. |
| Fewer than 100 valid images | Continue searching fallback sources; do not label the batch complete. |
| GitHub Action fails | Inspect the failed step/log, fix the specific cause, and rerun. Preserve successfully validated files. |
| Canva upload fails | Retry with the verified raw GitHub URL; if still blocked, use another valid image source and record the replacement. |
| Canva count is below 100 | Upload missing items and verify the folder again before handover. |

## Status and rights
- Downloaded does not mean visually accepted.
- Visually accepted does not mean rights-cleared.
- Unless item-level permissions establish otherwise, retain `RIGHTS NOT CLEARED` and `INTERNAL CREATIVE REVIEW ONLY`.
- Do not use assets publicly or in campaigns without a separate rights check.

## Completion gate
A batch is complete only when all of the following are true:
1. 100 accepted images have passed file validation.
2. All 100 image files and their metadata are committed to the designated GitHub `main` directory.
3. The GitHub manifest reports exactly 100 unique SHA-256 hashes.
4. All 100 assets have been uploaded to the designated Canva folder.
5. Canva folder contents have been checked and the Founder handover links are ready.
