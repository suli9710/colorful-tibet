// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'
import { createTextCardPopupContent, createTextPopupContent } from './domText'

describe('createTextPopupContent', () => {
  it('renders dynamic popup text without creating HTML nodes from input', () => {
    const popup = createTextPopupContent('<img src=x onerror=alert(1)>', 'Longitude <script>alert(1)</script>')

    expect(popup.querySelector('img')).toBeNull()
    expect(popup.querySelector('script')).toBeNull()
    expect(popup.textContent).toContain('<img src=x onerror=alert(1)>')
    expect(popup.textContent).toContain('Longitude <script>alert(1)</script>')
    expect(popup.innerHTML).toContain('&lt;img')
  })
})

describe('createTextCardPopupContent', () => {
  it('renders each dynamic field as text nodes in card popups', () => {
    const popup = createTextCardPopupContent('<img src=x onerror=alert(1)>', [
      { text: '<svg onload=alert(1)>' },
      { text: 'javascript:alert(1)', color: '#dc2626' }
    ])

    expect(popup.querySelector('img')).toBeNull()
    expect(popup.querySelector('svg')).toBeNull()
    expect(popup.textContent).toContain('<img src=x onerror=alert(1)>')
    expect(popup.textContent).toContain('<svg onload=alert(1)>')
    expect(popup.innerHTML).toContain('&lt;svg')
  })
})
