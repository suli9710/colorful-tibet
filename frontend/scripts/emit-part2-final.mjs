import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outPath = path.join(__dirname, 'national-bo-overlay-part2.json')
const bo = JSON.parse(
  fs.readFileSync(path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json'), 'utf8'),
)

const T = '\u0f0b'
const S = '\u0f0d'
const strip = (s) => s.replace(/[\u0f0d\u0f14]$/u, '')
const j = (...parts) => parts.filter(Boolean).join(T)
const end = (s) => strip(s) + S
const find = (id) => {
  for (const cat of Object.values(bo.heritageContent.national.categories)) {
    const hit = cat.items.find((i) => i.id === id)
    if (hit) return hit
  }
  throw new Error(`missing ${id}`)
}

const d30 = strip(find(30).description).split(T)
const d31 = strip(find(31).description).split(T)
const n40 = strip(find(40).name).split(T)
const n47 = strip(find(47).name).split(T)
const d47 = strip(find(47).description)
const bzoBaYi = d47.match(/བཟོ་བ[^\u0f0d\u0f14\s]+/u)[0]
const lagRtsal = d47.split('\u0f60\u0f56\u0f72\u0f0b').pop().replace(/\u0f0d$/, '')

const srolRgyun = j(d30[11], d30[12])
const zlosGar = j(d30[13], d30[14])
const bodZlosGar = j(d31[0], d31[1], d31[2])
const bodZlosGarGyi = j(bodZlosGar, '\u0f42\u0f72')
const rgyunLugsGcig = j(d31[7], d31[8], d31[9])
const bodRigs = j(n47[0], n47[1])
const bodKyi = j('\u0f56\u0f0b\u0f51', '\u0f42\u0f72')
const bzoRgyal = strip(find(60).name)
const chosLugs = d30[0]
const lo2007 = '\u0f22\u0f20\u0f20\u0f27'
const lo2015 = '\u0f22\u0f20\u0f21\u0f25'
const yin = '\u0f61\u0f72\u0f53'

const n36Parts = JSON.parse(fs.readFileSync(path.join(__dirname, '_n36parts.json'), 'utf8'))
const lhoKha = j(n36Parts[0], n36Parts[1])
const phyongRgyas = j(n36Parts[2], n36Parts[3])
const rdzongGi = j('\u0f62\u0f51\u0f7a\u0f44', '\u0f42\u0f72')
const thangKa = j(n40[2], n40[3])
const riBrgyud = j('\u0f62\u0f72', '\u0f56\u0f0b\u0f58\u0f0b\u0f42\u0f74\u0f66')
const monPa = j('\u0f58\u0f7c\u0f53', '\u0f54')
const monPaRigs = j(monPa, '\u0f62\u0f42\u0f74\u0f66', '\u0f42\u0f72')
const gzorBzo = j(bzoBaYi, lagRtsal)
const lhaSa = j('\u0f63\u0f72', '\u0f66')
const thangRiBrgyud = j(thangKa, riBrgyud)
const srolThangRiBrgyud = j(srolRgyun, thangRiBrgyud)
const yulGyi = j('\u0f61\u0f7a\u0f63', '\u0f42\u0f72')
const brugGung = j('\u0f60\u0f62\u0f56\u0f58\u0f71', '\u0f42\u0f74\u0f66')
const smanThang = j('\u0f66\u0f58\u0f0b\u0f58\u0f0b\u0f44\u0f0b\u0f62\u0f72')
const mkhyenBrgyud = j('\u0f58\u0f40\u0f7a\u0f44', riBrgyud)
const karmaBrgyad = j('\u0f40\u0f62\u0f58\u0f0b', '\u0f58\u0f0b', '\u0f40\u0f0b', '\u0f56\u0f0b\u0f58\u0f0b\u0f42\u0f74\u0f66')
const dbusBrgyud = j('\u0f51\u0f74\u0f66', riBrgyud)
const khams = j('\u0f40\u0f58\u0f0b\u0f66')
const smanSa = j('\u0f66\u0f58\u0f0b\u0f66', '\u0f66')
const zangsGzor = j('\u0f58\u0f0b\u0f58\u0f0b\u0f66', '\u0f62\u0f56\u0f0b', '\u0f42\u0f74\u0f66')
const lcagsGzor = j('\u0f63\u0f0b\u0f40\u0f0b\u0f66', '\u0f62\u0f56\u0f0b', '\u0f42\u0f74\u0f66')
const rdziSdong = j('\u0f62\u0f51\u0f7a\u0f44', '\u0f58\u0f7a\u0f44\u0f0b\u0f42\u0f74\u0f66')
const dgyesMdzes = j('\u0f51\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66')
const gserDngul = j('\u0f58\u0f72\u0f0b', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66')
const zangsRngul = j('\u0f58\u0f0b\u0f44\u0f0b\u0f58\u0f0b', '\u0f62\u0f56\u0f0b\u0f62\u0f74\u0f66', '\u0f62\u0f56\u0f0b', '\u0f42\u0f74\u0f66')
const tshaTsha = j('\u0f59\u0f0b', '\u0f59\u0f0b')
const sbraBa = j('\u0f66\u0f62\u0f7a\u0f72', '\u0f56\u0f0b')
const pangDen = j('\u0f54\u0f0b\u0f58\u0f0b', '\u0f51\u0f72\u0f53')
const kaGdan = j('\u0f40\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66')
const thagBzo = j('\u0f60\u0f0b', '\u0f44\u0f0b\u0f58\u0f0b', bzoRgyal)
const rgyalMi = j('\u0f62\u0f92\u0fb1\u0f0b\u0f58\u0f72', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66')
const chuGtan = j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f60\u0f0b', '\u0f44\u0f0b\u0f58\u0f0b')
const shingPar = j('\u0f64\u0f0b\u0f58\u0f0b', '\u0f54\u0f0b', '\u0f62\u0f0b', '\u0f62\u0f0b')
const parRgyal = j('\u0f54\u0f0b', '\u0f62\u0f0b', bzoRgyal)
const debBris = j('\u0f51\u0f72\u0f0b', '\u0f62\u0f56\u0f0b', '\u0f62\u0f0b')
const saGzugs = j('\u0f66\u0f0b', '\u0f58\u0f7a\u0f44\u0f0b\u0f42\u0f74\u0f66', bzoRgyal)
const rtseThar = j('\u0f62\u0f56\u0f0b', '\u0f44\u0f0b\u0f58\u0f0b', '\u0f60\u0f0b', '\u0f44\u0f0b\u0f58\u0f0b')
const graNang = j('\u0f58\u0f62\u0f0b', '\u0f58\u0f0b\u0f58\u0f0b')
const shingBzo = j('\u0f64\u0f0b\u0f58\u0f0b', bzoRgyal)
const tshwaBskams = j('\u0f59\u0f0b\u0f58\u0f0b', j('\u0f56\u0f0b\u0f58\u0f0b\u0f66', bzoRgyal))
const medTog = j('\u0f58\u0f72\u0f0b', '\u0f42\u0f74\u0f66')
const rdoThag = j('\u0f62\u0f51\u0f7a\u0f44', '\u0f44\u0f0b\u0f58\u0f0b', bzoRgyal)
const marBru = j('\u0f58\u0f62\u0f0b', '\u0f56\u0f72\u0f0b', '\u0f58\u0f0b\u0f58\u0f0b', '\u0f58\u0f0b\u0f58\u0f0b')
const gzhisKaRtse = j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b\u0f42\u0f74\u0f66', '\u0f62\u0f56\u0f0b')
const nagChu = j('\u0f58\u0f0b\u0f40\u0f0b\u0f58\u0f0b\u0f66')
const baChen = j('\u0f56\u0f0b', '\u0f58\u0f72\u0f0b\u0f53')
const smanKhams = j('\u0f66\u0f58\u0f0b\u0f58\u0f0b\u0f66')
const nyingKhri = j('\u0f58\u0f58\u0f7a\u0f44', '\u0f40\u0f62\u0f0b')
const snyeMdo = j('\u0f66\u0f58\u0f7a\u0f44', '\u0f58\u0f7a\u0f44\u0f0b\u0f51\u0f7a\u0f44')
const mtshoSna = j('\u0f58\u0f7a\u0f44\u0f0b\u0f40\u0f74\u0f72', '\u0f58\u0f7a\u0f44')
const lebYul = j('\u0f63\u0f72\u0f0b\u0f56\u0f72', '\u0f61\u0f7a\u0f63')
const bzhadMthong = j('\u0f56\u0f0b\u0f58\u0f0b', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66')
const smonRdzong = j('\u0f66\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66')
const rgyalRtse = j('\u0f62\u0f92\u0fb1\u0f0b\u0f58\u0f0b', '\u0f62\u0f56\u0f0b', '\u0f62\u0f56\u0f0b')

const name36 = end(j(...n36Parts))

const translations = [
  {
    id: 36,
    name: name36,
    description: end(j(lhoKha, phyongRgyas, rdzongGi, srolRgyun, bodZlosGarGyi, rgyunLugsGcig)),
  },
  {
    id: 37,
    name: end(j(lhoKha, monPa, '\u0f60\u0f72', zlosGar)),
    description: end(
      j(
        lhoKha,
        mtshoSna,
        rdzongGi,
        lebYul,
        j('\u0f40\u0f7a\u0f44\u0f0b\u0f58\u0f7a\u0f44', '\u0f42\u0f72'),
        monPaRigs,
        srolRgyun,
        zlosGar,
        yin,
        lo2007,
        '\u0f63\u0f72',
        zlosGar,
        '\u0f54\u0f0b\u0f60\u0f72',
        '\u0f66\u0f42\u0f42\u0f74\u0f66',
        '\u0f56\u0f0b\u0f40\u0f60\u0f72\u0f68',
        '\u0f42\u0f74\u0f66',
        '\u0f56\u0f7a\u0f68',
        '\u0f42\u0f74\u0f66',
        '\u0f58\u0f72',
        '\u0f29',
        '\u0f61\u0f7a\u0f51',
        '\u0f54\u0f0b\u0f60\u0f72',
        '\u0f60\u0f58\u0f62\u0f0b\u0f56\u0f0b',
        '\u0f66\u0f42\u0f42\u0f74\u0f66',
        '\u0f66\u0f74\u0f44\u0f42',
        '\u0f42\u0f74\u0f66',
        '\u0f62\u0f56\u0f0b\u0f58\u0f0b',
        yin,
      ),
    ),
  },
  {
    id: 38,
    name: end(brugGung),
    description: end(
      j(
        srolRgyun,
        zlosGar,
        '\u0f42\u0f72',
        '\u0f62\u0f74\u0f66',
        '\u0f58\u0f0b',
        '\u0f42\u0f74\u0f66',
        '\u0f42\u0f58\u0f72\u0f42',
        '\u0f66\u0f56\u0f72',
        '\u0f56\u0f0b\u0f51',
        bodKyi,
        '\u0f66\u0f0b\u0f40\u0f74\u0f72',
        '\u0f58\u0f7a\u0f44',
        '\u0f66\u0f0b\u0f40\u0f74\u0f72',
        '\u0f58\u0f7a\u0f44',
        '\u0f62\u0f56\u0f0b\u0f58\u0f0b',
        '\u0f40\u0f7a\u0f44\u0f0b\u0f60\u0f72\u0f66',
        yin,
      ),
    ),
  },
  {
    id: 41,
    name: end(j(smanThang, riBrgyud)),
    description: end(
      j(
        '\u0f63\u0f72',
        '\u0f56\u0f0b\u0f40\u0f74\u0f72',
        '\u0f58\u0f7a\u0f44',
        '\u0f58\u0f7a\u0f44\u0f0b\u0f51\u0f0b\u0f51\u0f74\u0f66',
        smanThang,
        j('\u0f56\u0f0b\u0f58\u0f0b', '\u0f56\u0f0b\u0f44\u0f0b\u0f42\u0f74\u0f66'),
        j('\u0f42\u0f72\u0f0b\u0f62\u0f92\u0fb1\u0f0b\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b\u0f56\u0f0b\u0f44\u0f0b\u0f42\u0f74\u0f66'),
        j('\u0f42\u0f72\u0f0b\u0f62\u0f92\u0fb1\u0f0b\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b\u0f56\u0f0b\u0f44\u0f0b\u0f42\u0f74\u0f66'),
        '\u0f42\u0f72\u0f0b\u0f62\u0f74\u0f66',
        '\u0f56\u0f0b\u0f60\u0f72\u0f66',
        j('\u0f44\u0f0b\u0f42\u0f0b', '\u0f56\u0f0b\u0f42\u0f74\u0f66', '\u0f56\u0f0b\u0f42\u0f74\u0f66', '\u0f56\u0f0b\u0f42\u0f74\u0f66'),
        j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f42\u0f72'),
        j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f42\u0f72'),
        j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f42\u0f72', '\u0f62\u0f42\u0f74\u0f66'),
      ),
    ),
  },
  {
    id: 42,
    name: end(mkhyenBrgyud),
    description: end(
      j(
        srolThangRiBrgyud,
        j('\u0f58\u0f72\u0f53', '\u0f54\u0f0b', '\u0f42\u0f74\u0f66'),
        j('\u0f44\u0f0b\u0f42\u0f0b', '\u0f62\u0f72', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66'),
        j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f66\u0f56\u0f72\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66'),
        j('\u0f58\u0f40\u0f74\u0f68', '\u0f58\u0f0b', yin),
      ),
    ),
  },
  {
    id: 43,
    name: end(j(karmaBrgyad, riBrgyud)),
    description: end(
      j(
        srolThangRiBrgyud,
        j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'),
        j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f0b', '\u0f42\u0f72'),
        j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f42\u0f72'),
      ),
    ),
  },
  {
    id: 44,
    name: end(dbusBrgyud),
    description: end(
      j(
        srolThangRiBrgyud,
        j('\u0f62\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f66\u0f44\u0f0b\u0f42\u0f74\u0f66'),
        j('\u0f44\u0f0b\u0f58\u0f0b', '\u0f62\u0f56\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'),
        j('\u0f62\u0f72', '\u0f58\u0f7a\u0f44', '\u0f42\u0f72'),
        j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f63\u0f48\u0f7c\u0f44'),
      ),
    ),
  },
  {
    id: 45,
    name: end(j(lhaSa, j('\u0f42\u0f74\u0f66', '\u0f62\u0f56\u0f0b', '\u0f62\u0f72'), thangKa)),
    description: end(j(lhaSa, '\u0f60\u0f72', srolRgyun, j('\u0f62\u0f56\u0f0b', '\u0f56\u0f72\u0f0b', '\u0f62\u0f56\u0f0b'), thangKa, gzorBzo)),
  },
  {
    id: 46,
    name: end(j(khams, smanSa, thangKa)),
    description: end(j(khams, yulGyi, thangRiBrgyud, j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'))),
  },
  {
    id: 48,
    name: end(j(bodRigs, zangsGzor, bzoRgyal)),
    description: end(j(bodKyi, srolRgyun, zangsRngul, gzorBzo)),
  },
  {
    id: 49,
    name: end(j(bodRigs, lcagsGzor, bzoRgyal)),
    description: end(j(bodKyi, srolRgyun, j('\u0f63\u0f0b\u0f40\u0f0b\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'), gzorBzo)),
  },
  {
    id: 50,
    name: end(j(rdziSdong, j('\u0f58\u0f0b\u0f58\u0f0b\u0f66', bzoRgyal))),
    description: end(j(gzhisKaRtse, rdziSdong, rdzongGi, srolRgyun, zangsRngul, gzorBzo)),
  },
  {
    id: 51,
    name: end(j(n36Parts[6], n36Parts[7], dgyesMdzes, gserDngul, zangsGzor, bzoRgyal)),
    description: end(
      j(lhaSa, n36Parts[6], n36Parts[7], dgyesMdzes, j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f42\u0f74\u0f66', '\u0f42\u0f74\u0f66'), rdzongGi, srolRgyun, gserDngul, j('\u0f44\u0f0b\u0f58\u0f0b', zangsRngul), gzorBzo),
    ),
  },
  {
    id: 52,
    name: end(j(tshaTsha, bzoRgyal)),
    description: end(
      j(srolRgyun, chosLugs, '\u0f42\u0f72', j('\u0f66\u0f42\u0f42\u0f74\u0f66', '\u0f62\u0f74\u0f66', bzoRgyal), yin, j('\u0f66\u0f0b\u0f58\u0f0b\u0f58\u0f0b\u0f42\u0f74\u0f66', '\u0f66\u0f58\u0f7a\u0f44', '\u0f42\u0f74\u0f66'), j('\u0f58\u0f7a\u0f44\u0f0b\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'), gzorBzo),
    ),
  },
  {
    id: 53,
    name: end(j(srolRgyun, j(sbraBa, bzoRgyal))),
    description: end(j(nagChu, baChen, rdzongGi, srolRgyun, j(sbraBa, j('\u0f62\u0f74\u0f66', '\u0f58\u0f0b', bzoRgyal)), gzorBzo)),
  },
  {
    id: 54,
    name: end(j(bodRigs, pangDen, j('\u0f44\u0f0b\u0f58\u0f0b', kaGdan), thagBzo)),
    description: end(j(bodKyi, srolRgyun, j('\u0f62\u0f56\u0f0b', '\u0f66\u0f0b', bzoRgyal), lagRtsal, yin, pangDen, j('\u0f44\u0f0b\u0f58\u0f0b', kaGdan), j('\u0f56\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f56\u0f7a\u0f68'))),
  },
  {
    id: 55,
    name: end(j(lhaSa, rgyalMi, chuGtan)),
    description: end(
      j(
        j('\u0f58\u0f7a\u0f44\u0f0b\u0f42\u0f74\u0f66', '\u0f62\u0f92\u0fb1'),
        '\u0f42\u0f72',
        j('\u0f66\u0f42\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'),
        j('\u0f56\u0f72\u0f0b', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f60\u0f72'),
        srolRgyun,
        lagRtsal,
        j(bodKyi, j('\u0f58\u0f72\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66'), j('\u0f56\u0f0b\u0f42\u0f74\u0f66', '\u0f62\u0f56\u0f0b', '\u0f42\u0f74\u0f66'), j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'), j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'), j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66')),
      ),
    ),
  },
  {
    id: 56,
    name: end(j(bodRigs, shingPar, parRgyal)),
    description: end(
      j(srolRgyun, parRgyal, lagRtsal, yin, j('\u0f56\u0f0b\u0f51', '\u0f61\u0f72\u0f0b\u0f42\u0f74\u0f66'), j('\u0f51\u0f72\u0f0b', '\u0f62\u0f58\u0f7a\u0f44'), j('\u0f51\u0f0b\u0f40\u0f0b', '\u0f58\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66'), j('\u0f66\u0f62\u0f74\u0f44\u0f42', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f56\u0f7a\u0f68', '\u0f42\u0f74\u0f66')),
    ),
  },
  {
    id: 57,
    name: end(j(bodRigs, lcagsGzor, bzoRgyal)),
    description: end(j(gzhisKaRtse, bzhadMthong, smonRdzong, rdzongGi, srolRgyun, j('\u0f63\u0f0b\u0f40\u0f0b\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'), gzorBzo)),
  },
  {
    id: 58,
    name: end(j(bodRigs, debBris, lagRtsal)),
    description: end(
      j(srolRgyun, j('\u0f63\u0f0b\u0f40\u0f0b\u0f66', '\u0f62\u0f0b', bzoRgyal), '\u0f42\u0f72', j(debBris, bzoRgyal), lagRtsal, j('\u0f66\u0f0b\u0f58\u0f0b\u0f58\u0f0b\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f62\u0f74\u0f66', '\u0f58\u0f0b\u0f64\u0f0b\u0f58\u0f0b', '\u0f66\u0f56\u0f72\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66')),
    ),
  },
  {
    id: 59,
    name: end(j(bodRigs, srolRgyun, saGzugs)),
    description: end(j(bodKyi, srolRgyun, j('\u0f56\u0f0b\u0f58\u0f0b', bzoRgyal), lagRtsal, yin, j('\u0f66\u0f58\u0f7a\u0f44', '\u0f62\u0f74\u0f44\u0f42'), j('\u0f44\u0f0b\u0f58\u0f0b', j('\u0f62\u0f56\u0f0b', '\u0f62\u0f0b', bzoRgyal)), j('\u0f63\u0f72', '\u0f66\u0f56\u0f72\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66'))),
  },
  {
    id: 61,
    name: end(j(rtseThar, thagBzo)),
    description: end(j(lhoKha, snyeMdo, rdzongGi, srolRgyun, j('\u0f56\u0f0b\u0f63', '\u0f60\u0f0b', '\u0f44\u0f0b\u0f58\u0f0b', lagRtsal), j(bodKyi, j('\u0f66\u0f58\u0f7a\u0f44', '\u0f58\u0f7a\u0f44\u0f0b\u0f42\u0f74\u0f66'), '\u0f42\u0f72'), j('\u0f58\u0f72\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f72\u0f66', '\u0f62\u0f42\u0f74\u0f66'))),
  },
  {
    id: 62,
    name: end(j(bodRigs, graNang, shingBzo)),
    description: end(j(graNang, rdzongGi, srolRgyun, shingBzo, lagRtsal, j('\u0f66\u0f58\u0f7a\u0f44', '\u0f62\u0f74\u0f44\u0f42'), j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f56\u0f7a\u0f68', '\u0f42\u0f74\u0f66'))),
  },
  {
    id: 63,
    name: end(tshwaBskams),
    description: end(j(smanKhams, rdzongGi, srolRgyun, j('\u0f59\u0f0b\u0f58\u0f0b', bzoRgyal), lagRtsal, j(bodKyi, j('\u0f58\u0f72', '\u0f44\u0f0b\u0f58\u0f0b'), j('\u0f62\u0f56\u0f0b\u0f58\u0f0b\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'), '\u0f42\u0f72', j('\u0f64\u0f72\u0f0b', '\u0f62\u0f0b\u0f42\u0f74\u0f66'), j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66')))),
  },
  {
    id: 64,
    name: end(rdoThag),
    description: end(
      j(nyingKhri, medTog, rdzongGi, srolRgyun, j('\u0f56\u0f0b\u0f58\u0f0b', bzoRgyal), lagRtsal, lo2015, '\u0f63\u0f72', j('\u0f62\u0f92\u0fb1\u0f0b\u0f58\u0f0b', '\u0f40\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f72'), j('\u0f66\u0f62\u0f74\u0f44\u0f42', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f66\u0f0b\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'), j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', bzoRgyal), j('\u0f51\u0f74\u0f66', j('\u0f58\u0f72\u0f0b', '\u0f51\u0f72\u0f0b', '\u0f66\u0f0b'), j('\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66', yin))),
    ),
  },
  {
    id: 65,
    name: end(j(bodRigs, srolRgyun, marBru, lagRtsal)),
    description: end(j(gzhisKaRtse, rgyalRtse, rdzongGi, srolRgyun, marBru, lagRtsal, j('\u0f64\u0f0b\u0f58\u0f0b', '\u0f63\u0f72\u0f0b', bzoBaYi), j('\u0f58\u0f0b\u0f58\u0f0b\u0f58\u0f0b', '\u0f60\u0f0b', '\u0f62\u0f56\u0f0b', '\u0f42\u0f74\u0f66'), j('\u0f66\u0f56\u0f72\u0f0b', '\u0f58\u0f0b', '\u0f42\u0f74\u0f66', '\u0f58\u0f7a\u0f44\u0f0b\u0f58\u0f0b', '\u0f42\u0f74\u0f66'))),
  },
]

for (const t of translations) {
  for (const key of ['name', 'description']) {
    if (/[A-Za-z]/.test(t[key])) throw new Error(`Latin in id ${t.id} ${key}`)
  }
}

if (translations.length !== 26) throw new Error(`Expected 26, got ${translations.length}`)

fs.writeFileSync(outPath, `${JSON.stringify({ translations }, null, 2)}\n`, 'utf8')
console.log(`Wrote ${translations.length} translations`)
