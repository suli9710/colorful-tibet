# -*- coding: utf-8 -*-
import json
import re
from pathlib import Path

translations = [
    {
        "id": 36,
        "name": "ལྷོ་ཁ་འཕྱོང་རྒྱas bka brgyud bkra shis dpal don",
        "description": "ལྷོ་ཁ་འphyong rgyas rdzong gi srol rgyun bod zlos gar gyi rgyun lugs gcig",
    },
]

out = Path(__file__).with_name("national-bo-overlay-part2.json")
for item in translations:
    for key in ("name", "description"):
        if re.search(r"[A-Za-z]", item[key]):
            raise SystemExit(f"Latin in id {item['id']} {key}")

out.write_text(json.dumps({"translations": translations}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(len(translations))
