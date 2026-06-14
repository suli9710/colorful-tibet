import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const boPath = path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json')
const zhPath = path.join(__dirname, '../src/i18n/locales/heritage-content.zh.json')
const part1 = JSON.parse(fs.readFileSync(path.join(__dirname, 'national-bo-overlay-part1.json'), 'utf8'))
const part2 = JSON.parse(fs.readFileSync(path.join(__dirname, 'national-bo-overlay-part2.json'), 'utf8'))

const overlayById = new Map(
  [...part1.translations, ...part2.translations].map((item) => [item.id, item])
)

const experienceSpotsBoPath = path.join(__dirname, 'experience-spots-bo.json')
const boRoot = JSON.parse(fs.readFileSync(boPath, 'utf8'))
const zhRoot = JSON.parse(fs.readFileSync(zhPath, 'utf8'))
const bo = boRoot.heritageContent
const zh = zhRoot.heritageContent

if (fs.existsSync(experienceSpotsBoPath)) {
  const experienceSpots = JSON.parse(fs.readFileSync(experienceSpotsBoPath, 'utf8'))
  if (experienceSpots.length > 0) {
    bo.experienceSpots = experienceSpots
  }
}

for (const [categoryKey, category] of Object.entries(bo.national.categories)) {
  for (const item of category.items) {
    const overlay = overlayById.get(item.id)
    if (!overlay) continue
    item.name = overlay.name
    item.description = overlay.description
  }
}

function resolveZhImagePath(aliases, zhName) {
  const normalized = zhName.replace(/[（）()《》“”"·/、\s]/g, '')
  const direct = aliases[zhName] || aliases[normalized]
  if (direct) return direct
  for (const [key, value] of Object.entries(aliases)) {
    if (typeof value !== 'string' || !value.startsWith('/heritage/')) continue
    const keyNorm = key.replace(/[（）()《》“”"·/、\s]/g, '')
    if (zhName.includes(key) || normalized.includes(keyNorm)) return value
  }
  return undefined
}

// Extend image aliases: map Tibetan names to same paths as Chinese counterparts
for (const [categoryKey, category] of Object.entries(bo.national.categories)) {
  for (const item of category.items) {
    const zhItem = zh.national.categories[categoryKey]?.items.find((entry) => entry.id === item.id)
    if (!zhItem) continue
    const zhPathValue = resolveZhImagePath(bo.imageAliases, zhItem.name)
    if (zhPathValue && item.name) {
      bo.imageAliases[item.name] = zhPathValue
      const normalized = item.name.replace(/[（）()《》“”"·/、\s]/g, '')
      if (normalized) bo.imageAliases[normalized] = zhPathValue
    }
  }
}

for (const item of Object.values(bo.representative)) {
  const zhPathValue = bo.imageAliases[item.imageUrl?.split('/').pop()?.replace('.jpg', '') || ''] 
  if (item.name && item.imageUrl) {
    bo.imageAliases[item.name] = item.imageUrl
  }
}

fs.writeFileSync(boPath, JSON.stringify({ heritageContent: bo }, null, 2) + '\n', 'utf8')

let remaining = 0
for (const category of Object.values(bo.national.categories)) {
  for (const item of category.items) {
    if (/[\u4e00-\u9fff]/.test(`${item.name}${item.description || ''}`)) remaining += 1
  }
}
console.log(`Merged ${overlayById.size} overlays; remaining Chinese entries: ${remaining}`)
