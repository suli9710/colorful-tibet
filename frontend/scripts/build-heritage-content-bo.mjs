import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const vuePath = path.join(__dirname, '../src/views/Heritage.vue')
const zhPath = path.join(__dirname, '../src/i18n/locales/heritage-content.zh.json')
const outPath = path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json')

const vue = fs.readFileSync(vuePath, 'utf8')
const zhRoot = JSON.parse(fs.readFileSync(zhPath, 'utf8'))
const zh = structuredClone(zhRoot.heritageContent)

function extractArray(source, marker) {
  const start = source.indexOf(marker)
  if (start === -1) throw new Error(`Missing marker: ${marker}`)
  const bracketStart = source.indexOf('[', start)
  let depth = 0
  for (let i = bracketStart; i < source.length; i += 1) {
    const ch = source[i]
    if (ch === '[') depth += 1
    if (ch === ']') {
      depth -= 1
      if (depth === 0) {
        // eslint-disable-next-line no-eval
        return eval(`(${source.slice(bracketStart, i + 1)})`)
      }
    }
  }
  throw new Error(`Unclosed array for ${marker}`)
}

function extractStringFields(source, marker) {
  const start = source.indexOf(marker)
  if (start === -1) throw new Error(`Missing marker: ${marker}`)
  const end = source.indexOf('\n]', start)
  const block = source.slice(start, end)
  const fields = []
  const regex = /(name|description|significance|imageUrl|baikeUrl|id):\s*(?:'([^']*)'|(\d+))/g
  let current = {}
  let match
  while ((match = regex.exec(block)) !== null) {
    const [, key, stringValue, numberValue] = match
    if (key === 'id' && Object.keys(current).length > 0) {
      fields.push(current)
      current = {}
    }
    current[key] = stringValue ?? Number(numberValue)
  }
  if (Object.keys(current).length > 0) fields.push(current)
  return fields
}

function extractTibetanNational(source) {
  const start = source.indexOf('const getTibetanNationalHeritageItems')
  const end = source.indexOf('\n]', start)
  const block = source.slice(start, end)
  const items = []
  const itemRegex =
    /id:\s*(\d+),[\s\S]*?name:\s*'([^']*)',[\s\S]*?description:\s*'([^']*)'/g
  let match
  while ((match = itemRegex.exec(block)) !== null) {
    items.push({
      id: Number(match[1]),
      name: match[2],
      description: match[3]
    })
  }
  return items
}

const experienceSpots = extractArray(vue, 'const experienceSpotsBo: ExperienceSpot[] = ')
const extraBoFields = extractStringFields(vue, 'const extraBo: HeritageItem[] = ')
const tibetanNational = extractTibetanNational(vue)

const representativeKeys = ['medicine', 'gesar', 'tibetanOpera', 'thangka']
const categoryKeys = [
  'traditionalMedicine',
  'folkLiterature',
  'traditionalDrama',
  'traditionalCraft'
]

const representative = Object.fromEntries(
  representativeKeys.map((key, index) => {
    const item = extraBoFields[index]
    return [
      key,
      {
        key,
        id: item.id,
        name: item.name,
        description: item.description,
        categoryKey: categoryKeys[index],
        significance: item.significance,
        imageUrl: item.imageUrl,
        baikeUrl: item.baikeUrl
      }
    ]
  })
)

const idOverlay = new Map([
  [1, 0],
  [2, 1],
  [6, 2],
  [7, 3],
  [8, 4],
  [9, 5],
  [30, 6],
  [31, 7],
  [39, 8],
  [40, 9],
  [47, 10],
  [60, 11],
  [66, 12],
  [67, 13],
  [68, 14]
])

const bo = structuredClone(zh)
bo.national.noDescription =
  'ད་དུང་ཞིབ་ཕྲའི་འགྲེལ་བཤད་མེད་པས། རྗེས་སུ་རྒྱབ་ངོས་ནས་ཁ་སྣོན་བྱེད་ཆོག'
bo.representative = representative
bo.experienceSpots = experienceSpots
bo.aliases = {
  thangkaBackendName: 'ཐང་ཀ',
  thangkaDisplayName: 'བོད་རིགས་ཐང་ཀ'
}
bo.eventMonthSuffix = 'ཟླ'

for (const [categoryKey, category] of Object.entries(bo.national.categories)) {
  for (const item of category.items) {
    const mapping = idOverlay.get(item.id)
    if (mapping === undefined) continue
    const source = tibetanNational[mapping]
    if (!source) continue
    item.name = source.name
    if (source.description) item.description = source.description
  }
}

fs.writeFileSync(outPath, JSON.stringify({ heritageContent: bo }, null, 2) + '\n', 'utf8')
console.log(`Wrote ${outPath}`)
