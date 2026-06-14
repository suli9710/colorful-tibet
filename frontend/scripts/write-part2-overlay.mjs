import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const boPath = path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json')
const samplePath = path.join(__dirname, 'correct-tibetan-samples.txt')
const outPath = path.join(__dirname, 'national-bo-overlay-part2.json')

const tsheg = '\u0f0b'
const shad = '\u0f0d'
const split = (s) => s.replace(new RegExp(`${shad}$`), '').split(tsheg)
const j = (...parts) => parts.flat().filter(Boolean).join(tsheg)
const end = (s) => (s.endsWith(shad) ? s : `${s}${shad}`)

const bo = JSON.parse(fs.readFileSync(boPath, 'utf8'))
const all = Object.values(bo.heritageContent.national.categories).flatMap((c) => c.items)
const byId = Object.fromEntries(all.map((i) => [i.id, i]))
const rep = bo.heritageContent.representative

const n30 = split(byId[30].name)
const d30 = split(byId[30].description)
const d31 = split(byId[31].description)
const n31 = split(byId[31].name)
const n40 = split(byId[40].name)
const d40 = split(byId[40].description)
const n47 = split(byId[47].name)
const d47parts = split(byId[47].description.replace(new RegExp(`${shad}$`), ''))
const d47 = byId[47].description.replace(new RegExp(`${shad}$`), '')
const d10003 = split(rep.tibetanOpera.description)
const d10004 = split(rep.thangka.description)

const bodZlosGar = j(n30[0], n30[1], n30[2])
const srolRgyun = j(d30[11], d30[12])
const zlosGar = j(d30[13], d30[14])
const srolRgyunZlosGar = j(srolRgyun, zlosGar)
const rgyunLugsGcig = j(d31[7], d31[8], d31[9])
const bodRigs = j(n40[0], n40[1])
const thangKa = j(n40[2], n40[3])
const bzoRtsal = j(n47[4], n47[5])
const bzoBaYiLagRtsal = d47.match(/བཟོ་བ[^\u0f0d]+/u)[0]
const bodKyi = j(d47parts[0], d47parts[1])
const riMo = j(d40[2], d40[3])
const chosLugs = j(d40[0], d40[1])
const ri = d40[2]
const brgyud = split(fs.readFileSync(samplePath, 'utf8').trim())[5]
const riBrgyud = j(ri, brgyud)
const sman = d40[6]
const thang = n40[2]
const smanThangRiBrgyud = j(sman, thang, ri, brgyud)
const thangKaRiBrgyudGcig = end(j(srolRgyun, thangKa, riMo, brgyud, d31[3], rgyunLugsGcig))

const n36 = split(fs.readFileSync(samplePath, 'utf8').trim())
const lhoKha = j(n36[0], n36[1])
const phyongRgyas = j(n36[2], n36[3])
const n36name = end(n36.join(tsheg))
const rdzong = j('རྫོང')
const gi = j('གི')
const gyi = d31[3]
const dang = j('དང')
const laSbyor = j('ལ', 'སྦྱོར')
const rigsKyi = j(n47[3], d47parts[1])
const monPa = j('མོན', 'པ')
const monPaYi = j('མོན', 'པ', 'འི')
const mtshoSna = j('མཚོ', 'སྣ')
const lebYul = j('ལེབ', 'ཡུལ')
const khruU = j('ཁྲུ', 'འུ')
const brugGung = j('འབྲུག', 'གུང')
const saKhul = j('ས', 'ཁུལ')
const yinTe = j('ཡིན', 'ཏེ')
const bodSaKhul = j('བོད', 'ས', 'ཁུལ')
const khyabPaYod = j('ཁྱབ', 'པ', 'ཡོད')
const lhaSa = j(n31[0], n31[1])
const lcags = n47[3]
const zangsGzor = j('ཟངས', 'གཟོར')
const lcagsGzor = j(lcags, 'གཟོར')
const bodLcagsGzor = j(bodRigs, lcags, 'གཟོར', bzoRtsal)
const bodZangsGzor = j(bodRigs, zangsGzor, bzoRtsal)
const gserDngul = j('གསེར', 'དངུལ')

const mkhyenBrtseRiBrgyud = j('མཁྱེན', 'བརྩེ', ri, brgyud)
const karmaDgaBzhiRiBrgyud = j('ཀར', 'མ', 'དགའ', 'བཞི', ri, brgyud)
const chosByungRiBrgyud = j('ཆོས', 'འbyung', ri, brgyud)

const craftDesc = (middle) => end(j(bodKyi, srolRgyun, middle, bzoBaYiLagRtsal))
const rdzongGiSrol = (place, middle) => end(j(place, rdzong, gi, srolRgyun, middle, bzoBaYiLagRtsal))

