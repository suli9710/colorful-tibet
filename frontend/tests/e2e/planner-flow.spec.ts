import { expect, test, type Page } from '@playwright/test'
import { readFile } from 'node:fs/promises'

// Browser behavior fixtures only: these do not certify a live backend or a booking.
const content = '# 西藏慢游计划\n\n## 第1天 拉萨适应\n上午抵达拉萨，下午休息，减少剧烈活动。\n\n## 第2天 城市漫步\n参观文化街区，开放与预约需确认。'
const record = { id: 41, days: 2, budget: 'comfort', preference: 'natural', status: 'COMPLETED', content, manuallySaved: false }

async function fixture(page: Page, restored = true) {
  const counts = { generate: 0, save: 0, details: 0, quote: 0 }
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname
    if (!path.startsWith('/api/')) return route.continue()
    const send = (data: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) })
    if (path === '/api/auth/me') return send({ nickname: '界面测试用户', role: 'USER' })
    if (path === '/api/routes/ai/latest') return restored ? send(record) : route.fulfill({ status: 204 })
    if (path === '/api/routes/ai/41/save') { counts.save++; return send({ ...record, manuallySaved: true }) }
    if (path === '/api/routes/generate/jobs') {
      counts.generate++
      await new Promise(resolve => setTimeout(resolve, 350))
      return send({ ...record, jobId: 'fixture-job', routeRecordId: 41, cached: false })
    }
    if (path === '/api/itineraries/generate') {
      counts.details++
      return send({ id: 81, title: '每日安排（测试响应）', days: 2, versionLabel: '标准', totalEstimatedCost: 1200, itineraryDays: [] })
    }
    if (path === '/api/itineraries/81/quote') {
      counts.quote++
      return counts.quote === 1 ? send({}, 503) : send({ totalEstimatedCost: 1200, bookableTotal: 400, informationalTotal: 800, currency: 'CNY' })
    }
    if (path.endsWith('/travel-kit')) return send({}, 503)
    return send([])
  })
  return counts
}

test('review preserves notes, confirms replacement, exports notes and saves once', async ({ page }) => {
  const counts = await fixture(page)
  await page.goto('/route-planner')
  const note = page.locator('#route-review-note-0')
  await note.fill('下午留给休息，不增加景点。')
  await page.getByRole('button', { name: '查看第1天的原安排' }).click()
  await expect(page.locator('.route-result-prose')).toContainText('下午休息')
  await page.getByRole('button', { name: '生成专属行程', exact: true }).first().click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: '保留当前行程', exact: true }).click()
  await expect(note).toHaveValue('下午留给休息，不增加景点。')
  expect(counts.generate).toBe(0)
  await page.getByRole('button', { name: '清空第1天的调整备注' }).click()
  await page.getByRole('dialog').getByRole('button', { name: '清空备注', exact: true }).click()
  await page.getByRole('button', { name: '撤销清空' }).click()
  await expect(note).toHaveValue('下午留给休息，不增加景点。')
  const downloadEvent = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出行程与备注' }).click()
  const download = await downloadEvent
  const file = await readFile((await download.path())!, 'utf8')
  expect(file).toContain('下午留给休息，不增加景点。')
  expect(file).toContain('不代表实时价格、库存或可订承诺')
  await page.locator('#planner-check').getByRole('button', { name: '保存到我的路线', exact: true }).click()
  await expect(page.getByRole('button', { name: '已保存原行程' })).toBeDisabled()
  expect(counts.save).toBe(1)
  await page.getByRole('link', { name: '先看看目的地' }).click()
  await expect(page).toHaveURL(/\/spots$/)
  await page.goBack()
  await expect(page).toHaveURL(/\/route-planner$/)
  await expect(note).toHaveValue('下午留给休息，不增加景点。')
  await note.click()
  const refreshPrompt = page.waitForEvent('dialog')
  await page.evaluate(() => { setTimeout(() => location.reload(), 0) })
  const dialog = await refreshPrompt
  expect(dialog.type()).toBe('beforeunload')
  await dialog.dismiss()
  await expect(note).toHaveValue('下午留给休息，不增加景点。')
})

test('generation deduplicates and missing quote can retry without replacing itinerary', async ({ page }) => {
  const counts = await fixture(page, false)
  await page.goto('/route-planner')
  const generate = page.locator('button[type="submit"]')
  await generate.evaluate(button => { (button as HTMLButtonElement).click(); (button as HTMLButtonElement).click() })
  await expect(page.getByRole('button', { name: '重试每日明细' })).toBeVisible()
  expect(counts.generate).toBe(1)
  expect(counts.details).toBe(1)
  await expect(page.getByText('待获取', { exact: true }).first()).toBeVisible()
  await page.getByRole('button', { name: '重试每日明细' }).click()
  await expect(page.getByRole('button', { name: '重试出行提醒' })).toBeVisible()
  expect(counts.details).toBe(1)
  expect(counts.quote).toBe(2)
})

test('preferences survive refresh; offline generation is disabled', async ({ page, context }) => {
  await fixture(page, false)
  await page.goto('/route-planner')
  await page.getByRole('button', { name: '增加计划天数' }).click()
  await expect(page.locator('#route-planner-days-value')).toHaveText('8')
  await page.reload()
  await expect(page.locator('#route-planner-days-value')).toHaveText('8')
  await context.setOffline(true)
  await expect(page.getByText(/当前离线/)).toBeVisible()
  await expect(page.locator('button[type="submit"]')).toBeDisabled()
  await context.setOffline(false)
  await expect(page.locator('button[type="submit"]')).toBeEnabled()
})

test('review reflows on desktop, tablet, mobile, narrow and 200% content zoom', async ({ page }) => {
  await fixture(page)
  await page.goto('/route-planner')
  await expect(page.locator('#route-review-note-0')).toBeVisible()
  for (const [name, width, height] of [['desktop', 1440, 1000], ['tablet', 768, 1024], ['mobile', 390, 844], ['narrow', 320, 812]] as const) {
    await page.setViewportSize({ width, height })
    await page.locator('#planner-review').scrollIntoViewIfNeeded()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({ path: `../docs/verification/ui-flow/result-fixture-${name}.png` })
  }
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.evaluate(() => { document.documentElement.style.zoom = '2' })
  await page.locator('#planner-review').scrollIntoViewIfNeeded()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  await page.screenshot({ path: '../docs/verification/ui-flow/result-fixture-content-zoom-200.png' })
  await page.evaluate(() => { document.documentElement.style.zoom = '' })
  await page.locator('#route-review-note-0').focus()
  await page.keyboard.press('Tab')
  await expect(page.getByRole('button', { name: '查看第1天的原安排' })).toBeFocused()
})
