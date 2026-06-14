import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outPath = path.join(__dirname, 'national-bo-overlay-part1.json')

const translations = [
  {
    id: 3,
    name: 'མོན་པའི་ས་ཨ་མ་མི་རིགས་གླུ།',
    description:
      'བོད་ཀྱི་མོན་པ་རིགས་ཀྱི་སྲོལ་རྒྱུན་གླུ་རོལ་སྟe། མཚོ་སྣ་གྲོང་ཁྱeར་གyི་ལeབ་ཁuལ་ན་ཁyab་པo་ཡod།',
  },
  {
    id: 4,
    name: 'གur g lu',
    description:
      'དamangས_srol rtsom rig dang chos lugs rol dbyangs mnyam sbyor byas pa ste bod rgyal rabs kyi dus kyi kha brgyud tsig rtsom yin།',
  },
  {
    id: 5,
    name: 'གློད་ཁags pi wang bod stong',
    description:
      'གློད་ཁags sa khul gyi srol rgyun pi wang phag mas gtong ba\'i rol gzhas yin།',
  },
  {
    id: 10,
    name: 'གzhis ka rgya gzhas',
    description:
      'ཆen po\'i bod kyi srol rgyun glu dang gar yin te bod sa khul gyi khyad chos rgyas pa yin།',
  },
  {
    id: 11,
    name: 'གzhis ka smar phrug',
    description:
      'དamangས_srol brdung ba ste 「phyi gzhis brdung」zer yang grags pa\'i srol rgyun rtsal yin།',
  },
  {
    id: 12,
    name: 'ལho kha rgyud chag brdung gar',
    description:
      'སrol rgyun smyag brdung ba ste lho kha \'phyongs rgyas rdzong du khyab pa yod།',
  },
  {
    id: 13,
    name: 'གu ge zhuon gar',
    description:
      'མnga\' ris sa khul gyi srol rgyun pho brang gar ste bod zlos gar dang gar bshad glu sogs dang mthun sbyar ba\'i rtsal yin།',
  },
  {
    id: 14,
    name: 'ལha sa\'i nang ma',
    description: 'གlu dang gar gyi khyad chos mthun gyi srol rgyun rtsal yin།',
  },
  {
    id: 15,
    name: 'ཤis rong sbrul rtsi',
    description:
      'ཆen po\'i \'chad rtsal gar ste lha sa stod lung bde chen rdzong du khyab pa yod།',
  },
  {
    id: 16,
    name: 'ཨgu ston pa brdung gar',
    description: 'བod sa khul \'ga zhir khyab pa\'i srol rgyun brdung gar yin།',
  },
  {
    id: 17,
    name: 'རwa sheng khrab pa',
    description: 'རwa sheng sa khul gyi srol rgyun gar rtsal yin།',
  },
  {
    id: 18,
    name: 'སtOD gzhas（ལa rtse stod gzhas）',
    description: '「bod lugs brdabs gar」zer grags shing la rtse rdzong du khyab pa yod།',
  },
  {
    id: 19,
    name: 'གzhas chen',
    description:
      'ཆen po\'i srol rgyun gzhas chen mnyam gar ste dus chen dang chos sgo\'i dus thabs su \'brel ba\'i srol ldan rtsal yin།',
  },
  {
    id: 20,
    name: 'རnam gling thub rgyal gzhas chen',
    description:
      'རnam gling rdzong gi thub rgyal yul sde\'i srol rgyun gzhas chen mnyam gar yin།',
  },
  {
    id: 21,
    name: 'ལha sa snar ru gzhas chen',
    description: 'ལha sa\'i snar ru sa khul gyi srol rgyun gzhas chen mnyam gar yin།',
  },
  {
    id: 22,
    name: 'ཉyi ma xiang gzhas chen',
    description: 'ཉyi ma xiang gi srol rgyun gzhas chen mnyam gar yin།',
  },
  {
    id: 23,
    name: 'ཨgzhas',
    description:
      'སrol rgyun las rgyun khyer ba\'i glu gar ste bod sa khul \'ga zhig tu khyab pa yod།',
  },
  {
    id: 24,
    name: 'སngam khong rgyud pa gsum gyi gar',
    description:
      'རgyud pa gsum gyis khyer ba\'i srol rgyun gar ste mang khams sa khul du khyab pa yod།',
  },
  {
    id: 25,
    name: 'སmin na \'cham',
    description: 'སrol rgyun chos gar \'cham ste bod sa khul \'ga zhig tu khyab pa yod།',
  },
  {
    id: 26,
    name: 'མgo rtses gar',
    description:
      'ལo brgya stong drug brgya lhag gi srol rgyun gar yin te kha brgyud brgyud pa rgyun \'dzin gyi srol ldan yin།',
  },
  {
    id: 27,
    name: 'ཞing thang shar pa\'i glu gar',
    description:
      'གding rgyal rdzong zhing thang shar pa mi rigs kyi srol rgyun glu gar yin།',
  },
  {
    id: 28,
    name: 'རgyal ri「agu ston pa」brdung gar',
    description: 'གna\' khri rgyal ri rdzong gi srol rgyun brdung gar yin།',
  },
  {
    id: 29,
    name: 'སpu rgyal「zhuon」gos dar gar',
    description:
      'མnga\' ris spu rgyal rdzong gi srol rgyun gar ste khyad par gyi gos dar rig rgyal dang mthun sbyar ba yin།',
  },
  {
    id: 32,
    name: 'གzhis ka \'phyong ba',
    description:
      'གzhis ka sa khul gyi srol rgyun bod zlos gar gyi rgyun lugs gcig yin།',
  },
  {
    id: 33,
    name: 'གzhis ka rnam gling zhang ba',
    description:
      'རnam gling rdzong gi srol rgyun bod zlos gar gyi rgyun lugs gcig yin།',
  },
  {
    id: 34,
    name: 'གzhis ka rnam pa ljang dkar',
    description:
      'རnam pa rdzong gi srol rgyun bod zlos gar gyi rgyun lugs gcig yin།',
  },
  {
    id: 35,
    name: 'ལho kha yar klung bkra shis zhal pa',
    description:
      'yar klung sa khul gyi srol rgyun bod zlos gar gyi rgyun lugs gcig yin།',
  },
]

for (const t of translations) {
  for (const key of ['name', 'description']) {
    if (/[A-Za-z]/.test(t[key])) {
      throw new Error(`Latin in id ${t.id} ${key}: ${t[key]}`)
    }
  }
}

fs.writeFileSync(outPath, `${JSON.stringify({ translations }, null, 2)}\n`, 'utf8')
console.log(`Wrote ${translations.length} translations`)
