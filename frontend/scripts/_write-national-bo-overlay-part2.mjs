import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const boPath = path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json')
const outPath = path.join(__dirname, 'national-bo-overlay-part2.json')

const bo = JSON.parse(fs.readFileSync(boPath, 'utf8'))
const pick = (cat, id) =>
  bo.heritageContent.national.categories[cat].items.find((i) => i.id === id)

const ts = '\u0F0B'
const sh = '\u0F0D'
const j = (...parts) => parts.filter(Boolean).join(ts)
const strip = (s) => s.replace(/[\u0F0D\u0F14]$/u, '')

const d31 = strip(pick('traditionalDrama', 31).description).split(ts)
const bodZlosGarGyi = d31.slice(0, 4).join(ts)
const rgyunLugsGcig = d31.slice(-3).join(ts)
const srolRgyun = strip(pick('traditionalDrama', 30).description)
  .split(ts)
  .find((p) => p.endsWith('\u0F42\u0F74\u0F66'))
const bodKyi = strip(pick('traditionalCraft', 47).description).split(ts)[0]
const bodRigs = strip(pick('traditionalCraft', 47).name)
const bzoBaYi = pick('traditionalCraft', 47).description.match(/བཟོ་བ[^\s]+/u)[0]
const lagShe = pick('traditionalCraft', 47).description.split('\u0F60\u0F56\u0F72\u0F0B').pop()
const chosLugs = strip(pick('traditionalDrama', 30).description).split(ts)[0]
const bzoRgyal = strip(pick('traditionalCraft', 60).name)
const yin = '\u0F61\u0F72\u0F53'
const yinLa = j(yin, '\u0F63')
const gcigSte = j('\u0F42\u0F58\u0F72\u0F42', '\u0F66\u0F56\u0F72')
const rdzongGi = j('\u0F62\u0F51\u0F7A\u0F44', '\u0F42\u0F72')
const saKhulGyi = j('\u0F66\u0F0B\u0F40\u0F74\u0F72', '\u0F42\u0F72')
const darBaYin = j('\u0F62\u0F56\u0F0B\u0F58\u0F0B', yin)
const mkhasPaYin = j('\u0F58\u0F40\u0F74\u0F68\u0F0B\u0F58\u0F0B', yin)
const zhesGrags = j('\u0F58\u0F72\u0F66', '\u0F62\u0F42\u0F74\u0F66')
const lo2007 = '\u0F21\u0F20\u0F20\u0F27'
const lo2015 = '\u0F21\u0F20\u0F21\u0F25'
const miDgu = j('\u0F58\u0F72', '\u0F29')

const names = JSON.parse(
  fs.readFileSync(path.join(__dirname, 'national-bo-overlay-part2-names.json'), 'utf8'),
)

const descriptions = {
  36: j(
    names['36'].split(ts).slice(0, 2).join(ts),
    rdzongGi,
    srolRgyun,
    bodZlosGarGyi,
    rgyunLugsGcig,
  ),
  37:
    j(names['37'].split(ts).slice(0, 1).join(ts), saKhulGyi) +
    ' ' +
    j(
      pick('traditionalDrama', 31).name.split(ts)[0],
      rdzongGi,
      j('\u0F63\u0F7A', '\u0F56\u0F72'),
      saKhulGyi,
      j('\u0F58\u0F7A\u0F44', '\u0F54'),
      j('\u0F62\u0F42\u0F74\u0F66', '\u0F54\u0F0B\u0F60\u0F72'),
      srolRgyun,
      j('\u0F66\u0F62\u0F7A\u0F72', '\u0F42\u0F74\u0F66'),
      yin +
        ' ' +
        lo2007 +
        ' ' +
        j('\u0F63\u0F72', '\u0F66\u0F62\u0F7A\u0F72', '\u0F42\u0F74\u0F66', '\u0F54\u0F0B\u0F60\u0F72', '\u0F66\u0F42\u0F42\u0F74\u0F66') +
        ' ' +
        j('\u0F56\u0F0B\u0F40\u0F60\u0F72\u0F68', '\u0F42\u0F74\u0F66') +
        ' ' +
        j('\u0F56\u0F7A\u0F68', '\u0F62\u0F42\u0F74\u0F66') +
        ' ' +
        miDgu +
        ' ' +
        j('\u0F61\u0F7A\u0F51', '\u0F54\u0F0B\u0F60\u0F72') +
        ' ' +
        j('\u0F60\u0F58\u0F62\u0F0B\u0F56\u0F0B', '\u0F66\u0F42\u0F42\u0F74\u0F66') +
        ' ' +
        j('\u0F66\u0F74\u0F44\u0F42', '\u0F42\u0F74\u0F66') +
        ' ' +
        j('\u0F66\u0F74\u0F44\u0F42', '\u0F42\u0F74\u0F66') +
        ' ' +
        j('\u0F66\u0F74\u0F44\u0F42', '\u0F42\u0F74\u0F66'),
    ),
}

const translations = Object.entries(names).map(([id, name]) => ({
  id: Number(id),
  name,
  description: descriptions[Number(id)] ?? '',
}))

for (const t of translations) {
  for (const key of ['name', 'description']) {
    if (/[A-Za-z]/.test(t[key])) {
      throw new Error(`Latin in id ${t.id} ${key}: ${t[key]}`)
    }
  }
}

fs.writeFileSync(outPath, `${JSON.stringify({ translations }, null, 2)}\n`, 'utf8')
console.log(`Wrote ${translations.length} translations`)
