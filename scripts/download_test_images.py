import html
import json
import re
import sys
import time
from pathlib import Path
from urllib.parse import quote

import requests

OUT = Path("research-assets/test-10-images")
OUT.mkdir(parents=True, exist_ok=True)
TARGET = 10
TERMS = ["mens suit", "tailoring", "suit jacket", "mens fashion", "wool fabric", "tailor workshop"]
HEADERS = {
    "User-Agent": "HIBRAH-ResearchAssetDownloader/3.0 (GitHub Actions)",
    "Accept": "application/json,image/*,*/*;q=0.8",
}
S = requests.Session()
S.headers.update(HEADERS)
records = []
seen = set()
provider_status = {}

def get(url, params=None, stream=False, attempts=4, timeout=50):
    last = None
    for n in range(attempts):
        try:
            r = S.get(url, params=params, stream=stream, timeout=timeout)
            if r.status_code == 429 or 500 <= r.status_code < 600:
                retry = r.headers.get("Retry-After")
                try:
                    wait = min(45, max(2, int(retry))) if retry else min(45, 2 ** n * 3)
                except ValueError:
                    wait = min(45, 2 ** n * 3)
                r.close()
                if n == attempts - 1:
                    raise requests.HTTPError(f"HTTP {r.status_code} after retries")
                print(f"HTTP {r.status_code}; retrying in {wait}s")
                time.sleep(wait)
                continue
            r.raise_for_status()
            return r
        except requests.RequestException as e:
            last = e
            if n == attempts - 1:
                break
            wait = min(30, 2 ** n * 3)
            print(f"Request failed ({e}); retrying in {wait}s")
            time.sleep(wait)
    raise RuntimeError(f"Request failed: {last}")

def clean(v):
    if not v:
        return ""
    return html.unescape(re.sub(r"<[^>]*>", "", str(v))).strip()

def download(item):
    url = item.get("download_url")
    if not url or not url.startswith("https://") or url in seen:
        return False
    seen.add(url)
    r = get(url, stream=True, attempts=3, timeout=90)
    ctype = r.headers.get("Content-Type", "").lower()
    if not ctype.startswith("image/"):
        r.close()
        raise ValueError(f"Not an image response: {ctype}")
    data = bytearray()
    for chunk in r.iter_content(128 * 1024):
        if chunk:
            data.extend(chunk)
            if len(data) > 25 * 1024 * 1024:
                r.close()
                raise ValueError("Image exceeds 25 MB")
    r.close()
    if len(data) < 5000:
        raise ValueError(f"Image too small: {len(data)} bytes")
    ext = {
        "image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp",
        "image/gif": ".gif", "image/tiff": ".tiff",
    }.get(ctype.split(";")[0], ".img")
    title = re.sub(r"[^A-Za-z0-9_-]+", "_", item.get("title", "image"))[:45].strip("_") or "image"
    idx = len(records) + 1
    path = OUT / f"{idx:02d}_{title}{ext}"
    path.write_bytes(data)
    item.update({"file": str(path), "bytes": len(data), "content_type": ctype})
    records.append(item)
    print(f"SUCCESS {idx}/{TARGET} — {item['provider']} — {path} ({len(data)} bytes)")
    return True

def add_candidate(item):
    if len(records) >= TARGET:
        return
    try:
        download(item)
    except Exception as e:
        print(f"Candidate skipped from {item.get('provider')}: {e}")

def run_openverse():
    name = "Openverse"
    try:
        for term in TERMS:
            if len(records) >= TARGET: break
            data = get("https://api.openverse.org/v1/images/", {
                "q": term, "license": "cc0,pdm,by,by-sa", "page_size": 30
            }).json()
            for x in data.get("results", []):
                if len(records) >= TARGET: break
                lic = (x.get("license") or "").lower()
                if lic not in {"cc0", "pdm", "by", "by-sa"}: continue
                add_candidate({
                    "provider": name, "title": clean(x.get("title")) or term,
                    "download_url": x.get("url"),
                    "source_page": x.get("foreign_landing_url") or x.get("detail_url"),
                    "creator": clean(x.get("creator")),
                    "attribution": clean(x.get("attribution")),
                    "license": lic, "license_version": x.get("license_version"),
                    "license_url": x.get("license_url"), "search_term": term,
                })
            time.sleep(1)
        provider_status[name] = {"status": "accessible", "images_downloaded": sum(x["provider"] == name for x in records)}
    except Exception as e:
        provider_status[name] = {"status": "failed_or_blocked", "error": str(e), "images_downloaded": sum(x["provider"] == name for x in records)}
        print(f"{name} unavailable; moving to next source: {e}")

