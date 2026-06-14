import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outPath = path.join(__dirname, 'national-bo-overlay-part1.json')

const CONS = {
  g: '\u0f42',
  gh: '\u0f43',
  ng: '\u0f44',
  c: '\u0f45',
  ch: '\u0f46',
  j: '\u0f47',
  ny: '\u0f49',
  t: '\u0f4f',
  th: '\u0f50',
  d: '\u0f51',
  dh: '\u0f52',
  n: '\u0f53',
  p: '\u0f54',
  ph: '\u0f55',
  b: '\u0f56',
  bh: '\u0f57',
  m: '\u0f58',
  ts: '\u0f59',
  tsh: '\u0f5a',
  dz: '\u0f5b',
  w: '\u0f5d',
  zh: '\u0f5e',
  z: '\u0f5f',
  "'": '\u0f60',
  y: '\u0f61',
  r: '\u0f62',
  l: '\u0f63',
  sh: '\u0f64',
  ss: '\u0f65',
  s: '\u0f66',
  h: '\u0f67',
  a: '\u0f68',
  k: '\u0f40',
  kh: '\u0f41',
}

const VOW = {
  a: '',
  i: '\u0f72',
  u: '\u0f74',
  e: '\u0f7a',
  o: '\u0f7c',
  aa: '\u0f71',
  ii: '\u0f73',
  uu: '\u0f75',
  ee: '\u0f7b',
  oo: '\u0f7d',
}

const FINALS = {
  M: '\u0f7e',
  H: '\u0f7f',
  X: '\u0f37',
  '~': '\u0f39',
}

const CONS_KEYS = Object.keys(CONS).sort((a, b) => b.length - a.length)
const VOW_KEYS = Object.keys(VOW).sort((a, b) => b.length - a.length)

function readCons(text, index) {
  for (const key of CONS_KEYS) {
    if (text.startsWith(key, index)) {
      return { key, next: index + key.length }
    }
  }
  return null
}

function readVowel(text, index) {
  for (const key of VOW_KEYS) {
    if (text.startsWith(key, index)) {
      return { key, next: index + key.length }
    }
  }
  return null
}

