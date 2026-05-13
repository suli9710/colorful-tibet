<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useReducedMotion } from 'motion-v'
import { useI18n } from 'vue-i18n'
import type { ECharts } from '@/lib/echarts'
import api, { endpoints } from '@/api'

const { locale, t } = useI18n()
const prefersReducedMotion = useReducedMotion()
const chartRef = ref<HTMLElement | null>(null)

let chart: ECharts | null = null
const zoomLevel = ref(1.0)
let mapLoaded = false
let resizeObserver: ResizeObserver | null = null
type EChartsKit = ReturnType<typeof import('@/lib/echarts').ensureECharts>
let echartsLoader: Promise<EChartsKit> | null = null

const loadECharts = async () => {
  if (!echartsLoader) {
    echartsLoader = import('@/lib/echarts').then(module => module.ensureECharts())
  }

  return echartsLoader
}

const fallbackHeatmapData = [
  { name: '布达拉宫', value: [91.1167, 29.653, 19850] },
  { name: '纳木错', value: [90.6, 30.75, 18780] },
  { name: '羊卓雍措', value: [90.65, 28.95, 18140] },
  { name: '珠穆朗玛峰', value: [86.925, 27.988, 17620] },
  { name: '雅鲁藏布大峡谷', value: [94.85, 29.6, 17100] },
  { name: '扎什伦布寺', value: [88.887, 29.267, 16640] },
  { name: '巴松措', value: [93.95, 30.0, 16260] },
  { name: '古格王国遗址', value: [79.67, 31.48, 15720] }
]

const getHeatLevel = (heat: number): string => {
  if (heat >= 19000) return t('heatmap.heatLevel.superHot')
  if (heat >= 18000) return t('heatmap.heatLevel.veryHot')
  if (heat >= 17000) return t('heatmap.heatLevel.hot')
  if (heat >= 16000) return t('heatmap.heatLevel.fairlyHot')
  if (heat >= 15500) return t('heatmap.heatLevel.normal')
  return t('heatmap.heatLevel.spot')
}

interface HeatmapPoint {
  id?: number
  name: string
  longitude: number
  latitude: number
  visitCount: number
}

const getHeatmapData = async () => {
  try {
    const response = await api.get(endpoints.spots.heatmap, { params: { limit: 100 } })
    const points = Array.isArray(response.data) ? response.data as HeatmapPoint[] : []

    const data = points.map((point) => ({
      name: point.name,
      value: [point.longitude, point.latitude, point.visitCount || 1]
    }))

    if (data.length) return data
    console.warn('Using fallback heatmap data because the heatmap response was empty.')
  } catch (error) {
    console.warn('Using fallback heatmap data after request failed:', error)
  }

  return fallbackHeatmapData
}

const getEffectData = (data: any[]) => data
  .filter((item: any) => item.value[2] >= 100)
  .sort((a: any, b: any) => b.value[2] - a.value[2])
  .slice(0, 10)

const loadTibetMapJson = async () => {
  const localResponse = await fetch('/geo/540000_full.json')
  if (localResponse.ok) return localResponse.json()

  const remoteResponse = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/540000_full.json')
  if (remoteResponse.ok) return remoteResponse.json()

  return null
}

const loadChartData = async () => {
  if (!chartRef.value || !chart) return

  const data = await getHeatmapData()

  const option: any = {
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
      formatter: function (params: any) {
        const heat = params.value[2]
        const level = getHeatLevel(heat)
        return `<strong style="font-size: 14px">${params.name}</strong><br/>${t('heatmap.accessHeat')}: ${heat}<br/>${t('heatmap.heatLevelLabel')}: ${level}`
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
        coordinateSystem: mapLoaded ? 'geo' : undefined,
        data,
        symbolSize: function (val: any) {
          const size = val[2] > 0 ? Math.max(Math.min(val[2] / 100, 25), 6) : 6
          return size
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
        coordinateSystem: mapLoaded ? 'geo' : undefined,
        data: getEffectData(data),
        symbolSize: function (val: any) {
          return Math.max(Math.min(val[2] / 80, 30), 18)
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
    option.geo = {
      roam: 'move',
      center: [90.0, 30.5],
      zoom: zoomLevel.value,
      scaleLimit: {
        min: 0.5,
        max: 5
      },
      map: undefined,
      itemStyle: {
        areaColor: 'transparent',
        borderColor: 'transparent'
      }
    }
    option.series[0].coordinateSystem = 'geo'
    option.series[1].coordinateSystem = 'geo'
  }

  chart.setOption(option)
}

onMounted(async () => {
  if (chartRef.value) {
    const { init, registerMap } = await loadECharts()
    chart = init(chartRef.value)
    
    try {
      const mapJson = await loadTibetMapJson()
      if (mapJson && typeof mapJson === 'object') {
        registerMap('tibet', mapJson)
        mapLoaded = true
      }
    } catch (e) {
      console.warn('Failed to load Tibet map data, falling back to simple scatter plot', e)
    }

    await loadChartData()
  }

  if (chartRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(handleResize)
    resizeObserver.observe(chartRef.value)
  } else {
    window.addEventListener('resize', handleResize)
  }
})

watch(zoomLevel, (newZoom) => {
  if (chart) {
    chart.setOption({
      geo: {
        zoom: newZoom
      }
    })
  }
}, { immediate: false })

watch(locale, () => {
  loadChartData()
})

watch(prefersReducedMotion, () => {
  loadChartData()
})

const handleResize = () => {
  chart?.resize()
}

const handleZoomChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  zoomLevel.value = parseFloat(target.value)
}

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  } else {
    window.removeEventListener('resize', handleResize)
  }
  chart?.dispose()
})
</script>

<template>
  <div class="relative w-full h-[600px] bg-white rounded-2xl shadow-lg border border-tibet-gold/25">
    <div ref="chartRef" class="w-full h-full"></div>
    <div class="absolute left-4 bottom-4 bg-white/90 backdrop-blur-sm rounded-lg shadow-lg border border-tibet-gold/25 p-4 min-w-[200px]">
      <div class="flex items-center justify-between mb-2">
        <span class="text-sm font-medium text-gray-700">{{ t('heatmap.zoomLevel') }}</span>
        <span class="text-sm font-bold text-blue-600">{{ zoomLevel.toFixed(1) }}x</span>
      </div>
      <input
        type="range"
        min="0.5"
        max="5"
        step="0.1"
        :value="zoomLevel"
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
