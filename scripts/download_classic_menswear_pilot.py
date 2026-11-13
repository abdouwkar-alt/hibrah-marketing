import hashlib, json, re, time
from pathlib import Path
from urllib.parse import urljoin, urlparse
import requests
from bs4 import BeautifulSoup
from PIL import Image
from io import BytesIO

ROOT = Path("research-assets/classic-menswear-library/00_incoming_pilot")
TARGET = 100
QUOTAS = {
    "01_Fabrics": 20,
    "02_Suits": 20,
    "03_Jacket_Details": 20,
    "04_Trouser_Details": 20,
    "05_Complete_Outfits": 20,
}
MIN_SIDE = 420
MAX_BYTES = 8 * 1024 * 1024
HEADERS = {"User-Agent": "HIBRAH-VisualResearch/1.1 (curated internal reference review)", "Accept": "text/html,application/xhtml+xml,image/avif,image/webp,image/apng,*/*"}
S = requests.Session()
S.headers.update(HEADERS)

SOURCES = [
    {"category":"01_Fabrics","brand":"Vitale Barberis Canonico","url":"https://vitalebarberiscanonico.com/collection/fabrics/10-712-276/","max":20},
    {"category":"01_Fabrics","brand":"Vitale Barberis Canonico","url":"https://vitalebarberiscanonico.com/collection/fabrics/10-712-311/","max":2},
    {"category":"01_Fabrics","brand":"Reda 1865","url":"https://store.reda1865.com/fabrics/","max":20},
    {"category":"01_Fabrics","brand":"Scabal","url":"https://scabal.com/pages/fabrics-collection-ss26","max":5},
    {"category":"02_Suits","brand":"Drake's","url":"https://us.drakes.com/blogs/news/tailoring-in-paris","max":20},
    {"category":"02_Suits","brand":"Drake's","url":"https://us.drakes.com/blogs/news/the-summer-tailoring-lookbook","max":4},
    {"category":"02_Suits","brand":"Drake's","url":"https://us.drakes.com/blogs/news/the-spring-games-edit","max":4},
    {"category":"03_Jacket_Details","brand":"Gentleman's Gazette","url":"https://www.gentlemansgazette.com/the-anatomy-suit-jacket/","max":5},
    {"category":"04_Trouser_Details","brand":"Gentleman's Gazette","url":"https://www.gentlemansgazette.com/how-a-suit-should-fit/","max":5},
    {"category":"05_Complete_Outfits","brand":"Permanent Style","url":"https://www.permanentstyle.com/style-lookbook","max":5},
    {"category":"05_Complete_Outfits","brand":"Drake's","url":"https://www.drakes.com/blogs/news/the-autumn-collection-part-one-lookbook","max":5},
]

def best_srcset(value):
    parts = [p.strip().split() for p in value.split(",") if p.strip()]
    if not parts: return ""
    def rank(p):
        if len(p) < 2: return 0
        m = re.match(r"(\d+)", p[1])
        return int(m.group(1)) if m else 0
    return max(parts, key=rank)[0]

def candidates(page_url, html):
    soup = BeautifulSoup(html, "html.parser")
    title = (soup.title.get_text(" ", strip=True) if soup.title else page_url)
    found, seen = [], set()
    for tag in soup.find_all(["meta", "img", "source"]):
        url = ""
        alt = ""
        if tag.name == "meta":
            key = (tag.get("property") or tag.get("name") or "").lower()
            if key in ("og:image", "twitter:image", "twitter:image:src"):
                url = tag.get("content", "")
                alt = title
        else:
            alt = tag.get("alt") or tag.get("title") or ""
            url = tag.get("src") or tag.get("data-src") or tag.get("data-original") or tag.get("data-lazy-src") or ""
            if tag.get("srcset"):
                url = best_srcset(tag.get("srcset")) or url
        if not url or url.startswith("data:"): continue
        url = urljoin(page_url, url)
        if url in seen: continue
        seen.add(url)
        low = url.lower() + " " + alt.lower()
        if any(x in low for x in ("logo", "icon", "sprite", "avatar", "flag", "payment", "placeholder", "badge")): continue
        if not re.search(r"\.(jpe?g|png|webp)(\?|$)", url, re.I) and "image" not in low and "cdn" not in urlparse(url).netloc:
            continue
        found.append({"image_url":url, "alt":alt[:300], "page_title":title[:300]})
    return found

def relevant(category, item):
    text = (item["image_url"] + " " + item["alt"] + " " + item["page_title"]).lower()
    if category == "01_Fabrics":
        if any(x in text for x in ("history", "archives", "showroom", "production process", "main banner", "menu collection")):
            return False
        return any(x in text for x in ("flat", "cover", "fabric", "wool", "hopsack", "flannel", "cloth", "swatch", "textile", "fabrics_ss26"))
    if category == "02_Suits":
        if any(x in text for x in ("waxed_jacket", "chore_jacket", "puffer", "knitwear", "overcoat")):
            return False
        return any(x in text for x in ("tailoring", "suit", "jacket", "lookbook", "spring_2026", "jago_layout", "paris"))
    if category == "03_Jacket_Details":
        return any(x in text for x in ("jacket", "lapel", "shoulder", "sleeve", "collar", "canvas", "pocket", "anatomy", "suit"))
    if category == "04_Trouser_Details":
        return any(x in text for x in ("trouser", "pants", "pant", "inseam", "waist", "rise", "break", "suspenders", "fit", "suit"))
    return True

