import { describe, expect, it } from 'vitest'
import { buildOfflineTrip, safeSourceUrl, tripEstimate, type WorkbenchTrip } from './tripWorkbench'
const trip: WorkbenchTrip = { id: 1, title: '拉萨 <script>alert(1)</script>', days: 1, totalEstimatedCost: 300, itineraryDays: [{id: 1, dayNumber: 1, title: '第一天', items: [{id: 1, title: '散步', itemType: 'ACTIVITY', estimatedCost: 0}]}] }
describe('truthful offline trip projection', () => {
  it.each(['javascript:alert(1)', 'data:text/html,a', 'http://example.com', 'https://user:pass@example.com', '/relative'])('rejects unsafe source %s', (value) => expect(safeSourceUrl(value)).toBeNull())
  it('allows HTTPS sources', () => expect(safeSourceUrl('https://wlt.xizang.gov.cn/guide')).toBe('https://wlt.xizang.gov.cn/guide'))
  it.each([null, undefined, NaN, Infinity, -1, '300'])('does not invent a price for %s', (value) => expect(tripEstimate(value)).toBe('待确认'))
  it('preserves a real zero estimate', () => expect(tripEstimate(0)).toBe('¥0'))
  it('escapes content and supplies a restrictive offline CSP', () => {
    const html = buildOfflineTrip(trip, new Set(), '2026-10-31')
    expect(html).not.toContain('<script>'); expect(html).toContain('&lt;script&gt;'); expect(html).toContain("default-src 'none'"); expect(html).toContain('不是预订确认或医疗建议'); expect(html).toContain('2026-10-31')
  })
  it('does not export response extras such as contact details or health data', () => {
    const input = {...trip, phone: '13800138000', healthHistory: 'PRIVATE', traveler: { passport: 'PASSPORT' }}
    const html = buildOfflineTrip(input, new Set())
    expect(html).not.toContain('13800138000'); expect(html).not.toContain('PRIVATE'); expect(html).not.toContain('PASSPORT')
  })
  it('minimizes personal data embedded in public free text', () => {
    const html = buildOfflineTrip({...trip, title:'联系 13800138000 me@example.org 110101199001011234', itineraryDays:[{...trip.itineraryDays[0], title:'护照号: SECRET123', items:[{...trip.itineraryDays[0].items[0], alternatives:'病史：PRIVATE'}]}]}, new Set())
    for (const secret of ['13800138000','me@example.org','110101199001011234','SECRET123','PRIVATE']) expect(html).not.toContain(secret)
  })
  it('rejects lookalike or unreviewed source origins', () => {
    expect(safeSourceUrl('https://wlt.xizang.gov.cn.attacker.example/')).toBeNull()
    expect(safeSourceUrl('https://example.org/')).toBeNull()
  })
  it('exports all days and preparation state without claiming verification', () => {
    const html = buildOfflineTrip(trip, new Set(['reservation']))
    expect(html).toContain('第一天'); expect(html).toContain('☑'); expect(html).toContain('没有完整核验结果'); expect(html).toContain('仅表示您完成了个人准备')
  })
  it('omits unsafe sources and renders dated reference-only sources', () => {
    const input = {...trip, readiness: {ruleVersion:'1',evaluatedAt:'2026-10-31',status:'BLOCKED',issues:[{code:'UNKNOWN',severity:'BLOCKING',message:'待查'}],checklist:[],sources:[{id:'a',title:'Bad',url:'javascript:alert(1)'},{id:'b',title:'Official',url:'https://wlt.xizang.gov.cn'}],disclaimer:'待确认'}}
    const html = buildOfflineTrip(input, new Set()); expect(html).not.toContain('javascript:'); expect(html).toContain('需处理：待查'); expect(html).toContain('尚未核验当前有效性')
  })
})