def run_cleveland():
    name = "Cleveland Museum of Art Open Access"
    try:
        for term in TERMS:
            if len(records) >= TARGET: break
            data = get("https://openaccess-api.clevelandart.org/api/artworks/", {
                "q": term, "cc0": "", "has_image": 1, "limit": 50
            }).json()
            for x in data.get("data", []):
                if len(records) >= TARGET: break
                if x.get("share_license_status") != "CC0": continue
                image = x.get("images") or {}
                web = image.get("web") or {}
                url = web.get("url") or image.get("print", {}).get("url") or image.get("full", {}).get("url")
                if not url: continue
                add_candidate({
                    "provider": name, "title": clean(x.get("title")) or term,
                    "download_url": url, "source_page": x.get("url") or f"https://www.clevelandart.org/art/{x.get('id')}",
                    "creator": clean(x.get("creators", [{}])[0].get("description")) if x.get("creators") else "",
                    "license": "CC0", "license_url": "https://creativecommons.org/publicdomain/zero/1.0/",
                    "search_term": term, "accession_number": x.get("accession_number"),
                })
            time.sleep(1)
        provider_status[name] = {"status": "accessible", "images_downloaded": sum(x["provider"] == name for x in records)}
    except Exception as e:
        provider_status[name] = {"status": "failed_or_blocked", "error": str(e), "images_downloaded": sum(x["provider"] == name for x in records)}
        print(f"{name} unavailable; moving to next source: {e}")

def run_met():
    name = "The Metropolitan Museum of Art"
    try:
        for term in TERMS:
            if len(records) >= TARGET: break
            search = get("https://collectionapi.metmuseum.org/public/collection/v1/search", {
                "q": term, "hasImages": "true"
            }).json()
            for oid in (search.get("objectIDs") or [])[:35]:
                if len(records) >= TARGET: break
                obj = get(f"https://collectionapi.metmuseum.org/public/collection/v1/objects/{oid}").json()
                if not obj.get("isPublicDomain") or not obj.get("primaryImage"): continue
                add_candidate({
                    "provider": name, "title": clean(obj.get("title")) or term,
                    "download_url": obj.get("primaryImage"),
                    "source_page": obj.get("objectURL"),
                    "creator": clean(obj.get("artistDisplayName")),
                    "license": "Public Domain / CC0 Open Access",
                    "license_url": "https://www.metmuseum.org/about-the-met/policies-and-documents/open-access",
                    "search_term": term, "object_id": oid,
                })
                time.sleep(0.25)
            time.sleep(1)
        provider_status[name] = {"status": "accessible", "images_downloaded": sum(x["provider"] == name for x in records)}
    except Exception as e:
        provider_status[name] = {"status": "failed_or_blocked", "error": str(e), "images_downloaded": sum(x["provider"] == name for x in records)}
        print(f"{name} unavailable; moving to next source: {e}")

def run_artic():
    name = "Art Institute of Chicago"
    try:
        for term in TERMS:
            if len(records) >= TARGET: break
            data = get("https://api.artic.edu/api/v1/artworks/search", {
                "q": term, "limit": 50,
                "fields": "id,title,image_id,is_public_domain,artist_title,date_display,api_link"
            }).json()
            for x in data.get("data", []):
                if len(records) >= TARGET: break
                if x.get("is_public_domain") is not True or not x.get("image_id"): continue
                image_url = f"https://www.artic.edu/iiif/2/{x['image_id']}/full/843,/0/default.jpg"
                add_candidate({
                    "provider": name, "title": clean(x.get("title")) or term,
                    "download_url": image_url, "source_page": x.get("api_link"),
                    "creator": clean(x.get("artist_title")),
                    "license": "Public Domain", "license_url": "https://www.artic.edu/image-licensing",
                    "search_term": term, "artwork_id": x.get("id"),
                    "date": x.get("date_display"),
                })
                time.sleep(1)
        provider_status[name] = {"status": "accessible", "images_downloaded": sum(x["provider"] == name for x in records)}
    except Exception as e:
        provider_status[name] = {"status": "failed_or_blocked", "error": str(e), "images_downloaded": sum(x["provider"] == name for x in records)}
        print(f"{name} unavailable; moving to next source: {e}")

