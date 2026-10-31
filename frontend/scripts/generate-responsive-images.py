"""Offline asset preparation only. Requires Pillow; retains original artwork and attribution."""
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageOps
root = Path(__file__).resolve().parents[1]
public = root / 'public'
output = public / 'optimized' / 'heritage'
output.mkdir(parents=True, exist_ok=True)
manifest = {}
for source in sorted((public / 'heritage').iterdir()):
    if source.suffix.lower() not in {'.jpg', '.jpeg', '.png'} or source.stat().st_size < 150_000:
        continue
    with Image.open(source) as original:
        image = ImageOps.exif_transpose(original).convert('RGB')
        variants = []
        for width in sorted({min(640, image.width), min(1280, image.width)}):
            resized = image.copy()
            resized.thumbnail((width, min(1280, round(image.height * width / image.width))))
            name = hashlib.sha256(source.name.encode()).hexdigest()[:12] + f'-{resized.width}.webp'
            target = output / name
            resized.save(target, 'WEBP', quality=74, method=6)
            if any(item['width'] == resized.width for item in variants):
                continue
            variants.append({'src': '/optimized/heritage/' + name, 'width': resized.width, 'height': resized.height, 'bytes': target.stat().st_size})
        manifest['/heritage/' + source.name] = variants
(root / 'src' / 'data' / 'responsiveImages.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')
print(f'Prepared {len(manifest)} local artworks. Originals preserved.')
