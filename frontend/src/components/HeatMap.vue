<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useReducedMotion } from 'motion-v'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import type { ECharts, EChartsOption } from '@/lib/echartsHeatMap'
import api, { endpoints } from '@/api'
import { summarizeClientError } from '@/utils/errorMonitoring'

const { locale, t } = useI18n()
const router = useRouter()
const prefersReducedMotion = useReducedMotion()
const chartRef = ref<HTMLElement | null>(null)
const chartError = ref(false)

let chart: ECharts | null = null
const zoomLevel = ref(1.0)
let mapLoaded = false
let resizeObserver: ResizeObserver | null = null
let chartDataRequestId = 0
let componentDisposed = false
type EChartsKit = ReturnType<typeof import('@/lib/echartsHeatMap').ensureHeatMapECharts>
let echartsLoader: Promise<EChartsKit> | null = null
const LOCAL_TIBET_GEO_JSON_URL = '/geo/540000_full.json'
const DEFAULT_REMOTE_TIBET_GEO_JSON_URL = 'https://geo.datav.aliyun.com/areas_v3/bound/540000_full.json'
const TRUSTED_REMOTE_GEO_FALLBACK_ORIGINS = new Set(['https://geo.datav.aliyun.com'])

const loadECharts = async () => {
  if (!echartsLoader) {
    echartsLoader = import('@/lib/echartsHeatMap').then(module => module.ensureHeatMapECharts())
  }

  return echartsLoader
}

interface HeatmapChartPoint {
  id?: number | string
  name: string
  value: [number, number, number]
}

type HeatmapValue = HeatmapChartPoint['value']

interface HeatmapChartPayload {
  id?: unknown
  name?: unknown
}

interface HeatmapClickParams {
  componentType?: unknown
  data?: unknown
}

interface HeatmapTooltipParams {
  value?: unknown
  name?: unknown
  data?: unknown
}

interface HeatmapSeriesOption {
  name: string
  type: 'scatter' | 'effectScatter'
  coordinateSystem?: 'geo' | 'cartesian2d'
  data: HeatmapChartPoint[]
  cursor: 'pointer'
  symbolSize: (value: unknown) => number
  label: Record<string, unknown>
  itemStyle: Record<string, unknown>
  emphasis?: Record<string, unknown>
  showEffectOn?: 'emphasis' | 'render'
  rippleEffect?: Record<string, unknown>
  zlevel?: number
}

interface HeatmapChartOption extends EChartsOption {
  animation: boolean
  animationDuration: number
  title: Record<string, unknown>
  tooltip: {
    trigger: 'item'
    formatter: (params: unknown) => string
    backgroundColor: string
    borderColor: string
    borderWidth: number
    textStyle: Record<string, unknown>
    padding: [number, number]
  }
  series: [HeatmapSeriesOption, HeatmapSeriesOption]
  geo?: EChartsOption['geo']
  grid?: EChartsOption['grid']
  xAxis?: EChartsOption['xAxis']
  yAxis?: EChartsOption['yAxis']
}

const HTML_ESCAPE_MAP: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;'
}