function convertSyllable(raw) {
  let body = raw.toLowerCase()
  let final = ''
  const finalMatch = body.match(/(M|H|X|~)$/)
  if (finalMatch) {
    final = finalMatch[1]
    body = body.slice(0, -final.length)
  }
  if (!body) return ''

  const prefix = []
  let i = 0
  while (i < body.length) {
    const vowel = readVowel(body, i)
    if (vowel) break
    const cons = readCons(body, i)
    if (!cons) throw new Error(`Unknown syllable fragment: ${body.slice(i)} in ${raw}`)
    prefix.push(CONS[cons.key])
    i = cons.next
  }

  let vowel = 'a'
  const vowelMatch = readVowel(body, i)
  if (vowelMatch) {
    vowel = vowelMatch.key
    i = vowelMatch.next
  }

  const toSubjoined = (base) => {
    const cp = base.codePointAt(0)
    if (cp >= 0x0f40 && cp <= 0x0f69) return String.fromCodePoint(cp + 0x50)
    return base
  }

  const suffix = []
  const stacks = []
  const remaining = body.slice(i)
  const achungVowel = remaining.match(/^'([aiueo]|aa|ee|ii|oo|uu)/)
  const achungOnly = remaining === "'"
  if (achungVowel || achungOnly) {
    i = body.length
  } else {
    while (i < body.length) {
      const stackMatch = body.slice(i).match(/^([yrlw])(aa|ee|ii|oo|uu|a|i|u|e|o)?/)
      if (stackMatch) {
        stacks.push({ cons: CONS[stackMatch[1]], vowel: stackMatch[2] || 'a' })
        i += stackMatch[0].length
        continue
      }
      const cons = readCons(body, i)
      if (!cons) throw new Error(`Unknown suffix fragment: ${body.slice(i)} in ${raw}`)
      suffix.push(CONS[cons.key])
      i = cons.next
    }
  }

  if (prefix.length === 0) {
    if (vowelMatch) return '\u0f68' + (VOW[vowel] || '\u0f71')
    throw new Error(`Empty syllable: ${raw}`)
  }

  let head = prefix[0]
  if (vowel !== 'a' && VOW[vowel] !== undefined) head += VOW[vowel]

  for (const sub of prefix.slice(1)) head += toSubjoined(sub)

  if (achungVowel) {
    const achungV = achungVowel[1]
    head += '\u0f60' + (VOW[achungV] || '\u0f71')
  } else if (achungOnly) {
    head += '\u0f60' + '\u0f71'
  } else {
    for (const sub of suffix) head += toSubjoined(sub)
    for (const stack of stacks) {
      head += toSubjoined(stack.cons)
      if (stack.vowel !== 'a') head += VOW[stack.vowel] || '\u0f71'
    }
  }
  if (final && FINALS[final]) head += FINALS[final]
  return head
}

function wylie(input) {
  const tsheg = '\u0f0b'
  const shad = '\u0f0d'
  const out = []

  for (const word of input.trim().split(/\s+/)) {
    if (!word) continue
    const syllables = word.split('.').filter(Boolean)
    out.push(syllables.map(convertSyllable).join(tsheg))
  }

  return out.join(tsheg)
}

const w = (s) =>
  wylie(
    s
      .replace(/「/g, '')
      .replace(/」/g, '')
      .replace(/（/g, ' ')
      .replace(/）/g, ' ')
      .replace(/"/g, ' ')
      .replace(/\(/g, ' ')
      .replace(/\)/g, ' ')
      .replace(/-/g, '.')
      .replace(/\s+/g, ' ')
      .trim(),
  )

const data = [
  [3, "mon.pa'i saa.ma mi.rigs.glu", "bod.kyi mon.pa rigs.kyi srol.rgyun glu.rol ste mtsho.sna grong.khyer gyi leb.khul na khyab.po yod"],
  [4, 'gur.glu', "dmangs.srol rtsom.rig dang chos.lugs rol.dbyangs mnyam.sbyor byas.pa ste bod.rgyal rabs dus.kyi kha.brgyud tsig.rtsom yin"],
  [5, 'kong.po brag.snyan bod.gong', "kong.po sa.khul gyi srol.rgyun brag.snyan dang bod.gong gi rol.mo yin"],
  [10, 'gzhis.ka rgya.gzhas', "chen.po'i bod.kyi srol.rgyun glu.dang gar yin te bod.sa khul gyi khyad.chos rgyas.pa yin"],
  [11, 'gzhis.ka smar.phrug', "dmangs.srol brdung.ba ste phyi.gzhis brdung zer yang grags.pa'i srol.rgyun rtsal yin"],
  [12, 'lho.kha rgyud.chag brdung.gar', "srol.rgyun smyag.brdung ba ste lho.kha phyongs.rgyas rdzong du khyab.pa yod"],
  [13, 'gu.ge zhu.on.gar', "mnga'.ris sa.khul gyi srol.rgyun pho.brang gar ste bod.zlos gar dang gar.bshad glu sogs dang mthun.sbyar ba'i rtsal yin"],
  [14, "lha.sa'i nang.ma", 'glu.dang gar gyi khyad.chos mthun gyi srol.rgyun rtsal yin'],
  [15, 'shis.rong sbrul.rtsi', "chen.po'i 'chad.rtsal gar ste lha.sa stod.lung bde.chen rdzong du khyab.pa yod"],
  [16, 'a.gu ston.pa brdung.gar', "bod.sa khul ga.zhir khyab.pa'i srol.rgyun brdung.gar yin"],
  [17, 'rwa.sheng khrab.pa', 'rwa.sheng sa.khul gyi srol.rgyun gar.rtsal yin'],
  [18, 'stod.gzhas la.rtse stod.gzhas', 'bod.lugs brdabs.gar zer grags shing la.rtse rdzong du khyab.pa yod'],
  [19, 'gzhas.chen', "chen.po'i srol.rgyun gzhas.chen mnyam.gar ste dus.chen dang chos.sgo'i dus.thabs su brel.ba'i srol.ldan rtsal yin"],
  [20, 'rnam.gling thub.rgyal gzhas.chen', "rnam.gling rdzong gi thub.rgyal yul.sde'i srol.rgyun gzhas.chen mnyam.gar yin"],
  [21, "lha.sa snar.ru gzhas.chen", "lha.sa'i snar.ru sa.khul gyi srol.rgyun gzhas.chen mnyam.gar yin"],
  [22, 'nyi.ma shangs gzhas.chen', 'nyi.ma shangs gi srol.rgyun gzhas.chen mnyam.gar yin'],
  [23, 'a.gzhas', "srol.rgyun las.rgyun khyer.ba'i glu.gar ste bod.sa khul ga.zhig tu khyab.pa yod"],
  [24, 'mang.khams rgyud.pa gsum gyi.gar', "rgyud.pa gsum gyis khyer.ba'i srol.rgyun gar ste mang.khams sa.khul du khyab.pa yod"],
  [25, "smin.na 'cham", "srol.rgyun chos.gar cham ste bod.sa khul ga.zhig tu khyab.pa yod"],
  [26, 'mgo.rtses gar', "lo.brgya stong.drug brgya lhag gi srol.rgyun gar yin te kha.brgyud brgyud.pa rgyun.dzin gyi srol.ldan yin"],
  [27, "zhing.thang shar.pa'i glu.gar", 'gding.rgyal rdzong zhing.thang shar.pa mi.rigs kyi srol.rgyun glu.gar yin'],
  [28, 'rgyal.ri a.gu ston.pa brdung.gar', "gna'.khri rgyal.ri rdzong gi srol.rgyun brdung.gar yin"],
  [29, 'spu.rgyal zhu.on gos.dar gar', "mnga'.ris spu.rgyal rdzong gi srol.rgyun gar ste khyad.par gyi gos.dar rig.rgyal dang mthun.sbyar ba yin"],
  [32, "gzhis.ka 'phyong.ba", 'gzhis.ka sa.khul gyi srol.rgyun bod.zlos gar gyi rgyun.lugs gcig yin'],
  [33, 'gzhis.ka rnam.gling zhang.ba', 'rnam.gling rdzong gi srol.rgyun bod.zlos gar gyi rgyun.lugs gcig yin'],
  [34, 'gzhis.ka rnam.rgyal rgyal.gar', 'rnam.rgyal rdzong gi srol.rgyun bod.zlos gar gyi rgyun.lugs gcig yin'],
  [35, 'lho.kha yar.klung bkra.shis zhal.pa', 'yar.klung sa.khul gyi srol.rgyun bod.zlos gar gyi rgyun.lugs gcig yin'],
]

const translations = data.map(([id, name, description]) => ({
  id,
  name: w(name) + '\u0f0d',
  description: w(description) + '\u0f0d',
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
