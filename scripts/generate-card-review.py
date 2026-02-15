#!/usr/bin/env python3
"""Generate an HTML review page for all card images in the project."""

import glob
import os
import subprocess
import sys

SCRIPTS = os.path.dirname(os.path.abspath(__file__))
BASE = os.path.dirname(SCRIPTS)
OUT = os.path.join(SCRIPTS, "card-image-review.html")

KNOWN_ISSUES = {
    "ximedes.png": "Not a card (text banner)",
    "szt_card.png": "Not a card (logo)",
    "zolotayakorona.png": "Not a card (logo)",
    "waltti_logo.svg": "Not a card (logo)",
    "city_union.svg": "Not a card (logo)",
}

SKIP = {"marker_start.png", "marker_end.png", "ic_cards_stack.svg"}
EXTENSIONS = {".png", ".jpeg", ".jpg", ".svg"}


def get_dimensions(path):
    if path.endswith(".svg"):
        return "SVG", "SVG"
    r = subprocess.run(
        ["sips", "-g", "pixelWidth", "-g", "pixelHeight", path],
        capture_output=True,
        text=True,
    )
    w = h = "?"
    for line in r.stdout.split("\n"):
        if "pixelWidth" in line:
            w = line.split()[-1]
        if "pixelHeight" in line:
            h = line.split()[-1]
    return w, h


def collect_images():
    images = []
    for pattern in [
        "farebot-transit-*/src/commonMain/composeResources/drawable/*",
        "farebot-app/src/commonMain/composeResources/drawable/*",
    ]:
        for f in sorted(glob.glob(os.path.join(BASE, pattern))):
            bn = os.path.basename(f)
            if bn in SKIP:
                continue
            if not any(bn.endswith(e) for e in EXTENSIONS):
                continue
            relpath = os.path.relpath(f, BASE)
            module = relpath.split("/")[0]
            size = os.path.getsize(f)
            images.append((module, bn, f, relpath, size))
    return images


def detect_issues(bn, ext, w, h):
    issues = []
    if ext == "jpeg":
        issues.append("JPEG (no transparency)")
    if w not in ("SVG", "?"):
        wi, hi = int(w), int(h)
        if wi > 600:
            issues.append(f"Oversized ({wi}px wide)")
        if 0 < wi < 380:
            issues.append(f"Low resolution ({wi}px)")
        if hi > wi:
            issues.append("Portrait orientation")
    if bn in KNOWN_ISSUES:
        issues.append(KNOWN_ISSUES[bn])
    return issues


def generate_html(images):
    html = []
    html.append(
        """<!DOCTYPE html>
<html><head><meta charset="utf-8">
<title>FareBot Card Image Review</title>
<style>
body { font-family: -apple-system, sans-serif; background: #1a1a1a; color: #ddd; margin: 20px; }
h1 { color: #fff; }
h2 { color: #aaa; margin-top: 30px; border-bottom: 1px solid #333; padding-bottom: 5px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.card { background: #2a2a2a; border-radius: 12px; overflow: hidden; border: 1px solid #333; }
.card.issue { border: 2px solid #ff6b6b; }
.card.ok { }
body.issues-only .card.ok { display: none; }
body.issues-only h2.no-issues { display: none; }
.card-img-wrapper {
  aspect-ratio: 1.586; overflow: hidden; display: flex; align-items: center; justify-content: center;
  background: repeating-conic-gradient(#333 0% 25%, #2a2a2a 0% 50%) 50% / 16px 16px;
}
.card-img-wrapper img { max-width: 100%; max-height: 100%; object-fit: contain; }
.card-info { padding: 8px 10px; font-size: 11px; }
.card-info .name { font-weight: bold; font-size: 13px; color: #fff; }
.card-info .dims { color: #888; }
.card-info .path { color: #555; font-size: 10px; word-break: break-all; }
.card-info .issue-label { color: #ff6b6b; font-weight: bold; }
.toolbar { background: #222; padding: 12px; border-radius: 8px; margin-bottom: 20px; display: flex; align-items: center; gap: 20px; }
.toggle { display: flex; align-items: center; gap: 8px; cursor: pointer; user-select: none; }
.toggle input { width: 18px; height: 18px; accent-color: #ff6b6b; cursor: pointer; }
.legend-item { display: inline-flex; align-items: center; gap: 4px; }
.count { color: #888; font-size: 13px; }
</style></head><body>
<h1>FareBot Card Image Review</h1>
<div class="toolbar">
  <label class="toggle">
    <input type="checkbox" id="issuesToggle" onchange="toggleIssues()">
    <span>Show issues only</span>
  </label>
  <span class="legend-item"><span style="color:#ff6b6b">&#9679;</span> Red border = flagged issue</span>
  <span class="legend-item">Checkerboard = transparency</span>
  <span class="count" id="countLabel"></span>
</div>
<script>
function toggleIssues() {
  var on = document.getElementById('issuesToggle').checked;
  document.body.classList.toggle('issues-only', on);
  updateCount();
}
function updateCount() {
  var all = document.querySelectorAll('.card');
  var issues = document.querySelectorAll('.card.issue');
  var on = document.getElementById('issuesToggle').checked;
  var label = document.getElementById('countLabel');
  if (on) {
    label.textContent = issues.length + ' issues';
  } else {
    label.textContent = all.length + ' total, ' + issues.length + ' with issues';
  }
}
document.addEventListener('DOMContentLoaded', updateCount);
</script>"""
    )

    # Pre-compute issues per module to know which headers to hide
    module_has_issues = {}
    image_data = []
    for module, bn, abspath, relpath, size in images:
        w, h = get_dimensions(abspath)
        ext = bn.rsplit(".", 1)[-1]
        issues = detect_issues(bn, ext, w, h)
        image_data.append((module, bn, abspath, relpath, size, w, h, ext, issues))
        if issues:
            module_has_issues[module] = True

    prev_module = None
    for module, bn, abspath, relpath, size, w, h, ext, issues in image_data:
        if module != prev_module:
            if prev_module is not None:
                html.append("</div>")
            hcls = "" if module in module_has_issues else " no-issues"
            html.append(f'<h2 class="{hcls.strip()}">{module}</h2>')
            html.append('<div class="grid">')
            prev_module = module

        hsize = f"{size // 1024}KB" if size > 1024 else f"{size}B"
        cls = " issue" if issues else " ok"
        issue_html = (
            f'<div class="issue-label">{" | ".join(issues)}</div>' if issues else ""
        )

        html.append(
            f"""<div class="card{cls}">
  <div class="card-img-wrapper"><img src="file://{abspath}" alt="{bn}"></div>
  <div class="card-info">
    <div class="name">{bn}</div>
    <div class="dims">{w}x{h} &middot; {ext} &middot; {hsize}</div>
    {issue_html}
    <div class="path">{relpath}</div>
  </div></div>"""
        )

    html.append("</div></body></html>")
    return "\n".join(html)


def main():
    images = collect_images()
    html = generate_html(images)
    with open(OUT, "w") as f:
        f.write(html)
    print(f"Generated {OUT} with {len(images)} images")

    if "--open" in sys.argv:
        subprocess.run(["open", OUT])


if __name__ == "__main__":
    main()
