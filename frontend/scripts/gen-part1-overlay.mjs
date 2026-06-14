import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const boPath = path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json')
const outPath = path.join(__dirname, 'national-bo-overlay-part1.json')

const bo = JSON.parse(fs.readFileSync(boPath, 'utf8'))
const pick = (cat, id) =>
  bo.heritageContent.national.categories[cat].items.find((i) => i.id === id)

const ts = '\u0F0B'
const sh = '\u0F0D'
const j = (...parts) => parts.filter(Boolean).join(ts)
const end = (s) => s + sh
const strip = (s) => s.replace(/[\u0F0D\u0F14]$/u, '')

const id6 = pick('traditionalMusic', 6)
const id7 = pick('traditionalDance', 7)
const id31 = pick('traditionalDrama', 31)

const bodKyi = j('\u0F56\u0FBC', '\u0F40\u0F72')
const srolRgyun = j('\u0F66\u0F62\u0F7A\u0F44', '\u0F62\u0F42\u0F74\u0F66')
const saKhulGyi = j('\u0F66\u0F0B\u0F40\u0F74\u0F72', '\u0F42\u0F72')
const rdzongGi = j('\u0F62\u0F51\u0F7A\u0F44', '\u0F42\u0F72')
const rdzongDu = j('\u0F62\u0F51\u0F7A\u0F44', '\u0F51\u0F74')
const yin = j('\u0F61\u0F72\u0F53')
const ste = j('\u0F66\u0F56\u0F72')
const khyabPoYod = j('\u0F40\u0F74\u0F56\u0F0B\u0F54\u0F7C', '\u0F61\u0F7A\u0F51')
const khyabPaYod = j('\u0F40\u0F74\u0F56\u0F0B\u0F58\u0F0B', '\u0F61\u0F7A\u0F51')
const gaZhir = j('\u0F40\u0F74\u0F72', '\u0F42\u0F72\u0F74')
const gaZhigTu = j('\u0F40\u0F74\u0F72', '\u0F42\u0F72\u0F42', '\u0F51\u0F74')
const chenPo = j('\u0F46\u0F7A\u0F53', '\u0F54\u0F7C')
const dmangsSrol = j('\u0F51\u0F58\u0F44\u0F0B\u0F66\u0F0B', srolRgyun)
const bodSaKhul = j(bodKyi, j('\u0F66\u0F0B\u0F40\u0F74\u0F72'))
const gluDangGar = strip(id7.name)
const d31 = strip(id31.description).split(ts)
const bodZlosGarGyi = d31.slice(0, 4).join(ts)
const rgyunLugsGcig = d31.slice(-3).join(ts)
const mnyamGar = j('\u0F58\u0F7A\u0F44', '\u0F58\u0F0B', '\u0F42\u0F68')
const gzhasChen = j('\u0F42\u0F56\u0F0B\u0F66', '\u0F46\u0F7A\u0F53')
const gzhisKa = j('\u0F42\u0F56\u0F0B\u0F66', '\u0F40\u0F0B', '\u0F40\u0F68')
const lhoKha = j('\u0F63\u0F0B\u0F40\u0F0B', '\u0F40\u0F68')
const mngaRis = j('\u0F58\u0F44\u0F0B\u0F62\u0F/i\u0F66'.replace('/i', ''))
const mangKhams = j('\u0F58\u0F44\u0F0B\u0F40\u0F0B', '\u0F40\u0F68\u0F0B\u0F66\u0F0B', '\u0F40\u0F68\u0F0B\u0F66')
const yarKlung = j('\u0F61\u0F7A\u0F62', '\u0F63\u0F74\u0F44')

const dramaDesc = (...regionParts) =>
  end(j(...regionParts, saKhulGyi, srolRgyun, bodZlosGarGyi, rgyunLugsGcig, yin))

const translations = [
  {
    id: 3,
    name: end(
      j(
        '\u0F58\u0F7A\u0F53',
        '\u0F54\u0F0B\u0F60\u0F72',
        j('\u0F66\u0F0B', '\u0F68'),
        j('\u0F58\u0F72', '\u0F62\u0F42\u0F74\u0F66'),
        j('\u0F42\u0F63\u0F74'),
      ),
    ),
    description: end(
      j(
        bodKyi,
        j('\u0F58\u0F7A\u0F53', '\u0F54'),
        j('\u0F62\u0F42\u0F74\u0F66', '\u0F42\u0F72'),
        srolRgyun,
        j('\u0F42\u0F63\u0F74', '\u0F62\u0F7A\u0F63'),
        ste,
        j('\u0F58\u0F7A\u0F44', '\u0F66\u0F42\u0F42'),
        j('\u0F42\u0F62\u0F7A\u0F44', '\u0F40\u0F74\u0F72'),
        j('\u0F63\u0F7A\u0F56', '\u0F40\u0F74\u0F72'),
        rdzongDu,
        j('\u0F63\u0F7A\u0F56', '\u0F40\u0F74\u0F56\u0F0B\u0F58\u0F0B', khyabPoYod),
      ),
    ),
  },
  {
    id: 4,
    name: end(j('\u0F42\u0F74\u0F62', '\u0F42\u0F63\u0F74')),
    description: end(
      j(
        dmangsSrol,
        j('\u0F62\u0F0B\u0F58\u0F0B', '\u0F62\u0F/i\u0F42\u0F74\u0F66'.replace('/i', '')),
        j('\u0F51\u0F44\u0F0B', chosLugs),
        j('\u0F62\u0F7A\u0F63', '\u0F51\u0F44\u0F0B\u0F60\u0F72'),
        j('\u0F58\u0F7A\u0F44', '\u0F58\u0F0B', '\u0F66\u0F42\u0F60\u0F72\u0F62'),
        j('\u0F58\u0F7A\u0F44', '\u0F66\u0F42\u0F60\u0F72\u0F62', '\u0F56\u0F0B\u0F58\u0F0B', '\u0F58\u0F7A\u0F44', '\u0F66\u0F42\u0F60\u0F72\u0F62'),
        ste,
        j(bodKyi, j('\u0F62\u0F42\u0F74\u0F62\u0F0B\u0F66', '\u0F62\u0F42\u0F74\u0F66'), j('\u0F51\u0F74\u0F0B\u0F42\u0F72'), '\u0F40\u0F0B\u0F60\u0F72', j('\u0F40\u0F0B\u0F60\u0F72', '\u0F42\u0F62\u0F74\u0F66'), j('\u0F66\u0F/i\u0F42\u0F74\u0F66'.replace('/i', ''), '\u0F62\u0F0B\u0F58\u0F0B', '\u0F66\u0F0B'), yin),
      ),
    ),
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
