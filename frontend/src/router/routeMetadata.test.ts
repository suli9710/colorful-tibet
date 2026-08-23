// @vitest-environment jsdom

import { nextTick, ref } from 'vue'
import {
  createMemoryHistory,
  createRouter,
  type RouteLocationNormalized,
  type RouteMeta
} from 'vue-router'
import { describe, expect, expectTypeOf, it } from 'vitest'
import i18n from '../i18n'
import { loadLocaleMessages } from '../i18n'
import appRouter from './index'
import {
  ROUTE_TITLE_KEYS,
  buildDocumentTitle,
  installDocumentTitleSync,
  routeScrollBehavior,
  type RouteTitleKey
} from './routeMetadata'

const emptyRoute = { hash: '' } as RouteLocationNormalized

describe('route metadata', () => {
  it('types the supported document title key on RouteMeta', () => {
    expectTypeOf<RouteMeta['titleKey']>().toEqualTypeOf<RouteTitleKey | undefined>()
  })

  it('provides a localized title key for every rendered application route', () => {
    const routesWithoutTitle = appRouter
      .getRoutes()
      .filter(route => !route.redirect && !route.meta.titleKey)
      .map(route => route.path)

    expect(routesWithoutTitle).toEqual([])
  })

  it('declares admin authorization through route metadata instead of path conventions', () => {
    const adminRoute = appRouter.getRoutes().find(route => route.path === '/admin')

    expect(adminRoute?.meta).toMatchObject({
      requiresAuth: true,
      roles: ['ADMIN']
    })
  })

  it('resolves every configured title key in both supported locales', async () => {
    await loadLocaleMessages('bo')
    for (const locale of ['zh', 'bo'] as const) {
      for (const titleKey of Object.values(ROUTE_TITLE_KEYS)) {
        expect(i18n.global.te(titleKey, locale), `${locale}:${titleKey}`).toBe(true)
      }
    }
  })
})

describe('document title synchronization', () => {
  it('combines the localized page name with the localized brand', () => {
    const translations: Record<string, string> = {
      'common.brandName': '七彩西藏',
      'common.home': '首页'
    }

    expect(buildDocumentTitle(ROUTE_TITLE_KEYS.home, key => translations[key] ?? key))
      .toBe('首页 | 七彩西藏')
  })

  it('updates after navigation and immediately after the locale changes', async () => {
    const locale = ref('zh')
    const messages: Record<string, Record<string, string>> = {
      zh: {
        'common.brandName': '七彩西藏',
        'common.home': '首页',
        'spots.title': '景点导览'
      },
      bo: {
        'common.brandName': 'བོད་ཀྱི་ཚོན་མདངས།',
        'common.home': 'ཁྱིམ་ཤོག',
        'spots.title': 'གཟིགས་སྐོར་ས་གནས།'
      }
    }
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: {}, meta: { titleKey: ROUTE_TITLE_KEYS.home } },
        { path: '/spots', component: {}, meta: { titleKey: ROUTE_TITLE_KEYS.scenicSpots } }
      ]
    })
    const stopSync = installDocumentTitleSync(router, {
      locale,
      translate: key => messages[locale.value]?.[key] ?? key
    })

    try {
      await router.push('/')
      expect(document.title).toBe('首页 | 七彩西藏')

      await router.push('/spots')
      expect(document.title).toBe('景点导览 | 七彩西藏')

      locale.value = 'bo'
      await nextTick()
      expect(document.title).toBe('གཟིགས་སྐོར་ས་གནས། | བོད་ཀྱི་ཚོན་མདངས།')
    } finally {
      stopSync()
    }
  })
})

describe('route scroll behavior', () => {
  it('restores browser history positions', () => {
    const savedPosition = { left: 18, top: 240 }

    expect(routeScrollBehavior(emptyRoute, emptyRoute, savedPosition)).toEqual(savedPosition)
  })

  it('scrolls to a hash target or the top for a fresh navigation', () => {
    expect(routeScrollBehavior(
      { hash: '#details' } as RouteLocationNormalized,
      emptyRoute,
      null
    )).toEqual({ el: '#details' })
    expect(routeScrollBehavior(emptyRoute, emptyRoute, null)).toEqual({ left: 0, top: 0 })
  })
})
