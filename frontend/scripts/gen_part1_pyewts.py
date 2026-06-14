import json
from pathlib import Path

import pyewts

c = pyewts.pyewts()

items = [
    (
        3,
        "mon pa'i sa a ma mi rigs glu",
        "bod kyi mon pa rigs kyi srol rgyun glu rol ste mtsho sna grong khyer gyi leb khul na khyab po yod",
    ),
    (
        4,
        "gur glu",
        "dmangs srol rtsom rig dang chos lugs rol dbyangs mnyam sbyor byas pa ste bod rgyal rabs dus kyi kha brgyud tsig rtsom yin",
    ),
    (
        5,
        "glod khags pi wang bod stong",
        "glod khags sa khul gyi srol rgyun pi wang phag mas gtong ba'i rol gzhas yin",
    ),
    (
        10,
        "gzhis ka rgya gzhas",
        "chen po'i bod kyi srol rgyun glu dang gar yin te bod sa khul gyi khyad chos rgyas pa yin",
    ),
    (
        11,
        "gzhis ka smar phrug",
        "dmangs srol brdung ba ste phyi gzhis brdung zer yang grags pa'i srol rgyun rtsal yin",
    ),
    (
        12,
        "lho kha rgyud chag brdung gar",
        "srol rgyun smyag brdung ba ste lho kha phyongs rgyas rdzong du khyab pa yod",
    ),
    (
        13,
        "gu ge zhuon gar",
        "mnga ris sa khul gyi srol rgyun pho brang gar ste bod zlos gar dang gar bshad glu sogs dang mthun sbyar ba'i rtsal yin",
    ),
    (
        14,
        "lha sa'i nang ma",
        "glu dang gar gyi khyad chos mthun gyi srol rgyun rtsal yin",
    ),
    (
        15,
        "shis rong sbrul rtsi",
        "chen po'i 'chad rtsal gar ste lha sa stod lung bde chen rdzong du khyab pa yod",
    ),
    (
        16,
        "a gu ston pa brdung gar",
        "bod sa khul ga zhir khyab pa'i srol rgyun brdung gar yin",
    ),
    (
        17,
        "rwa sheng khrab pa",
        "rwa sheng sa khul gyi srol rgyun gar rtsal yin",
    ),
    (
        18,
        "stod gzhas (la rtse stod gzhas)",
        "bod lugs brdabs gar zer grags shing la rtse rdzong du khyab pa yod",
    ),
    (
        19,
        "gzhas chen",
        "chen po'i srol rgyun gzhas chen mnyam gar ste dus chen dang chos sgo'i dus thabs su 'brel ba'i srol ldan rtsal yin",
    ),
    (
        20,
        "rnam gling thub rgyal gzhas chen",
        "rnam gling rdzong gi thub rgyal yul sde'i srol rgyun gzhas chen mnyam gar yin",
    ),
    (
        21,
        "lha sa snar ru gzhas chen",
        "lha sa'i snar ru sa khul gyi srol rgyun gzhas chen mnyam gar yin",
    ),
    (
        22,
        "nyi ma xiang gzhas chen",
        "nyi ma xiang gi srol rgyun gzhas chen mnyam gar yin",
    ),
    (
        23,
        "a gzhas",
        "srol rgyun las rgyun khyer ba'i glu gar ste bod sa khul ga zhig tu khyab pa yod",
    ),
    (
        24,
        "mang khams rgyud pa gsum gyi gar",
        "rgyud pa gsum gyis khyer ba'i srol rgyun gar ste mang khams sa khul du khyab pa yod",
    ),
    (
        25,
        "smin na 'cham",
        "srol rgyun chos gar 'cham ste bod sa khul ga zhig tu khyab pa yod",
    ),
    (
        26,
        "mgo rtses gar",
        "lo brgya stong drug brgya lhag gi srol rgyun gar yin te kha brgyud brgyud pa rgyun 'dzin gyi srol ldan yin",
    ),
    (
        27,
        "zhing thang shar pa'i glu gar",
        "gding rgyal rdzong zhing thang shar pa mi rigs kyi srol rgyun glu gar yin",
    ),
    (
        28,
        "rgyal ri a gu ston pa brdung gar",
        "gna khri rgyal ri rdzong gi srol rgyun brdung gar yin",
    ),
    (
        29,
        "spu rgyal zhuon gos dar gar",
        "mnga ris spu rgyal rdzong gi srol rgyun gar ste khyad par gyi gos dar rig rgyal dang mthun sbyar ba yin",
    ),
    (
        32,
        "gzhis ka 'phyong ba",
        "gzhis ka sa khul gyi srol rgyun bod zlos gar gyi rgyun lugs gcig yin",
    ),
    (
        33,
        "gzhis ka rnam gling zhang ba",
        "rnam gling rdzong gi srol rgyun bod zlos gar gyi rgyun lugs gcig yin",
    ),
    (
        34,
        "gzhis ka rnam pa ljang dkar",
        "rnam pa rdzong gi srol rgyun bod zlos gar gyi rgyun lugs gcig yin",
    ),
    (
        35,
        "lho kha yar klung bkra shis zhal pa",
        "yar klung sa khul gyi srol rgyun bod zlos gar gyi rgyun lugs gcig yin",
    ),
]

translations = []
for item_id, name_wylie, desc_wylie in items:
    name = c.toUnicode(name_wylie).rstrip("/")
    description = c.toUnicode(desc_wylie).rstrip("/")
    if any(ch.isascii() and ch.isalpha() for ch in name + description):
        raise ValueError(f"Latin remains in id {item_id}: {name} | {description}")
    translations.append({"id": item_id, "name": name, "description": description})

out = Path(__file__).with_name("national-bo-overlay-part1.json")
out.write_text(json.dumps({"translations": translations}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Wrote {len(translations)} translations to {out}")
