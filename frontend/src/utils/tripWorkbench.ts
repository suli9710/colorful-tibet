/** Public-only projection: never serialize the server response wholesale into an offline pack. */
export interface TripIssue { code: string; severity: string; dayNumber?: number | null; itemId?: number | null; message: string }
export interface TripSource { id: string; title: string; url: string; status?: string; lastVerifiedAt?: string | null }
export interface TripChecklist { id: string; title: string; status?: string; sourceId?: string }
export interface TripReadiness { ruleVersion: string; evaluatedAt: string; status: string; issues: TripIssue[]; sources: TripSource[]; checklist: TripChecklist[]; disclaimer: string }
export interface TripItem { id: number; version?: number; title: string; itemType: string; startTime?: string; durationMinutes?: number; estimatedCost?: number; description?: string; alternatives?: string; bookingAction?: string; bookingStatus?: string; bookingReferenceId?: number | null; hotelName?: string; scenicSpotName?: string }
export interface TripDay { id: number; dayNumber: number; title: string; travelDate?: string; region?: string; items: TripItem[] }
export interface WorkbenchTrip { id: number; title: string; days: number; travelers?: number | null; startDate?: string; totalEstimatedCost?: number; itineraryDays: TripDay[]; readiness?: TripReadiness }
export const safeSourceUrl = (value: string): string | null => {
  try {
    const url = new URL(value)
    const officialHosts = new Set(['wlt.xizang.gov.cn', 'www.lasa.gov.cn', 's.nia.gov.cn', 'www.nia.gov.cn', 'www.cdc.gov'])
    return url.protocol === 'https:' && !url.username && !url.password && !url.port && officialHosts.has(url.hostname) ? url.href : null
  } catch { return null }
}
export const minimizeTripText = (value: unknown) => String(value ?? '')
  .replace(/^[\t ]*(?:[-*][\t ]*)?(?:身份证(?:号|号码)?|护照(?:号|号码)?|证件号|手机号|联系电话|病史|健康史|用药|医疗记录|passport|medical history|medications)\s*[:：]\s*[^\r\n]*/gim, '[私人资料已隐藏]')
  .replace(/(?<![A-Za-z0-9])\d{17}[0-9Xx](?![A-Za-z0-9])/g, '[证件号码已隐藏]')
  .replace(/(?<![A-Za-z0-9])(?:\+?86[- ]?)?1[3-9]\d{9}(?![A-Za-z0-9])/g, '[手机号码已隐藏]')
  .replace(/(?<![A-Za-z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}(?![A-Za-z0-9])/gi, '[邮箱已隐藏]')
const escapeHtml = (text: unknown) => minimizeTripText(text).replace(/[&<>"']/g, (c) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]!))
export const tripEstimate = (value: unknown) => typeof value === 'number' && Number.isFinite(value) && value >= 0 ? `¥${value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })}` : '待确认'
export const defaultPreparation: TripChecklist[] = [
  { id: 'reservation', title: '按实际出发日期向景区确认开放时间与预约要求' },
  { id: 'documents', title: '按身份与目的地向官方确认所需证件，不自动判定出行资格' },
  { id: 'lodging', title: '核对每晚住宿地点、人数容量、实际可订价与退改条件' },
  { id: 'transport', title: '出发前确认交通、天气及道路情况，保留替代安排' },
  { id: 'health', title: '高原出行安排如有疑问，请咨询合格医疗专业人员' }
]
export function buildOfflineTrip(trip: WorkbenchTrip, completed: ReadonlySet<string>, generatedAt = new Date().toISOString()): string {
  const readiness = trip.readiness
  const sources = (readiness?.sources ?? []).flatMap((source) => { const url = safeSourceUrl(source.url); return url ? [`<li><a href="${escapeHtml(url)}" rel="noreferrer">${escapeHtml(source.title)}</a> · ${source.lastVerifiedAt ? `最后核验 ${escapeHtml(source.lastVerifiedAt)}` : '仅参考，尚未核验当前有效性'}</li>`] : [] })
  const checklist = readiness?.checklist?.length ? readiness.checklist : defaultPreparation
  const days = trip.itineraryDays.map((day) => `<section><h2>第 ${escapeHtml(day.dayNumber)} 天 · ${escapeHtml(day.title)}</h2><p>${escapeHtml(day.travelDate || '日期待确认')} ${escapeHtml(day.region)}</p><ol>${day.items.map((item) => `<li><strong>${escapeHtml(item.startTime || '时间待确认')} · ${escapeHtml(item.title)}</strong><p>参考估价 ${escapeHtml(tripEstimate(item.estimatedCost))} · 实际库存、预约与履约需向提供方确认</p>${item.alternatives ? `<p>替代安排：${escapeHtml(item.alternatives)}</p>` : ''}</li>`).join('')}</ol></section>`).join('')
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'"><title>${escapeHtml(trip.title)} · 离线行前包</title><style>body{font:16px/1.7 system-ui,sans-serif;color:#322b27;background:#faf8f4;margin:0}main{max-width:850px;margin:auto;padding:32px 20px}h1,h2{color:#762c36}section,aside{background:white;border:1px solid #ddd4c7;padding:20px;margin:20px 0;border-radius:12px}li{margin:12px 0}a{color:#174f70;overflow-wrap:anywhere}.note{color:#634c2d}@media print{body{background:white}section,aside{break-inside:avoid}a{color:inherit}}</style></head><body><main><p>七彩西藏 · 离线行前包</p><h1>${escapeHtml(trip.title)}</h1><p>导出时间：${escapeHtml(generatedAt)} · 总参考估价：${escapeHtml(tripEstimate(trip.totalEstimatedCost))}</p><aside class="note"><strong>离线内容可能过时，不是预订确认或医疗建议。</strong><p>出发前重新核对官方信息、库存、交通与价格。已勾选仅表示您完成了个人准备，不代表平台核验。导出不包含账户资料，并尽量隐藏常见私人信息；分享前仍请自行检查自由文本。</p><p>规则版本：${escapeHtml(readiness?.ruleVersion || '尚无核验记录')}；评估时间：${escapeHtml(readiness?.evaluatedAt || '未知')}</p></aside><section><h2>待解决事项</h2><ul>${(readiness?.issues ?? []).map((issue) => `<li>${issue.severity === 'BLOCKING' ? '需处理' : '需确认'}：${escapeHtml(issue.message)}</li>`).join('') || '<li>没有完整核验结果，请逐项确认行程信息。</li>'}</ul></section>${days}<section><h2>个人准备清单</h2><ul>${checklist.map((item) => `<li>${completed.has(item.id) ? '☑' : '☐'} ${escapeHtml(item.title)}</li>`).join('')}</ul></section><section><h2>信息来源</h2><ul>${sources.join('') || '<li>尚无来源记录，请访问景区及相关官方渠道核实。</li>'}</ul></section></main></body></html>`
}
