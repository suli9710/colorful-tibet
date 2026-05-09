<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import api from '@/api'

const { locale, t } = useI18n()
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
const zoomLevel = ref(1.0)
let mapLoaded = false
let spotsData: any[] = []
let fluctuationTimer: ReturnType<typeof setInterval> | null = null

const getHeatLevel = (heat: number): string => {
  if (heat >= 19000) return t('heatmap.heatLevel.superHot')
  if (heat >= 18000) return t('heatmap.heatLevel.veryHot')
  if (heat >= 17000) return t('heatmap.heatLevel.hot')
  if (heat >= 16000) return t('heatmap.heatLevel.fairlyHot')
  if (heat >= 15500) return t('heatmap.heatLevel.normal')
  return t('heatmap.heatLevel.spot')
}

const applyFluctuation = (spots: any[]): any[] => {
  const t0 = Date.now() / 4000
  return spots.map((spot, i) => {
    const factor = 1 + Math.sin(t0 + i * 0.7) * 0.06
    return {
      ...spot,
      value: [spot.value[0], spot.value[1], Math.round(spot.value[2] * factor)]
    }
  })
}

const updateChartData = () => {
  if (!chart || !spotsData.length) return

  const fluctuated = applyFluctuation(spotsData)

  const scatterData = fluctuated
  const effectData = fluctuated
    .filter((item: any) => item.value[2] >= 100)
    .sort((a: any, b: any) => b.value[2] - a.value[2])
    .slice(0, 10)

  chart.setOption({
    series: [
      { data: scatterData },
      { data: effectData }
    ]
  })
}

const loadChartData = async () => {
  if (!chartRef.value || !chart) return
  
  try {
    const response = await api.get('/spots', { params: { size: 100 } })
    const spots = Array.isArray(response.data) ? response.data : (response.data.content || [])
    
    const data = spots
      .filter((spot: any) => spot.longitude != null && spot.latitude != null)
      .map((spot: any) => ({
        name: spot.name,
        value: [spot.longitude, spot.latitude, spot.visitCount || 1]
      }))

    spotsData = data

    if (fluctuationTimer) clearInterval(fluctuationTimer)
    fluctuationTimer = setInterval(updateChartData, 2500)

    const option: any = {
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
          name: '景点',
          type: 'scatter',
          coordinateSystem: mapLoaded ? 'geo' : undefined,
          data: data,
          symbolSize: function (val: any) {
            const size = val[2] > 0 ? Math.max(Math.min(val[2] / 100, 25), 6) : 6;
            return size;
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
          name: '热门景点',
          type: 'effectScatter',
          coordinateSystem: mapLoaded ? 'geo' : undefined,
          data: data
            .filter((item: any) => item.value[2] >= 100)
            .sort((a: any, b: any) => b.value[2] - a.value[2])
            .slice(0, 10),
          symbolSize: function (val: any) {
            return Math.max(Math.min(val[2] / 80, 30), 18);
          },
          showEffectOn: 'render',
          rippleEffect: {
            brushType: 'stroke',
            scale: 3,
            period: 4
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
  } catch (error) {
    console.error('Failed to load heatmap data', error)
  }
}

onMounted(async () => {
  if (chartRef.value) {
    chart = echarts.init(chartRef.value)
    
    try {
      const mapResponse = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/540000_full.json')
      if (mapResponse.ok) {
        const contentType = mapResponse.headers.get('content-type')
        if (contentType && contentType.includes('application/json')) {
          const mapJson = await mapResponse.json()
          if (mapJson && typeof mapJson === 'object') {
            echarts.registerMap('tibet', mapJson)
            mapLoaded = true
          }
        }
      }
    } catch (e) {
      console.warn('Failed to load Tibet map data, falling back to simple scatter plot', e)
    }

    await loadChartData()
  }

  window.addEventListener('resize', handleResize)
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

const handleResize = () => {
  chart?.resize()
}

const handleZoomChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  zoomLevel.value = parseFloat(target.value)
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (fluctuationTimer) clearInterval(fluctuationTimer)
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
