import { statSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import homeSource from '../views/Home.vue?raw'
import { resolveResponsiveImage } from './responsiveImages'

describe('responsive public image assets', () => {
  it('maps known homepage images to AVIF and WebP source sets', () => {
    const sources = resolveResponsiveImage('/heritage/雅鲁藏布大峡谷.jpg')

    expect(sources?.avif).toContain('雅鲁藏布大峡谷-640.avif 640w')
    expect(sources?.avif).toContain('雅鲁藏布大峡谷-1280.avif 1280w')
    expect(sources?.webp).toContain('雅鲁藏布大峡谷-1280.webp 1280w')
    expect(resolveResponsiveImage('https://images.example/remote.jpg')).toBeUndefined()
  })

  it('keeps every generated homepage variant below 200 KiB', () => {
    const files = [
      '布达拉宫3-640.avif', '布达拉宫3-1280.avif', '布达拉宫3-1920.avif',
      '布达拉宫3-640.webp', '布达拉宫3-1280.webp', '布达拉宫3-1920.webp',
      '纳木错-640.avif', '纳木错-999.avif', '纳木错-640.webp', '纳木错-999.webp',
      '藏戏-500.avif', '藏戏-500.webp',
      '雅鲁藏布大峡谷-640.avif', '雅鲁藏布大峡谷-1280.avif',
      '雅鲁藏布大峡谷-640.webp', '雅鲁藏布大峡谷-1280.webp',
    ]

    for (const file of files) {
      const size = statSync(resolve(process.cwd(), 'public', 'heritage', 'optimized', file)).size
      expect(size, file).toBeLessThan(200 * 1024)
    }
  })

  it('renders responsive sources on both the hero and recommendation cards', () => {
    expect(homeSource).toContain('resolveResponsiveImage(heroSlides[currentSlide].image)')
    expect(homeSource).toContain('resolveResponsiveImage(spot.imageUrl)')
    expect(homeSource).toContain('type="image/avif"')
    expect(homeSource).toContain('type="image/webp"')
  })
})
