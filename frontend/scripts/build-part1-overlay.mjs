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
const id8 = pick('traditionalDance', 8)
const id30 = pick('traditionalDrama', 30)
const id31 = pick('traditionalDrama', 31)

const bodKyi = j('\u0F56\u0FBC', '\u0F40\u0F72')
const srolRgyun = j('\u0F66\u0F62\u0F7A\u0F44', '\u0F62\u0F42\u0F74\u0F66')
const saKhulGyi = j('\u0F66\u0F0B\u0F40\u0F74\u0F72', '\u0F42\u0F72')
const rdzongDu = j('\u0F62\u0F51\u0F7A\u0F44', '\u0F51\u0F74')
const rdzongGi = j('\u0F62\u0F51\u0F7A\u0F44', '\u0F42\u0F72')
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
const gluGar = j('\u0F42\u0F63\u0F74', '\u0F62\u0F7A\u0F63')
const gzhasChen = j('\u0F42\u0F56\u0F0B\u0F66', '\u0F46\u0F7A\u0F53')
const mnyamGar = j('\u0F58\u0F7A\u0F44', '\u0F58\u0F0B', '\u0F42\u0F68')
const gzhisKa = j('\u0F42\u0F56\u0F0B\u0F66', '\u0F40\u0F0B', '\u0F40\u0F68')
const lhoKha = j('\u0F63\u0F0B\u0F40\u0F0B', '\u0F40\u0F68')
const mngaRis = j('\u0F58\u0F44\u0F0B\u0F62\u0F/i\u0F66'.replace('/i', ''))
const mangKhams = j('\u0F58\u0F44\u0F0B\u0F40\u0F0B', '\u0F40\u0F68\u0F0B\u0F66\u0F0B', '\u0F40\u0F68\u0F0B\u0F66')
const yarKlung = j('\u0F61\u0F7A\u0F62', '\u0F63\u0F74\u0F44')
const d31 = strip(id31.description).split(ts)
const bodZlosGarGyi = d31.slice(0, 4).join(ts)
const rgyunLugsGcig = d31.slice(-3).join(ts)
const chosLugs = strip(id30.description).split(ts)[0]
const rolDbyangs = j('\u0F62\u0F7A\u0F63', '\u0F51\u0F44\u0F0B\u0F60\u0F72')
const mnyamSbyor = j('\u0F58\u0F7A\u0F44', '\u0F58\u0F0B', '\u0F66\u0F42\u0F60\u0F72\u0F62')
const khyadChos = j('\u0F40\u0F74\u0F56\u0F0B\u0F46\u0F7A\u0F66')

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
        gluGar,
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
        j('\u0F62\u0F0B\u0F58\u0F0B', '\u0F62\u0F42\u0F74\u0F66'),
        j('\u0F51\u0F44\u0F0B', chosLugs),
        rolDbyangs,
        mnyamSbyor,
        j('\u0F56\u0F0B\u0F58\u0F0B', '\u0F58\u0F7A\u0F44', '\u0F66\u0F42\u0F60\u0F72\u0F62'),
        ste,
        j(
          bodKyi,
          j('\u0F62\u0F42\u0F74\u0F62\u0F0B\u0F66', '\u0F62\u0F42\u0F74\u0F66'),
          j('\u0F51\u0F74\u0F0B\u0F42\u0F72'),
          j('\u0F40\u0F0B\u0F60\u0F72', '\u0F42\u0F62\u0F74\u0F66'),
          j('\u0F66\u0F/i\u0F42\u0F74\u0F66'.replace('/i', ''), '\u0F62\u0F0B\u0F58\u0F0B', '\u0F66\u0F0B'),
          yin,
        ),
      ),
    ),
  },
  {
    id: 5,
    name: end(
      j(
        '\u0F42\u0F63\u0F7A\u0F44',
        '\u0F40\u0F0B\u0F40\u0F68\u0F0B\u0F66',
        j('\u0F54\u0F/i\u0F58\u0F44'.replace('/i', '')),
        j('\u0F56\u0FBC', '\u0F66\u0F56\u0F7A\u0F44'),
      ),
    ),
    description: end(
      j(
        j('\u0F42\u0F63\u0F7A\u0F44', '\u0F40\u0F0B\u0F40\u0F68\u0F0B\u0F66'),
        saKhulGyi,
        srolRgyun,
        j('\u0F54\u0F/i\u0F58\u0F44'.replace('/i', '')),
        j('\u0F54\u0F0B\u0F40\u0F0B', '\u0F58\u0F0B', '\u0F42\u0F56\u0F0B', '\u0F58\u0F0B'),
        j('\u0F42\u0F56\u0F7A\u0F44', '\u0F60\u0F72'),
        j('\u0F62\u0F7A\u0F63', '\u0F42\u0F56\u0F0B\u0F66'),
        yin,
      ),
    ),
  },
  {
    id: 10,
    name: end(j(gzhisKa, j('\u0F62\u0F42\u0F74', '\u0F42\u0F56\u0F0B\u0F66'))),
    description: end(
      j(
        j(chenPo, '\u0F60\u0F72'),
        bodKyi,
        srolRgyun,
        gluGar,
        j('\u0F62\u0F7A\u0F63', '\u0F42\u0F68'),
        ste,
        bodSaKhul,
        j(khyadChos, '\u0F62\u0F42\u0F74\u0F66', '\u0F58\u0F0B'),
        yin,
      ),
    ),
  },
  {
    id: 11,
    name: end(j(gzhisKa, j('\u0F66\u0F58\u0F62', '\u0F55\u0F62\u0F56'))),
    description: end(
      j(
        dmangsSrol,
        j('\u0F56\u0F62\u0F51\u0F44', '\u0F58\u0F0B'),
        ste,
        j('\u0F54\u0F/i\u0F42\u0F/i\u0F66'.replace(/\/i/g, ''), gzhisKa, '\u0F56\u0F62\u0F51\u0F44'),
        j('\u0F58\u0F72\u0F66', '\u0F62\u0F42\u0F74\u0F66'),
        ste,
        j('\u0F62\u0F42\u0F74\u0F66', '\u0F54\u0F0B', '\u0F60\u0F72'),
        srolRgyun,
        j('\u0F62\u0F0B\u0F58\u0F0B', '\u0F62\u0F/i\u0F66'.replace('/i', '')),
        yin,
      ),
    ),
  },
  {
    id: 12,
    name: end(j(lhoKha, j('\u0F62\u0F42\u0F74\u0F51\u0F44', '\u0F46\u0F0B', '\u0F56\u0F62\u0F51\u0F44', '\u0F42\u0F68'))),
    description: end(
      j(
        srolRgyun,
        j('\u0F58\u0F7A\u0F44', '\u0F42\u0F56\u0F0B', '\u0F56\u0F62\u0F51\u0F44'),
        ste,
        lhoKha,
        j('\u0F54\u0F7C\u0F44\u0F0B\u0F62\u0F42\u0F74\u0F66', '\u0F62\u0F51\u0F7A\u0F44'),
        rdzongDu,
        khyabPaYod,
      ),
    ),
  },
  {
    id: 13,
    name: end(j('\u0F42\u0F74', '\u0F42\u0F72', j('\u0F58\u0F7C\u0F53', '\u0F42\u0F68'))),
    description: end(
      j(
        mngaRis,
        saKhulGyi,
        srolRgyun,
        j('\u0F54\u0F7C', '\u0F56\u0F62\u0F44', '\u0F42\u0F68'),
        ste,
        bodZlosGarGyi,
        j('\u0F62\u0F7A\u0F63', '\u0F42\u0F68'),
        j('\u0F56\u0F62\u0F51\u0F44', '\u0F42\u0F63\u0F74'),
        j('\u0F66\u0F7A\u0F42\u0F66', '\u0F51\u0F44\u0F0B'),
        j('\u0F58\u0F7A\u0F44', '\u0F58\u0F0B', '\u0F66\u0F42\u0F60\u0F72\u0F62', '\u0F60\u0F72'),
        j('\u0F62\u0F0B\u0F58\u0F0B', '\u0F62\u0F/i\u0F66'.replace('/i', '')),
        yin,
      ),
    ),
  },
  {
    id: 14,
    name: strip(id6.name) + sh,
    description: end(j(gluDangGar, khyadChos, j('\u0F58\u0F7A\u0F44', '\u0F58\u0F0B', '\u0F42\u0F/i\u0F66'.replace('/i', '')), srolRgyun, j('\u0F62\u0F0B\u0F58\u0F0B', '\u0F62\u0F/i\u0F66'.replace('/i', '')), yin)),
  },
  {
    id: 15,
    name: end(j('\u0F64\u0F/i\u0F66'.replace('/i', ''), '\u0F62\u0F7A\u0F44', j('\u0F66\u0F56\u0F62\u0F63', '\u0F62\u0F0B\u0F58\u0F0B'))),
    description: end(
      j(
        j(chenPo, '\u0F60\u0F72'),
        j('\u0F60\u0F46\u0F0B', '\u0F62\u0F0B\u0F58\u0F0B', '\u0F62\u0F/i\u0F66'.replace('/i', '')),
        j('\u0F42\u0F68', '\u0F66\u0F56\u0F72'),
        j(strip(id6.name).split(ts)[0], j('\u0F66\u0F56\u0F7A\u0F44', '\u0F63\u0F74\u0F44')),
        j('\u0F56\u0F7A\u0F44', '\u0F46\u0F7A\u0F53'),
        rdzongDu,
        khyabPaYod,
      ),
    ),
  },
  {
    id: 16,
    name: end(j('\u0F68', '\u0F42\u0F74', j('\u0F66\u0F56\u0F7A\u0F44', '\u0F54\u0F0B'), j('\u0F56\u0F62\u0F51\u0F44', '\u0F42\u0F68'))),
    description: end(j(bodSaKhul, gaZhir, j('\u0F40\u0F74\u0F56\u0F0B\u0F58\u0F0B', '\u0F60\u0F72'), srolRgyun, j('\u0F56\u0F62\u0F51\u0F44', '\u0F42\u0F68'), yin)),
  },
  {
    id: 17,
    name: end(j('\u0F62\u0F/i\u0F66'.replace('/i', ''), j('\u0F58\u0F72\u0F62', '\u0F42\u0F56'), j('\u0F40\u0F74\u0F62\u0F0B\u0F58\u0F0B'))),
    description: end(j(j('\u0F62\u0F/i\u0F66'.replace('/i', ''), j('\u0F58\u0F72\u0F62', '\u0F42\u0F56')), saKhulGyi, srolRgyun, j('\u0F42\u0F68', '\u0F62\u0F0B\u0F58\u0F0B', '\u0F62\u0F/i\u0F66'.replace('/i', '')), yin)),
  },
  {
    id: 18,
    name: end(j(j('\u0F66\u0F56\u0F7A\u0F44', '\u0F42\u0F56\u0F0B\u0F66'), j('\u0F63\u0F0B', '\u0F62\u0F0B\u0F58\u0F0B', '\u0F66\u0F56\u0F7A\u0F44', '\u0F42\u0F56\u0F0B\u0F66'))),
    description: end(
      j(
        j(bodKyi, '\u0F63\u0F74\u0F42\u0F66'),
        j('\u0F56\u0F62\u0F51\u0F44', '\u0F42\u0F68'),
        j('\u0F58\u0F72\u0F66', '\u0F62\u0F42\u0F74\u0F66'),
        j('\u0F58\u0F/i\u0F42\u0F/i\u0F66'.replace(/\/i/g, ''), j('\u0F63\u0F0B', '\u0F62\u0F0B\u0F58\u0F0B', '\u0F66\u0F56\u0F7A\u0F44', '\u0F62\u0F51\u0F7A\u0F44')),
        rdzongDu,
        khyabPaYod,
      ),
    ),
  },
  {
    id: 19,
    name: end(gzhasChen),
    description: end(
      j(
        j(chenPo, '\u0F60\u0F72'),
        srolRgyun,
        gzhasChen,
        mnyamGar,
        ste,
        j('\u0F51\u0F74\u0F0B\u0F46\u0F7A\u0F53', '\u0F46\u0F7A\u0F53'),
        j('\u0F51\u0F44\u0F0B', j('\u0F46\u0F7A\u0F66', '\u0F60\u0F72'), j('\u0F51\u0F74\u0F0B\u0F56\u0F0B', '\u0F66\u0F0B')),
        j('\u0F60\u0F56\u0F0B\u0F66', '\u0F63\u0F/i\u0F66'.replace('/i', '')),
        j('\u0F66\u0F62\u0F7A\u0F44', '\u0F63\u0F/i\u0F66'.replace('/i', '')),
        j('\u0F62\u0F0B\u0F58\u0F0B', '\u0F62\u0F/i\u0F66'.replace('/i', '')),
        yin,
      ),
    ),
  },
  {
    id: 20,
    name: end(j(j('\u0F62\u0F/i\u0F58\u0F0B'.replace('/i', ''), '\u0F42\u0F63\u0F/i\u0F66'.replace('/i', '')), j('\u0F56\u0F74\u0F42\u0F62\u0F0B\u0F42\u0F62\u0F0B', '\u0F62\u0F42\u0F74\u0F62'), gzhasChen)),
    description: end(
      j(
        j('\u0F62\u0F/i\u0F58\u0F0B'.replace('/i', ''), '\u0F42\u0F63\u0F/i\u0F66'.replace('/i', '')),
        rdzongGi,
        j('\u0F56\u0F74\u0F42\u0F62\u0F0B\u0F42\u0F62\u0F0B', '\u0F62\u0F42\u0F74\u0F62'),
        j('\u0F61\u0F74\u0F0B', '\u0F66\u0F/i\u0F66'.replace('/i', ''), '\u0F60\u0F72'),
        srolRgyun,
        gzhasChen,
        mnyamGar,
        yin,
      ),
    ),
  },
  {
    id: 21,
    name: end(j(strip(id6.name).split(ts)[0], j('\u0F66\u0F/i\u0F58\u0F62'.replace('/i', ''), '\u0F62\u0F74'), gzhasChen)),
    description: end(
      j(
        j(strip(id6.name).split(ts)[0], '\u0F60\u0F72'),
        j('\u0F66\u0F/i\u0F58\u0F62'.replace('/i', ''), '\u0F62\u0F74'),
        saKhulGyi,
        srolRgyun,
        gzhasChen,
        mnyamGar,
        yin,
      ),
    ),
  },
  {
    id: 22,
    name: end(j('\u0F45\u0F72', '\u0F58\u0F0B', j('\u0F66\u0F0B', '\u0F64\u0F/i\u0F66'.replace('/i', '')), gzhasChen)),
    description: end(
      j(
        j('\u0F45\u0F72', '\u0F58\u0F0B', j('\u0F66\u0F0B', '\u0F64\u0F/i\u0F66'.replace('/i', ''))),
        j('\u0F42\u0F72', '\u0F60\u0F72'),
        srolRgyun,
        gzhasChen,
        mnyamGar,
        yin,
      ),
    ),
  },
  {
    id: 23,
    name: end(j('\u0F68', '\u0F42\u0F56\u0F0B\u0F66')),
    description: end(
      j(
        srolRgyun,
        j('\u0F63\u0F/i\u0F66'.replace('/i', ''), '\u0F62\u0F42\u0F74\u0F58\u0F0B', '\u0F40\u0F74\u0F62\u0F0B', '\u0F60\u0F72'),
        j('\u0F42\u0F63\u0F74', '\u0F42\u0F68'),
        ste,
        bodSaKhul,
        gaZhigTu,
        khyabPaYod,
      ),
    ),
  },
  {
    id: 24,
    name: end(j(mangKhams, j('\u0F62\u0F42\u0F74\u0F54\u0F0B', '\u0F42\u0F62\u0F0B', '\u0F42\u0F/i\u0F66'.replace('/i', '')), j('\u0F42\u0F68', '\u0F60\u0F72'))),
    description: end(
      j(
        j('\u0F62\u0F42\u0F74\u0F54\u0F0B', '\u0F42\u0F62\u0F0B', '\u0F42\u0F/i\u0F66'.replace('/i', '')),
        j('\u0F42\u0F/i\u0F66'.replace('/i', ''), '\u0F42\u0F/i\u0F66'.replace('/i', '')),
        j('\u0F40\u0F74\u0F62\u0F0B', '\u0F60\u0F72'),
        srolRgyun,
        j('\u0F42\u0F68', '\u0F66\u0F56\u0F72'),
        mangKhams,
        saKhulGyi,
        rdzongDu,
        khyabPaYod,
      ),
    ),
  },
  {
    id: 25,
    name: end(j('\u0F66\u0F/i\u0F58\u0F0B'.replace('/i', ''), '\u0F/i\u0F46\u0F0B'.replace('/i', ''))),
    description: end(
      j(
        srolRgyun,
        j(chosLugs, '\u0F42\u0F68'),
        j('\u0F60\u0F46\u0F0B', '\u0F58\u0F0B'),
        ste,
        bodSaKhul,
        gaZhigTu,
        khyabPaYod,
      ),
    ),
  },
  {
    id: 26,
    name: end(j('\u0F58\u0F42\u0F7A\u0F0B', '\u0F62\u0F/i\u0F66'.replace('/i', ''), '\u0F42\u0F68')),
    description: end(
      j(
        j('\u0F63\u0F7A\u0F44', '\u0F56\u0F62\u0F42\u0F74\u0F66'),
        j('\u0F66\u0F56\u0F7A\u0F44', '\u0F42\u0F62\u0F0B', '\u0F42\u0F62\u0F0B'),
        j('\u0F63\u0F7A\u0F44', '\u0F42\u0F62\u0F0B', '\u0F42\u0F62\u0F0B'),
        j('\u0F63\u0F7A\u0F44', '\u0F42\u0F62\u0F0B'),
        j('\u0F42\u0F/i\u0F66'.replace('/i', '')),
        srolRgyun,
        j('\u0F42\u0F68', '\u0F66\u0F56\u0F72'),
        ste,
        j('\u0F40\u0F0B', '\u0F60\u0F72', '\u0F56\u0F62\u0F42\u0F74\u0F66'),
        j('\u0F56\u0F62\u0F42\u0F74\u0F66', '\u0F54\u0F0B'),
        j('\u0F62\u0F42\u0F74\u0F58\u0F0B', '\u0F60\u0F72\u0F62\u0F0B', '\u0F60\u0F72'),
        j('\u0F66\u0F62\u0F7A\u0F44', '\u0F63\u0F/i\u0F66'.replace('/i', '')),
        yin,
      ),
    ),
  },
  {
    id: 27,
    name: end(
      j(
        j('\u0F58\u0F/i\u0F42\u0F/i\u0F66'.replace(/\/i/g, ''), '\u0F56\u0F0B', '\u0F66\u0F0B'),
        j('\u0F64\u0F0B', '\u0F62\u0F0B', '\u0F54\u0F0B', '\u0F60\u0F72'),
        gluGar,
      ),
    ),
    description: end(
      j(
        j('\u0F42\u0F51\u0F7A\u0F44', '\u0F62\u0F42\u0F74\u0F62'),
        rdzongGi,
        j('\u0F58\u0F/i\u0F42\u0F/i\u0F66'.replace(/\/i/g, ''), '\u0F56\u0F0B', '\u0F66\u0F0B'),
        j('\u0F64\u0F0B', '\u0F62\u0F0B', '\u0F54\u0F0B'),
        j('\u0F58\u0F72', '\u0F62\u0F42\u0F74\u0F66', '\u0F42\u0F72'),
        srolRgyun,
        gluGar,
        yin,
      ),
    ),
  },
  {
    id: 28,
    name: end(
      j(
        j('\u0F62\u0F42\u0F74\u0F62', '\u0F62\u0F/i\u0F66'.replace('/i', '')),
        j('\u0F68', '\u0F42\u0F74'),
        j('\u0F66\u0F56\u0F7A\u0F44', '\u0F54\u0F0B'),
        j('\u0F56\u0F62\u0F51\u0F44', '\u0F42\u0F68'),
      ),
    ),
    description: end(
      j(
        j('\u0F42\u0F60\u0F0B', '\u0F40\u0F/i\u0F62\u0F0B'.replace('/i', '')),
        j('\u0F62\u0F42\u0F74\u0F62', '\u0F62\u0F/i\u0F66'.replace('/i', '')),
        rdzongGi,
        srolRgyun,
        j('\u0F56\u0F62\u0F51\u0F44', '\u0F42\u0F68'),
        yin,
      ),
    ),
  },
  {
    id: 29,
    name: end(
      j(
        j('\u0F66\u0F54\u0F74', '\u0F62\u0F42\u0F74\u0F62'),
        j('\u0F58\u0F7C\u0F53', '\u0F42\u0F68'),
        j('\u0F42\u0F56\u0F7A\u0F44', '\u0F62\u0F0B', '\u0F58\u0F0B'),
        j('\u0F42\u0F68', '\u0F60\u0F72'),
      ),
    ),
    description: end(
      j(
        mngaRis,
        j('\u0F66\u0F54\u0F74', '\u0F62\u0F42\u0F74\u0F62'),
        rdzongGi,
        srolRgyun,
        j('\u0F42\u0F68', '\u0F66\u0F56\u0F72'),
        ste,
        j(khyadChos, '\u0F42\u0F72'),
        j('\u0F42\u0F56\u0F7A\u0F44', '\u0F62\u0F0B', '\u0F58\u0F0B'),
        j('\u0F62\u0F42\u0F74\u0F62', '\u0F42\u0F/i\u0F66'.replace('/i', '')),
        j('\u0F51\u0F44\u0F0B', '\u0F58\u0F7A\u0F44', '\u0F58\u0F0B', '\u0F66\u0F42\u0F60\u0F72\u0F62', '\u0F60\u0F72'),
        yin,
      ),
    ),
  },
  {
    id: 32,
    name: end(j(gzhisKa, j('\u0F60\u0F56\u0F7A\u0F44', '\u0F54\u0F0B', '\u0F58\u0F0B'))),
    description: dramaDesc(gzhisKa),
  },
  {
    id: 33,
    name: end(j(gzhisKa, j('\u0F62\u0F/i\u0F58\u0F0B'.replace('/i', ''), '\u0F42\u0F63\u0F/i\u0F66'.replace('/i', '')), j('\u0F58\u0F0B', '\u0F54\u0F0B'))),
    description: dramaDesc(j('\u0F62\u0F/i\u0F58\u0F0B'.replace('/i', ''), '\u0F42\u0F63\u0F/i\u0F66'.replace('/i', '')), rdzongGi),
  },
  {
    id: 34,
    name: end(j(gzhisKa, j('\u0F62\u0F/i\u0F58\u0F0B'.replace('/i', ''), '\u0F54\u0F0B'), j('\u0F63\u0F/i\u0F66'.replace('/i', ''), '\u0F42\u0F/i\u0F66'.replace('/i', '')))),
    description: dramaDesc(j('\u0F62\u0F/i\u0F58\u0F0B'.replace('/i', ''), '\u0F54\u0F0B'), rdzongGi),
  },
  {
    id: 35,
    name: end(j(lhoKha, yarKlung, j('\u0F56\u0F/i\u0F66'.replace('/i', ''), '\u0F64\u0F/i\u0F66'.replace('/i', '')), j('\u0F58\u0F0B', '\u0F63\u0F0B', '\u0F58\u0F0B'))),
    description: dramaDesc(yarKlung),
  },
]

for (const t of translations) {
  for (const key of ['name', 'description']) {
    if (/[A-Za-z]/.test(t[key])) {
      throw new Error(`Latin in id ${t.id} ${key}: ${t[key]}`)
    }
    if (/[\u4e00-\u9fff]/.test(t[key])) {
      throw new Error(`Chinese in id ${t.id} ${key}: ${t[key]}`)
    }
  }
}

fs.writeFileSync(outPath, `${JSON.stringify({ translations }, null, 2)}\n`, 'utf8')
console.log(`Wrote ${translations.length} translations`)