def run_commons():
    name = "Wikimedia Commons"
    try:
        for term in TERMS:
            if len(records) >= TARGET: break
            data = get("https://commons.wikimedia.org/w/api.php", {
                "action": "query", "format": "json", "generator": "search",
                "gsrsearch": f"filetype:bitmap {term}", "gsrnamespace": 6,
                "gsrlimit": 30, "prop": "imageinfo", "iiprop": "url|extmetadata"
            }).json()
            pages = data.get("query", {}).get("pages", {})
            for page in pages.values():
                if len(records) >= TARGET: break
                info_list = page.get("imageinfo", [])
                if not info_list: continue
                info = info_list[0]
                meta = info.get("extmetadata", {})
                lic = clean(meta.get("LicenseShortName", {}).get("value"))
                low = lic.lower()
                if any(x in low for x in ("noncommercial", "non-commercial", "no derivatives", "no-derivatives")): continue
                if not any(x in low for x in ("cc0", "public domain", "cc by", "cc-by")): continue
                title = page.get("title", term)
                add_candidate({
                    "provider": name, "title": title, "download_url": info.get("url"),
                    "source_page": "https://commons.wikimedia.org/wiki/" + quote(title.replace(" ", "_")),
                    "creator": clean(meta.get("Artist", {}).get("value")),
                    "attribution": clean(meta.get("Credit", {}).get("value")),
                    "license": lic, "license_url": clean(meta.get("LicenseUrl", {}).get("value")),
                    "search_term": term,
                })
            time.sleep(3)
        provider_status[name] = {"status": "accessible", "images_downloaded": sum(x["provider"] == name for x in records)}
    except Exception as e:
        provider_status[name] = {"status": "failed_or_blocked", "error": str(e), "images_downloaded": sum(x["provider"] == name for x in records)}
        print(f"{name} unavailable: {e}")

# Sequential fallback: a failed, blocked, rate-limited, or exhausted provider never stops later providers.
SOURCES = [run_openverse, run_cleveland, run_met, run_artic, run_commons]
for source in SOURCES:
    if len(records) >= TARGET: break
    source()
    print(f"Progress: {len(records)}/{TARGET} images after {source.__name__}")

result = {
    "requested_images": TARGET,
    "downloaded_images": len(records),
    "complete": len(records) == TARGET,
    "source_order": [f.__name__ for f in SOURCES],
    "provider_status": provider_status,
    "license_policy": "Accept only CC0, public-domain, or CC BY/CC BY-SA assets. Exclude non-commercial, no-derivatives, and unknown-license assets. Verify each individual record before commercial publication.",
    "images": records,
}
(OUT / "sources-and-licenses.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
(OUT / "README.md").write_text(
    "# HIBRAH — Multi-source 10-image download test\n\n"
    f"- Requested: {TARGET}\n- Downloaded: {len(records)}\n"
    f"- Complete: {'yes' if len(records) == TARGET else 'no'}\n\n"
    "Sequential fallback providers: Openverse, Cleveland Museum of Art Open Access, "
    "The Metropolitan Museum of Art Open Access, Art Institute of Chicago public-domain collection, "
    "then Wikimedia Commons. Provider failures do not stop the next provider. "
    "Source URLs, creator details and license metadata are recorded in sources-and-licenses.json. "
    "These are research assets, not approved HIBRAH campaign assets.\n",
    encoding="utf-8",
)
print(json.dumps(result, ensure_ascii=False, indent=2))
if len(records) != TARGET:
    print(f"INCOMPLETE: {len(records)}/{TARGET}; no false success reported.")
    sys.exit(1)
print(f"SUCCESS: all {TARGET} image files downloaded.")
