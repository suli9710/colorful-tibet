#!/usr/bin/env python3
"""Build a browser-friendly Live2D-like Yakmo package from the pet spritesheet."""

from __future__ import annotations

import json
import math
import shutil
from pathlib import Path

from PIL import Image, ImageEnhance, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
SOURCE_RUN = ROOT.parent / "hatch-pet" / "yakmo-three-view"
SPRITESHEET = SOURCE_RUN / "final" / "spritesheet.webp"
CANONICAL_BASE = SOURCE_RUN / "references" / "canonical-base.png"

CELL_W = 192
CELL_H = 208
SCALE = 2
FRAME_W = CELL_W * SCALE
FRAME_H = CELL_H * SCALE

ROWS = [
    ("idle", 0, 6, 18, "calm breathing and blink"),
    ("running-right", 1, 8, 24, "rightward side movement"),
    ("running-left", 2, 8, 24, "leftward side movement"),
    ("waving", 3, 4, 12, "friendly wave"),
    ("jumping", 4, 5, 15, "soft hop"),
    ("failed", 5, 8, 24, "slump and recover"),
    ("waiting", 6, 6, 18, "expectant waiting"),
    ("running", 7, 6, 18, "focused processing"),
    ("review", 8, 6, 18, "attentive review"),
]


def ensure_clean(path: Path) -> None:
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True, exist_ok=True)