const escapeTooltipHtml = (value: unknown): string =>
  String(value ?? '').replace(/[&<>"']/g, char => HTML_ESCAPE_MAP[char] || char)

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const readText = (value: unknown): string | undefined => {
  if (typeof value === 'string') {
    const trimmedValue = value.trim()
    return trimmedValue || undefined
  }

  if (typeof value === 'number' && Number.isFinite(value)) return String(value)

  return undefined
}

const readHeatmapValue = (value: unknown): HeatmapValue | null => {
  if (!Array.isArray(value) || value.length < 3) return null

  const longitude = Number(value[0])
  const latitude = Number(value[1])
  const heat = Number(value[2])

  if (!Number.isFinite(longitude) || !Number.isFinite(latitude) || !Number.isFinite(heat)) {
    return null
  }

  return [longitude, latitude, heat]
}

const readHeatmapPayload = (payload: unknown): HeatmapChartPayload =>
  isRecord(payload) ? { id: payload.id, name: payload.name } : {}

const readTooltipParams = (params: unknown): HeatmapTooltipParams =>
  isRecord(params) ? params : {}

const readSpotName = (params: HeatmapTooltipParams): unknown => {
  const payload = readHeatmapPayload(params.data)

  return params.name || payload.name || ''
}

const hasNavigableSpotId = (spotId: unknown): spotId is number | string =>
  (typeof spotId === 'number' && Number.isFinite(spotId)) ||
  (typeof spotId === 'string' && spotId.trim().length > 0)

const getHeatmapSymbolSize = (value: unknown, divisor: number, min: number, max: number): number => {
  const heat = readHeatmapValue(value)?.[2] ?? 0

  if (heat <= 0) return min

  return Math.max(Math.min(heat / divisor, max), min)
}

const fallbackHeatmapData: HeatmapChartPoint[] = [
  { id: 1, name: '布达拉宫', value: [91.1167, 29.653, 19850] },
  { id: 2, name: '纳木错', value: [90.6, 30.75, 18780] },
  { id: 3, name: '羊卓雍措', value: [90.65, 28.95, 18140] },
  { id: 4, name: '珠穆朗玛峰', value: [86.925, 27.988, 17620] },
  { id: 5, name: '雅鲁藏布大峡谷', value: [94.85, 29.6, 17100] },
  { id: 6, name: '扎什伦布寺', value: [88.887, 29.267, 16640] },
  { id: 7, name: '巴松措', value: [93.95, 30.0, 16260] },
  { id: 8, name: '古格王国遗址', value: [79.67, 31.48, 15720] }
]

const getHeatLevel = (heat: number): string => {
  if (heat >= 19000) return t('heatmap.heatLevel.superHot')
  if (heat >= 18000) return t('heatmap.heatLevel.veryHot')
  if (heat >= 17000) return t('heatmap.heatLevel.hot')
  if (heat >= 16000) return t('heatmap.heatLevel.fairlyHot')
  if (heat >= 15500) return t('heatmap.heatLevel.normal')
  return t('heatmap.heatLevel.spot')
}

const normalizeHeatmapPoint = (point: unknown): HeatmapChartPoint | null => {
  if (!isRecord(point)) return null

  const longitude = Number(point.longitude)
  const latitude = Number(point.latitude)
  const visitCount = Number(point.visitCount)
  const name = readText(point.name)
  if (!name || !Number.isFinite(longitude) || !Number.isFinite(latitude)) return null

  return {
    ...(hasNavigableSpotId(point.id) ? { id: point.id } : {}),
    name,
    value: [
      longitude,
      latitude,
      Number.isFinite(visitCount) && visitCount > 0 ? visitCount : 1
    ]
  }
}

const getHeatmapData = async (): Promise<HeatmapChartPoint[]> => {
  try {
    const response = await api.get<unknown>(endpoints.spots.heatmap, { params: { limit: 100 } })
    const points = Array.isArray(response.data) ? response.data : []
    const data = points
      .map(normalizeHeatmapPoint)
      .filter((point): point is HeatmapChartPoint => point !== null)

    if (data.length) return data
    console.warn('Using fallback heatmap data because the heatmap response was empty.')
  } catch (error) {
    console.warn('Using fallback heatmap data after request failed:', summarizeClientError(error))
  }

  return fallbackHeatmapData
}

const getEffectData = (data: HeatmapChartPoint[]) => data
  .filter((item) => item.value[2] >= 100)
  .sort((a, b) => b.value[2] - a.value[2])
  .slice(0, 10)

const goToSpot = (spotId?: number | string) => {
  if (spotId === undefined || spotId === null || spotId === '') return

  router.push(`/spots/${encodeURIComponent(String(spotId))}`)
}

const handleChartClick = (params: HeatmapClickParams) => {
  if (!isRecord(params) || params.componentType !== 'series') return

  const payload = readHeatmapPayload(params.data)
  if (!hasNavigableSpotId(payload.id)) return

  goToSpot(payload.id)
}

const isTruthyEnvValue = (value: string | undefined): boolean =>
  ['1', 'true', 'yes', 'on'].includes(String(value || '').trim().toLowerCase())

const isRemoteGeoFallbackEnabled = (): boolean =>
  isTruthyEnvValue(import.meta.env.VITE_HEATMAP_REMOTE_GEO_FALLBACK_ENABLED)

const getRemoteGeoFallbackUrl = (): string =>
  String(import.meta.env.VITE_HEATMAP_REMOTE_GEO_FALLBACK_URL || DEFAULT_REMOTE_TIBET_GEO_JSON_URL).trim()

const getTrustedRemoteGeoFallbackUrl = (): string => {
  const remoteGeoFallbackUrl = getRemoteGeoFallbackUrl()
  if (!remoteGeoFallbackUrl) return ''

  try {
    const url = new URL(remoteGeoFallbackUrl)
    if (url.protocol !== 'https:' || !TRUSTED_REMOTE_GEO_FALLBACK_ORIGINS.has(url.origin)) {
      console.warn('Remote Tibet map fallback URL is not trusted; skipping remote geo JSON fallback.')
      return ''
    }

    return url.toString()
  } catch {
    console.warn('Remote Tibet map fallback URL is invalid; skipping remote geo JSON fallback.')
    return ''
  }
}

const loadTibetMapJson = async () => {
  const localResponse = await fetch(LOCAL_TIBET_GEO_JSON_URL)
  if (localResponse.ok) return localResponse.json()

  if (!isRemoteGeoFallbackEnabled()) {
    console.warn('Local Tibet map data unavailable; remote geo JSON fallback is disabled.')
    return null
  }

  const remoteGeoFallbackUrl = getTrustedRemoteGeoFallbackUrl()
  if (!remoteGeoFallbackUrl) return null

  const remoteResponse = await fetch(remoteGeoFallbackUrl)
  if (remoteResponse.ok) return remoteResponse.json()

  return null
}

const loadChartData = async () => {
  const requestId = ++chartDataRequestId
  if (componentDisposed || !chartRef.value || !chart) return

  const data = await getHeatmapData()
  if (requestId !== chartDataRequestId || componentDisposed || !chartRef.value || !chart) return

  const option: HeatmapChartOption = {
    animation: !prefersReducedMotion.value,
    animationDuration: 300,
    title: {
      text: t('heatmap.title'),
      left: 'center',
      top: 20,
      textStyle: {
        color: '#1f2937',
        fontSize: 20,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: function (rawParams: unknown) {
        const params = readTooltipParams(rawParams)
        const heat = readHeatmapValue(params.value)?.[2] ?? 0
        const level = getHeatLevel(heat)
        const payload = readHeatmapPayload(params.data)
        const spotName = escapeTooltipHtml(readSpotName(params))
        const heatText = escapeTooltipHtml(heat)
        const levelText = escapeTooltipHtml(level)
        const accessHeatLabel = escapeTooltipHtml(t('heatmap.accessHeat'))
        const heatLevelLabel = escapeTooltipHtml(t('heatmap.heatLevelLabel'))
        const clickHint = hasNavigableSpotId(payload.id)
          ? `<br/><span style="color: #fde68a">${escapeTooltipHtml(t('heatmap.clickToView'))}</span>`
          : ''
        return `<strong style="font-size: 14px">${spotName}</strong><br/>${accessHeatLabel}: ${heatText}<br/>${heatLevelLabel}: ${levelText}${clickHint}`
      },
      backgroundColor: 'rgba(0, 0, 0, 0.85)',
      borderColor: '#fbbf24',
      borderWidth: 1,
      textStyle: {
        color: '#fff',
        fontSize: 13
      },
      padding: [10, 15]
    },
    series: [
      {
        name: t('heatmap.heatLevel.spot'),
        type: 'scatter',
        coordinateSystem: mapLoaded ? 'geo' : 'cartesian2d',
        data,
        cursor: 'pointer',
        symbolSize: function (value: unknown) {
          return getHeatmapSymbolSize(value, 100, 6, 25)
        },
        label: {
          formatter: '{b}',
          position: 'right',
          show: false,
          color: '#1f2937',
          fontSize: 11
        },
        itemStyle: {
          color: '#f59e0b',
          shadowBlur: 8,
          shadowColor: 'rgba(245, 158, 11, 0.5)'
        },
        emphasis: {
          label: {
            show: true
          },
          itemStyle: {
            color: '#dc2626',
            shadowBlur: 15
          }
        }
      },
      {
        name: t('home.hotSpotsDistribution'),
        type: 'effectScatter',
        coordinateSystem: mapLoaded ? 'geo' : 'cartesian2d',
        data: getEffectData(data),
        cursor: 'pointer',
        symbolSize: function (value: unknown) {
          return getHeatmapSymbolSize(value, 80, 18, 30)
        },
        showEffectOn: prefersReducedMotion.value ? 'emphasis' : 'render',
        rippleEffect: {
          brushType: 'stroke',
          scale: prefersReducedMotion.value ? 1.2 : 3,
          period: prefersReducedMotion.value ? 8 : 4
        },
        label: {
          formatter: '{b}',
          position: 'right',
          show: true,
          color: '#1e40af',
          fontSize: 12,
          fontWeight: 'bold',
          backgroundColor: 'rgba(255, 255, 255, 0.8)',
          padding: [4, 8],
          borderRadius: 4
        },
        itemStyle: {
          color: '#ef4444',
          shadowBlur: 15,
          shadowColor: 'rgba(239, 68, 68, 0.6)'
        },
        zlevel: 1
      }
    ]
  }

  if (mapLoaded) {
    option.geo = {
      map: 'tibet',
      roam: 'move',
      center: [90.0, 30.5],
      zoom: zoomLevel.value,
      scaleLimit: {
        min: 0.5,
        max: 5
      },
      label: {
        show: true,
        color: '#4b5563',
        fontSize: 11
      },
      itemStyle: {
        areaColor: '#e0f2fe',
        borderColor: '#0ea5e9',
        borderWidth: 1.5
      },
      emphasis: {
        label: {
          color: '#1e40af'
        },
        itemStyle: {
          areaColor: '#bfdbfe'
        }
      }
    }
  } else {
    option.grid = {
      left: 40,
      right: 40,
      top: 80,
      bottom: 40
    }
    option.xAxis = {
      type: 'value',
      min: 78,
      max: 99,
      show: false
    }
    option.yAxis = {
      type: 'value',
      min: 26,
      max: 37,
      show: false
    }
  }

  chart.setOption(option)
}

onMounted(async () => {
  componentDisposed = false
  try {
    chartError.value = false

    if (chartRef.value) {
      const { init, registerMap } = await loadECharts()
      if (componentDisposed || !chartRef.value) return

      chart = init(chartRef.value)
      chart.off('click', handleChartClick)
      chart.on('click', handleChartClick)

      try {
        const mapJson = await loadTibetMapJson()
        if (componentDisposed) return
        if (mapJson && typeof mapJson === 'object') {
          registerMap('tibet', mapJson)
          mapLoaded = true
        }
      } catch (e) {
        console.warn('Failed to load Tibet map data, falling back to simple scatter plot', summarizeClientError(e))
      }

      await loadChartData()
    }
  } catch (error) {
    if (componentDisposed) return
    chartError.value = true
    console.warn('Failed to initialize heatmap chart:', summarizeClientError(error))
  }

  if (componentDisposed) return

  if (chartRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(handleResize)
    resizeObserver.observe(chartRef.value)
  } else {
    window.addEventListener('resize', handleResize)
  }
})

watch(zoomLevel, (newZoom) => {
  if (chart && mapLoaded) {
    chart.setOption({
      geo: {
        zoom: newZoom
      }
    })
  }
}, { immediate: false })

watch(locale, () => {
  void loadChartData()
})

watch(prefersReducedMotion, () => {
  void loadChartData()
})

const handleResize = () => {
  chart?.resize()
}

const handleZoomChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  zoomLevel.value = parseFloat(target.value)
}

onUnmounted(() => {
  componentDisposed = true
  chartDataRequestId += 1
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  } else {
    window.removeEventListener('resize', handleResize)
  }
  const disposedChart = chart
  chart = null
  mapLoaded = false
  disposedChart?.off('click', handleChartClick)
  disposedChart?.dispose()
})
</script>

<template>
  <div class="relative h-[360px] w-full rounded-2xl border border-tibet-gold/25 bg-white shadow-lg sm:h-[520px] lg:h-[600px]">
    <div ref="chartRef" class="w-full h-full" role="img" :aria-label="t('heatmap.title')"></div>
    <div
      v-if="chartError"
      class="absolute inset-4 flex items-center justify-center rounded-xl border border-tibet-gold/20 bg-tibet-cream/80 text-center text-tibet-brown"
      role="alert"
      aria-live="assertive"
    >
      <div>
        <p class="text-lg font-semibold">{{ t('spotDetail.mapLoadFailed') }}</p>
      </div>
    </div>
    <div class="absolute inset-x-3 bottom-3 rounded-lg border border-tibet-gold/25 bg-white/90 p-3 shadow-lg backdrop-blur-sm sm:inset-x-auto sm:left-4 sm:bottom-4 sm:min-w-[200px] sm:p-4">
      <div class="flex items-center justify-between mb-2">
        <label for="heatmap-zoom-slider" class="text-sm font-medium text-gray-700">{{ t('heatmap.zoomLevel') }}</label>
        <span class="text-sm font-bold text-blue-600">{{ zoomLevel.toFixed(1) }}x</span>
      </div>
      <input
        id="heatmap-zoom-slider"
        type="range"
        min="0.5"
        max="5"
        step="0.1"
        :value="zoomLevel"
        :aria-valuetext="`${zoomLevel.toFixed(1)}x`"
        @input="handleZoomChange"
        class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer slider"
      />
      <div class="flex justify-between text-xs text-gray-500 mt-1">
        <span>0.5x</span>
        <span>5.0x</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 缩放滑块样式 */
.slider {
  outline: none;
}
.slider:focus::-webkit-slider-thumb {
  box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.25), 0 2px 8px rgba(14, 165, 233, 0.4);
}
.slider::-webkit-slider-thumb {
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #0ea5e9;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: all 0.2s;
}

.slider::-webkit-slider-thumb:hover {
  background: #0284c7;
  transform: scale(1.1);
  box-shadow: 0 0 0 4px rgba(14, 165, 233, 0.2), 0 2px 8px rgba(14, 165, 233, 0.4);
}

.slider::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #0ea5e9;
  cursor: pointer;
  border: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: all 0.2s;
}

.slider::-moz-range-thumb:hover {
  background: #0284c7;
  transform: scale(1.1);
}

.slider::-webkit-slider-runnable-track {
  height: 8px;
  background: linear-gradient(to right, #e0f2fe, #0ea5e9);
  border-radius: 4px;
}

.slider::-moz-range-track {
  height: 8px;
  background: linear-gradient(to right, #e0f2fe, #0ea5e9);
  border-radius: 4px;
}
</style>
