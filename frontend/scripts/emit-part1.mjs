import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outPath = path.join(__dirname, 'national-bo-overlay-part1.json')

// Minimal EWTS-ish syllable builder for heritage overlay strings.
const T = {
  tsheg: '\u0f0b',
  shad: '\u0f0d',
  yin: '\u0f0d',
  a: '',
  aa: '\u0f71',
  i: '\u0f72',
  ii: '\u0f73',
  u: '\u0f74',
  uu: '\u0f75',
  e: '\u0f7a',
  ee: '\u0f7b',
  o: '\u0f7c',
  oo: '\u0f7d',
  am: '\u0f0d',
  ang: '\u0f7e',
  ar: '\u0f62\u0f0d',
  al: '\u0f63\u0f0d',
  ai: '\u0f60\u0f72',
  au: '\u0f61\u0f74',
  bo: '\u0f56\u0f7c\u0f51',
  bod: '\u0f56\u0f7c\u0f51',
  kyi: '\u0f40\u0fb1\u0f72',
  gyi: '\u0f42\u0fb1\u0f72',
  ste: '\u0f66\u0f74\u0f72',
  yin: '\u0f0d',
  yod: '\u0f0d',
  du: '\u0f51\u0f74',
  te: '\u0f74\u0f72',
  gar: '\u0f42\u0f62\u0f0b',
  glu: '\u0f42\u0f63\u0f74\u0f0b',
  glu2: '\u0f42\u0f63\u0f74',
  rigs: '\u0f62\u0f72\u0f42\u0f66\u0f0b',
  srol: '\u0f66\u0f90\u0f7c\u0f58\u0f0b',
  rgyun: '\u0f62\u0f74\u0fb1\u0f7a\u0f0b',
  rol: '\u0f62\u0f7c\u0f58\u0f0b',
  dmangs: '\u0f51\u0f58\u0f44\u0f0b',
  chos: '\u0f46\u0f7c\u0f66\u0f0b',
  lugs: '\u0f63\u0f74\u0fb1\u0f66\u0f0b',
  dang: '\u0f51\u0f44\u0f0b',
  mnyam: '\u0f58\u0f5a\u0f71\u0f58\u0f0b',
  sbyor: '\u0f66\u0f56\u0fb1\u0f7c\u0f62\u0f0b',
  byas: '\u0f56\u0fb1\u0f71\u0f66\u0f0b',
  pa: '\u0f54\u0f0b',
  rgyal: '\u0f62\u0f74\u0fb1\u0f71\u0f58\u0f0b',
  rabs: '\u0f62\u0f0b\u0f66\u0f0b',
  dus: '\u0f51\u0f74\u0f66\u0f0b',
  kha: '\u0f40\u0f0b',
  brgyud: '\u0f56\u0f62\u0f74\u0fb1\u0f7a\u0f51\u0f0b',
  tsig: '\u0f56\u0f72\u0f42\u0f0b',
  rtsom: '\u0f62\u0f0b\u0f66\u0f0b',
  gur: '\u0f42\u0f74\u0f62\u0f0b',
  glod: '\u0f42\u0f63\u0f7c\u0f51\u0f0b',
  khags: '\u0f40\u0f0b\u0f42\u0f66\u0f0b',
  pi: '\u0f54\u0f72\u0f0b',
  wang: '\u0f58\u0f0b\u0f44\u0f0b',
  stong: '\u0f66\u0f74\u0f0b\u0f58\u0f0b',
  sa: '\u0f66\u0f0b',
  khul: '\u0f40\u0f74\u0fb1\u0f0b',
  phag: '\u0f54\u0f0b\u0f42\u0f66\u0f0b',
  mas: '\u0f58\u0f0b\u0f66\u0f0b',
  gtong: '\u0f42\u0f74\u0f0b\u0f58\u0f0b',
  gzhas: '\u0f42\u0f7a\u0f66\u0f0b',
  gzhis: '\u0f42\u0f7a\u0f66\u0f0b',
  ka: '\u0f40\u0f0b',
  rgya: '\u0f62\u0f74\u0fb1\u0f71\u0f0b',
  gzh: '\u0f42\u0f7a\u0f66\u0f0b',
  chen: '\u0f46\u0f72\u0f53\u0f0b',
  smar: '\u0f66\u0f58\u0f0b\u0f62\u0f0b',
  phrug: '\u0f54\u0f0b\u0f62\u0f74\u0fb1\u0f42\u0f0b',
  lho: '\u0f63\u0f7c\u0f0b',
  kha2: '\u0f40\u0f0b',
  brdung: '\u0f56\u0f62\u0f51\u0f74\u0fb1\u0f44\u0f0b',
  smyag: '\u0f66\u0f58\u0fb1\u0f71\u0f42\u0f0b',
  phyongs: '\u0f54\u0fb1\u0f7c\u0f42\u0f0b\u0f58\u0f66\u0f0b',
  rgyas: '\u0f62\u0f74\u0fb1\u0f71\u0f66\u0f0b',
  rdzong: '\u0f62\u0f51\u0f5a\u0f0b\u0f58\u0f0b',
  khyab: '\u0f40\u0fb1\u0f71\u0f56\u0f0b',
  mnga: '\u0f58\u0f44\u0f0b',
  ris: '\u0f62\u0f72\u0f66\u0f0b',
  pho: '\u0f54\u0f7c\u0f0b',
  brang: '\u0f56\u0f62\u0f0b\u0f44\u0f0b',
  zlos: '\u0f5f\u0f63\u0f66\u0f0b',
  bshad: '\u0f56\u0f66\u0f0b\u0f51\u0f0b',
  sogs: '\u0f66\u0f7c\u0f42\u0f66\u0f0b',
  mthun: '\u0f58\u0f56\u0f0b\u0f40\u0f53\u0f0b',
  sbyar: '\u0f66\u0f56\u0fb1\u0f71\u0f62\u0f0b',
  rtsal: '\u0f62\u0f0b\u0f66\u0f0b\u0f66\u0f0b',
  lha: '\u0f63\u0f0b',
  nang: '\u0f58\u0f44\u0f0b',
  ma: '\u0f58\u0f0b',
  khyad: '\u0f40\u0fb1\u0f71\u0f51\u0f0b',
  chos2: '\u0f46\u0f7c\u0f66\u0f0b',
  mthun2: '\u0f58\u0f56\u0f0b\u0f40\u0f53\u0f0b',
}

const j = (...keys) => keys.map((k) => T[k] ?? k).join('')

const translations = [
  {
    id: 3,
    name: 'མོན་པའི་ས་ཨ་མ་མི་རིགས་གླུ།',
    description:
      'བོད་ཀྱི་མོན་པ་རིགས་ཀྱི་སྲོལ་རྒྱུན་གླུ་རོལ་སྟེ། མཚོ་སྣ་གྲོང་ཁྱeར་གyི་ལeབ་ཁuལ་ན་ཁyab་པo་ཡod།',
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
console.log(translations.length)
