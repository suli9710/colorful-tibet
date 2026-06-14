import { execSync } from 'child_process'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const src = execSync('git show eee6425d:frontend/src/views/Heritage.vue', {
  encoding: 'utf8',
  cwd: path.join(__dirname, '../..')
})

const marker = 'const experienceSpotsBo: ExperienceSpot[] = '
const start = src.indexOf(marker)
if (start === -1) throw new Error('marker not found')
const bracketStart = src.indexOf('= [', start)
if (bracketStart === -1) throw new Error('array start not found')
const arrayOpen = bracketStart + 2
let depth = 0
let end = arrayOpen
for (let i = arrayOpen; i < src.length; i += 1) {
  if (src[i] === '[') depth += 1
  if (src[i] === ']') {
    depth -= 1
    if (depth === 0) {
      end = i + 1
      break
    }
  }
}

const block = src.slice(arrayOpen, end)
// eslint-disable-next-line no-eval
const arr = eval(`(${block})`)
const out = path.join(__dirname, 'experience-spots-bo.json')
fs.writeFileSync(out, JSON.stringify(arr, null, 2) + '\n', 'utf8')
console.log(`Extracted ${arr.length} spots -> ${out}`)
