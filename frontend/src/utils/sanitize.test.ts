// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'
import { renderMarkdownToSafeHtml, sanitizeHtml } from './sanitize'

describe('HTML sanitization', () => {
  it('removes scripts, event handlers, and unsafe URLs from HTML', () => {
    const clean = sanitizeHtml(`
      <p onclick="alert(1)">Hello</p>
      <script>alert(2)</script>
      <a href="javascript:alert(3)" onmouseover="alert(4)">bad link</a>
      <a href="//evil.example/phish">protocol relative</a>
      <img src="javascript:alert(5)" onerror="alert(6)" alt="bad image">
    `)

    expect(clean.toLowerCase()).not.toContain('<script')
    expect(clean).not.toContain('onclick')
    expect(clean).not.toContain('onmouseover')
    expect(clean).not.toContain('onerror')
    expect(clean.toLowerCase()).not.toContain('javascript:')
    expect(clean).not.toContain('//evil.example')
    expect(clean).toContain('<p>Hello</p>')
  })

  it('preserves product-supported Markdown structure', () => {
    const clean = renderMarkdownToSafeHtml(`
# Title

A [safe link](https://example.com/route) with **bold text** and \`inline code\`.

| Day | Plan |
| --- | --- |
| 1 | Lhasa |

\`\`\`text
Day 1: Lhasa
\`\`\`

![Potala](/images/potala.jpg)
`)

    expect(clean).toContain('<h1>Title</h1>')
    expect(clean).toContain('<a href="https://example.com/route" target="_blank" rel="noopener noreferrer">safe link</a>')
    expect(clean).toContain('<strong>bold text</strong>')
    expect(clean).toContain('<code>inline code</code>')
    expect(clean).toContain('<table>')
    expect(clean).toContain('<pre><code>Day 1: Lhasa')
    expect(clean).toMatch(/<img[^>]+src="\/images\/potala\.jpg"[^>]*>/)
    expect(clean).toMatch(/<img[^>]+alt="Potala"[^>]*>/)
  })

  it('sanitizes raw HTML and javascript links embedded in Markdown', () => {
    const clean = renderMarkdownToSafeHtml(`
[bad](javascript:alert(1))

<img src="/images/route.jpg" onerror="alert(2)" alt="route">
<iframe src="https://example.com/embed"></iframe>
`)

    expect(clean.toLowerCase()).not.toContain('javascript:')
    expect(clean).not.toContain('onerror')
    expect(clean.toLowerCase()).not.toContain('<iframe')
    expect(clean).toMatch(/<img[^>]+src="\/images\/route\.jpg"[^>]*>/)
  })

  it('normalizes blank-target links and auto-hardens absolute http links', () => {
    const clean = sanitizeHtml(`
      <a href="https://example.com" target=" _BLANK " rel="opener">external</a>
      <a href="https://docs.example/route">auto external</a>
      <a href="/local" target="sidebar" rel="opener">local</a>
      <a target="_blank" rel="opener">missing href</a>
      <p target="_blank" rel="bookmark">not a link</p>
    `)

    expect(clean.match(/target="_blank"/g)?.length).toBe(2)
    expect(clean.match(/rel="noopener noreferrer"/g)?.length).toBe(2)
    expect(clean).toContain('<a href="https://docs.example/route" target="_blank" rel="noopener noreferrer">auto external</a>')
    expect(clean).not.toContain('rel="opener"')
    expect(clean).not.toContain('target="sidebar"')
    expect(clean).not.toContain('<p target=')
    expect(clean).not.toContain('<p rel=')
  })

  it('keeps image sources to local, same-origin, or safe inline URLs', () => {
    const sameOriginImage = `${window.location.origin}/uploads/comment/potala.jpg`
    const pngData = 'data:image/png;base64,iVBORw0KGgo='
    const clean = sanitizeHtml(`
      <img src="https://images.example/potala.jpg" alt="remote">
      <img src="${sameOriginImage}" alt="same origin">
      <img src="/uploads/comment/potala.jpg" alt="upload">
      <img src="../images/potala.jpg" alt="relative">
      <img src="${pngData}" alt="inline png">
      <img src="mailto:security@example.com" alt="mail">
      <img src="tel:+123456789" alt="phone">
      <img src="#preview" alt="fragment">
      <img src="?preview=1" alt="query">
    `)

    expect(clean).not.toContain('images.example')
    expect(clean).not.toContain('alt="remote"')
    expect(clean).toContain(`src="${sameOriginImage}"`)
    expect(clean).toMatch(/<img[^>]+src="\/uploads\/comment\/potala\.jpg"[^>]*>/)
    expect(clean).toMatch(/<img[^>]+src="\.\.\/images\/potala\.jpg"[^>]*>/)
    expect(clean).toContain(`src="${pngData}"`)
    expect(clean).not.toContain('src="mailto:')
    expect(clean).not.toContain('src="tel:')
    expect(clean).not.toContain('src="#preview"')
    expect(clean).not.toContain('src="?preview=1"')
  })

  it('keeps rich text images accessible and lazy-loaded after sanitizing', () => {
    const clean = sanitizeHtml(`
      <img src="/uploads/comment/no-alt.jpg">
      <img src="/uploads/comment/eager.jpg" alt="hero" loading="eager">
      <img src="/uploads/comment/bad-loading.jpg" alt="bad loading" loading="auto" decoding="sync">
    `)
    const root = document.createElement('div')
    root.innerHTML = clean
    const images = Array.from(root.querySelectorAll('img'))

    expect(images).toHaveLength(3)
    expect(images[0]?.getAttribute('alt')).toBe('')
    expect(images[0]?.getAttribute('loading')).toBe('lazy')
    expect(images[0]?.getAttribute('decoding')).toBe('async')
    expect(images[1]?.getAttribute('alt')).toBe('hero')
    expect(images[1]?.getAttribute('loading')).toBe('eager')
    expect(images[1]?.getAttribute('decoding')).toBe('async')
    expect(images[2]?.getAttribute('loading')).toBe('lazy')
    expect(images[2]?.getAttribute('decoding')).toBe('async')
  })

  it('removes remote Markdown images while preserving local upload Markdown images', () => {
    const clean = renderMarkdownToSafeHtml(`
![remote tracker](https://tracker.example/pixel.png)
![local upload](/uploads/routes/day-1.jpg)
`)

    expect(clean).not.toContain('tracker.example')
    expect(clean).not.toContain('remote tracker')
    expect(clean).toMatch(/<img[^>]+src="\/uploads\/routes\/day-1\.jpg"[^>]*>/)
    expect(clean).toMatch(/<img[^>]+alt="local upload"[^>]*>/)
  })

  it('forbids dangerous rich embed tags and encoded unsafe protocols', () => {
    const clean = sanitizeHtml(`
      <object data="https://example.com/app.swf"></object>
      <embed src="https://example.com/app.swf">
      <iframe srcdoc="<script>alert(1)</script>"></iframe>
      <a href="java&#x0D;script:alert(1)">encoded bad link</a>
      <a href="data:text/html,<script>alert(2)</script>">data bad link</a>
      <img src="data:image/svg+xml,<svg onload=alert(3)>" alt="data image">
    `)

    expect(clean.toLowerCase()).not.toContain('<object')
    expect(clean.toLowerCase()).not.toContain('<embed')
    expect(clean.toLowerCase()).not.toContain('<iframe')
    expect(clean.toLowerCase()).not.toContain('javascript:')
    expect(clean.toLowerCase()).not.toContain('data:text/html')
    expect(clean.toLowerCase()).not.toContain('data:image/svg')
    expect(clean).toContain('encoded bad link')
    expect(clean).toContain('data bad link')
  })

  it('blocks mixed rich text XSS payloads while keeping safe link and image policies', () => {
    const clean = sanitizeHtml(`
      <math><mtext><table><mglyph><style><!--</style><img title="--><img src=x onerror=alert(1)>"></mglyph></table></mtext></math>
      <svg><a xlink:href="javascript:alert(2)">svg link</a></svg>
      <a href="JaVaScRiPt:alert(3)" target="_blank">case protocol</a>
      <a href="https://example.com/docs" rel="opener">docs</a>
      <img src="/uploads/comment/potala.jpg" alt="Potala" loading="interactive" onload="alert(4)">
    `)
    const root = document.createElement('div')
    root.innerHTML = clean
    const links = Array.from(root.querySelectorAll('a'))
    const images = Array.from(root.querySelectorAll('img'))

    expect(clean.toLowerCase()).not.toContain('<svg')
    expect(clean.toLowerCase()).not.toContain('<math')
    expect(clean.toLowerCase()).not.toContain('<style')
    expect(clean.toLowerCase()).not.toContain('javascript:')
    expect(clean).not.toContain('onerror')
    expect(clean).not.toContain('onload')
    expect(links.some(link => link.textContent === 'case protocol' && link.hasAttribute('href'))).toBe(false)
    expect(links.find(link => link.textContent === 'docs')?.getAttribute('target')).toBe('_blank')
    expect(links.find(link => link.textContent === 'docs')?.getAttribute('rel')).toBe('noopener noreferrer')
    expect(images).toHaveLength(1)
    expect(images[0]?.getAttribute('src')).toBe('/uploads/comment/potala.jpg')
    expect(images[0]?.getAttribute('loading')).toBe('lazy')
    expect(images[0]?.getAttribute('decoding')).toBe('async')
  })
})
