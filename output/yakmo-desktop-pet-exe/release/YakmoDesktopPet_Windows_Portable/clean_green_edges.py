#!/usr/bin/env python3
"""Remove chroma-key green fringe from Yakmo transparent PNG assets."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageFilter


def is_key_green(r: int, g: int, b: int) -> bool:
    return g > 135 and r < 185 and b < 185 and g - r > 42 and g - b > 42


def is_green_spill(r: int, g: int, b: int) -> bool:
    return g > 130 and b < 180 and g - r > 8 and g - b > 24


def is_hard_key_green(r: int, g: int, b: int) -> bool:
    return g > 165 and r < 145 and b < 155 and g - r > 62 and g - b > 62


def edge_mask(alpha: Image.Image) -> bytearray:
    width, height = alpha.size
    data = alpha.tobytes()
    mask = bytearray(width * height)
    for y in range(height):
        row = y * width
        for x in range(width):
            index = row + x
            if data[index] == 0:
                continue
            if data[index] < 245:
                mask[index] = 1
                continue
            for yy in range(max(0, y - 2), min(height, y + 3)):
                base = yy * width
                for xx in range(max(0, x - 2), min(width, x + 3)):
                    if data[base + xx] == 0:
                        mask[index] = 1
                        break
                if mask[index]:
                    break
    return mask


def contract_alpha(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    alpha = rgba.getchannel("A").filter(ImageFilter.MinFilter(3))
    data = bytearray(rgba.tobytes())
    alpha_data = alpha.tobytes()
    contracted = 0
    for index in range(0, len(data), 4):
        pixel_index = index // 4
        old_alpha = data[index + 3]
        new_alpha = min(old_alpha, alpha_data[pixel_index])
        if new_alpha != old_alpha:
            contracted += 1
        data[index + 3] = new_alpha
        if new_alpha == 0:
            data[index] = 0
            data[index + 1] = 0
            data[index + 2] = 0
    output = Image.frombytes("RGBA", rgba.size, bytes(data))
    output.info["contracted"] = contracted
    return output


def remove_specks(image: Image.Image, threshold: int) -> Image.Image:
    if threshold <= 0:
        image.info["specks_removed"] = 0
        return image
    rgba = image.convert("RGBA")
    width, height = rgba.size
    alpha = rgba.getchannel("A").tobytes()
    visited = bytearray(width * height)
    data = bytearray(rgba.tobytes())
    removed = 0

    for start, value in enumerate(alpha):
        if value == 0 or visited[start]:
            continue
        stack = [start]
        visited[start] = 1
        component: list[int] = []
        while stack:
            current = stack.pop()
            component.append(current)
            x = current % width
            neighbors = []
            if x > 0:
                neighbors.append(current - 1)
            if x + 1 < width:
                neighbors.append(current + 1)
            if current >= width:
                neighbors.append(current - width)
            if current + width < width * height:
                neighbors.append(current + width)
            for neighbor in neighbors:
                if not visited[neighbor] and alpha[neighbor] > 0:
                    visited[neighbor] = 1
                    stack.append(neighbor)
        if len(component) < threshold:
            removed += len(component)
            for pixel_index in component:
                index = pixel_index * 4
                data[index] = 0
                data[index + 1] = 0
                data[index + 2] = 0
                data[index + 3] = 0

    output = Image.frombytes("RGBA", rgba.size, bytes(data))
    output.info["specks_removed"] = removed
    return output


def clean_image(path: Path, contract: bool, speck_threshold: int) -> dict[str, int]:
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    alpha = image.getchannel("A")
    edges = edge_mask(alpha)
    pixels = bytearray(image.tobytes())

    removed = 0
    despilled = 0
    green_before = 0
    green_after = 0

    for index in range(0, len(pixels), 4):
        r, g, b, a = pixels[index : index + 4]
        pixel_index = index // 4
        if a == 0:
            pixels[index] = 0
            pixels[index + 1] = 0
            pixels[index + 2] = 0
            continue

        if is_key_green(r, g, b):
            green_before += 1

        if edges[pixel_index] and (
            is_hard_key_green(r, g, b)
            or (is_green_spill(r, g, b) and a < 230)
            or (is_green_spill(r, g, b) and g - r > 34 and b < 145)
        ):
            pixels[index + 3] = 0
            pixels[index] = 0
            pixels[index + 1] = 0
            pixels[index + 2] = 0
            removed += 1
            continue

        if edges[pixel_index] and (is_key_green(r, g, b) or is_green_spill(r, g, b)):
            # Pull green spill back toward the warmer neighboring pet palette.
            cap = max(b + 34, r - 8)
            if g > cap:
                pixels[index + 1] = cap
                despilled += 1
            if r < 120 and b < 120:
                pixels[index + 3] = max(0, min(a, 180))
                despilled += 1

        r2, g2, b2, a2 = pixels[index : index + 4]
        if a2 and is_key_green(r2, g2, b2):
            green_after += 1

    cleaned = Image.frombytes("RGBA", (width, height), bytes(pixels))
    contracted = 0
    if contract:
        cleaned = contract_alpha(cleaned)
        contracted = int(cleaned.info.get("contracted", 0))
    cleaned = remove_specks(cleaned, speck_threshold)
    specks_removed = int(cleaned.info.get("specks_removed", 0))
    cleaned.save(path)
    return {
        "removed": removed,
        "despilled": despilled,
        "contracted": contracted,
        "specks_removed": specks_removed,
        "green_before": green_before,
        "green_after": green_after,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("roots", nargs="+")
    parser.add_argument("--contract-alpha", action="store_true")
    parser.add_argument("--remove-specks-under", type=int, default=0)
    args = parser.parse_args()

    summary = {
        "files": 0,
        "changed_files": 0,
        "removed": 0,
        "despilled": 0,
        "contracted": 0,
        "specks_removed": 0,
        "green_before": 0,
        "green_after": 0,
    }

    for raw_root in args.roots:
        root = Path(raw_root).expanduser().resolve()
        for path in root.rglob("*.png"):
            result = clean_image(path, args.contract_alpha, args.remove_specks_under)
            summary["files"] += 1
            for key in [
                "removed",
                "despilled",
                "contracted",
                "specks_removed",
                "green_before",
                "green_after",
            ]:
                summary[key] += result[key]
            if (
                result["removed"]
                or result["despilled"]
                or result["contracted"]
                or result["specks_removed"]
            ):
                summary["changed_files"] += 1

    print(json.dumps(summary, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
