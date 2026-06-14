import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const boPath = path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json')
const outPath = path.join(__dirname, 'national-bo-overlay-part2.json')

const bo = JSON.parse(fs.readFileSync(boPath, 'utf8'))
const pick = (cat, id) =>
  bo.heritageContent.national.categories[cat].items.find((i) => i.id === id)

const tsek = '\u0F0B'
const shad = '\u0F0D'
const splitTsek = (s) => s.replace(/།$/u, '').split(tsek)

const bodZlosGar = pick('traditionalDrama', 30).name.replace(/།$/u, '')
const d31Parts = splitTsek(pick('traditionalDrama', 31).description)
const bodZlosGarGyi = d31Parts.slice(0, 4).join(tsek)
const rgyunLugsGcig = d31Parts.slice(-3).join(tsek)
const srolRgyun = pick('traditionalDrama', 30).description.split(tsek).find((p) => p.endsWith('\u0F42\u0F74\u0F66')) ?? 'སྲོལ་རྒྱུན'
const bodKyi = splitTsek(pick('traditionalCraft', 47).description)[0]
const bodRigs = pick('traditionalCraft', 47).name.replace(/།$/u, '')
const bzoBaYi = pick('traditionalCraft', 47).description.match(/བཟོ་བ[^\s]+/u)[0]
const lagShe = pick('traditionalCraft', 47).description.split('\u0F60i\u0F0B').pop()
const chosLugs = splitTsek(pick('traditionalDrama', 30).description)[0]
const bzoRgyal = pick('traditionalCraft', 60).name.replace(/།$/u, '')
const yinLa = '\u0F0B\u0F61\u0F72\u0F53\u0F0B\u0F63\u0F0B'
const yin = '\u0F61\u0F72\u0F53'
const dang = '\u0F0B\u0F51\u0F0B\u0F44\u0F0B'
const gi = '\u0F42\u0F72'
const rdzongGi = `\u0F62\u0F51\u0F7A\u0F44\u0F0B${gi}`
const saKhulGyi = `\u0F66\u0F0B\u0F40\u0F74\u0F72\u0F0B${gi}`
const darKhyab = `\u0F62\u0F56\u0F0B\u0F40\u0F7B\u0F68\u0F0B\u0F61\u0F7A\u0F0B\u0F51\u0F0D`
const gcigSte = `\u0F42\u0F58\u0F72\u0F42\u0F0B\u0F66\u0F56\u0F72`
const mkhasPaYin = `\u0F58\u0F40\u0F74\u0F68\u0F0B\u0F58\u0F0B\u0F0B${yin}${shad}`

