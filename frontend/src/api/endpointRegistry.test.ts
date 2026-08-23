import { describe, expect, it } from 'vitest'
import { endpoints } from './endpoints'
import authStoreSource from '../stores/auth.ts?raw'
import scenicSpotDetailSource from '../views/ScenicSpotDetail.vue?raw'

describe('endpoint registry guardrails', () => {
  it('exposes registered auth and scenic comment endpoints', () => {
    expect(endpoints.auth.me).toBe('/auth/me')
    expect(endpoints.comments.liked(42)).toBe('/comments/42/liked')
    expect(endpoints.comments.like(42)).toBe('/comments/42/like')
  })

  it('keeps non-SSE fetches and comment like calls on endpoint helpers', () => {
    expect(authStoreSource).toContain('endpoints.auth.me')
    expect(authStoreSource).not.toMatch(/fetch\(\s*`[^`]*\/auth\/me/)
    expect(authStoreSource).not.toMatch(/['"`]\/auth\/me['"`]/)

    expect(scenicSpotDetailSource).toContain('endpoints.comments.list(requestedSpotId)')
    expect(scenicSpotDetailSource).toContain('endpoints.comments.like(comment.id)')
    expect(scenicSpotDetailSource).not.toContain('`/comments/${comment.id}/liked`')
    expect(scenicSpotDetailSource).not.toContain('`/comments/${comment.id}/like`')
    expect(scenicSpotDetailSource).not.toContain('endpoints.comments.liked(comment.id)')
  })
})
