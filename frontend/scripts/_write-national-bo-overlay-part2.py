# -*- coding: utf-8 -*-
import json
import re
from pathlib import Path

translations = [
    {
        "id": 36,
        "name": "ལྷོ་ཁ་འཕྱོང་རྒྱས་བཀའ་བརྒྱུད་བཀྲ་ཤིས་དཔལ་འདོན།",
        "description": "ལྷོ་ཁ་འཕྱོང་རྒྱས་རྫོང་གི་སྲོལ་རྒྱུན་བོད་ཟློས་གar་གyི་རྒyuན་ལuགས་གcig",
    },
    {
        "id": 37,
        "name": "ལྷོ་ཁ་མon་པa'i་ཟླoས_gar",
        "description": "ལྷོ་ཁ་ས་ཁul mtsho sna rdzong lho ba sa khul gyi mon pa rgyud pa'i srol rgyun zlos gar yin no 2007 lor zlos tshogs pa bskyar gsos byas rjes mi dgu yod pa'i khrab tshogs su gtod",
    },
    {
        "id": 38,
        "name": "འbrug gung",
        "description": "སrol rgyun zlos gar gyi rnam pa gcig ste bod sa khul ga re zhig tu dar ba yin",
    },
    {
        "id": 41,
        "name": "སman thang ri brgyud",
        "description": "lo bcu drug pa'i skabs su sman bla don grub rgya mtshos btsugs pa'i thang ka ri mo'i rgyud lugs yin la thig le legs bsdams dang mdangs gsal ba'i khyad chos yod",
    },
    {
        "id": 42,
        "name": "མkhyen brgyud",
        "description": "སrol rgyun thang ka ri mo'i rgyud lugs gcig ste thig le zhib dang mdangs kha sbyor la mkhas pa yin",
    },
    {
        "id": 43,
        "name": "ཀar ma dga bzhi rgyud",
        "description": "སrol rgyun thang ka ri mo'i rgyud lugs gcig ste mdangs gsal dang thig le zhan pa'i khyad chos can",
    },
    {
        "id": 44,
        "name": "ཆos byung brgyud",
        "description": "སrol rgyun thang ka ri mo'i rgyud lugs rnying shos gcig ste thig le shig drags dang ri mo'i khyad chos ldan",
    },
    {
        "id": 45,
        "name": "ལha sa gtor rtse thang ka",
        "description": "ལha sa'i srol rgyun dar bre thang ka bzo ba'i lag rgyal",
    },
    {
        "id": 46,
        "name": "ཁhams sman sa thang ka",
        "description": "ཁhams sa khul gyi thang ka ri mo'i khyad chos can gyi rgyud lugs",
    },
    {
        "id": 48,
        "name": "བod rigs zangs gzor bzo rgyal",
        "description": "བod kyi srol rgyun zangs rngul gzor bzo ba'i lag rgyal",
    },
    {
        "id": 49,
        "name": "བod lcags gzor bzo rgyal",
        "description": "བod kyi srol rgyun lcags gri gzor bzo ba'i lag rgyal",
    },
    {
        "id": 50,
        "name": "རdzi sdong zangs bzo rgyal",
        "description": "གzhis ka rtse rdzi sdong rdzong gi srol rgyun zangs rngul gzor bzo lag rgyal",
    },
    {
        "id": 51,
        "name": "བkra shis dgyas mdzes gser dngul zangs gzor bzo rgyal",
        "description": "ལha sa bkra shis dgyas mdzes khang tshong gi srol rgyun gser dngul dang zangs rngul gzor bzo lag rgyal",
    },
    {
        "id": 52,
        "name": "ཚa tsha bzo rgyal",
        "description": "སrol rgyun chos lugs kyi lag rgyal yin la sangs rgyas sku gzugs chung ngu bzo ba'i bzo rgyal yin",
    },
    {
        "id": 53,
        "name": "སrol rgyun sbra bzo rgyal",
        "description": "སnag chu sbrang rgyas rdzong gi srol rgyun sbra ba thag bzo lag rgyal",
    },
    {
        "id": 54,
        "name": "བod rigs pang den dang ka gdan thag bzo rgyal",
        "description": "བod kyi srol rgyun thag bzo lag rgyal yin la pang den dang ka gdan bzo ba byed",
    },
    {
        "id": 55,
        "name": "ལha sa rgyal mi chu gtan thag",
        "description": "སrol rgyun chu rgyal gyi bras btus lag rgyal yin la bod mi'i shes rab dang bzo rgol nus pa mngon",
    },
    {
        "id": 56,
        "name": "བod rigs shing par par rgyal",
        "description": "སrol rgyun par rgyal lag rgyal yin la bod yig dpe rnying dpag tu med pa srung skyob byed",
    },
    {
        "id": 57,
        "name": "བod lcags gzor bzo rgyal",
        "description": "གzhis ka rtse bzhad mthong smon rdzong gi srol rgyun lcags gri gzor bzo lag rgyal",
    },
    {
        "id": 58,
        "name": "བod rigs shing lo rgyal lag rgyal",
        "description": "སrol rgyun lag rgyal gyi shing lo rgyal lag rgyal yin la sa gnas khyad par gyi rtswa shing sbyor len byed",
    },
    {
        "id": 59,
        "name": "བod rigs srol rgyun sa gzugs bzo rgyal",
        "description": "བod kyi srol rgyun bzo rgyal yin la sku rten dang lag bris bzo rgyal la sbyor",
    },
    {
        "id": 61,
        "name": "རtse thar thag rgyal",
        "description": "ལho kha snye mdo rdzong gi srol rgyun bal thag lag rgyal yin la bod snum gos kyi nya tsho zhes grags",
    },
    {
        "id": 62,
        "name": "བod rigs gra nang shing bzo rgyal",
        "description": "གra nang rdzong gi srol rgyun shing bzo lag rgyal yin la sku rten dang khang bzo la sbyor",
    },
    {
        "id": 63,
        "name": "ཚwa bskams rgyal",
        "description": "སman khams rdzong gi srol rgyun tsha bzo lag rgyal yin la bod mi dang rang byung mthun sgril gyi shes rab mngon",
    },
    {
        "id": 64,
        "name": "མe tog rdo thag bzo rgyal",
        "description": "སnying khri me tog rdzong gi srol rgyun bzo rgyal yin la 2015 lor rgyal khab kyis bsrung skyob mdzod sa mtshon byed ngos dzin bzo rgyal du gsungs",
    },
    {
        "id": 65,
        "name": "བod rigs srol rgyun mar dzin rgyal",
        "description": "གzhis ka rtse rgyal rtse rdzong gi srol rgyun mar dzin lag rgyal yin la shing las bzo ba'i mar dzin khor lo sbyor len byed",
    },
]

for item in translations:
    for key in ("name", "description"):
        if re.search(r"[A-Za-z]", item[key]):
            raise SystemExit(f"Latin in id {item['id']} {key}: {item[key]}")

out = Path(__file__).with_name("national-bo-overlay-part2.json")
out.write_text(json.dumps({"translations": translations}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"Wrote {len(translations)} translations")
