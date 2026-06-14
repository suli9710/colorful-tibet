// @vitest-environment jsdom

import { createApp, nextTick } from 'vue'
import type { App } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import HeatMap from './HeatMap.vue'

const chartMocks = vi.hoisted(() => {
  const chart = {
    off: vi.fn(),
    on: vi.fn(),
    resize: vi.fn(),
    dispose: vi.fn(),
    setOption: vi.fn()
  }

  return {
    chart,
    init: vi.fn(() => chart),
    registerMap: vi.fn()
  }
})

const apiMock = vi.hoisted(() => ({
  get: vi.fn()
}))

const routerMock = vi.hoisted(() => ({
  push: vi.fn()
}))

const i18nMock = vi.hoisted(() => ({
  locale: null as { value: string } | null
}))

vi.mock('@/lib/echartsHeatMap', () => ({
  ensureHeatMapECharts: () => ({
    init: chartMocks.init,
    registerMap: chartMocks.registerMap
  })
}))

vi.mock('@/api', () => ({
  default: apiMock,
  endpoints: {
    spots: {
      heatmap: '/api/spots/heatmap'
    }
  }
}))

vi.mock('vue-i18n', async () => {
  const vue = await vi.importActual<typeof import('vue')>('vue')
  i18nMock.locale ||= vue.ref('zh')

  return {
    useI18n: () => ({
      locale: i18nMock.locale,
      t: (key: string) => key
    })
  }
})

vi.mock('vue-router', () => ({
  useRouter: () => routerMock
}))

vi.mock('motion-v', async () => {
  const vue = await vi.importActual<typeof import('vue')>('vue')

  return {
    useReducedMotion: () => vue.ref(false)
  }
})

const LOCAL_TIBET_GEO_JSON_URL = '/geo/540000_full.json'
const DEFAULT_REMOTE_TIBET_GEO_JSON_URL = 'https://geo.datav.aliyun.com/areas_v3/bound/540000_full.json'

interface HeatmapSeriesOption {
  data: HeatmapChartPoint[]
  symbolSize: (value: unknown) => number
}

interface HeatmapChartPoint {
  id?: number | string
  name: string
  value: [number, number, number]
}

interface HeatmapChartOption {
  tooltip: {
    formatter: (params: unknown) => string
  }
  series: [HeatmapSeriesOption, HeatmapSeriesOption]
}

type ChartClickHandler = (params: unknown) => void

const mountedApps: Array<{ app: App<Element>, root: HTMLElement }> = []
let fetchMock: ReturnType<typeof vi.fn>
let consoleWarnSpy: ReturnType<typeof vi.spyOn>

const createDeferred = <T>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })

  return {
    promise,
    resolve,
    reject
  }
}

const response = (ok: boolean, body: unknown = null) => ({
  ok,
  json: vi.fn().mockResolvedValue(body)
}) as unknown as Response

const flushMountedWork = async () => {
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 0))
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 0))
}

const mountHeatMap = async () => {
  const root = document.createElement('div')
  document.body.appendChild(root)
  const app = createApp(HeatMap)
  app.mount(root)
  mountedApps.push({ app, root })
  await flushMountedWork()
}

const getLatestChartOption = (): HeatmapChartOption => {
  const call = chartMocks.chart.setOption.mock.calls.at(-1)

  expect(call).toBeDefined()

  return call?.[0] as HeatmapChartOption
}

const getChartClickHandler = (): ChartClickHandler => {
  const call = chartMocks.chart.on.mock.calls.find(([eventName]) => eventName === 'click')

  expect(call).toBeDefined()

  return call?.[1] as ChartClickHandler
}

beforeEach(() => {
  if (i18nMock.locale) i18nMock.locale.value = 'zh'
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
  consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
  apiMock.get.mockResolvedValue({
    data: [
      {
        id: 1,
        name: 'Potala',
        longitude: 91.1167,
        latitude: 29.653,
        visitCount: 19850
      }
    ]
  })
})