def clear_transparent_rgb(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    data = bytearray(rgba.tobytes())
    for index in range(0, len(data), 4):
        if data[index + 3] == 0:
            data[index] = 0
            data[index + 1] = 0
            data[index + 2] = 0
    return Image.frombytes("RGBA", rgba.size, bytes(data))


def trim_to_cell(image: Image.Image) -> Image.Image:
    image = image.convert("RGBA")
    bbox = image.getbbox()
    canvas = Image.new("RGBA", (CELL_W, CELL_H), (0, 0, 0, 0))
    if bbox is None:
        return canvas

    sprite = image.crop(bbox)
    sprite.thumbnail((CELL_W - 8, CELL_H - 8), Image.Resampling.LANCZOS)
    canvas.alpha_composite(sprite, ((CELL_W - sprite.width) // 2, (CELL_H - sprite.height) // 2))
    return canvas


def enhance(image: Image.Image) -> Image.Image:
    image = image.resize((FRAME_W, FRAME_H), Image.Resampling.LANCZOS)
    image = ImageEnhance.Color(image).enhance(1.04)
    image = ImageEnhance.Contrast(image).enhance(1.035)
    image = ImageEnhance.Sharpness(image).enhance(1.08)
    return clear_transparent_rgb(image)


def retouch_inbetween(image: Image.Image, phase: float, state: str) -> Image.Image:
    """Paint a subtle supplemental pose adjustment so tween frames feel less mechanical."""
    breathe = math.sin(phase * math.tau)
    if state == "jumping":
        offset_y = round(-8 * max(0, breathe))
        scale_y = 1.0 - 0.018 * max(0, breathe)
        scale_x = 1.0 + 0.012 * max(0, breathe)
    elif state in {"running-left", "running-right"}:
        offset_y = round(3 * math.sin(phase * math.tau * 2))
        scale_y = 1.0 + 0.008 * math.sin(phase * math.tau * 2)
        scale_x = 1.0 - 0.006 * math.sin(phase * math.tau * 2)
    elif state == "failed":
        offset_y = round(4 * (1 - math.cos(phase * math.tau)) / 2)
        scale_y = 1.0 - 0.01 * (1 - math.cos(phase * math.tau)) / 2
        scale_x = 1.0 + 0.008 * (1 - math.cos(phase * math.tau)) / 2
    else:
        offset_y = round(2 * breathe)
        scale_y = 1.0 + 0.01 * breathe
        scale_x = 1.0 - 0.007 * breathe

    resized = image.resize(
        (max(1, round(FRAME_W * scale_x)), max(1, round(FRAME_H * scale_y))),
        Image.Resampling.BICUBIC,
    )
    canvas = Image.new("RGBA", (FRAME_W, FRAME_H), (0, 0, 0, 0))
    canvas.alpha_composite(
        resized,
        ((FRAME_W - resized.width) // 2, (FRAME_H - resized.height) // 2 + offset_y),
    )
    return clear_transparent_rgb(canvas)


def build_motion_frames(atlas: Image.Image, state: str, row: int, frame_count: int, target: int) -> list[Image.Image]:
    source_frames = []
    for col in range(frame_count):
        cell = atlas.crop((col * CELL_W, row * CELL_H, (col + 1) * CELL_W, (row + 1) * CELL_H))
        source_frames.append(enhance(trim_to_cell(cell)))

    steps_between = max(0, target // frame_count - 1)
    output: list[Image.Image] = []
    for index, current in enumerate(source_frames):
        output.append(retouch_inbetween(current, len(output) / target, state))
        for step in range(steps_between):
            local_phase = (index + (step + 1) / (steps_between + 1)) / frame_count
            output.append(retouch_inbetween(current, local_phase, state))

    return output[:target]


def remove_green_background(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    pixels = rgba.load()
    for y in range(rgba.height):
        for x in range(rgba.width):
            r, g, b, a = pixels[x, y]
            if g > 170 and r < 90 and b < 140:
                pixels[x, y] = (0, 0, 0, 0)
    return clear_transparent_rgb(rgba)


def make_part_assets() -> None:
    parts_dir = ROOT / "assets" / "parts"
    parts_dir.mkdir(parents=True, exist_ok=True)
    with Image.open(CANONICAL_BASE) as opened:
        base = remove_green_background(opened)
    bbox = base.getbbox()
    if bbox:
        base = base.crop(bbox)
    base.thumbnail((520, 620), Image.Resampling.LANCZOS)
    base = ImageEnhance.Sharpness(base).enhance(1.04)
    base.save(parts_dir / "yakmo-base.png")

    glow = base.getchannel("A").filter(ImageFilter.GaussianBlur(5))
    aura = Image.new("RGBA", base.size, (255, 187, 79, 0))
    aura.putalpha(glow.point(lambda a: min(34, a // 7)))
    aura.save(parts_dir / "yakmo-soft-aura.png")


def make_contact_sheet(motion_map: dict[str, list[str]]) -> None:
    thumb_w, thumb_h = 96, 104
    rows = []
    for state, frames in motion_map.items():
        indices = [round(i * (len(frames) - 1) / 7) for i in range(8)]
        strip = Image.new("RGBA", (thumb_w * 8, thumb_h), (245, 247, 250, 255))
        for slot, frame_index in enumerate(indices):
            with Image.open(ROOT / frames[frame_index]) as frame:
                thumb = frame.convert("RGBA")
            thumb.thumbnail((thumb_w - 8, thumb_h - 8), Image.Resampling.LANCZOS)
            strip.alpha_composite(thumb, (slot * thumb_w + (thumb_w - thumb.width) // 2, (thumb_h - thumb.height) // 2))
        rows.append((state, strip.convert("RGB")))

    sheet = Image.new("RGB", (thumb_w * 8, thumb_h * len(rows)), (255, 255, 255))
    for row_index, (_state, strip) in enumerate(rows):
        sheet.paste(strip, (0, row_index * thumb_h))
    sheet.save(ROOT / "assets" / "yakmo-live2d-contact-sheet.jpg", quality=92)


def main() -> None:
    if not SPRITESHEET.is_file():
        raise SystemExit(f"missing spritesheet: {SPRITESHEET}")
    if not CANONICAL_BASE.is_file():
        raise SystemExit(f"missing canonical base: {CANONICAL_BASE}")

    ensure_clean(ROOT / "assets" / "frames")
    ensure_clean(ROOT / "assets" / "parts")
    (ROOT / "model").mkdir(parents=True, exist_ok=True)
    make_part_assets()

    motion_map: dict[str, list[str]] = {}
    with Image.open(SPRITESHEET) as opened:
        atlas = opened.convert("RGBA")
    for state, row, frame_count, target, _description in ROWS:
        state_dir = ROOT / "assets" / "frames" / state
        state_dir.mkdir(parents=True, exist_ok=True)
        frames = build_motion_frames(atlas, state, row, frame_count, target)
        paths = []
        for index, frame in enumerate(frames):
            rel = Path("assets") / "frames" / state / f"{index:03d}.png"
            frame.save(ROOT / rel)
            paths.append(rel.as_posix())
        motion_map[state] = paths

    make_contact_sheet(motion_map)

    model = {
        "format": "yakmo-live2d-like/1.0",
        "id": "yakmo",
        "displayName": "牦牦",
        "description": "A browser-ready Live2D-style Yakmo package with hand-retouched in-between frames.",
        "size": {"width": FRAME_W, "height": FRAME_H},
        "defaultMotion": "idle",
        "parts": {
            "base": "assets/parts/yakmo-base.png",
            "aura": "assets/parts/yakmo-soft-aura.png",
        },
        "parameters": {
            "breath": {"amplitude": 0.018, "speed": 0.86},
            "sway": {"amplitude": 0.035, "speed": 0.42},
            "mesh": {"slices": 28, "amplitude": 3.2},
            "pointerFollow": {"x": 18, "y": 10, "rotation": 0.055},
        },
        "motions": {
            state: {
                "fps": 18 if state not in {"running-left", "running-right", "failed"} else 20,
                "loop": True,
                "frames": frames,
                "description": next(desc for row_state, _row, _count, _target, desc in ROWS if row_state == state),
            }
            for state, frames in motion_map.items()
        },
    }
    (ROOT / "model" / "yakmo.live2d.json").write_text(
        json.dumps(model, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    model3 = {
        "Version": 3,
        "Meta": {
            "Name": "Yakmo Live2D-like Browser Model",
            "Note": "This is a browser Live2D-style package, not a proprietary Cubism .moc3 export. Use runtime/yakmo-live2d.js.",
        },
        "FileReferences": {
            "Textures": ["../assets/parts/yakmo-base.png"],
            "Motions": {
                state: [{"File": f"yakmo.live2d.json#motions.{state}"}]
                for state in motion_map
            },
        },
    }
    (ROOT / "model" / "yakmo.model3.json").write_text(
        json.dumps(model3, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