const hex = (h) => Buffer.from(h, 'hex').toString('utf8')
const terms = {
  lhoKha: hex('e0bda3e0beb7e0bdbce0bc8be0bd81'),
  phyongRgyas: hex('e0bc80e0bd95e0bca6e0bda6e0bdbce0bc8be0bd82e0bca6'),
  bkaBrgyudBkraShisDpalDon: hex(
    'e0bdb6e0bc80e0bca7e0bda6e0bdb6e0bca6e0bda6e0bdb6e0bca6e0bda3e0bdb6e0bca6e0bda4e0bdb6e0bca6e0bda2e0bd8de0bc8d',
  ),
  mtshoSna: hex('e0bda3e0bca6e0bda6e0bca6'),
  lhoBa: hex('e0bda3e0beb7e0bdb6'),
  monPa: hex('e0bda3e0beb7e0bda6'),
  monPaYiZlosGar: hex('e0bda3e0beb7e0bdb6e0bca7e0bda6e0bca7e0bd90e0bca2e0bda6e0bca6e0bda2'),
  sbrugGung: hex('e0bc80e0bdb6e0bca6e0bda2e0bca6e0bda2'),
  smanThangRiBrgyud: hex('e0bda2e0bca3e0bda6e0bca6e0bda4e0bdb6e0bca6e0bda6e0bca6e0bda2'),
  mkhyenBrgyud: hex('e0bda1e0bca5e0bda6e0bca6e0bda2e0bca6e0bda2'),
  karmaDgaBzhiRgyud: hex('e0bd80e0bca7e0bda6e0bca7e0bda4e0bca1e0bda6e0bca6e0bda2'),
  chosByungBrgyud: hex('e0bd86e0bca6e0bd90e0bca6e0bda5e0bca6e0bda2'),
  lhaSaGtorRtseThangKa: hex('e0bda3e0beb7e0bca6e0b0b66e0bca7e0bda6e0bca6e0bda6e0bca6e0bda4e0bca1'),
  khamsSmanSaThangKa: hex('e0bda1e0bca6e0bda2e0bca3e0bda6e0bca6e0bda4e0bca1'),
  zangsGzorBzoRgyal: hex('e0bda2e0bca6e0bda2e0bca7e0bda6e0bca6e0bda2e0bca6e0bda2'),
  lcagsGzorBzoRgyal: hex('e0bda3e0bca6e0bda2e0bca7e0bda6e0bca6e0bda2e0bca6e0bda2'),
  lcagsGri: hex('e0bda3e0bca6e0bda2e0bca7e0bda6'),
  rdziSdong: hex('e0bda2e0bca6e0bda2e0bca6'),
  bkraShisDgyasMdzes: hex('e0bdb6e0bca7e0bda6e0bca6e0bda3e0bca6e0bda2e0bca3e0bda6'),
  tshaTshaBzoRgyal: hex('e0bda3e0bca6e0bda2e0bca6e0bda2'),
  sbraBzoRgyal: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  pangDenKaGdan: hex('e0bda2e0bca6e0bda2e0bca6e0bda4e0bca6e0bda2'),
  rgyalMiChuGtan: hex('e0bda2e0bca6e0bda2e0bca6e0bda3e0bca6e0bda2'),
  shingParParRgyal: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  shingLoRgyal: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  saGzugsBzoRgyal: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  rtseTharThagRgyal: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  graNang: hex('e0bda2e0bca6e0bda2'),
  tshaBskamsRgyal: hex('e0bda3e0bca6e0bda2e0bca6e0bda2'),
  meTogRdoThagBzoRgyal: hex('e0bda3e0bca6e0bda2e0bca6e0bda2'),
  marDzinRgyal: hex('e0bda3e0bca6e0bda2e0bca6e0bda2'),
  gzhisKaRtse: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  rgyalRtse: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  snyeMdo: hex('e0bda2e0bca6e0bda2e0bca6'),
  bzhadMthongSmon: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  sbrangRgyas: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  smanKhams: hex('e0bda2e0bca6e0bda2e0bca6'),
  nyingKhri: hex('e0bda2e0bca6e0bda2e0bca6'),
  meTog: hex('e0bda3e0bca6e0bda2'),
  thangKa: hex('e0bda4e0bca6e0bda4e0bca1'),
  riMo: hex('e0bda2e0bca7e0bda6'),
  darBre: hex('e0bda2e0bca6e0bda2'),
  zangsRngul: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  gserDngul: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  khangTshong: hex('e0bda1e0bca6e0bda2e0bca6e0bda2'),
  sangsRgyas: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  skuGzugs: hex('e0bda2e0bca6e0bda2e0bca6e0bda2'),
  chungNgu: hex('e0bda2e0bca6e0bda2e0bca6'),
  sbraBa: hex('e0bda2e0bca6e0bda2'),
  thagBzo: hex('e0bda4e0bca6e0bda2'),
  pangDen: hex('e0bda2e0bca6e0bda2'),
  kaGdan: hex('e0bda1e0bca6e0bda2'),
  chuRgyal: hex('e0bda2e0bca6e0bda2'),
  brasBtus: hex('e0bda2e0bca6e0bda2'),
  bodMi: hex('e0bdb6e0bda3e0bca7'),
  shesRab: hex('e0bda2e0bca6e0bda2'),
  bzoRgod: hex('e0bda2e0bca6e0bda2'),
  parRgyal: hex('e0bda2e0bca6e0bda2'),
  bodYig: hex('e0bdb6e0bda3e0bca7'),
  dpeRnying: hex('e0bda2e0bca6e0bda2'),
  balThag: hex('e0bda2e0bca6e0bda2'),
  bodSnumGos: hex('e0bdb6e0bda3e0bca7'),
  nyaTsho: hex('e0bda3e0bca6e0bda2'),
  shingBzo: hex('e0bda2e0bca6e0bda2'),
  skuRten: hex('e0bda2e0bca6e0bda2'),
  khangBzo: hex('e0bda1e0bca6e0bda2'),
  tshaBzo: hex('e0bda3e0bca6e0bda2'),
  rangByung: hex('e0bda2e0bca6e0bda2'),
  mthunSgril: hex('e0bda3e0bca6e0bda2'),
  rdoThag: hex('e0bda2e0bca6e0bda2'),
  marDzin: hex('e0bda3e0bca6e0bda2'),
  shingLas: hex('e0bda2e0bca6e0bda2'),
  khorLo: hex('e0bca1e0bca6e0bda2'),
  lo2015: '\u0F21\u0F20\u0F21\u0F25',
  lo2007: '\u0F21\u0F20\u0F20\u0F27',
  miDgu: '\u0F58\u0F72\u0F0B\u0F29',
}

console.log('terms check', terms.lhoKha)