afterEach(() => {
  mountedApps.splice(0).forEach(({ app, root }) => {
    app.unmount()
    root.remove()
  })
  consoleWarnSpy.mockRestore()
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
  vi.clearAllMocks()
})

describe('HeatMap geo JSON fallback policy', () => {
  it('does not request third-party geo JSON when the local map fails by default', async () => {
    fetchMock.mockResolvedValueOnce(response(false))

    await mountHeatMap()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith(LOCAL_TIBET_GEO_JSON_URL)
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('geo.datav.aliyun.com'))).toBe(false)
    expect(chartMocks.registerMap).not.toHaveBeenCalled()
  })

  it('uses the remote geo JSON fallback only when explicitly enabled', async () => {
    const mapJson = { type: 'FeatureCollection', features: [] }
    vi.stubEnv('VITE_HEATMAP_REMOTE_GEO_FALLBACK_ENABLED', 'true')
    fetchMock
      .mockResolvedValueOnce(response(false))
      .mockResolvedValueOnce(response(true, mapJson))

    await mountHeatMap()

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock).toHaveBeenNthCalledWith(1, LOCAL_TIBET_GEO_JSON_URL)
    expect(fetchMock).toHaveBeenNthCalledWith(2, DEFAULT_REMOTE_TIBET_GEO_JSON_URL)
    expect(chartMocks.registerMap).toHaveBeenCalledWith('tibet', mapJson)
  })

  it('rejects untrusted remote geo JSON fallback URLs even when fallback is enabled', async () => {
    vi.stubEnv('VITE_HEATMAP_REMOTE_GEO_FALLBACK_ENABLED', 'true')
    vi.stubEnv('VITE_HEATMAP_REMOTE_GEO_FALLBACK_URL', 'https://evil.example/540000_full.json')
    fetchMock.mockResolvedValueOnce(response(false))

    await mountHeatMap()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith(LOCAL_TIBET_GEO_JSON_URL)
    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('evil.example'))).toBe(false)
    expect(chartMocks.registerMap).not.toHaveBeenCalled()
  })
})

