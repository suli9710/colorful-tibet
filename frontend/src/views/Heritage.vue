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
              :placeholder="t('heritage.searchPlaceholder')"
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
            {{ searchLoading ? '...' : t('heritage.search') }}
          </button>
          <button
            v-if="searchKeyword"
            type="button"
            class="mobile-touch-target px-3 py-2.5 rounded-xl border border-stone-200 text-sm text-stone-500 hover:bg-stone-50 transition"
            @click="searchKeyword = ''; handleSearch()"
          >
            {{ t('heritage.clearSearch') }}
          </button>
        </form>
      </div>

      <div
        v-if="heritageErrorMessage"
        class="mx-auto mb-8 max-w-3xl rounded-2xl border border-tibet-red/20 bg-white/85 px-5 py-4 text-center shadow-sm"
        role="alert"
        aria-live="assertive"
      >
        <p class="text-sm leading-6 text-tibet-brown/75">{{ heritageErrorMessage }}</p>
        <button
          type="button"
          class="mt-3 inline-flex min-h-10 items-center justify-center rounded-full bg-tibet-dark px-5 py-2 text-sm font-semibold text-white transition hover:bg-tibet-dark/90 focus:outline-none focus:ring-2 focus:ring-tibet-gold/60 focus:ring-offset-2"
          :disabled="searchLoading"
          @click="refreshHeritageModule"
        >
          {{ searchLoading ? t('common.loading') : t('spots.reload') }}
        </button>
      </div>

      <motion.section
        v-if="!loading && (!heritageErrorMessage || heritageItems.length)"
        class="mb-8 grid grid-cols-1 gap-4 sm:mb-10 sm:gap-6 xl:grid-cols-[minmax(0,1fr)_360px]"
        :initial="{ opacity: 0, y: 20 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.34, ease: motionEase }"
      >
        <div class="tibet-panel rounded-2xl p-4 sm:p-6">
          <div class="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4 mb-5">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.2em] text-tibet-red/70 mb-2">{{ t('heritage.library.eyebrow') }}</p>
              <h2 class="text-2xl font-bold text-tibet-dark">{{ t('heritage.library.title') }}</h2>
              <p class="mt-1 text-sm text-tibet-brown/60">
                {{ t('heritage.library.subtitle') }}
              </p>
            </div>
            <button
              type="button"
              class="inline-flex items-center justify-center gap-2 rounded-xl border border-tibet-gold/30 bg-white/75 px-4 py-2 text-sm font-medium text-tibet-dark transition hover:border-tibet-red/35 hover:text-tibet-red disabled:opacity-50"
              :disabled="searchLoading"
              @click="refreshHeritageModule"
            >
              <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': searchLoading }" />
              {{ t('heritage.library.refresh') }}
            </button>
          </div>

          <div class="grid grid-cols-2 lg:grid-cols-4 gap-2.5 sm:gap-3 mb-5">
            <div class="rounded-xl border border-stone-200 bg-white/70 px-3 py-3 sm:px-4">
              <div class="flex items-center justify-between gap-2">
                <p class="text-xs text-stone-500">{{ t('heritage.stats.items') }}</p>
                <BookOpen class="h-4 w-4 text-tibet-red" />
              </div>
              <p class="mt-2 text-xl font-bold text-stone-900 sm:text-2xl">{{ formatCompact(heritageItems.length) }}</p>
            </div>
            <div class="rounded-xl border border-stone-200 bg-white/70 px-3 py-3 sm:px-4">
              <div class="flex items-center justify-between gap-2">
                <p class="text-xs text-stone-500">{{ t('heritage.stats.views') }}</p>
                <Eye class="h-4 w-4 text-blue-600" />
              </div>
              <p class="mt-2 text-xl font-bold text-stone-900 sm:text-2xl">{{ formatCompact(totalViews) }}</p>
            </div>
            <div class="rounded-xl border border-stone-200 bg-white/70 px-3 py-3 sm:px-4">
              <div class="flex items-center justify-between gap-2">
                <p class="text-xs text-stone-500">{{ t('heritage.stats.interactions') }}</p>
                <MessageCircle class="h-4 w-4 text-emerald-600" />
              </div>
              <p class="mt-2 text-xl font-bold text-stone-900 sm:text-2xl">{{ formatCompact(totalInteractions) }}</p>
            </div>
            <div class="rounded-xl border border-stone-200 bg-white/70 px-3 py-3 sm:px-4">
              <div class="flex items-center justify-between gap-2">
                <p class="text-xs text-stone-500">{{ t('heritage.stats.inheritors') }}</p>
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
                {{ t('heritage.filter.all') }}
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
                    @error="markHeritageImageFailed(item)"
                  />
                  <div v-else class="flex h-full w-full items-center justify-center bg-gradient-to-br from-stone-100 to-amber-50 text-stone-400">
                    <div class="text-center">
                      <BookOpen class="mx-auto h-8 w-8" />
                      <p class="mt-2 text-xs text-stone-500">{{ item.name }}</p>
                    </div>
                  </div>
                  <div class="absolute left-3 top-3 max-w-[75%] rounded-full bg-black/55 px-2.5 py-1 text-[11px] font-medium text-white backdrop-blur">
                    <span class="line-clamp-1">{{ item.category || t('heritage.fallback.itemCategory') }}</span>
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
                    {{ item.description || t('heritage.fallback.itemDescription') }}
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
              <p class="mt-3 text-sm font-medium text-stone-700">{{ t('heritage.empty.noResults') }}</p>
              <p class="mt-1 text-xs text-stone-400">{{ t('heritage.empty.noResultsHint') }}</p>
            </motion.div>
          </AnimatePresence>
        </div>

        <aside class="space-y-4">
          <div class="tibet-panel rounded-2xl p-4 sm:p-5">
            <div class="mb-4 flex items-center justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.16em] text-tibet-red/60">{{ t('heritage.sidebar.inheritorsEyebrow') }}</p>
                <h3 class="mt-1 text-lg font-bold text-stone-900">{{ t('heritage.sidebar.inheritorsTitle') }}</h3>
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
                    {{ heritageNameById[inheritor.heritageItemId] || inheritor.region || t('heritage.fallback.itemCategory') }}
                  </p>
                  <p class="mt-1 line-clamp-2 text-xs leading-relaxed text-stone-600">
                    {{ inheritor.story || inheritor.bio || t('heritage.fallback.noStory') }}
                  </p>
                </div>
              </button>
            </div>
            <p v-else class="rounded-xl border border-dashed border-stone-200 bg-white/60 px-4 py-5 text-center text-xs text-stone-400">
              {{ t('heritage.empty.noInheritors') }}
            </p>
          </div>

          <div class="tibet-panel rounded-2xl p-4 sm:p-5">
            <div class="mb-4 flex items-center justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.16em] text-tibet-red/60">{{ t('heritage.sidebar.eventsEyebrow') }}</p>
                <h3 class="mt-1 text-lg font-bold text-stone-900">{{ t('heritage.sidebar.eventsTitle') }}</h3>
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
              {{ t('heritage.empty.noEvents') }}
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
              @click="toggleCategory(category.key)"
            >
              <div class="flex items-center gap-4 w-full">
                <!-- 图标 -->
                <div class="flex-shrink-0">
                  <motion.div
                    class="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-gradient-to-br from-tibet-red via-tibet-gold to-tibet-yellow text-white flex items-center justify-center shadow-sm group-hover:shadow-md"
                    :animate="activeCategory === category.key ? { rotate: -4, scale: 1.06 } : { rotate: 0, scale: 1 }"
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
                  :animate="activeCategory === category.key ? { rotate: 45, scale: 1.04 } : { rotate: 0, scale: 1 }"
                  :transition="softSpring"
                  aria-hidden="true"
                >
                  +
                </motion.span>
              </div>

              <!-- 卡片内部可滚动的国家级非遗项目列表 -->
              <AnimatePresence>
                <motion.div
                  v-if="activeCategory === category.key"
                  :key="category.key + '-items'"
                  layout
                  class="mt-1 w-full origin-top rounded-xl bg-tibet-white/70 border border-tibet-gold/25 px-3 py-2 max-h-44 overflow-y-auto text-xs sm:text-sm text-tibet-brown/80 space-y-2"
                  :initial="{ opacity: 0, y: -10, scaleY: 0.96 }"
                  :animate="{ opacity: 1, y: 0, scaleY: 1 }"
                  :exit="{ opacity: 0, y: -8, scaleY: 0.96 }"
                  :transition="{ duration: 0.28, ease: motionEase }"
                >
                  <p class="text-[11px] text-tibet-brown/45">
                    {{ t('heritage.nationalItems') }} · {{ getItemsByCategory(category.key).length }} {{ t('heritage.items') }}
                  </p>
                  <motion.div
                    v-for="(item, itemIndex) in getItemsByCategory(category.key)"
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
                    v-if="!getItemsByCategory(category.key).length"
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
                  v-if="resolveHeritageImage(item)"
                  :src="resolveHeritageImage(item)"
                  :alt="item.name"
                  class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                  loading="lazy"
                  @error="markHeritageImageFailed(item)"
                />
                <div v-else class="flex h-full w-full items-center justify-center bg-gradient-to-br from-stone-100 to-amber-50 text-stone-400">
                  <BookOpen class="h-8 w-8" />
                </div>
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
              v-if="resolveHeritageImage(selectedItem)"
              :src="resolveHeritageImage(selectedItem)"
              :alt="selectedItem.name"
              class="w-full h-full object-cover"
              :initial="{ opacity: 0, scale: 1.06 }"
              :animate="{ opacity: 1, scale: 1 }"
              :transition="{ duration: 0.5, ease: motionEase }"
              @error="markHeritageImageFailed(selectedItem)"
            />
            <div v-else class="flex h-full w-full items-center justify-center bg-gradient-to-br from-stone-100 to-amber-50 text-stone-400">
              <BookOpen class="h-10 w-10" />
            </div>
            <motion.button
              type="button"
              class="absolute top-3 right-3 bg-black/50 text-white p-2 rounded-full hover:bg-black/70 transition sm:top-4 sm:right-4"
              :aria-label="t('common.closeMenu')"
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
              {{ t('heritage.loginToViewDetail') }}
            </p>
            <router-link
              to="/login"
              class="mt-2 inline-flex rounded-lg bg-tibet-red px-3 py-1.5 text-xs font-medium text-white hover:bg-tibet-red/90"
            >
              {{ t('common.login') }}
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
              {{ liked ? t('heritage.interaction.liked') : t('heritage.interaction.like') }}
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
                  {{ t('heritage.videoUnsupported') }}
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
                {{ t('heritage.inheritors') }}
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
                {{ t('heritage.relatedEvents') }}
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
                {{ t('heritage.commentsTitle') }} ({{ selectedItem.commentCount || 0 }})
              </h4>

              <!-- 发表评论 -->
              <div v-if="authStore.isLoggedIn" class="mb-4 space-y-2">
                <div class="flex items-center gap-1 mb-1">
                  <span class="text-xs text-stone-500">{{ t('heritage.rating') }}:</span>
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
                    :placeholder="t('heritage.commentPlaceholder')"
                    class="flex-1 px-3 py-2 rounded-lg border border-stone-200 text-sm focus:outline-none focus:ring-1 focus:ring-tibet-red/30"
                    @keydown.enter.prevent="submitComment"
                  />
                  <button
                    type="button"
                    class="mobile-touch-target px-4 py-2 rounded-lg bg-tibet-red text-white text-sm font-medium hover:bg-tibet-red/90 transition disabled:opacity-50"
                    :disabled="submittingComment || !newCommentContent.trim()"
                    @click="submitComment"
                  >
                    {{ submittingComment ? '...' : t('heritage.submitComment') }}
                  </button>
                </div>
              </div>
              <p v-else class="text-xs text-stone-400 mb-3">
                {{ t('heritage.loginToComment') }}
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
                        <span v-else>{{ (comment.nickname || t('heritage.anonymous')).charAt(0) }}</span>
                      </div>
                      <span class="text-xs font-medium text-stone-700">{{ comment.nickname || t('heritage.anonymous') }}</span>
                      <span v-if="comment.rating" class="text-[11px] text-amber-500">
                        {{ '★'.repeat(comment.rating) }}
                      </span>
                    </div>
                    <div class="flex items-center gap-2">
                      <span class="text-[10px] text-stone-400">{{ formatDate(comment.createdAt) }}</span>
                      <button
                        v-if="comment.owner"
                        type="button"
                        class="text-[10px] text-red-400 hover:text-red-600 transition disabled:cursor-wait disabled:opacity-60"
                        :disabled="isDeletingComment(comment.id)"
                        :aria-busy="isDeletingComment(comment.id)"
                        @click="deleteComment(comment.id)"
                      >
                        {{ t('heritage.deleteComment') }}
                      </button>
                    </div>
                  </div>
                  <p class="text-sm text-stone-600 leading-relaxed">{{ comment.content }}</p>
                </div>
                <div class="flex flex-col gap-2 pt-1 sm:flex-row sm:items-center sm:justify-between">
                  <span class="text-xs text-stone-400">
                    {{ itemCommentsPage + 1 }} / {{ itemCommentsTotalPages || 1 }}
                    <span v-if="itemCommentsTotalElements"> · {{ itemCommentsTotalElements }}</span>
                  </span>
                  <button
                    v-if="hasMoreItemComments"
                    type="button"
                    class="mobile-touch-target inline-flex items-center justify-center rounded-full border border-stone-200 bg-white px-4 py-2 text-xs font-medium text-stone-600 transition hover:border-tibet-red/30 hover:text-tibet-red disabled:cursor-wait disabled:opacity-60"
                    :disabled="commentsLoadingMore"
                    :aria-busy="commentsLoadingMore"
                    @click="loadNextItemCommentsPage"
                  >
                    {{ commentsLoadingMore ? t('common.loading') : t('community.nextPage') }}
                  </button>
                </div>
              </div>
              <p v-else class="text-xs text-stone-400 text-center py-3">
                {{ t('heritage.noComments') }}
              </p>
            </div>
          </motion.div>
        </template>
      </MotionModal>

      <AnimatePresence>
        <motion.div
          v-if="loading"
          key="heritage-loading"
          class="flex h-64 flex-col items-center justify-center gap-3 text-sm text-tibet-brown/60"
          role="status"
          aria-live="polite"
          :aria-label="t('heritage.loadingLabel')"
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
          <span>{{ t('heritage.loadingLabel') }}</span>
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
import { hasNextPage, mergeUniqueById, readPaginatedResponse, type PageMetadata, type PaginatedHttpResponse } from '../api/endpoints'
import type {
  HeritageCommentItem,
  HeritageEventItem,
  HeritageInheritorItem,
  HeritageItem
} from '../api'
import { useAuthStore } from '../stores/auth'
import { useToast } from '../composables/useToast'
import { createTextCardPopupContent } from '../utils/domText'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'
import { toIntlLocale } from '../i18n/formatting'
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

