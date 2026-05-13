<template>
  <div ref="panelEl" class="bg-white rounded-2xl shadow overflow-hidden border border-stone-100">
    <div class="px-6 py-5 border-b border-stone-200 flex items-center justify-between">
      <div>
        <h3 class="text-xl font-bold text-stone-800">{{ t('admin.analytics.title') }}</h3>
        <p class="text-sm text-stone-500 mt-1">{{ t('admin.analytics.subtitle') }}</p>
      </div>
      <div class="text-xs text-stone-400">
        {{ chartData?.updatedAt ? t('admin.analytics.updatedAt', { time: formatDateTime(chartData.updatedAt) }) : t('admin.analytics.realtime') }}
      </div>
    </div>

    <div v-if="loading" class="p-10 text-center text-stone-500">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
      <p>{{ t('admin.analytics.loading') }}</p>
    </div>

    <div v-else-if="error" class="p-8 text-center text-red-600">
      <p class="font-medium">{{ error }}</p>
    </div>

    <div v-else class="p-6 space-y-6">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div v-for="card in summaryCards" :key="card.label" class="rounded-2xl bg-stone-50 p-4 border border-stone-100">
          <p class="text-sm text-stone-500">{{ card.label }}</p>
          <p class="mt-2 text-2xl font-bold text-stone-900">{{ card.value }}</p>
          <p class="text-xs text-stone-400 mt-1">{{ card.hint }}</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div class="rounded-2xl border border-stone-100 p-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="font-semibold text-stone-800">{{ t('admin.analytics.charts.orderRevenueTrend') }}</h4>
            <span class="text-xs text-stone-400">{{ t('admin.analytics.charts.lastSixMonths') }}</span>
          </div>
          <div ref="trendChartEl" class="w-full h-80"></div>
        </div>

        <div class="rounded-2xl border border-stone-100 p-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="font-semibold text-stone-800">{{ t('admin.analytics.charts.categoryShare') }}</h4>
            <span class="text-xs text-stone-400">{{ t('admin.analytics.charts.currentSpotData') }}</span>
          </div>
          <div ref="categoryChartEl" class="w-full h-80"></div>
        </div>

        <div class="rounded-2xl border border-stone-100 p-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="font-semibold text-stone-800">{{ t('admin.analytics.charts.popularSpotVisits') }}</h4>
            <span class="text-xs text-stone-400">{{ t('admin.analytics.charts.topEight') }}</span>
          </div>
          <div ref="spotChartEl" class="w-full h-80"></div>
        </div>

        <div class="rounded-2xl border border-stone-100 p-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="font-semibold text-stone-800">{{ t('admin.analytics.charts.newsAndUserGrowth') }}</h4>
            <span class="text-xs text-stone-400">{{ t('admin.analytics.charts.lastSixMonths') }}</span>
          </div>
          <div ref="growthChartEl" class="w-full h-80"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ECharts } from '@/lib/echarts'

interface AnalyticsStats {
  userCount: number
  orderCount: number
  totalRevenue: number
  spotCount?: number
  recentBookings?: any[]
  recentHotelBookings?: any[]
  popularSpots?: any[]
  newsCount?: number
  monthlyBookingTrend?: Array<{ month: string; orderCount: number; revenue: number }>
  userGrowthTrend?: Array<{ month: string; count: number }>
  newsPublishTrend?: Array<{ month: string; count: number }>
  spotCategories?: Array<{ name: string; value: number }>
  updatedAt?: string
}

const props = defineProps<{
  loading: boolean
  error?: string
  chartData?: AnalyticsStats | null
}>()

const trendChartEl = ref<HTMLElement | null>(null)
const categoryChartEl = ref<HTMLElement | null>(null)
const spotChartEl = ref<HTMLElement | null>(null)
const growthChartEl = ref<HTMLElement | null>(null)
const panelEl = ref<HTMLElement | null>(null)
const charts: ECharts[] = []
const { t, locale } = useI18n()
type EChartsKit = ReturnType<typeof import('@/lib/echarts').ensureECharts>
let echartsLoader: Promise<EChartsKit> | null = null
let chartInitRun = 0
let resizeObserver: ResizeObserver | null = null

const loadECharts = async () => {
  if (!echartsLoader) {
    echartsLoader = import('@/lib/echarts').then(module => module.ensureECharts())
  }

  return echartsLoader
}

const getCategoryLabel = (category: string) => {
  const key = `admin.analytics.spotCategories.${category}`
  const label = t(key)
  return label === key ? category : label
}

