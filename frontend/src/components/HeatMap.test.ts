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

  return {
    useI18n: () => ({
      locale: vue.ref('zh'),
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

const mountedApps: Array<{ app: App<Element>, root: HTMLElement }> = []
let fetchMock: ReturnType<typeof vi.fn>
let consoleWarnSpy: ReturnType<typeof vi.spyOn>

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

beforeEach(() => {
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
})