const { t, tm, locale } = useI18n()
const { showToast } = useToast()
const activeIntlLocale = computed(() => toIntlLocale(locale.value))

type HeritageCategoryKey =
  | 'folkLiterature'
  | 'traditionalMusic'
  | 'traditionalDance'
  | 'traditionalDrama'
  | 'traditionalSports'
  | 'traditionalCraft'
  | 'traditionalMedicine'
  | 'folkCustom'

interface HeritageContentNationalItem {
  id: number
  name: string
  description: string
}

interface HeritageContentRepresentativeItem {
  key: string
  id: number
  name: string
  description: string
  categoryKey: HeritageCategoryKey
  significance: string
  imageUrl: string
  baikeUrl: string
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

interface HeritageContentMessages {
  national: {
    noDescription: string
    categories: Record<HeritageCategoryKey, { items: HeritageContentNationalItem[] }>
  }
  representative: Record<string, HeritageContentRepresentativeItem>
  experienceSpots: ExperienceSpot[]
  imageAliases: Record<string, string>
  aliases: {
    thangkaBackendName: string
    thangkaDisplayName: string
  }
  eventMonthSuffix: string
}

const heritageContent = computed(
  () => tm('heritageContent') as HeritageContentMessages
)

const heritageCategories = computed(() => [
  {
    key: 'folkLiterature' as const,
    name: t('heritage.category.folkLiterature'),
    prefix: t('heritage.category.folkLiterature'),
    icon: '📜',
    layout: 'lg:-mt-4 lg:ml-4 z-20'
  },
  {
    key: 'traditionalMusic' as const,
    name: t('heritage.category.traditionalMusic'),
    prefix: t('heritage.category.traditionalMusic'),
    icon: '🥁',
    layout: 'lg:mt-8 z-30'
  },
  {
    key: 'traditionalDance' as const,
    name: t('heritage.category.traditionalDance'),
    prefix: t('heritage.category.traditionalDance'),
    icon: '💃',
    layout: 'lg:-mt-10 lg:-mr-4 z-40'
  },
  {
    key: 'traditionalDrama' as const,
    name: t('heritage.category.traditionalDrama'),
    prefix: t('heritage.category.traditionalDrama'),
    icon: '🎭',
    layout: 'lg:-mt-2 lg:ml-8 z-30'
  },
  {
    key: 'traditionalSports' as const,
    name: t('heritage.category.traditionalSports'),
    prefix: t('heritage.category.traditionalSports'),
    icon: '🏹',
    layout: 'lg:mt-10 z-20'
  },
  {
    key: 'traditionalCraft' as const,
    name: t('heritage.category.traditionalCraft'),
    prefix: t('heritage.category.traditionalCraft'),
    icon: '🧶',
    layout: 'lg:-mt-6 lg:-mr-6 z-30'
  },
  {
    key: 'traditionalMedicine' as const,
    name: t('heritage.category.traditionalMedicine'),
    prefix: t('heritage.category.traditionalMedicine'),
    icon: '🌿',
    layout: 'lg:mt-6 z-20'
  },
  {
    key: 'folkCustom' as const,
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

const responseContent = <T>(data: unknown): T[] => {
  if (Array.isArray(data)) return data as T[]
  if (data && typeof data === 'object' && Array.isArray((data as { content?: unknown }).content)) {
    return (data as { content: T[] }).content
  }
  return []
}

const heritageCommentsPageSize = 20

const emptyHeritageCommentsPage = (): PageMetadata => ({
  page: 0,
  size: heritageCommentsPageSize,
  totalElements: 0,
  totalPages: 0
})

const heritageItems = ref<HeritageItem[]>([])
const loading = ref(true)
const heritageErrorMessage = ref('')
const selectedItem = ref<HeritageItem | null>(null)

const searchKeyword = ref('')
const searchLoading = ref(false)
const failedHeritageImages = ref<Record<string, boolean>>({})

const itemComments = ref<HeritageCommentItem[]>([])
const itemCommentsPageInfo = ref<PageMetadata>(emptyHeritageCommentsPage())
const commentsLoading = ref(false)
const commentsLoadingMore = ref(false)
const itemInheritors = ref<HeritageInheritorItem[]>([])
const itemEvents = ref<HeritageEventItem[]>([])
const detailAuthRequired = ref(false)
const liked = ref(false)
let detailRequestId = 0
let commentsPageRequestId = 0
let likeMutationRequestId = 0
const newCommentContent = ref('')
const newCommentRating = ref(5)
const submittingComment = ref(false)
const deletingCommentIds = ref<ReadonlySet<number>>(new Set())
const upcomingEvents = ref<HeritageEventItem[]>([])
const featuredInheritors = ref<HeritageInheritorItem[]>([])
const featuredInheritorsLoading = ref(false)
const selectedCategory = ref('all')

type HeritageSortMode = 'hot' | 'views' | 'likes' | 'comments' | 'latest' | 'name'

const sortMode = ref<HeritageSortMode>('hot')
const itemCommentsPage = computed(() => itemCommentsPageInfo.value.page)
const itemCommentsTotalPages = computed(() => itemCommentsPageInfo.value.totalPages)
const itemCommentsTotalElements = computed(() => itemCommentsPageInfo.value.totalElements)
const hasMoreItemComments = computed(() => hasNextPage(itemCommentsPageInfo.value))

const resetItemCommentsPageInfo = () => {
  itemCommentsPageInfo.value = emptyHeritageCommentsPage()
}

const isCurrentDetailRequest = (requestId: number, itemId: number) =>
  requestId === detailRequestId && selectedItem.value?.id === itemId

const isCurrentCommentsPageRequest = (requestId: number, itemId: number) =>
  requestId === commentsPageRequestId && selectedItem.value?.id === itemId

const isSelectedHeritageItem = (itemId: number) =>
  selectedItem.value?.id === itemId

const isDeletingComment = (commentId: number) => deletingCommentIds.value.has(commentId)

const setDeletingComment = (commentId: number, deleting: boolean) => {
  const next = new Set(deletingCommentIds.value)
  if (deleting) {
    next.add(commentId)
  } else {
    next.delete(commentId)
  }
  deletingCommentIds.value = next
}

const genericHeritageImagePatterns = [
  'images.unsplash.com/photo-1559827291'
]

const heritageImageByName = computed<Record<string, string>>(
  () => heritageContent.value?.imageAliases ?? {}
)

const isGenericHeritageImage = (imageUrl?: string | null): boolean =>
  !imageUrl || genericHeritageImagePatterns.some(pattern => imageUrl.includes(pattern))

const findMappedHeritageImage = (name?: string | null): string => {
  if (!name) return ''
  const aliases = heritageImageByName.value
  const normalizedName = name.replace(/[（）()《》“”"·/、\s]/g, '')
  const direct = aliases[name] || aliases[normalizedName]
  if (direct) return direct

  const match = Object.entries(aliases).find(([key]) => {
    const normalizedKey = key.replace(/[（）()《》“”"·/、\s]/g, '')
    return normalizedName.includes(normalizedKey) || normalizedKey.includes(normalizedName)
  })
  return match?.[1] || ''
}

const getHeritageImageKey = (item?: HeritageItem | null): string =>
  String(item?.id || item?.imageUrl || item?.name || 'heritage-image')

const resolveHeritageImage = (item?: HeritageItem | null): string => {
  if (failedHeritageImages.value[getHeritageImageKey(item)]) return ''

  const mapped = findMappedHeritageImage(item?.name)
  if (mapped) return mapped

  const imageUrl = item?.imageUrl?.trim()
  return isGenericHeritageImage(imageUrl) ? '' : (imageUrl || '')
}

const markHeritageImageFailed = (item?: HeritageItem | null): void => {
  failedHeritageImages.value[getHeritageImageKey(item)] = true
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

const experienceSpots = computed<ExperienceSpot[]>(
  () => heritageContent.value?.experienceSpots ?? []
)

interface NationalHeritageItem {
  id: number
  category: string
  name: string
  description: string
}

const representativeItems = computed<HeritageItem[]>(() => {
  const content = heritageContent.value
  const extra = Object.values(content?.representative ?? {}).map(item => ({
    id: item.id,
    name: item.name,
    description: item.description,
    category: t(`heritage.category.${item.categoryKey}`),
    imageUrl: item.imageUrl,
    videoUrl: '',
    originStory: '',
    significance: item.significance,
    baikeUrl: item.baikeUrl
  }))

  const extraNames = new Set(extra.map(item => item.name))
  const thangkaBackendName = content?.aliases?.thangkaBackendName ?? ''
  const thangkaDisplayName = content?.aliases?.thangkaDisplayName ?? ''

  const base = heritageItems.value
    .map(item => ({
      ...item,
      imageUrl: resolveHeritageImage(item)
    }))
    .filter(item => {
      if (extraNames.has(item.name)) return false
      if (item.name === thangkaBackendName && extraNames.has(thangkaDisplayName)) return false
      if (!item.imageUrl) return false
      return true
    })
    .slice(0, 4)

  return [...base, ...extra]
})

const activeCategory = ref<HeritageCategoryKey | null>(null)

const getItemsByCategory = (categoryKey: HeritageCategoryKey): NationalHeritageItem[] => {
  const items = heritageContent.value?.national?.categories?.[categoryKey]?.items ?? []
  const noDescription = heritageContent.value?.national?.noDescription ?? ''
  const categoryLabel = t(`heritage.category.${categoryKey}`)

  return items.map(item => ({
    id: item.id,
    category: categoryLabel,
    name: item.name,
    description: item.description || noDescription
  }))
}

const sortOptions = computed<Array<{ value: HeritageSortMode; label: string }>>(() => [
  { value: 'hot', label: t('heritage.sort.hot') },
  { value: 'views', label: t('heritage.sort.views') },
  { value: 'likes', label: t('heritage.sort.likes') },
  { value: 'comments', label: t('heritage.sort.comments') },
  { value: 'latest', label: t('heritage.sort.latest') },
  { value: 'name', label: t('heritage.sort.name') }
])

const categoryOptions = computed(() => {
  const categories = new Set<string>()
  heritageItems.value.forEach(item => {
    const category = item.category?.trim()
    if (category) categories.add(category)
  })
  return Array.from(categories).sort((a, b) => a.localeCompare(b, activeIntlLocale.value))
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
        return (a.name || '').localeCompare(b.name || '', activeIntlLocale.value)
      case 'hot':
      default:
        return getHeritageScore(b) - getHeritageScore(a)
    }
  })
})

const formatCompact = (value: number) =>
  new Intl.NumberFormat(activeIntlLocale.value, {
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(value)

const formatEventMonth = (dateStr?: string) => {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '--'
  return `${date.getMonth() + 1}${heritageContent.value?.eventMonthSuffix ?? ""}`
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
          .then(response => responseContent<HeritageInheritorItem>(response.data))
          .catch(() => [])
      )
    )
    const merged = new Map<number, HeritageInheritorItem>()
    responses.flat().forEach((inheritor: HeritageInheritorItem) => {
      if (inheritor?.id) merged.set(inheritor.id, inheritor)
    })
    featuredInheritors.value = Array.from(merged.values()).slice(0, 6)
  } catch (error) {
    console.error('Failed to fetch featured inheritors:', summarizeClientError(error))
  } finally {
    featuredInheritorsLoading.value = false
  }
}

const fetchUpcomingEvents = async () => {
  try {
    const response = await api.get(endpoints.heritage.upcomingEvents, { params: { size: 6 } })
    upcomingEvents.value = responseContent<HeritageEventItem>(response.data)
  } catch (error) {
    console.error('Failed to fetch upcoming heritage events:', summarizeClientError(error))
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
    heritageErrorMessage.value = ''
    const params: Record<string, string> = { size: '100' }
    if (keyword) params.keyword = keyword
    const response = await api.get(endpoints.heritage.list, { params })
    heritageItems.value = responseContent<HeritageItem>(response.data)
    if (authStore.isLoggedIn) {
      void fetchFeaturedInheritors(heritageItems.value)
    } else {
      featuredInheritors.value = []
    }
  } catch (error) {
    console.error('Failed to fetch heritage items:', summarizeClientError(error))
    heritageErrorMessage.value = safeClientErrorMessage(error, t('toast.pageLoadFailed'))
    if (!heritageItems.value.length) {
      featuredInheritors.value = []
    }
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

  const requestId = ++detailRequestId
  commentsPageRequestId += 1
  const itemId = item.id
  commentsLoading.value = true
  commentsLoadingMore.value = false
  detailAuthRequired.value = false
  try {
    const detailRes = await api.get(endpoints.heritage.detail(itemId), detailRequestConfig)
      .catch(error => {
        if (isUnauthorizedError(error)) {
          if (!isCurrentDetailRequest(requestId, itemId)) return null
          detailAuthRequired.value = true
          return null
        }
        throw error
      })
    if (!isCurrentDetailRequest(requestId, itemId)) return

    if (detailAuthRequired.value) {
      itemComments.value = []
      itemInheritors.value = []
      itemEvents.value = []
      return
    }

    const [commentsRes, inheritorsRes, eventsRes] = await Promise.all([
      api.get(endpoints.heritage.comments(itemId), {
        ...detailRequestConfig,
        params: { page: 0, size: heritageCommentsPageSize }
      }).catch(error => {
        if (isUnauthorizedError(error)) return null
        throw error
      }),
      api.get(endpoints.heritage.inheritors(itemId), detailRequestConfig).catch(error => {
        if (isUnauthorizedError(error)) return null
        throw error
      }),
      api.get(endpoints.heritage.events(itemId), detailRequestConfig).catch(error => {
        if (isUnauthorizedError(error)) return null
        throw error
      })
    ])
    if (!isCurrentDetailRequest(requestId, itemId)) return
    const detailItem = detailRes?.data as HeritageItem | undefined
    if (detailItem?.id) {
      selectedItem.value = { ...item, ...detailItem }
      updateHeritageItem(itemId, detailItem)
    }
    applyItemCommentsPage(commentsRes)
    itemInheritors.value = responseContent<HeritageInheritorItem>(inheritorsRes?.data)
    itemEvents.value = responseContent<HeritageEventItem>(eventsRes?.data)
    mergeFeaturedInheritors(itemInheritors.value)
  } catch (e) {
    if (!isCurrentDetailRequest(requestId, itemId)) return
    console.error('Failed to load detail data:', summarizeClientError(e))
  } finally {
    if (isCurrentDetailRequest(requestId, itemId)) {
      commentsLoading.value = false
    }
  }

  if (authStore.isLoggedIn && !detailAuthRequired.value && isCurrentDetailRequest(requestId, itemId)) {
    try {
      const res = await api.get(endpoints.heritage.likeStatus(itemId), detailRequestConfig)
      if (!isCurrentDetailRequest(requestId, itemId)) return
      liked.value = res.data?.liked || false
    } catch {
      if (isCurrentDetailRequest(requestId, itemId)) {
        liked.value = false
      }
    }
  }
}

const applyItemCommentsPage = (response: PaginatedHttpResponse | null | undefined, append = false) => {
  const page = readPaginatedResponse<HeritageCommentItem>(response ?? { data: [] }, {
    page: append ? itemCommentsPageInfo.value.page + 1 : 0,
    size: heritageCommentsPageSize
  })

  itemComments.value = append ? mergeUniqueById(itemComments.value, page.content) : page.content
  itemCommentsPageInfo.value = {
    page: page.page,
    size: page.size || heritageCommentsPageSize,
    totalElements: page.totalElements,
    totalPages: page.totalPages
  }
}

const loadNextItemCommentsPage = async () => {
  if (!selectedItem.value || commentsLoadingMore.value || !hasMoreItemComments.value) return
  const itemId = selectedItem.value.id
  const requestId = ++commentsPageRequestId
  commentsLoadingMore.value = true
  try {
    const response = await api.get(endpoints.heritage.comments(itemId), {
      ...detailRequestConfig,
      params: { page: itemCommentsPageInfo.value.page + 1, size: heritageCommentsPageSize }
    })
    if (!isCurrentCommentsPageRequest(requestId, itemId)) return
    applyItemCommentsPage(response, true)
  } catch (error) {
    if (!isCurrentCommentsPageRequest(requestId, itemId)) return
    console.error('Failed to load more heritage comments:', summarizeClientError(error))
    showToast(t('toast.pageLoadFailed'), 'error')
  } finally {
    if (isCurrentCommentsPageRequest(requestId, itemId)) {
      commentsLoadingMore.value = false
    }
  }
}

const toggleLike = async () => {
  if (!selectedItem.value || !authStore.isLoggedIn) return
  const itemId = selectedItem.value.id
  const requestId = ++likeMutationRequestId
  const previousLiked = liked.value
  const previousLikeCount = selectedItem.value.likeCount || 0
  try {
    const res = await api.post(endpoints.heritage.like(itemId))
    if (requestId !== likeMutationRequestId || !isSelectedHeritageItem(itemId)) return

    const nextLiked = res.data?.liked ?? !liked.value
    const nextLikeCount = typeof res.data?.likeCount === 'number'
      ? res.data.likeCount
      : Math.max(0, previousLikeCount + (nextLiked === previousLiked ? 0 : nextLiked ? 1 : -1))
    liked.value = nextLiked
    selectedItem.value = { ...selectedItem.value, likeCount: nextLikeCount }
    updateHeritageItem(itemId, { likeCount: nextLikeCount })
  } catch (e) {
    console.error('Failed to toggle like:', summarizeClientError(e))
  }
}

const submitComment = async () => {
  if (!selectedItem.value || !authStore.isLoggedIn || !newCommentContent.value.trim() || submittingComment.value) return
  const itemId = selectedItem.value.id
  const content = newCommentContent.value.trim()
  const rating = newCommentRating.value
  const previousCommentCount = selectedItem.value.commentCount || 0
  submittingComment.value = true
  try {
    const res = await api.post(endpoints.heritage.comments(itemId), {
      content,
      rating
    })
    if (!isSelectedHeritageItem(itemId)) return

    if (res.data) {
      itemComments.value = [res.data, ...itemComments.value]
      const nextTotalElements = itemCommentsPageInfo.value.totalElements + 1
      itemCommentsPageInfo.value = {
        ...itemCommentsPageInfo.value,
        totalElements: nextTotalElements,
        totalPages: Math.max(
          itemCommentsPageInfo.value.totalPages,
          Math.ceil(nextTotalElements / heritageCommentsPageSize)
        )
      }
      if (selectedItem.value) {
        const nextCommentCount = previousCommentCount + 1
        selectedItem.value = { ...selectedItem.value, commentCount: nextCommentCount }
        updateHeritageItem(itemId, { commentCount: nextCommentCount })
      }
    }
    newCommentContent.value = ''
    newCommentRating.value = 5
  } catch (e) {
    console.error('Failed to submit comment:', summarizeClientError(e))
  } finally {
    submittingComment.value = false
  }
}

const deleteComment = async (commentId: number) => {
  if (!selectedItem.value || isDeletingComment(commentId)) return
  const itemId = selectedItem.value.id
  const previousCommentCount = selectedItem.value.commentCount || 0
  setDeletingComment(commentId, true)
  try {
    await api.delete(endpoints.heritage.deleteComment(itemId, commentId))
    if (!isSelectedHeritageItem(itemId)) return

    itemComments.value = itemComments.value.filter(c => c.id !== commentId)
    const nextTotalElements = Math.max(0, itemCommentsPageInfo.value.totalElements - 1)
    itemCommentsPageInfo.value = {
      ...itemCommentsPageInfo.value,
      totalElements: nextTotalElements,
      totalPages: Math.ceil(nextTotalElements / heritageCommentsPageSize)
    }
    if (selectedItem.value) {
      const nextCommentCount = Math.max(0, previousCommentCount - 1)
      selectedItem.value = { ...selectedItem.value, commentCount: nextCommentCount }
      updateHeritageItem(itemId, { commentCount: nextCommentCount })
    }
  } catch (e) {
    console.error('Failed to delete comment:', summarizeClientError(e))
  } finally {
    setDeletingComment(commentId, false)
  }
}

const formatDate = (dateStr: string) => {
  try {
    return new Date(dateStr).toLocaleDateString(activeIntlLocale.value, { year: 'numeric', month: 'short', day: 'numeric' })
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
    resetItemCommentsPageInfo()
    itemInheritors.value = []
    itemEvents.value = []
    if (selectedItem.value && selectedItem.value.id < 10000) {
      detailAuthRequired.value = true
    }
  }
})

const toggleCategory = (categoryKey: HeritageCategoryKey) => {
  activeCategory.value = activeCategory.value === categoryKey ? null : categoryKey
}

const openDetail = (item: HeritageItem) => {
  detailRequestId += 1
  commentsPageRequestId += 1
  likeMutationRequestId += 1
  selectedItem.value = item
  itemComments.value = []
  resetItemCommentsPageInfo()
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
    showToast(t('security.invalidExternalLink'), 'warning')
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

const createExperienceMarkerBadge = (tag: string) => {
  const badge = document.createElement('span')
  badge.textContent = tag
  badge.style.display = 'inline-flex'
  badge.style.alignItems = 'center'
  badge.style.justifyContent = 'center'
  badge.style.minWidth = '26px'
  badge.style.height = '26px'
  badge.style.padding = '0 7px'
  badge.style.borderRadius = '999px'
  badge.style.background = '#ef4444'
  badge.style.color = '#fff'
  badge.style.fontSize = '12px'
  badge.style.fontWeight = '700'
  badge.style.boxShadow = '0 8px 18px rgba(127,29,29,.28)'
  badge.style.border = '2px solid rgba(255,255,255,.9)'
  return badge
}

const buildExperiencePopupContent = (spot: ExperienceSpot) =>
  createTextCardPopupContent(spot.name, [
    { text: spot.address, color: '#57534e' },
    { text: spot.brief, color: '#78716c' },
    { text: spot.highlight, color: '#dc2626' }
  ])

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
          html: createExperienceMarkerBadge(spot.tag),
          iconSize: [34, 34],
          iconAnchor: [17, 17],
          popupAnchor: [0, -16]
        })
      })

      marker.bindPopup(buildExperiencePopupContent(spot))
      marker.addTo(leafletMap)
      markers.push(marker)
    } catch (error) {
      console.error('Failed to add heritage map marker:', summarizeClientError(error))
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
    console.error('Failed to initialize heritage map:', summarizeClientError(error))
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
