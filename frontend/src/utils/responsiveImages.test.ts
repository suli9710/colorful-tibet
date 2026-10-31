import { describe, expect, it } from 'vitest'
import { readFileSync, statSync } from 'node:fs'
import { resolve } from 'node:path'
import { responsiveImage, responsiveSrcset } from './responsiveImages'
import manifest from '../data/responsiveImages.json'
import homeSource from '../views/Home.vue?raw'
describe('local responsive artwork', () => {
  it('keeps unknown and external images untouched', () => { expect(responsiveImage('https://example.org/a.jpg')).toBe('https://example.org/a.jpg'); expect(responsiveSrcset('/unknown.jpg')).toBeUndefined() })
  it('uses actual width descriptors and real bounded generated files', () => {
    for (const variants of Object.values(manifest)) for (const variant of variants) {
      expect(variant.width).toBeLessThanOrEqual(1280); expect(variant.width).toBeGreaterThan(0)
      const path = resolve('public', variant.src.slice(1)); expect(statSync(path).size).toBe(variant.bytes)
      expect(variant.bytes).toBeLessThan(400_000)
      const bytes = readFileSync(path); expect(bytes.subarray(0,4).toString()).toBe('RIFF'); expect(bytes.subarray(8,12).toString()).toBe('WEBP')
    }
  })
  it('retains original artwork and chooses a lightweight canyon derivative', () => {
    expect(statSync(resolve('public/heritage/雅鲁藏布大峡谷.jpg')).size).toBeGreaterThan(8_000_000)
    expect(responsiveImage('/heritage/雅鲁藏布大峡谷.jpg')).toMatch(/^\/optimized\/heritage\//)
  })
  it('prioritizes only the hero and provides responsive dimensions', () => {
    expect(homeSource).toContain(':fetchpriority="currentSlide === 0 ? \'high\' : \'auto\'"')
    expect(homeSource).toContain(':srcset="heroSrcset(')
    expect(homeSource).toContain('to="/route-planner" class="tibet-btn w-full')
    expect(statSync(resolve('public/optimized/potala-1280.webp')).size).toBeLessThan(120_000)
  })
})
