<template>
  <div class="min-h-screen tibet-page-shell py-12 sm:py-16 lg:py-24 relative overflow-hidden">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative">
      <motion.div
        class="text-center mb-8 sm:mb-10"
        :initial="revealInitial"
        :animate="revealInView"
        :transition="revealTransition"
      >
        <h1 class="tibet-heading inline-flex justify-center text-3xl font-bold text-tibet-dark mb-3 sm:text-4xl sm:mb-4">{{ t('heritage.title') }}</h1>
        <p class="text-sm leading-relaxed text-tibet-brown/70 max-w-3xl mx-auto mb-2 sm:text-lg sm:mb-3">
          {{ t('heritage.description') }}
        </p>
        <p class="text-xs leading-relaxed text-tibet-brown/50 max-w-3xl mx-auto sm:text-sm">
          {{ t('heritage.description2') }}
        </p>
      </motion.div>

      <!-- 搜索栏 -->
      <div class="mb-6 max-w-xl mx-auto sm:mb-8">
        <form class="flex flex-col gap-2 sm:flex-row sm:items-center" @submit.prevent="handleSearch">
          <div class="relative flex-1">
            <input
              v-model="searchKeyword"
              type="text"
              :placeholder="t('heritage.searchPlaceholder', '搜索非遗项目...')"
              class="w-full pl-10 pr-4 py-3 sm:py-2.5 rounded-xl border border-stone-200 bg-white/80 backdrop-blur text-sm text-stone-700 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-tibet-red/30 focus:border-tibet-red/40 transition"
            />
            <svg xmlns="http://www.w3.org/2000/svg" class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <button
            type="submit"
            class="mobile-touch-target px-4 py-2.5 rounded-xl bg-tibet-red text-white text-sm font-medium hover:bg-tibet-red/90 transition disabled:opacity-50"
            :disabled="searchLoading"
          >
            {{ searchLoading ? '...' : t('heritage.search', '搜索') }}
          </button>
          <button
            v-if="searchKeyword"
            type="button"
            class="mobile-touch-target px-3 py-2.5 rounded-xl border border-stone-200 text-sm text-stone-500 hover:bg-stone-50 transition"
            @click="searchKeyword = ''; handleSearch()"
          >
            {{ t('heritage.clearSearch', '清除') }}
          </button>
        </form>
      </div>

      <motion.section
        v-if="!loading"
        class="mb-8 grid grid-cols-1 gap-4 sm:mb-10 sm:gap-6 xl:grid-cols-[minmax(0,1fr)_360px]"
        :initial="{ opacity: 0, y: 20 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.34, ease: motionEase }"
      >
        <div class="tibet-panel rounded-2xl p-4 sm:p-6">
          <div class="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4 mb-5">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.2em] text-tibet-red/70 mb-2">Heritage Knowledge Base</p>
              <h2 class="text-2xl font-bold text-tibet-dark">非遗项目库</h2>
              <p class="mt-1 text-sm text-tibet-brown/60">
                项目热度、传承人故事、活动日历与用户讨论集中呈现。
              </p>
            </div>
            <button
              type="button"
              class="inline-flex items-center justify-center gap-2 rounded-xl border border-tibet-gold/30 bg-white/75 px-4 py-2 text-sm font-medium text-tibet-dark transition hover:border-tibet-red/35 hover:text-tibet-red disabled:opacity-50"
              :disabled="searchLoading"
              @click="refreshHeritageModule"
            >
              <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': searchLoading }" />
              刷新
            </button>
          </div>

          <div class="grid grid-cols-2 lg:grid-cols-4 gap-2.5 sm:gap-3 mb-5">
            <div class="rounded-xl border border-stone-200 bg-white/70 px-3 py-3 sm:px-4">
              <div class="flex items-center justify-between gap-2">
                <p class="text-xs text-stone-500">收录项目</p>
                <BookOpen class="h-4 w-4 text-tibet-red" />
              </div>
              <p class="mt-2 text-xl font-bold text-stone-900 sm:text-2xl">{{ formatCompact(heritageItems.length) }}</p>
            </div>
            <div class="rounded-xl border border-stone-200 bg-white/70 px-3 py-3 sm:px-4">
              <div class="flex items-center justify-between gap-2">
                <p class="text-xs text-stone-500">累计浏览</p>
                <Eye class="h-4 w-4 text-blue-600" />
              </div>
              <p class="mt-2 text-xl font-bold text-stone-900 sm:text-2xl">{{ formatCompact(totalViews) }}</p>
            </div>
            <div class="rounded-xl border border-stone-200 bg-white/70 px-3 py-3 sm:px-4">
              <div class="flex items-center justify-between gap-2">
                <p class="text-xs text-stone-500">互动量</p>
                <MessageCircle class="h-4 w-4 text-emerald-600" />
              </div>
              <p class="mt-2 text-xl font-bold text-stone-900 sm:text-2xl">{{ formatCompact(totalInteractions) }}</p>
            </div>
            <div class="rounded-xl border border-stone-200 bg-white/70 px-3 py-3 sm:px-4">
              <div class="flex items-center justify-between gap-2">
                <p class="text-xs text-stone-500">传承人档案</p>
                <UserRound class="h-4 w-4 text-amber-600" />
              </div>
              <p class="mt-2 text-xl font-bold text-stone-900 sm:text-2xl">{{ formatCompact(featuredInheritors.length) }}</p>
            </div>
          </div>

          <div class="mb-5 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div class="flex min-w-0 items-center gap-2 overflow-x-auto pb-1">
              <button
                type="button"
                class="shrink-0 rounded-full border px-3 py-1.5 text-xs font-medium transition"
                :class="selectedCategory === 'all' ? 'border-tibet-red bg-tibet-red text-white' : 'border-stone-200 bg-white/70 text-stone-600 hover:border-tibet-red/30 hover:text-tibet-red'"
                @click="selectedCategory = 'all'"
              >
                全部
              </button>
              <button
                v-for="category in categoryOptions"
                :key="category"
                type="button"
                class="shrink-0 rounded-full border px-3 py-1.5 text-xs font-medium transition"
                :class="selectedCategory === category ? 'border-tibet-red bg-tibet-red text-white' : 'border-stone-200 bg-white/70 text-stone-600 hover:border-tibet-red/30 hover:text-tibet-red'"
                @click="selectCategory(category)"
              >
                {{ category }}
              </button>
            </div>
            <label class="inline-flex items-center gap-2 rounded-xl border border-stone-200 bg-white/75 px-3 py-2 text-sm text-stone-600">
              <SlidersHorizontal class="h-4 w-4 text-stone-400" />
              <select v-model="sortMode" class="bg-transparent text-sm font-medium text-stone-700 focus:outline-none">
                <option v-for="option in sortOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>

          <AnimatePresence mode="popLayout">
            <motion.div
              v-if="filteredHeritageItems.length"
              key="heritage-dynamic-grid"
              class="grid grid-cols-1 md:grid-cols-2 2xl:grid-cols-3 gap-4"
              :initial="{ opacity: 0 }"
              :animate="{ opacity: 1 }"
              :exit="{ opacity: 0 }"
              :transition="{ duration: 0.22, ease: motionEase }"
            >
              <motion.button
                v-for="(item, index) in filteredHeritageItems"
                :key="item.id"
                type="button"
                layout
                class="group rounded-2xl border border-stone-200 bg-white/80 p-3 text-left shadow-sm transition hover:border-tibet-red/30 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2"
                :initial="{ opacity: 0, y: 14 }"
                :animate="{ opacity: 1, y: 0 }"
                :transition="cardTransition(index, 0.03)"
                :whileHover="{ y: -4 }"
                :whileTap="{ scale: 0.99 }"
                @click="openDetail(item)"
              >
                <div class="relative h-40 overflow-hidden rounded-xl bg-stone-100">
                  <img
                    v-if="resolveHeritageImage(item)"
                    :src="resolveHeritageImage(item)"
                    :alt="item.name"
                    class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                    loading="lazy"
                    @error="applyHeritageImageFallback"
                  />
                  <div v-else class="flex h-full w-full items-center justify-center bg-gradient-to-br from-stone-100 to-amber-50 text-stone-400">
                    <BookOpen class="h-8 w-8" />
                  </div>
                  <div class="absolute left-3 top-3 max-w-[75%] rounded-full bg-black/55 px-2.5 py-1 text-[11px] font-medium text-white backdrop-blur">
                    <span class="line-clamp-1">{{ item.category || '非遗项目' }}</span>
                  </div>
                  <div v-if="item.videoUrl" class="absolute bottom-3 right-3 rounded-full bg-white/90 p-2 text-tibet-red shadow-sm">
                    <Video class="h-4 w-4" />
                  </div>
                </div>
                <div class="px-1 pt-3">
                  <div class="mb-2 flex items-start justify-between gap-3">
                    <h3 class="line-clamp-1 text-lg font-bold text-stone-900 group-hover:text-tibet-red">
                      {{ item.name }}
                    </h3>
                    <ArrowUpRight class="mt-1 h-4 w-4 shrink-0 text-stone-300 transition group-hover:text-tibet-red" />
                  </div>
                  <p class="line-clamp-2 min-h-[2.5rem] text-sm leading-relaxed text-stone-600">
                    {{ item.description || '项目简介正在完善。' }}
                  </p>
                  <div class="mt-3 flex flex-wrap items-center gap-3 text-xs text-stone-500">
                    <span class="inline-flex items-center gap-1">
                      <Eye class="h-3.5 w-3.5" />
                      {{ formatCompact(item.viewCount || 0) }}
                    </span>
                    <span class="inline-flex items-center gap-1">
                      <Heart class="h-3.5 w-3.5" />
                      {{ formatCompact(item.likeCount || 0) }}
                    </span>
                    <span class="inline-flex items-center gap-1">
                      <MessageCircle class="h-3.5 w-3.5" />
                      {{ formatCompact(item.commentCount || 0) }}
                    </span>
                    <span v-if="item.protectionLevel" class="ml-auto rounded-full bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700">
                      {{ item.protectionLevel }}
                    </span>
                  </div>
                </div>
              </motion.button>
            </motion.div>
            <motion.div
              v-else
              key="heritage-empty"
              class="rounded-2xl border border-dashed border-stone-300 bg-white/60 px-6 py-10 text-center"
              :initial="{ opacity: 0, y: 12 }"
              :animate="{ opacity: 1, y: 0 }"
              :exit="{ opacity: 0, y: 12 }"
              :transition="{ duration: 0.22, ease: motionEase }"
            >
              <Search class="mx-auto h-8 w-8 text-stone-300" />
              <p class="mt-3 text-sm font-medium text-stone-700">没有匹配的非遗项目</p>
              <p class="mt-1 text-xs text-stone-400">清空关键词或切换分类后再查看。</p>
            </motion.div>
          </AnimatePresence>
        </div>

        <aside class="space-y-4">
          <div class="tibet-panel rounded-2xl p-4 sm:p-5">
            <div class="mb-4 flex items-center justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.16em] text-tibet-red/60">Inheritors</p>
                <h3 class="mt-1 text-lg font-bold text-stone-900">传承人档案</h3>
              </div>
              <UserRound class="h-5 w-5 text-tibet-red" />
            </div>
            <div v-if="featuredInheritorsLoading" class="flex justify-center py-6">
              <div class="h-6 w-6 rounded-full border-b-2 border-tibet-red animate-spin"></div>
            </div>
            <div v-else-if="featuredInheritors.length" class="space-y-3">
              <button
                v-for="inheritor in featuredInheritors"
                :key="inheritor.id"
                type="button"
                class="flex w-full items-start gap-3 rounded-xl border border-stone-200 bg-white/70 px-3 py-3 text-left transition hover:border-tibet-red/30 hover:bg-white"
                @click="openHeritageById(inheritor.heritageItemId)"
              >
                <div class="h-12 w-12 shrink-0 overflow-hidden rounded-full bg-stone-100">
                  <img
                    v-if="inheritor.avatarUrl"
                    :src="inheritor.avatarUrl"
                    :alt="inheritor.name"
                    class="h-full w-full object-cover"
                    loading="lazy"
                  />
                  <div v-else class="flex h-full w-full items-center justify-center text-sm font-bold text-tibet-red">
                    {{ inheritor.name?.charAt(0) }}
                  </div>
                </div>
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2">
                    <p class="truncate text-sm font-bold text-stone-900">{{ inheritor.name }}</p>
                    <span v-if="inheritor.level" class="shrink-0 rounded bg-amber-50 px-1.5 py-0.5 text-[10px] font-medium text-amber-700">
                      {{ inheritor.level }}
                    </span>
                  </div>
                  <p class="mt-0.5 text-[11px] text-stone-400">
                    {{ heritageNameById[inheritor.heritageItemId] || inheritor.region || '非遗项目' }}
                  </p>
                  <p class="mt-1 line-clamp-2 text-xs leading-relaxed text-stone-600">
                    {{ inheritor.story || inheritor.bio || '暂无传承故事' }}
                  </p>
                </div>
              </button>
            </div>
            <p v-else class="rounded-xl border border-dashed border-stone-200 bg-white/60 px-4 py-5 text-center text-xs text-stone-400">
              暂无可展示的传承人档案
            </p>
          </div>

          <div class="tibet-panel rounded-2xl p-4 sm:p-5">
            <div class="mb-4 flex items-center justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.16em] text-tibet-red/60">Calendar</p>
                <h3 class="mt-1 text-lg font-bold text-stone-900">活动日历</h3>
              </div>
              <CalendarDays class="h-5 w-5 text-tibet-red" />
            </div>
            <div v-if="upcomingEvents.length" class="space-y-3">
              <button
                v-for="event in upcomingEvents.slice(0, 5)"
                :key="event.id"
                type="button"
                class="flex w-full gap-3 rounded-xl border border-stone-200 bg-white/70 px-3 py-3 text-left transition hover:border-tibet-red/30 hover:bg-white"
                @click="event.heritageItemId && openHeritageById(event.heritageItemId)"
              >
                <div class="w-14 shrink-0 rounded-lg bg-tibet-red/10 px-2 py-2 text-center text-tibet-red">
                  <p class="text-[11px] font-semibold leading-tight">{{ formatEventMonth(event.eventDate) }}</p>
                  <p class="text-lg font-bold leading-tight">{{ formatEventDay(event.eventDate) }}</p>
                </div>
                <div class="min-w-0 flex-1">
                  <p class="line-clamp-1 text-sm font-semibold text-stone-900">{{ event.title }}</p>
                  <p v-if="event.location" class="mt-1 line-clamp-1 text-xs text-stone-500">{{ event.location }}</p>
                  <p v-if="event.description" class="mt-1 line-clamp-2 text-xs leading-relaxed text-stone-500">{{ event.description }}</p>
                </div>
              </button>
            </div>
            <p v-else class="rounded-xl border border-dashed border-stone-200 bg-white/60 px-4 py-5 text-center text-xs text-stone-400">
              暂无近期活动
            </p>
          </div>
        </aside>
      </motion.section>

      <!-- 非遗大类一览（简洁卡片设计） -->
      <motion.section
        class="mb-10 sm:mb-14"
        :initial="cardInitial"
        :whileInView="cardInView"
        :inViewOptions="inViewOnce"
        :transition="cardTransition(0, 0.05)"
      >
        <div class="tibet-panel rounded-3xl px-4 py-6 sm:px-8 sm:py-8 lg:px-10 lg:py-10">
          <!-- 区块标题 -->
          <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 mb-6 sm:mb-8">
            <div>
              <h2 class="text-xl sm:text-2xl font-bold text-stone-900 mb-1">
                {{ t('heritage.categoriesTitle') }}
              </h2>
              <p class="text-sm sm:text-base text-stone-500 max-w-2xl">
                {{ t('heritage.categoriesDescription') }}
              </p>
            </div>
            <div class="flex items-center gap-2">
              <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-red-50 text-red-700 border border-red-100">
                {{ t('heritage.totalCategories', { count: heritageCategories.length }) }}
              </span>
            </div>
          </div>

          <!-- 卡片网格（点击某一大类，在卡片内部展开可滑动的国家级非遗项目列表） -->
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 lg:gap-7">
            <motion.button
              v-for="(category, index) in heritageCategories"
              :key="category.name"
              type="button"
              layout
              class="group tibet-card-elevated rounded-2xl border border-tibet-gold/20 px-4 py-4 sm:px-5 sm:py-5 flex flex-col gap-3 hover:border-tibet-red/25 text-left w-full cursor-pointer"
              :initial="cardInitial"
              :whileInView="cardInView"
              :inViewOptions="inViewOnce"
              :transition="cardTransition(index, 0.08)"
              :whileHover="{ y: -5, scale: 1.012 }"
              :whileTap="{ scale: 0.985 }"
              @click="toggleCategory(category.name)"
            >
              <div class="flex items-center gap-4 w-full">
                <!-- 图标 -->
                <div class="flex-shrink-0">
                  <motion.div
                    class="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-gradient-to-br from-tibet-red via-tibet-gold to-tibet-yellow text-white flex items-center justify-center shadow-sm group-hover:shadow-md"
                    :animate="activeCategory === category.name ? { rotate: -4, scale: 1.06 } : { rotate: 0, scale: 1 }"
                    :transition="softSpring"
                  >
                    <span class="text-xl sm:text-2xl">
                      {{ category.icon }}
                    </span>
                  </motion.div>
                </div>

                <!-- 文本 -->
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-tibet-red/10 text-tibet-red border border-tibet-red/15">
                      {{ category.prefix }}
                    </span>
                  </div>
                  <p class="text-base sm:text-lg font-semibold text-tibet-dark truncate">
                    {{ category.name }}
                  </p>
                  <p class="text-xs text-tibet-brown/45 mt-0.5">
                    {{ t('heritage.clickToExpand') }}
                  </p>
                </div>

                <motion.span
                  class="ml-auto flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-tibet-gold/25 bg-white/70 text-lg leading-none text-tibet-red"
                  :animate="activeCategory === category.name ? { rotate: 45, scale: 1.04 } : { rotate: 0, scale: 1 }"
                  :transition="softSpring"
                  aria-hidden="true"
                >
                  +
                </motion.span>
              </div>

              <!-- 卡片内部可滚动的国家级非遗项目列表 -->
              <AnimatePresence>
                <motion.div
                  v-if="activeCategory === category.name"
                  :key="category.name + '-items'"
                  layout
                  class="mt-1 w-full origin-top rounded-xl bg-tibet-white/70 border border-tibet-gold/25 px-3 py-2 max-h-44 overflow-y-auto text-xs sm:text-sm text-tibet-brown/80 space-y-2"
                  :initial="{ opacity: 0, y: -10, scaleY: 0.96 }"
                  :animate="{ opacity: 1, y: 0, scaleY: 1 }"
                  :exit="{ opacity: 0, y: -8, scaleY: 0.96 }"
                  :transition="{ duration: 0.28, ease: motionEase }"
                >
                  <p class="text-[11px] text-tibet-brown/45">
                    {{ t('heritage.nationalItems') }} · {{ getItemsByCategory(category.name).length }} {{ t('heritage.items') }}
                  </p>
                  <motion.div
                    v-for="(item, itemIndex) in getItemsByCategory(category.name)"
                    :key="item.id"
                    class="border-b border-tibet-gold/15 last:border-b-0 pb-1.5 last:pb-0"
                    :initial="{ opacity: 0, x: -8 }"
                    :animate="{ opacity: 1, x: 0 }"
                    :transition="cardTransition(itemIndex, 0.06)"
                    :whileHover="{ x: 3 }"
                  >
                    <div class="flex items-center justify-between gap-2">
                      <p class="font-medium text-tibet-dark mb-0.5 truncate">
                        {{ item.name }}
                      </p>
                      <a
                        :href="buildBaikeUrl(item.name)"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="flex-shrink-0 inline-flex items-center gap-0.5 text-[10px] text-tibet-red hover:text-tibet-brown hover:underline transition"
                        :title="t('heritage.openBaike') + ': ' + item.name"
                        @click.stop
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                        </svg>
                        {{ t('heritage.openBaike') }}
                      </a>
                    </div>
                    <p class="text-[11px] leading-snug text-tibet-brown/70 line-clamp-2">
                      {{ item.description }}
                    </p>
                  </motion.div>
                  <p
                    v-if="!getItemsByCategory(category.name).length"
                    class="text-[11px] text-stone-400"
                  >
                    {{ t('heritage.noItemsInCategory') }}
                  </p>
                </motion.div>
              </AnimatePresence>
            </motion.button>
          </div>
        </div>
      </motion.section>

      <!-- 代表性非遗项目：上来先展示几个可以点击的典型案例 -->
      <AnimatePresence mode="popLayout">
        <motion.section
          v-if="!loading && representativeItems.length"
          key="heritage-representative"
          class="mb-10 sm:mb-12"
          :initial="{ opacity: 0, y: 24 }"
          :animate="{ opacity: 1, y: 0 }"
          :exit="{ opacity: 0, y: 16 }"
          :transition="{ duration: 0.36, ease: motionEase }"
        >
          <div class="flex items-center justify-between mb-4">
            <h2 class="tibet-heading text-xl font-bold text-tibet-dark sm:text-2xl">{{ t('heritage.representativeTitle') }}</h2>
            <p class="text-sm text-tibet-brown/50 hidden md:block">
              {{ t('heritage.representativeDescription') }}
            </p>
          </div>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 sm:gap-6 lg:grid-cols-4">
            <motion.button
              v-for="(item, index) in representativeItems"
              :key="item.id"
              layout
              @click="openDetail(item)"
              class="group tibet-card-elevated rounded-2xl p-4 text-left hover:border-tibet-red/25 focus:outline-none focus:ring-2 focus:ring-tibet-gold focus:ring-offset-2 overflow-hidden sm:p-5"
              :initial="cardInitial"
              :whileInView="cardInView"
              :exit="cardExit"
              :inViewOptions="inViewOnce"
              :transition="cardTransition(index, 0.04)"
              :whileHover="{ y: -6, scale: 1.015 }"
              :whileTap="{ scale: 0.985 }"
            >
              <div class="h-36 -mx-4 -mt-4 mb-4 overflow-hidden rounded-t-2xl bg-stone-100 sm:-mx-5 sm:-mt-5">
                <img
                  :src="resolveHeritageImage(item)"
                  :alt="item.name"
                  class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                  loading="lazy"
                  @error="applyHeritageImageFallback"
                />
              </div>
              <div class="mb-3">
                <span class="inline-block px-3 py-1 rounded-full text-xs font-medium bg-tibet-red/10 text-tibet-red border border-tibet-red/15">
                  {{ item.category }}
                </span>
              </div>
              <h3 class="text-lg font-semibold text-tibet-dark mb-2 line-clamp-1">
                {{ item.name }}
              </h3>
              <p class="text-sm text-tibet-brown/70 mb-3 line-clamp-3">
                {{ item.description }}
              </p>
              <div class="flex items-center justify-between gap-2">
                <span class="inline-flex items-center text-sm font-medium text-tibet-red">
                  {{ t('heritage.viewDetails') }}
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
                <span
                  v-if="safeExternalUrl(item.baikeUrl)"
                  class="inline-flex items-center gap-0.5 text-xs text-stone-400 hover:text-red-500 transition cursor-pointer"
                  :title="t('heritage.openBaike')"
                  @click.stop="openBaikeUrl(item.baikeUrl)"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                  </svg>
                  {{ t('heritage.openBaike') }}
                </span>
              </div>
            </motion.button>
          </div>
        </motion.section>
      </AnimatePresence>

      <!-- 代表性非遗项目详情弹层（带图片与更详细介绍） -->
      <MotionModal
        :show="Boolean(selectedItem)"
        modal-key="heritage-detail-modal"
        labelled-by="heritage-detail-title"
        root-class="z-[120] px-3 sm:px-4"
        backdrop-class="bg-black/45 backdrop-blur-sm"
        panel-class="max-w-3xl rounded-2xl bg-white overflow-hidden p-0 max-h-[92dvh]"
        @close="selectedItem = null"
      >
        <template v-if="selectedItem">
          <!-- 顶部大图 -->
          <div class="relative h-44 sm:h-56 md:h-72 bg-stone-100">
            <motion.img
              :src="resolveHeritageImage(selectedItem)"
              :alt="selectedItem.name"
              class="w-full h-full object-cover"
              :initial="{ opacity: 0, scale: 1.06 }"
              :animate="{ opacity: 1, scale: 1 }"
              :transition="{ duration: 0.5, ease: motionEase }"
              @error="applyHeritageImageFallback"
            />
            <motion.button
              type="button"
              class="absolute top-3 right-3 bg-black/50 text-white p-2 rounded-full hover:bg-black/70 transition sm:top-4 sm:right-4"
              :whileHover="{ rotate: 90, scale: 1.08 }"
              :whileTap="{ scale: 0.92 }"
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
            </motion.button>
            <motion.div
              class="absolute inset-x-3 bottom-3 bg-black/45 backdrop-blur px-3 py-2 rounded-xl sm:inset-x-auto sm:left-4 sm:bottom-4 sm:px-4"
              :initial="{ opacity: 0, y: 12 }"
              :animate="{ opacity: 1, y: 0 }"
              :transition="{ duration: 0.34, delay: 0.1, ease: motionEase }"
            >
              <p class="text-xs text-red-100 font-medium mb-1">
                {{ selectedItem.category || t('heritage.representativeTitle') }}
              </p>
              <h3 id="heritage-detail-title" class="text-lg md:text-2xl font-bold text-white line-clamp-2">
                {{ selectedItem.name }}
              </h3>
            </motion.div>
          </div>

          <div
            v-if="detailAuthRequired"
            class="mx-4 mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800 sm:mx-6"
          >
            <p class="font-medium">
              {{ t('heritage.loginToViewDetail', '登录后可查看完整非遗故事、传承人、活动与评论') }}
            </p>
            <router-link
              to="/login"
              class="mt-2 inline-flex rounded-lg bg-tibet-red px-3 py-1.5 text-xs font-medium text-white hover:bg-tibet-red/90"
            >
              {{ t('common.login', '登录') }}
            </router-link>
          </div>

          <!-- 互动状态栏：浏览数 / 点赞 / 评论 -->
          <div v-if="selectedItem.id < 10000" class="px-4 py-3 flex flex-wrap items-center justify-between gap-3 border-b border-stone-100 sm:px-6">
            <div class="flex min-w-0 flex-wrap items-center gap-3 text-xs text-stone-400 sm:gap-4">
              <span v-if="selectedItem.viewCount" class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                {{ selectedItem.viewCount }}
              </span>
              <span class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
                {{ selectedItem.likeCount || 0 }}
              </span>
              <span class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
                {{ selectedItem.commentCount || 0 }}
              </span>
              <span v-if="selectedItem.protectionLevel" class="px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-100 text-[11px] font-medium">
                {{ selectedItem.protectionLevel }}
              </span>
            </div>
            <button
              v-if="authStore.isLoggedIn"
              class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-medium transition"
              :class="liked ? 'bg-red-50 text-red-600 border border-red-200' : 'bg-stone-50 text-stone-500 border border-stone-200 hover:bg-red-50 hover:text-red-500'"
              @click="toggleLike"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" :fill="liked ? 'currentColor' : 'none'" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" /></svg>
              {{ liked ? '已点赞' : '点赞' }}
            </button>
          </div>

          <!-- 文字内容区：分段更详细介绍 + 线下体验模块 -->
          <motion.div
            class="px-4 py-4 text-sm text-stone-700 space-y-5 max-h-[calc(92dvh-11rem)] overflow-y-auto sm:px-6 sm:py-5 sm:max-h-[65vh]"
            :initial="{ opacity: 0, y: 14 }"
            :animate="{ opacity: 1, y: 0 }"
            :transition="{ duration: 0.34, delay: 0.12, ease: motionEase }"
          >
            <motion.div
              class="space-y-4"
              :initial="{ opacity: 0, y: 10 }"
              :animate="{ opacity: 1, y: 0 }"
              :transition="{ duration: 0.28, delay: 0.18, ease: motionEase }"
            >
              <!-- 百度百科跳转按钮 -->
              <a
                v-if="safeExternalUrl(selectedItem.baikeUrl)"
                :href="safeExternalUrl(selectedItem.baikeUrl)"
                target="_blank"
                rel="noopener noreferrer"
                class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-600 hover:text-white hover:border-blue-600 transition font-medium text-sm shadow-sm"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                </svg>
                {{ t('heritage.viewOnBaike') }}
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </a>
              <p class="text-[13px] text-stone-500">
                {{ t('heritage.detailNote') }}
              </p>

              <!-- 视频播放器 -->
              <div v-if="selectedItem.videoUrl" class="rounded-xl overflow-hidden border border-stone-200 bg-black">
                <video
                  :src="selectedItem.videoUrl"
                  controls
                  preload="metadata"
                  class="w-full max-h-[320px]"
                >
                  您的浏览器不支持视频播放
                </video>
              </div>

              <motion.div
                class="space-y-2"
                :initial="{ opacity: 0, y: 8 }"
                :animate="{ opacity: 1, y: 0 }"
                :transition="{ duration: 0.26, delay: 0.22, ease: motionEase }"
              >
                <h4 class="text-sm font-semibold text-stone-900">
                  {{ t('heritage.basicIntroduction') }}
                </h4>
                <p class="leading-relaxed whitespace-pre-line">
                  {{ selectedItem.description || t('heritage.noDetailedDescription') }}
                </p>
              </motion.div>

              <motion.div
                v-if="selectedItem.originStory"
                class="space-y-2"
                :initial="{ opacity: 0, y: 8 }"
                :animate="{ opacity: 1, y: 0 }"
                :transition="{ duration: 0.26, delay: 0.27, ease: motionEase }"
              >
                <h4 class="text-sm font-semibold text-stone-900">
                  {{ t('heritage.originStory') }}
                </h4>
                <p class="leading-relaxed whitespace-pre-line">
                  {{ selectedItem.originStory }}
                </p>
              </motion.div>

              <motion.div
                v-if="selectedItem.significance"
                class="space-y-2 border-t border-dashed border-stone-200 pt-3"
                :initial="{ opacity: 0, y: 8 }"
                :animate="{ opacity: 1, y: 0 }"
                :transition="{ duration: 0.26, delay: 0.32, ease: motionEase }"
              >
                <h4 class="text-sm font-semibold text-stone-900">
                  {{ t('heritage.culturalValue') }}
                </h4>
                <p class="text-stone-700 text-sm leading-relaxed whitespace-pre-line">
                  {{ selectedItem.significance }}
                </p>
              </motion.div>

              <p
                v-if="!selectedItem.originStory && !selectedItem.significance"
                class="text-xs text-stone-400 border-t border-dashed border-stone-200 pt-3"
              >
                {{ t('heritage.basicDescriptionNote') }}
              </p>
            </motion.div>

            <!-- 线下体验模块：地图示意 + 门店列表 + 导航 -->
            <motion.div
              v-if="!detailAuthRequired"
              class="border-t border-dashed border-stone-200 pt-4"
              :initial="{ opacity: 0, y: 12 }"
              :animate="{ opacity: 1, y: 0 }"
              :transition="{ duration: 0.3, delay: 0.28, ease: motionEase }"
            >
              <div class="flex flex-col gap-2 mb-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h4 class="text-sm font-semibold text-stone-900">
                    {{ t('heritage.offlineExperience') }}
                  </h4>
                  <p class="text-[12px] text-stone-500 mt-0.5">
                    {{ t('heritage.experienceDescription', { name: selectedItem.name }) }}
                  </p>
                </div>
                <span class="inline-flex w-fit items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-red-50 text-red-700 border border-red-100">
                  {{ t('heritage.totalExperienceSpots', { count: experienceSpots.length }) }}
                </span>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-5 gap-4">
                <!-- 交互式地图 -->
                <div class="md:col-span-2 relative rounded-xl overflow-hidden border border-stone-200 min-h-[200px] bg-stone-100 sm:min-h-[220px]">
                  <div ref="mapContainer" class="w-full h-full min-h-[200px] sm:min-h-[220px]"></div>
                  <!-- 地图加载提示 -->
                  <AnimatePresence>
                    <!-- 地图加载提示 -->
                    <motion.div
                      v-if="mapLoading && !mapError"
                      key="heritage-map-loading"
                      class="absolute inset-0 flex items-center justify-center bg-stone-100/90 backdrop-blur-sm z-20"
                      :initial="{ opacity: 0 }"
                      :animate="{ opacity: 1 }"
                      :exit="{ opacity: 0 }"
                      :transition="{ duration: 0.22, ease: motionEase }"
                    >
                      <div class="text-center">
                        <motion.div
                          class="rounded-full h-8 w-8 border-b-2 border-red-500 mx-auto mb-2"
                          :animate="{ rotate: 360 }"
                          :transition="{ duration: 1, repeat: Infinity, ease: 'linear' }"
                        ></motion.div>
                        <p class="text-xs text-stone-600">{{ t('heritage.mapLoading') }}</p>
                      </div>
                    </motion.div>
                    <!-- 地图加载失败提示 -->
                    <motion.div
                      v-if="mapError"
                      key="heritage-map-error"
                      class="absolute inset-0 flex items-center justify-center bg-stone-100/90 backdrop-blur-sm z-20"
                      :initial="{ opacity: 0, scale: 0.98 }"
                      :animate="{ opacity: 1, scale: 1 }"
                      :exit="{ opacity: 0, scale: 0.98 }"
                      :transition="{ duration: 0.22, ease: motionEase }"
                    >
                      <div class="text-center px-4">
                        <p class="text-xs text-stone-600 mb-2">{{ t('heritage.mapLoadFailed') }}</p>
                        <p class="text-xs text-stone-500">{{ t('heritage.checkRightList') }}</p>
                      </div>
                    </motion.div>
                  </AnimatePresence>
                  <!-- 地图标题覆盖层 -->
                  <div class="absolute top-0 left-0 right-0 p-3 bg-gradient-to-b from-black/40 to-transparent z-10 pointer-events-none">
                    <p class="text-[11px] font-medium uppercase tracking-widest text-amber-200">
                      Tibet Experience Map
                    </p>
                    <p class="text-sm font-semibold text-white">
                      {{ t('heritage.experienceSpotsDistribution') }}
                    </p>
                  </div>
                  <!-- 图例 -->
                  <div class="absolute bottom-0 left-0 right-0 p-3 bg-gradient-to-t from-black/40 to-transparent z-10 pointer-events-none">
                    <div class="flex flex-wrap items-center gap-2 text-[11px]">
                      <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-black/35 backdrop-blur text-white">
                        <span class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-300"></span>
                        {{ t('heritage.experienceShop') }}
                      </span>
                      <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-black/35 backdrop-blur text-white">
                        <span class="inline-block w-1.5 h-1.5 rounded-full bg-amber-300"></span>
                        {{ t('heritage.culturalSpace') }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 门店列表 -->
                <div class="md:col-span-3 space-y-3 max-h-[260px] overflow-y-auto pr-1 md:max-h-[220px]">
                  <motion.div
                    v-for="(spot, index) in experienceSpots"
                    :key="spot.name"
                    class="flex flex-col gap-3 rounded-xl border border-stone-200 bg-stone-50/60 px-3 py-2.5 hover:bg-white hover:border-red-200 transition sm:flex-row sm:items-start sm:justify-between"
                    :initial="{ opacity: 0, x: 16 }"
                    :animate="{ opacity: 1, x: 0 }"
                    :transition="cardTransition(index, 0.18)"
                    :whileHover="{ x: 4, scale: 1.01 }"
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
                    <div class="flex flex-wrap items-center gap-2 sm:flex-col sm:items-end sm:gap-1">
                      <a
                        class="inline-flex items-center px-2.5 py-1 rounded-full text-[11px] font-medium bg-red-50 text-red-700 border border-red-100 hover:bg-red-600 hover:text-white hover:border-red-600 transition"
                        :href="buildNavUrl(spot)"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {{ t('heritage.navigate') }}
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
                        {{ t('heritage.checkIn') }} {{ spot.highlight }}
                      </span>
                    </div>
                  </motion.div>
                </div>
              </div>
            </motion.div>

            <!-- 传承人 -->
            <div v-if="!detailAuthRequired && itemInheritors.length" class="border-t border-dashed border-stone-200 pt-4">
              <h4 class="text-sm font-semibold text-stone-900 mb-3">
                {{ t('heritage.inheritors', '代表性传承人') }}
              </h4>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div
                  v-for="inheritor in itemInheritors"
                  :key="inheritor.id"
                  class="flex items-start gap-3 rounded-xl border border-stone-200 bg-stone-50/60 px-3 py-2.5"
                >
                  <div v-if="inheritor.avatarUrl" class="flex-shrink-0 w-10 h-10 rounded-full overflow-hidden bg-stone-200">
                    <img :src="inheritor.avatarUrl" :alt="inheritor.name" class="w-full h-full object-cover" />
                  </div>
                  <div v-else class="flex-shrink-0 w-10 h-10 rounded-full bg-tibet-red/10 flex items-center justify-center text-tibet-red font-bold text-sm">
                    {{ inheritor.name?.charAt(0) }}
                  </div>
                  <div class="min-w-0 flex-1">
                    <div class="flex items-center gap-2 mb-0.5">
                      <p class="text-sm font-semibold text-stone-900">{{ inheritor.name }}</p>
                      <span v-if="inheritor.level" class="px-1.5 py-0.5 rounded text-[10px] bg-amber-50 text-amber-700 border border-amber-100">{{ inheritor.level }}</span>
                    </div>
                    <p v-if="inheritor.region" class="text-[11px] text-stone-400 mb-0.5">{{ inheritor.region }}</p>
                    <p v-if="inheritor.bio" class="text-xs text-stone-500 line-clamp-2">{{ inheritor.bio }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 相关活动 -->
            <div v-if="!detailAuthRequired && itemEvents.length" class="border-t border-dashed border-stone-200 pt-4">
              <h4 class="text-sm font-semibold text-stone-900 mb-3">
                {{ t('heritage.relatedEvents', '相关活动') }}
              </h4>
              <div class="space-y-2">
                <div
                  v-for="event in itemEvents"
                  :key="event.id"
                  class="flex items-center gap-3 rounded-xl border border-stone-200 bg-stone-50/60 px-3 py-2"
                >
                  <div class="flex-shrink-0 w-12 text-center">
                    <p class="text-xs font-bold text-tibet-red">{{ formatDate(event.eventDate || '') }}</p>
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="text-sm font-medium text-stone-900">{{ event.title }}</p>
                    <p v-if="event.location" class="text-[11px] text-stone-400">{{ event.location }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 评论区 -->
            <div v-if="selectedItem.id < 10000 && !detailAuthRequired" class="border-t border-dashed border-stone-200 pt-4">
              <h4 class="text-sm font-semibold text-stone-900 mb-3">
                {{ t('heritage.commentsTitle', '用户评论') }} ({{ selectedItem.commentCount || 0 }})
              </h4>

              <!-- 发表评论 -->
              <div v-if="authStore.isLoggedIn" class="mb-4 space-y-2">
                <div class="flex items-center gap-1 mb-1">
                  <span class="text-xs text-stone-500">{{ t('heritage.rating', '评分') }}:</span>
                  <button
                    v-for="star in 5"
                    :key="star"
                    type="button"
                    class="text-lg leading-none transition"
                    :class="star <= newCommentRating ? 'text-amber-400' : 'text-stone-200'"
                    @click="newCommentRating = star"
                  >
                    ★
                  </button>
                </div>
                <div class="flex flex-col gap-2 sm:flex-row">
                  <input
                    v-model="newCommentContent"
                    type="text"
                    maxlength="1000"
                    :placeholder="t('heritage.commentPlaceholder', '分享你对这个非遗项目的感受...')"
                    class="flex-1 px-3 py-2 rounded-lg border border-stone-200 text-sm focus:outline-none focus:ring-1 focus:ring-tibet-red/30"
                    @keydown.enter.prevent="submitComment"
                  />
                  <button
                    type="button"
                    class="mobile-touch-target px-4 py-2 rounded-lg bg-tibet-red text-white text-sm font-medium hover:bg-tibet-red/90 transition disabled:opacity-50"
                    :disabled="submittingComment || !newCommentContent.trim()"
                    @click="submitComment"
                  >
                    {{ submittingComment ? '...' : t('heritage.submitComment', '发表') }}
                  </button>
                </div>
              </div>
              <p v-else class="text-xs text-stone-400 mb-3">
                {{ t('heritage.loginToComment', '登录后可以评论') }}
              </p>

              <!-- 评论列表 -->
              <div v-if="commentsLoading" class="flex justify-center py-4">
                <div class="rounded-full h-6 w-6 border-b-2 border-red-500 animate-spin"></div>
              </div>
              <div v-else-if="itemComments.length" class="space-y-3">
                <div
                  v-for="comment in itemComments"
                  :key="comment.id"
                  class="rounded-lg border border-stone-100 bg-stone-50/40 px-3 py-2"
                >
                  <div class="flex flex-col gap-1 mb-1 sm:flex-row sm:items-center sm:justify-between">
                    <div class="flex min-w-0 flex-wrap items-center gap-2">
                      <div class="w-6 h-6 rounded-full bg-tibet-red/10 flex items-center justify-center text-tibet-red text-[10px] font-bold overflow-hidden">
                        <img v-if="comment.avatar" :src="comment.avatar" class="w-full h-full object-cover" />
                        <span v-else>{{ (comment.nickname || comment.username)?.charAt(0) }}</span>
                      </div>
                      <span class="text-xs font-medium text-stone-700">{{ comment.nickname || comment.username }}</span>
                      <span v-if="comment.rating" class="text-[11px] text-amber-500">
                        {{ '★'.repeat(comment.rating) }}
                      </span>
                    </div>
                    <div class="flex items-center gap-2">
                      <span class="text-[10px] text-stone-400">{{ formatDate(comment.createdAt) }}</span>
                      <button
                        v-if="authStore.user?.id === comment.userId"
                        type="button"
                        class="text-[10px] text-red-400 hover:text-red-600 transition"
                        @click="deleteComment(comment.id)"
                      >
                        {{ t('heritage.deleteComment', '删除') }}
                      </button>
                    </div>
                  </div>
                  <p class="text-sm text-stone-600 leading-relaxed">{{ comment.content }}</p>
                </div>
              </div>
              <p v-else class="text-xs text-stone-400 text-center py-3">
                {{ t('heritage.noComments', '暂无评论，快来发表第一条吧') }}
              </p>
            </div>
          </motion.div>
        </template>
      </MotionModal>

      <AnimatePresence>
        <motion.div
          v-if="loading"
          key="heritage-loading"
          class="flex justify-center items-center h-64"
          :initial="{ opacity: 0 }"
          :animate="{ opacity: 1 }"
          :exit="{ opacity: 0 }"
          :transition="{ duration: 0.22, ease: motionEase }"
        >
          <motion.div
            class="rounded-full h-12 w-12 border-b-2 border-red-600"
            :animate="{ rotate: 360 }"
            :transition="{ duration: 1, repeat: Infinity, ease: 'linear' }"
          ></motion.div>
        </motion.div>
      </AnimatePresence>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { AnimatePresence, motion } from 'motion-v'
import { useI18n } from 'vue-i18n'
import {
  ArrowUpRight,
  BookOpen,
  CalendarDays,
  Eye,
  Heart,
  MessageCircle,
  RefreshCw,
  Search,
  SlidersHorizontal,
  UserRound,
  Video
} from 'lucide-vue-next'
import MotionModal from '../components/motion/MotionModal.vue'
import api, { endpoints } from '../api'
import type {
  HeritageCommentItem,
  HeritageEventItem,
  HeritageInheritorItem,
  HeritageItem
} from '../api'
import { useAuthStore } from '../stores/auth'
import type * as Leaflet from 'leaflet'
import {
  cardExit,
  cardInitial,
  cardInView,
  cardTransition,
  inViewOnce,
  motionEase,
  revealInitial,
  revealInView,
  revealTransition,
  softSpring
} from '../motion/presets'

const { t, locale } = useI18n()

const heritageCategories = computed(() => [
  {
    name: t('heritage.category.folkLiterature'),
    prefix: t('heritage.category.folkLiterature'),
    icon: '📜',
    layout: 'lg:-mt-4 lg:ml-4 z-20'
  },
  {
    name: t('heritage.category.traditionalMusic'),
    prefix: t('heritage.category.traditionalMusic'),
    icon: '🥁',
    layout: 'lg:mt-8 z-30'
  },
  {
    name: t('heritage.category.traditionalDance'),
    prefix: t('heritage.category.traditionalDance'),
    icon: '💃',
    layout: 'lg:-mt-10 lg:-mr-4 z-40'
  },
  {
    name: t('heritage.category.traditionalDrama'),
    prefix: t('heritage.category.traditionalDrama'),
    icon: '🎭',
    layout: 'lg:-mt-2 lg:ml-8 z-30'
  },
  {
    name: t('heritage.category.traditionalSports'),
    prefix: t('heritage.category.traditionalSports'),
    icon: '🏹',
    layout: 'lg:mt-10 z-20'
  },
  {
    name: t('heritage.category.traditionalCraft'),
    prefix: t('heritage.category.traditionalCraft'),
    icon: '🧶',
    layout: 'lg:-mt-6 lg:-mr-6 z-30'
  },
  {
    name: t('heritage.category.traditionalMedicine'),
    prefix: t('heritage.category.traditionalMedicine'),
    icon: '🌿',
    layout: 'lg:mt-6 z-20'
  },
  {
    name: t('heritage.category.folkCustom'),
    prefix: t('heritage.category.folkCustom'),
    icon: '🏔️',
    layout: 'lg:-mt-4 lg:mr-4 z-20'
  }
])

const authStore = useAuthStore()

const buildBaikeUrl = (name: string): string => {
  return 'https://baike.baidu.com/search?word=' + encodeURIComponent(name)
}

const safeExternalUrl = (value?: string | null): string => {
  if (!value) return ''
  try {
    const url = new URL(value)
    return ['https:', 'http:'].includes(url.protocol) ? url.href : ''
  } catch {
    return ''
  }
}

const heritageItems = ref<HeritageItem[]>([])
const loading = ref(true)
const selectedItem = ref<HeritageItem | null>(null)

const searchKeyword = ref('')
const searchLoading = ref(false)

const itemComments = ref<HeritageCommentItem[]>([])
const commentsLoading = ref(false)
const itemInheritors = ref<HeritageInheritorItem[]>([])
const itemEvents = ref<HeritageEventItem[]>([])
const detailAuthRequired = ref(false)
const liked = ref(false)
const newCommentContent = ref('')
const newCommentRating = ref(5)
const submittingComment = ref(false)
const upcomingEvents = ref<HeritageEventItem[]>([])
const featuredInheritors = ref<HeritageInheritorItem[]>([])
const featuredInheritorsLoading = ref(false)
const selectedCategory = ref('all')

type HeritageSortMode = 'hot' | 'views' | 'likes' | 'comments' | 'latest' | 'name'

const sortMode = ref<HeritageSortMode>('hot')

const genericHeritageImagePatterns = [
  'images.unsplash.com/photo-1559827291'
]

const heritageImageByName: Record<string, string> = {
  格萨尔史诗: '/heritage/格萨尔史诗.jpg',
  格萨尔: '/heritage/格萨尔史诗.jpg',
  藏戏: '/heritage/藏戏.jpg',
  藏族唐卡: '/heritage/唐卡.jpg',
  唐卡: '/heritage/唐卡.jpg',
  藏医药浴法: '/heritage/藏药.jpg',
  藏药: '/heritage/藏药.jpg',
  拉萨囊玛: '/heritage/拉萨囊玛.jpg',
  拉萨朗玛: '/heritage/拉萨囊玛.jpg',
  囊玛: '/heritage/拉萨囊玛.jpg',
  那曲山歌: '/heritage/那曲山歌.jpeg',
  藏族山歌: '/heritage/那曲山歌.jpeg',
  藏北民歌: '/heritage/那曲山歌.jpeg',
  热巴舞: '/heritage/热巴舞.jpg',
  锅庄舞: '/heritage/锅庄舞.jpg',
  弦子舞: '/heritage/弦子舞.jpg',
  门巴戏: '/heritage/门巴戏.jpg',
  藏族传统马术: '/heritage/藏族传统马术.jpg',
  马术: '/heritage/藏族传统马术.jpg',
  藏香制作技艺: '/heritage/藏香制作技艺.jpg',
  藏香: '/heritage/藏香制作技艺.jpg',
  藏刀锻制技艺: '/heritage/藏刀锻制技艺.jpg',
  藏刀: '/heritage/藏刀锻制技艺.jpg',
  '藏族邦典/卡垫织造技艺': '/heritage/藏族邦典卡垫织造技艺.jpg',
  藏族邦典卡垫织造技艺: '/heritage/藏族邦典卡垫织造技艺.jpg',
  邦典: '/heritage/藏族邦典卡垫织造技艺.jpg',
  卡垫: '/heritage/藏族邦典卡垫织造技艺.jpg',
  藏族雕版印刷技艺: '/heritage/藏族雕版印刷技艺.jpg',
  雕版印刷: '/heritage/藏族雕版印刷技艺.jpg',
  藏族造纸技艺: '/heritage/藏族造纸技艺.jpg',
  藏纸: '/heritage/藏族造纸技艺.jpg',
  雪顿节: '/heritage/雪顿节.jpg',
  望果节: '/heritage/望果节.jpg',
  藏族金属锻造技艺: '/heritage/藏族金属锻造技艺.jpg',
  金属锻造: '/heritage/藏族金属锻造技艺.jpg',
  墨脱石锅制作技艺: '/heritage/墨脱石锅制作技艺.jpg',
  墨脱石锅: '/heritage/墨脱石锅制作技艺.jpg',
  羌姆: '/heritage/羌姆.jpg',
  'གེ་སར': '/heritage/格萨尔史诗.jpg',
  'བོད་ཟློས་གར': '/heritage/藏戏.jpg',
  'ཐང་ཀ': '/heritage/唐卡.jpg',
  'བོད་སྨན': '/heritage/藏药.jpg'
}

const isGenericHeritageImage = (imageUrl?: string | null): boolean =>
  !imageUrl || genericHeritageImagePatterns.some(pattern => imageUrl.includes(pattern))

const findMappedHeritageImage = (name?: string | null): string => {
  if (!name) return ''
  const normalizedName = name.replace(/[（）()《》“”"·/、\s]/g, '')
  const direct = heritageImageByName[name] || heritageImageByName[normalizedName]
  if (direct) return direct

  const match = Object.entries(heritageImageByName).find(([key]) => {
    const normalizedKey = key.replace(/[（）()《》“”"·/、\s]/g, '')
    return normalizedName.includes(normalizedKey) || normalizedKey.includes(normalizedName)
  })
  return match?.[1] || ''
}

const resolveHeritageImage = (item?: HeritageItem | null): string => {
  const mapped = findMappedHeritageImage(item?.name)
  if (mapped) return mapped

  const imageUrl = item?.imageUrl?.trim()
  return isGenericHeritageImage(imageUrl) ? '' : (imageUrl || '')
}

const applyHeritageImageFallback = (event: Event): void => {
  const image = event.target as HTMLImageElement
  image.style.display = 'none'
}

// 地图相关
const mapContainer = ref<HTMLElement | null>(null)
const mapLoading = ref(true)
const mapError = ref(false) // 地图加载失败标志
let map: Leaflet.Map | null = null
let markers: Leaflet.Layer[] = []
let pendingMapFrame: number | null = null
let mapInitVersion = 0
let leafletLoader: Promise<typeof Leaflet> | null = null

const loadLeaflet = async () => {
  if (!leafletLoader) {
    leafletLoader = Promise.all([
      import('leaflet'),
      import('leaflet/dist/leaflet.css')
    ]).then(([leaflet]) => leaflet)
  }

  return leafletLoader
}

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
const experienceSpotsZh: ExperienceSpot[] = [
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

const experienceSpotsBo: ExperienceSpot[] = [
  {
    name: 'ལྷ་ས་རིག་གནས་ཤུལ་བཞག་ཉམས་མྱོང་ལྟེ་གནས། བར་སྐོར།',
    city: 'ལྷ་ས།',
    address: 'ལྷ་ས་ཁྲེང་ཀོན་ཆུས། བར་སྐོར་རྐང་ཐང་ལམ་ཉེ་འགྲམ།',
    lat: 29.653,
    lng: 91.117,
    tag: 'བོད།',
    brief: 'བོད་ཟློས་གར། ཐང་ཀ བོད་སྤོས་སོགས་རིག་གནས་ཤུལ་བཞག་ཉམས་མྱོང་ས་ཚིགས།',
    highlight: 'ས་ཚིགས་གཅིག་ཏུ་རིག་གནས་ཤུལ་བཞག་མང་པོ་ཉམས་མྱོང་།'
  },
  {
    name: 'ནོར་བུ་གླིང་ཁ་ཐང་ཀའི་ལས་ཁང་།',
    city: 'ལྷ་ས།',
    address: 'ལྷ་ས་ཁྲེང་ཀོན་ཆུས། ནོར་བུ་གླིང་ཁ་ཉེ་འགྲམ་གྱི་ལག་ཤེས་སྲང་ལམ།',
    lat: 29.642,
    lng: 91.071,
    tag: 'རིས།',
    brief: 'ཐང་ཀ་འབྲི་བ་དང་བཤམས་སྟོན་ལ་ཆེད་དུ་གནས་པའི་ལས་ཁང་།',
    highlight: 'རང་གིས་ཐང་ཀའི་རི་མོ་དམར་པོ་ཞིག་ཚོད་འབྲི།'
  },
  {
    name: 'གཞིས་ཀ་རྩེ་བོད་ཟློས་གར་སྦྱོང་བརྡར་ལྟེ་གནས།',
    city: 'གཞིས་ཀ་རྩེ།',
    address: 'གཞིས་ཀ་རྩེ་བསམ་འགྲུབ་རྩེ་ཆུས། རིག་གནས་སྲང་ལམ་ནང་།',
    lat: 29.268,
    lng: 88.882,
    tag: 'ཟློས།',
    brief: 'བོད་ཟློས་གར་སྦྱོང་བརྡར་དང་འཁྲབ་སྟོན་བྱེད་པའི་ས་ཚིགས།',
    highlight: 'ཉེ་ས་ནས་བོད་ཟློས་གར་ཆ་ཚང་ཞིག་ལྟ།'
  },
  {
    name: 'ཉིང་ཁྲི་བོད་སྨན་རིག་གནས་ཉམས་མྱོང་ཁང་།',
    city: 'ཉིང་ཁྲི།',
    address: 'ཉིང་ཁྲི་བ་ཡི་ཆུས། བོད་སྨན་དང་བདེ་ཐང་རིག་གནས་ས་ཁུལ།',
    lat: 29.654,
    lng: 94.362,
    tag: 'སྨན།',
    brief: 'བོད་སྨན་བཤམས་སྟོན། འགྲེལ་བཤད། སྟབས་བདེའི་གསོ་རིག་ཉམས་མྱོང་ཟུང་འབྲེལ།',
    highlight: 'བོད་སྨན་རྒྱུ་ཆ་དང་སྲོལ་རྒྱུན་བདེ་ཐང་ཐབས་ལམ་ཤེས་པ།'
  }
]

const experienceSpots = computed(() => locale.value === 'bo' ? experienceSpotsBo : experienceSpotsZh)

// 代表性项目中要固定展示的几个核心项目
const representativeItems = computed<HeritageItem[]>(() => {
  const extraZh: HeritageItem[] = [
    {
      id: 10001,
      name: '藏药',
      description: '源自雪域高原的传统医学体系，吸收了藏族本土经验与印度、汉地医学精华，以丸、散、膏、丹等剂型闻名。',
      category: t('heritage.category.traditionalMedicine'),
      imageUrl: '/heritage/藏药.jpg',
      videoUrl: '',
      originStory: '',
      significance: '体现了藏族人民与高原自然环境长期博弈中形成的健康智慧，是中华传统医学宝库的重要组成部分。',
      baikeUrl: 'https://baike.baidu.com/item/%E8%97%8F%E5%8C%BB%E8%8D%AF%E6%B5%B4%E6%B3%95'
    },
    {
      id: 10002,
      name: '格萨尔史诗',
      description: '被誉为”世界上最长的史诗”，通过艺人口耳相传、即兴说唱的方式一代代流传下来。',
      category: '民间文学',
      imageUrl: '/heritage/格萨尔史诗.jpg',
      videoUrl: '',
      originStory: '',
      significance: '记录了藏族社会的历史记忆、英雄理想与价值观，是中华民族口头传统中的璀璨明珠。',
      baikeUrl: 'https://baike.baidu.com/item/%E6%A0%BC%E8%90%A8%E5%B0%94%E7%8E%8B%E4%BC%A0'
    },
    {
      id: 10003,
      name: '藏戏',
      description: '被誉为”藏文化的活化石”，集歌舞、说唱、表演于一体，常在寺院法会和民间节日中演出。',
      category: '传统戏剧',
      imageUrl: '/heritage/藏戏.jpg',
      videoUrl: '',
      originStory: '',
      significance: '藏戏综合了宗教仪式、历史故事与民间传说，是研究藏族社会生活与信仰体系的重要窗口。',
      baikeUrl: 'https://baike.baidu.com/item/%E8%97%8F%E6%88%8F'
    },
    {
      id: 10004,
      name: '藏族唐卡',
      description: '以矿物颜料在布、纸或丝绸上绘制的宗教卷轴画，色彩瑰丽、构图严谨，多悬挂于寺院与居室。',
      category: '传统技艺',
      imageUrl: '/heritage/唐卡.jpg',
      videoUrl: '',
      originStory: '',
      significance: '唐卡承载着藏传佛教教义、历史人物与宇宙观，被视为”可以卷起来带走的宫殿壁画”，是西藏艺术的代表符号之一。',
      baikeUrl: 'https://baike.baidu.com/item/%E5%94%90%E5%8D%A1'
    }
  ]
  const extraBo: HeritageItem[] = [
    {
      id: 10001,
      name: 'བོད་སྨན།',
      description: 'གངས་ལྗོངས་ས་མཐོ་ནས་བྱུང་བའི་སྲོལ་རྒྱུན་སྨན་རིག་མ་ལག བོད་མིའི་ཉམས་མྱོང་དང་རྒྱ་གར། རྒྱ་ནག་སྨན་རིག་གི་སྙིང་པོ་བསྡུས་ཡོད།',
      category: t('heritage.category.traditionalMedicine'),
      imageUrl: '/heritage/藏药.jpg',
      videoUrl: '',
      originStory: '',
      significance: 'བོད་མིས་ས་མཐོའི་རང་བྱུང་ཁོར་ཡུག་དང་མཉམ་འཚོའི་ནང་གྲུབ་པའི་བདེ་ཐང་ཤེས་རབ་མཚོན།',
      baikeUrl: 'https://baike.baidu.com/item/%E8%97%8F%E5%8C%BB%E8%8D%AF%E6%B5%B4%E6%B3%95'
    },
    {
      id: 10002,
      name: 'གེ་སར་སྒྲུང་།',
      description: '“འཛམ་གླིང་གི་སྒྲུང་རིང་ཤོས” ཞེས་གྲགས། སྒྲུང་མཁན་གྱི་ཁ་བརྒྱུད་དང་རང་བྱུང་གླུ་སྒྲུང་གིས་མི་རབས་ནས་མི་རབས་སུ་བརྒྱུད།',
      category: t('heritage.category.folkLiterature'),
      imageUrl: '/heritage/格萨尔史诗.jpg',
      videoUrl: '',
      originStory: '',
      significance: 'བོད་མིའི་ལོ་རྒྱུས་དྲན་ཤེས། དཔའ་བོའི་ཕུགས་བསམ། རིན་ཐང་ལྟ་ཚུལ་བཅས་ཟིན་ཐོར་བཀོད་ཡོད།',
      baikeUrl: 'https://baike.baidu.com/item/%E6%A0%BC%E8%90%A8%E5%B0%94%E7%8E%8B%E4%BC%A0'
    },
    {
      id: 10003,
      name: 'བོད་ཟློས་གར།',
      description: '“བོད་རིག་གནས་ཀྱི་གསོན་པོའི་དངོས་རྫས” ཞེས་གྲགས། གླུ་གར། གཏམ་བཤད། འཁྲབ་སྟོན་བཅས་ཟུང་འབྲེལ་གྱི་སྲོལ་རྒྱུན་ཟློས་གར།',
      category: t('heritage.category.traditionalDrama'),
      imageUrl: '/heritage/藏戏.jpg',
      videoUrl: '',
      originStory: '',
      significance: 'ཆོས་ལུགས་ཆོ་ག ལོ་རྒྱུས་སྒྲུང་། མི་དམངས་གཏམ་རྒྱུད་བཅས་མཉམ་སྡེབ་བྱས་པའི་རིག་གནས་སྒེའུ་ཁུང་།',
      baikeUrl: 'https://baike.baidu.com/item/%E8%97%8F%E6%88%8F'
    },
    {
      id: 10004,
      name: 'བོད་རིགས་ཐང་ཀ',
      description: 'རྡོ་སྨན་ཚོན་རྫས་ཀྱིས་རས། ཤོག་བུའམ་དར་རས་ཐོག་ཏུ་འབྲི་བའི་ཆོས་ལུགས་རི་མོ། ཚོན་མདོག་བཀྲ་ཤིས་ཤིང་གྲུབ་ཚུལ་ནན་ཏན་ཡིན།',
      category: t('heritage.category.traditionalCraft'),
      imageUrl: '/heritage/唐卡.jpg',
      videoUrl: '',
      originStory: '',
      significance: 'ཐང་ཀས་བོད་བརྒྱུད་ནང་བསྟན་གྱི་ཆོས་དོན། ལོ་རྒྱུས་མི་སྣ། འཇིག་རྟེན་ལྟ་ཚུལ་བཅས་འཁུར་ཡོད།',
      baikeUrl: 'https://baike.baidu.com/item/%E5%94%90%E5%8D%A1'
    }
  ]

  const extra = locale.value === 'bo' ? extraBo : extraZh

  // 为了避免和后台数据重复，先把与手动固定项目同名的条目从后台列表中排除
  const extraNames = new Set(extra.map(item => item.name))

  const base = heritageItems.value
    .map(item => ({
      ...item,
      imageUrl: resolveHeritageImage(item)
    }))
    .filter(item => {
      // 1. 排除与固定项目同名的条目
      if (extraNames.has(item.name)) return false
      // 2. 特殊处理：后台里叫“唐卡”，前端固定用“藏族唐卡”，这里直接去掉后台的“唐卡”
      if (item.name === '唐卡' && extraNames.has('藏族唐卡')) return false
      // 3. 代表卡片只收录能映射到明确本地图片的条目，避免泛图或断链图混入
      if (!item.imageUrl) return false
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

// 结合本模块已收录内容与国家级非遗类别，保留明确属于非遗的项目。
const nationalHeritageRaw = `西藏国家级非物质文化遗产项目导览
西藏自治区拥有丰富的国家级非物质文化遗产代表性项目，其中格萨尔、藏戏、藏医药浴法等项目还被列入联合国教科文组织人类非物质文化遗产代表作名录。以下围绕本模块已收录内容与常见国家级项目，按八类整理，避免把普通景点或泛文化内容混入非遗名录：
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
藏族传统榨油技艺（江孜传统榨油技艺）：日喀则市江孜县传统榨油工艺，使用传统的木制榨油设备
七、传统医药类 (1 项)
藏医药浴法（藏医药浴疗法）：以藏医学理论为基础，结合雪域高原药材、温泉资源和浴疗经验形成的外治疗法，2018 年入选联合国教科文组织人类非遗代表作名录
八、民俗类 (2 项)
雪顿节（第一批，2006 年）：以展佛、藏戏汇演、民俗游艺等活动为核心的藏族重要节庆
望果节（第四批，2014 年）：西藏农区秋收前后举行的农耕民俗节日，以绕田巡游、祈愿丰收、歌舞竞技等活动传承乡土共同体记忆`

const getTibetanNationalHeritageItems = (): NationalHeritageItem[] => [
  {
    id: 1,
    category: t('heritage.category.folkLiterature'),
    name: 'གེ་སར།',
    description: 'འཛམ་གླིང་གི་སྒྲུང་རིང་ཤོས་སུ་གྲགས་པའི་བོད་ཀྱི་དཔའ་བོའི་སྒྲུང་། སྒྲུང་མཁན་གྱི་ཁ་བརྒྱུད་ཀྱིས་ད་བར་བརྒྱུད་ཡོད།'
  },
  {
    id: 2,
    category: t('heritage.category.traditionalMusic'),
    name: 'ནག་ཆུའི་རི་གླུ།',
    description: 'བྱང་ཐང་ས་མཐོའི་རྩྭ་ཐང་འཚོ་བ་ལས་བྱུང་བའི་སྲོལ་རྒྱུན་དམངས་གླུ།'
  },
  {
    id: 3,
    category: t('heritage.category.traditionalMusic'),
    name: 'ལྷ་སའི་ནང་མ།',
    description: 'བོད་ཀྱི་གསོལ་སྟོན་དང་གླུ་གར་སྲོལ་རྒྱུན་ནང་གི་གླུ་རོལ་རྣམ་པ་གཙོ་བོ།'
  },
  {
    id: 4,
    category: t('heritage.category.traditionalDance'),
    name: 'རེ་པ་གར།',
    description: 'གླུ་དང་གར། རྩལ་འཁྲབ་བཅས་མཉམ་སྡེབ་ཀྱི་བོད་ཀྱི་སྲོལ་རྒྱུན་འཁྲབ་རྩལ།'
  },
  {
    id: 5,
    category: t('heritage.category.traditionalDance'),
    name: 'མང་ཁང་གི་ཞན་ཙི།',
    description: 'སྒྲ་སྙན་གྱི་རོལ་མོ་དང་མཉམ་དུ་འཁྲབ་པའི་མང་ཁང་ས་ཁུལ་གྱི་མཉམ་གར།'
  },
  {
    id: 6,
    category: t('heritage.category.traditionalDance'),
    name: 'སྐོར་གར།',
    description: 'བོད་མིའི་རྩྭ་ཐང་དང་གྲོང་སྡེའི་འཚོ་བའི་ནང་ཁྱབ་པའི་སྡེ་ཚན་མཉམ་གར།'
  },
  {
    id: 7,
    category: t('heritage.category.traditionalDrama'),
    name: 'བོད་ཟློས་གར།',
    description: 'ཆོས་ལུགས་ཆོ་ག་དང་ལོ་རྒྱུས་སྒྲུང་མཉམ་འདྲེས་ཀྱི་སྲོལ་རྒྱུན་ཟློས་གར།'
  },
  {
    id: 8,
    category: t('heritage.category.traditionalDrama'),
    name: 'ལྷ་ས་ཇོ་མོ་ལུང་།',
    description: 'བོད་ཟློས་གར་གྱི་གྲགས་ཆེ་བའི་རྒྱུན་ལུགས་གཅིག'
  },
  {
    id: 9,
    category: t('heritage.category.traditionalSports'),
    name: 'བོད་རིགས་ཀྱི་རྟ་རྩལ།',
    description: 'བོད་ཀྱི་རྟ་ཞོན་རྩལ་དང་རྩྭ་ཐང་འཚོ་བ་མཚོན་པའི་སྲོལ་རྒྱུན་ལུས་རྩལ།'
  },
  {
    id: 10,
    category: t('heritage.category.traditionalCraft'),
    name: 'བོད་རིགས་ཐང་ཀ',
    description: 'ཆོས་ལུགས་རི་མོ་དང་རྡོ་སྨན་ཚོན་རྫས་ལག་རྩལ་ཟུང་འབྲེལ་གྱི་བོད་ཀྱི་རི་མོའི་མཚོན་རྟགས།'
  },
  {
    id: 11,
    category: t('heritage.category.traditionalCraft'),
    name: 'བོད་རིགས་ལྕགས་རིགས་བཟོ་རྩལ།',
    description: 'བོད་ཀྱི་གྲི། ཟངས་ཆས། གསེར་དངུལ་རྒྱན་ཆ་སོགས་བཟོ་བའི་ལག་ཤེས།'
  },
  {
    id: 12,
    category: t('heritage.category.traditionalCraft'),
    name: 'བོད་སྤོས་བཟོ་རྩལ།',
    description: 'མིན་གྲོལ་གླིང་སོགས་སྲོལ་རྒྱུན་ནས་བྱུང་བའི་སྤོས་རྫས་དང་བཟོ་རྩལ།'
  },
  {
    id: 13,
    category: t('heritage.category.traditionalMedicine'),
    name: 'བོད་སྨན་ཁྲུས་ཐབས།',
    description: 'བོད་སྨན་རིག་པའི་གཞི་རྩ་དང་ས་མཐོའི་སྨན་རྩྭ། ཆུ་ཚན་སོགས་ཟུང་འབྲེལ་གྱི་ཕྱི་བཅོས་ཐབས་ལམ།'
  },
  {
    id: 14,
    category: t('heritage.category.folkCustom'),
    name: 'ཞོ་སྟོན།',
    description: 'འགྲེམས་སྟོན་ཆོ་ག བོད་ཟློས་གར་འཁྲབ་སྟོན། དམངས་ཁྲོད་རྩེད་མོ་བཅས་མཉམ་སྡེབ་ཀྱི་བོད་ཀྱི་དུས་ཆེན་གལ་ཆེན།'
  },
  {
    id: 15,
    category: t('heritage.category.folkCustom'),
    name: 'འོང་སྐོར།',
    description: 'ཞིང་ལས་ཐོན་སྐྱེད་དང་ལོ་ལེགས་སྨོན་འདུན་ལ་འབྲེལ་བའི་བོད་ཀྱི་ཞིང་གྲོང་དམངས་སྲོལ།'
  }
]

// 简单归一化分类名称，便于和上方大类卡片对应
const normalizeCategoryName = (name: string) => {
  return name
    .replace(/类$/, '')
    .replace(/国家级/g, '')
    .replace(/\s+/g, '')
    .replace(/・/g, '·')
}

const nationalHeritageItems = computed<NationalHeritageItem[]>(() => {
  if (locale.value === 'bo') {
    return getTibetanNationalHeritageItems()
  }

  const lines = nationalHeritageRaw.split('\n').map(l => l.trim()).filter(Boolean)
  const items: NationalHeritageItem[] = []
  let currentCategory = ''
  let id = 1

  const categoryPrefixes = ['一、', '二、', '三、', '四、', '五、', '六、', '七、', '八、']

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

const sortOptions = computed<Array<{ value: HeritageSortMode; label: string }>>(() => [
  { value: 'hot', label: '综合热度' },
  { value: 'views', label: '浏览最多' },
  { value: 'likes', label: '点赞最多' },
  { value: 'comments', label: '评论最多' },
  { value: 'latest', label: '最新收录' },
  { value: 'name', label: '名称排序' }
])

const categoryOptions = computed(() => {
  const categories = new Set<string>()
  heritageItems.value.forEach(item => {
    const category = item.category?.trim()
    if (category) categories.add(category)
  })
  return Array.from(categories).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const heritageNameById = computed<Record<number, string>>(() => {
  return heritageItems.value.reduce<Record<number, string>>((map, item) => {
    map[item.id] = item.name
    return map
  }, {})
})

const totalViews = computed(() =>
  heritageItems.value.reduce((sum, item) => sum + (item.viewCount || 0), 0)
)

const totalInteractions = computed(() =>
  heritageItems.value.reduce((sum, item) => sum + (item.likeCount || 0) + (item.commentCount || 0), 0)
)

const getHeritageScore = (item: HeritageItem) =>
  (item.viewCount || 0) + (item.likeCount || 0) * 8 + (item.commentCount || 0) * 12 + (item.videoUrl ? 15 : 0) + (item.protectionLevel ? 5 : 0)

const filteredHeritageItems = computed<HeritageItem[]>(() => {
  const keyword = searchKeyword.value.trim().toLocaleLowerCase()
  const items = heritageItems.value.filter(item => {
    if (selectedCategory.value !== 'all' && item.category !== selectedCategory.value) return false
    if (!keyword) return true

    return [
      item.name,
      item.nameTibetan,
      item.description,
      item.category,
      item.region,
      item.protectionLevel,
      item.originStory,
      item.significance
    ]
      .filter(Boolean)
      .join(' ')
      .toLocaleLowerCase()
      .includes(keyword)
  })

  return [...items].sort((a, b) => {
    switch (sortMode.value) {
      case 'views':
        return (b.viewCount || 0) - (a.viewCount || 0)
      case 'likes':
        return (b.likeCount || 0) - (a.likeCount || 0)
      case 'comments':
        return (b.commentCount || 0) - (a.commentCount || 0)
      case 'latest':
        return new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
      case 'name':
        return (a.name || '').localeCompare(b.name || '', 'zh-CN')
      case 'hot':
      default:
        return getHeritageScore(b) - getHeritageScore(a)
    }
  })
})

const formatCompact = (value: number) =>
  new Intl.NumberFormat('zh-CN', {
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(value)

const formatEventMonth = (dateStr?: string) => {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '--'
  return `${date.getMonth() + 1}月`
}

const formatEventDay = (dateStr?: string) => {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '--'
  return String(date.getDate()).padStart(2, '0')
}

const selectCategory = (category: string) => {
  selectedCategory.value = selectedCategory.value === category ? 'all' : category
}

const mergeFeaturedInheritors = (inheritors: HeritageInheritorItem[]) => {
  const merged = new Map<number, HeritageInheritorItem>()
  ;[...featuredInheritors.value, ...inheritors].forEach(inheritor => {
    if (inheritor?.id) merged.set(inheritor.id, inheritor)
  })
  featuredInheritors.value = Array.from(merged.values()).slice(0, 6)
}

const isUnauthorizedError = (error: unknown): boolean => {
  return Boolean(
    error &&
    typeof error === 'object' &&
    'response' in error &&
    (error as { response?: { status?: number } }).response?.status === 401
  )
}

const detailRequestConfig = {
  skipAuthRedirect: true
}

const fetchFeaturedInheritors = async (items: HeritageItem[]) => {
  const candidates = items.filter(item => item.id && item.id < 10000).slice(0, 8)
  if (!candidates.length) {
    featuredInheritors.value = []
    return
  }

  featuredInheritorsLoading.value = true
  try {
    const responses = await Promise.all(
      candidates.map(item =>
        api.get(endpoints.heritage.inheritors(item.id))
          .then(response => response.data || [])
          .catch(() => [])
      )
    )
    const merged = new Map<number, HeritageInheritorItem>()
    responses.flat().forEach((inheritor: HeritageInheritorItem) => {
      if (inheritor?.id) merged.set(inheritor.id, inheritor)
    })
    featuredInheritors.value = Array.from(merged.values()).slice(0, 6)
  } catch (error) {
    console.error('Failed to fetch featured inheritors:', error)
  } finally {
    featuredInheritorsLoading.value = false
  }
}

const fetchUpcomingEvents = async () => {
  try {
    const response = await api.get(endpoints.heritage.upcomingEvents, { params: { size: 6 } })
    upcomingEvents.value = response.data?.content || response.data || []
  } catch (error) {
    console.error('Failed to fetch upcoming heritage events:', error)
  }
}

const updateHeritageItem = (id: number, patch: Partial<HeritageItem>) => {
  heritageItems.value = heritageItems.value.map(item =>
    item.id === id ? { ...item, ...patch } : item
  )
}

const openHeritageById = (id?: number) => {
  if (!id) return
  const item = heritageItems.value.find(entry => entry.id === id)
  if (item) openDetail(item)
}

const fetchHeritageItems = async (keyword?: string) => {
  try {
    const params: Record<string, string> = { size: '100' }
    if (keyword) params.keyword = keyword
    const response = await api.get(endpoints.heritage.list, { params })
    const items = response.data?.content || response.data || []
    heritageItems.value = items
    if (authStore.isLoggedIn) {
      void fetchFeaturedInheritors(items)
    } else {
      featuredInheritors.value = []
    }
  } catch (error) {
    console.error('Failed to fetch heritage items:', error)
  } finally {
    loading.value = false
    searchLoading.value = false
  }
}

const handleSearch = async () => {
  const kw = searchKeyword.value.trim()
  if (!kw) {
    loading.value = true
    await fetchHeritageItems()
    return
  }
  searchLoading.value = true
  await fetchHeritageItems(kw)
}

const refreshHeritageModule = async () => {
  searchLoading.value = true
  await Promise.all([
    fetchHeritageItems(searchKeyword.value.trim() || undefined),
    fetchUpcomingEvents()
  ])
}

const loadDetailData = async (item: HeritageItem) => {
  if (!item.id || item.id >= 10000) return

  commentsLoading.value = true
  detailAuthRequired.value = false
  try {
    const detailRes = await api.get(endpoints.heritage.detail(item.id), detailRequestConfig)
      .catch(error => {
        if (isUnauthorizedError(error)) {
          detailAuthRequired.value = true
          return null
        }
        throw error
      })

    if (detailAuthRequired.value) {
      itemComments.value = []
      itemInheritors.value = []
      itemEvents.value = []
      return
    }

    const [commentsRes, inheritorsRes, eventsRes] = await Promise.all([
      api.get(endpoints.heritage.comments(item.id), detailRequestConfig).catch(error => {
        if (isUnauthorizedError(error)) return null
        throw error
      }),
      api.get(endpoints.heritage.inheritors(item.id), detailRequestConfig).catch(error => {
        if (isUnauthorizedError(error)) return null
        throw error
      }),
      api.get(endpoints.heritage.events(item.id), detailRequestConfig).catch(error => {
        if (isUnauthorizedError(error)) return null
        throw error
      })
    ])
    const detailItem = detailRes?.data as HeritageItem | undefined
    if (detailItem?.id) {
      selectedItem.value = { ...item, ...detailItem }
      updateHeritageItem(item.id, detailItem)
    }
    itemComments.value = commentsRes?.data?.content || commentsRes?.data || []
    itemInheritors.value = inheritorsRes?.data || []
    itemEvents.value = eventsRes?.data || []
    mergeFeaturedInheritors(itemInheritors.value)
  } catch (e) {
    console.error('Failed to load detail data:', e)
  } finally {
    commentsLoading.value = false
  }

  if (authStore.isLoggedIn && !detailAuthRequired.value) {
    try {
      const res = await api.get(endpoints.heritage.likeStatus(item.id), detailRequestConfig)
      liked.value = res.data?.liked || false
    } catch { liked.value = false }
  }
}

const toggleLike = async () => {
  if (!selectedItem.value || !authStore.isLoggedIn) return
  try {
    const res = await api.post(endpoints.heritage.like(selectedItem.value.id))
    const nextLiked = res.data?.liked ?? !liked.value
    const nextLikeCount = typeof res.data?.likeCount === 'number'
      ? res.data.likeCount
      : Math.max(0, (selectedItem.value.likeCount || 0) + (nextLiked ? 1 : -1))
    liked.value = nextLiked
    selectedItem.value = { ...selectedItem.value, likeCount: nextLikeCount }
    updateHeritageItem(selectedItem.value.id, { likeCount: nextLikeCount })
  } catch (e) {
    console.error('Failed to toggle like:', e)
  }
}

const submitComment = async () => {
  if (!selectedItem.value || !authStore.isLoggedIn || !newCommentContent.value.trim()) return
  submittingComment.value = true
  try {
    const res = await api.post(endpoints.heritage.comments(selectedItem.value.id), {
      content: newCommentContent.value.trim(),
      rating: newCommentRating.value
    })
    if (res.data) {
      itemComments.value = [res.data, ...itemComments.value]
      if (selectedItem.value) {
        const nextCommentCount = (selectedItem.value.commentCount || 0) + 1
        selectedItem.value = { ...selectedItem.value, commentCount: nextCommentCount }
        updateHeritageItem(selectedItem.value.id, { commentCount: nextCommentCount })
      }
    }
    newCommentContent.value = ''
    newCommentRating.value = 5
  } catch (e) {
    console.error('Failed to submit comment:', e)
  } finally {
    submittingComment.value = false
  }
}

const deleteComment = async (commentId: number) => {
  if (!selectedItem.value) return
  try {
    await api.delete(endpoints.heritage.deleteComment(selectedItem.value.id, commentId))
    itemComments.value = itemComments.value.filter(c => c.id !== commentId)
    if (selectedItem.value) {
      const nextCommentCount = Math.max(0, (selectedItem.value.commentCount || 1) - 1)
      selectedItem.value = { ...selectedItem.value, commentCount: nextCommentCount }
      updateHeritageItem(selectedItem.value.id, { commentCount: nextCommentCount })
    }
  } catch (e) {
    console.error('Failed to delete comment:', e)
  }
}

const formatDate = (dateStr: string) => {
  try {
    return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' })
  } catch { return dateStr }
}

// 监听语言变化，重新获取数据
watch(locale, () => {
  void fetchHeritageItems(searchKeyword.value.trim() || undefined)
  void fetchUpcomingEvents()
})

watch(() => authStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    void fetchFeaturedInheritors(heritageItems.value)
    if (selectedItem.value && selectedItem.value.id < 10000 && detailAuthRequired.value) {
      void loadDetailData(selectedItem.value)
    }
  } else {
    featuredInheritors.value = []
    itemComments.value = []
    itemInheritors.value = []
    itemEvents.value = []
    if (selectedItem.value && selectedItem.value.id < 10000) {
      detailAuthRequired.value = true
    }
  }
})

const toggleCategory = (categoryName: string) => {
  activeCategory.value = activeCategory.value === categoryName ? null : categoryName
}

const openDetail = (item: HeritageItem) => {
  selectedItem.value = item
  itemComments.value = []
  itemInheritors.value = []
  itemEvents.value = []
  detailAuthRequired.value = false
  liked.value = false
  newCommentContent.value = ''
  loadDetailData(item)
}

const openBaikeUrl = (url?: string | null) => {
  const safeUrl = safeExternalUrl(url)
  if (!safeUrl) {
    window.alert(t('security.invalidExternalLink'))
    return
  }
  window.open(safeUrl, '_blank', 'noopener,noreferrer')
}

// 构建地图导航链接（这里以高德地图 Web 导航链接为例，可根据实际需要切换为百度地图等）
const buildNavUrl = (spot: ExperienceSpot) => {
  const base = 'https://uri.amap.com/navigation'
  const to = `${spot.lng},${spot.lat},${encodeURIComponent(spot.name)}`
  return `${base}?to=${to}&mode=car&utm_source=colorful-tibet`
}

const clearMap = () => {
  mapInitVersion += 1
  if (pendingMapFrame !== null) {
    window.cancelAnimationFrame(pendingMapFrame)
    pendingMapFrame = null
  }

  if (map) {
    map.remove()
    map = null
  }
  markers = []
}

const htmlEscapeMap: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;'
}

const escapeHtml = (value: string): string =>
  value.replace(/[&<>"']/g, char => htmlEscapeMap[char] || char)

const PI = Math.PI
const A = 6378245.0
const EE = 0.00669342162296594323

const outOfChina = (lng: number, lat: number): boolean =>
  lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271

const transformLat = (x: number, y: number): number => {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
  return ret
}

const transformLon = (x: number, y: number): number => {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
  return ret
}

const wgs84ToGcj02 = (lng: number, lat: number): [number, number] => {
  if (outOfChina(lng, lat)) return [lng, lat]
  const dlat = transformLat(lng - 105.0, lat - 35.0)
  const dlng = transformLon(lng - 105.0, lat - 35.0)
  const radlat = lat / 180.0 * PI
  let magic = Math.sin(radlat)
  magic = 1 - EE * magic * magic
  const sqrtmagic = Math.sqrt(magic)
  const mglat = (dlat * 180.0) / ((A * (1 - EE)) / (magic * sqrtmagic) * PI)
  const mglng = (dlng * 180.0) / (A / sqrtmagic * Math.cos(radlat) * PI)
  return [lng + mglng, lat + mglat]
}

const fitExperienceMarkers = (L: typeof Leaflet) => {
  const leafletMap = map
  if (!leafletMap) return

  const positions = experienceSpots.value.map((spot) => {
    const [lng, lat] = wgs84ToGcj02(spot.lng, spot.lat)
    return L.latLng(lat, lng)
  })
  if (!positions.length) return

  leafletMap.fitBounds(L.latLngBounds(positions), {
    padding: [28, 28],
    maxZoom: 8
  })
}

const addExperienceMarkers = (L: typeof Leaflet) => {
  const leafletMap = map
  if (!leafletMap) return
  markers.forEach(marker => marker.remove())
  markers = []

  experienceSpots.value.forEach((spot) => {
    try {
      const [lng, lat] = wgs84ToGcj02(spot.lng, spot.lat)
      const marker = L.marker([lat, lng], {
        title: spot.name,
        icon: L.divIcon({
          className: 'heritage-map-marker',
          html: `<span style="display:inline-flex;align-items:center;justify-content:center;min-width:26px;height:26px;padding:0 7px;border-radius:999px;background:#ef4444;color:#fff;font-size:12px;font-weight:700;box-shadow:0 8px 18px rgba(127,29,29,.28);border:2px solid rgba(255,255,255,.9);">${escapeHtml(spot.tag)}</span>`,
          iconSize: [34, 34],
          iconAnchor: [17, 17],
          popupAnchor: [0, -16]
        })
      })

      marker.bindPopup(`
        <div style="padding:6px 2px;min-width:190px;max-width:240px;">
          <h3 style="margin:0 0 8px 0;font-size:15px;font-weight:700;color:#1c1917;">${escapeHtml(spot.name)}</h3>
          <p style="margin:4px 0;font-size:12px;color:#57534e;line-height:1.55;">${escapeHtml(spot.address)}</p>
          <p style="margin:6px 0 0 0;font-size:12px;color:#78716c;line-height:1.55;">${escapeHtml(spot.brief)}</p>
          <p style="margin:6px 0 0 0;font-size:12px;color:#dc2626;line-height:1.55;">${escapeHtml(spot.highlight)}</p>
        </div>
      `)
      marker.addTo(leafletMap)
      markers.push(marker)
    } catch (error) {
      console.error('添加标记失败:', error)
    }
  })

  fitExperienceMarkers(L)
}

// 初始化体验点地图
const initMap = async () => {
  if (!mapContainer.value) {
    console.warn('地图容器未找到')
    mapLoading.value = false
    return
  }

  mapLoading.value = true
  mapError.value = false
  const initVersion = ++mapInitVersion

  try {
    const L = await loadLeaflet()
    if (!mapContainer.value || !selectedItem.value || initVersion !== mapInitVersion) return

    map = L.map(mapContainer.value, {
      zoomControl: false,
      attributionControl: false
    })
    map.setView([29.653, 91.117], 6)

    const tileLayer = L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      maxZoom: 18,
      minZoom: 3,
      subdomains: '1234'
    })

    tileLayer.on('load', () => {
      if (initVersion !== mapInitVersion) return
      mapLoading.value = false
    })
    tileLayer.on('tileerror', () => {
      if (initVersion !== mapInitVersion) return
      mapLoading.value = false
    })
    tileLayer.addTo(map)
    addExperienceMarkers(L)

    window.setTimeout(() => {
      if (initVersion !== mapInitVersion || !map) return
      map.invalidateSize()
      fitExperienceMarkers(L)
    }, 250)

    const finishMapLoad = () => {
      if (initVersion !== mapInitVersion || !selectedItem.value) return
      mapLoading.value = false
    }
    window.setTimeout(finishMapLoad, 8000)
  } catch (error) {
    if (initVersion !== mapInitVersion) return
    console.error('地图初始化失败:', error)
    mapError.value = true
    mapLoading.value = false
  }
}

// 监听 selectedItem 变化，当地图容器出现时初始化地图
watch(selectedItem, async (newItem) => {
  if (newItem) {
    // 等待DOM更新，确保地图容器已渲染
    await nextTick()
    pendingMapFrame = window.requestAnimationFrame(() => {
      pendingMapFrame = null
      void initMap()
    })
  } else {
    // 如果关闭了详情，清理地图
    clearMap()
    mapLoading.value = true
    mapError.value = false
  }
})

onMounted(() => {
  void fetchHeritageItems()
  void fetchUpcomingEvents()
})

onBeforeUnmount(() => {
  clearMap()
})
</script>

<style scoped>
</style>
