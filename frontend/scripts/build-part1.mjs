import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outPath = path.join(__dirname, 'national-bo-overlay-part1.json')

const dec = (s) => Buffer.from(s, 'base64').toString('utf8')

/** Base64-encoded UTF-8 Tibetan strings (avoids source encoding drift). */
const translations = [
  {
    id: 3,
    name: dec('4L2Y4L284L2T4LyL4L2U4L2g4L2y4LyL4L2m4LyL4L2o4LyL4L2Y4LyL4L2Y4L2y4LyL4L2i4L2y4L2C4L2m4LyL4L2C4L6z4L204LyN'),
    description: dec(
      '4L2W4L284L2R4LyL4L2A4L6x4L2y4LyL4L2Y4L284L2T4LyL4L2U4LyL4L2i4L2y4L2C4L2m4LyL4L2A4L6x4L2y4LyL4L2m4L6y4L284L2j4LyL4L2i4L6S4L6x4L204L2T4LyL4L2C4L6z4L204LyL4L2i4L284L2j4LyL4L2m4L6f4L264LyNIOC9mOC9muC9vOC8i+C9puC+o+C8i+C9guC+suC9vOC9hOC8i+C9geC+sWXgvaLgvIvgvYJ54L2y4LyL4L2jZeC9luC8i+C9gXXgvaPgvIvgvZPgvIvgvYF5YWLgvIvgvZRv4LyL4L2hb2TgvI0='
    ),
  },
  {
    id: 4,
    name: dec('4L2W4L284L2R4LyL4L2C4L2m4LyL4L2C4L6z4L204LyN'),
    description: dec(
      '4L2Y4L284L2T4LyL4L2U4L2g4L2y4LyL4L2m4L6y4L284L2j4LyL4L2i4L6S4L6x4L204L2T4LyL4L2C4L6z4L204LyL4L2i4L284L2j4LyL4L2m4L6f4L264LyNIOC9mOC9muC9vOC8i+C9puC+o+C8i+C9guC+suC9vOC9hOC8i+C9geC+sWXgvaLgvIvgvYJ54L2y4LyL4L2jZeC9luC8i+C9gXXgvaPgvIvgvZPgvIvgvYF5YWLgvIvgvZRv4LyL4L2hb2TgvI0='
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
console.log(`Wrote ${translations.length} items (partial build)`)
