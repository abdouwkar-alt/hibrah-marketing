import hashlib, json, re, time
from pathlib import Path
from urllib.parse import urljoin, urlparse
from io import BytesIO
import requests
from bs4 import BeautifulSoup
from PIL import Image

ROOT = Path("research-assets/classic-menswear-library/00_incoming_pilot")
MANIFEST = ROOT / "sources-and-review.json"
TARGET = 100
MAX_BYTES = 8 * 1024 * 1024
MIN_SIDE = 400
HEADERS = {"User-Agent": "Mozilla/5.0 (compatible; HIBRAH-VisualResearch/1.2; internal reference review)", "Accept": "text/html,application/xhtml+xml,image/avif,image/webp,*/*"}
S = requests.Session()
S.headers.update(HEADERS)

# The original 49 records are preserved. These sources fill the three under-represented categories.
SOURCES = [
    {"category":"03_Jacket_Details","brand":"Gentleman's Gazette","url":"https://www.gentlemansgazette.com/the-anatomy-suit-jacket/","max":9},
    {"category":"03_Jacket_Details","brand":"Permanent Style","url":"https://www.permanentstyle.com/2020/07/the-guide-to-jacket-pockets.html","max":4},
    {"category":"03_Jacket_Details","brand":"Gentleman's Gazette","url":"https://www.gentlemansgazette.com/surgeons-cuffs-guide/","max":3},
    {"category":"03_Jacket_Details","brand":"Gentleman's Gazette","url":"https://www.gentlemansgazette.com/kiton-jackets-review/","max":3},
    {"category":"04_Trouser_Details","brand":"Gentleman's Gazette","url":"https://www.gentlemansgazette.com/features-of-a-pair-of-pants/","max":7},
    {"category":"04_Trouser_Details","brand":"Gentleman's Gazette","url":"https://www.gentlemansgazette.com/how-pants-should-fit/","max":5},
    {"category":"04_Trouser_Details","brand":"Permanent Style","url":"https://www.permanentstyle.com/2020/01/suit-style-9-trouser-cuffs-belts-and-tabs.html","max":3},
    {"category":"04_Trouser_Details","brand":"Permanent Style","url":"https://www.permanentstyle.com/2019/11/suit-style-7-a-guide-to-pleats-on-trousers.html","max":3},
    {"category":"05_Complete_Outfits","brand":"Drake's","url":"https://www.drakes.com/blogs/news/the-winter-lookbook-26","max":5},
    {"category":"05_Complete_Outfits","brand":"Drake's","url":"https://www.drakes.com/blogs/news/the-spring-lookbook","max":4},
    {"category":"05_Complete_Outfits","brand":"Drake's","url":"https://us.drakes.com/blogs/news/the-summer-tailoring-lookbook-1","max":3},
    {"category":"05_Complete_Outfits","brand":"Drake's","url":"https://www.drakes.com/blogs/news/drake-s-by-aaron-levine-the-lookbook","max":2},
]

def slug(s):
    s = re.sub(r"[^A-Za-z0-9_-]+", "_", s).strip("_")
    return (s[:65] or "reference")

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
    title = soup.title.get_text(" ", strip=True) if soup.title else page_url
    scope = soup.find("article") or soup.find("main") or soup
    found, seen = [], set()
    # Include page hero image, then images inside the article body.
    for tag in soup.find_all("meta"):
        key = (tag.get("property") or tag.get("name") or "").lower()
        if key in ("og:image", "twitter:image", "twitter:image:src"):
            u = urljoin(page_url, tag.get("content", ""))
            if u and u not in seen:
                seen.add(u); found.append({"image_url":u,"alt":title[:300],"page_title":title[:300]})
    for tag in scope.find_all(["img", "source"]):
        alt = tag.get("alt") or tag.get("title") or ""
        u = tag.get("src") or tag.get("data-src") or tag.get("data-original") or tag.get("data-lazy-src") or tag.get("data-image") or ""
        if tag.get("srcset"): u = best_srcset(tag.get("srcset")) or u
        if not u or u.startswith("data:"): continue
        u = urljoin(page_url, u)
        if u in seen: continue
        seen.add(u)
        low = (u + " " + alt).lower()
        if any(x in low for x in ("logo","icon","sprite","avatar","flag","payment","placeholder","badge","advert")): continue
        if not re.search(r"\.(jpe?g|png|webp)(\?|$)", u, re.I) and "cdn" not in urlparse(u).netloc and "image" not in low: continue
        found.append({"image_url":u,"alt":alt[:300],"page_title":title[:300]})
    return found

def fetch_image(url, page_url):
    r = S.get(url, headers={"Referer":page_url}, timeout=25)
    r.raise_for_status()
    ctype = r.headers.get("Content-Type","").split(";")[0].lower()
    if not ctype.startswith("image/"): raise ValueError("not image: " + ctype)
    data = r.content
    if len(data) > MAX_BYTES: raise ValueError("over size limit")
    im = Image.open(BytesIO(data)); im.verify()
    im = Image.open(BytesIO(data))
    if min(im.size) < MIN_SIDE: raise ValueError("image too small")
    ext = {"JPEG":".jpg","PNG":".png","WEBP":".webp"}.get((im.format or "").upper())
    if not ext: raise ValueError("unsupported format")
    return data, ext, im.size

