import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const boPath = path.join(__dirname, '../src/i18n/locales/heritage-content.bo.json')
const zhPath = path.join(__dirname, '../src/i18n/locales/heritage-content.zh.json')
const bo = JSON.parse(fs.readFileSync(boPath, 'utf8')).heritageContent
const zh = JSON.parse(fs.readFileSync(zhPath, 'utf8')).heritageContent

const issues = []
const cjk = /[\u4e00-\u9fff]/
const garbled = /[a-zA-Z]|(?:\u0f71|\u0f72|\u0f74|\u0f7a|\u0f7c)[\u0f90-\u0fbf]/ // Latin or vowel+subjoined stack
const genericDesc = /^བོད་ཀྱི་སྲོལ་རྒྱུན་བཟོ་བའི་ལག་ཤེས།$/

const names = []
const boItems = []
for (const [catKey, cat] of Object.entries(bo.national.categories)) {
  for (const item of cat.items) {
    const text = `${item.name}${item.description || ''}`
    if (cjk.test(text)) issues.push({ type: 'cjk', id: item.id, name: item.name })
    if (garbled.test(item.name) || garbled.test(item.description || '')) {
      issues.push({ type: 'garbled', id: item.id, name: item.name })
    }
    if (genericDesc.test(item.description || '')) {
      issues.push({ type: 'generic', id: item.id, name: item.name })
    }
    names.push(item.name)
    boItems.push({ ...item, categoryKey: catKey })
  }
}

const seenBoNames = new Map()
for (const item of boItems) {
  if (!seenBoNames.has(item.name)) {
    seenBoNames.set(item.name, item)
    continue
  }
  const prev = seenBoNames.get(item.name)
  const prevZh = zh.national.categories[prev.categoryKey]?.items.find((e) => e.id === prev.id)
  const currZh = zh.national.categories[item.categoryKey]?.items.find((e) => e.id === item.id)
  if (prevZh?.name !== currZh?.name) {
    issues.push({ type: 'duplicate-name', name: item.name, ids: [prev.id, item.id] })
  }
}

if (bo.experienceSpots.length !== zh.experienceSpots.length) {
  issues.push({
    type: 'experienceSpots-count',
    bo: bo.experienceSpots.length,
    zh: zh.experienceSpots.length
  })
}
for (const spot of bo.experienceSpots) {
  const text = Object.values(spot).filter((v) => typeof v === 'string').join('')
  if (cjk.test(text)) issues.push({ type: 'cjk-experience', name: spot.name })
}

const tibetanNamesWithoutAlias = []
for (const [catKey, cat] of Object.entries(bo.national.categories)) {
  for (const item of cat.items) {
    if (!bo.imageAliases[item.name]) tibetanNamesWithoutAlias.push(item.name)
  }
}

console.log('=== Heritage BO validation ===')
console.log(`National items: ${names.length}`)
console.log(`Experience spots: ${bo.experienceSpots.length}`)
console.log(`Issues: ${issues.length}`)
if (issues.length) console.log(JSON.stringify(issues.slice(0, 20), null, 2))
console.log(`Tibetan names missing image alias: ${tibetanNamesWithoutAlias.length}`)
process.exit(issues.length ? 1 : 0)
