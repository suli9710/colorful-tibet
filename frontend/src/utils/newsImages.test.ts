import { describe, expect, it } from 'vitest'
import {
  NEWS_DEFAULT_IMAGE,
  NEWS_FALLBACK_IMAGE,
  markNewsImageFailed,
  resolveNewsImage,
} from './newsImages'
import newsViewSource from '../views/News.vue?raw'

describe('news image fallback', () => {
  it('uses the default image when a news item has no image URL', () => {
    expect(resolveNewsImage({ id: 1, title: 'Policy' }, {})).toBe(NEWS_DEFAULT_IMAGE)
  })

  it('switches to the fallback image after an image load error', () => {
    const failedImages: Record<string, boolean> = {}
    const item = { id: 2, imageUrl: '/images/news/policy.jpg', title: 'Policy' }

    expect(resolveNewsImage(item, failedImages)).toBe('/images/news/policy.jpg')
    markNewsImageFailed(item, failedImages)

    expect(resolveNewsImage(item, failedImages)).toBe(NEWS_FALLBACK_IMAGE)
  })

  it('keeps News.vue free of inline image error handlers', () => {
    expect(newsViewSource).not.toContain('onerror=')
    expect(newsViewSource).toContain('@error="handleNewsImageError')
  })
})
