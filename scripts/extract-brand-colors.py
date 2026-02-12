#!/usr/bin/env python3
"""
Extract dominant brand color from each card image.

Outputs Kotlin-ready brandColor values for CardInfo definitions.
Skips near-white, near-black, and low-saturation colors to find the
most "brand-like" color in each image.

SVG files are rendered to PNG via rsvg-convert before analysis.

Usage:
    python3 scripts/extract-brand-colors.py
"""

import colorsys
import io
import subprocess
from collections import Counter
from pathlib import Path
from PIL import Image

DRAWABLE_DIR = Path(__file__).parent.parent / "farebot-app/src/commonMain/composeResources/drawable"

SUPPORTED_RASTER = {".png", ".jpg", ".jpeg"}
SUPPORTED_SVG = {".svg"}

# Skip map markers and other non-card images
SKIP = {"marker_start", "marker_end"}


def rgb_to_hsv(r, g, b):
    return colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)


def is_brandable(r, g, b):
    """Return True if the color is saturated/vivid enough to be a brand color."""
    h, s, v = rgb_to_hsv(r, g, b)
    # Skip near-white, near-black, and desaturated grays
    if v < 0.15 or (v > 0.95 and s < 0.1):
        return False
    if s < 0.2:
        return False
    return True


def quantize_color(r, g, b, step=16):
    """Bucket colors to reduce noise."""
    return (r // step * step, g // step * step, b // step * step)


def load_image(path):
    """Load an image file, rendering SVGs to PNG first."""
    if path.suffix.lower() in SUPPORTED_SVG:
        result = subprocess.run(
            ["rsvg-convert", "--width=320", "--format=png", str(path)],
            capture_output=True,
        )
        if result.returncode != 0:
            return None
        return Image.open(io.BytesIO(result.stdout)).convert("RGBA")
    else:
        return Image.open(path).convert("RGBA")


def dominant_color(img_path):
    """Extract the most prominent brand-like color from an image."""
    img = load_image(img_path)
    if img is None:
        return None

    # Downsample for speed
    img = img.resize((80, 50), Image.LANCZOS)
    pixels = list(img.getdata())

    counts = Counter()
    for r, g, b, a in pixels:
        if a < 128:
            continue
        if not is_brandable(r, g, b):
            continue
        q = quantize_color(r, g, b)
        counts[q] += 1

    if not counts:
        return None

    # Pick the most frequent saturated color
    top = counts.most_common(1)[0][0]

    # Refine: average all pixels that quantized to this bucket
    rsum, gsum, bsum, n = 0, 0, 0, 0
    for r, g, b, a in pixels:
        if a < 128:
            continue
        if quantize_color(r, g, b) == top:
            rsum += r
            gsum += g
            bsum += b
            n += 1
    if n == 0:
        return top
    return (rsum // n, gsum // n, bsum // n)


def main():
    if not DRAWABLE_DIR.is_dir():
        print(f"Error: {DRAWABLE_DIR} not found")
        return

    results = []
    for path in sorted(DRAWABLE_DIR.iterdir()):
        stem = path.stem
        if stem in SKIP:
            continue
        suffix = path.suffix.lower()
        if suffix not in SUPPORTED_RASTER | SUPPORTED_SVG:
            continue

        color = dominant_color(path)
        if color is None:
            results.append((stem, None))
            continue

        r, g, b = color
        hex_val = f"0x{r:02X}{g:02X}{b:02X}"
        results.append((stem, hex_val))

    # Print as a table
    print(f"{'Image':<35} {'brandColor':<12} {'Preview'}")
    print("-" * 60)
    for name, hex_val in results:
        if hex_val:
            r = int(hex_val[2:4], 16)
            g = int(hex_val[4:6], 16)
            b = int(hex_val[6:8], 16)
            print(f"{name:<35} {hex_val:<12} \033[48;2;{r};{g};{b}m     \033[0m")
        else:
            print(f"{name:<35} {'(none)':<12}")


if __name__ == "__main__":
    main()
