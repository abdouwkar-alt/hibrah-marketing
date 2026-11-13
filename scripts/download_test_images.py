import html
import json
import os
import re
import sys
import time
from pathlib import Path
from urllib.parse import quote

import requests

OUT = Path("research-assets/test-10-images")
OUT.mkdir(parents=True, exist_ok=True)
TARGET = 10
HEADERS = {
    "User-Agent": "HIBRAH-ResearchAssetDownloader/2.0 (GitHub Actions; contact: repository maintainer)",
    "Accept": "application/json,image/*,*/*;q=0.8",
}
SESSION = requests.Session()
SESSION.headers.update(HEADERS)

# Only licenses suitable for commercial use and redistribution are accepted.
ALLOWED_LICENSES = {"cc0", "pdm", "by", "by-sa"}
SEARCH_TERMS = [
    "mens bespoke suit",
    "mens tailoring workshop",
    "wool suiting fabric",
    "suit lapel tailoring",
    "tailor taking measurements",
    "hand stitched suit",
]

def request_with_retry(url, *, params=None, stream=False, timeout=45, attempts=5):
    last_error = None
    for attempt in range(attempts):
        try:
            response = SESSION.get(url, params=params, timeout=timeout, stream=stream)
            if response.status_code == 429 or 500 <= response.status_code < 600:
                retry_after = response.headers.get("Retry-After")
                try:
                    delay = min(60, max(2, int(retry_after))) if retry_after else min(60, 2 ** attempt * 3)
                except ValueError:
                    delay = min(60, 2 ** attempt * 3)
                response.close()
                if attempt == attempts - 1:
                    raise requests.HTTPError(f"HTTP {response.status_code} after {attempts} attempts")
                print(f"Temporary HTTP {response.status_code}; retrying in {delay}s")
                time.sleep(delay)
                continue
            response.raise_for_status()
            return response
        except (requests.RequestException, TimeoutError) as exc:
            last_error = exc
            if attempt == attempts - 1:
                break
            delay = min(45, 2 ** attempt * 3)
            print(f"Request error: {exc}; retrying in {delay}s")
            time.sleep(delay)
    raise RuntimeError(f"Request failed after {attempts} attempts: {last_error}")

def clean(value):
    if not value:
        return ""
    return html.unescape(re.sub(r"<[^>]*>", "", str(value))).strip()

def safe_extension(url, content_type):
    ext = Path(url.split("?", 1)[0]).suffix.lower()
    if ext in {".jpg", ".jpeg", ".png", ".webp", ".gif", ".tif", ".tiff"}:
        return ".jpg" if ext == ".jpeg" else ext
    return {
        "image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp",
        "image/gif": ".gif", "image/tiff": ".tiff",
    }.get(content_type.split(";", 1)[0].lower(), ".img")

def download_candidate(candidate, index):
    url = candidate.get("download_url")
    if not url or not url.startswith("https://"):
        raise ValueError("Missing or non-HTTPS image URL")
    response = request_with_retry(url, stream=True, timeout=90, attempts=4)
    content_type = response.headers.get("Content-Type", "").lower()
    if not content_type.startswith("image/"):
        response.close()
        raise ValueError(f"URL did not return an image: {content_type}")
    chunks, size = [], 0
    for chunk in response.iter_content(chunk_size=1024 * 128):
        if chunk:
            size += len(chunk)
            if size > 25 * 1024 * 1024:
                response.close()
                raise ValueError("Image exceeds 25 MB test limit")
            chunks.append(chunk)
    response.close()
    if size < 5_000:
        raise ValueError(f"Image too small ({size} bytes)")
    ext = safe_extension(url, content_type)
    slug = re.sub(r"[^A-Za-z0-9_-]+", "_", candidate.get("title", "image"))[:55].strip("_") or "image"
    target = OUT / f"{index:02d}_{slug}{ext}"
    target.write_bytes(b"".join(chunks))
    candidate["file"] = str(target)
    candidate["bytes"] = size
    candidate["content_type"] = content_type
    print(f"Downloaded {index}/{TARGET} from {candidate['provider']}: {target} ({size} bytes)")
    return candidate

