import { describe, expect, it } from 'vitest'
import appSource from '../App.vue?raw'
import navBarSource from './NavBar.vue?raw'

describe('app shell accessibility safeguards', () => {
  it('keeps keyboard users able to skip persistent navigation', () => {
    expect(appSource).toContain('href="#main-content"')
    expect(appSource).toContain('id="main-content"')
    expect(appSource).toContain('ref="mainContent"')
    expect(appSource).toContain('tabindex="-1"')
    expect(appSource).toContain('class="skip-link"')
  })

  it('keeps route transitions announced without blocking the page', () => {
    expect(appSource).toContain('role="progressbar"')
    expect(appSource).toContain(':aria-busy="isNavigating"')
    expect(appSource).toContain('role="status"')
    expect(appSource).toContain('aria-live="polite"')
    expect(appSource).toContain('mainContent.value?.focus({ preventScroll: true })')
  })

  it('keeps mobile menu scroll lock wired through the shell', () => {
    expect(navBarSource).toContain("document.body.classList.toggle('nav-menu-open', locked)")
    expect(navBarSource).toContain('syncBodyScrollLock(false)')
  })
})
