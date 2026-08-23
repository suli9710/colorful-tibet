import { watch, type WatchSource } from 'vue'
import type { Router, RouterScrollBehavior } from 'vue-router'

const BRAND_TITLE_KEY = 'common.brandName'
const FALLBACK_BRAND_TITLE = 'Colorful Tibet'

export const ROUTE_TITLE_KEYS = {
  home: 'common.home',
  login: 'common.login',
  register: 'common.register',
  scenicSpots: 'spots.title',
  scenicSpotDetail: 'routeTitles.scenicSpotDetail',
  heritage: 'heritage.title',
  news: 'news.title',
  admin: 'admin.title',
  routePlanner: 'routePlanner.title',
  community: 'community.title',
  questionDetail: 'community.questionDetail',
  routeDetail: 'routeTitles.routeDetail',
  createRoute: 'createRoute.title',
  profile: 'profile.title',
  privacy: 'privacy.title',
  terms: 'terms.title',
  hotels: 'hotel.listTitle',
  hotelDetail: 'routeTitles.hotelDetail',
  hotelBooking: 'hotel.bookingTitle',
  favorites: 'favorites.title',
  orders: 'orderCenter.title',
  hotelOrders: 'hotel.ordersTitle',
  notFound: 'routeTitles.notFound'
} as const

export type RouteTitleKey = (typeof ROUTE_TITLE_KEYS)[keyof typeof ROUTE_TITLE_KEYS]
export type RouteRole = 'ADMIN'

declare module 'vue-router' {
  interface RouteMeta {
    titleKey?: RouteTitleKey
    requiresAuth?: boolean
    roles?: readonly RouteRole[]
  }
}

type Translate = (key: string) => string

interface DocumentTitleContext {
  locale: WatchSource<unknown>
  translate: Translate
}

const translatedValue = (key: string, translate: Translate) => {
  const value = translate(key).trim()
  return value && value !== key ? value : ''
}

export const buildDocumentTitle = (
  titleKey: RouteTitleKey | undefined,
  translate: Translate
) => {
  const brand = translatedValue(BRAND_TITLE_KEY, translate) || FALLBACK_BRAND_TITLE
  const page = titleKey ? translatedValue(titleKey, translate) : ''

  return page && page !== brand ? `${page} | ${brand}` : brand
}

export const installDocumentTitleSync = (
  router: Router,
  context: DocumentTitleContext
) => {
  const syncTitle = (titleKey: RouteTitleKey | undefined) => {
    if (typeof document === 'undefined') return
    document.title = buildDocumentTitle(titleKey, context.translate)
  }

  const removeAfterEach = router.afterEach(to => syncTitle(to.meta.titleKey))
  const stopLocaleWatch = watch(
    context.locale,
    () => syncTitle(router.currentRoute.value.meta.titleKey)
  )

  syncTitle(router.currentRoute.value.meta.titleKey)

  return () => {
    removeAfterEach()
    stopLocaleWatch()
  }
}

export const routeScrollBehavior: RouterScrollBehavior = (to, _from, savedPosition) => {
  if (savedPosition) return savedPosition
  if (to.hash) return { el: to.hash }
  return { left: 0, top: 0 }
}
