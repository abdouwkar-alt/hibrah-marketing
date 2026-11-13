import csv, hashlib, io, json, re, time
from pathlib import Path
from urllib.parse import urljoin
import requests
from bs4 import BeautifulSoup
from PIL import Image, ImageDraw, ImageFont

ROOT = Path("research-assets/classic-menswear-library/01_fabric_visual_review_100")
IMG_DIR = ROOT / "images"
TARGET = 100
MAX_BYTES = 10 * 1024 * 1024
MIN_SIDE = 420
HEADERS = {"User-Agent": "Mozilla/5.0 (compatible; HIBRAH-ReferenceResearch/1.0)", "Accept": "application/json,text/html,image/avif,image/webp,*/*"}
SESSION = requests.Session()
SESSION.headers.update(HEADERS)

# Specialist clothmakers and established suiting-fabric retailers. One lead image per product.
SOURCES = [
    {"name":"Fox Brothers","url":"https://foxflannel.com/collections/all-cloth/products.json?limit=250&page=1"},
    {"name":"Standeven Fabrics","url":"https://www.standevenfabrics.co.uk/collections/all/products.json?limit=250&page=1"},
    {"name":"Yorkshire Fabric","url":"https://www.yorkshirefabric.com/collections/all/products.json?limit=250&page=1"},
    {"name":"Shivas Fabrics","url":"https://www.shivasfabrics.com/collections/all/products.json?limit=250&page=1"},
    {"name":"New Tess","url":"https://shop.newtess.com/collections/all/products.json?limit=250&page=1"},
    {"name":"B&J Fabrics","url":"https://www.bandjfabrics.com/fabric"},
    {"name":"Giovani Fabrics","url":"https://giovanifabrics.com/"},
    {"name":"Marlen","url":"https://www.marlen.cz/"},
]
INCLUDE = re.compile(r"\b(wool|suiting|suit|flannel|tweed|worsted|merino|lambswool|herringbone|houndstooth|sharkskin|birdseye|glen|prince of wales|windowpane|chalk stripe|pinstripe|cloth|fabric)\b", re.I)
EXCLUDE = re.compile(r"\b(shirt|tie|scarf|curtain|upholstery|carpet|coat|overcoat|knitwear|sweater|dress|linen only|cotton only|denim|leather|accessory)\b", re.I)

def get_json_products(url):
    r = SESSION.get(url, timeout=35)
    r.raise_for_status()
    data = r.json()
    products = data.get("products", [])
    return products

def shopify_candidates(source):
    products = get_json_products(source["url"])
    out = []
    for p in products:
        title = str(p.get("title") or "")
        body = BeautifulSoup(str(p.get("body_html") or ""), "html.parser").get_text(" ", strip=True)
        tags = p.get("tags") or ""
        desc = " ".join([title, body[:1200], str(tags)])
        if not INCLUDE.search(desc) or EXCLUDE.search(title):
            continue
        imgs = p.get("images") or []
        if not imgs:
            continue
        # Only the first product image to avoid repeated angles of one cloth.
        image = imgs[0]
        out.append({
            "brand":source["name"], "product_title":title,
            "source_page":p.get("handle") and source["url"].split("/collections/")[0] + "/products/" + p["handle"] or source["url"],
            "image_url":image.get("src") or "", "alt_text":image.get("alt") or title,
            "metadata":desc[:1000]
        })
    return out

def html_candidates(source):
    r = SESSION.get(source["url"], timeout=35)
    r.raise_for_status()
    soup = BeautifulSoup(r.text, "html.parser")
    out, seen = [], set()
    for img in soup.find_all("img"):
        alt = img.get("alt") or img.get("title") or ""
        src = img.get("data-src") or img.get("data-original") or img.get("src") or ""
        if img.get("srcset"):
            src = img.get("srcset").split(",")[-1].strip().split()[0]
        if not src or src.startswith("data:"):
            continue
        src = urljoin(source["url"], src)
        key = (src, alt)
        if key in seen or not INCLUDE.search(alt):
            continue
        seen.add(key)
        out.append({"brand":source["name"],"product_title":alt,"source_page":source["url"],"image_url":src,"alt_text":alt,"metadata":alt})
    return out

def download_image(item):
    r = SESSION.get(item["image_url"], headers={"Referer":item["source_page"]}, timeout=35, stream=True)
    r.raise_for_status()
    ctype = r.headers.get("Content-Type","").split(";")[0].lower()
    if not ctype.startswith("image/"):
        raise ValueError("not an image: " + ctype)
    b = bytearray()
    for chunk in r.iter_content(65536):
        if chunk:
            b.extend(chunk)
            if len(b) > MAX_BYTES:
                raise ValueError("image exceeds 10 MB")
    im = Image.open(io.BytesIO(b)); im.verify()
    im = Image.open(io.BytesIO(b))
    if min(im.size) < MIN_SIDE:
        raise ValueError("below minimum dimensions")
    fmt = (im.format or "").upper()
    ext = {"JPEG":".jpg","PNG":".png","WEBP":".webp"}.get(fmt)
    if not ext:
        raise ValueError("unsupported image format " + fmt)
    return bytes(b), ext, im.size

