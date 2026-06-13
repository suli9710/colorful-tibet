// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createApp, ref, type App } from 'vue'
import PrayerFlags from './PrayerFlags.vue'

vi.mock('motion-v', () => ({
  useReducedMotion: () => ref(false)
}))

const mounted: App[] = []

const mount = (props: Record<string, unknown> = {}) => {
  const root = document.createElement('div')
  document.body.appendChild(root)
  const app = createApp(PrayerFlags, props)
  mounted.push(app)
  app.mount(root)
  return root
}

afterEach(() => {
  mounted.splice(0).forEach(app => app.unmount())
  document.body.innerHTML = ''
})

describe('PrayerFlags', () => {
  it('renders the requested number of flags and stays decorative', () => {
    const root = mount({ count: 10 })
    const container = root.querySelector('.prayer-flags')

    expect(container).toBeTruthy()
    expect(container?.getAttribute('aria-hidden')).toBe('true')
    expect(root.querySelectorAll('.prayer-flag')).toHaveLength(10)
  })

  it('cycles through the canonical five-element prayer-flag colours in order', () => {
    const root = mount({ count: 6 })
    const colors = Array.from(root.querySelectorAll<HTMLElement>('.prayer-flag')).map(flag =>
      flag.style.getPropertyValue('--flag-color')
    )

    expect(colors.slice(0, 5)).toEqual(['#2D5F8A', '#F7F3EE', '#8B2E3A', '#3BA99C', '#F2C94C'])
    // The sixth flag wraps back to the first colour.
    expect(colors[5]).toBe('#2D5F8A')
  })

  it('defaults to at least one flag even with a non-positive count', () => {
    const root = mount({ count: 0 })
    expect(root.querySelectorAll('.prayer-flag').length).toBeGreaterThanOrEqual(1)
  })
})