describe('HeatMap chart callback hardening', () => {
  it('normalizes API heatmap points and drops malformed rows', async () => {
    fetchMock.mockResolvedValueOnce(response(false))
    apiMock.get.mockResolvedValueOnce({
      data: [
        {
          id: 'spot-1',
          name: '  Safe Spot  ',
          longitude: '91.1167',
          latitude: '29.653',
          visitCount: '42'
        },
        {
          id: 2,
          name: { unsafe: true },
          longitude: 91,
          latitude: 29,
          visitCount: 99
        },
        {
          id: 3,
          name: 'Missing longitude',
          longitude: 'bad',
          latitude: 29,
          visitCount: 99
        }
      ]
    })

    await mountHeatMap()

    const option = getLatestChartOption()
    expect(option.series[0].data).toEqual([
      {
        id: 'spot-1',
        name: 'Safe Spot',
        value: [91.1167, 29.653, 42]
      }
    ])
  })

  it('escapes tooltip content and tolerates malformed chart values', async () => {
    fetchMock.mockResolvedValueOnce(response(false))

    await mountHeatMap()

    const option = getLatestChartOption()
    const trustedHtml = option.tooltip.formatter({
      value: [91.1167, 29.653, 19850],
      data: {
        id: 'potala',
        name: '<img src=x onerror=alert(1)>'
      }
    })
    const malformedHtml = option.tooltip.formatter({
      value: ['bad'],
      name: 'Bad & "Name"',
      data: {
        id: ''
      }
    })

    expect(trustedHtml).toContain('&lt;img src=x onerror=alert(1)&gt;')
    expect(trustedHtml).not.toContain('<img src=x')
    expect(trustedHtml).toContain('heatmap.clickToView')
    expect(malformedHtml).toContain('Bad &amp; &quot;Name&quot;')
    expect(malformedHtml).toContain('heatmap.accessHeat: 0')
    expect(malformedHtml).not.toContain('heatmap.clickToView')
  })

  it('keeps symbol sizing stable when ECharts passes unexpected values', async () => {
    fetchMock.mockResolvedValueOnce(response(false))

    await mountHeatMap()

    const [scatter, effectScatter] = getLatestChartOption().series

    expect(scatter.symbolSize([91.1167, 29.653, 19850])).toBe(25)
    expect(scatter.symbolSize(['bad'])).toBe(6)
    expect(scatter.symbolSize([91.1167, 29.653, -1])).toBe(6)
    expect(effectScatter.symbolSize([91.1167, 29.653, 19850])).toBe(30)
    expect(effectScatter.symbolSize(['bad'])).toBe(18)
  })

  it('navigates only for series clicks with a valid spot id', async () => {
    fetchMock.mockResolvedValueOnce(response(false))

    await mountHeatMap()

    const clickHandler = getChartClickHandler()
    routerMock.push.mockClear()

    clickHandler({ componentType: 'geo', data: { id: 'ignored' } })
    clickHandler({ componentType: 'series', data: { id: '' } })
    clickHandler({ componentType: 'series', data: { id: 'spot 1' } })

    expect(routerMock.push).toHaveBeenCalledTimes(1)
    expect(routerMock.push).toHaveBeenCalledWith('/spots/spot%201')
  })

  it('ignores stale heatmap data responses after a newer chart reload completes', async () => {
    const staleResponse = createDeferred<{ data: unknown[] }>()
    fetchMock.mockResolvedValueOnce(response(false))
    apiMock.get
      .mockReturnValueOnce(staleResponse.promise)
      .mockResolvedValueOnce({
        data: [
          {
            id: 2,
            name: 'Fresh',
            longitude: 92,
            latitude: 30,
            visitCount: 200
          }
        ]
      })

    await mountHeatMap()
    expect(apiMock.get).toHaveBeenCalledTimes(1)

    i18nMock.locale!.value = 'bo'
    await flushMountedWork()

    expect(apiMock.get).toHaveBeenCalledTimes(2)
    expect(chartMocks.chart.setOption).toHaveBeenCalledTimes(1)
    expect(getLatestChartOption().series[0].data[0]?.name).toBe('Fresh')

    staleResponse.resolve({
      data: [
        {
          id: 1,
          name: 'Stale',
          longitude: 91,
          latitude: 29,
          visitCount: 1
        }
      ]
    })
    await flushMountedWork()

    expect(chartMocks.chart.setOption).toHaveBeenCalledTimes(1)
    expect(getLatestChartOption().series[0].data[0]?.name).toBe('Fresh')
  })

  it('labels the canvas chart and zoom range for screen readers', async () => {
    fetchMock.mockResolvedValueOnce(response(false))

    await mountHeatMap()

    const chartRegion = document.querySelector('[role="img"]')
    const slider = document.querySelector<HTMLInputElement>('#heatmap-zoom-slider')
    const sliderLabel = document.querySelector('label[for="heatmap-zoom-slider"]')

    expect(chartRegion?.getAttribute('aria-label')).toBe('heatmap.title')
    expect(slider).toBeTruthy()
    expect(slider?.getAttribute('aria-valuetext')).toBe('1.0x')
    expect(sliderLabel?.textContent).toBe('heatmap.zoomLevel')
  })

  it('does not reintroduce geo options when zooming after map fallback', async () => {
    fetchMock.mockResolvedValueOnce(response(false))

    await mountHeatMap()

    const initialOption = getLatestChartOption()
    expect('geo' in initialOption).toBe(false)
    chartMocks.chart.setOption.mockClear()

    const slider = document.querySelector<HTMLInputElement>('#heatmap-zoom-slider')
    expect(slider).toBeTruthy()
    slider!.value = '2.5'
    slider!.dispatchEvent(new Event('input', { bubbles: true }))
    await flushMountedWork()

    expect(chartMocks.chart.setOption).not.toHaveBeenCalled()
  })
})