def slug(s):
    return re.sub(r"[^A-Za-z0-9_-]+","_",s).strip("_")[:65] or "fabric"

def contact_sheets(records):
    font = ImageFont.load_default()
    for start in range(0, len(records), 20):
        batch = records[start:start+20]
        w,h,cols,rows = 240,210,4,5
        sheet = Image.new("RGB",(w*cols,h*rows),"white")
        draw = ImageDraw.Draw(sheet)
        for j,item in enumerate(batch):
            im = Image.open(item["file"]).convert("RGB")
            im.thumbnail((w-12,h-42))
            x=(j%cols)*w+(w-im.width)//2
            y=(j//cols)*h+4
            sheet.paste(im,(x,y))
            label=f'{item["id"]:03d} | {item["brand"][:18]}'
            draw.text(((j%cols)*w+6,(j//cols)*h+h-32),label,fill="black",font=font)
            draw.text(((j%cols)*w+6,(j//cols)*h+h-18),item["product_title"][:30],fill="black",font=font)
        sheet.save(ROOT / f"contact_sheet_{start//20+1:02d}.jpg",quality=88)

ROOT.mkdir(parents=True,exist_ok=True)
IMG_DIR.mkdir(parents=True,exist_ok=True)
records, errors, hashes, urls = [], [], set(), set()
for source in SOURCES:
    if len(records) >= TARGET: break
    try:
        if ".json?" in source["url"]:
            candidates = shopify_candidates(source)
        else:
            candidates = html_candidates(source)
        print(f'{source["name"]}: {len(candidates)} candidates')
        for item in candidates:
            if len(records) >= TARGET: break
            if not item["image_url"] or item["image_url"] in urls: continue
            try:
                data, ext, dims = download_image(item)
                digest = hashlib.sha256(data).hexdigest()
                if digest in hashes: continue
                idx=len(records)+1
                name=f'{idx:03d}_{slug(item["brand"])}_{slug(item["product_title"])}{ext}'
                path=IMG_DIR/name
                path.write_bytes(data)
                record={
                    "id":idx,"file":str(path),"brand":item["brand"],
                    "product_title":item["product_title"],"category":"Fabrics / Suiting Cloth",
                    "source_page":item["source_page"],"image_url":item["image_url"],
                    "alt_text":item["alt_text"],"source_metadata":item["metadata"],
                    "bytes":len(data),"dimensions":list(dims),"sha256":digest,
                    "visual_description":"","visual_quality":"PENDING",
                    "logo_watermark":"PENDING","duplicate_check":"PENDING",
                    "creative_review_status":"PENDING","rights_status":"NOT_CLEARED",
                    "intended_use":"INTERNAL_CREATIVE_REVIEW_ONLY"
                }
                records.append(record); hashes.add(digest); urls.add(item["image_url"])
                print(f'ADDED {idx}/100 {name} {dims}')
            except Exception as e:
                errors.append({"brand":item["brand"],"url":item["image_url"],"error":str(e)[:220]})
        time.sleep(0.5)
    except Exception as e:
        errors.append({"brand":source["name"],"source":source["url"],"error":str(e)[:300]})
        print("SOURCE FAILED",source["name"],str(e)[:200])

(ROOT/"manifest.json").write_text(json.dumps({
    "title":"HIBRAH — 100 Fabric Reference Images",
    "target":TARGET,"downloaded":len(records),"complete":len(records)==TARGET,
    "sources":SOURCES,"images":records,"errors":errors,
    "review_note":"Visual review fields are pending until contact sheets are inspected. Rights are not cleared. Internal review only."
},ensure_ascii=False,indent=2),encoding="utf-8")

with (ROOT/"manifest.csv").open("w",newline="",encoding="utf-8-sig") as f:
    fields=["id","file","brand","product_title","category","source_page","image_url","alt_text","dimensions","visual_description","visual_quality","logo_watermark","duplicate_check","creative_review_status","rights_status"]
    wr=csv.DictWriter(f,fieldnames=fields); wr.writeheader()
    for x in records:
        row={k:x.get(k,"") for k in fields}
        row["dimensions"]="x".join(map(str,x["dimensions"]))
        wr.writerow(row)

contact_sheets(records)
print(json.dumps({"downloaded":len(records),"complete":len(records)==TARGET,"errors":len(errors),"brands":{b:sum(1 for x in records if x["brand"]==b) for b in sorted(set(x["brand"] for x in records))}},ensure_ascii=False,indent=2))
if len(records) < TARGET:
    raise SystemExit(f"Only {len(records)}/100 images downloaded; inspect manifest errors and add fallback sources before continuing.")
