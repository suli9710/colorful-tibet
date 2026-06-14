# -*- coding: utf-8 -*-
import json
from pathlib import Path

import pyewts

c = pyewts.pyewts()

items = [
    (
        36,
        "lho kha 'phyong rgyas bka' brgyud bkra shis dpal 'don",
        "lho kha 'phyong rgyas rdzong gi srol rgyun bod zlos gar gyi rgyun lugs gcig",
    ),
    (
        37,
        "lho kha mon pa'i zlos gar",
        "lho kha mtsho sna rdzong leb yul khongs kyi mon pa rigs kyi srol rgyun zlos gar yin/ 2007 lor zlos pa tshogs pa bskyar sgrigs byas rjes mi dgu'i 'tshon grwa ru chang ba yin",
    ),
    (
        38,
        "'brug gung",
        "srol rgyun zlos gar gyi rnam pa gcig ste bod sa cha phyogs re res dar khyab",
    ),
    (
        41,
        "sman thang ri brgyud",
        "lo bcu drug pa'i dus su sman bla don grub rgyal mtshan gyis btsugs shing/ thig le bde legs dang mdangs gsal ba'i phyir grags che",
    ),
    (
        42,
        "mkhyen brgyud",
        "srol rgyun thang ka ri brgyud chen po gcig/ thig le zhib mo dang tshon mdangs sbyar ba la mkhas",
    ),
    (
        43,
        "karma dga' brgyad ri brgyud",
        "srol rgyun thang ka ri brgyud gcig/ mdangs gsal dang thig phra ba'i khyad chos can",
    ),
    (
        44,
        "dbus brgyud",
        "srol rgyun thang ka ri brgyud rnying shos shig/ thig drags dang ri mo'i khyad chos ldan",
    ),
    (
        45,
        "lha sa gtor rtse thang ka",
        "lha sa'i srol rgyun dar 'bru thang ka bzo ba'i lag rtsal",
    ),
    (
        46,
        "khams sman sa thang ka",
        "khams yul gyi thang ka ri brgyud khyad chos can",
    ),
    (
        48,
        "bod rigs zangs gzor bzo rgyal",
        "bod kyi srol rgyun zangs rngul gzor bzo ba'i lag rtsal",
    ),
    (
        49,
        "bod lcags gzor bzo rgyal",
        "bod kyi srol rgyun lcags mdung gzor bzo ba'i lag rtsal",
    ),
    (
        50,
        "rdzi sdong zangs bzo rgyal",
        "gzhis ka rtse rdzi sdong rdzong gi srol rgyun zangs rngul gzor bzo ba'i lag rtsal",
    ),
    (
        51,
        "bkra shis dgyes mdzes gser dngul zangs gzor bzo rgyal",
        "lha sa bkra shis dgyes mdzes khang tshong gi srol rgyun gser dngul dang zangs rngul gzor bzo ba'i lag rtsal",
    ),
    (
        52,
        "tsha tsha bzo rgyal",
        "srol rgyun chos lugs kyi sgyu rtsal bzo rgyal yin/ sangs rgyas sku gzugs chung ngu bzo ba'i lag rtsal",
    ),
    (
        53,
        "srol rgyun sbra bzo rgyal",
        "nag chu ba chen rdzong gi srol rgyun sbra ba rtsal bzo ba'i lag rtsal",
    ),
    (
        54,
        "bod rigs pang den dang ka gdan 'thag bzo rgyal",
        "bod kyi srol rgyun ras bzo lag rtsal yin/ pang den (pang kheb) dang ka gdan (sa gdan) bzo ba byed",
    ),
    (
        55,
        "lha sa rgyal mi chu gtan 'thag",
        "chu rgyun gyi stobs kyis 'bru btus pa'i srol rgyun lag rtsal/ bod mi'i blo gros dang bzo rgol nus pa mngon",
    ),
    (
        56,
        "bod rigs shing par par rgyal",
        "srol rgyun par rgyal lag rtsal yin/ bod yig dpe rnying dpag tu med pa srung skyob byas yod",
    ),
    (
        57,
        "bod lcags gzor bzo rgyal",
        "gzhis ka rtse bzhad mthong smon rdzong gi srol rgyun lcags mdung gzor bzo ba'i lag rtsal",
    ),
    (
        58,
        "bod rigs deb bris lag rtsal",
        "srol rgyun lag rgyal gyi deb bris bzo lag rtsal/ sa gnas khyad par gyi rtswa shing sbyor len byed",
    ),
    (
        59,
        "bod rigs srol rgyun sa gzugs bzo rgyal",
        "bod kyi srol rgyun bzo lag rtsal yin/ sku rten dang lag bris bzo rgyal la sbyor",
    ),
    (
        61,
        "rtse thar 'thag bzo rgyal",
        "lho kha snye mdo rdzong gi srol rgyun bal 'thag lag rtsal/ bod snum gos kyi nya tsho zhes grags",
    ),
    (
        62,
        "bod rigs gra nang shing bzo rgyal",
        "gra nang rdzong gi srol rgyun shing bzo lag rtsal/ sku rten dang khyim cha bzo ba la sbyor",
    ),
    (
        63,
        "tshwa bskams rgyal",
        "sman khams rdzong gi srol rgyun tshwa bzo lag rtsal/ bod mi dang rang byung mthun sgril gyi shes rab mngon",
    ),
    (
        64,
        "med tog rdo thag bzo rgyal",
        "nying khri med tog rdzong gi srol rgyun bzo lag rtsal/ 2015 lor rgyal khab kyis bsrung skyob sa mtshon byed ngos 'dzin bzo rgyal du gsol 'debs gtong yod",
    ),
    (
        65,
        "bod rigs srol rgyun mar 'bru ngnan lag rtsal",
        "gzhis ka rtse rgyal rtse rdzong gi srol rgyun mar 'bru ngnan lag rtsal/ shing las bzo ba'i ngnan 'khor sbyor len byed",
    ),
]

translations = []
for item_id, name_wylie, desc_wylie in items:
    name = c.toUnicode(name_wylie).rstrip("/")
    description = c.toUnicode(desc_wylie).rstrip("/")
    if any(ch.isascii() and ch.isalpha() for ch in name + description):
        raise ValueError(f"Latin remains in id {item_id}: {name} | {description}")
    translations.append({"id": item_id, "name": name, "description": description})

out = Path(__file__).with_name("national-bo-overlay-part2.json")
out.write_text(json.dumps({"translations": translations}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Wrote {len(translations)} translations to {out}")
