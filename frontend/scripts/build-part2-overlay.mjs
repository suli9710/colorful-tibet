import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const boPath = path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json')
const samplePath = path.join(__dirname, 'correct-tibetan-samples.txt')
const outPath = path.join(__dirname, 'national-bo-overlay-part2.json')

const T = '\u0f0b'
const S = '\u0f0d'
const split = (s) => s.replace(new RegExp(`${S}$`), '').split(T)
const j = (...p) => p.flat().filter(Boolean).join(T)
const end = (s) => (s.endsWith(S) ? s : s + S)

const bo = JSON.parse(fs.readFileSync(boPath, 'utf8'))
const items = Object.values(bo.heritageContent.national.categories).flatMap((c) => c.items)
const byId = Object.fromEntries(items.map((i) => [i.id, i]))

const n30 = split(byId[30].name)
const d30 = split(byId[30].description)
const d31 = split(byId[31].description)
const n31 = split(byId[31].name)
const n40 = split(byId[40].name)
const d40 = split(byId[40].description)
const n47 = split(byId[47].name)
const d47raw = byId[47].description.replace(new RegExp(`${S}$`), '')
const d47p = split(d47raw)

const bodZlosGar = j(n30[0], n30[1], n30[2])
const srolRgyun = j(d30[11], d30[12])
const zlosGar = j(d30[13], d30[14])
const rgyunLugsGcig = j(d31[7], d31[8], d31[9])
const bodRigs = j(n40[0], n40[1])
const thangKa = j(n40[2], n40[3])
const bzoRtsal = j(n47[4], n47[5])
const bzoBaYiLagRtsal = d47raw.match(/བཟོ་བ[^\u0f0d]+/u)[0]
const bodKyi = j(d47p[0], d47p[1])
const riMo = j(d40[2], d40[3])
const chosLugs = j(d40[0], d40[1])
const ri = d40[2]
const brgyud = split(fs.readFileSync(samplePath, 'utf8').trim())[5]
const sman = d40[6]
const thang = n40[2]
const lhaSa = j(n31[0], n31[1])
const lcags = n47[2]
const rigs = n47[3]
const rigsKyi = j(rigs, d47p[1])
const gyi = d31[3]

const n36 = split(fs.readFileSync(samplePath, 'utf8').trim())
const lhoKha = j(n36[0], n36[1])
const phyongRgyas = j(n36[2], n36[3])
const rdzong = j('རྫོང')
const gi = j('གི')
const yin = j('ཡིན')

const monPa = j('མོན', 'པ')
const monPaYi = 'མོན་པའི'
const mtshoSna = j('མཚོ', 'སྣ')
const lebYul = j('ལེབ', 'ཡུལ')
const khruU = j('ཁྲུ', 'འུ')
const brugGung = j('འབྲུག', 'གུང')
const saKhul = j('ས', 'ཁུལ')
const khams = 'ཁམས'
const tshaTsha = j('ཚ', 'ཚ')
const sbra = j('སྦྲ')
const thag = j('འཐག')
const par = j('པར')
const deb = j('དེབ')
const saGzugs = j('ས', 'གཟུགས')
const bkraShis = j(n36[7], n36[8])
const genericCraftDesc = end(j(bodKyi, srolRgyun, bzoBaYiLagRtsal))

const smanThangRiBrgyud = j(sman, thang, ri, brgyud)
const mkhyenBrtseRiBrgyud = j('མཁྱེན', 'བརྩེ', ri, brgyud)
const karmaDgaBzhiRiBrgyud = j('ཀར', 'མ', 'དགའ', 'བཞི', ri, brgyud)
const chosByungRiBrgyud = j('ཆོས', 'འབྱུང', ri, brgyud)
const thangKaSchoolDesc = end(j(srolRgyun, thangKa, riMo, brgyud, gyi, rgyunLugsGcig))

const zangs = j('ཟངས')
const gzor = j('གཟོར')
const gserDngul = j('གསེར', 'དངུལ')
const craftDesc = (mid) => end(j(bodKyi, srolRgyun, mid, bzoBaYiLagRtsal))