def openverse_candidates(status):
    api = "https://api.openverse.org/v1/images/"
    seen = set()
    for term in SEARCH_TERMS:
        if len(records) >= TARGET:
            break
        try:
            response = request_with_retry(api, params={
                "q": term,
                "license": "cc0,pdm,by,by-sa",
                "page_size": 20,
            }, timeout=45, attempts=4)
            data = response.json()
            status["openverse"] = "accessible"
            for item in data.get("results", []):
                license_code = (item.get("license") or "").lower()
                url = item.get("url")
                if license_code not in ALLOWED_LICENSES or not url or url in seen:
                    continue
                seen.add(url)
                candidate = {
                    "provider": "Openverse",
                    "title": clean(item.get("title")) or term,
                    "download_url": url,
                    "source_page": item.get("foreign_landing_url") or item.get("detail_url"),
                    "creator": clean(item.get("creator")),
                    "attribution": clean(item.get("attribution")),
                    "license": license_code,
                    "license_version": item.get("license_version"),
                    "license_url": item.get("license_url"),
                    "search_term": term,
                }
                try:
                    records.append(download_candidate(candidate, len(records) + 1))
                except Exception as exc:
                    print(f"Openverse candidate skipped: {exc}")
                if len(records) >= TARGET:
                    break
            time.sleep(1)
        except Exception as exc:
            status["openverse"] = f"error: {exc}"
            print(f"Openverse search failed for '{term}': {exc}")

def commons_candidates(status):
    api = "https://commons.wikimedia.org/w/api.php"
    seen = set()
    for term in SEARCH_TERMS:
        if len(records) >= TARGET:
            break
        try:
            response = request_with_retry(api, params={
                "action": "query",
                "format": "json",
                "generator": "search",
                "gsrsearch": f"filetype:bitmap {term}",
                "gsrnamespace": 6,
                "gsrlimit": 30,
                "prop": "imageinfo",
                "iiprop": "url|extmetadata",
            }, timeout=45, attempts=5)
            pages = response.json().get("query", {}).get("pages", {})
            status["wikimedia_commons"] = "accessible"
            for page in pages.values():
                infos = page.get("imageinfo", [])
                if not infos:
                    continue
                info = infos[0]
                meta = info.get("extmetadata", {})
                license_name = clean(meta.get("LicenseShortName", {}).get("value"))
                license_url = clean(meta.get("LicenseUrl", {}).get("value"))
                license_lower = license_name.lower()
                # Reject non-commercial and no-derivatives terms; permit CC0, public domain, CC BY and CC BY-SA.
                if any(term in license_lower for term in ("nc", "noncommercial", "nd", "no derivatives")):
                    continue
                if not any(term in license_lower for term in ("cc0", "public domain", "cc by", "cc-by")):
                    continue
                url = info.get("url")
                if not url or url in seen:
                    continue
                seen.add(url)
                title = page.get("title", term)
                candidate = {
                    "provider": "Wikimedia Commons",
                    "title": title,
                    "download_url": url,
                    "source_page": "https://commons.wikimedia.org/wiki/" + quote(title.replace(" ", "_")),
                    "creator": clean(meta.get("Artist", {}).get("value")),
                    "attribution": clean(meta.get("Credit", {}).get("value")),
                    "license": license_name,
                    "license_url": license_url,
                    "description": clean(meta.get("ImageDescription", {}).get("value")),
                    "search_term": term,
                }
                try:
                    records.append(download_candidate(candidate, len(records) + 1))
                except Exception as exc:
                    print(f"Wikimedia candidate skipped: {exc}")
                if len(records) >= TARGET:
                    break
            time.sleep(2)
        except Exception as exc:
            status["wikimedia_commons"] = f"error: {exc}"
            print(f"Wikimedia search failed for '{term}': {exc}")

records = []
provider_status = {"openverse": "not_tested", "wikimedia_commons": "not_tested"}
openverse_candidates(provider_status)
if len(records) < TARGET:
    print(f"Openverse supplied {len(records)} images; trying Wikimedia Commons fallback.")
    commons_candidates(provider_status)

metadata = {
    "requested_images": TARGET,
    "downloaded_images": len(records),
    "complete": len(records) == TARGET,
    "provider_status": provider_status,
    "license_policy": "Only CC0, public-domain, CC BY and CC BY-SA assets are accepted; NC/ND and unknown licenses are excluded.",
    "images": records,
}
(OUT / "sources-and-licenses.json").write_text(
    json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8"
)
(OUT / "README.md").write_text(
    "# HIBRAH — 10-image download test\n\n"
    f"- Requested: {TARGET}\n- Downloaded: {len(records)}\n"
    f"- Complete: {'yes' if len(records) == TARGET else 'no'}\n\n"
    "Downloaded by GitHub Actions from Openverse and, when needed, Wikimedia Commons. "
    "Review each source page and license/attribution record before any use. "
    "This test does not imply that every image is suitable for HIBRAH's brand or campaigns.\n",
    encoding="utf-8",
)
print(json.dumps(metadata, ensure_ascii=False, indent=2))
if len(records) != TARGET:
    print(f"INCOMPLETE: expected {TARGET}, downloaded {len(records)}.")
    sys.exit(1)
print(f"SUCCESS: downloaded all {TARGET} images.")
