<template>
  <div class="bg-white rounded-2xl shadow overflow-hidden border border-stone-100">
    <div class="px-6 py-5 border-b border-stone-200 flex items-center justify-between">
      <div>
        <h3 class="text-xl font-bold text-stone-800">运营分析面板</h3>
        <p class="text-sm text-stone-500 mt-1">景点、订单、用户与资讯的关键运营指标</p>
      </div>
      <div class="text-xs text-stone-400">{{ chartData?.updatedAt ? `数据更新时间：${formatDateTime(chartData.updatedAt)}` : '实时统计' }}</div>
    </div>

    <div v-if="loading" class="p-10 text-center text-stone-500">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
      <p>正在加载运营分析数据...</p>
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
            <h4 class="font-semibold text-stone-800">订单与营收趋势</h4>
            <span class="text-xs text-stone-400">近六个月</span>
          </div>
          <div ref="trendChartEl" class="w-full h-80"></div>
        </div>

        <div class="rounded-2xl border border-stone-100 p-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="font-semibold text-stone-800">景点分类占比</h4>
            <span class="text-xs text-stone-400">按当前景点数据统计</span>
          </div>
          <div ref="categoryChartEl" class="w-full h-80"></div>
        </div>

        <div class="rounded-2xl border border-stone-100 p-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="font-semibold text-stone-800">热门景点访问量</h4>
            <span class="text-xs text-stone-400">前八名</span>
          </div>
          <div ref="spotChartEl" class="w-full h-80"></div>
        </div>

        <div class="rounded-2xl border border-stone-100 p-4">
          <div class="flex items-center justify-between mb-3">
            <h4 class="font-semibold text-stone-800">资讯与用户增长</h4>
            <span class="text-xs text-stone-400">近六个月</span>
          </div>
          <div ref="growthChartEl" class="w-full h-80"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

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
const charts: echarts.ECharts[] = []

const categoryLabelMap: Record<string, string> = {
  NATURAL: '自然景观',
  HISTORICAL: '历史人文',
  CULTURAL: '文化体验',
  UNCLASSIFIED: '未分类'
}

const summaryCards = computed(() => {
  const data = props.chartData
  return [
    { label: '总用户数', value: data?.userCount ?? 0, hint: '平台注册用户规模' },
    { label: '总订单数', value: data?.orderCount ?? 0, hint: '景点 + 酒店已确认订单' },
    { label: '总营收', value: `¥${Number(data?.totalRevenue ?? 0).toLocaleString()}`, hint: '已确认订单营收' },
    { label: '景点总数', value: data?.spotCount ?? 0, hint: '当前景点资源数量' },
  ]
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

const disposeCharts = () => {
  while (charts.length) {
    charts.pop()?.dispose()
  }
}

const initCharts = () => {
  if (!props.chartData) return
  disposeCharts()

  const data = props.chartData
  const trendChart = trendChartEl.value ? echarts.init(trendChartEl.value) : null
  const categoryChart = categoryChartEl.value ? echarts.init(categoryChartEl.value) : null
  const spotChart = spotChartEl.value ? echarts.init(spotChartEl.value) : null
  const growthChart = growthChartEl.value ? echarts.init(growthChartEl.value) : null

  if (trendChart) {
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['订单数', '营收'], top: 6 },
      grid: { left: 40, right: 30, top: 50, bottom: 30 },
      xAxis: {
        type: 'category',
        name: '月份',
        data: (data.monthlyBookingTrend || []).map(item => item.month)
      },
      yAxis: [
        { type: 'value', name: '订单数' },
        { type: 'value', name: '营收', position: 'right' }
      ],
      series: [
        {
          name: '订单数',
          type: 'bar',
          data: (data.monthlyBookingTrend || []).map(item => item.orderCount),
          itemStyle: { color: '#3b82f6' }
        },
        {
          name: '营收',
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
      name: categoryLabelMap[item.name] || item.name,
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
      xAxis: { type: 'value', name: '访问量' },
      yAxis: {
        type: 'category',
        name: '景点',
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
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
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
      legend: { data: ['用户增长', '资讯发布'], top: 6 },
      grid: { left: 40, right: 30, top: 50, bottom: 30 },
      xAxis: {
        type: 'category',
        name: '月份',
        data: (data.userGrowthTrend || []).map(item => item.month)
      },
      yAxis: { type: 'value', name: '数量' },
      series: [
        {
          name: '用户增长',
          type: 'line',
          smooth: true,
          areaStyle: {},
          data: (data.userGrowthTrend || []).map(item => item.count),
          itemStyle: { color: '#10b981' }
        },
        {
          name: '资讯发布',
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
  initCharts()
}, { deep: true })

onMounted(() => {
  window.addEventListener('resize', handleResize)
  nextTick(() => initCharts())
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})
</script>