def acceptable(category, item):
    text = (item["image_url"] + " " + item["alt"] + " " + item["page_title"]).lower()
    if category == "03_Jacket_Details":
        return any(k in text for k in ("jacket","lapel","pocket","shoulder","sleeve","canvas","button","collar","anatomy","tailor","tailoring","kiton","cuff"))
    if category == "04_Trouser_Details":
        return any(k in text for k in ("pant","trouser","waistband","pleat","cuff","turn-up","turnup","side-adjust","rise","hem","fit"))
    return True

manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
records = manifest.get("images", [])
existing_hashes = {x.get("sha256") for x in records if x.get("sha256")}
existing_urls = {x.get("image_url") for x in records if x.get("image_url")}
counts = {k:0 for k in ("01_Fabrics","02_Suits","03_Jacket_Details","04_Trouser_Details","05_Complete_Outfits")}
for x in records: counts[x["category"]] = counts.get(x["category"],0) + 1
errors, page_results = [], []
next_index = len(records) + 1
remaining = TARGET - len(records)

for source in SOURCES:
    if remaining <= 0: break
    cat = source["category"]
    desired = {"03_Jacket_Details":19,"04_Trouser_Details":18,"05_Complete_Outfits":14}[cat]
    if counts.get(cat,0) >= desired: continue
    page = source["url"]
    result = {"source_page":page,"brand":source["brand"],"category":cat,"downloaded":0,"status":"pending"}
    try:
        response = S.get(page, timeout=30); response.raise_for_status()
        for item in candidates(page, response.text):
            if remaining <= 0 or result["downloaded"] >= source["max"]: break
            if item["image_url"] in existing_urls or not acceptable(cat,item): continue
            try:
                data, ext, dimensions = fetch_image(item["image_url"],page)
                digest = hashlib.sha256(data).hexdigest()
                if digest in existing_hashes: continue
                filename = f"{next_index:03d}_{slug(source['brand'])}_{slug(item['alt'] or item['page_title'])}{ext}"
                folder = ROOT / cat; folder.mkdir(parents=True,exist_ok=True)
                target = folder / filename
                target.write_bytes(data)
                records.append({"file":str(target),"category":cat,"source_brand":source["brand"],"source_page":page,
                    "image_url":item["image_url"],"alt_text":item["alt"],"page_title":item["page_title"],
                    "bytes":len(data),"dimensions":list(dimensions),"sha256":digest,
                    "rights_status":"NOT_CLEARED","intended_use":"INTERNAL_CREATIVE_REVIEW_ONLY",
                    "creative_review_status":"PENDING"})
                existing_hashes.add(digest); existing_urls.add(item["image_url"])
                counts[cat] = counts.get(cat,0)+1; next_index += 1; remaining -= 1
                result["downloaded"] += 1
                print(f"ADDED {len(records)}/{TARGET}: {filename} {dimensions}")
            except Exception as exc:
                errors.append({"page":page,"image_url":item["image_url"],"error":str(exc)[:250]})
        result["status"] = "images_found" if result["downloaded"] else "no_new_usable_images"
    except Exception as exc:
        result["status"] = "page_failed"; result["error"] = str(exc)[:250]
        errors.append({"page":page,"error":str(exc)[:250]})
    page_results.append(result)
    time.sleep(0.4)

manifest["target"] = TARGET
manifest["downloaded"] = len(records)
manifest["complete"] = len(records) == TARGET
manifest["category_counts"] = counts
manifest["note"] = "All images are for internal Creative review only. Rights are NOT CLEARED; do not publish, redistribute, or use in campaigns until item-level terms and rights are reviewed."
manifest.setdefault("sources", []).extend(page_results)
manifest.setdefault("errors", []).extend(errors)
manifest["images"] = records
MANIFEST.write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding="utf-8")

readme = [
"# HIBRAH — Specialist Classic Menswear Image Pilot","",
f"- Target: {TARGET}",f"- Current total: {len(records)}",f"- Complete: {'yes' if len(records)==TARGET else 'no'}","",
"## Category counts",""
]
readme += [f"- {k}: {v}" for k,v in counts.items()]
readme += ["","## Review status","- All records remain PENDING for Creative review.","- All records remain NOT_CLEARED for rights.","- Internal reference review only; no publication or redistribution before item-level rights review.",""]
(ROOT/"README.md").write_text("\n".join(readme),encoding="utf-8")
print(json.dumps({"total":len(records),"added":len(records)-(next_index-1-(len(records)-(next_index-1))),"remaining":remaining,"category_counts":counts,"complete":len(records)==TARGET,"sources":page_results,"errors":errors},ensure_ascii=False,indent=2))
if len(records) < TARGET:
    raise SystemExit(f"Collection incomplete: {len(records)}/{TARGET}; existing files preserved.")
