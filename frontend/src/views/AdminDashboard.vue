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
          <div class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer" @click="showAllHotelOrders = !showAllHotelOrders">
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

          <div v-else-if="showAllHotelOrders" class="overflow-x-auto">
            <table class="min-w-full divide-y divide-stone-200">
              <thead class="bg-stone-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.orderId') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.user') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.hotelAndRoom') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.checkInOut') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.booker') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.amount') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.role') }}</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.orderedAt') }}</th>
                  <th class="px-6 py-3 text-right text-xs font-medium text-stone-500 uppercase tracking-wider">{{ t('admin.action') }}</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-stone-200">
                <tr v-for="order in hotelOrders" :key="order.id" class="hover:bg-stone-50">
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ order.id }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-stone-800">{{ order.user?.username || '-' }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-600">
                    <div>{{ order.hotel?.name || t('admin.unknownHotel') }}</div>
                    <div class="text-xs text-stone-400">{{ order.roomName }}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">
                    <div>{{ order.checkInDate }}</div>
                    <div class="text-xs text-stone-400">{{ t('admin.toDate', { date: order.checkOutDate }) }}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-600">
                    <div>{{ order.guestName }}</div>
                    <div class="text-xs text-stone-400">{{ order.phone }}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-bold text-red-600">¥{{ order.totalPrice }}</td>
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
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ formatDateTime(order.createdAt) }}</td>
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
            <div class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer" @click="showRecentOrders = !showRecentOrders">
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
            <div v-if="showRecentOrders" class="divide-y divide-stone-100">
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
            <div class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer" @click="showPopularSpots = !showPopularSpots">
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
            <div v-if="showPopularSpots" class="divide-y divide-stone-100">
              <div v-for="(spot, index) in sortedPopularSpots" :key="spot.id" class="px-6 py-4 flex items-center hover:bg-stone-50 transition-colors">
                <span class="text-lg font-bold text-stone-300 w-8">{{ index + 1 }}</span>
                <img :src="spot.imageUrl" class="w-10 h-10 rounded-lg object-cover mr-4" alt="">
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
          <div class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer" @click="showSpots = !showSpots">
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

          <template v-if="showSpots">
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
                   class="group bg-white rounded-xl border border-stone-200 overflow-hidden hover:shadow-lg hover:border-emerald-300 transition-all duration-200 cursor-pointer"
                   @click="openEditModal(spot)">
                <!-- Thumbnail -->
                <div class="relative h-40 overflow-hidden bg-stone-100">
                  <img v-if="spot.imageUrl" :src="spot.imageUrl" :alt="spot.name"
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
                    <span v-if="spot.visitCount > 0">· 👁 {{ spot.visitCount }}</span>
                  </div>
                  <p v-if="spot.description" class="text-xs text-stone-500 line-clamp-2 leading-relaxed">{{ spot.description }}</p>
                  <div class="mt-3 pt-3 border-t border-stone-100 flex items-center justify-between">
                    <span class="text-xs text-stone-400">ID: {{ spot.id }}</span>
                    <span class="text-xs font-medium text-emerald-600 group-hover:text-emerald-700 transition-colors">{{ t('admin.editArrow') }}</span>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="px-6 py-4 text-center text-stone-500 text-sm">
            {{ t('admin.expandSpots', { count: spots.length }) }}
          </div>
        </div>

        <!-- User Management Section -->
        <div class="bg-white rounded-lg shadow overflow-hidden mt-8">
          <div class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer" @click="showUsers = !showUsers">
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
          <div v-if="showUsers">
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
                  <button v-if="u.role !== 'ADMIN' && !u.locked" @click="updateRole(u.id, 'ADMIN')" class="text-blue-600 hover:text-blue-900">{{ t('admin.setAdmin') }}</button>
                  <button v-else-if="u.username !== 'lzh' && !u.locked" @click="updateRole(u.id, 'USER')" class="text-orange-600 hover:text-orange-900">{{ t('admin.unsetAdmin') }}</button>
                  <button v-if="canDeleteUser(u)" @click="deleteUser(u)" class="text-red-600 hover:text-red-900">{{ t('common.delete') }}</button>
                  <span v-if="u.username === 'lzh'" class="text-gray-400">{{ t('admin.notOperable') }}</span>
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
                    <button v-if="u.role !== 'ADMIN' && !u.locked" @click="updateRole(u.id, 'ADMIN')" class="text-blue-600 hover:text-blue-900 mr-4">{{ t('admin.setAdmin') }}</button>
                    <button v-else-if="u.username !== 'lzh' && !u.locked" @click="updateRole(u.id, 'USER')" class="text-orange-600 hover:text-orange-900 mr-4">{{ t('admin.unsetAdmin') }}</button>
                    <button v-if="canDeleteUser(u)" @click="deleteUser(u)" class="text-red-600 hover:text-red-900">{{ t('common.delete') }}</button>
                    <span v-if="u.username === 'lzh'" class="text-gray-400 cursor-not-allowed">{{ t('admin.notOperable') }}</span>
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
          <div class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer" @click="showAllNews = !showAllNews">
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
          <div v-else-if="showAllNews" class="divide-y divide-stone-200">
            <div v-for="news in newsList" :key="news.id" class="px-4 py-4 transition-colors hover:bg-stone-50 sm:px-6">
              <div class="flex flex-col gap-4 sm:flex-row sm:items-start">
                <div class="h-40 w-full flex-shrink-0 overflow-hidden rounded-lg bg-gray-200 sm:h-24 sm:w-24">
                  <img v-if="news.imageUrl" :src="news.imageUrl" :alt="news.title" class="w-full h-full object-cover">
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
          <div class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer" @click="showCarousels = !showCarousels">
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
          <div v-if="showCarousels" class="p-6">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="c in carousels" :key="c.id" class="border rounded-lg overflow-hidden">
                <img :src="c.imageUrl" class="w-full h-32 object-cover">
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
          <div class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer" @click="showRoutes = !showRoutes">
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
          <div v-if="showRoutes" class="overflow-x-auto">
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
          <div class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer" @click="showHotels = !showHotels">
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
          <div v-if="showHotels" class="p-4 grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div v-for="h in adminHotels" :key="h.id"
                 class="bg-white rounded-xl border border-stone-200 overflow-hidden hover:shadow-md transition-all duration-200"
                 :class="{ 'ring-2 ring-indigo-200 shadow-md': expandedHotelId === h.id }">
              <!-- Card Header (always visible) -->
              <div class="flex gap-4 p-4 cursor-pointer" @click="toggleHotelExpand(h)">
                <!-- Thumbnail -->
                <div class="w-24 h-24 shrink-0 rounded-lg overflow-hidden bg-stone-100">
                  <img v-if="h.imageUrl && !failedHotelImages[h.id]" :src="h.imageUrl" :alt="h.name"
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
              <div v-if="expandedHotelId === h.id" class="border-t border-stone-100 bg-stone-50/50 px-4 py-3 animate-slide-up">
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
            <img v-if="editingSpot.imageUrl" :src="editingSpot.imageUrl" :alt="editingSpot.name" class="w-full h-full object-cover">
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AdminAnalyticsPanel from '../components/AdminAnalyticsPanel.vue'
import AdminCommunityPanel from '../components/AdminCommunityPanel.vue'
import AdminHeritagePanel from '../components/AdminHeritagePanel.vue'
import AdminSecurityPosturePanel from '../components/AdminSecurityPosturePanel.vue'
import ImageUploadField from '../components/ImageUploadField.vue'
import MotionModal from '../components/motion/MotionModal.vue'
import api, { endpoints, clearTokenCache, type SecurityPostureResponse } from '../api'
import { useAuthStore } from '../stores/auth'

