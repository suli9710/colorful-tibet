<template>
  <div class="min-h-screen bg-stone-50 py-12">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-10">
        <h1 class="text-4xl font-bold text-stone-800 mb-4">西藏非物质文化遗产</h1>
        <p class="text-lg text-stone-600 max-w-3xl mx-auto mb-3">
          西藏非物质文化遗产是雪域高原几千年文明积淀下来的“活态记忆”，
          既承载着独特的宗教信仰与生活方式，也见证着中华文明多元一体的发展历程。
        </p>
        <p class="text-sm text-stone-500 max-w-3xl mx-auto">
          通过保护和传承非遗，我们不仅守护了西藏的文化根脉，也为世界了解中国、了解西藏提供了一扇独特窗口。
        </p>
      </div>

      <!-- 非遗大类一览（简洁卡片设计） -->
      <section class="mb-14">
        <div class="bg-white rounded-3xl border border-stone-200 shadow-sm px-5 sm:px-8 lg:px-10 py-8 lg:py-10">
          <!-- 区块标题 -->
          <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 mb-8">
            <div>
              <h2 class="text-xl sm:text-2xl font-bold text-stone-900 mb-1">
                国家级非物质文化遗产代表性项目大类
              </h2>
              <p class="text-sm sm:text-base text-stone-500 max-w-2xl">
                按照国家级非遗名录的划分，将西藏相关项目归入若干大类，方便你从不同维度走近这片高原的“活态文化”。
              </p>
            </div>
            <div class="flex items-center gap-2">
              <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-red-50 text-red-700 border border-red-100">
                共 {{ heritageCategories.length }} 个大类
              </span>
            </div>
          </div>

          <!-- 卡片网格（点击某一大类，在卡片内部展开可滑动的国家级非遗项目列表） -->
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 lg:gap-7">
            <button
              v-for="(category, index) in heritageCategories"
              :key="category.name"
              type="button"
              class="group bg-stone-50 rounded-2xl border border-stone-200 px-4 py-4 sm:px-5 sm:py-5 flex flex-col gap-3 hover:border-red-200 hover:bg-white hover:shadow-md transition duration-200 text-left w-full cursor-pointer"
              :style="{ transitionDelay: (index * 60) + 'ms' }"
              @click="toggleCategory(category.name)"
            >
              <div class="flex items-center gap-4 w-full">
                <!-- 图标 -->
                <div class="flex-shrink-0">
                  <div class="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-gradient-to-br from-red-500/90 to-amber-400/90 text-white flex items-center justify-center shadow-sm group-hover:shadow-md group-hover:scale-[1.03] transform transition">
                    <span class="text-xl sm:text-2xl">
                      {{ category.icon }}
                    </span>
                  </div>
                </div>

                <!-- 文本 -->
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-red-50 text-red-700 border border-red-100">
                      {{ category.prefix }}
                    </span>
                  </div>
                  <p class="text-base sm:text-lg font-semibold text-stone-900 truncate">
                    {{ category.name }}
                  </p>
                  <p class="text-xs text-stone-400 mt-0.5">
                    点击展开，在卡片内滑动查看该大类下的国家级非遗项目
                  </p>
                </div>
              </div>

              <!-- 卡片内部可滚动的国家级非遗项目列表 -->
              <div
                v-if="activeCategory === category.name"
                class="mt-1 w-full rounded-xl bg-white border border-red-100/70 px-3 py-2 max-h-44 overflow-y-auto text-xs sm:text-sm text-stone-700 space-y-2"
              >
                <p class="text-[11px] text-stone-400">
                  国家级非遗项目 · {{ getItemsByCategory(category.name).length }} 项
                </p>
                <div
                  v-for="item in getItemsByCategory(category.name)"
                  :key="item.id"
                  class="border-b border-stone-100 last:border-b-0 pb-1.5 last:pb-0"
                >
                  <p class="font-medium text-stone-900 mb-0.5">
                    {{ item.name }}
                  </p>
                  <p class="text-[11px] leading-snug text-stone-600 line-clamp-2">
                    {{ item.description }}
                  </p>
                </div>
                <p
                  v-if="!getItemsByCategory(category.name).length"
                  class="text-[11px] text-stone-400"
                >
                  这一大类在当前名录数据中暂未整理出具体项目，后续可以在后台补充。
                </p>
              </div>
            </button>
          </div>
        </div>
      </section>

      <!-- 代表性非遗项目：上来先展示几个可以点击的典型案例 -->
      <section v-if="!loading && heritageItems.length" class="mb-12">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-2xl font-bold text-stone-800">代表性非遗项目</h2>
          <p class="text-sm text-stone-500 hidden md:block">
            从众多非遗中精选出几项最具代表性的项目，点击卡片即可了解它们背后的故事。
          </p>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <button
            v-for="item in representativeItems"
            :key="item.id"
            @click="openDetail(item)"
            class="bg-white rounded-xl shadow-sm border border-stone-200 p-5 text-left hover:shadow-lg hover:border-red-200 transition duration-200 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
          >
            <div class="mb-3">
              <span class="inline-block px-3 py-1 rounded-full text-xs font-medium bg-red-50 text-red-700">
                {{ item.category }}
              </span>
            </div>
            <h3 class="text-lg font-semibold text-stone-900 mb-2 line-clamp-1">
              {{ item.name }}
            </h3>
            <p class="text-sm text-stone-600 mb-3 line-clamp-3">
              {{ item.description }}
            </p>
            <span class="inline-flex items-center text-sm font-medium text-red-600">
              了解详情
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="h-4 w-4 ml-1"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </span>
          </button>
        </div>
      </section>

      <!-- 代表性非遗项目详情弹层（带图片与更详细介绍） -->
      <div
        v-if="selectedItem"
        class="fixed inset-0 bg-black/40 flex items-center justify-center px-4 z-40"
        @click="selectedItem = null"
      >
        <div
          class="bg-white rounded-2xl max-w-3xl w-full shadow-2xl overflow-hidden"
          @click.stop
        >
          <!-- 顶部大图 -->
          <div class="relative h-56 md:h-72 bg-stone-100">
            <img
              :src="selectedItem.imageUrl || '/images/heritage/default-heritage.jpg'"
              :alt="selectedItem.name"
              class="w-full h-full object-cover"
            >
            <button
              type="button"
              class="absolute top-4 right-4 bg-black/50 text-white p-2 rounded-full hover:bg-black/70 transition"
              @click="selectedItem = null"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                class="h-5 w-5"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fill-rule="evenodd"
                  d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <div class="absolute bottom-4 left-4 bg-black/45 backdrop-blur px-4 py-2 rounded-xl">
              <p class="text-xs text-red-100 font-medium mb-1">
                {{ selectedItem.category || '代表性非遗项目' }}
              </p>
              <h3 class="text-xl md:text-2xl font-bold text-white">
                {{ selectedItem.name }}
              </h3>
            </div>
          </div>

          <!-- 文字内容区：分段更详细介绍 + 线下体验模块 -->
          <div class="px-6 py-5 text-sm text-stone-700 space-y-5 max-h-[65vh] overflow-y-auto">
            <div class="space-y-4">
              <p class="text-[13px] text-stone-500">
                下面的文字可以在后台按项目进行更精细的补充，这里将基础说明拆成故事性更强的段落展示。
              </p>

              <div class="space-y-2">
                <h4 class="text-sm font-semibold text-stone-900">
                  基本介绍
                </h4>
                <p class="leading-relaxed whitespace-pre-line">
                  {{ selectedItem.description || '暂无详细介绍，后续可在后台完善这一项目的说明。' }}
                </p>
              </div>

              <div v-if="selectedItem.originStory" class="space-y-2">
                <h4 class="text-sm font-semibold text-stone-900">
                  形成渊源与传说
                </h4>
                <p class="leading-relaxed whitespace-pre-line">
                  {{ selectedItem.originStory }}
                </p>
              </div>

              <div v-if="selectedItem.significance" class="space-y-2 border-t border-dashed border-stone-200 pt-3">
                <h4 class="text-sm font-semibold text-stone-900">
                  文化价值与当代意义
                </h4>
                <p class="text-stone-700 text-sm leading-relaxed whitespace-pre-line">
                  {{ selectedItem.significance }}
                </p>
              </div>

              <p
                v-if="!selectedItem.originStory && !selectedItem.significance"
                class="text-xs text-stone-400 border-t border-dashed border-stone-200 pt-3"
              >
                当前显示的是基础简介，后续可以在后台为该项目补充更完整的“渊源故事”和“文化价值”等字段，让游客对非遗有更立体的理解。
              </p>
            </div>

            <!-- 线下体验模块：地图示意 + 门店列表 + 导航 -->
            <div class="border-t border-dashed border-stone-200 pt-4">
              <div class="flex items-center justify-between gap-2 mb-3">
                <div>
                  <h4 class="text-sm font-semibold text-stone-900">
                    线下体验 · 西藏本地推荐打卡点
                  </h4>
                  <p class="text-[12px] text-stone-500 mt-0.5">
                    找一家离你最近的体验店，现场感受 {{ selectedItem.name }} 背后的技艺与故事。
                  </p>
                </div>
                <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-red-50 text-red-700 border border-red-100">
                  共 {{ experienceSpots.length }} 处体验点
                </span>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-5 gap-4">
                <!-- 交互式地图 -->
                <div class="md:col-span-2 relative rounded-xl overflow-hidden border border-stone-200 min-h-[220px] bg-stone-100">
                  <div ref="mapContainer" class="w-full h-full min-h-[220px]"></div>
                  <!-- 地图加载提示 -->
                  <div v-if="mapLoading" class="absolute inset-0 flex items-center justify-center bg-stone-100/90 backdrop-blur-sm z-20">
                    <div class="text-center">
                      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-red-500 mx-auto mb-2"></div>
                      <p class="text-xs text-stone-600">地图加载中...</p>
                    </div>
                  </div>
                  <!-- 地图标题覆盖层 -->
                  <div class="absolute top-0 left-0 right-0 p-3 bg-gradient-to-b from-black/40 to-transparent z-10 pointer-events-none">
                    <p class="text-[11px] font-medium uppercase tracking-widest text-amber-200">
                      Tibet Experience Map
                    </p>
                    <p class="text-sm font-semibold text-white">
                      西藏体验店分布
                    </p>
                  </div>
                  <!-- 图例 -->
                  <div class="absolute bottom-0 left-0 right-0 p-3 bg-gradient-to-t from-black/40 to-transparent z-10 pointer-events-none">
                    <div class="flex items-center gap-2 text-[11px]">
                      <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-black/35 backdrop-blur text-white">
                        <span class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-300"></span>
                        体验店
                      </span>
                      <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-black/35 backdrop-blur text-white">
                        <span class="inline-block w-1.5 h-1.5 rounded-full bg-amber-300"></span>
                        文化空间
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 门店列表 -->
                <div class="md:col-span-3 space-y-3 max-h-[220px] overflow-y-auto pr-1">
                  <div
                    v-for="spot in experienceSpots"
                    :key="spot.name"
                    class="flex items-start justify-between gap-3 rounded-xl border border-stone-200 bg-stone-50/60 px-3 py-2.5 hover:bg-white hover:border-red-200 transition"
                  >
                    <div class="min-w-0 flex-1">
                      <div class="flex items-center gap-1.5 mb-0.5">
                        <span class="inline-flex items-center justify-center w-5 h-5 rounded-full bg-red-500 text-[11px] text-white font-semibold">
                          {{ spot.tag }}
                        </span>
                        <p class="text-[13px] font-semibold text-stone-900 truncate">
                          {{ spot.name }}
                        </p>
                      </div>
                      <p class="text-[11px] text-stone-500 mb-0.5">
                        {{ spot.city }} · {{ spot.brief }}
                      </p>
                      <p class="text-[11px] text-stone-400 line-clamp-1">
                        {{ spot.address }}
                      </p>
                    </div>
                    <div class="flex flex-col items-end gap-1">
                      <a
                        class="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-medium bg-red-50 text-red-700 border border-red-100 hover:bg-red-600 hover:text-white hover:border-red-600 transition"
                        :href="buildNavUrl(spot)"
                        target="_blank"
                        rel="noopener"
                      >
                        导航前往
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          class="h-3.5 w-3.5 ml-0.5"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke="currentColor"
                        >
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                        </svg>
                      </a>
                      <span class="inline-flex items-center gap-0.5 text-[10px] text-stone-400">
                        <span class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-400/80"></span>
                        打卡 {{ spot.highlight }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api, { endpoints } from '../api'

const cardsEntered = ref(false)

const heritageCategories = [
  {
    name: '民间文学',
    prefix: '民间文学',
    icon: '📜',
    layout: 'lg:-mt-4 lg:ml-4 z-20'
  },
  {
    name: '传统音乐',
    prefix: '传统音乐',
    icon: '🥁',
    layout: 'lg:mt-8 z-30'
  },
  {
    name: '传统舞蹈',
    prefix: '传统舞蹈',
    icon: '💃',
    layout: 'lg:-mt-10 lg:-mr-4 z-40'
  },
  {
    name: '传统戏剧',
    prefix: '传统戏剧',
    icon: '🎭',
    layout: 'lg:-mt-2 lg:ml-8 z-30'
  },
  {
    name: '传统体育·游艺与杂技',
    prefix: '传统体育·游艺与杂技',
    icon: '🏹',
    layout: 'lg:mt-10 z-20'
  },
  {
    name: '传统技艺',
    prefix: '传统技艺',
    icon: '🧶',
    layout: 'lg:-mt-6 lg:-mr-6 z-30'
  }
]

interface HeritageItem {
  id: number
  name: string
  description: string
  category: string
  imageUrl: string
  videoUrl: string
  originStory: string
  significance: string
}

const heritageItems = ref<HeritageItem[]>([])
const loading = ref(true)
const selectedItem = ref<HeritageItem | null>(null)

// 地图相关
const mapContainer = ref<HTMLElement | null>(null)
const mapLoading = ref(true)
let map: L.Map | null = null
let markers: L.Marker[] = []

interface ExperienceSpot {
  name: string
  city: string
  address: string
  lat: number
  lng: number
  tag: string
  brief: string
  highlight: string
}

// 西藏本地线下体验点（示例数据，可在后台扩展为可配置）
const experienceSpots: ExperienceSpot[] = [
  {
    name: '拉萨非遗体验中心（八廓街）',
    city: '拉萨',
    address: '拉萨市城关区八廓街步行街附近',
    lat: 29.653,
    lng: 91.117,
    tag: '藏',
    brief: '非遗集合体验空间，可预约藏戏、唐卡、藏香等项目体验',
    highlight: '一站式打卡多种非遗项目'
  },
  {
    name: '罗布林卡唐卡工坊',
    city: '拉萨',
    address: '拉萨市城关区罗布林卡景区周边传统手工街区',
    lat: 29.642,
    lng: 91.071,
    tag: '画',
    brief: '专注藏族唐卡绘制与展示的工作室，支持短时体验与深度课程',
    highlight: '亲手绘制一幅简易唐卡或吉祥纹样'
  },
  {
    name: '日喀则藏戏传习中心',
    city: '日喀则',
    address: '日喀则市桑珠孜区传统文化街区内',
    lat: 29.268,
    lng: 88.882,
    tag: '戏',
    brief: '定期排练和展演藏戏的传习点，游客可预约观摩与互动体验',
    highlight: '近距离观看一场完整的藏戏表演'
  },
  {
    name: '林芝藏药文化体验馆',
    city: '林芝',
    address: '林芝市巴宜区林芝镇附近康养文化街区',
    lat: 29.654,
    lng: 94.362,
    tag: '药',
    brief: '结合藏药展示、讲解与简易调养体验的综合空间',
    highlight: '了解常见藏药材与传统养生方式'
  }
]

// 代表性项目中要固定展示的几个核心项目
const representativeItems = computed<HeritageItem[]>(() => {
  const extra: HeritageItem[] = [
    {
      id: 10001,
      name: '藏药',
      description: '源自雪域高原的传统医学体系，吸收了藏族本土经验与印度、汉地医学精华，以丸、散、膏、丹等剂型闻名。',
      category: '传统医药',
      imageUrl: '/heritage/藏药.jpg',
      videoUrl: '',
      originStory: '',
      significance: '体现了藏族人民与高原自然环境长期博弈中形成的健康智慧，是中华传统医学宝库的重要组成部分。'
    },
    {
      id: 10002,
      name: '格萨尔史诗',
      description: '被誉为“世界上最长的史诗”，通过艺人口耳相传、即兴说唱的方式一代代流传下来。',
      category: '民间文学',
      imageUrl: '/heritage/格萨尔史诗.jpg',
      videoUrl: '',
      originStory: '',
      significance: '记录了藏族社会的历史记忆、英雄理想与价值观，是中华民族口头传统中的璀璨明珠。'
    },
    {
      id: 10003,
      name: '藏戏',
      description: '被誉为“藏文化的活化石”，集歌舞、说唱、表演于一体，常在寺院法会和民间节日中演出。',
      category: '传统戏剧',
      imageUrl: '/heritage/藏戏.jpg',
      videoUrl: '',
      originStory: '',
      significance: '藏戏综合了宗教仪式、历史故事与民间传说，是研究藏族社会生活与信仰体系的重要窗口。'
    },
    {
      id: 10004,
      name: '藏族唐卡',
      description: '以矿物颜料在布、纸或丝绸上绘制的宗教卷轴画，色彩瑰丽、构图严谨，多悬挂于寺院与居室。',
      category: '传统技艺',
      imageUrl: '/heritage/唐卡.jpg',
      videoUrl: '',
      originStory: '',
      significance: '唐卡承载着藏传佛教教义、历史人物与宇宙观，被视为“可以卷起来带走的宫殿壁画”，是西藏艺术的代表符号之一。'
    }
  ]

  // 为了避免和后台数据重复，先把与手动固定项目同名的条目从后台列表中排除
  const extraNames = new Set(extra.map(item => item.name))

  const base = heritageItems.value
    .filter(item => {
      // 1. 排除与固定项目同名的条目
      if (extraNames.has(item.name)) return false
      // 2. 特殊处理：后台里叫“唐卡”，前端固定用“藏族唐卡”，这里直接去掉后台的“唐卡”
      if (item.name === '唐卡' && extraNames.has('藏族唐卡')) return false
      return true
    })
    .slice(0, 4)

  return [...base, ...extra]
})

// ---- 国家级非物质文化遗产（静态文本解析） ----

interface NationalHeritageItem {
  id: number
  category: string
  name: string
  description: string
}

// 直接把你提供的 txt 内容嵌进来，后面做简单解析
const nationalHeritageRaw = `西藏国家级非物质文化遗产名录 (仅国家级项目)
西藏自治区共有106 项国家级非物质文化遗产代表性项目，其中 3 项 (格萨尔、藏戏、藏医药浴法) 被列入联合国教科文组织人类非物质文化遗产代表作名录。以下是按照六大类整理的完整名单：
一、民间文学类 (1 项)
格萨尔（第一批，2006 年）：世界最长的史诗，被誉为 "活形态史诗"，由艺人口头传唱，2009 年入选联合国教科文组织人类非遗代表作名录
二、传统音乐类 (5 项)
那曲山歌（第一批，2006 年）：西藏高原地区传统民歌形式
门巴族萨玛民歌（第五批，2021 年）：西藏门巴族传统音乐形式，流行于错那市勒布区
古尔鲁（第五批，2021 年）：民间文学与宗教音乐的结合，源于吐蕃时期的口头诗歌，属曲艺类
工布扎念博咚（第五批，2021 年）：工布地区传统弹拨乐器音乐
拉萨囊玛：西藏古典音乐，融合了藏族传统音乐与内地音乐元素
三、传统舞蹈类 (20 项)
热巴舞 (丁青热巴)（第二批，2008 年）：昌都丁青县传统舞蹈，历史可追溯至公元 11 世纪，融合歌舞、杂技的综合性表演
芒康弦子（弦子舞）（第二批，2008 年）：以弦乐伴奏的集体舞，流行于西藏芒康地区
锅庄舞（第一批，2006 年）：包括昌都锅庄舞、那曲锅庄等多个地区流派，藏族传统集体舞
日喀则甲谐（第一批，2006 年）：大型传统歌舞，具有浓郁的西藏地方特色
日喀则斯马卓（第一批，2006 年）：民间鼓舞，又称 "后藏鼓舞"
山南久河卓舞（第四批，2014 年）：传统腰鼓舞，流行于山南市琼结县
古格宣舞（阿里宣舞）（第二批，2008 年）：阿里地区传统宫廷舞蹈，融合藏戏、舞蹈、说唱等传统民间艺术
拉萨囊玛（部分归类为传统舞蹈）：兼具音乐与舞蹈特点的艺术形式
协荣仲孜：大型表演性舞蹈，流行于拉萨市堆龙德庆区
阿古顿巴卓舞：流行于西藏部分地区的传统舞蹈
热振曲卓（第五批，2021 年）：热振地区传统舞蹈形式
堆谐 (拉孜堆谐)："藏式踢踏舞"，流行于日喀则拉孜县
谐钦（多个地区变体）：
南木林土布加谐钦
拉萨纳如谐钦
尼玛乡谐钦
阿谐（达布阿谐）：传统劳动歌舞，流行于西藏部分地区
芒康三弦舞：以三弦琴伴奏的传统舞蹈，流行于西藏芒康地区
米纳羌姆（第四批，2014 年）：传统宗教舞蹈，流行于西藏部分地区
果尔孜舞（第三批，2011 年）：有 1300 多年历史的传统舞蹈，传承沿袭传统的口口相传
陈塘夏尔巴歌舞（第三批，2011 年）：定结县陈塘地区夏尔巴人传统歌舞
嘉黎 "阿古顿巴" 卓舞：那曲市嘉黎县传统舞蹈
普兰 "宣" 服饰舞蹈：阿里地区普兰县传统舞蹈，融合了独特的服饰文化
四、传统戏剧类 (3 项)
藏戏（第一批，2006 年）：包括多个流派，2009 年入选联合国教科文组织人类非遗代表作名录
拉萨觉木隆
日喀则迥巴
日喀则南木林湘巴
日喀则仁布江嘎尔
山南雅隆扎西雪巴（第一批，2006 年）
山南琼结卡卓扎西宾顿（第二批，2008 年）
山南门巴戏（第一批，2006 年）：山南地区错那县勒布区门巴族传统戏剧，2007 年重组戏班后形成 9 人演出团体
巴贡（霞尔巴贡）（第五批，2021 年）：传统戏剧形式，流行于西藏部分地区
五、传统体育・游艺与杂技类 (1 项)
藏族传统马术（第三批，2011 年）：西藏传统体育竞技项目，展示藏族精湛的骑马技艺
六、传统技艺类 (16 项)
藏族唐卡（第一批，2006 年）：包括多个画派，西藏传统绘画艺术
勉唐画派：形成于 15 世纪，由勉拉・顿珠嘉措创立，以线条工整、色彩明快著称
钦泽画派
噶玛嘎孜画派
齐吾岗派（第五批，2021 年）
拉萨堆绣唐卡（第五批，2021 年）
康勉萨唐卡（第五批，2021 年）
藏族金属锻造技艺（第二批，2008 年）：包括多个流派和工艺
藏族锻铜技艺（南木林县）
藏刀锻制技艺（拉孜县）
孜东铜器锻制技艺
扎西吉彩金银锻铜技艺（第三批扩展，2011 年）
擦擦制作技艺（拉萨擦擦制作技艺）（第五批，2021 年）：传统佛教艺术品制作技艺
传统帐篷编制技艺（巴青牛毛帐篷编制技艺）（第五批，2021 年）：那曲市巴青县传统帐篷制作技艺
藏族邦典、卡垫织造技艺（第一批，2006 年）：西藏传统纺织工艺，生产邦典（围裙）和卡垫（地毯）
拉萨甲米水磨坊（第一批，2006 年）：传统水利磨面技术，展示了藏族人民的智慧和创造力
藏族雕版印刷技艺（纳唐寺雕版印刷技艺）（第二批，2008 年）：传统印刷工艺，保存了大量藏文典籍
藏刀锻制技艺（谢通门藏刀锻制技艺）：日喀则市谢通门县传统刀具制作技艺
藏族造纸技艺（热如藏纸制作技艺）：传统手工造纸技术，使用当地特有的植物原料
藏族传统泥塑技艺：西藏传统雕塑工艺，用于制作佛像和工艺品
藏香制作技艺（敏珠林寺藏香制作技艺）（第五批，2021 年）：传统香料制作技艺，具有独特的配方和工艺
泽帖尔编制技艺（第五批，2021 年）：山南市乃东区传统毛纺织技艺，被誉为 "西藏氆氇中的佳品"
藏族扎囊木雕（第五批，2021 年）：扎囊县传统木雕工艺，用于制作佛像和家具
晒盐技艺（井盐晒制技艺）（第二批，2008 年）：芒康县传统制盐工艺，展示了藏族人民与自然和谐共处的智慧
墨脱石锅制作技艺（第四批，2014 年）：林芝市墨脱县传统厨具制作技艺，2015 年成为国家批准保护的地理标志产品
藏族传统榨油技艺（江孜传统榨油技艺）：日喀则市江孜县传统榨油工艺，使用传统的木制榨油设备`

// 简单归一化分类名称，便于和上方大类卡片对应
const normalizeCategoryName = (name: string) => {
  return name
    .replace(/类$/, '')
    .replace(/国家级/g, '')
    .replace(/\s+/g, '')
    .replace(/・/g, '·')
}

const nationalHeritageItems = computed<NationalHeritageItem[]>(() => {
  const lines = nationalHeritageRaw.split('\n').map(l => l.trim()).filter(Boolean)
  const items: NationalHeritageItem[] = []
  let currentCategory = ''
  let id = 1

  const categoryPrefixes = ['一、', '二、', '三、', '四、', '五、', '六、']

  for (const line of lines) {
    // 分类标题行
    if (categoryPrefixes.some(prefix => line.startsWith(prefix))) {
      const parts = line.split('、')
      const rest = parts.slice(1).join('、')
      currentCategory = rest.replace(/\(.*?\)/g, '').trim()
      continue
    }

    // 忽略总说明
    if (!currentCategory || line.startsWith('西藏自治区共有')) continue

    // 尽量用全角冒号或半角冒号拆分为“名称 + 描述”
    const sepIndex = line.indexOf('：')
    let name = line
    let description = ''

    if (sepIndex !== -1) {
      name = line.slice(0, sepIndex).trim()
      description = line.slice(sepIndex + 1).trim()
    }

    // 去掉项目批次等括号信息中的数字，只保留主体名称
    const firstParenIdx = name.indexOf('（')
    if (firstParenIdx !== -1) {
      name = name.slice(0, firstParenIdx).trim()
    }

    items.push({
      id: id++,
      category: currentCategory,
      name,
      description: description || '（暂无补充说明，后续可在后台完善这一条目的详细介绍。）'
    })
  }

  return items
})

// 根据归一化后的分类名称分组，方便点击大类卡片时展示
const nationalHeritageByCategory = computed<Record<string, NationalHeritageItem[]>>(() => {
  const map: Record<string, NationalHeritageItem[]> = {}
  for (const item of nationalHeritageItems.value) {
    const key = normalizeCategoryName(item.category)
    if (!map[key]) map[key] = []
    map[key].push(item)
  }
  return map
})

const activeCategory = ref<string | null>(null)

const getItemsByCategory = (categoryName: string): NationalHeritageItem[] => {
  const key = normalizeCategoryName(categoryName)
  return nationalHeritageByCategory.value[key] || []
}

const fetchHeritageItems = async () => {
  try {
    const response = await api.get(endpoints.heritage.list)
    heritageItems.value = response.data
  } catch (error) {
    console.error('Failed to fetch heritage items:', error)
  } finally {
    loading.value = false
  }
}

const toggleCategory = (categoryName: string) => {
  activeCategory.value = activeCategory.value === categoryName ? null : categoryName
}

const openDetail = (item: HeritageItem) => {
  selectedItem.value = item
}

// 构建地图导航链接（这里以高德地图 Web 导航链接为例，可根据实际需要切换为百度地图等）
const buildNavUrl = (spot: ExperienceSpot) => {
  const base = 'https://uri.amap.com/navigation'
  const to = `${spot.lng},${spot.lat},${encodeURIComponent(spot.name)}`
  return `${base}?to=${to}&mode=car&utm_source=colorful-tibet`
}

// 处理地图图片加载错误
const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  console.warn('地图图片加载失败，使用备用方案')
  // 如果图片加载失败，可以设置一个备用图片或隐藏图片
  img.style.display = 'none'
}

onMounted(() => {
  fetchHeritageItems()
})
</script>

<style scoped>
</style>