const translations = [
  {
    id: 36,
    name: n36name,
    description: end(j(lhoKha, phyongRgyas, rdzong, gi, srolRgyun, bodZlosGar, gyi, rgyunLugsGcig)),
  },
  {
    id: 37,
    name: end(j(lhoKha, monPaYi, zlosGar)),
    description: end(
      j(
        lhoKha,
        saKhul,
        mtshoSna,
        rdzong,
        lebYul,
        khruU,
        monPa,
        rigsKyi,
        srolRgyun,
        zlosGar,
        yinTe,
        '༢༠༠༧',
        j('ལོ', 'ར'),
        zlosGar,
        j('པ', 'འི'),
        j('སྒྲིག', 'འdzugs'),
        j('བྱas', 'རjes'),
        j('མི', 'དgu'),
        j('པ', 'འི'),
        j('འtshogs', 'པ'),
        j('zhig', 'tu'),
        j('bsgyur', 'zin'),
      ),
    ),
  },
  {
    id: 38,
    name: end(brugGung),
    description: end(
      j(srolRgyun, zlosGar, gyi, j('རnam', 'པ'), j('zhig', 'ste'), bodSaKhul, j('ga', 'zhig'), j('tu'), khyabPaYod),
    ),
  },
  {
    id: 41,
    name: end(smanThangRiBrgyud),
    description: end(
      j(
        '༡༥',
        j('སkori', 'bcu', 'lnga', 'pa'),
        j('འi', 'dus'),
        j('su'),
        j('byung', 'ste'),
        sman,
        j('la'),
        j('don', 'grub'),
        j('rgyal', 'mtshan'),
        gyi,
        j('bzhed', 'pa'),
        j('ste'),
        j('thig', 'tshad'),
        j('gtan', 'la'),
        j('phab'),
        j('zhing'),
        j('mdog', 'tshon'),
        j('gsal', 'ba'),
        j('འi'),
        thangKa,
        ri,
        brgyud,
        j('yin'),
      ),
    ),
  },
  {
    id: 42,
    name: end(mkhyenBrtseRiBrgyud),
    description: thangKaRiBrgyudGcig,
  },
  {
    id: 43,
    name: end(karmaDgaBzhiRiBrgyud),
    description: thangKaRiBrgyudGcig,
  },
  {
    id: 44,
    name: end(chosByungRiBrgyud),
    description: thangKaRiBrgyudGcig,
  },
  {
    id: 45,
    name: end(j(lhaSa, 'གtor', 'rtse', thangKa)),
    description: end(j(lhaSa, j('འi'), srolRgyun, j('gtor', 'rtse'), thangKa, j('བzo', 'ba'), j('འi'), j('ལag', 'rtsal'))),
  },
  {
    id: 46,
    name: end(j('ཁams', sman, j('sa'), thangKa)),
    description: thangKaRiBrgyudGcig,
  },
  {
    id: 48,
    name: end(bodZangsGzor),
    description: craftDesc(zangsGzor),
  },
  {
    id: 49,
    name: end(bodLcagsGzor),
    description: craftDesc(lcagsGzor),
  },
  {
    id: 50,
    name: end(j('རdzi', 'sdong', 'ཟangs', bzoRtsal)),
    description: rdzongGiSrol(j('རdzi', 'sdong'), j('ཟangs', j('བzo', 'ba'))),
  },
  {
    id: 51,
    name: end(j('བkra', 'shis', 'rgyal', 'mdzes', gserDngul, zangsGzor, bzoRtsal)),
    description: rdzongGiSrol(j('བkra', 'shis', 'rgyal', 'mdzes'), j(gserDngul, zangsGzor, j('gzor', 'ba'))),
  },
  {
    id: 52,
    name: end(j('ཚa', 'tsha', bzoRtsal)),
    description: end(j(srolRgyun, chosLugs, j('sku'), j('ཚa', 'tsha'), j('བzo', 'ba'), j('འi'), j('ལag', 'rtsal'))),
  },
  {
    id: 53,
    name: end(j(srolRgyun, j('སbra'), bzoRtsal)),
    description: rdzongGiSrol(j('ནag', 'chu', j('སbrags', 'chen')), j('སbra', j('བzo', 'ba'))),
  },
  {
    id: 54,
    name: end(j(bodRigs, j('པang', 'den'), dang, j('ཀa', 'gdan'), j('འthag'), bzoRtsal)),
    description: end(
      j(
        bodKyi,
        srolRgyun,
        j('འthag', j('བzo')),
        j('ལag', 'rtsal'),
        j('པang', 'den'),
        dang,
        j('ཀa', 'gdan'),
        j('བzo', 'ba'),
      ),
    ),
  },
  {
    id: 55,
    name: end(j(lhaSa, j('rgyal', 'mi'), j('chu', 'gtan'), j('འthag'))),
    description: end(
      j(
        j('chu', 'rlung'),
        gyi,
        j('nus', 'pas'),
        j('འbru', j('བzo', 'ba')),
        j('འi'),
        srolRgyun,
        j('ལag', 'rtsal'),
        j('བod', 'mi'),
        j('འi'),
        j('blo', 'gros'),
        dang,
        j('བzo', 'rig'),
        j('mtshon', 'pa'),
      ),
    ),
  },
  {
    id: 56,
    name: end(j(bodRigs, j('ཤing', 'par'), j('par', 'rtsal'))),
    description: end(
      j(
        srolRgyun,
        j('par', 'rtsal'),
        j('ལag', 'rtsal'),
        j('བod', 'yig'),
        j('dpe', 'cha'),
        j('mang', 'po'),
        j('bsrung', 'skyong'),
        j('byas'),
      ),
    ),
  },
  {
    id: 57,
    name: end(bodLcagsGzor),
    description: rdzongGiSrol(j('གzhis', 'ka', j('ཤes', 'thong')), j(lcags, j('བzo', 'ba'))),
  },
  {
    id: 58,
    name: end(j(bodRigs, j('deb', 'gzhir'), bzoRtsal)),
    description: end(
      j(
        srolRgyun,
        j('ལag', 'rtsal'),
        gyi,
        j('deb', 'gzhir'),
        j('བzo', 'ba'),
        j('སa', 'gnas'),
        gyi,
        j('rtsi', 'shing'),
        j('sog'),
        j('las'),
        j('བzo'),
      ),
    ),
  },
  {
    id: 59,
    name: end(j(bodRigs, srolRgyun, j('སa', 'gzugs'), bzoRtsal)),
    description: end(
      j(
        bodKyi,
        srolRgyun,
        j('སa', 'gzugs'),
        j('བzo', 'ba'),
        j('འi'),
        j('ལag', 'rtsal'),
        j('sku', 'brnyan'),
        dang,
        j('ལag', 'rtsal'),
        j('rdzas', 'brgyad'),
        j('བzo', 'ba'),
        laSbyor,
      ),
    ),
  },
  {
    id: 61,
    name: end(j('རtse', 'thar', j('འthag'), j('rtsal'))),
    description: end(
      j(
        lhoKha,
        j('སnar', 'thang'),
        rdzong,
        gi,
        srolRgyun,
        j('བal'),
        j('འthag'),
        j('ལag', 'rtsal'),
        j('བod', 'phrug'),
        j('phran'),
        j('thal', 'rgyal'),
        j('zer'),
        j('grags'),
      ),
    ),
  },
  {
    id: 62,
    name: end(j(bodRigs, j('gra', 'nang'), j('ཤing', 'bzo'), bzoRtsal)),
    description: end(
      j(
        j('gra', 'nang'),
        rdzong,
        gi,
        srolRgyun,
        j('ཤing', 'bzo'),
        j('ལag', 'rtsal'),
        j('sku', 'brnyan'),
        dang,
        j('khrI', 'bzhi'),
        j('བzo', 'ba'),
        laSbyor,
      ),
    ),
  },
  {
    id: 63,
    name: end(j('ཚwa', j('bskams'), j('rtsal'))),
    description: end(
      j(
        j('mang', 'khams'),
        rdzong,
        gi,
        srolRgyun,
        j('ཚwa'),
        j('བzo'),
        j('ལag', 'rtsal'),
        j('བod', 'mi'),
        dang,
        j('kho', 'rang'),
        j('gnyis', 'ka'),
        j('འi'),
        j('mthun', 'sgril'),
        gyi,
        j('blo', 'gros'),
        j('mtshon', 'pa'),
      ),
    ),
  },
  {
    id: 64,
    name: end(j('མe', 'tog', j('rdo', 'thag'), bzoRtsal)),
    description: end(
      j(
        j('ཉing', 'khri'),
        j('མe', 'tog'),
        rdzong,
        gi,
        srolRgyun,
        bzoRtsal,
        j('༢༠༡༥'),
        j('ལor'),
        j('rgyal', 'yongs'),
        j('su'),
        j('bkra', 'shis'),
        j('སa', 'khul'),
        j('mtshan', 'tshad'),
        j('bsrung', 'skyong'),
        laSbyor,
        j('phog'),
      ),
    ),
  },
  {
    id: 65,
    name: end(j(bodRigs, srolRgyun, j('mar', j('snum'), j('dzin')), j('rtsal'))),
    description: end(
      j(
        j('གzhis', 'ka'),
        j('rgyal', 'rtse'),
        rdzong,
        gi,
        srolRgyun,
        j('mar', j('snum')),
        j('dzin'),
        j('ལag', 'rtsal'),
        j('ཤing', 'las'),
        j('བzo', 'ba'),
        j('འi'),
        j('mar', j('dzin')),
        j('khor', 'lo'),
        j('sbyor'),
      ),
    ),
  },
]

for (const t of translations) {
  for (const key of ['name', 'description']) {
    if (/[\u4e00-\u9fffA-Za-z]/.test(t[key])) {
      throw new Error(`Bad chars in id ${t.id} ${key}: ${t[key]}`)
    }
  }
}

if (translations.length !== 26) {
  throw new Error(`Expected 26 items, got ${translations.length}`)
}

fs.writeFileSync(outPath, `${JSON.stringify({ translations }, null, 2)}\n`, 'utf8')
console.log(`Wrote ${translations.length} translations to ${outPath}`)