interface Stats {
  userCount: number
  orderCount: number
  totalRevenue: number
  recentBookings: any[]
  recentHotelBookings: any[]
  popularSpots: any[]
  spotCount?: number
  newsCount?: number
  monthlyBookingTrend?: Array<{ month: string; orderCount: number; revenue: number }>
  userGrowthTrend?: Array<{ month: string; count: number }>
  newsPublishTrend?: Array<{ month: string; count: number }>
  spotCategories?: Array<{ name: string; value: number }>
  visitorCityDistribution?: Array<{ name: string; value: number }>
  updatedAt?: string
}

const { t, locale, te } = useI18n()
const router = useRouter()
const auth = useAuthStore()

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
let operationalRefreshTimer: ReturnType<typeof window.setInterval> | null = null

const fetchSecurityPosture = async () => {
  loadingSecurityPosture.value = true
  securityPostureError.value = ''
  try {
    const response = await api.get(endpoints.admin.securityPosture)
    securityPosture.value = response.data
  } catch (error: any) {
    console.error('获取安全态势失败：', error)
    securityPostureError.value = error.response?.data?.message
      || error.response?.data?.error
      || t('admin.securityPosture.loadFailed')
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
      alert(t('admin.unauthorizedAdmin'))
      await router.push('/')
      return
    }

    analyticsError.value = ''
    const response = await api.get(endpoints.admin.stats)
    stats.value = response.data
    analyticsData.value = response.data
  } catch (error: any) {
    console.error('获取统计数据失败：', error)
    const status = error.response?.status
    if (status === 401) {
      auth.logout()
      clearTokenCache()
      await router.push('/login')
      return
    }
    if (status === 403) {
      alert(t('admin.forbiddenAdmin'))
      await router.push('/')
    } else {
      analyticsError.value = error.response?.data?.message || error.response?.data?.error || t('admin.analyticsLoadFailed')
    }
  } finally {
    loading.value = false
  }
}

