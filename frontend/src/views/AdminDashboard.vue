<template>
  <div class="min-h-screen bg-stone-50 py-8 pt-24 sm:py-12">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="mb-8 text-center sm:mb-12">
        <h1 class="mb-3 text-3xl font-bold text-stone-800 sm:mb-4 sm:text-4xl">{{ t('admin.title') }}</h1>
        <p class="text-base text-stone-600 sm:text-lg">{{ t('admin.subtitle') }}</p>
      </div>

      <div v-if="loading" class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>

      <div v-else class="space-y-6 sm:space-y-8">
        <AdminSecurityPosturePanel
          :posture="securityPosture"
          :loading="loadingSecurityPosture"
          :error="securityPostureError"
          @refresh="fetchSecurityPosture"
        />

        <AdminAnalyticsPanel :loading="loading" :chart-data="analyticsData" :error="analyticsError" />

        <!-- Stats Cards -->
        <div class="grid grid-cols-1 gap-4 md:grid-cols-3 md:gap-6">
          <div class="bg-white rounded-lg shadow p-5 sm:p-6 border-l-4 border-blue-500">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-stone-500 uppercase font-semibold">{{ t('admin.totalUsers') }}</p>
                <p class="text-2xl font-bold text-stone-800 sm:text-3xl">{{ stats.userCount }}</p>
              </div>
              <div class="bg-blue-100 p-3 rounded-full">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-lg shadow p-5 sm:p-6 border-l-4 border-green-500">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-stone-500 uppercase font-semibold">{{ t('admin.totalOrders') }}</p>
                <p class="text-2xl font-bold text-stone-800 sm:text-3xl">{{ stats.orderCount }}</p>
              </div>
              <div class="bg-green-100 p-3 rounded-full">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                </svg>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-lg shadow p-5 sm:p-6 border-l-4 border-yellow-500">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-stone-500 uppercase font-semibold">{{ t('admin.totalRevenue') }}</p>
                <p class="text-2xl font-bold text-stone-800 sm:text-3xl">¥{{ stats.totalRevenue?.toLocaleString() || 0 }}</p>
              </div>
              <div class="bg-yellow-100 p-3 rounded-full">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- Hotel Orders Management -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden">
          <div
            class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
            role="button"
            tabindex="0"
            :aria-expanded="showAllHotelOrders"
            aria-controls="admin-hotel-orders-content"
            :aria-label="t('admin.hotelOrders')"
            @click="toggleAllHotelOrders"
            @keydown.enter="activateKeyboardPanel($event, toggleAllHotelOrders)"
            @keydown.space="activateKeyboardPanel($event, toggleAllHotelOrders)"
          >
            <div class="flex min-w-0 items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-amber-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
              </div>
                <h3 class="text-lg font-bold text-stone-800">{{ t('admin.hotelOrders') }} <span class="text-sm font-normal text-stone-400">({{ hotelOrders.length }}{{ t('common.items') }})</span></h3>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <button @click.stop="fetchHotelOrders" :disabled="loadingHotelOrders" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">
                {{ loadingHotelOrders ? t('common.loading') : t('common.refresh') }}
              </button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showAllHotelOrders }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
              </svg>
            </div>
          </div>

          <div v-if="loadingHotelOrders" class="p-8 text-center text-stone-500">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
            <p>{{ t('admin.loadingHotelOrders') }}</p>
          </div>

          <div v-else-if="showAllHotelOrders" id="admin-hotel-orders-content" class="overflow-x-auto">
            <table class="min-w-full divide-y divide-stone-200">
              <thead class="bg-stone-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.orderId') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.booker') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.hotelAndRoom') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.checkInOut') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('hotel.guests') }} / {{ t('hotel.nights') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.amount') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.status') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.orderedAt') }}</th>
                  <th class="px-6 py-3 text-right text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.action') }}</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-stone-200">
                <tr v-for="order in hotelOrders" :key="order.id" class="hover:bg-stone-50">
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ order.id }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-stone-800">
                    <div>{{ displayHotelOrderGuest(order) }}</div>
                    <div class="text-xs font-normal text-stone-400">{{ maskHotelOrderPhone(order.phone) }}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-600">
                    <div>{{ displayHotelOrderName(order) }}</div>
                    <div class="text-xs text-stone-400">{{ displayHotelOrderRoom(order) }}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">
                    <div>{{ displayHotelOrderDate(order.checkInDate) }}</div>
                    <div class="text-xs text-stone-400">{{ t('admin.toDate', { date: displayHotelOrderDate(order.checkOutDate) }) }}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-600">
                    <div>{{ displayHotelOrderOccupancy(order) }}</div>
                    <div v-if="displayHotelOrderNote(order)" class="mt-1 max-w-xs truncate text-xs text-stone-400">{{ displayHotelOrderNote(order) }}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-red-600">{{ formatHotelOrderCurrency(order.totalPrice) }}</td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <select
                      :value="order.status"
                      @change="updateHotelOrderStatus(order.id, ($event.target as HTMLSelectElement).value)"
                      class="text-xs px-2 py-1 rounded-full font-semibold border-0 cursor-pointer"
                      :class="{
                        'bg-yellow-100 text-yellow-800': order.status === 'PENDING',
                        'bg-green-100 text-green-800': order.status === 'CONFIRMED',
                        'bg-red-100 text-red-800': order.status === 'CANCELLED'
                      }"
                    >
                      <option value="PENDING">{{ t('admin.statusLabel.PENDING') }}</option>
                      <option value="CONFIRMED">{{ t('admin.statusLabel.CONFIRMED') }}</option>
                      <option value="CANCELLED">{{ t('admin.statusLabel.CANCELLED') }}</option>
                    </select>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ displayHotelOrderDateTime(order.createdAt) }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button @click="deleteHotelOrder(order.id)" class="text-red-600 hover:text-red-900">{{ t('common.delete') }}</button>
                  </td>
                </tr>
                <tr v-if="hotelOrders.length === 0">
                  <td colspan="9" class="px-6 py-8 text-center text-stone-500">{{ t('admin.noHotelOrders') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else-if="!loadingHotelOrders && hotelOrders.length > 0" class="px-6 py-4 text-center text-stone-500 text-sm">
            {{ t('admin.expandHotelOrders', { count: hotelOrders.length }) }}
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mt-8">
          <!-- Recent Bookings -->
          <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden">
            <div
              class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
              role="button"
              tabindex="0"
              :aria-expanded="showRecentOrders"
              aria-controls="admin-recent-orders-content"
              :aria-label="t('admin.latestOrders')"
              @click="toggleRecentOrders"
              @keydown.enter="activateKeyboardPanel($event, toggleRecentOrders)"
              @keydown.space="activateKeyboardPanel($event, toggleRecentOrders)"
            >
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-lg bg-blue-100 flex items-center justify-center">
                  <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"/></svg>
                </div>
                <h3 class="text-lg font-bold text-stone-800">{{ t('admin.latestOrders') }} <span class="text-sm font-normal text-stone-400">({{ recentOrders.length }}{{ t('common.items') }})</span></h3>
              </div>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showRecentOrders }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
              </svg>
            </div>
            <div v-if="showRecentOrders" id="admin-recent-orders-content" class="divide-y divide-stone-100">
              <template v-if="recentOrders.length > 0">
                <div v-for="order in recentOrders" :key="order.id" class="px-6 py-4 flex items-center justify-between hover:bg-stone-50 transition-colors">
                  <div>
                    <div class="flex items-center gap-2">
                      <span v-if="order.hotel" class="text-xs px-2 py-0.5 rounded bg-blue-100 text-blue-700">{{ t('admin.hotel') }}</span>
                      <span v-else class="text-xs px-2 py-0.5 rounded bg-orange-100 text-orange-700">{{ t('admin.spot') }}</span>
                      <p class="text-sm font-medium text-stone-800">{{ order.hotel?.name || order.spot?.name || t('common.unknown') }}</p>
                    </div>
                    <p class="text-xs text-stone-500 mt-1">{{ formatDate(order.createdAt) }}</p>
                  </div>
                  <div class="text-right">
                    <p class="text-sm font-bold text-stone-800">¥{{ order.totalPrice }}</p>
                    <span :class="getStatusClass(order.status)" class="text-xs px-2 py-1 rounded-full">{{ getStatusLabel(order.status) }}</span>
                  </div>
                </div>
              </template>
              <div v-else class="px-6 py-4 text-center text-stone-400 text-sm">{{ t('admin.noOrders') }}</div>
            </div>
          </div>

          <!-- Popular Spots -->
          <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden">
            <div
              class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
              role="button"
              tabindex="0"
              :aria-expanded="showPopularSpots"
              aria-controls="admin-popular-spots-content"
              :aria-label="t('admin.popularSpots')"
              @click="togglePopularSpots"
              @keydown.enter="activateKeyboardPanel($event, togglePopularSpots)"
              @keydown.space="activateKeyboardPanel($event, togglePopularSpots)"
            >
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-lg bg-rose-100 flex items-center justify-center">
                  <svg class="w-5 h-5 text-rose-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"/></svg>
                </div>
                <h3 class="text-lg font-bold text-stone-800">{{ t('admin.popularSpots') }} <span class="text-sm font-normal text-stone-400">({{ sortedPopularSpots.length }}{{ t('common.countUnit') }})</span></h3>
              </div>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showPopularSpots }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
              </svg>
            </div>
            <div v-if="showPopularSpots" id="admin-popular-spots-content" class="divide-y divide-stone-100">
              <div v-for="(spot, index) in sortedPopularSpots" :key="spot.id" class="px-6 py-4 flex items-center hover:bg-stone-50 transition-colors">
                <span class="text-lg font-bold text-stone-300 w-8">{{ index + 1 }}</span>
                <img :src="spot.imageUrl || ''" class="w-10 h-10 rounded-lg object-cover mr-4" alt="">
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-stone-800 truncate">{{ spot.name }}</p>
                  <p class="text-xs text-stone-500 mb-2">{{ spot.location }}</p>
                  <div class="flex items-center gap-2 mt-1">
                    <div class="flex-1 bg-stone-100 rounded-full h-2 overflow-hidden">
                      <div
                        class="h-full bg-gradient-to-r from-rose-400 to-rose-600 rounded-full transition-all duration-500"
                        :style="{ width: getClickCountPercentage(spot) + '%' }"
                      ></div>
                    </div>
                    <span class="text-xs text-stone-500 font-medium whitespace-nowrap">{{ t('admin.visits', { count: spot.visitCount || 0 }) }}</span>
                  </div>
                </div>
                <div class="text-right ml-4">
                  <p class="text-sm font-bold text-red-600">¥{{ spot.ticketPrice }}</p>
                </div>
              </div>
              <div v-if="!sortedPopularSpots?.length" class="px-6 py-4 text-center text-stone-400 text-sm">{{ t('common.noData') }}</div>
            </div>
          </div>
        </div>
        
        <!-- Spots Management Section -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div
            class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
            role="button"
            tabindex="0"
            :aria-expanded="showSpots"
            aria-controls="admin-spots-content"
            :aria-label="t('admin.spotManagement')"
            @click="toggleSpotsPanel"
            @keydown.enter="activateKeyboardPanel($event, toggleSpotsPanel)"
            @keydown.space="activateKeyboardPanel($event, toggleSpotsPanel)"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-emerald-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">{{ t('admin.spotManagement') }} <span class="text-sm font-normal text-stone-400">({{ spots.length }}{{ t('common.countUnit') }})</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="fetchSpots" :disabled="loadingSpots" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">
                {{ loadingSpots ? t('common.loading') : t('common.refresh') }}
              </button>
              <button @click.stop="batchFetchPrices" :disabled="batchFetching" class="text-xs px-3 py-1.5 rounded-lg bg-emerald-100 text-emerald-700 hover:bg-emerald-200 transition-colors font-medium">
                {{ batchFetching ? t('admin.fetchingPrices') : t('admin.batchFetchPrices') }}
              </button>
              <button v-if="spots.length > 6 && showSpots" @click.stop="showAllSpots = !showAllSpots" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">
                {{ showAllSpots ? t('common.collapse') : t('common.expandAll') + '(' + spots.length + ')' }}
              </button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showSpots }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
            </div>
          </div>

          <div v-if="priceBatchJob" class="border-b border-emerald-100 bg-emerald-50/60 px-4 py-4 sm:px-6">
            <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div class="min-w-0">
                <p class="text-sm font-semibold text-emerald-800">爬虫更新进度</p>
                <p class="mt-1 truncate text-xs text-emerald-700/80">
                  {{ priceBatchStatusText }}
                </p>
              </div>
              <div class="text-sm font-bold text-emerald-800">{{ priceBatchProgressPercent }}%</div>
            </div>
            <div class="mt-3 h-2 overflow-hidden rounded-full bg-white">
              <div
                class="h-full rounded-full bg-emerald-500 transition-all duration-500"
                :style="{ width: `${priceBatchProgressPercent}%` }"
              ></div>
            </div>
            <div class="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-emerald-700/80">
              <span>已处理 {{ priceBatchJob.processed || 0 }}/{{ priceBatchJob.total || 0 }}</span>
              <span>成功 {{ priceBatchJob.success || 0 }}</span>
              <span>失败 {{ priceBatchJob.failed || 0 }}</span>
              <span>跳过 {{ priceBatchJob.skipped || 0 }}</span>
            </div>
          </div>

          <div v-if="showSpots" id="admin-spots-content">
            <!-- Loading State -->
            <div v-if="loadingSpots" class="p-12 text-center">
              <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-emerald-500 mx-auto mb-4"></div>
              <p class="text-stone-400 text-sm">{{ t('admin.loadingSpots') }}</p>
            </div>

            <!-- Error State -->
            <div v-else-if="spotsError" class="p-12 text-center">
              <div class="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center mx-auto mb-4">
                <svg class="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"/></svg>
              </div>
              <p class="text-red-500 text-sm mb-1">{{ t('admin.loadFailed') }}</p>
              <p class="text-stone-400 text-xs mb-4">{{ spotsError }}</p>
              <button @click="fetchSpots" class="text-sm px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors">{{ t('common.retry') }}</button>
            </div>

            <!-- Empty State -->
            <div v-else-if="spots.length === 0" class="p-16 text-center">
              <svg class="w-16 h-16 text-stone-200 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
              <p class="text-stone-400">{{ t('admin.noSpotData') }}</p>
            </div>

            <!-- Spots Card Grid -->
            <div v-else class="p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="spot in displayedSpots" :key="spot.id"
                   class="group bg-white rounded-xl border border-stone-200 overflow-hidden hover:shadow-lg hover:border-emerald-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-300 transition-all duration-200 cursor-pointer"
                   role="button"
                   tabindex="0"
                   :aria-label="`${t('common.edit')} ${spot.name || ''}`.trim()"
                   @click="openEditModal(spot)"
                   @keydown.enter="activateKeyboardPanel($event, () => openEditModal(spot))"
                   @keydown.space="activateKeyboardPanel($event, () => openEditModal(spot))">
                <!-- Thumbnail -->
                <div class="relative h-40 overflow-hidden bg-stone-100">
                  <img v-if="spot.imageUrl" :src="textOrEmpty(spot.imageUrl)" :alt="textOrEmpty(spot.name)"
                       class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                       @error="($event.target as HTMLImageElement).style.display='none'" />
                  <div v-if="!spot.imageUrl" class="w-full h-full flex items-center justify-center bg-gradient-to-br from-emerald-400 to-teal-600">
                    <span class="text-4xl font-bold text-white/80">{{ spot.name?.charAt(0) }}</span>
                  </div>
                  <!-- Price badge + fetch button -->
                  <div class="absolute top-3 right-3 flex items-center gap-1">
                    <span class="px-2.5 py-1 rounded-full bg-white/90 backdrop-blur text-emerald-700 text-xs font-bold shadow-sm">
                      ¥{{ spot.ticketPrice || '-' }}
                    </span>
                    <button
                      @click.stop="fetchSpotPrice(spot)"
                      :disabled="fetchingPriceId === spot.id"
                      class="w-6 h-6 rounded-full bg-white/90 backdrop-blur text-stone-500 hover:text-emerald-600 hover:bg-emerald-50 shadow-sm flex items-center justify-center transition-colors"
                      :title="t('admin.fetchPrice')"
                    >
                      <svg v-if="fetchingPriceId !== spot.id" class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
                      <svg v-else class="w-3.5 h-3.5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/></svg>
                    </button>
                  </div>
                  <!-- Edit overlay on hover -->
                  <div class="absolute inset-0 bg-black/0 group-hover:bg-black/30 transition-all flex items-center justify-center">
                    <span class="px-4 py-2 rounded-lg bg-white/90 text-stone-700 text-sm font-medium opacity-0 group-hover:opacity-100 transition-opacity shadow-lg backdrop-blur">
                    {{ t('admin.clickEdit') }}
                    </span>
                  </div>
                </div>
                <!-- Info -->
                <div class="p-4">
                  <h4 class="font-semibold text-stone-800 truncate mb-1">{{ spot.name }}</h4>
                  <div class="flex items-center gap-2 text-xs text-stone-400 mb-2">
                    <span v-if="spot.city" class="flex items-center gap-0.5">
                      <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
                      {{ spot.city }}
                    </span>
                    <span v-if="(spot.visitCount || 0) > 0">· 👁 {{ spot.visitCount }}</span>
                  </div>
                  <p v-if="spot.description" class="text-xs text-stone-500 line-clamp-2 leading-relaxed">{{ spot.description }}</p>
                  <div class="mt-3 pt-3 border-t border-stone-100 flex items-center justify-between">
                    <span class="text-xs text-stone-400">ID: {{ spot.id }}</span>
                    <span class="text-xs font-medium text-emerald-600 group-hover:text-emerald-700 transition-colors">{{ t('admin.editArrow') }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="px-6 py-4 text-center text-stone-500 text-sm">
            {{ t('admin.expandSpots', { count: spots.length }) }}
          </div>
        </div>

        <!-- User Management Section -->
        <div class="bg-white rounded-lg shadow overflow-hidden mt-8">
          <div
            class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
            role="button"
            tabindex="0"
            :aria-expanded="showUsers"
            aria-controls="admin-users-content"
            :aria-label="t('admin.userManagement')"
            @click="toggleUsersPanel"
            @keydown.enter="activateKeyboardPanel($event, toggleUsersPanel)"
            @keydown.space="activateKeyboardPanel($event, toggleUsersPanel)"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-violet-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-violet-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">{{ t('admin.userManagement') }} <span class="text-sm font-normal text-stone-400">({{ users.length }}{{ t('common.peopleUnit') }})</span></h3>
            </div>
            <div class="flex items-center gap-2">
            <button @click.stop="fetchUsers" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">{{ t('common.refresh') }}</button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showUsers }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
            </div>
          </div>
          <div v-if="showUsers" id="admin-users-content">
            <div class="divide-y divide-stone-100 md:hidden">
              <div v-for="u in users" :key="u.id" class="space-y-3 px-4 py-4" :class="{ 'bg-red-50/50': u.locked }">
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <p class="break-all text-base font-semibold" :class="u.locked ? 'text-red-600' : 'text-stone-900'">{{ u.username }}</p>
                    <p class="mt-1 text-xs text-stone-500">ID: {{ u.id }} · {{ u.nickname || '-' }}</p>
                  </div>
                  <div class="flex shrink-0 flex-col items-end gap-1">
                    <span :class="u.role === 'ADMIN' ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-800'"
                          class="inline-flex rounded-full px-2 py-0.5 text-xs font-semibold">
                      {{ u.role === 'ADMIN' ? t('admin.adminRole') : t('admin.userRole') }}
                    </span>
                    <span v-if="u.locked" class="inline-flex rounded-full bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-700">
                      {{ t('admin.locked') }}
                    </span>
                  </div>
                </div>
                <div class="grid grid-cols-1 gap-2 rounded-lg bg-stone-50 p-3 text-xs text-stone-500">
                  <div>
                    <span class="font-medium text-stone-600">{{ t('admin.registeredAt') }}：</span>
                    <span v-if="u.createdAt">{{ formatDateTime(u.createdAt) }}</span>
                    <span v-else>-</span>
                  </div>
                  <div>
                    <span class="font-medium text-stone-600">{{ t('admin.password') }}：</span>
                    <span class="rounded bg-white px-2 py-1 font-mono" :title="t('admin.passwordHashTitle')">{{ t('admin.bcryptHash') }}</span>
                  </div>
                </div>
                <div class="flex flex-wrap gap-3 text-sm font-medium">
                  <button v-if="u.locked" @click="unlockUser(u)" class="text-green-600 hover:text-green-900">{{ t('admin.unlock') }}</button>
                  <button v-if="canChangeUserRole(u) && u.role !== 'ADMIN'" @click="updateRole(u.id, 'ADMIN')" class="text-blue-600 hover:text-blue-900">{{ t('admin.setAdmin') }}</button>
                  <button v-else-if="canChangeUserRole(u) && u.role === 'ADMIN'" @click="updateRole(u.id, 'USER')" class="text-orange-600 hover:text-orange-900">{{ t('admin.unsetAdmin') }}</button>
                  <button v-if="canDeleteUser(u)" @click="deleteUser(u)" class="text-red-600 hover:text-red-900">{{ t('common.delete') }}</button>
                  <span v-if="u.protectedAccount" class="text-gray-400">{{ t('admin.notOperable') }}</span>
                </div>
              </div>
            </div>
          <div class="hidden overflow-x-auto md:block">
            <div class="overflow-x-auto">
              <table class="min-w-full divide-y divide-stone-200">
                <thead class="bg-stone-50">
                  <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">ID</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.username') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.password') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.nickname') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.registeredAt') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.status') }}</th>
                  <th class="px-6 py-3 text-right text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.action') }}</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-stone-200">
                  <tr v-for="u in users" :key="u.id" class="hover:bg-stone-50" :class="{ 'bg-red-50/50': u.locked }">
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ u.id }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium" :class="u.locked ? 'text-red-600' : 'text-stone-900'">{{ u.username }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500 font-mono">
                      <div class="flex items-center gap-2">
                    <span class="text-xs bg-gray-100 px-2 py-1 rounded" :title="t('admin.passwordHashTitle')">
                      {{ t('admin.bcryptHash') }}
                        </span>
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm" :class="u.locked ? 'text-red-600' : 'text-stone-500'">{{ u.nickname || '-' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">
                      <span v-if="u.createdAt">{{ formatDateTime(u.createdAt) }}</span>
                      <span v-else class="text-stone-400">-</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <div class="flex items-center gap-1.5">
                        <span :class="u.role === 'ADMIN' ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-800'"
                              class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full">
                    {{ u.role === 'ADMIN' ? t('admin.adminRole') : t('admin.userRole') }}
                        </span>
                        <span v-if="u.locked" class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-700">
                          {{ t('admin.locked') }}
                        </span>
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button v-if="u.locked" @click="unlockUser(u)" class="text-green-600 hover:text-green-900 mr-4">{{ t('admin.unlock') }}</button>
                    <button v-if="canChangeUserRole(u) && u.role !== 'ADMIN'" @click="updateRole(u.id, 'ADMIN')" class="text-blue-600 hover:text-blue-900 mr-4">{{ t('admin.setAdmin') }}</button>
                    <button v-else-if="canChangeUserRole(u) && u.role === 'ADMIN'" @click="updateRole(u.id, 'USER')" class="text-orange-600 hover:text-orange-900 mr-4">{{ t('admin.unsetAdmin') }}</button>
                    <button v-if="canDeleteUser(u)" @click="deleteUser(u)" class="text-red-600 hover:text-red-900">{{ t('common.delete') }}</button>
                    <span v-if="u.protectedAccount" class="text-gray-400 cursor-not-allowed">{{ t('admin.notOperable') }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        </div>
        <AdminCommunityPanel />
        <AdminHeritagePanel />
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div
            class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
            role="button"
            tabindex="0"
            :aria-expanded="showAllNews"
            aria-controls="admin-news-content"
            :aria-label="t('admin.newsManagement')"
            @click="toggleNewsPanel"
            @keydown.enter="activateKeyboardPanel($event, toggleNewsPanel)"
            @keydown.space="activateKeyboardPanel($event, toggleNewsPanel)"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-cyan-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-cyan-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">{{ t('admin.newsManagement') }} <span class="text-sm font-normal text-stone-400">({{ newsList.length }}{{ t('common.items') }})</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="fetchNews" :disabled="loadingNews" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">
                {{ loadingNews ? t('common.loading') : t('common.refresh') }}
              </button>
              <button @click.stop="openCreateNewsModal" class="text-xs px-3 py-1.5 rounded-lg bg-cyan-500 text-white hover:bg-cyan-600 transition-colors">{{ t('admin.createNews') }}</button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showAllNews }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
              </svg>
            </div>
          </div>
          
          <!-- Loading State -->
          <div v-if="loadingNews" class="p-8 text-center text-stone-500">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
            <p>{{ t('admin.loadingNews') }}</p>
          </div>
          
          <!-- News List (Collapsible) -->
          <div v-else-if="showAllNews" id="admin-news-content" class="divide-y divide-stone-200">
            <div v-for="news in newsList" :key="news.id" class="px-4 py-4 transition-colors hover:bg-stone-50 sm:px-6">
              <div class="flex flex-col gap-4 sm:flex-row sm:items-start">
                <div class="h-40 w-full flex-shrink-0 overflow-hidden rounded-lg bg-gray-200 sm:h-24 sm:w-24">
                  <img v-if="news.imageUrl" :src="textOrEmpty(news.imageUrl)" :alt="textOrEmpty(news.title)" class="w-full h-full object-cover">
                  <div v-else class="w-full h-full flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600 text-white text-2xl font-bold">
                    {{ news.title?.charAt(0) || 'N' }}
                  </div>
                </div>
                <div class="flex-1 min-w-0 flex flex-col">
                  <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                    <div class="flex-1 min-w-0">
                      <h4 class="text-lg font-bold text-stone-800 mb-1 line-clamp-1">{{ news.title }}</h4>
                      <p class="text-sm text-stone-600 line-clamp-2 mb-2">{{ news.content }}</p>
                      <div class="flex items-center flex-wrap gap-2 text-xs text-stone-500">
                        <span :class="getCategoryClass(news.category)" class="px-2 py-1 rounded-full font-medium whitespace-nowrap">
                          {{ getCategoryLabel(news.category) }}
                        </span>
                        <span class="whitespace-nowrap">{{ t('admin.viewCount', { count: news.viewCount || 0 }) }}</span>
                        <span class="whitespace-nowrap">{{ formatDate(news.createdAt) }}</span>
                      </div>
                    </div>
                    <div class="flex flex-shrink-0 gap-4 sm:gap-2">
                      <button @click="openEditNewsModal(news)" class="text-blue-600 hover:text-blue-800 text-sm font-medium whitespace-nowrap">
                        {{ t('common.edit') }}
                      </button>
                      <button @click="deleteNewsItem(news.id)" class="text-red-600 hover:text-red-800 text-sm font-medium whitespace-nowrap">
                        {{ t('common.delete') }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="newsList.length === 0" class="px-6 py-8 text-center text-stone-500">
              {{ t('admin.noNews') }}
            </div>
          </div>
          <div v-else-if="!loadingNews && newsList.length > 0" class="px-6 py-4 text-center text-stone-500 text-sm">
            {{ t('admin.expandNews', { count: newsList.length }) }}
          </div>
        </div>

        <!-- Carousel Management Section -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div
            class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
            role="button"
            tabindex="0"
            :aria-expanded="showCarousels"
            aria-controls="admin-carousels-content"
            :aria-label="t('admin.carouselManagement')"
            @click="toggleCarouselsPanel"
            @keydown.enter="activateKeyboardPanel($event, toggleCarouselsPanel)"
            @keydown.space="activateKeyboardPanel($event, toggleCarouselsPanel)"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-pink-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-pink-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">{{ t('admin.carouselManagement') }} <span class="text-sm font-normal text-stone-400">({{ carousels.length }}{{ t('common.imagesUnit') }})</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="openCreateCarouselModal" class="text-xs px-3 py-1.5 rounded-lg bg-pink-500 text-white hover:bg-pink-600 transition-colors">{{ t('admin.addCarousel') }}</button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showCarousels }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
            </div>
          </div>
          <div v-if="showCarousels" id="admin-carousels-content" class="p-6">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="c in carousels" :key="c.id" class="border rounded-lg overflow-hidden">
                <img :src="textOrEmpty(c.imageUrl)" class="w-full h-32 object-cover">
                <div class="p-3">
                  <h4 class="font-bold text-sm">{{ c.title }}</h4>
                  <p class="text-xs text-stone-500">{{ c.subtitle }}</p>
                  <div class="flex justify-between items-center mt-2">
                    <span :class="c.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'" class="text-xs px-2 py-0.5 rounded">{{ c.active ? t('common.enabled') : t('common.disabled') }}</span>
                    <div class="space-x-2">
                      <button @click="openEditCarouselModal(c)" class="text-blue-600 text-xs">{{ t('common.edit') }}</button>
                      <button @click="deleteCarousel(c.id)" class="text-red-600 text-xs">{{ t('common.delete') }}</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Route Management Section -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div
            class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
            role="button"
            tabindex="0"
            :aria-expanded="showRoutes"
            aria-controls="admin-routes-content"
            :aria-label="t('admin.routeManagement')"
            @click="toggleRoutesPanel"
            @keydown.enter="activateKeyboardPanel($event, toggleRoutesPanel)"
            @keydown.space="activateKeyboardPanel($event, toggleRoutesPanel)"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-teal-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-teal-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">{{ t('admin.routeManagement') }} <span class="text-sm font-normal text-stone-400">({{ adminRoutes.length }}{{ t('common.items') }})</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="fetchAdminRoutes" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">{{ t('common.refresh') }}</button>
              <button @click.stop="openCreateRouteModal" class="text-xs px-3 py-1.5 rounded-lg bg-teal-500 text-white hover:bg-teal-600 transition-colors">{{ t('admin.addNew') }}</button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showRoutes }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
            </div>
          </div>
          <div v-if="showRoutes" id="admin-routes-content" class="overflow-x-auto">
            <table class="min-w-full divide-y divide-stone-200">
              <thead class="bg-stone-50">
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.titleLabel') }}</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.routeSource') }}</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.days') }}</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.communityMeta') }}</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.content') }}</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.routeStats') }}</th>
                  <th class="px-4 py-3 text-right text-xs font-medium text-stone-500 uppercase">{{ t('admin.action') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-200">
                <tr v-for="r in adminRoutes" :key="r.id" class="hover:bg-stone-50">
                  <td class="px-4 py-3 text-sm font-medium text-stone-800 max-w-[220px]">
                    <span class="block truncate">{{ r.title || r.name }}</span>
                    <span class="text-xs font-normal text-stone-400">ID: {{ r.id }}</span>
                  </td>
                  <td class="px-4 py-3 text-sm text-stone-600">
                    <span
                      class="inline-flex px-2 py-1 rounded-full text-xs font-medium"
                      :class="r.sourceType === 'OFFICIAL' ? 'bg-teal-50 text-teal-700' : 'bg-blue-50 text-blue-700'"
                    >
                      {{ routeSourceLabel(r) }}
                    </span>
                    <div class="text-xs text-stone-400 mt-1">{{ routeAuthorLabel(r) }}</div>
                  </td>
                  <td class="px-4 py-3 text-sm">{{ t('admin.daysValue', { count: r.days }) }}</td>
                  <td class="px-4 py-3 text-sm text-stone-500">
                    <div>{{ r.budget || '-' }} · {{ r.preference || '-' }}</div>
                    <div class="text-xs text-stone-400">
                      {{ routePriceLabel(r) }} · {{ routeDifficultyLabel(r.difficulty) }}
                    </div>
                  </td>
                  <td class="px-4 py-3 text-sm text-stone-600 max-w-[320px]">{{ routePreview(r) }}</td>
                  <td class="px-4 py-3 text-sm text-stone-500">
                    {{ t('admin.communityCounts', { views: r.viewCount || 0, likes: r.likeCount || 0, comments: r.commentCount || 0 }) }}
                  </td>
                  <td class="px-4 py-3 text-right text-sm space-x-2">
                    <button @click="openEditRouteModal(r)" class="text-blue-600">{{ t('common.edit') }}</button>
                    <button @click="deleteRoute(r.id)" class="text-red-600">{{ t('common.delete') }}</button>
                  </td>
                </tr>
                <tr v-if="adminRoutes.length === 0">
                  <td colspan="7" class="px-4 py-8 text-center text-stone-500">{{ t('admin.noCommunityRoutes') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Hotel Management Section -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div
            class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
            role="button"
            tabindex="0"
            :aria-expanded="showHotels"
            aria-controls="admin-hotels-content"
            :aria-label="t('admin.hotelManagement')"
            @click="toggleHotelsPanel"
            @keydown.enter="activateKeyboardPanel($event, toggleHotelsPanel)"
            @keydown.space="activateKeyboardPanel($event, toggleHotelsPanel)"
          >
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-indigo-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">{{ t('admin.hotelManagement') }} <span class="text-sm font-normal text-stone-400">({{ adminHotels.length }}{{ t('common.hotelsUnit') }})</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="fetchAdminHotels" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">{{ t('common.refresh') }}</button>
              <button @click.stop="openCreateHotelModal" class="text-xs px-4 py-2 rounded-lg bg-indigo-500 text-white hover:bg-indigo-600 transition-colors shadow-sm">{{ t('admin.addHotel') }}</button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showHotels }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
            </div>
          </div>

          <!-- Card Grid -->
          <div v-if="showHotels" id="admin-hotels-content" class="p-4 grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div v-for="h in adminHotels" :key="h.id"
                 class="bg-white rounded-xl border border-stone-200 overflow-hidden hover:shadow-md transition-all duration-200"
                 :class="{ 'ring-2 ring-indigo-200 shadow-md': expandedHotelId === h.id }">
              <!-- Card Header (always visible) -->
              <div
                class="flex gap-4 p-4 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-300"
                role="button"
                tabindex="0"
                :aria-expanded="expandedHotelId === h.id"
                :aria-controls="`admin-hotel-${h.id}-rooms`"
                :aria-label="`${t('admin.roomManagement')} ${h.name || ''}`.trim()"
                @click="toggleHotelExpand(h)"
                @keydown.enter="activateKeyboardPanel($event, () => toggleHotelExpand(h))"
                @keydown.space="activateKeyboardPanel($event, () => toggleHotelExpand(h))"
              >
                <!-- Thumbnail -->
                <div class="w-24 h-24 shrink-0 rounded-lg overflow-hidden bg-stone-100">
                  <img v-if="h.imageUrl && !failedHotelImages[h.id]" :src="textOrEmpty(h.imageUrl)" :alt="textOrEmpty(h.name)"
                       class="w-full h-full object-cover"
                       @error="failedHotelImages[h.id] = true" />
                  <div v-if="!h.imageUrl || failedHotelImages[h.id]" class="w-full h-full flex items-center justify-center text-stone-300">
                    <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
                  </div>
                </div>
                <!-- Info -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between gap-2">
                    <h4 class="font-semibold text-stone-800 truncate">{{ h.name }}</h4>
                    <span class="shrink-0 inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 text-xs font-medium">
                      ★ {{ h.rating || '-' }}
                    </span>
                  </div>
                  <div class="mt-1.5 space-y-1 text-xs text-stone-500">
                    <div class="flex items-center gap-1">
                      <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
                      <span class="truncate">{{ h.location || t('common.unknownLocation') }}</span>
                    </div>
                    <div v-if="h.phone" class="flex items-center gap-1">
                      <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/></svg>
                      {{ h.phone }}
                    </div>
                  </div>
                  <div class="mt-2 flex items-center gap-2">
                    <span class="text-xs font-semibold text-red-600 bg-red-50 px-2 py-0.5 rounded">{{ h.priceRange || t('common.consult') }}</span>
                    <span v-if="h.facilities" class="text-xs text-stone-400 truncate">{{ h.facilities.split(',')[0] }}{{ h.facilities.split(',').length > 1 ? '...' : '' }}</span>
                  </div>
                </div>
                <!-- Expand chevron -->
                <div class="shrink-0 self-center text-stone-300 transition-transform duration-200" :class="{ 'rotate-180': expandedHotelId === h.id }">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
                </div>
              </div>

              <!-- Expanded Room Type Management -->
              <div v-if="expandedHotelId === h.id" :id="`admin-hotel-${h.id}-rooms`" class="border-t border-stone-100 bg-stone-50/50 px-4 py-3 animate-slide-up">
                <div class="flex items-center justify-between mb-3">
                  <span class="text-xs font-semibold text-stone-500 uppercase tracking-wide">{{ t('admin.roomManagement') }}</span>
                  <span class="text-xs text-stone-400">{{ t('admin.roomTypesCount', { count: expandedRoomTypes.length }) }}</span>
                </div>

                <!-- Room type list -->
                <div class="space-y-2 mb-3">
                  <div v-if="loadingRoomTypes" class="text-center py-2 text-xs text-stone-400">{{ t('common.loading') }}</div>
                  <div v-else-if="expandedRoomTypes.length === 0" class="text-center py-2 text-xs text-stone-400">{{ t('admin.noRoomTypes') }}</div>
                  <div v-else v-for="rt in expandedRoomTypes" :key="rt.id"
                       class="flex items-center justify-between bg-white rounded-lg border border-stone-200 px-3 py-2.5 text-sm">
                    <div class="flex-1 min-w-0">
                      <span class="font-medium text-stone-700">{{ rt.name }}</span>
                      <span class="ml-2 text-xs text-stone-400">{{ t('admin.pricePerNightWithCapacity', { price: rt.price, capacity: rt.capacity }) }}</span>
                      <span v-if="rt.amenities" class="ml-2 text-xs text-stone-300">· {{ rt.amenities }}</span>
                    </div>
                    <button @click="deleteRoomTypeInline(rt.id, h.id)" class="shrink-0 ml-2 text-xs text-red-400 hover:text-red-600 transition-colors">{{ t('common.delete') }}</button>
                  </div>
                </div>

                <!-- Add room type form -->
                <div class="flex items-center gap-1.5 bg-white rounded-lg border border-stone-200 px-2.5 py-2">
                  <input v-model="roomTypeForm.name" :placeholder="t('admin.roomNamePlaceholder')" class="flex-1 min-w-0 text-xs border-0 outline-none px-1" @keyup.enter="addRoomTypeInline(h.id)">
                  <input v-model.number="roomTypeForm.price" type="number" placeholder="¥" class="w-14 text-xs border-0 outline-none text-right px-1" @keyup.enter="addRoomTypeInline(h.id)">
                  <input v-model.number="roomTypeForm.capacity" type="number" :placeholder="t('admin.capacityPlaceholder')" class="w-8 text-xs border-0 outline-none text-right px-1" @keyup.enter="addRoomTypeInline(h.id)">
                  <button @click="addRoomTypeInline(h.id)" class="shrink-0 text-xs px-2.5 py-1.5 rounded-md bg-green-500 text-white hover:bg-green-600 transition-colors">+</button>
                </div>
              </div>

              <!-- Card Actions -->
              <div class="flex border-t border-stone-100 divide-x divide-stone-100">
                <button @click="openEditHotelModal(h)"
                        class="flex-1 py-2.5 text-xs text-stone-500 hover:text-blue-600 hover:bg-blue-50 transition-colors flex items-center justify-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                  {{ t('common.edit') }}
                </button>
                <button @click="toggleHotelExpand(h)"
                        class="flex-1 py-2.5 text-xs text-stone-500 hover:text-indigo-600 hover:bg-indigo-50 transition-colors flex items-center justify-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
                  {{ expandedHotelId === h.id ? t('common.collapse') : t('admin.roomTypes') }}
                </button>
                <button @click="deleteHotel(h.id)"
                        class="flex-1 py-2.5 text-xs text-stone-500 hover:text-red-600 hover:bg-red-50 transition-colors flex items-center justify-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                  {{ t('common.delete') }}
                </button>
              </div>
            </div>

            <!-- Empty state -->
            <div v-if="adminHotels.length === 0" class="col-span-full text-center py-16">
              <svg class="w-16 h-16 text-stone-200 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
              <p class="text-stone-400">{{ t('admin.noHotels') }}</p>
              <button @click="openCreateHotelModal" class="mt-3 text-sm text-indigo-500 hover:text-indigo-700">{{ t('admin.addFirstHotel') }}</button>
            </div>
          </div>
          <div v-else class="px-6 py-4 text-center text-stone-500 text-sm">
            {{ t('admin.expandHotels', { count: adminHotels.length }) }}
          </div>
        </div>

        <!-- Carousel Edit/Create Modal -->
        <MotionModal
          :show="showCarouselModal"
          modal-key="admin-carousel-modal"
          panel-class="max-w-lg rounded-2xl bg-white p-4 sm:p-6"
          @close="closeCarouselModal"
        >
            <h2 class="text-xl font-bold mb-4">{{ t('admin.editOrCreateCarousel', { mode: editingCarousel.id ? t('common.edit') : t('common.create') }) }}</h2>
            <div class="space-y-3">
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.titleLabel') }} *</label><input v-model="carouselForm.title" class="w-full border rounded px-3 py-2" :placeholder="t('admin.titleLabel')"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.subtitleLabel') }}</label><input v-model="carouselForm.subtitle" class="w-full border rounded px-3 py-2" :placeholder="t('admin.subtitleLabel')"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.tagLabel') }}</label><input v-model="carouselForm.tag" class="w-full border rounded px-3 py-2"></div>
              <div>
                <label class="block text-sm font-medium mb-1">{{ t('admin.image') }}</label>
                <ImageUploadField v-model="carouselForm.imageUrl" :upload-endpoint="endpoints.admin.uploadImage" placeholder="/images/banner.jpg" />
              </div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.linkUrl') }}</label><input v-model="carouselForm.linkUrl" class="w-full border rounded px-3 py-2" placeholder="/spots"></div>
              <div class="flex items-center gap-2"><label class="text-sm font-medium">{{ t('admin.sortOrder') }}</label><input v-model.number="carouselForm.sortOrder" type="number" class="border rounded px-2 py-1 w-20"></div>
              <div class="flex items-center gap-2"><input v-model="carouselForm.active" type="checkbox" id="carousel-active"><label for="carousel-active" class="text-sm">{{ t('common.enabled') }}</label></div>
            </div>
            <div class="flex space-x-3 mt-6">
              <button @click="saveCarousel" class="flex-1 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600">{{ t('common.save') }}</button>
              <button @click="closeCarouselModal" class="flex-1 bg-stone-200 py-2 rounded-lg">{{ t('common.cancel') }}</button>
            </div>
        </MotionModal>

        <!-- Route Edit/Create Modal -->
        <MotionModal
          :show="showRouteModal"
          modal-key="admin-route-modal"
          panel-class="max-w-2xl rounded-2xl bg-white p-4 sm:p-6 max-h-[88dvh] overflow-y-auto"
          @close="closeRouteModal"
        >
            <h2 class="text-xl font-bold mb-4">{{ t('admin.editOrCreateRoute', { mode: editingRoute.id ? t('common.edit') : t('common.create') }) }}</h2>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.titleLabel') }} *</label><input v-model="routeForm.title" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.days') }}</label><input v-model.number="routeForm.days" type="number" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.budget') }}</label>
                <select v-model="routeForm.budget" class="w-full border rounded px-3 py-2">
                  <option value="">{{ t('common.unknown') }}</option>
                  <option v-for="option in adminBudgetOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.preference') }}</label>
                <select v-model="routeForm.preference" class="w-full border rounded px-3 py-2">
                  <option value="">{{ t('common.unknown') }}</option>
                  <option v-for="option in adminPreferenceOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.price') }}</label><input v-model.number="routeForm.price" type="number" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.difficulty') }}</label>
                <select v-model="routeForm.difficulty" class="w-full border rounded px-3 py-2">
                  <option value="">{{ t('common.unknown') }}</option>
                  <option value="EASY">{{ t('admin.easy') }}</option><option value="MEDIUM">{{ t('admin.medium') }}</option><option value="HARD">{{ t('admin.hard') }}</option>
                </select>
              </div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.temperature') }}</label><input v-model="routeForm.temperature" class="w-full border rounded px-3 py-2" :placeholder="t('admin.temperaturePlaceholder')"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.geography') }}</label><input v-model="routeForm.geography" class="w-full border rounded px-3 py-2" :placeholder="t('admin.geographyPlaceholder')"></div>
              <div class="sm:col-span-2"><label class="block text-sm font-medium mb-1">{{ t('admin.content') }}</label><textarea v-model="routeForm.content" rows="6" class="w-full border rounded px-3 py-2"></textarea></div>
            </div>
            <div class="mt-6 flex flex-col gap-3 sm:flex-row">
              <button @click="saveRoute" class="flex-1 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600">{{ t('common.save') }}</button>
              <button @click="closeRouteModal" class="flex-1 bg-stone-200 py-2 rounded-lg">{{ t('common.cancel') }}</button>
            </div>
        </MotionModal>

        <!-- Hotel Edit/Create Modal -->
        <MotionModal
          :show="showHotelModal"
          modal-key="admin-hotel-modal"
          panel-class="max-w-2xl rounded-2xl bg-white p-4 sm:p-6 max-h-[88dvh] overflow-y-auto"
          @close="closeHotelModal"
        >
            <h2 class="text-xl font-bold mb-4">{{ t('admin.editOrCreateHotel', { mode: editingHotel.id ? t('common.edit') : t('common.create') }) }}</h2>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.name') }} *</label><input v-model="hotelForm.name" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.location') }}</label><input v-model="hotelForm.location" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.phone') }}</label><input v-model="hotelForm.phone" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.priceRange') }}</label><input v-model="hotelForm.priceRange" class="w-full border rounded px-3 py-2" placeholder="¥500 - ¥1500"></div>
              <div><label class="block text-sm font-medium mb-1">{{ t('admin.rating') }}</label><input v-model.number="hotelForm.rating" type="number" step="0.1" class="w-full border rounded px-3 py-2"></div>
              <div class="sm:col-span-2">
                <label class="block text-sm font-medium mb-1">{{ t('admin.image') }}</label>
                <ImageUploadField v-model="hotelForm.imageUrl" :upload-endpoint="endpoints.admin.uploadImage" />
              </div>
              <div class="sm:col-span-2"><label class="block text-sm font-medium mb-1">{{ t('admin.facilities') }}</label><input v-model="hotelForm.facilities" class="w-full border rounded px-3 py-2" :placeholder="t('admin.facilitiesPlaceholder')"></div>
            </div>
            <div class="mt-6 flex flex-col gap-3 sm:flex-row">
              <button @click="saveHotel" class="flex-1 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600">{{ t('common.save') }}</button>
              <button @click="closeHotelModal" class="flex-1 bg-stone-200 py-2 rounded-lg">{{ t('common.cancel') }}</button>
            </div>
        </MotionModal>
      </div>
    </div>

    <!-- Edit/Create News Modal -->
    <MotionModal
      :show="showNewsModal"
      modal-key="admin-news-modal"
      panel-class="max-w-3xl rounded-2xl bg-white p-4 sm:p-8 max-h-[90dvh] overflow-y-auto"
      @close="closeNewsModal"
    >
        <h2 class="text-xl font-bold mb-5 text-stone-800 sm:text-2xl sm:mb-6">{{ editingNews.id ? t('admin.editNewsTitle') : t('admin.createNewsTitle') }}</h2>
        
        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.titleLabel') }} <span class="text-red-500">*</span></label>
          <input v-model="newsForm.title" type="text" required
                 class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                 :placeholder="t('admin.titlePlaceholder')">
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.content') }} <span class="text-red-500">*</span></label>
          <textarea v-model="newsForm.content" rows="8" required
                    class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-none"
                    :placeholder="t('admin.contentPlaceholder')"></textarea>
          <p class="text-xs text-stone-500 mt-2">{{ t('admin.currentWordCount', { count: newsForm.content.length }) }}</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.category') }} <span class="text-red-500">*</span></label>
          <select v-model="newsForm.category" required
                  class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none">
            <option value="">{{ t('admin.selectCategory') }}</option>
            <option value="POLICY">{{ t('admin.categoryPolicy') }}</option>
            <option value="EVENT">{{ t('admin.categoryEvent') }}</option>
            <option value="NOTICE">{{ t('admin.categoryNotice') }}</option>
          </select>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.image') }}</label>
          <ImageUploadField v-model="newsForm.imageUrl" :upload-endpoint="endpoints.admin.uploadImage" />
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.views') }}</label>
          <input v-model.number="newsForm.viewCount" type="number" min="0"
                 class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                 placeholder="0">
        </div>

        <div class="flex flex-col gap-3 sm:flex-row sm:space-x-4">
          <button @click="saveNews" :disabled="updatingNews || !newsForm.title || !newsForm.content || !newsForm.category"
                  class="flex-1 bg-blue-500 text-white py-3 rounded-lg hover:bg-blue-600 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed">
            {{ updatingNews ? t('admin.saving') : (editingNews.id ? t('admin.saveEditing') : t('admin.createNewsTitle')) }}
          </button>
          <button @click="closeNewsModal" :disabled="updatingNews"
                  class="flex-1 bg-stone-200 text-stone-700 py-3 rounded-lg hover:bg-stone-300 transition-colors disabled:cursor-not-allowed">
            {{ t('common.cancel') }}
          </button>
        </div>
    </MotionModal>

    <!-- Edit Spot Modal -->
    <MotionModal
      :show="showEditModal"
      modal-key="admin-spot-modal"
      panel-class="max-w-2xl rounded-2xl bg-white p-4 sm:p-8 max-h-[88dvh] overflow-y-auto"
      @close="closeEditModal"
    >
        <h2 class="text-xl font-bold mb-5 text-stone-800 sm:text-2xl sm:mb-6">{{ t('admin.editSpotInfo') }}</h2>
        
        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.spotName') }}</label>
          <p class="text-lg font-bold text-stone-900">{{ editingSpot.name }}</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.currentCover') }}</label>
          <div class="relative h-44 bg-gray-200 rounded-lg overflow-hidden mb-4 sm:h-64">
            <img v-if="editingSpot.imageUrl" :src="textOrEmpty(editingSpot.imageUrl)" :alt="textOrEmpty(editingSpot.name)" class="w-full h-full object-cover">
            <div v-else class="w-full h-full flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600 text-white text-6xl font-bold">
              {{ editingSpot.name?.charAt(0) }}
            </div>
          </div>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.newImage') }}</label>
          <ImageUploadField v-model="newImageUrl" :upload-endpoint="endpoints.admin.uploadImage" />
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.ticketPriceYuan') }}</label>
          <div class="flex items-center space-x-2">
            <span class="text-stone-500 text-sm">{{ t('admin.current') }}</span>
            <span class="font-semibold text-red-600 mr-4">¥{{ editingSpot.ticketPrice }}</span>
          </div>
          <input
            v-model.number="newTicketPrice"
            type="number"
            min="0"
            step="0.01"
            class="mt-2 w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            :placeholder="t('admin.ticketPlaceholder')"
          >
          <p class="text-xs text-stone-500 mt-2">{{ t('admin.ticketHint') }}</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.spotIntro') }}</label>
          <textarea v-model="newDescription" rows="6"
                    class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-none"
                    :placeholder="t('admin.spotIntroPlaceholder')"></textarea>
          <p class="text-xs text-stone-500 mt-2">{{ t('admin.currentWordCount', { count: newDescription.length }) }}</p>
        </div>

        <div class="flex flex-col gap-3 sm:flex-row sm:space-x-4">
          <button @click="updateSpotImage" :disabled="updating"
                  class="flex-1 bg-blue-500 text-white py-3 rounded-lg hover:bg-blue-600 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed">
            {{ updating ? t('admin.saving') : t('admin.saveEditing') }}
          </button>
          <button @click="closeEditModal" :disabled="updating"
                  class="flex-1 bg-stone-200 text-stone-700 py-3 rounded-lg hover:bg-stone-300 transition-colors disabled:cursor-not-allowed">
            {{ t('common.cancel') }}
          </button>
        </div>
    </MotionModal>
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent, ref, computed, onMounted, onUnmounted } from 'vue'
import { isAxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AdminCommunityPanel from '../components/AdminCommunityPanel.vue'
import AdminHeritagePanel from '../components/AdminHeritagePanel.vue'
import AdminSecurityPosturePanel from '../components/AdminSecurityPosturePanel.vue'
import ImageUploadField from '../components/ImageUploadField.vue'
import MotionModal from '../components/motion/MotionModal.vue'
import api, { endpoints, clearTokenCache, type AdminUserSummary, type SecurityPostureResponse } from '../api'
import { useAuthStore } from '../stores/auth'
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'

interface Stats {
  userCount: number
  orderCount: number
  totalRevenue: number
  recentBookings: AdminRecentOrder[]
  recentHotelBookings: AdminRecentOrder[]
  popularSpots: AdminSpot[]
  spotCount?: number
  newsCount?: number
  monthlyBookingTrend?: Array<{ month: string; orderCount: number; revenue: number }>
  userGrowthTrend?: Array<{ month: string; count: number }>
  newsPublishTrend?: Array<{ month: string; count: number }>
  spotCategories?: Array<{ name: string; value: number }>
  visitorCityDistribution?: Array<{ name: string; value: number }>
  updatedAt?: string
}

interface AdminRecentOrder {
  id: number | string
  spot?: {
    id?: number
    name?: string | null
  } | null
  hotel?: {
    id?: number
    name?: string | null
  } | null
  totalPrice?: number | string | null
  status: string
  createdAt?: string | null
  _type?: 'scenic' | 'hotel'
}

interface HotelOrder {
  id: number
  hotel?: {
    id?: number
    name?: string | null
    location?: string | null
    imageUrl?: string | null
  } | null
  hotelName?: string | null
  roomName?: string | null
  checkInDate?: string | null
  checkOutDate?: string | null
  guests?: number | string | null
  nights?: number | string | null
  guestName?: string | null
  phone?: string | null
  note?: string | null
  totalPrice?: number | string | null
  status: string
  createdAt?: string | null
}

interface AdminSpot {
  id: number
  name?: string | null
  city?: string | null
  location?: string | null
  imageUrl?: string | null
  ticketPrice?: number | string | null
  visitCount?: number | null
  description?: string | null
}

interface AdminNewsItem {
  id: number
  title?: string | null
  content?: string | null
  category?: string | null
  imageUrl?: string | null
  viewCount?: number | null
  createdAt?: string | null
}

interface AdminCarouselItem {
  id: number
  title?: string | null
  subtitle?: string | null
  tag?: string | null
  imageUrl?: string | null
  linkUrl?: string | null
  sortOrder?: number | null
  active?: boolean | null
}

interface AdminRouteItem {
  id: number
  title?: string | null
  name?: string | null
  sourceType?: string | null
  author?: {
    username?: string | null
    nickname?: string | null
  } | null
  days?: number | string | null
  budget?: string | null
  preference?: string | null
  price?: number | string | null
  difficulty?: string | null
  content?: string | null
  description?: string | null
  temperature?: string | null
  geography?: string | null
  viewCount?: number | null
  likeCount?: number | null
  commentCount?: number | null
}

interface AdminHotelItem {
  id: number
  name?: string | null
  location?: string | null
  phone?: string | null
  priceRange?: string | null
  rating?: number | string | null
  imageUrl?: string | null
  facilities?: string | null
}

interface AdminRoomType {
  id: number
  name?: string | null
  price?: number | string | null
  capacity?: number | string | null
  amenities?: string | null
}

interface PriceBatchJob {
  jobId?: string | null
  status?: 'RUNNING' | 'COMPLETED' | 'FAILED' | string
  total?: number | null
  processed?: number | null
  success?: number | null
  failed?: number | null
  skipped?: number | null
  currentSpot?: string | null
  errorMessage?: string | null
}

interface SpotPriceFetchResponse {
  success?: boolean
  priceInfo?: {
    basePrice?: number | string | null
    peakSeasonPrice?: number | string | null
  } | null
}

type SpotUpdatePayload = Partial<Pick<AdminSpot, 'imageUrl' | 'description' | 'ticketPrice'>>
type NewsPayload = Pick<Required<AdminNewsItem>, 'title' | 'content' | 'category'> & Pick<AdminNewsItem, 'imageUrl' | 'viewCount'>

const { t, locale, te } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const { showConfirm } = useConfirm()
const { showToast } = useToast()
const AdminAnalyticsPanel = defineAsyncComponent(() => import('../components/AdminAnalyticsPanel.vue'))

const confirmDanger = (message: string) => showConfirm({ message, tone: 'danger' })
const apiErrorMessage = (error: unknown, fallback: string) => safeClientErrorMessage(error, fallback)
const apiErrorStatus = (error: unknown) => isAxiosError(error) ? error.response?.status : undefined
const isNetworkClientError = (error: unknown) => isAxiosError(error) && Boolean(error.request)
const textOrEmpty = (value?: string | null) => value ?? ''
const numberOrNull = (value: unknown) => {
  if (value === null || value === undefined || value === '') return null
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue : null
}
const numberOrDefault = (value: unknown, fallback: number) => numberOrNull(value) ?? fallback
const reportAdminListLoadFailure = (resource: string, error: unknown) => {
  console.error(`Failed to fetch ${resource}:`, summarizeClientError(error))
  showToast(apiErrorMessage(error, '列表加载失败，请稍后重试'), 'error')
}
const activateKeyboardPanel = (event: KeyboardEvent, action: () => void | Promise<void>) => {
  if (event.target !== event.currentTarget) return
  event.preventDefault()
  void action()
}

const stats = ref<Stats>({
  userCount: 0,
  orderCount: 0,
  totalRevenue: 0,
  recentBookings: [],
  recentHotelBookings: [],
  popularSpots: []
})
const loading = ref(true)
const analyticsError = ref('')
const analyticsData = ref<Stats | null>(null)
const securityPosture = ref<SecurityPostureResponse | null>(null)
const loadingSecurityPosture = ref(false)
const securityPostureError = ref('')
let operationalRefreshTimer: number | null = null

const fetchSecurityPosture = async () => {
  loadingSecurityPosture.value = true
  securityPostureError.value = ''
  try {
    const response = await api.get<SecurityPostureResponse>(endpoints.admin.securityPosture)
    securityPosture.value = response.data
  } catch (error: unknown) {
    console.error('Failed to fetch security posture:', summarizeClientError(error))
    securityPostureError.value = safeClientErrorMessage(error, t('admin.securityPosture.loadFailed'))
  } finally {
    loadingSecurityPosture.value = false
  }
}

const fetchStats = async () => {
  try {
    if (!(await auth.ensureSession())) {
      await router.push('/login')
      return
    }

    if (!auth.isAdmin) {
      showToast(t('admin.unauthorizedAdmin'), 'warning')
      await router.push('/')
      return
    }

    analyticsError.value = ''
    const response = await api.get<Stats>(endpoints.admin.stats)
    stats.value = response.data
    analyticsData.value = response.data
  } catch (error: unknown) {
    console.error('Failed to fetch admin stats:', summarizeClientError(error))
    const status = apiErrorStatus(error)
    if (status === 401) {
      auth.logout()
      clearTokenCache()
      await router.push('/login')
      return
    }
    if (status === 403) {
      showToast(t('admin.forbiddenAdmin'), 'warning')
      await router.push('/')
    } else {
      analyticsError.value = safeClientErrorMessage(error, t('admin.analyticsLoadFailed'))
    }
  } finally {
    loading.value = false
  }
}

const activeDateLocale = computed(() => locale.value === 'bo' ? 'bo-CN' : 'zh-CN')

const formatDate = (dateStr?: string | null) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString(activeDateLocale.value)
}

