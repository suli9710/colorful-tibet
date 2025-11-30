<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import api from '@/api'

const { locale } = useI18n()
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
const zoomLevel = ref(1.0) // 缩放倍率，范围 0.5 - 5.0
let mapLoaded = false // 地图是否加载成功（在onMounted中设置）

const loadChartData = async () => {
  if (!chartRef.value || !chart) return
  
  try {
    const response = await api.get('/spots')
    const spots = Array.isArray(response.data) ? response.data : (response.data.content || [])
    
    // 过滤掉没有经纬度的景点，保留所有有坐标的景点（包括visitCount为0的）
    const data = spots
      .filter((spot: any) => spot.longitude != null && spot.latitude != null)
      .map((spot: any) => ({
        name: spot.name,
        value: [spot.longitude, spot.latitude, spot.visitCount || 1] // 最小值为1，确保所有景点都能显示
      }))

    // 根据地图是否加载成功，使用不同的配置
    const option: any = {
      title: {
        text: '西藏热门景点热力分布',
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
          let level = '一般'
          if (heat >= 2000) level = '🔥🔥🔥 超级热门'
          else if (heat >= 1500) level = '🔥🔥 非常热门'
          else if (heat >= 1000) level = '🔥 热门'
          else if (heat >= 500) level = '⭐ 较热门'
          else if (heat >= 100) level = '⭐ 一般热门'
          else if (heat > 0) level = '📍 景点'
          else level = '📍 景点'
          return `<strong style="font-size: 14px">${params.name}</strong><br/>访问热度: ${heat}<br/>热度等级: ${level}`
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
            // 根据热度动态调整大小：1-2500 -> 6-25px
            // 确保即使热度很低的景点也能看到（最小6px）
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
          // 显示热度前10的景点，或者热度大于等于100的景点
          data: data
            .filter((item: any) => item.value[2] >= 100) // 只显示热度>=100的景点
            .sort((a: any, b: any) => b.value[2] - a.value[2])
            .slice(0, 10), // 最多显示10个热门景点
          symbolSize: function (val: any) {
            // 热门景点更大：100-2500 -> 18-30px
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

    // 如果地图加载成功，添加geo配置
    if (mapLoaded) {
      option.geo = {
        map: 'tibet',
        roam: 'move', // 只允许拖拽，缩放由滑块控制
        center: [90.0, 30.5], // 西藏中心位置
        zoom: zoomLevel.value, // 使用响应式的缩放级别
        scaleLimit: {
          min: 0.5, // 最小缩放级别（可以缩小到50%）
          max: 5 // 最大缩放级别（可以放大到500%）
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
      // 如果地图加载失败，使用地理坐标系（基于经纬度的散点图）
      option.geo = {
        roam: 'move',
        center: [90.0, 30.5],
        zoom: zoomLevel.value,
        scaleLimit: {
          min: 0.5,
          max: 5
        },
        map: undefined, // 不使用地图
        itemStyle: {
          areaColor: 'transparent',
          borderColor: 'transparent'
        }
      }
      // 为散点图添加地理坐标配置
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
    
    // Load Tibet Map Data (西藏自治区地图)
    try {
      const mapResponse = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/540000_full.json')
      // 检查响应状态和内容类型
      if (mapResponse.ok) {
        const contentType = mapResponse.headers.get('content-type')
        if (contentType && contentType.includes('application/json')) {
          const mapJson = await mapResponse.json()
          // 验证返回的是有效的JSON对象
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

// 监听缩放级别变化，更新地图
watch(zoomLevel, (newZoom) => {
  if (chart) {
    chart.setOption({
      geo: {
        zoom: newZoom
      }
    })
  }
}, { immediate: false })

// 监听语言变化，重新加载数据
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
  chart?.dispose()
})
</script>

<template>
  <div class="relative w-full h-[600px] bg-white rounded-2xl shadow-lg border border-gray-200">
    <div ref="chartRef" class="w-full h-full"></div>
    <!-- 左下角缩放控制器 -->
    <div class="absolute left-4 bottom-4 bg-white/90 backdrop-blur-sm rounded-lg shadow-lg border border-gray-200 p-4 min-w-[200px]">
      <div class="flex items-center justify-between mb-2">
        <span class="text-sm font-medium text-gray-700">缩放倍率</span>
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
/* 自定义滑块样式 */
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