const activeDateLocale = computed(() => locale.value === 'bo' ? 'bo-CN' : 'zh-CN')

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString(activeDateLocale.value)
}

const formatDateTime = (dateStr: string) => {
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

const routeSourceLabel = (route: any) => {
  return route?.sourceType === 'OFFICIAL' ? t('admin.officialRoute') : t('admin.userSharedRoute')
}

const routeAuthorLabel = (route: any) => {
  if (route?.sourceType === 'OFFICIAL' && !route?.author?.username) {
    return t('admin.officialAuthor')
  }
  return route?.author?.nickname || route?.author?.username || t('common.unknown')
}

const routePreview = (route: any) => {
  const content = route?.content || route?.description || ''
  const normalized = content.replace(/\s+/g, ' ').trim()
  return normalized.length > 90 ? `${normalized.slice(0, 90)}...` : (normalized || '-')
}

const routePriceLabel = (route: any) => {
  if (route?.price === null || route?.price === undefined || route?.price === '') {
    return '-'
  }
  return `¥${route.price}`
}

const routeDifficultyLabel = (difficulty: string) => {
  const labels: Record<string, string> = {
    EASY: t('admin.easy'),
    MEDIUM: t('admin.medium'),
    HARD: t('admin.hard')
  }
  return labels[difficulty] || difficulty || '-'
}

const getRoleLabel = (role: string) => {
  return role === 'ADMIN' ? t('admin.adminRole') : t('admin.userRole')
}

// 合并景点门票订单和酒店预订，按时间倒序
const recentOrders = computed(() => {
  const scenic = (stats.value.recentBookings || []).map((b: any) => ({ ...b, _type: 'scenic' }))
  const hotel = (stats.value.recentHotelBookings || []).map((h: any) => ({ ...h, _type: 'hotel' }))
  return [...scenic, ...hotel]
    .sort((a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 10)
})

// 确保热门景点按点击量降序排序
const sortedPopularSpots = computed(() => {
  if (!stats.value.popularSpots || stats.value.popularSpots.length === 0) {
    return []
  }
  return [...stats.value.popularSpots].sort((a: any, b: any) => {
    const countA = a.visitCount || 0
    const countB = b.visitCount || 0
    return countB - countA // 降序排序
  })
})

// 计算点击量百分比（相对于最高点击量）
const getClickCountPercentage = (spot: any) => {
  if (!sortedPopularSpots.value || sortedPopularSpots.value.length === 0) {
    return 0
  }
  const maxCount = Math.max(...sortedPopularSpots.value.map((s: any) => s.visitCount || 0))
  if (maxCount === 0) return 0
  const currentCount = spot.visitCount || 0
  return Math.round((currentCount / maxCount) * 100)
}

const users = ref<any[]>([])
const currentUser = ref<any>(null) // 当前登录用户信息
const spots = ref<any[]>([])
const loadingSpots = ref(false)
const fetchingPriceId = ref<number | null>(null)
const batchFetching = ref(false)
const priceBatchJob = ref<any | null>(null)
const spotsError = ref('')
const showSpots = ref(false)
const showAllSpots = ref(false)
const showEditModal = ref(false)
const editingSpot = ref<any>({})
const newImageUrl = ref('')
const newDescription = ref('')
const newTicketPrice = ref<number | null>(null)
const updating = ref(false)
const originalBodyOverflow = ref<string | null>(null)

// News management
const newsList = ref<any[]>([])
const loadingNews = ref(false)
const showAllNews = ref(false) // 默认折叠
const showNewsModal = ref(false)
const editingNews = ref<any>({})
const newsForm = ref({
  title: '',
  content: '',
  category: '',
  imageUrl: '',
  viewCount: 0
})
const updatingNews = ref(false)

// Computed property to control displayed spots
const displayedSpots = computed(() => {
  return showAllSpots.value ? spots.value : spots.value.slice(0, 6)
})

const priceBatchProgressPercent = computed(() => {
  const job = priceBatchJob.value
  if (!job) return 0
  if (job.total <= 0) return job.status === 'COMPLETED' ? 100 : 0
  return Math.min(100, Math.round(((job.processed || 0) / job.total) * 100))
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
const toList = (value: any) => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  return []
}

const fetchUsers = async () => {
  try {
    const response = await api.get(endpoints.admin.users, { params: adminPageParams })
    const userData = toList(response.data)
    if (Array.isArray(userData)) {
      users.value = userData
    } else {
      console.error('响应数据格式错误:', response.data)
      users.value = []
    }
  } catch (error: any) {
    console.error('获取用户数据失败:', error)
    if (error.response) {
      console.error('响应状态:', error.response.status)
      console.error('响应数据:', error.response.data)
    } else if (error.request) {
      console.error('请求已发送但无响应:', error.request)
    }
    users.value = []
  }
}

const fetchSpots = async () => {
  loadingSpots.value = true
  spotsError.value = ''
  try {
    const response = await api.get(endpoints.admin.spots, { params: adminPageParams })
    
    const spotsData = toList(response.data)
    if (Array.isArray(spotsData)) {
      spots.value = spotsData
    } else {
      console.error('❌ 响应数据格式错误:', response.data)
      spotsError.value = t('admin.malformedResponse', { type: typeof response.data })
      spots.value = []
    }
  } catch (error: any) {
    console.error('❌ 获取景点数据失败:', error)
    console.error('错误类型:', error.constructor.name)
    console.error('错误消息:', error.message)
    
    if (error.response) {
      console.error('响应状态:', error.response.status)
      console.error('响应头:', error.response.headers)
      console.error('响应数据:', error.response.data)
      
      if (error.response.status === 401) {
        spotsError.value = t('admin.unauthorizedAdmin')
      } else if (error.response.status === 403) {
        spotsError.value = t('admin.forbiddenAdmin')
      } else {
        spotsError.value = error.response.data?.message || t('admin.serverError', { status: error.response.status })
      }
    } else if (error.request) {
      console.error('请求已发送但无响应')
      console.error('请求配置:', error.config)
      spotsError.value = t('admin.connectionFailed')
    } else {
      console.error('请求配置错误:', error.config)
      spotsError.value = error.message || t('admin.unknownError')
    }
    spots.value = []
  } finally {
    loadingSpots.value = false
  }
}

const openEditModal = (spot: any) => {
  editingSpot.value = { ...spot }
  newImageUrl.value = spot.imageUrl || ''
  newDescription.value = spot.description || ''
  newTicketPrice.value = spot.ticketPrice ?? null
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
    const payload: any = {}
    if (newImageUrl.value) payload.imageUrl = newImageUrl.value
    if (newDescription.value) payload.description = newDescription.value
    if (newTicketPrice.value !== null && !Number.isNaN(newTicketPrice.value)) {
      payload.ticketPrice = newTicketPrice.value
    }

    if (Object.keys(payload).length === 0) {
      alert(t('admin.fillOneField'))
      updating.value = false
      return
    }
    
    await api.put(endpoints.admin.updateSpot(editingSpot.value.id), payload)
    alert(t('admin.spotUpdateSuccess'))
    await fetchSpots()
    closeEditModal()
  } catch (error) {
    console.error('Failed to update spot:', error)
    alert(t('admin.updateFailed'))
  } finally {
    updating.value = false
  }
}

// 单独抓取某个景点价格
const fetchSpotPrice = async (spot: any) => {
  fetchingPriceId.value = spot.id
  try {
    const response = await api.get(endpoints.prices.fetch(spot.id))
    const data = response.data
    if (data.success && data.priceInfo) {
      alert(t('admin.priceFetched', { name: spot.name, price: data.priceInfo.basePrice || data.priceInfo.peakSeasonPrice || 'N/A' }))
      await fetchSpots()
    } else {
      alert(t('admin.priceFetchFailed', { name: spot.name }))
    }
  } catch (error: any) {
    console.error('Failed to fetch price:', error)
    alert(t('admin.priceFetchFailed', { name: spot.name }))
  } finally {
    fetchingPriceId.value = null
  }
}

// 批量抓取所有景点价格
const batchFetchPrices = async () => {
  if (!confirm(t('admin.confirmBatchFetch'))) return
  batchFetching.value = true
  priceBatchJob.value = null
  try {
    const response = await api.post(endpoints.prices.batchUpdateJob + '?force=true')
    priceBatchJob.value = response.data

    while (priceBatchJob.value?.jobId && priceBatchJob.value.status === 'RUNNING') {
      await new Promise(resolve => window.setTimeout(resolve, 1200))
      const statusResponse = await api.get(endpoints.prices.batchUpdateJobStatus(priceBatchJob.value.jobId))
      priceBatchJob.value = statusResponse.data
    }

    if (priceBatchJob.value?.status === 'FAILED') {
      throw new Error(priceBatchJob.value.errorMessage || 'Batch price update failed')
    }

    alert(t('admin.batchFetchDone', {
      updated: priceBatchJob.value?.success || 0,
      failed: priceBatchJob.value?.failed || 0
    }))
    await fetchSpots()
  } catch (error: any) {
    console.error('Failed to batch fetch prices:', error)
    alert(t('admin.batchFetchFailed'))
  } finally {
    batchFetching.value = false
  }
}

const updateRole = async (userId: number, newRole: string) => {
  if (!confirm(t('admin.confirmRoleChange', { role: getRoleLabel(newRole) }))) return

  try {
    await api.post(endpoints.admin.updateRole(userId), { role: newRole })
    await fetchUsers() // 刷新列表
    alert(t('admin.operationSuccess'))
  } catch (error) {
    console.error('Failed to update role:', error)
    alert(t('admin.operationFailed'))
  }
}

const deleteUser = async (user: any) => {
  if (!confirm(t('admin.confirmDeleteUser', { name: user.username }))) return

  try {
    await api.delete(endpoints.admin.deleteUser(user.id))
    alert(t('admin.userDeleted', { name: user.username }))
    await fetchUsers()
  } catch (error: any) {
    console.error('Failed to delete user:', error)
    alert(error.response?.data?.error || t('admin.deleteFailed'))
  }
}

const unlockUser = async (user: any) => {
  if (!confirm(t('admin.confirmUnlock', { name: user.username }))) return

  try {
    await api.post(endpoints.admin.unlockUser(user.id))
    alert(t('admin.unlockSuccess', { name: user.username }))
    await fetchUsers()
  } catch (error: any) {
    console.error('Failed to unlock user:', error)
    alert(error.response?.data?.error || t('admin.operationFailed'))
  }
}

// Hotel orders management
const hotelOrders = ref<any[]>([])
const loadingHotelOrders = ref(false)
const showAllHotelOrders = ref(false)
const showRecentOrders = ref(false)
const showPopularSpots = ref(false)
const showUsers = ref(false)

const fetchHotelOrders = async () => {
  loadingHotelOrders.value = true
  try {
    const response = await api.get(endpoints.hotelBookings.all, { params: adminPageParams })
    hotelOrders.value = toList(response.data)
  } catch (error: any) {
    console.error('Failed to fetch hotel orders:', error)
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
    console.error('Failed to update hotel order status:', error)
    alert(t('admin.statusUpdateFailed'))
  }
}

const deleteHotelOrder = async (orderId: number) => {
  if (!confirm(t('admin.confirmDeleteOrder'))) return
  try {
    await api.delete(endpoints.hotelBookings.delete(orderId))
    hotelOrders.value = hotelOrders.value.filter(order => order.id !== orderId)
    await refreshOperationalData()
    alert(t('admin.deleteSuccess'))
  } catch (error) {
    console.error('Failed to delete hotel order:', error)
    alert(t('admin.deleteFailed'))
  }
}

const fetchNews = async () => {
  loadingNews.value = true
  try {
    if (!(await auth.ensureSession())) {
      alert(t('admin.notLoggedIn'))
      await router.push('/login')
      return
    }

    if (!auth.isAdmin) {
      alert(t('admin.noAdminPermission'))
      return
    }
    
    const response = await api.get(endpoints.admin.news, { params: adminPageParams })
    
    const newsData = toList(response.data)
    if (Array.isArray(newsData)) {
      newsList.value = newsData
    } else {
      console.error('❌ 响应数据格式错误:', response.data)
      newsList.value = []
    }
  } catch (error: any) {
    console.error('❌ 获取资讯数据失败:', error)
    console.error('错误类型:', error.constructor?.name)
    console.error('错误消息:', error.message)
    
    if (error.response) {
      console.error('响应状态:', error.response.status)
      console.error('响应数据:', error.response.data)
      
      if (error.response.status === 401) {
        alert(t('admin.unauthorizedAdmin'))
      } else if (error.response.status === 403) {
        alert(t('admin.forbiddenAdmin'))
      } else {
        const errorMsg = error.response.data?.message || error.response.data || t('admin.serverError', { status: error.response.status })
        alert(t('admin.fetchNewsFailed', { message: errorMsg }))
      }
    } else if (error.request) {
      console.error('请求已发送但无响应')
      alert(t('admin.connectionFailed'))
    } else {
      console.error('请求配置错误:', error.config)
      alert(t('admin.fetchNewsFailed', { message: error.message || t('admin.unknownError') }))
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

const openEditNewsModal = (news: any) => {
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
    alert(t('admin.fillNewsFields'))
    return
  }

  updatingNews.value = true
  try {
    const payload: any = {
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
      alert(t('admin.newsUpdateSuccess'))
    } else {
      // Create new news
      await api.post(endpoints.admin.createNews, payload)
      alert(t('admin.newsCreateSuccess'))
    }
    await fetchNews()
    closeNewsModal()
  } catch (error) {
    console.error('Failed to save news:', error)
    alert(t('admin.saveFailed'))
  } finally {
    updatingNews.value = false
  }
}

const deleteNewsItem = async (id: number) => {
  if (!confirm(t('admin.confirmDeleteNews'))) {
    return
  }

  try {
    await api.delete(endpoints.admin.deleteNews(id))
    alert(t('admin.deleteSuccess'))
    await fetchNews()
  } catch (error) {
    console.error('Failed to delete news:', error)
    alert(t('admin.deleteFailed'))
  }
}

const getCategoryLabel = (category: string) => {
  const labels: Record<string, string> = {
    POLICY: t('admin.categoryPolicy'),
    EVENT: t('admin.categoryEvent'),
    NOTICE: t('admin.categoryNotice')
  }
  return labels[category] || category
}

const getCategoryClass = (category: string) => {
  const classes: Record<string, string> = {
    POLICY: 'bg-blue-100 text-blue-800',
    EVENT: 'bg-green-100 text-green-800',
    NOTICE: 'bg-yellow-100 text-yellow-800'
  }
  return classes[category] || 'bg-gray-100 text-gray-800'
}

// 判断当前用户是否是超级管理员（lzh）
const canDeleteUser = (user: any) => {
  return user?.role === 'USER'
    && user?.username !== 'lzh'
    && user?.username !== currentUser.value?.username
}

// 加载当前用户信息
const loadCurrentUser = async () => {
  await auth.ensureSession()
  currentUser.value = auth.user
}

// Carousel management
const carousels = ref<any[]>([])
const showCarousels = ref(false)
const showCarouselModal = ref(false)
const editingCarousel = ref<any>({})
const carouselForm = ref({ title: '', subtitle: '', tag: '', imageUrl: '', linkUrl: '', sortOrder: 0, active: true })

const fetchCarousels = async () => {
  try {
    const res = await api.get(endpoints.carousels.adminList)
    carousels.value = Array.isArray(res.data) ? res.data : []
  } catch (e) { carousels.value = [] }
}

const openCreateCarouselModal = () => {
  editingCarousel.value = {}
  carouselForm.value = { title: '', subtitle: '', tag: '', imageUrl: '', linkUrl: '', sortOrder: 0, active: true }
  showCarouselModal.value = true
}

const openEditCarouselModal = (c: any) => {
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
    alert(t('admin.saveSuccess'))
  } catch (e) { alert(t('admin.saveFailed')) }
}

const deleteCarousel = async (id: number) => {
  if (!confirm(t('admin.confirmDelete'))) return
  try {
    await api.delete(endpoints.carousels.adminDelete(id))
    await fetchCarousels()
  } catch (e) { alert(t('admin.deleteFailed')) }
}

// Route management
const adminRoutes = ref<any[]>([])
const showRoutes = ref(false)
const showRouteModal = ref(false)
const editingRoute = ref<any>({})
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
    const res = await api.get(endpoints.adminRoutes.list, { params: adminPageParams })
    adminRoutes.value = toList(res.data)
  } catch (e) { adminRoutes.value = [] }
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

const openEditRouteModal = (r: any) => {
  editingRoute.value = { ...r }
  routeForm.value = {
    title: r.title || r.name || '',
    days: r.days || 1,
    budget: r.budget || '',
    preference: r.preference || '',
    price: r.price ?? null,
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
    alert(t('admin.saveSuccess'))
  } catch (e) { alert(t('admin.saveFailed')) }
}

const deleteRoute = async (id: number) => {
  if (!confirm(t('admin.confirmDelete'))) return
  try {
    await api.delete(endpoints.adminRoutes.delete(id))
    await fetchAdminRoutes()
  } catch (e) { alert(t('admin.deleteFailed')) }
}

// Hotel management
const adminHotels = ref<any[]>([])
const showHotels = ref(false)
const showHotelModal = ref(false)
const editingHotel = ref<any>({})
const hotelForm = ref({ name: '', location: '', phone: '', priceRange: '', rating: 0, imageUrl: '', facilities: '' })
const failedHotelImages = ref<Record<number, boolean>>({})

const fetchAdminHotels = async () => {
  try {
    const res = await api.get(endpoints.adminHotels.list, { params: adminPageParams })
    adminHotels.value = toList(res.data)
    failedHotelImages.value = {}
  } catch (e) { adminHotels.value = [] }
}

const openCreateHotelModal = () => {
  editingHotel.value = {}
  hotelForm.value = { name: '', location: '', phone: '', priceRange: '', rating: 0, imageUrl: '', facilities: '' }
  showHotelModal.value = true
}

const openEditHotelModal = (h: any) => {
  editingHotel.value = { ...h }
  hotelForm.value = { name: h.name || '', location: h.location || '', phone: h.phone || '', priceRange: h.priceRange || '', rating: h.rating || 0, imageUrl: h.imageUrl || '', facilities: h.facilities || '' }
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
    alert(t('admin.saveSuccess'))
  } catch (e) { alert(t('admin.saveFailed')) }
}

const deleteHotel = async (id: number) => {
  if (!confirm(t('admin.confirmDelete'))) return
  try {
    await api.delete(endpoints.adminHotels.delete(id))
    await fetchAdminHotels()
  } catch (e) { alert(t('admin.deleteFailed')) }
}

// Room type management (inline in expanded cards)
const expandedHotelId = ref<number | null>(null)
const expandedRoomTypes = ref<any[]>([])
const loadingRoomTypes = ref(false)
const roomTypeForm = ref({ name: '', price: 0, capacity: 2, amenities: '' })

const toggleHotelExpand = async (hotel: any) => {
  if (expandedHotelId.value === hotel.id) {
    expandedHotelId.value = null
    expandedRoomTypes.value = []
  } else {
    expandedHotelId.value = hotel.id
    loadingRoomTypes.value = true
    try {
      const res = await api.get(endpoints.adminHotels.roomTypes(hotel.id))
      expandedRoomTypes.value = Array.isArray(res.data) ? res.data : []
    } catch (e) { expandedRoomTypes.value = [] }
    loadingRoomTypes.value = false
  }
}

const addRoomTypeInline = async (hotelId: number) => {
  if (!roomTypeForm.value.name) return
  try {
    await api.post(endpoints.adminHotels.roomTypes(hotelId), roomTypeForm.value)
    roomTypeForm.value = { name: '', price: 0, capacity: 2, amenities: '' }
    const res = await api.get(endpoints.adminHotels.roomTypes(hotelId))
    expandedRoomTypes.value = Array.isArray(res.data) ? res.data : []
  } catch (e) { alert(t('admin.addFailed')) }
}

const deleteRoomTypeInline = async (roomTypeId: number, hotelId: number) => {
  if (!confirm(t('admin.confirmDeleteRoomType'))) return
  try {
    await api.delete(endpoints.adminHotels.deleteRoomType(roomTypeId))
    const res = await api.get(endpoints.adminHotels.roomTypes(hotelId))
    expandedRoomTypes.value = Array.isArray(res.data) ? res.data : []
  } catch (e) { alert(t('admin.deleteFailed')) }
}

onMounted(async () => {
  await loadCurrentUser()
  if (!auth.hasValidSession()) {
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