const formatDateTime = (dateStr?: string | null) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString(activeDateLocale.value, {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusClass = (status: string) => {
  switch (status) {
    case 'CONFIRMED': return 'bg-green-100 text-green-800'
    case 'PENDING': return 'bg-yellow-100 text-yellow-800'
    case 'CANCELLED': return 'bg-red-100 text-red-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getStatusLabel = (status: string) => {
  const key = `admin.statusLabel.${status}`
  return te(key) ? t(key) : status
}

const normalizeDisplayText = (value: unknown) => String(value ?? '').trim()

const displayHotelOrderGuest = (order: HotelOrder) =>
  normalizeDisplayText(order.guestName) || t('common.unknown')

const maskHotelOrderPhone = (phone: unknown) => {
  const digits = normalizeDisplayText(phone).replace(/\D/g, '')
  if (!digits) return t('common.unknown')
  if (digits.length <= 4) return '*'.repeat(Math.max(4, digits.length))

  const prefix = digits.length >= 7 ? `${digits.slice(0, 3)} ` : ''
  return `${prefix}**** ${digits.slice(-4)}`
}

const displayHotelOrderName = (order: HotelOrder) =>
  normalizeDisplayText(order.hotel?.name || order.hotelName) || t('admin.unknownHotel')

const displayHotelOrderRoom = (order: HotelOrder) =>
  normalizeDisplayText(order.roomName) || t('common.unknown')

const displayHotelOrderDate = (date: unknown) =>
  normalizeDisplayText(date) || t('common.unknown')

const displayHotelOrderDateTime = (date: unknown) => {
  const dateText = normalizeDisplayText(date)
  if (!dateText) return t('common.unknown')

  const formattedDate = formatDateTime(dateText)
  return formattedDate === '-' ? t('common.unknown') : formattedDate
}

const formatHotelOrderCount = (value: unknown, unitKey: string) => {
  const count = Number(value)
  return Number.isFinite(count) && count > 0 ? `${count}${t(unitKey)}` : ''
}

const displayHotelOrderOccupancy = (order: HotelOrder) => {
  const parts = [
    formatHotelOrderCount(order.guests, 'hotel.guests'),
    formatHotelOrderCount(order.nights, 'common.nightsUnit')
  ].filter(Boolean)

  return parts.length ? parts.join(' · ') : t('common.unknown')
}

const displayHotelOrderNote = (order: HotelOrder) => normalizeDisplayText(order.note)

const formatHotelOrderCurrency = (value: unknown) => {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return t('common.unknown')

  const price = amount.toLocaleString(activeDateLocale.value, {
    minimumFractionDigits: Number.isInteger(amount) ? 0 : 2,
    maximumFractionDigits: 2
  })

  return t('common.priceCny', { price })
}

const routeSourceLabel = (route: AdminRouteItem) => {
  return route?.sourceType === 'OFFICIAL' ? t('admin.officialRoute') : t('admin.userSharedRoute')
}

const routeAuthorLabel = (route: AdminRouteItem) => {
  if (route?.sourceType === 'OFFICIAL' && !route?.author?.username) {
    return t('admin.officialAuthor')
  }
  return route?.author?.nickname || route?.author?.username || t('common.unknown')
}

const routePreview = (route: AdminRouteItem) => {
  const content = route?.content || route?.description || ''
  const normalized = content.replace(/\s+/g, ' ').trim()
  return normalized.length > 90 ? `${normalized.slice(0, 90)}...` : (normalized || '-')
}

const routePriceLabel = (route: AdminRouteItem) => {
  if (route?.price === null || route?.price === undefined || route?.price === '') {
    return '-'
  }
  return `¥${route.price}`
}

const routeDifficultyLabel = (difficulty?: string | null) => {
  const labels: Record<string, string> = {
    EASY: t('admin.easy'),
    MEDIUM: t('admin.medium'),
    HARD: t('admin.hard')
  }
  if (!difficulty) return '-'
  return labels[difficulty] || difficulty
}

const getRoleLabel = (role: string) => {
  return role === 'ADMIN' ? t('admin.adminRole') : t('admin.userRole')
}

// 合并景点门票订单和酒店预订，按时间倒序
const recentOrders = computed(() => {
  const scenic = (stats.value.recentBookings || []).map((booking): AdminRecentOrder => ({ ...booking, _type: 'scenic' }))
  const hotel = (stats.value.recentHotelBookings || []).map((booking): AdminRecentOrder => ({ ...booking, _type: 'hotel' }))
  return [...scenic, ...hotel]
    .sort((a, b) => new Date(b.createdAt || '').getTime() - new Date(a.createdAt || '').getTime())
    .slice(0, 10)
})

// 确保热门景点按点击量降序排序
const sortedPopularSpots = computed(() => {
  if (!stats.value.popularSpots || stats.value.popularSpots.length === 0) {
    return []
  }
  return [...stats.value.popularSpots].sort((a, b) => {
    const countA = a.visitCount || 0
    const countB = b.visitCount || 0
    return countB - countA // 降序排序
  })
})

// 计算点击量百分比（相对于最高点击量）
const getClickCountPercentage = (spot: AdminSpot) => {
  if (!sortedPopularSpots.value || sortedPopularSpots.value.length === 0) {
    return 0
  }
  const maxCount = Math.max(...sortedPopularSpots.value.map((s) => s.visitCount || 0))
  if (maxCount === 0) return 0
  const currentCount = spot.visitCount || 0
  return Math.round((currentCount / maxCount) * 100)
}

const users = ref<AdminUserSummary[]>([])
const spots = ref<AdminSpot[]>([])
const loadingSpots = ref(false)
const fetchingPriceId = ref<number | null>(null)
const batchFetching = ref(false)
const priceBatchJob = ref<PriceBatchJob | null>(null)
const spotsError = ref('')
const showSpots = ref(false)
const showAllSpots = ref(false)
const showEditModal = ref(false)
const editingSpot = ref<Partial<AdminSpot>>({})
const newImageUrl = ref('')
const newDescription = ref('')
const newTicketPrice = ref<number | null>(null)
const updating = ref(false)
const originalBodyOverflow = ref<string | null>(null)

// News management
const newsList = ref<AdminNewsItem[]>([])
const loadingNews = ref(false)
const showAllNews = ref(false) // 默认折叠
const showNewsModal = ref(false)
const editingNews = ref<Partial<AdminNewsItem>>({})
const newsForm = ref({
  title: '',
  content: '',
  category: '',
  imageUrl: '',
  viewCount: 0
})
const updatingNews = ref(false)

const toggleAllHotelOrders = () => { showAllHotelOrders.value = !showAllHotelOrders.value }
const toggleRecentOrders = () => { showRecentOrders.value = !showRecentOrders.value }
const togglePopularSpots = () => { showPopularSpots.value = !showPopularSpots.value }
const toggleSpotsPanel = () => { showSpots.value = !showSpots.value }
const toggleUsersPanel = () => { showUsers.value = !showUsers.value }
const toggleNewsPanel = () => { showAllNews.value = !showAllNews.value }
const toggleCarouselsPanel = () => { showCarousels.value = !showCarousels.value }
const toggleRoutesPanel = () => { showRoutes.value = !showRoutes.value }
const toggleHotelsPanel = () => { showHotels.value = !showHotels.value }

// Computed property to control displayed spots
const displayedSpots = computed(() => {
  return showAllSpots.value ? spots.value : spots.value.slice(0, 6)
})

const priceBatchProgressPercent = computed(() => {
  const job = priceBatchJob.value
  if (!job) return 0
  const total = job.total || 0
  if (total <= 0) return job.status === 'COMPLETED' ? 100 : 0
  return Math.min(100, Math.round(((job.processed || 0) / total) * 100))
})

const priceBatchStatusText = computed(() => {
  const job = priceBatchJob.value
  if (!job) return ''
  if (job.status === 'COMPLETED') return '爬虫价格更新已完成'
  if (job.status === 'FAILED') return job.errorMessage || t('admin.batchFetchFailed')
  if (job.currentSpot) return `正在更新：${job.currentSpot}`
  return '正在准备爬虫任务...'
})

const adminPageParams = { page: 0, size: 100 }
const toList = <T>(value: unknown): T[] => {
  if (Array.isArray(value)) return value as T[]
  if (value && typeof value === 'object' && Array.isArray((value as { content?: unknown }).content)) {
    return (value as { content: T[] }).content
  }
  return []
}

const fetchUsers = async () => {
  try {
    const response = await api.get<unknown>(endpoints.admin.users, { params: adminPageParams })
    users.value = toList<AdminUserSummary>(response.data)
  } catch (error: unknown) {
    console.error('Failed to fetch users:', summarizeClientError(error))
    users.value = []
  }
}

const fetchSpots = async () => {
  loadingSpots.value = true
  spotsError.value = ''
  try {
    const response = await api.get<unknown>(endpoints.admin.spots, { params: adminPageParams })
    
    spots.value = toList<AdminSpot>(response.data)
  } catch (error: unknown) {
    console.error('Failed to fetch spots:', summarizeClientError(error))
    
    const status = apiErrorStatus(error)
    if (status) {
      if (status === 401) {
        spotsError.value = t('admin.unauthorizedAdmin')
      } else if (status === 403) {
        spotsError.value = t('admin.forbiddenAdmin')
      } else {
        spotsError.value = safeClientErrorMessage(error, t('admin.serverError', { status }))
      }
    } else if (isNetworkClientError(error)) {
      spotsError.value = t('admin.connectionFailed')
    } else {
      spotsError.value = safeClientErrorMessage(error, t('admin.unknownError'))
    }
    spots.value = []
  } finally {
    loadingSpots.value = false
  }
}

const openEditModal = (spot: AdminSpot) => {
  editingSpot.value = { ...spot }
  newImageUrl.value = spot.imageUrl || ''
  newDescription.value = spot.description || ''
  newTicketPrice.value = numberOrNull(spot.ticketPrice)
  showEditModal.value = true

  // 锁定主页面滚动
  if (typeof document !== 'undefined') {
    if (originalBodyOverflow.value === null) {
      originalBodyOverflow.value = document.body.style.overflow || ''
    }
    document.body.style.overflow = 'hidden'
  }
}

const closeEditModal = () => {
  showEditModal.value = false
  editingSpot.value = {}
  newImageUrl.value = ''
  newDescription.value = ''
  newTicketPrice.value = null

  // 恢复主页面滚动
  if (typeof document !== 'undefined' && originalBodyOverflow.value !== null) {
    document.body.style.overflow = originalBodyOverflow.value
    originalBodyOverflow.value = null
  }
}

const updateSpotImage = async () => {
  updating.value = true
  try {
    const payload: SpotUpdatePayload = {}
    if (newImageUrl.value) payload.imageUrl = newImageUrl.value
    if (newDescription.value) payload.description = newDescription.value
    if (newTicketPrice.value !== null && !Number.isNaN(newTicketPrice.value)) {
      payload.ticketPrice = newTicketPrice.value
    }

    if (Object.keys(payload).length === 0) {
      showToast(t('admin.fillOneField'), 'warning')
      updating.value = false
      return
    }

    const spotId = editingSpot.value.id
    if (spotId == null) {
      showToast(t('admin.updateFailed'), 'error')
      updating.value = false
      return
    }
    
    await api.put(endpoints.admin.updateSpot(spotId), payload)
    showToast(t('admin.spotUpdateSuccess'), 'success')
    await fetchSpots()
    closeEditModal()
  } catch (error) {
    console.error('Failed to update spot:', summarizeClientError(error))
    showToast(t('admin.updateFailed'), 'error')
  } finally {
    updating.value = false
  }
}

// 单独抓取某个景点价格
const fetchSpotPrice = async (spot: AdminSpot) => {
  fetchingPriceId.value = spot.id
  try {
    const response = await api.get<SpotPriceFetchResponse>(endpoints.prices.fetch(spot.id))
    const data = response.data
    if (data.success && data.priceInfo) {
      showToast(t('admin.priceFetched', { name: spot.name, price: data.priceInfo.basePrice || data.priceInfo.peakSeasonPrice || 'N/A' }), 'success')
      await fetchSpots()
    } else {
      showToast(t('admin.priceFetchFailed', { name: spot.name }), 'error')
    }
  } catch (error: unknown) {
    console.error('Failed to fetch price:', summarizeClientError(error))
    showToast(t('admin.priceFetchFailed', { name: spot.name }), 'error')
  } finally {
    fetchingPriceId.value = null
  }
}

// 批量抓取所有景点价格
const batchFetchPrices = async () => {
  if (!(await showConfirm(t('admin.confirmBatchFetch')))) return
  batchFetching.value = true
  priceBatchJob.value = null
  try {
    const response: { data: PriceBatchJob } = await api.post<PriceBatchJob>(endpoints.prices.batchUpdateJob + '?force=true')
    let currentJob: PriceBatchJob | null = response.data
    priceBatchJob.value = currentJob

    while (currentJob?.jobId && currentJob.status === 'RUNNING') {
      await new Promise(resolve => window.setTimeout(resolve, 1200))
      const jobId: string = currentJob.jobId
      const statusResponse: { data: PriceBatchJob } = await api.get<PriceBatchJob>(
        endpoints.prices.batchUpdateJobStatus(jobId)
      )
      currentJob = statusResponse.data
      priceBatchJob.value = currentJob
    }

    if (priceBatchJob.value?.status === 'FAILED') {
      throw new Error(priceBatchJob.value.errorMessage || 'Batch price update failed')
    }

    showToast(t('admin.batchFetchDone', {
      updated: priceBatchJob.value?.success || 0,
      failed: priceBatchJob.value?.failed || 0
    }), 'success')
    await fetchSpots()
  } catch (error: unknown) {
    console.error('Failed to batch fetch prices:', summarizeClientError(error))
    showToast(t('admin.batchFetchFailed'), 'error')
  } finally {
    batchFetching.value = false
  }
}

const updateRole = async (userId: number, newRole: string) => {
  if (!(await showConfirm(t('admin.confirmRoleChange', { role: getRoleLabel(newRole) })))) return

  try {
    await api.post(endpoints.admin.updateRole(userId), { role: newRole })
    await fetchUsers() // 刷新列表
    showToast(t('admin.operationSuccess'), 'success')
  } catch (error) {
    console.error('Failed to update role:', summarizeClientError(error))
    showToast(t('admin.operationFailed'), 'error')
  }
}

const deleteUser = async (user: AdminUserSummary) => {
  if (!(await confirmDanger(t('admin.confirmDeleteUser', { name: user.username })))) return

  try {
    await api.delete(endpoints.admin.deleteUser(user.id))
    showToast(t('admin.userDeleted', { name: user.username }), 'success')
    await fetchUsers()
  } catch (error: unknown) {
    console.error('Failed to delete user:', summarizeClientError(error))
    showToast(apiErrorMessage(error, t('admin.deleteFailed')), 'error')
  }
}

const unlockUser = async (user: AdminUserSummary) => {
  if (!(await showConfirm(t('admin.confirmUnlock', { name: user.username })))) return

  try {
    await api.post(endpoints.admin.unlockUser(user.id))
    showToast(t('admin.unlockSuccess', { name: user.username }), 'success')
    await fetchUsers()
  } catch (error: unknown) {
    console.error('Failed to unlock user:', summarizeClientError(error))
    showToast(apiErrorMessage(error, t('admin.operationFailed')), 'error')
  }
}

// Hotel orders management
const hotelOrders = ref<HotelOrder[]>([])
const loadingHotelOrders = ref(false)
const showAllHotelOrders = ref(false)
const showRecentOrders = ref(false)
const showPopularSpots = ref(false)
const showUsers = ref(false)

const fetchHotelOrders = async () => {
  loadingHotelOrders.value = true
  try {
    const response = await api.get<unknown>(endpoints.hotelBookings.all, { params: adminPageParams })
    hotelOrders.value = toList<HotelOrder>(response.data)
  } catch (error: unknown) {
    console.error('Failed to fetch hotel orders:', summarizeClientError(error))
    hotelOrders.value = []
  } finally {
    loadingHotelOrders.value = false
  }
}

const refreshOperationalData = async () => {
  await Promise.all([fetchStats(), fetchHotelOrders()])
}

const updateHotelOrderStatus = async (orderId: number, status: string) => {
  try {
    await api.put(endpoints.hotelBookings.updateStatus(orderId), { status })
    await refreshOperationalData()
  } catch (error) {
    console.error('Failed to update hotel order status:', summarizeClientError(error))
    showToast(t('admin.statusUpdateFailed'), 'error')
  }
}

const deleteHotelOrder = async (orderId: number) => {
  if (!(await confirmDanger(t('admin.confirmDeleteOrder')))) return
  try {
    await api.delete(endpoints.hotelBookings.delete(orderId))
    hotelOrders.value = hotelOrders.value.filter(order => order.id !== orderId)
    await refreshOperationalData()
    showToast(t('admin.deleteSuccess'), 'success')
  } catch (error) {
    console.error('Failed to delete hotel order:', summarizeClientError(error))
    showToast(t('admin.deleteFailed'), 'error')
  }
}

const fetchNews = async () => {
  loadingNews.value = true
  try {
    if (!(await auth.ensureSession())) {
      showToast(t('admin.notLoggedIn'), 'warning')
      await router.push('/login')
      return
    }

    if (!auth.isAdmin) {
      showToast(t('admin.noAdminPermission'), 'warning')
      return
    }
    
    const response = await api.get<unknown>(endpoints.admin.news, { params: adminPageParams })
    
    newsList.value = toList<AdminNewsItem>(response.data)
  } catch (error: unknown) {
    console.error('Failed to fetch news:', summarizeClientError(error))
    
    const status = apiErrorStatus(error)
    if (status) {
      if (status === 401) {
        showToast(t('admin.unauthorizedAdmin'), 'warning')
      } else if (status === 403) {
        showToast(t('admin.forbiddenAdmin'), 'warning')
      } else {
        const errorMsg = safeClientErrorMessage(error, t('admin.serverError', { status }))
        showToast(t('admin.fetchNewsFailed', { message: errorMsg }), 'error')
      }
    } else if (isNetworkClientError(error)) {
      showToast(t('admin.connectionFailed'), 'error')
    } else {
      showToast(t('admin.fetchNewsFailed', { message: safeClientErrorMessage(error, t('admin.unknownError')) }), 'error')
    }
    newsList.value = []
  } finally {
    loadingNews.value = false
  }
}

const openCreateNewsModal = () => {
  editingNews.value = {}
  newsForm.value = {
    title: '',
    content: '',
    category: '',
    imageUrl: '',
    viewCount: 0
  }
  showNewsModal.value = true
  if (typeof document !== 'undefined') {
    if (originalBodyOverflow.value === null) {
      originalBodyOverflow.value = document.body.style.overflow || ''
    }
    document.body.style.overflow = 'hidden'
  }
}

const openEditNewsModal = (news: AdminNewsItem) => {
  editingNews.value = { ...news }
  newsForm.value = {
    title: news.title || '',
    content: news.content || '',
    category: news.category || '',
    imageUrl: news.imageUrl || '',
    viewCount: news.viewCount || 0
  }
  showNewsModal.value = true
  if (typeof document !== 'undefined') {
    if (originalBodyOverflow.value === null) {
      originalBodyOverflow.value = document.body.style.overflow || ''
    }
    document.body.style.overflow = 'hidden'
  }
}

const closeNewsModal = () => {
  showNewsModal.value = false
  editingNews.value = {}
  newsForm.value = {
    title: '',
    content: '',
    category: '',
    imageUrl: '',
    viewCount: 0
  }
  if (typeof document !== 'undefined' && originalBodyOverflow.value !== null) {
    document.body.style.overflow = originalBodyOverflow.value
    originalBodyOverflow.value = null
  }
}

const saveNews = async () => {
  if (!newsForm.value.title || !newsForm.value.content || !newsForm.value.category) {
    showToast(t('admin.fillNewsFields'), 'warning')
    return
  }

  updatingNews.value = true
  try {
    const payload: NewsPayload = {
      title: newsForm.value.title,
      content: newsForm.value.content,
      category: newsForm.value.category
    }
    if (newsForm.value.imageUrl) {
      payload.imageUrl = newsForm.value.imageUrl
    }
    if (newsForm.value.viewCount !== null && newsForm.value.viewCount !== undefined) {
      payload.viewCount = newsForm.value.viewCount
    }

    if (editingNews.value.id) {
      // Update existing news
      await api.put(endpoints.admin.updateNews(editingNews.value.id), payload)
      showToast(t('admin.newsUpdateSuccess'), 'success')
    } else {
      // Create new news
      await api.post(endpoints.admin.createNews, payload)
      showToast(t('admin.newsCreateSuccess'), 'success')
    }
    await fetchNews()
    closeNewsModal()
  } catch (error) {
    console.error('Failed to save news:', summarizeClientError(error))
    showToast(t('admin.saveFailed'), 'error')
  } finally {
    updatingNews.value = false
  }
}

const deleteNewsItem = async (id: number) => {
  if (!(await confirmDanger(t('admin.confirmDeleteNews')))) {
    return
  }

  try {
    await api.delete(endpoints.admin.deleteNews(id))
    showToast(t('admin.deleteSuccess'), 'success')
    await fetchNews()
  } catch (error) {
    console.error('Failed to delete news:', summarizeClientError(error))
    showToast(t('admin.deleteFailed'), 'error')
  }
}

const getCategoryLabel = (category?: string | null) => {
  const labels: Record<string, string> = {
    POLICY: t('admin.categoryPolicy'),
    EVENT: t('admin.categoryEvent'),
    NOTICE: t('admin.categoryNotice')
  }
  if (!category) return ''
  return labels[category] || category
}

const getCategoryClass = (category?: string | null) => {
  const classes: Record<string, string> = {
    POLICY: 'bg-blue-100 text-blue-800',
    EVENT: 'bg-green-100 text-green-800',
    NOTICE: 'bg-yellow-100 text-yellow-800'
  }
  return category ? classes[category] || 'bg-gray-100 text-gray-800' : 'bg-gray-100 text-gray-800'
}

// The backend owns admin account action policy; the UI only renders it.
const canChangeUserRole = (user: AdminUserSummary) => Boolean(user.roleMutable) && !user.locked
const canDeleteUser = (user: AdminUserSummary) => Boolean(user.deletable)

// Carousel management
const carousels = ref<AdminCarouselItem[]>([])
const showCarousels = ref(false)
const showCarouselModal = ref(false)
const editingCarousel = ref<Partial<AdminCarouselItem>>({})
const carouselForm = ref({ title: '', subtitle: '', tag: '', imageUrl: '', linkUrl: '', sortOrder: 0, active: true })

const fetchCarousels = async () => {
  try {
    const res = await api.get<unknown>(endpoints.carousels.adminList)
    carousels.value = toList<AdminCarouselItem>(res.data)
  } catch (e) {
    carousels.value = []
    reportAdminListLoadFailure('admin carousels', e)
  }
}

const openCreateCarouselModal = () => {
  editingCarousel.value = {}
  carouselForm.value = { title: '', subtitle: '', tag: '', imageUrl: '', linkUrl: '', sortOrder: 0, active: true }
  showCarouselModal.value = true
}

const openEditCarouselModal = (c: AdminCarouselItem) => {
  editingCarousel.value = { ...c }
  carouselForm.value = { title: c.title || '', subtitle: c.subtitle || '', tag: c.tag || '', imageUrl: c.imageUrl || '', linkUrl: c.linkUrl || '', sortOrder: c.sortOrder || 0, active: c.active !== false }
  showCarouselModal.value = true
}

const closeCarouselModal = () => { showCarouselModal.value = false }

const saveCarousel = async () => {
  try {
    const payload = { ...carouselForm.value }
    if (editingCarousel.value.id) {
      await api.put(endpoints.carousels.adminUpdate(editingCarousel.value.id), payload)
    } else {
      await api.post(endpoints.carousels.adminCreate, payload)
    }
    await fetchCarousels()
    closeCarouselModal()
    showToast(t('admin.saveSuccess'), 'success')
  } catch (e) { showToast(t('admin.saveFailed'), 'error') }
}

const deleteCarousel = async (id: number) => {
  if (!(await confirmDanger(t('admin.confirmDelete')))) return
  try {
    await api.delete(endpoints.carousels.adminDelete(id))
    await fetchCarousels()
  } catch (e) { showToast(t('admin.deleteFailed'), 'error') }
}

// Route management
const adminRoutes = ref<AdminRouteItem[]>([])
const showRoutes = ref(false)
const showRouteModal = ref(false)
const editingRoute = ref<Partial<AdminRouteItem>>({})
const routeForm = ref({
  title: '',
  days: 1,
  budget: '',
  preference: '',
  price: null as number | null,
  difficulty: '',
  content: '',
  temperature: '',
  geography: ''
})

const adminBudgetOptions = computed(() => [
  { value: '经济型', label: t('routePlanner.budget.economy') },
  { value: '舒适型', label: t('routePlanner.budget.comfort') },
  { value: '豪华型', label: t('routePlanner.budget.luxury') }
])

const adminPreferenceOptions = computed(() => [
  { value: '自然风光', label: t('routePlanner.preferenceOptions.natural') },
  { value: '人文历史', label: t('routePlanner.preferenceOptions.cultural') },
  { value: '深度摄影', label: t('routePlanner.preferenceOptions.photography') },
  { value: '休闲度假', label: t('routePlanner.preferenceOptions.relaxation') }
])

const fetchAdminRoutes = async () => {
  try {
    const res = await api.get<unknown>(endpoints.adminRoutes.list, { params: adminPageParams })
    adminRoutes.value = toList<AdminRouteItem>(res.data)
  } catch (e) {
    adminRoutes.value = []
    reportAdminListLoadFailure('admin routes', e)
  }
}

const openCreateRouteModal = () => {
  editingRoute.value = {}
  routeForm.value = {
    title: '',
    days: 1,
    budget: '',
    preference: '',
    price: null,
    difficulty: '',
    content: '',
    temperature: '',
    geography: ''
  }
  showRouteModal.value = true
}

const openEditRouteModal = (r: AdminRouteItem) => {
  editingRoute.value = { ...r }
  routeForm.value = {
    title: r.title || r.name || '',
    days: numberOrDefault(r.days, 1),
    budget: r.budget || '',
    preference: r.preference || '',
    price: numberOrNull(r.price),
    difficulty: r.difficulty || '',
    content: r.content || r.description || '',
    temperature: r.temperature || '',
    geography: r.geography || ''
  }
  showRouteModal.value = true
}

const closeRouteModal = () => { showRouteModal.value = false }

const saveRoute = async () => {
  try {
    const payload = {
      title: routeForm.value.title,
      content: routeForm.value.content,
      days: routeForm.value.days,
      budget: routeForm.value.budget,
      preference: routeForm.value.preference,
      price: routeForm.value.price,
      difficulty: routeForm.value.difficulty,
      temperature: routeForm.value.temperature,
      geography: routeForm.value.geography
    }
    if (editingRoute.value.id) {
      await api.put(endpoints.adminRoutes.update(editingRoute.value.id), payload)
    } else {
      await api.post(endpoints.adminRoutes.create, payload)
    }
    await fetchAdminRoutes()
    closeRouteModal()
    showToast(t('admin.saveSuccess'), 'success')
  } catch (e) { showToast(t('admin.saveFailed'), 'error') }
}

const deleteRoute = async (id: number) => {
  if (!(await confirmDanger(t('admin.confirmDelete')))) return
  try {
    await api.delete(endpoints.adminRoutes.delete(id))
    await fetchAdminRoutes()
  } catch (e) { showToast(t('admin.deleteFailed'), 'error') }
}

// Hotel management
const adminHotels = ref<AdminHotelItem[]>([])
const showHotels = ref(false)
const showHotelModal = ref(false)
const editingHotel = ref<Partial<AdminHotelItem>>({})
const hotelForm = ref({ name: '', location: '', phone: '', priceRange: '', rating: 0, imageUrl: '', facilities: '' })
const failedHotelImages = ref<Record<number, boolean>>({})

const fetchAdminHotels = async () => {
  try {
    const res = await api.get<unknown>(endpoints.adminHotels.list, { params: adminPageParams })
    adminHotels.value = toList<AdminHotelItem>(res.data)
    failedHotelImages.value = {}
  } catch (e) {
    adminHotels.value = []
    failedHotelImages.value = {}
    reportAdminListLoadFailure('admin hotels', e)
  }
}

const openCreateHotelModal = () => {
  editingHotel.value = {}
  hotelForm.value = { name: '', location: '', phone: '', priceRange: '', rating: 0, imageUrl: '', facilities: '' }
  showHotelModal.value = true
}

const openEditHotelModal = (h: AdminHotelItem) => {
  editingHotel.value = { ...h }
  hotelForm.value = { name: h.name || '', location: h.location || '', phone: h.phone || '', priceRange: h.priceRange || '', rating: numberOrDefault(h.rating, 0), imageUrl: h.imageUrl || '', facilities: h.facilities || '' }
  showHotelModal.value = true
}

const closeHotelModal = () => { showHotelModal.value = false }

const saveHotel = async () => {
  try {
    const payload = { ...hotelForm.value }
    if (editingHotel.value.id) {
      await api.put(endpoints.adminHotels.update(editingHotel.value.id), payload)
    } else {
      await api.post(endpoints.adminHotels.create, payload)
    }
    await fetchAdminHotels()
    closeHotelModal()
    showToast(t('admin.saveSuccess'), 'success')
  } catch (e) { showToast(t('admin.saveFailed'), 'error') }
}

const deleteHotel = async (id: number) => {
  if (!(await confirmDanger(t('admin.confirmDelete')))) return
  try {
    await api.delete(endpoints.adminHotels.delete(id))
    await fetchAdminHotels()
  } catch (e) { showToast(t('admin.deleteFailed'), 'error') }
}

// Room type management (inline in expanded cards)
const expandedHotelId = ref<number | null>(null)
const expandedRoomTypes = ref<AdminRoomType[]>([])
const loadingRoomTypes = ref(false)
const roomTypeForm = ref({ name: '', price: 0, capacity: 2, amenities: '' })

const toggleHotelExpand = async (hotel: AdminHotelItem) => {
  if (expandedHotelId.value === hotel.id) {
    expandedHotelId.value = null
    expandedRoomTypes.value = []
  } else {
    expandedHotelId.value = hotel.id
    loadingRoomTypes.value = true
    try {
      const res = await api.get<unknown>(endpoints.adminHotels.roomTypes(hotel.id))
      expandedRoomTypes.value = toList<AdminRoomType>(res.data)
    } catch (e) {
      expandedRoomTypes.value = []
      reportAdminListLoadFailure('admin room types', e)
    }
    loadingRoomTypes.value = false
  }
}

const addRoomTypeInline = async (hotelId: number) => {
  if (!roomTypeForm.value.name) return
  try {
    await api.post(endpoints.adminHotels.roomTypes(hotelId), roomTypeForm.value)
    roomTypeForm.value = { name: '', price: 0, capacity: 2, amenities: '' }
    const res = await api.get<unknown>(endpoints.adminHotels.roomTypes(hotelId))
    expandedRoomTypes.value = toList<AdminRoomType>(res.data)
  } catch (e) { showToast(t('admin.addFailed'), 'error') }
}

const deleteRoomTypeInline = async (roomTypeId: number, hotelId: number) => {
  if (!(await confirmDanger(t('admin.confirmDeleteRoomType')))) return
  try {
    await api.delete(endpoints.adminHotels.deleteRoomType(roomTypeId))
    const res = await api.get<unknown>(endpoints.adminHotels.roomTypes(hotelId))
    expandedRoomTypes.value = toList<AdminRoomType>(res.data)
  } catch (e) { showToast(t('admin.deleteFailed'), 'error') }
}

onMounted(async () => {
  const hasSession = await auth.ensureSession()
  if (!hasSession || !auth.hasValidSession()) {
    await router.push('/login')
    return
  }
  if (!auth.isAdmin) {
    await router.push('/')
    return
  }

  fetchStats()
  fetchSecurityPosture()
  fetchUsers()
  fetchSpots()
  fetchHotelOrders()
  fetchNews()
  fetchCarousels()
  fetchAdminRoutes()
  fetchAdminHotels()

  operationalRefreshTimer = window.setInterval(() => {
    void refreshOperationalData()
    void fetchSecurityPosture()
  }, 15000)
})

onUnmounted(() => {
  if (operationalRefreshTimer !== null) {
    window.clearInterval(operationalRefreshTimer)
    operationalRefreshTimer = null
  }

  // 组件卸载时确保恢复主页面滚动
  if (typeof document !== 'undefined' && originalBodyOverflow.value !== null) {
    document.body.style.overflow = originalBodyOverflow.value
    originalBodyOverflow.value = null
  }
})
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

</style>
