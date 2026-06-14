import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outPath = path.join(__dirname, 'national-bo-overlay-part2.json')

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

  const suffix = []
  const remaining = body.slice(i)
  const achungVowel = remaining.match(/^'([aiueo]|aa|ee|ii|oo|uu)/)
  const achungOnly = remaining === "'"
  if (achungVowel || achungOnly) {
    i = body.length
  } else {
    while (i < body.length) {
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
  else if (vowel === 'a' && prefix.length === 1 && suffix.length === 0 && !final && !achungVowel && !achungOnly) {
    head += '\u0f71'
  }

  const toSubjoined = (base) => {
    const cp = base.codePointAt(0)
    if (cp >= 0x0f40 && cp <= 0x0f69) return String.fromCodePoint(cp + 0x50)
    return base
  }

  for (const sub of prefix.slice(1)) head += toSubjoined(sub)

  if (achungVowel) {
    const achungV = achungVowel[1]
    head += '\u0f60' + (VOW[achungV] || '\u0f71')
  } else if (achungOnly) {
    head += '\u0f60' + '\u0f71'
  } else {
    for (const sub of suffix) head += toSubjoined(sub)
  }
  if (final && FINALS[final]) head += FINALS[final]
  return head
}

function wylie(input) {
  const tsheg = '\u0f0b'
  const out = []

  for (const word of input.trim().split(/\s+/)) {
    if (!word) continue
    if (/^[\u0f00-\u0fff]+$/.test(word)) {
      out.push(word)
      continue
    }
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
      .replace(/\//g, ' / ')
      .replace(/\s+/g, ' ')
      .trim(),
  )

const data = [
  [
    36,
    "lho.kha 'phyong.rgyas bka'.brgyud bkra.shis dpal.'don",
    "lho.kha 'phyong.rgyas rdzong gi srol.rgyun bod.zlos gar gyi rgyun.lugs gcig",
  ],
  [
    37,
    "lho.kha mon.pa'i zlos.gar",
    "lho.kha mtsho.sna rdzong leb.yul khongs kyi mon.pa rigs.kyi srol.rgyun zlos.gar yin \u0f22\u0f20\u0f20\u0f27 lor zlos.pa tshogs.pa bskyar.sgrigs byas rjes mi.dgu'i 'tshon.grwa ru chang.ba yin",
  ],
  [
    38,
    "'brug.gung",
    "srol.rgyun zlos.gar gyi rnam.pa gcig ste bod.sa cha phyogs re res dar.khyab",
  ],
  [
    41,
    'sman.thang ri.brgyud',
    "lo.bcu drug.pa'i dus su sman.bla don.grub rgyal.mtshan gyis btsugs shing thig.le bde.legs dang mdangs.gsal ba'i phyir grags.che",
  ],
  [
    42,
    'mkhyen.brgyud',
    "srol.rgyun thang.ka ri.brgyud chen.po gcig thig.le zhib.mo dang tshon.mdangs sbyar.ba la mkhas",
  ],
  [
    43,
    "karma dga'.brgyad ri.brgyud",
    "srol.rgyun thang.ka ri.brgyud gcig mdangs.gsal dang thig.phra ba'i khyad.chos can",
  ],
  [
    44,
    'dbus.brgyud',
    "srol.rgyun thang.ka ri.brgyud rnying.shos shig thig.drags dang ri.mo'i khyad.chos ldan",
  ],
  [
    45,
    'lha.sa gtor.rtse thang.ka',
    "lha.sa'i srol.rgyun dar.'bru thang.ka bzo.ba'i lag.rtsal",
  ],
  [
    46,
    'khams sman.sa thang.ka',
    'khams yul gyi thang.ka ri.brgyud khyad.chos can',
  ],
  [
    48,
    'bod.rigs zangs.gzor bzo.rgyal',
    "bod.kyi srol.rgyun zangs.rngul gzor bzo.ba'i lag.rtsal",
  ],
  [
    49,
    'bod lcags.gzor bzo.rgyal',
    "bod.kyi srol.rgyun lcags.mdung gzor bzo.ba'i lag.rtsal",
  ],
  [
    50,
    'rdzi.sdong zangs bzo.rgyal',
    "gzhis.ka rtse rdzi.sdong rdzong gi srol.rgyun zangs.rngul gzor bzo.ba'i lag.rtsal",
  ],
  [
    51,
    'bkra.shis dgyes.mdzes gser.dngul zangs.gzor bzo.rgyal',
    "lha.sa bkra.shis dgyes.mdzes khang.tshong gi srol.rgyun gser.dngul dang zangs.rngul gzor bzo.ba'i lag.rtsal",
  ],
  [
    52,
    'tsha.tsha bzo.rgyal',
    "srol.rgyun chos.lugs kyi sgyu.rtsal bzo.rgyal yin sangs.rgyas sku.gzugs chung.ngu bzo.ba'i lag.rtsal",
  ],
  [
    53,
    'srol.rgyun sbra bzo.rgyal',
    "nag.chu ba.chen rdzong gi srol.rgyun sbra.ba rtsal bzo.ba'i lag.rtsal",
  ],
  [
    54,
    "bod.rigs pang.den dang ka.gdan 'thag bzo.rgyal",
    "bod.kyi srol.rgyun ras.bzo lag.rtsal yin pang.den pang.kheb dang ka.gdan sa.gdan bzo.ba byed",
  ],
  [
    55,
    "lha.sa rgyal.mi chu.gtan 'thag",
    "chu.rgyun gyi stobs kyis 'bru.btu pa'i srol.rgyun lag.rtsal bod.mi'i blo.gros dang bzo.rgol nus.pa mngon",
  ],
  [
    56,
    'bod.rigs shing.par par.rgyal',
    "srol.rgyun par.rgyal lag.rtsal yin bod.yig dpe.rnying dpag.tu med.pa srung.skyob byas yod",
  ],
  [
    57,
    'bod lcags.gzor bzo.rgyal',
    "gzhis.ka rtse bzhad.mthong smon.rdzong gi srol.rgyun lcags.mdung gzor bzo.ba'i lag.rtsal",
  ],
  [
    58,
    'bod.rigs deb.bris lag.rtsal',
    "srol.rgyun lag.rgyal gyi deb.bris bzo lag.rtsal sa.gnas khyad.par gyi rtswa.shing sbyor.len byed",
  ],
  [
    59,
    'bod.rigs srol.rgyun sa.gzugs bzo.rgyal',
    "bod.kyi srol.rgyun bzo lag.rtsal yin sku.rten dang lag.bris bzo.rgyal la sbyor",
  ],
  [
    61,
    "rtse.thar 'thag bzo.rgyal",
    "lho.kha snye.mdo rdzong gi srol.rgyun bal.'thag lag.rtsal bod.snum gos kyi nya.tsho zhes grags",
  ],
  [
    62,
    'bod.rigs gra.nang shing bzo.rgyal',
    "gra.nang rdzong gi srol.rgyun shing.bzo lag.rtsal sku.rten dang khyim.cha bzo.ba la sbyor",
  ],
  [
    63,
    'tshwa.bskams rgyal',
    "sman.khams rdzong gi srol.rgyun tshwa.bzo lag.rtsal bod.mi dang rang.byung mthun.sgril gyi shes.rab mngon",
  ],
  [
    64,
    'med.tog rdo.thag bzo.rgyal',
    "nying.khri med.tog rdzong gi srol.rgyun bzo lag.rtsal \u0f22\u0f20\u0f21\u0f25 lor rgyal.khab kyis bsrung.skyob sa.mtshon byed ngos.'dzin bzo.rgyal du gsol.'debs gtong yod",
  ],
  [
    65,
    "bod.rigs srol.rgyun mar.'bru ngnan lag.rtsal",
    "gzhis.ka rtse rgyal.rtse rdzong gi srol.rgyun mar.'bru ngnan lag.rtsal shing.las bzo.ba'i ngnan.'khor sbyor.len byed",
  ],
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