def fetch_image(url, page_url):
    r = S.get(url, headers={"Referer": page_url}, timeout=25, stream=True)
    r.raise_for_status()
    ctype = r.headers.get("Content-Type", "").split(";")[0].lower()
    if not ctype.startswith("image/"):
        r.close()
        raise ValueError("response is not an image: " + ctype)
    data = bytearray()
    for chunk in r.iter_content(65536):
        if chunk:
            data.extend(chunk)
            if len(data) > MAX_BYTES:
                r.close()
                raise ValueError("image exceeds size limit")
    r.close()
    im = Image.open(BytesIO(data))
    im.verify()
    im = Image.open(BytesIO(data))
    if min(im.size) < MIN_SIDE:
        raise ValueError(f"image too small: {im.size}")
    fmt = (im.format or "").upper()
    ext = {"JPEG":".jpg","PNG":".png","WEBP":".webp","GIF":".gif"}.get(fmt)
    if not ext: raise ValueError("unsupported image format " + fmt)
    return bytes(data), ext, im.size

def slug(s):
    s = re.sub(r"[^A-Za-z0-9_-]+", "_", s).strip("_")
    return (s[:55] or "reference")

for p in ROOT.glob("*"):
    if p.is_dir():
        for f in p.iterdir():
            if f.is_file(): f.unlink()
        p.rmdir()
    elif p.is_file(): p.unlink()
ROOT.mkdir(parents=True, exist_ok=True)

records, hashes, errors, page_results = [], set(), [], []
category_counts = {k:0 for k in QUOTAS}
for source in SOURCES:
    category = source["category"]
    if category_counts[category] >= QUOTAS[category]: continue
    page = source["url"]
    result = {"source_page":page, "brand":source["brand"], "category":category, "downloaded":0, "status":"pending"}
    try:
        response = S.get(page, timeout=25)
        response.raise_for_status()
        options = candidates(page, response.text)
        for candidate in options:
            if category_counts[category] >= QUOTAS[category] or result["downloaded"] >= source["max"]: break
            if not relevant(category, candidate): continue
            try:
                data, ext, dimensions = fetch_image(candidate["image_url"], page)
                digest = hashlib.sha256(data).hexdigest()
                if digest in hashes: continue
                hashes.add(digest)
                idx = len(records) + 1
                folder = ROOT / category
                folder.mkdir(parents=True, exist_ok=True)
                name = f"{idx:02d}_{slug(source['brand'])}_{slug(candidate['alt'] or candidate['page_title'])}{ext}"
                path = folder / name
                path.write_bytes(data)
                records.append({
                    "file":str(path), "category":category, "source_brand":source["brand"],
                    "source_page":page, "image_url":candidate["image_url"], "alt_text":candidate["alt"],
                    "page_title":candidate["page_title"], "bytes":len(data), "dimensions":list(dimensions),
                    "sha256":digest, "rights_status":"NOT_CLEARED",
                    "intended_use":"INTERNAL_CREATIVE_REVIEW_ONLY",
                    "creative_review_status":"PENDING"
                })
                category_counts[category] += 1
                result["downloaded"] += 1
                print(f"DOWNLOADED {len(records)}/{TARGET}: {path} {dimensions}")
            except Exception as exc:
                errors.append({"page":page,"image_url":candidate["image_url"],"error":str(exc)[:250]})
        result["status"] = "images_found" if result["downloaded"] else "no_relevant_images"
    except Exception as exc:
        result["status"] = "page_failed"
        result["error"] = str(exc)[:250]
        errors.append({"page":page,"error":str(exc)[:250]})
    page_results.append(result)
    time.sleep(0.5)

complete = all(category_counts[k] >= QUOTAS[k] for k in QUOTAS)
manifest = {
    "test":"HIBRAH specialist classic menswear image pilot — balanced categories",
    "target":TARGET,
    "downloaded":len(records),
    "complete":complete,
    "category_counts":category_counts,
    "note":"Files are for internal Creative review only. Rights are NOT CLEARED; do not publish or redistribute until item-level terms and rights are reviewed.",
    "sources":page_results,
    "images":records,
    "errors":errors
}
(ROOT / "sources-and-review.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding="utf-8")
(ROOT / "README.md").write_text(
    "# HIBRAH — Specialist Classic Menswear Image Pilot\n\n"
    f"- Target: {TARGET}\n- Downloaded: {len(records)}\n- Complete category coverage: {'yes' if complete else 'no'}\n\n"
    "## Category counts\n" + "\n".join(f"- {k}: {category_counts[k]}/{QUOTAS[k]}" for k in QUOTAS) + "\n\n"
    "## Creative review required\n"
    "Inspect every image for visual relevance, quality, duplicates, correct category and usefulness to HIBRAH content. "
    "Record ACCEPT / REJECT / RECLASSIFY in sources-and-review.json before any Canva handoff.\n\n"
    "**Rights status: NOT CLEARED.** Internal research only; no publication or redistribution before item-level rights review.\n",
    encoding="utf-8"
)
print(json.dumps({"downloaded":len(records),"target":TARGET,"category_counts":category_counts,"complete":complete,"sources":page_results,"errors":errors},ensure_ascii=False,indent=2))
if not records:
    raise SystemExit("No usable images downloaded")