const summaryCards = computed(() => {
  const data = props.chartData
  return [
    { label: t('admin.analytics.summary.userCount'), value: data?.userCount ?? 0, hint: t('admin.analytics.summary.userHint') },
    { label: t('admin.analytics.summary.orderCount'), value: data?.orderCount ?? 0, hint: t('admin.analytics.summary.orderHint') },
    { label: t('admin.analytics.summary.revenue'), value: `¥${Number(data?.totalRevenue ?? 0).toLocaleString()}`, hint: t('admin.analytics.summary.revenueHint') },
    { label: t('admin.analytics.summary.spotCount'), value: data?.spotCount ?? 0, hint: t('admin.analytics.summary.spotHint') },
  ]
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString(locale.value === 'bo' ? 'bo-CN' : 'zh-CN')
}

const disposeCharts = () => {
  while (charts.length) {
    charts.pop()?.dispose()
  }
}

const initCharts = async () => {
  if (!props.chartData) return
  const runId = ++chartInitRun
  const { graphic, init } = await loadECharts()
  if (runId !== chartInitRun) return

  disposeCharts()

  const data = props.chartData
  const trendChart = trendChartEl.value ? init(trendChartEl.value) : null
  const categoryChart = categoryChartEl.value ? init(categoryChartEl.value) : null
  const spotChart = spotChartEl.value ? init(spotChartEl.value) : null
  const growthChart = growthChartEl.value ? init(growthChartEl.value) : null

  if (trendChart) {
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: [t('admin.analytics.charts.orders'), t('admin.analytics.charts.revenue')], top: 6 },
      grid: { left: 40, right: 30, top: 50, bottom: 30 },
      xAxis: {
        type: 'category',
        name: t('admin.analytics.charts.month'),
        data: (data.monthlyBookingTrend || []).map(item => item.month)
      },
      yAxis: [
        { type: 'value', name: t('admin.analytics.charts.orders') },
        { type: 'value', name: t('admin.analytics.charts.revenue'), position: 'right' }
      ],
      series: [
        {
          name: t('admin.analytics.charts.orders'),
          type: 'bar',
          data: (data.monthlyBookingTrend || []).map(item => item.orderCount),
          itemStyle: { color: '#3b82f6' }
        },
        {
          name: t('admin.analytics.charts.revenue'),
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          data: (data.monthlyBookingTrend || []).map(item => item.revenue),
          itemStyle: { color: '#f59e0b' }
        }
      ]
    })
    charts.push(trendChart)
  }

  if (categoryChart) {
    const categoryData = (data.spotCategories || []).map(item => ({
      name: getCategoryLabel(item.name),
      value: item.value
    }))
    categoryChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: true,
          data: categoryData,
          label: { formatter: '{b}\n{d}%' }
        }
      ]
    })
    charts.push(categoryChart)
  }

  if (spotChart) {
    const sortedSpots = [...(data.popularSpots || [])]
      .sort((a, b) => (b.visitCount || 0) - (a.visitCount || 0))
      .slice(0, 8)
      .reverse()

    spotChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 120, right: 30, top: 20, bottom: 20 },
      xAxis: { type: 'value', name: t('admin.analytics.charts.visits') },
      yAxis: {
        type: 'category',
        name: t('admin.analytics.charts.spot'),
        data: sortedSpots.map(item => item.name),
        axisLabel: { interval: 0, width: 100, overflow: 'truncate' }
      },
      series: [
        {
          type: 'bar',
          data: sortedSpots.map(item => item.visitCount || 0),
          barWidth: 16,
          itemStyle: {
            borderRadius: [0, 8, 8, 0],
            color: new graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#60a5fa' },
              { offset: 1, color: '#2563eb' }
            ])
          }
        }
      ]
    })
    charts.push(spotChart)
  }

  if (growthChart) {
    growthChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: [t('admin.analytics.charts.userGrowth'), t('admin.analytics.charts.newsPublished')], top: 6 },
      grid: { left: 40, right: 30, top: 50, bottom: 30 },
      xAxis: {
        type: 'category',
        name: t('admin.analytics.charts.month'),
        data: (data.userGrowthTrend || []).map(item => item.month)
      },
      yAxis: { type: 'value', name: t('admin.analytics.charts.quantity') },
      series: [
        {
          name: t('admin.analytics.charts.userGrowth'),
          type: 'line',
          smooth: true,
          areaStyle: {},
          data: (data.userGrowthTrend || []).map(item => item.count),
          itemStyle: { color: '#10b981' }
        },
        {
          name: t('admin.analytics.charts.newsPublished'),
          type: 'bar',
          data: (data.newsPublishTrend || []).map(item => item.count),
          itemStyle: { color: '#8b5cf6' }
        }
      ]
    })
    charts.push(growthChart)
  }
}

const handleResize = () => {
  charts.forEach(chart => chart.resize())
}

watch(() => props.chartData, async () => {
  await nextTick()
  await initCharts()
}, { deep: true })

watch(locale, async () => {
  await nextTick()
  await initCharts()
})

onMounted(() => {
  if (panelEl.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(handleResize)
    resizeObserver.observe(panelEl.value)
  } else {
    window.addEventListener('resize', handleResize)
  }
  nextTick(() => {
    void initCharts()
  })
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  } else {
    window.removeEventListener('resize', handleResize)
  }
  disposeCharts()
})
</script>
