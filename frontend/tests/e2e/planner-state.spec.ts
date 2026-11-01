import { expect, test } from '@playwright/test'

const routeContent = (name: string) => `# ${name}\n\n## 第1天 拉萨适应\n抵达后休息。\n\n## 第2天 城市慢游\n游览街区，开放情况待确认。`

test('editing next-trip preferences leaves the existing itinerary, notes and export metadata intact', async ({ page }) => {
  const content = routeContent('原来的两天行程')
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname
    if (!path.startsWith('/api/')) return route.continue()
    const send = (data: unknown) => route.fulfill({ contentType: 'application/json', body: JSON.stringify(data) })
    if (path === '/api/auth/me') return send({ nickname: '状态测试用户', role: 'USER' })
    if (path === '/api/routes/ai/latest') return send({ id: 51, days: 2, budget: 'comfort', preference: 'natural', status: 'COMPLETED', content })
    return send([])
  })
  await page.goto('/route-planner')
  await expect(page.locator('.review-day')).toHaveCount(2)
  await page.locator('#route-review-note-0').fill('原行程第一天的备注')
  await page.getByRole('button', { name: '减少计划天数' }).click()
  await expect(page.locator('#route-planner-days-value')).toHaveText('1')
  // Changing the next request must not remove day 2 from the existing review.
  await expect(page.locator('.review-day')).toHaveCount(2)
  await expect(page.locator('#route-review-note-0')).toHaveValue('原行程第一天的备注')
  const downloadEvent = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出行程与备注', exact: true }).click()
  expect((await downloadEvent).suggestedFilename()).toBe('西藏2日游行程.md')
})

test('a delayed response for the previous itinerary cannot replace the new itinerary details', async ({ page }) => {
  let generationCount = 0
  let detailsCount = 0
  let releaseOldDetails: (() => void) | undefined
  let oldDetailsFinished = false
  const oldDetailsGate = new Promise<void>(resolve => { releaseOldDetails = resolve })
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname
    if (!path.startsWith('/api/')) return route.continue()
    const send = (data: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) })
    if (path === '/api/auth/me') return send({ nickname: '状态测试用户', role: 'USER' })
    if (path === '/api/routes/ai/latest') return route.fulfill({ status: 204 })
    if (path === '/api/routes/generate/jobs') {
      generationCount++
      return send({ jobId: `state-job-${generationCount}`, routeRecordId: 50 + generationCount,
        days: 2, budget: 'comfort', preference: 'natural', status: 'COMPLETED',
        content: routeContent(`第${generationCount}份行程`), cached: false })
    }
    if (path === '/api/itineraries/generate') {
      detailsCount++
      const index = detailsCount
      if (index === 1) await oldDetailsGate
      await send({ id: 80 + index, title: index === 1 ? '旧行程明细不应回填' : '当前新行程明细',
        days: 2, versionLabel: '标准', totalEstimatedCost: index * 1000, itineraryDays: [] })
      if (index === 1) oldDetailsFinished = true
      return
    }
    if (path.endsWith('/quote')) return send({ totalEstimatedCost: 2000, bookableTotal: 500, informationalTotal: 1500, currency: 'CNY' })
    if (path.endsWith('/travel-kit')) return send({}, 503)
    return send([])
  })
  await page.goto('/route-planner')
  await page.locator('button[type="submit"]').click()
  await expect.poll(() => detailsCount).toBe(1)
  await expect(page.locator('.route-result-prose')).toContainText('第1份行程')
  await page.locator('button[type="submit"]').click()
  await page.getByRole('dialog').getByRole('button', { name: '重新生成', exact: true }).click()
  await expect(page.getByText('当前新行程明细', { exact: true })).toBeVisible()
  releaseOldDetails!()
  await expect.poll(() => oldDetailsFinished).toBe(true)
  await page.evaluate(() => new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve()))))
  await expect(page.getByText('旧行程明细不应回填', { exact: true })).toHaveCount(0)
  await expect(page.getByText('当前新行程明细', { exact: true })).toBeVisible()
  expect(generationCount).toBe(2)
})
