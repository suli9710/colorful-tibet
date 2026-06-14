import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const srcPath = path.join(__dirname, '_write-national-bo-overlay-part1.mjs')
const outPath = path.join(__dirname, 'national-bo-overlay-part1.json')

const src = fs.readFileSync(srcPath, 'utf8')
const block = src.match(/const translations = \[([\s\S]*?)\n\]/)?.[1]
if (!block) throw new Error('Could not parse translations block')

const itemRe =
  /\{\s*id:\s*(\d+),\s*name:\s*'((?:\\'|[^'])*)',\s*description:\s*(?:'((?:\\'|[^'])*)'|([\s\S]*?'(?:\\'|[^']*)'))/g

/** @type {{ id: number; name: string; description: string }[]} */
const translations = []
let m
while ((m = itemRe.exec(block)) !== null) {
  const id = Number(m[1])
  const name = m[2].replace(/\\'/g, "'")
  const description = (m[3] ?? m[4].trim().replace(/^'|'$/g, '')).replace(/\\'/g, "'")
  translations.push({ id, name, description })
}

const wordMap = new Map(
  Object.entries({
    bod: 'བོད',
    kyi: 'ཀྱི',
    gyi: 'གྱི',
    ste: 'སྟe',
    te: 'ཏe',
    yin: 'ཡin',
    yod: 'ཡod',
    du: 'དu',
    tu: 'ཏu',
    su: 'སu',
    gi: 'གi',
    pa: 'པ',
    po: 'པo',
    ba: 'བ',
    ma: 'མ',
    sa: 'ས',
    ka: 'ཀ',
    la: 'ལ',
    ri: 'རi',
    lu: 'ལu',
    glu: 'གླu',
    gar: 'གar',
    glu2: 'གླu',
    rol: 'རol',
    gzhas: 'གzhás',
    gzhas2: 'གzhás',
    srol: 'སrol',
    rgyun: 'རgyun',
    rigs: 'རigས',
    dmangs: 'དamangས',
    chos: 'ཆos',
    lugs: 'ལugs',
    dang: 'དang',
    mnyam: 'མnyam',
    sbyor: 'སbyor',
    byas: 'བyas',
    rgyal: 'རgyal',
    rabs: 'རabས',
    dus: 'དus',
    kha: 'ཁ',
    brgyud: 'བrgyud',
    tsig: 'ཚig',
    rtsom: 'རtsom',
    gur: 'གur',
    glod: 'གlod',
    khags: 'ཁags',
    pi: 'པi',
    wang: 'ཝang',
    stong: 'སtong',
    khul: 'ཁul',
    phag: 'ཕag',
    mas: 'མas',
    gtong: 'གtong',
    gzhis: 'གzhis',
    rgya: 'རgya',
    gzh: 'གzh',
    chen: 'ཆen',
    smar: 'སmar',
    phrug: 'ཕrug',
    lho: 'ལho',
    brdung: 'བrdung',
    smyag: 'སmyag',
    phyongs: 'ཕyongs',
    rgyas: 'རgyas',
    rdzong: 'རdzong',
    khyab: 'ཁyab',
    mnga: 'མnga',
    ris: 'རis',
    pho: 'ཕo',
    brang: 'བrang',
    zlos: 'ཟlos',
    bshad: 'བshad',
    sogs: 'སogs',
    mthun: 'མthun',
    sbyar: 'སbyar',
    rtsal: 'རtsal',
    lha: 'ལha',
    nang: 'ནang',
    khyad: 'ཁyad',
    chad: 'ཆad',
    stod: 'སtod',
    lung: 'ལung',
    bde: 'བde',
    zhir: 'ཞir',
    rwa: 'རwa',
    sheng: 'ཤeng',
    khrab: 'ཁrab',
    stod2: 'སtod',
    gzhas3: 'གzhás',
    rtse: 'རtse',
    brdabs: 'བrdabs',
    zer: 'ཟer',
    grags: 'གrags',
    shing: 'ཤing',
    yang: 'ཡang',
    phyi: 'ཕyi',
    gzhis2: 'གzhis',
    snar: 'སnar',
    ru: 'རu',
    nyi: 'ཉyi',
    xiang: 'ཞiang',
    las: 'ལas',
    rgyun2: 'རgyun',
    khyer: 'ཁyer',
    zhig: 'ཞig',
    ga: 'ག',
    rgyud: 'རgyud',
    gsum: 'གsum',
    gyis: 'གyis',
    khyer2: 'ཁyer',
    mang: 'མang',
    khams: 'ཁams',
    smin: 'སmin',
    na: 'ན',
    cham: 'ཆam',
    mgo: 'མgo',
    rtses: 'རtses',
    lo: 'ལo',
    brgya: 'བrgya',
    stong2: 'སtong',
    drug: 'དrug',
    lhag: 'ལhag',
    dzin: 'ཞin',
    ldan: 'ལdan',
    zhing: 'ཞing',
    thang: 'ཐang',
    shar: 'ཤar',
    gding: 'གding',
    mi: 'མi',
    gna: 'གna',
    khri: 'ཁri',
    spu: 'སpu',
    zhuon: 'ཞuon',
    gos: 'གos',
    dar: 'དar',
    par: 'པar',
    rig: 'རig',
    phyong: 'ཕyong',
    zhang: 'ཞang',
    rnam: 'རnam',
    ling: 'ལing',
    thub: 'ཐub',
    ljang: 'ལjang',
    dkar: 'དkar',
    yar: 'ཡar',
    klung: 'ཀlung',
    bkra: 'བkra',
    shis: 'ཤis',
    zhal: 'ཞal',
    agu: 'ཨgu',
    ston: 'སton',
    mon: 'མon',
    leb: 'ལeb',
    mtsho: 'མtsho',
    sna: 'སna',
    grong: 'གrong',
    khyer3: 'ཁyer',
    zer2: 'ཟer',
    brel: 'བrel',
    ldan2: 'ལdan',
    sde: 'སde',
    yul: 'ཡul',
    pho2: 'ཕo',
    brang2: 'བrang',
    gcig: 'གcig',
    lugs2: 'ལugs',
    shig: 'ཤig',
    oper: 'པer',
  })
)

function fixLatin(text) {
  let out = text
  const keys = [...wordMap.keys()].sort((a, b) => b.length - a.length)
  for (const key of keys) {
    out = out.replace(new RegExp(`\\b${key}\\b`, 'g'), wordMap.get(key))
  }
  out = out
    .replace(/「/g, '')
    .replace(/」/g, '')
    .replace(/（/g, ' ')
    .replace(/）/g, ' ')
    .replace(/"/g, '')
  return out
}

for (const t of translations) {
  t.name = fixLatin(t.name)
  t.description = fixLatin(t.description)
  for (const key of ['name', 'description']) {
    if (/[A-Za-z]/.test(t[key])) {
      throw new Error(`Latin remains in id ${t.id} ${key}: ${t[key]}`)
    }
  }
}

fs.writeFileSync(outPath, `${JSON.stringify({ translations }, null, 2)}\n`, 'utf8')
console.log(`Wrote ${translations.length} translations`)
