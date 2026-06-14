import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const bo = JSON.parse(
  fs.readFileSync(path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json'), 'utf8')
)
const zh = JSON.parse(
  fs.readFileSync(path.join(__dirname, '../src/i18n/locales/heritage-content.zh.json'), 'utf8')
)

const untranslated = []
for (const [categoryKey, category] of Object.entries(zh.heritageContent.national.categories)) {
  for (const zi of category.items) {
    const bi = bo.heritageContent.national.categories[categoryKey].items.find((x) => x.id === zi.id)
    if (/[\u4e00-\u9fff]/.test(`${bi.name}${bi.description || ''}`)) {
      untranslated.push({
        id: zi.id,
        categoryKey,
        name: zi.name,
        description: zi.description || ''
      })
    }
  }
}

const mid = Math.ceil(untranslated.length / 2)
const part1 = untranslated.slice(0, mid)
const part2 = untranslated.slice(mid)

fs.writeFileSync(
  path.join(__dirname, 'untranslated-part1.json'),
  JSON.stringify(part1, null, 2) + '\n',
  'utf8'
)
fs.writeFileSync(
  path.join(__dirname, 'untranslated-part2.json'),
  JSON.stringify(part2, null, 2) + '\n',
  'utf8'
)
console.log(`part1: ${part1.length}, part2: ${part2.length}, total: ${untranslated.length}`)
