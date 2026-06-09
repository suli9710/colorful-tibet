import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import heritageSource from './Heritage.vue?raw'
import hotelOrdersSource from './HotelOrders.vue?raw'
import newsSource from './News.vue?raw'
import orderCenterSource from './OrderCenter.vue?raw'
import questionDetailSource from './QuestionDetail.vue?raw'
import routeCommunitySource from './RouteCommunity.vue?raw'
import routeDetailSource from './RouteDetail.vue?raw'
import routePlannerSource from './RoutePlanner.vue?raw'
import scenicSpotDetailSource from './ScenicSpotDetail.vue?raw'
import scenicSpotsSource from './ScenicSpots.vue?raw'
import userProfileSource from './UserProfile.vue?raw'
import extraMessages from '../i18n/locales/extra.json'

const criticalViewSources = [
  ['News.vue', newsSource],
  ['Heritage.vue', heritageSource],
  ['RouteCommunity.vue', routeCommunitySource],
  ['QuestionDetail.vue', questionDetailSource],
  ['RouteDetail.vue', routeDetailSource],
  ['ScenicSpots.vue', scenicSpotsSource],
  ['ScenicSpotDetail.vue', scenicSpotDetailSource],
  ['OrderCenter.vue', orderCenterSource],
  ['HotelOrders.vue', hotelOrdersSource],
  ['UserProfile.vue', userProfileSource],
  ['RoutePlanner.vue', routePlannerSource]
] as const

const getMessage = (messages: Record<string, any>, key: string) =>
  key.split('.').reduce<unknown>((value, segment) => {
    if (!value || typeof value !== 'object') return undefined
    return (value as Record<string, unknown>)[segment]
  }, messages)

const readLocaleMessages = (relativePath: string) =>
  JSON.parse(readFileSync(new URL(relativePath, import.meta.url), 'utf8')) as Record<string, any>

const baseLocaleMessages = [
  ['zh', readLocaleMessages('../i18n/locales/zh.json')],
  ['bo', readLocaleMessages('../i18n/locales/bo.json')]
] as const

const scenicSpotsContractKeys = [
  'spots.title',
  'spots.subtitle',
  'spots.category.all',
  'spots.category.natural',
  'spots.category.cultural',
  'spots.ticketFrom',
  'spots.resultSummary',
  'spots.resultSummaryWithKeyword',
  'spots.loadingLabel',
  'spots.searchPlaceholder',
  'spots.searchHelp',
  'spots.clearSearch',
  'spots.paginationLabel',
  'spots.filterByCategory',
  'spots.loadErrorTitle',
  'spots.retryLoad',
  'spots.cardAria',
  'spots.visitCount',
  'spots.visitCountValue',
  'spots.viewDetails',
  'spots.noDataTitle',
  'spots.noDataMessage',
  'spots.checkList',
  'spots.checkItem1',
  'spots.checkItem2',
  'spots.checkItem3',
  'spots.reload',
  'spots.noCategoryTitle',
  'spots.noCategoryMessage',
  'spots.noSearchMessage',
  'spots.showAll'
] as const

const scenicSpotsParameterizedKeys = [
  ['spots.resultSummary', '{count}'],
  ['spots.resultSummary', '{category}'],
  ['spots.resultSummaryWithKeyword', '{base}'],
  ['spots.resultSummaryWithKeyword', '{keyword}'],
  ['spots.filterByCategory', '{category}'],
  ['spots.cardAria', '{name}'],
  ['spots.visitCountValue', '{count}']
] as const

