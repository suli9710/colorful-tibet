import { expect, test, type Page } from '@playwright/test'

const json = (value: unknown) => JSON.stringify(value)

const stubPublicApi = async (page: Page) => {
  await page.route('**/api/**', async route => {
    const pathname = new URL(route.request().url()).pathname
    if (!pathname.startsWith('/api/')) {
      await route.continue()
      return
    }

    if (pathname === '/api/auth/me') {
      await route.fulfill({ status: 401, contentType: 'application/json', body: json({}) })
      return
    }

    const body = pathname === '/api/spots'
      ? { content: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }
      : []
    await route.fulfill({ status: 200, contentType: 'application/json', body: json(body) })
  })
}

test('boots, lazy-loads Tibetan, persists locale, and navigates', async ({ page }) => {
  const runtimeErrors: string[] = []
  page.on('pageerror', error => runtimeErrors.push(error.message))
  page.on('console', message => {
    const text = message.text()
    const expectedGuestSessionRejection = text === (
      'Failed to load resource: the server responded with a status of 401 (Unauthorized)'
    )
    if (message.type() === 'error' && !expectedGuestSessionRejection) runtimeErrors.push(text)
  })

  await page.emulateMedia({ reducedMotion: 'reduce' })
  await stubPublicApi(page)
  await page.goto('/')

  await expect(page).toHaveTitle('首页 | 七彩西藏')
  await expect(page.getByRole('heading', { level: 1, name: '走进西藏' })).toBeVisible()

  await page.getByRole('button', { name: '藏文' }).click()
  await expect(page.locator('html')).toHaveAttribute('lang', 'bo')
  await expect(page).toHaveTitle('ཁྱིམ་ཤོག | བོད་ཀྱི་ཚོན་མདངས།')
  await expect(page.getByRole('heading', { level: 1, name: 'བོད་ལྗོངས་ལ་ཕེབས་པ།' })).toBeVisible()

  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('lang', 'bo')
  await expect(page).toHaveTitle('ཁྱིམ་ཤོག | བོད་ཀྱི་ཚོན་མདངས།')

  await page.getByRole('link', { name: 'གཟིགས་སྐོར་ས་གནས།', exact: true }).first().click()
  await expect(page).toHaveURL(/\/spots$/)
  await expect(page).toHaveTitle('གཟིགས་སྐོར་ས་གནས། | བོད་ཀྱི་ཚོན་མདངས།')
  expect(runtimeErrors).toEqual([])
})