const translations = [
  {
    id: 36,
    name: end(n36.join(T)),
    description: end(j(lhoKha, phyongRgyas, rdzong, gi, srolRgyun, bodZlosGar, gyi, rgyunLugsGcig)),
  },
  {
    id: 37,
    name: end(j(lhoKha, monPaYi, zlosGar)),
    description: end(j(lhoKha, saKhul, mtshoSna, rdzong, lebYul, khruU, monPa, rigsKyi, srolRgyun, zlosGar, yin)),
  },
  {
    id: 38,
    name: end(brugGung),
    description: end(j(srolRgyun, zlosGar, gyi, rgyunLugsGcig)),
  },
  { id: 41, name: end(smanThangRiBrgyud), description: thangKaSchoolDesc },
  { id: 42, name: end(mkhyenBrtseRiBrgyud), description: thangKaSchoolDesc },
  { id: 43, name: end(karmaDgaBzhiRiBrgyud), description: thangKaSchoolDesc },
  { id: 44, name: end(chosByungRiBrgyud), description: thangKaSchoolDesc },
  { id: 45, name: end(j(lhaSa, thangKa)), description: craftDesc(thangKa) },
  { id: 46, name: end(j(khams, sman, j('ས'), thangKa)), description: thangKaSchoolDesc },
  { id: 48, name: end(j(bodRigs, zangs, gzor, bzoRtsal)), description: craftDesc(j(zangs, gzor)) },
  { id: 49, name: end(j(bodRigs, lcags, gzor, bzoRtsal)), description: craftDesc(j(lcags, gzor)) },
  { id: 50, name: end(j(bodRigs, zangs, bzoRtsal)), description: craftDesc(j(zangs)) },
  { id: 51, name: end(j(bodRigs, gserDngul, zangs, gzor, bzoRtsal)), description: craftDesc(j(gserDngul, zangs, gzor)) },
  { id: 52, name: end(j(tshaTsha, bzoRtsal)), description: end(j(srolRgyun, chosLugs, bzoBaYiLagRtsal)) },
  { id: 53, name: end(j(srolRgyun, sbra, bzoRtsal)), description: genericCraftDesc },
  { id: 54, name: end(j(bodRigs, thag, bzoRtsal)), description: genericCraftDesc },
  { id: 55, name: end(j(lhaSa, bzoRtsal)), description: genericCraftDesc },
  { id: 56, name: end(j(bodRigs, par, bzoRtsal)), description: genericCraftDesc },
  { id: 57, name: end(j(bodRigs, lcags, gzor, bzoRtsal)), description: craftDesc(j(lcags)) },
  { id: 58, name: end(j(bodRigs, deb, bzoRtsal)), description: genericCraftDesc },
  { id: 59, name: end(j(bodRigs, srolRgyun, saGzugs, bzoRtsal)), description: genericCraftDesc },
  { id: 61, name: end(j(bodRigs, thag, bzoRtsal)), description: genericCraftDesc },
  { id: 62, name: end(j(bodRigs, bzoRtsal)), description: genericCraftDesc },
  { id: 63, name: end(j('ཚྭ', bzoRtsal)), description: genericCraftDesc },
  { id: 64, name: end(j(bodRigs, bzoRtsal)), description: genericCraftDesc },
  { id: 65, name: end(j(bodRigs, srolRgyun, bzoRtsal)), description: genericCraftDesc },
]

for (const t of translations) {
  for (const key of ['name', 'description']) {
    if (/[\u4e00-\u9fffA-Za-z]/.test(t[key])) {
      throw new Error(`Bad chars id ${t.id} ${key}: ${t[key]}`)
    }
  }
}

if (translations.length !== 26) throw new Error(`Expected 26, got ${translations.length}`)

fs.writeFileSync(outPath, `${JSON.stringify({ translations }, null, 2)}\n`, 'utf8')
console.log(`Wrote ${translations.length} translations`)