describe('critical flow i18n coverage', () => {
  it('keeps high-traffic views off hard-coded Intl locale tags', () => {
    for (const [fileName, source] of criticalViewSources) {
      expect(source, fileName).not.toMatch(/\b(?:zh-CN|bo-CN)\b/)
    }
  })

  it('keeps community and spot detail dates bound to the active i18n locale', () => {
    for (const [fileName, source] of [
      ['RouteCommunity.vue', routeCommunitySource],
      ['QuestionDetail.vue', questionDetailSource],
      ['ScenicSpotDetail.vue', scenicSpotDetailSource],
      ['Heritage.vue', heritageSource]
    ] as const) {
      expect(source, fileName).toContain("toIntlLocale")
      expect(source, fileName).toContain('toIntlLocale(locale.value)')
      expect(source, fileName).not.toContain('readBrowserStorage')
    }
  })

  it('keeps consultation center labels, states, and actions behind i18n keys', () => {
    expect(orderCenterSource).toContain("t('orderCenter.title')")
    expect(orderCenterSource).toContain("t('orderCenter.emptyTitle')")
    expect(orderCenterSource).toContain("t('orderCenter.loadErrorTitle')")
    expect(orderCenterSource).toContain("t('orderCenter.cancelConsultation')")
    expect(orderCenterSource).toContain("t('orderCenter.deleteConfirm'")
    expect(orderCenterSource).toContain('toIntlLocale(locale.value)')
    expect(orderCenterSource).toContain('voucherStatusLabel(voucher.status)')
    expect(orderCenterSource).toContain('transactionStatusLabel(confirmation.status)')
    expect(orderCenterSource).not.toContain('咨询记录中心')
    expect(orderCenterSource).not.toContain('搜索咨询编号、项目或凭证')
    expect(orderCenterSource).not.toContain('取消申请已提交。')
  })

  it('keeps scenic spots accessibility, feedback, and visit count formatting localized', () => {
    expect(scenicSpotsSource).toContain("import { toIntlLocale } from '../i18n/formatting'")
    expect(scenicSpotsSource).toContain('const { t, locale } = useI18n()')
    expect(scenicSpotsSource).toContain(':aria-label="t(\'spots.searchPlaceholder\')"')
    expect(scenicSpotsSource).toContain(':aria-describedby="spotsSearchDescribedBy"')
    expect(scenicSpotsSource).toContain('id="spots-result-summary"')
    expect(scenicSpotsSource).toContain('role="status"')
    expect(scenicSpotsSource).toContain('aria-live="polite"')
    expect(scenicSpotsSource).toContain('role="alert"')
    expect(scenicSpotsSource).toContain('aria-live="assertive"')
    expect(scenicSpotsSource).toContain('formatVisitCount')
    expect(scenicSpotsSource).toContain('toLocaleString(toIntlLocale(locale.value))')
    expect(scenicSpotsSource).not.toContain('toLocaleString()')
    expect(scenicSpotsSource).not.toContain("t('spots.searchPlaceholder',")
    expect(scenicSpotsSource).not.toContain("t('spots.noSearchMessage',")
    expect(scenicSpotsSource).not.toContain("t('community.nextPage',")

    for (const key of scenicSpotsContractKeys) {
      expect(scenicSpotsSource, key).toContain(`t('${key}'`)
    }

    for (const [localeName, messages] of baseLocaleMessages) {
      for (const key of scenicSpotsContractKeys) {
        expect(getMessage(messages, key), `${localeName} ${key}`).not.toBeUndefined()
      }

      for (const [key, placeholder] of scenicSpotsParameterizedKeys) {
        expect(getMessage(messages, key), `${localeName} ${key}`).toContain(placeholder)
      }
    }
  })

  it('keeps news, hotel orders, route detail, and profile residual copy localized', () => {
    expect(newsSource).toContain("t('news.metaLine'")
    expect(newsSource).toContain("t('news.resultSummaryWithKeyword'")
    expect(newsSource).not.toContain("t('news.searchPlaceholder',")

    expect(routeDetailSource).toContain("t('routeDetail.loadingRoute')")
    expect(routeDetailSource).not.toContain('const text = (key')

    expect(hotelOrdersSource).toContain("t('hotel.ordersEmptyHint')")
    expect(hotelOrdersSource).not.toContain('选择酒店和房型后提交咨询意向')

    expect(userProfileSource).toContain("t('profile.commentImageAlt')")
    expect(userProfileSource).not.toContain('alt="评论图片"')
  })

  it('keeps route planner itinerary, pause/resume, and travel kit copy localized', () => {
    const routePlannerContractKeys = [
      'routePlanner.resumeGenerationStarting',
      'routePlanner.pausedWithSavedContentAndActions',
      'routePlanner.bookableItineraryMeta',
      'routePlanner.travelKitTitle',
      'routePlanner.riskLabels.extreme',
      'routePlanner.itemTypes.scenicSpot',
      'routePlanner.communityBudget.economy'
    ]

    expect(routePlannerSource).toContain("import { toFiniteAmount, toIntlLocale } from '../i18n/formatting'")
    expect(routePlannerSource).toContain('toIntlLocale(locale.value)')
    expect(routePlannerSource).toContain("t('routePlanner.travelKitTitle')")
    expect(routePlannerSource).toContain("t('routePlanner.pausedWithSavedContentAndActions'")
    expect(routePlannerSource).not.toContain("toLocaleString('zh-CN'")
    expect(routePlannerSource).not.toContain('正在恢复 AI 路线生成...')
    expect(routePlannerSource).not.toContain('西藏深度旅行包')
    expect(routePlannerSource).not.toContain('极高风险')

    for (const key of routePlannerContractKeys) {
      expect(getMessage(extraMessages.zh, key), `zh ${key}`).not.toBeUndefined()
      expect(getMessage(extraMessages.bo, key), `bo ${key}`).not.toBeUndefined()
    }
  })
})
