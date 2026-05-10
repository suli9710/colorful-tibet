<template>
  <div class="min-h-screen bg-stone-50 py-12 pt-24">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-12">
        <h1 class="text-4xl font-bold text-stone-800 mb-4">后台管理看板</h1>
        <p class="text-lg text-stone-600">数据概览与运营统计</p>
      </div>

      <div v-if="loading" class="flex justify-center items-center h-64">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
      </div>

      <div v-else class="space-y-8">
        <AdminAnalyticsPanel :loading="loading" :chart-data="analyticsData" :error="analyticsError" />

        <!-- Stats Cards -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div class="bg-white rounded-lg shadow p-6 border-l-4 border-blue-500">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-stone-500 uppercase font-semibold">总用户数</p>
                <p class="text-3xl font-bold text-stone-800">{{ stats.userCount }}</p>
              </div>
              <div class="bg-blue-100 p-3 rounded-full">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-lg shadow p-6 border-l-4 border-green-500">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-stone-500 uppercase font-semibold">总订单数</p>
                <p class="text-3xl font-bold text-stone-800">{{ stats.orderCount }}</p>
              </div>
              <div class="bg-green-100 p-3 rounded-full">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                </svg>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-lg shadow p-6 border-l-4 border-yellow-500">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-stone-500 uppercase font-semibold">总营收</p>
                <p class="text-3xl font-bold text-stone-800">¥{{ stats.totalRevenue?.toLocaleString() || 0 }}</p>
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
          <div class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white cursor-pointer hover:bg-stone-100/50 transition-colors" @click="showAllHotelOrders = !showAllHotelOrders">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-amber-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">酒店订单 <span class="text-sm font-normal text-stone-400">({{ hotelOrders.length }}条)</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="fetchHotelOrders" :disabled="loadingHotelOrders" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">
                {{ loadingHotelOrders ? '加载中...' : '刷新' }}
              </button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showAllHotelOrders }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
              </svg>
            </div>
          </div>

          <div v-if="loadingHotelOrders" class="p-8 text-center text-stone-500">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
            <p>正在加载酒店订单...</p>
          </div>

          <div v-else-if="showAllHotelOrders" class="overflow-x-auto">
            <table class="min-w-full divide-y divide-stone-200">
              <thead class="bg-stone-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">订单ID</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">用户</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">酒店 / 房型</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">入住 - 离店</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">预订人</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">金额</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">状态</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">下单时间</th>
                  <th class="px-6 py-3 text-right text-xs font-medium text-stone-500 uppercase tracking-wider">操作</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-stone-200">
                <tr v-for="order in hotelOrders" :key="order.id" class="hover:bg-stone-50">
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ order.id }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-stone-800">{{ order.user?.username || '-' }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-600">
                    <div>{{ order.hotel?.name || '未知酒店' }}</div>
                    <div class="text-xs text-stone-400">{{ order.roomName }}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">
                    <div>{{ order.checkInDate }}</div>
                    <div class="text-xs text-stone-400">至 {{ order.checkOutDate }}</div>
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
                      <option value="PENDING">待确认</option>
                      <option value="CONFIRMED">已确认</option>
                      <option value="CANCELLED">已取消</option>
                    </select>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ formatDateTime(order.createdAt) }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button @click="deleteHotelOrder(order.id)" class="text-red-600 hover:text-red-900">删除</button>
                  </td>
                </tr>
                <tr v-if="hotelOrders.length === 0">
                  <td colspan="9" class="px-6 py-8 text-center text-stone-500">暂无酒店订单</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else-if="!loadingHotelOrders && hotelOrders.length > 0" class="px-6 py-4 text-center text-stone-500 text-sm">
            点击上方标题栏展开查看全部订单（共{{ hotelOrders.length }}条）
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mt-8">
          <!-- Recent Bookings -->
          <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden">
            <div class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white cursor-pointer hover:bg-stone-100/50 transition-colors" @click="showRecentOrders = !showRecentOrders">
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-lg bg-blue-100 flex items-center justify-center">
                  <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"/></svg>
                </div>
                <h3 class="text-lg font-bold text-stone-800">最新订单 <span class="text-sm font-normal text-stone-400">({{ recentOrders.length }}条)</span></h3>
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
                      <span v-if="order.hotel" class="text-xs px-2 py-0.5 rounded bg-blue-100 text-blue-700">酒店</span>
                      <span v-else class="text-xs px-2 py-0.5 rounded bg-orange-100 text-orange-700">景点</span>
                      <p class="text-sm font-medium text-stone-800">{{ order.hotel?.name || order.spot?.name || '未知' }}</p>
                    </div>
                    <p class="text-xs text-stone-500 mt-1">{{ formatDate(order.createdAt) }}</p>
                  </div>
                  <div class="text-right">
                    <p class="text-sm font-bold text-stone-800">¥{{ order.totalPrice }}</p>
                    <span :class="getStatusClass(order.status)" class="text-xs px-2 py-1 rounded-full">{{ order.status }}</span>
                  </div>
                </div>
              </template>
              <div v-else class="px-6 py-4 text-center text-stone-400 text-sm">暂无订单</div>
            </div>
          </div>

          <!-- Popular Spots -->
          <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden">
            <div class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white cursor-pointer hover:bg-stone-100/50 transition-colors" @click="showPopularSpots = !showPopularSpots">
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-lg bg-rose-100 flex items-center justify-center">
                  <svg class="w-5 h-5 text-rose-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"/></svg>
                </div>
                <h3 class="text-lg font-bold text-stone-800">热门景点 <span class="text-sm font-normal text-stone-400">({{ sortedPopularSpots.length }}个)</span></h3>
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
                    <span class="text-xs text-stone-500 font-medium whitespace-nowrap">{{ spot.visitCount || 0 }}次</span>
                  </div>
                </div>
                <div class="text-right ml-4">
                  <p class="text-sm font-bold text-red-600">¥{{ spot.ticketPrice }}</p>
                </div>
              </div>
              <div v-if="!sortedPopularSpots?.length" class="px-6 py-4 text-center text-stone-400 text-sm">暂无数据</div>
            </div>
          </div>
        </div>
        
        <!-- Spots Management Section -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-emerald-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">景点管理 <span class="text-sm font-normal text-stone-400">({{ spots.length }}个)</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click="fetchSpots" :disabled="loadingSpots" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">
                {{ loadingSpots ? '加载中...' : '刷新' }}
              </button>
              <button v-if="spots.length > 6" @click="showAllSpots = !showAllSpots" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">
                {{ showAllSpots ? '收起' : '查看全部(' + spots.length + ')' }}
              </button>
            </div>
          </div>

          <!-- Loading State -->
          <div v-if="loadingSpots" class="p-12 text-center">
            <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-emerald-500 mx-auto mb-4"></div>
            <p class="text-stone-400 text-sm">正在加载景点数据...</p>
          </div>

          <!-- Error State -->
          <div v-else-if="spotsError" class="p-12 text-center">
            <div class="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center mx-auto mb-4">
              <svg class="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"/></svg>
            </div>
            <p class="text-red-500 text-sm mb-1">加载失败</p>
            <p class="text-stone-400 text-xs mb-4">{{ spotsError }}</p>
            <button @click="fetchSpots" class="text-sm px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors">重试</button>
          </div>

          <!-- Empty State -->
          <div v-else-if="spots.length === 0" class="p-16 text-center">
            <svg class="w-16 h-16 text-stone-200 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
            <p class="text-stone-400">暂无景点数据</p>
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
                <!-- Price badge -->
                <div class="absolute top-3 right-3 px-2.5 py-1 rounded-full bg-white/90 backdrop-blur text-emerald-700 text-xs font-bold shadow-sm">
                  ¥{{ spot.ticketPrice || '-' }}
                </div>
                <!-- Edit overlay on hover -->
                <div class="absolute inset-0 bg-black/0 group-hover:bg-black/30 transition-all flex items-center justify-center">
                  <span class="px-4 py-2 rounded-lg bg-white/90 text-stone-700 text-sm font-medium opacity-0 group-hover:opacity-100 transition-opacity shadow-lg backdrop-blur">
                    点击编辑
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
                  <span class="text-xs font-medium text-emerald-600 group-hover:text-emerald-700 transition-colors">编辑 →</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- User Management Section -->
        <div class="bg-white rounded-lg shadow overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white cursor-pointer hover:bg-stone-100/50 transition-colors" @click="showUsers = !showUsers">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-violet-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-violet-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">用户管理 <span class="text-sm font-normal text-stone-400">({{ users.length }}人)</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="fetchUsers" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">刷新</button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showUsers }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
            </div>
          </div>
          <div v-if="showUsers">
          <div class="overflow-x-auto">
            <div class="overflow-x-auto">
              <table class="min-w-full divide-y divide-stone-200">
                <thead class="bg-stone-50">
                  <tr>
                    <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">ID</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">用户名</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">密码</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">昵称</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">城市</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">IP地址</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">最后登录</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">注册时间</th>
                    <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">角色</th>
                    <th class="px-6 py-3 text-right text-xs font-medium text-stone-500 uppercase tracking-wider">操作</th>
                  </tr>
                </thead>
                <tbody class="bg-white divide-y divide-stone-200">
                  <tr v-for="u in users" :key="u.id" class="hover:bg-stone-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ u.id }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-stone-900">{{ u.username }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500 font-mono">
                      <div class="flex items-center gap-2">
                        <span class="text-xs bg-gray-100 px-2 py-1 rounded" title="密码仅保存 BCrypt 哈希，无法查看明文">
                          BCrypt 哈希
                        </span>
                      </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">{{ u.nickname || '-' }}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-600">
                      <span v-if="u.city" class="px-2 py-1 bg-blue-50 text-blue-700 rounded-full text-xs">{{ u.city }}</span>
                      <span v-else class="text-stone-400">-</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500 font-mono">
                      <span v-if="u.ipAddress" class="text-xs">{{ u.ipAddress }}</span>
                      <span v-else class="text-stone-400">-</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">
                      <span v-if="u.lastLoginAt">{{ formatDateTime(u.lastLoginAt) }}</span>
                      <span v-else class="text-stone-400">从未登录</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-500">
                      <span v-if="u.createdAt">{{ formatDateTime(u.createdAt) }}</span>
                      <span v-else class="text-stone-400">-</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <span :class="u.role === 'ADMIN' ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-800'" 
                            class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full">
                        {{ u.role === 'ADMIN' ? '管理员' : '普通用户' }}
                      </span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button v-if="u.role !== 'ADMIN'" @click="updateRole(u.id, 'ADMIN')" class="text-blue-600 hover:text-blue-900 mr-4">设为管理员</button>
                      <button v-else-if="u.username !== 'lzh'" @click="updateRole(u.id, 'USER')" class="text-orange-600 hover:text-orange-900 mr-4">取消管理员</button>
                      <button v-if="isSuperAdmin && u.username !== 'lzh'" @click="deleteUser(u)" class="text-red-600 hover:text-red-900">删除</button>
                      <span v-if="u.username === 'lzh'" class="text-gray-400 cursor-not-allowed">不可操作</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 审计日志（仅超级管理员） -->
        <div v-if="isSuperAdmin" class="bg-white rounded-lg shadow overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-200 flex justify-between items-center">
            <h3 class="text-lg font-bold text-stone-800">用户操作审计日志</h3>
            <button @click="fetchAuditLogs" class="text-sm text-blue-600 hover:text-blue-800" :disabled="loadingAuditLogs">
              {{ loadingAuditLogs ? '加载中...' : '刷新日志' }}
            </button>
          </div>
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-stone-200">
              <thead class="bg-stone-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">时间</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">操作人</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">目标用户</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">结果</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-stone-500 uppercase tracking-wider">原因</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-stone-200">
                <tr v-for="log in auditLogs" :key="log.id" class="hover:bg-stone-50">
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-600">{{ formatDateTime(log.createdAt) }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-stone-800">{{ log.operatorUsername }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-stone-600">{{ log.targetUsername }} (ID: {{ log.targetUserId }})</td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span :class="getAuditActionClass(log.action)" class="px-2 py-1 rounded-full text-xs font-semibold">
                      {{ getAuditActionLabel(log.action) }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-sm text-stone-600">{{ log.detail || '-' }}</td>
                </tr>
                <tr v-if="!loadingAuditLogs && auditLogs.length === 0">
                  <td colspan="5" class="px-6 py-6 text-center text-stone-500">暂无审计记录</td>
                </tr>
              </tbody>
            </table>
          </div>
          </div>
        </div>
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white cursor-pointer hover:bg-stone-100/50 transition-colors" @click="showAllNews = !showAllNews">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-cyan-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-cyan-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">旅游资讯管理 <span class="text-sm font-normal text-stone-400">({{ newsList.length }}条)</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="fetchNews" :disabled="loadingNews" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">
                {{ loadingNews ? '加载中...' : '刷新' }}
              </button>
              <button @click.stop="openCreateNewsModal" class="text-xs px-3 py-1.5 rounded-lg bg-cyan-500 text-white hover:bg-cyan-600 transition-colors">+ 创建</button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showAllNews }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
              </svg>
            </div>
          </div>
          
          <!-- Loading State -->
          <div v-if="loadingNews" class="p-8 text-center text-stone-500">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
            <p>正在加载资讯数据...</p>
          </div>
          
          <!-- News List (Collapsible) -->
          <div v-else-if="showAllNews" class="divide-y divide-stone-200">
            <div v-for="news in newsList" :key="news.id" class="px-6 py-4 hover:bg-stone-50 transition-colors">
              <div class="flex items-start space-x-4">
                <div class="flex-shrink-0 w-24 h-24 bg-gray-200 rounded-lg overflow-hidden flex-shrink-0">
                  <img v-if="news.imageUrl" :src="news.imageUrl" :alt="news.title" class="w-full h-full object-cover">
                  <div v-else class="w-full h-full flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600 text-white text-2xl font-bold">
                    {{ news.title?.charAt(0) || 'N' }}
                  </div>
                </div>
                <div class="flex-1 min-w-0 flex flex-col">
                  <div class="flex items-start justify-between gap-4">
                    <div class="flex-1 min-w-0">
                      <h4 class="text-lg font-bold text-stone-800 mb-1 line-clamp-1">{{ news.title }}</h4>
                      <p class="text-sm text-stone-600 line-clamp-2 mb-2">{{ news.content }}</p>
                      <div class="flex items-center flex-wrap gap-2 text-xs text-stone-500">
                        <span :class="getCategoryClass(news.category)" class="px-2 py-1 rounded-full font-medium whitespace-nowrap">
                          {{ getCategoryLabel(news.category) }}
                        </span>
                        <span class="whitespace-nowrap">浏览量: {{ news.viewCount || 0 }}</span>
                        <span class="whitespace-nowrap">{{ formatDate(news.createdAt) }}</span>
                      </div>
                    </div>
                    <div class="flex space-x-2 flex-shrink-0">
                      <button @click="openEditNewsModal(news)" class="text-blue-600 hover:text-blue-800 text-sm font-medium whitespace-nowrap">
                        编辑
                      </button>
                      <button @click="deleteNewsItem(news.id)" class="text-red-600 hover:text-red-800 text-sm font-medium whitespace-nowrap">
                        删除
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="newsList.length === 0" class="px-6 py-8 text-center text-stone-500">
              暂无资讯数据
            </div>
          </div>
          <div v-else-if="!loadingNews && newsList.length > 0" class="px-6 py-4 text-center text-stone-500 text-sm">
            点击上方标题栏展开查看全部资讯（共{{ newsList.length }}条）
          </div>
        </div>

        <!-- Carousel Management Section -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white cursor-pointer hover:bg-stone-100/50 transition-colors" @click="showCarousels = !showCarousels">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-pink-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-pink-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">轮播图管理 <span class="text-sm font-normal text-stone-400">({{ carousels.length }}张)</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="openCreateCarouselModal" class="text-xs px-3 py-1.5 rounded-lg bg-pink-500 text-white hover:bg-pink-600 transition-colors">+ 添加</button>
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
                    <span :class="c.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'" class="text-xs px-2 py-0.5 rounded">{{ c.active ? '启用' : '禁用' }}</span>
                    <div class="space-x-2">
                      <button @click="openEditCarouselModal(c)" class="text-blue-600 text-xs">编辑</button>
                      <button @click="deleteCarousel(c.id)" class="text-red-600 text-xs">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Route Management Section -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white cursor-pointer hover:bg-stone-100/50 transition-colors" @click="showRoutes = !showRoutes">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-teal-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-teal-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">线路管理 <span class="text-sm font-normal text-stone-400">({{ adminRoutes.length }}条)</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="fetchAdminRoutes" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">刷新</button>
              <button @click.stop="openCreateRouteModal" class="text-xs px-3 py-1.5 rounded-lg bg-teal-500 text-white hover:bg-teal-600 transition-colors">+ 新增</button>
              <svg class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showRoutes }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
            </div>
          </div>
          <div v-if="showRoutes" class="overflow-x-auto">
            <table class="min-w-full divide-y divide-stone-200">
              <thead class="bg-stone-50">
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">名称</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">天数</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">价格</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">难度</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">温度</th>
                  <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">地理</th>
                  <th class="px-4 py-3 text-right text-xs font-medium text-stone-500 uppercase">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-stone-200">
                <tr v-for="r in adminRoutes" :key="r.id" class="hover:bg-stone-50">
                  <td class="px-4 py-3 text-sm font-medium">{{ r.name }}</td>
                  <td class="px-4 py-3 text-sm">{{ r.days }}天</td>
                  <td class="px-4 py-3 text-sm text-red-600">¥{{ r.price }}</td>
                  <td class="px-4 py-3 text-sm">{{ r.difficulty }}</td>
                  <td class="px-4 py-3 text-sm">{{ r.temperature || '-' }}</td>
                  <td class="px-4 py-3 text-sm">{{ r.geography || '-' }}</td>
                  <td class="px-4 py-3 text-right text-sm space-x-2">
                    <button @click="openEditRouteModal(r)" class="text-blue-600">编辑</button>
                    <button @click="deleteRoute(r.id)" class="text-red-600">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Hotel Management Section -->
        <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-lg bg-indigo-100 flex items-center justify-center">
                <svg class="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
              </div>
              <h3 class="text-lg font-bold text-stone-800">酒店管理 <span class="text-sm font-normal text-stone-400">({{ adminHotels.length }}家)</span></h3>
            </div>
            <div class="flex items-center gap-2">
              <button @click.stop="fetchAdminHotels" class="text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors">刷新</button>
              <button @click.stop="openCreateHotelModal" class="text-xs px-4 py-2 rounded-lg bg-indigo-500 text-white hover:bg-indigo-600 transition-colors shadow-sm">+ 新增酒店</button>
            </div>
          </div>

          <!-- Card Grid -->
          <div class="p-4 grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div v-for="h in adminHotels" :key="h.id"
                 class="bg-white rounded-xl border border-stone-200 overflow-hidden hover:shadow-md transition-all duration-200"
                 :class="{ 'ring-2 ring-indigo-200 shadow-md': expandedHotelId === h.id }">
              <!-- Card Header (always visible) -->
              <div class="flex gap-4 p-4 cursor-pointer" @click="toggleHotelExpand(h)">
                <!-- Thumbnail -->
                <div class="w-24 h-24 shrink-0 rounded-lg overflow-hidden bg-stone-100">
                  <img v-if="h.imageUrl" :src="h.imageUrl" :alt="h.name"
                       class="w-full h-full object-cover"
                       @error="($event.target as HTMLImageElement).style.display='none'" />
                  <div v-if="!h.imageUrl" class="w-full h-full flex items-center justify-center text-stone-300">
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
                      <span class="truncate">{{ h.location || '未知位置' }}</span>
                    </div>
                    <div v-if="h.phone" class="flex items-center gap-1">
                      <svg class="w-3.5 h-3.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/></svg>
                      {{ h.phone }}
                    </div>
                  </div>
                  <div class="mt-2 flex items-center gap-2">
                    <span class="text-xs font-semibold text-red-600 bg-red-50 px-2 py-0.5 rounded">{{ h.priceRange || '咨询' }}</span>
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
                  <span class="text-xs font-semibold text-stone-500 uppercase tracking-wide">房型管理</span>
                  <span class="text-xs text-stone-400">{{ expandedRoomTypes.length }}个房型</span>
                </div>

                <!-- Room type list -->
                <div class="space-y-2 mb-3">
                  <div v-if="loadingRoomTypes" class="text-center py-2 text-xs text-stone-400">加载中...</div>
                  <div v-else-if="expandedRoomTypes.length === 0" class="text-center py-2 text-xs text-stone-400">暂无房型，请在下方添加</div>
                  <div v-else v-for="rt in expandedRoomTypes" :key="rt.id"
                       class="flex items-center justify-between bg-white rounded-lg border border-stone-200 px-3 py-2.5 text-sm">
                    <div class="flex-1 min-w-0">
                      <span class="font-medium text-stone-700">{{ rt.name }}</span>
                      <span class="ml-2 text-xs text-stone-400">¥{{ rt.price }} / 晚 · {{ rt.capacity }}人</span>
                      <span v-if="rt.amenities" class="ml-2 text-xs text-stone-300">· {{ rt.amenities }}</span>
                    </div>
                    <button @click="deleteRoomTypeInline(rt.id, h.id)" class="shrink-0 ml-2 text-xs text-red-400 hover:text-red-600 transition-colors">删除</button>
                  </div>
                </div>

                <!-- Add room type form -->
                <div class="flex items-center gap-1.5 bg-white rounded-lg border border-stone-200 px-2.5 py-2">
                  <input v-model="roomTypeForm.name" placeholder="名称" class="flex-1 min-w-0 text-xs border-0 outline-none px-1" @keyup.enter="addRoomTypeInline(h.id)">
                  <input v-model.number="roomTypeForm.price" type="number" placeholder="¥" class="w-14 text-xs border-0 outline-none text-right px-1" @keyup.enter="addRoomTypeInline(h.id)">
                  <input v-model.number="roomTypeForm.capacity" type="number" placeholder="人" class="w-8 text-xs border-0 outline-none text-right px-1" @keyup.enter="addRoomTypeInline(h.id)">
                  <button @click="addRoomTypeInline(h.id)" class="shrink-0 text-xs px-2.5 py-1.5 rounded-md bg-green-500 text-white hover:bg-green-600 transition-colors">+</button>
                </div>
              </div>

              <!-- Card Actions -->
              <div class="flex border-t border-stone-100 divide-x divide-stone-100">
                <button @click="openEditHotelModal(h)"
                        class="flex-1 py-2.5 text-xs text-stone-500 hover:text-blue-600 hover:bg-blue-50 transition-colors flex items-center justify-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                  编辑
                </button>
                <button @click="toggleHotelExpand(h)"
                        class="flex-1 py-2.5 text-xs text-stone-500 hover:text-indigo-600 hover:bg-indigo-50 transition-colors flex items-center justify-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
                  {{ expandedHotelId === h.id ? '收起' : '房型' }}
                </button>
                <button @click="deleteHotel(h.id)"
                        class="flex-1 py-2.5 text-xs text-stone-500 hover:text-red-600 hover:bg-red-50 transition-colors flex items-center justify-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                  删除
                </button>
              </div>
            </div>

            <!-- Empty state -->
            <div v-if="adminHotels.length === 0" class="col-span-full text-center py-16">
              <svg class="w-16 h-16 text-stone-200 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>
              <p class="text-stone-400">暂无酒店数据</p>
              <button @click="openCreateHotelModal" class="mt-3 text-sm text-indigo-500 hover:text-indigo-700">+ 添加第一家酒店</button>
            </div>
          </div>
        </div>

        <!-- Carousel Edit/Create Modal -->
        <div v-if="showCarouselModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" @click.self="closeCarouselModal">
          <div class="bg-white rounded-2xl max-w-lg w-full p-6 animate-scale-in">
            <h2 class="text-xl font-bold mb-4">{{ editingCarousel.id ? '编辑' : '创建' }}轮播图</h2>
            <div class="space-y-3">
              <div><label class="block text-sm font-medium mb-1">标题 *</label><input v-model="carouselForm.title" class="w-full border rounded px-3 py-2" placeholder="轮播标题"></div>
              <div><label class="block text-sm font-medium mb-1">副标题</label><input v-model="carouselForm.subtitle" class="w-full border rounded px-3 py-2" placeholder="副标题"></div>
              <div><label class="block text-sm font-medium mb-1">标签</label><input v-model="carouselForm.tag" class="w-full border rounded px-3 py-2" placeholder="如：热门推荐"></div>
              <div><label class="block text-sm font-medium mb-1">图片URL</label><input v-model="carouselForm.imageUrl" class="w-full border rounded px-3 py-2" placeholder="/images/banner.jpg"></div>
              <div><label class="block text-sm font-medium mb-1">链接URL</label><input v-model="carouselForm.linkUrl" class="w-full border rounded px-3 py-2" placeholder="/spots"></div>
              <div class="flex items-center gap-2"><label class="text-sm font-medium">排序</label><input v-model.number="carouselForm.sortOrder" type="number" class="border rounded px-2 py-1 w-20"></div>
              <div class="flex items-center gap-2"><input v-model="carouselForm.active" type="checkbox" id="carousel-active"><label for="carousel-active" class="text-sm">启用</label></div>
            </div>
            <div class="flex space-x-3 mt-6">
              <button @click="saveCarousel" class="flex-1 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600">保存</button>
              <button @click="closeCarouselModal" class="flex-1 bg-stone-200 py-2 rounded-lg">取消</button>
            </div>
          </div>
        </div>

        <!-- Route Edit/Create Modal -->
        <div v-if="showRouteModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" @click.self="closeRouteModal">
          <div class="bg-white rounded-2xl max-w-2xl w-full p-6 max-h-[85vh] overflow-y-auto animate-scale-in">
            <h2 class="text-xl font-bold mb-4">{{ editingRoute.id ? '编辑' : '创建' }}线路</h2>
            <div class="grid grid-cols-2 gap-3">
              <div><label class="block text-sm font-medium mb-1">名称 *</label><input v-model="routeForm.name" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">天数</label><input v-model.number="routeForm.days" type="number" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">价格</label><input v-model.number="routeForm.price" type="number" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">难度</label>
                <select v-model="routeForm.difficulty" class="w-full border rounded px-3 py-2">
                  <option value="EASY">简单</option><option value="MEDIUM">中等</option><option value="HARD">困难</option>
                </select>
              </div>
              <div><label class="block text-sm font-medium mb-1">温度</label><input v-model="routeForm.temperature" class="w-full border rounded px-3 py-2" placeholder="如: 15°C - 25°C"></div>
              <div><label class="block text-sm font-medium mb-1">地理特征</label><input v-model="routeForm.geography" class="w-full border rounded px-3 py-2" placeholder="如: 高原河谷地带"></div>
              <div class="col-span-2"><label class="block text-sm font-medium mb-1">描述</label><textarea v-model="routeForm.description" rows="3" class="w-full border rounded px-3 py-2"></textarea></div>
            </div>
            <div class="flex space-x-3 mt-6">
              <button @click="saveRoute" class="flex-1 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600">保存</button>
              <button @click="closeRouteModal" class="flex-1 bg-stone-200 py-2 rounded-lg">取消</button>
            </div>
          </div>
        </div>

        <!-- Hotel Edit/Create Modal -->
        <div v-if="showHotelModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4" @click.self="closeHotelModal">
          <div class="bg-white rounded-2xl max-w-2xl w-full p-6 max-h-[85vh] overflow-y-auto animate-scale-in">
            <h2 class="text-xl font-bold mb-4">{{ editingHotel.id ? '编辑' : '创建' }}酒店</h2>
            <div class="grid grid-cols-2 gap-3">
              <div><label class="block text-sm font-medium mb-1">名称 *</label><input v-model="hotelForm.name" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">位置</label><input v-model="hotelForm.location" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">电话</label><input v-model="hotelForm.phone" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">价格区间</label><input v-model="hotelForm.priceRange" class="w-full border rounded px-3 py-2" placeholder="¥500 - ¥1500"></div>
              <div><label class="block text-sm font-medium mb-1">评分</label><input v-model.number="hotelForm.rating" type="number" step="0.1" class="w-full border rounded px-3 py-2"></div>
              <div><label class="block text-sm font-medium mb-1">图片URL</label><input v-model="hotelForm.imageUrl" class="w-full border rounded px-3 py-2"></div>
              <div class="col-span-2"><label class="block text-sm font-medium mb-1">设施</label><input v-model="hotelForm.facilities" class="w-full border rounded px-3 py-2" placeholder="WiFi, 停车场, 餐厅..."></div>
            </div>
            <div class="flex space-x-3 mt-6">
              <button @click="saveHotel" class="flex-1 bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600">保存</button>
              <button @click="closeHotelModal" class="flex-1 bg-stone-200 py-2 rounded-lg">取消</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit/Create News Modal -->
    <div
      v-if="showNewsModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto"
      @click.self="closeNewsModal"
    >
      <div class="bg-white rounded-2xl max-w-3xl w-full p-8 animate-scale-in max-h-[90vh] overflow-y-auto">
        <h2 class="text-2xl font-bold mb-6 text-stone-800">{{ editingNews.id ? '编辑资讯' : '创建资讯' }}</h2>
        
        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">标题 <span class="text-red-500">*</span></label>
          <input v-model="newsForm.title" type="text" required
                 class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                 placeholder="输入资讯标题">
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">内容 <span class="text-red-500">*</span></label>
          <textarea v-model="newsForm.content" rows="8" required
                    class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-none"
                    placeholder="输入资讯内容..."></textarea>
          <p class="text-xs text-stone-500 mt-2">当前字数：{{ newsForm.content.length }}</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">分类 <span class="text-red-500">*</span></label>
          <select v-model="newsForm.category" required
                  class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none">
            <option value="">请选择分类</option>
            <option value="POLICY">政策</option>
            <option value="EVENT">活动</option>
            <option value="NOTICE">通知</option>
          </select>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">图片URL</label>
          <input v-model="newsForm.imageUrl" type="url"
                 class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                 placeholder="https://example.com/image.jpg">
          <p class="text-xs text-stone-500 mt-2">提示：输入有效的图片URL地址，建议使用600px宽度的图片</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">浏览量</label>
          <input v-model.number="newsForm.viewCount" type="number" min="0"
                 class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                 placeholder="0">
        </div>

        <div v-if="newsForm.imageUrl" class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">图片预览</label>
          <div class="relative h-64 bg-gray-200 rounded-lg overflow-hidden">
            <img :src="newsForm.imageUrl" alt="预览" class="w-full h-full object-cover" @error="newsImageError = true">
            <div v-if="newsImageError" class="absolute inset-0 flex items-center justify-center bg-red-100 text-red-600">
              <p>图片加载失败，请检查URL是否正确</p>
            </div>
          </div>
        </div>

        <div class="flex space-x-4">
          <button @click="saveNews" :disabled="updatingNews || !newsForm.title || !newsForm.content || !newsForm.category"
                  class="flex-1 bg-blue-500 text-white py-3 rounded-lg hover:bg-blue-600 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed">
            {{ updatingNews ? '保存中...' : (editingNews.id ? '保存修改' : '创建资讯') }}
          </button>
          <button @click="closeNewsModal" :disabled="updatingNews"
                  class="flex-1 bg-stone-200 text-stone-700 py-3 rounded-lg hover:bg-stone-300 transition-colors disabled:cursor-not-allowed">
            取消
          </button>
        </div>
      </div>
    </div>

    <!-- Edit Spot Modal -->
    <div
      v-if="showEditModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto"
      @click.self="closeEditModal"
    >
      <div class="bg-white rounded-2xl max-w-2xl w-full p-8 animate-scale-in max-h-[85vh] overflow-y-auto">
        <h2 class="text-2xl font-bold mb-6 text-stone-800">编辑景点信息</h2>
        
        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">景点名称</label>
          <p class="text-lg font-bold text-stone-900">{{ editingSpot.name }}</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">当前封面</label>
          <div class="relative h-64 bg-gray-200 rounded-lg overflow-hidden mb-4">
            <img v-if="editingSpot.imageUrl" :src="editingSpot.imageUrl" :alt="editingSpot.name" class="w-full h-full object-cover">
            <div v-else class="w-full h-full flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600 text-white text-6xl font-bold">
              {{ editingSpot.name?.charAt(0) }}
            </div>
          </div>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">新图片URL</label>
          <input v-model="newImageUrl" type="url" 
                 class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                 placeholder="https://example.com/image.jpg">
          <p class="text-xs text-stone-500 mt-2">提示：输入有效的图片URL地址，建议使用600px宽度的图片</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">门票价格（元）</label>
          <div class="flex items-center space-x-2">
            <span class="text-stone-500 text-sm">当前：</span>
            <span class="font-semibold text-red-600 mr-4">¥{{ editingSpot.ticketPrice }}</span>
          </div>
          <input
            v-model.number="newTicketPrice"
            type="number"
            min="0"
            step="0.01"
            class="mt-2 w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            placeholder="不修改则留空，例：199.00"
          >
          <p class="text-xs text-stone-500 mt-2">提示：输入新的门票价格（保留两位小数），不修改价格时可留空。</p>
        </div>

        <div class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">景点介绍</label>
          <textarea v-model="newDescription" rows="6"
                    class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-none"
                    placeholder="输入景点介绍..."></textarea>
          <p class="text-xs text-stone-500 mt-2">当前字数：{{ newDescription.length }}</p>
        </div>

        <div v-if="newImageUrl" class="mb-6">
          <label class="block text-sm font-medium text-stone-700 mb-2">预览</label>
          <div class="relative h-64 bg-gray-200 rounded-lg overflow-hidden">
            <img :src="newImageUrl" alt="预览" class="w-full h-full object-cover" @error="imageError = true">
            <div v-if="imageError" class="absolute inset-0 flex items-center justify-center bg-red-100 text-red-600">
              <p>图片加载失败，请检查URL是否正确</p>
            </div>
          </div>
        </div>

        <div class="flex space-x-4">
          <button @click="updateSpotImage" :disabled="imageError || updating"
                  class="flex-1 bg-blue-500 text-white py-3 rounded-lg hover:bg-blue-600 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed">
            {{ updating ? '保存中...' : '保存修改' }}
          </button>
          <button @click="closeEditModal" :disabled="updating"
                  class="flex-1 bg-stone-200 text-stone-700 py-3 rounded-lg hover:bg-stone-300 transition-colors disabled:cursor-not-allowed">
            取消
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import AdminAnalyticsPanel from '../components/AdminAnalyticsPanel.vue'
import api, { endpoints, clearTokenCache } from '../api'

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
  updatedAt?: string
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

const fetchStats = async () => {
  try {
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      window.location.href = '/login'
      return
    }

    const user = JSON.parse(userStr)
    if (!user.token) {
      localStorage.removeItem('user')
      localStorage.removeItem('token')
      window.location.href = '/login'
      return
    }

    // 简单检查 token 是否过期（JWT payload 中间段包含 exp）
    try {
      const payload = JSON.parse(atob(user.token.split('.')[1]))
      if (payload.exp && payload.exp * 1000 < Date.now()) {
        localStorage.removeItem('user')
        localStorage.removeItem('token')
        window.location.href = '/login'
        return
      }
    } catch (_) { /* 解析失败继续尝试请求 */ }

    if (user.role !== 'ADMIN') {
      alert('您没有管理员权限，请使用管理员账户登录')
      window.location.href = '/'
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
      localStorage.removeItem('user')
      localStorage.removeItem('token')
      clearTokenCache()
      window.location.href = '/login'
      return
    }
    if (status === 403) {
      alert('禁止访问：您没有管理员权限，请使用管理员账户登录')
      window.location.href = '/'
    } else {
      analyticsError.value = error.response?.data?.message || error.response?.data?.error || '统计面板加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
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
const auditLogs = ref<any[]>([])
const loadingAuditLogs = ref(false)
const spots = ref<any[]>([])
const loadingSpots = ref(false)
const spotsError = ref('')
const showAllSpots = ref(false)
const showEditModal = ref(false)
const editingSpot = ref<any>({})
const newImageUrl = ref('')
const newDescription = ref('')
const newTicketPrice = ref<number | null>(null)
const imageError = ref(false)
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
const newsImageError = ref(false)
const updatingNews = ref(false)

// Computed property to control displayed spots
const displayedSpots = computed(() => {
  return showAllSpots.value ? spots.value : spots.value.slice(0, 6)
})

const fetchUsers = async () => {
  try {
    console.log('正在获取用户数据...')
    const response = await api.get(endpoints.admin.users)
    console.log('用户数据响应:', response)
    if (Array.isArray(response.data)) {
      users.value = response.data
      console.log(`成功加载 ${users.value.length} 个用户`)
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
    console.log('=== 开始获取景点数据 ===')
    console.log('请求 URL:', endpoints.admin.spots)
    console.log('完整路径:', '/api' + endpoints.admin.spots)
    
    // 检查认证 token
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      console.log('用户 token 存在:', user.token ? '是' : '否')
    } else {
      console.warn('未找到用户信息，可能未登录')
    }
    
    const response = await api.get(endpoints.admin.spots)
    console.log('景点数据响应状态:', response.status)
    console.log('景点数据响应头:', response.headers)
    console.log('景点数据响应数据:', response.data)
    console.log('数据类型:', Array.isArray(response.data) ? '数组' : typeof response.data)
    
    const spotsData = response.data?.content || response.data
    if (Array.isArray(spotsData)) {
      spots.value = spotsData
      console.log(`✅ 成功加载 ${spots.value.length} 个景点`)
      if (spots.value.length > 0) {
        console.log('第一个景点示例:', spots.value[0])
      }
    } else {
      console.error('❌ 响应数据格式错误:', response.data)
      spotsError.value = '响应数据格式错误: 期望数组，但收到 ' + typeof response.data
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
        spotsError.value = '未授权：请先登录管理员账户'
      } else if (error.response.status === 403) {
        spotsError.value = '禁止访问：您没有管理员权限'
      } else {
        spotsError.value = error.response.data?.message || `服务器错误 (${error.response.status})`
      }
    } else if (error.request) {
      console.error('请求已发送但无响应')
      console.error('请求配置:', error.config)
      spotsError.value = '无法连接到后端服务，请检查：1) 后端是否运行在 http://localhost:8080 2) 网络连接是否正常'
    } else {
      console.error('请求配置错误:', error.config)
      spotsError.value = error.message || '未知错误'
    }
    spots.value = []
  } finally {
    loadingSpots.value = false
    console.log('=== 获取景点数据完成 ===')
  }
}

const openEditModal = (spot: any) => {
  editingSpot.value = { ...spot }
  newImageUrl.value = spot.imageUrl || ''
  newDescription.value = spot.description || ''
  newTicketPrice.value = spot.ticketPrice ?? null
  imageError.value = false
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
  imageError.value = false

  // 恢复主页面滚动
  if (typeof document !== 'undefined' && originalBodyOverflow.value !== null) {
    document.body.style.overflow = originalBodyOverflow.value
    originalBodyOverflow.value = null
  }
}

const updateSpotImage = async () => {
  if (imageError.value) {
    alert('图片URL无效，请检查后重试')
    return
  }
  
  updating.value = true
  try {
    const payload: any = {}
    if (newImageUrl.value) payload.imageUrl = newImageUrl.value
    if (newDescription.value) payload.description = newDescription.value
    if (newTicketPrice.value !== null && !Number.isNaN(newTicketPrice.value)) {
      payload.ticketPrice = newTicketPrice.value
    }

    if (Object.keys(payload).length === 0) {
      alert('请至少修改一项内容')
      updating.value = false
      return
    }
    
    await api.put(endpoints.admin.updateSpot(editingSpot.value.id), payload)
    alert('景点信息更新成功！')
    await fetchSpots()
    closeEditModal()
  } catch (error) {
    console.error('Failed to update spot:', error)
    alert('更新失败，请重试')
  } finally {
    updating.value = false
  }
}

const updateRole = async (userId: number, newRole: string) => {
  if (!confirm(`确定要将该用户设置为 ${newRole} 吗？`)) return

  try {
    await api.post(endpoints.admin.updateRole(userId), { role: newRole })
    await fetchUsers() // 刷新列表
    alert('操作成功')
  } catch (error) {
    console.error('Failed to update role:', error)
    alert('操作失败')
  }
}

const deleteUser = async (user: any) => {
  if (!confirm(`确定要删除用户 "${user.username}" 吗？此操作不可恢复。`)) return

  try {
    await api.delete(endpoints.admin.deleteUser(user.id))
    alert(`用户 "${user.username}" 已删除`)
    await fetchUsers()
  } catch (error: any) {
    console.error('Failed to delete user:', error)
    alert(error.response?.data?.error || '删除失败')
  }
}

const fetchAuditLogs = async () => {
  if (!isSuperAdmin.value) return
  loadingAuditLogs.value = true
  try {
    const response = await api.get(endpoints.admin.auditLogs)
    auditLogs.value = Array.isArray(response.data) ? response.data : []
  } catch (error: any) {
    console.error('Failed to fetch audit logs:', error)
    auditLogs.value = []
  } finally {
    loadingAuditLogs.value = false
  }
}

// Hotel orders management
const hotelOrders = ref<any[]>([])
const loadingHotelOrders = ref(false)
const showAllHotelOrders = ref(false)
const showRecentOrders = ref(true)
const showPopularSpots = ref(true)
const showUsers = ref(false)

const fetchHotelOrders = async () => {
  loadingHotelOrders.value = true
  try {
    const response = await api.get(endpoints.hotelBookings.all)
    hotelOrders.value = Array.isArray(response.data?.content) ? response.data.content : (Array.isArray(response.data) ? response.data : [])
  } catch (error: any) {
    console.error('Failed to fetch hotel orders:', error)
    hotelOrders.value = []
  } finally {
    loadingHotelOrders.value = false
  }
}

const updateHotelOrderStatus = async (orderId: number, status: string) => {
  try {
    await api.put(endpoints.hotelBookings.updateStatus(orderId), { status })
    await fetchHotelOrders()
  } catch (error) {
    console.error('Failed to update hotel order status:', error)
    alert('更新状态失败')
  }
}

const deleteHotelOrder = async (orderId: number) => {
  if (!confirm('确定要删除此订单吗？')) return
  try {
    await api.delete(endpoints.hotelBookings.cancel(orderId))
    await fetchHotelOrders()
    alert('删除成功')
  } catch (error) {
    console.error('Failed to delete hotel order:', error)
    alert('删除失败')
  }
}

const fetchNews = async () => {
  loadingNews.value = true
  try {
    console.log('=== 开始获取资讯数据 ===')
    console.log('请求 URL:', endpoints.admin.news)
    
    // 检查认证 token
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      alert('未登录，请先登录')
      window.location.href = '/login'
      return
    }
    
    const user = JSON.parse(userStr)
    console.log('用户信息:', user)
    console.log('用户 token 存在:', user.token ? '是' : '否')
    console.log('用户角色:', user.role)
    
    if (!user.token) {
      alert('登录已过期，请重新登录')
      localStorage.removeItem('user')
      window.location.href = '/login'
      return
    }
    
    if (user.role !== 'ADMIN') {
      alert('您没有管理员权限')
      return
    }
    
    const response = await api.get(endpoints.admin.news)
    console.log('资讯数据响应状态:', response.status)
    console.log('资讯数据响应数据:', response.data)
    console.log('数据类型:', Array.isArray(response.data) ? '数组' : typeof response.data)
    
    const newsData = response.data?.content || response.data
    if (Array.isArray(newsData)) {
      newsList.value = newsData
      console.log(`✅ 成功加载 ${newsList.value.length} 条资讯`)
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
        alert('未授权：请先登录管理员账户')
      } else if (error.response.status === 403) {
        alert('禁止访问：您没有管理员权限')
      } else {
        const errorMsg = error.response.data?.message || error.response.data || `服务器错误 (${error.response.status})`
        alert('获取资讯列表失败: ' + errorMsg)
      }
    } else if (error.request) {
      console.error('请求已发送但无响应')
      alert('无法连接到后端服务，请检查：1) 后端是否运行在 http://localhost:8080 2) 网络连接是否正常')
    } else {
      console.error('请求配置错误:', error.config)
      alert('获取资讯列表失败: ' + (error.message || '未知错误'))
    }
    newsList.value = []
  } finally {
    loadingNews.value = false
    console.log('=== 获取资讯数据完成 ===')
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
  newsImageError.value = false
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
  newsImageError.value = false
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
  newsImageError.value = false
  if (typeof document !== 'undefined' && originalBodyOverflow.value !== null) {
    document.body.style.overflow = originalBodyOverflow.value
    originalBodyOverflow.value = null
  }
}

const saveNews = async () => {
  if (!newsForm.value.title || !newsForm.value.content || !newsForm.value.category) {
    alert('请填写标题、内容和分类')
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
      alert('资讯更新成功！')
    } else {
      // Create new news
      await api.post(endpoints.admin.createNews, payload)
      alert('资讯创建成功！')
    }
    await fetchNews()
    closeNewsModal()
  } catch (error) {
    console.error('Failed to save news:', error)
    alert('保存失败，请重试')
  } finally {
    updatingNews.value = false
  }
}

const deleteNewsItem = async (id: number) => {
  if (!confirm('确定要删除这条资讯吗？此操作不可恢复。')) {
    return
  }

  try {
    await api.delete(endpoints.admin.deleteNews(id))
    alert('删除成功！')
    await fetchNews()
  } catch (error) {
    console.error('Failed to delete news:', error)
    alert('删除失败，请重试')
  }
}

const getCategoryLabel = (category: string) => {
  const labels: Record<string, string> = {
    POLICY: '政策',
    EVENT: '活动',
    NOTICE: '通知'
  }
  return labels[category] || category
}

const getAuditActionLabel = (action: string) => {
  switch (action) {
    case 'DELETE_USER': return '删除用户成功'
    case 'DELETE_USER_DENIED': return '删除用户拒绝'
    default: return action
  }
}

const getAuditActionClass = (action: string) => {
  switch (action) {
    case 'DELETE_USER':
      return 'bg-green-100 text-green-800'
    case 'DELETE_USER_DENIED':
      return 'bg-red-100 text-red-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
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
const isSuperAdmin = computed(() => {
  return currentUser.value?.username === 'lzh'
})

// 加载当前用户信息
const loadCurrentUser = () => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      currentUser.value = JSON.parse(userStr)
      console.log('当前登录用户:', currentUser.value?.username, '是否超级管理员:', isSuperAdmin.value)
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
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
    alert('保存成功')
  } catch (e) { alert('保存失败') }
}

const deleteCarousel = async (id: number) => {
  if (!confirm('确定删除？')) return
  try {
    await api.delete(endpoints.carousels.adminDelete(id))
    await fetchCarousels()
  } catch (e) { alert('删除失败') }
}

// Route management
const adminRoutes = ref<any[]>([])
const showRoutes = ref(false)
const showRouteModal = ref(false)
const editingRoute = ref<any>({})
const routeForm = ref({ name: '', days: 1, price: 0, difficulty: 'EASY', description: '', temperature: '', geography: '' })

const fetchAdminRoutes = async () => {
  try {
    const res = await api.get(endpoints.adminRoutes.list)
    adminRoutes.value = Array.isArray(res.data) ? res.data : []
  } catch (e) { adminRoutes.value = [] }
}

const openCreateRouteModal = () => {
  editingRoute.value = {}
  routeForm.value = { name: '', days: 1, price: 0, difficulty: 'EASY', description: '', temperature: '', geography: '' }
  showRouteModal.value = true
}

const openEditRouteModal = (r: any) => {
  editingRoute.value = { ...r }
  routeForm.value = { name: r.name || '', days: r.days || 1, price: r.price || 0, difficulty: r.difficulty || 'EASY', description: r.description || '', temperature: r.temperature || '', geography: r.geography || '' }
  showRouteModal.value = true
}

const closeRouteModal = () => { showRouteModal.value = false }

const saveRoute = async () => {
  try {
    const payload = { ...routeForm.value }
    if (editingRoute.value.id) {
      await api.put(endpoints.adminRoutes.update(editingRoute.value.id), payload)
    } else {
      await api.post(endpoints.adminRoutes.create, payload)
    }
    await fetchAdminRoutes()
    closeRouteModal()
    alert('保存成功')
  } catch (e) { alert('保存失败') }
}

const deleteRoute = async (id: number) => {
  if (!confirm('确定删除？')) return
  try {
    await api.delete(endpoints.adminRoutes.delete(id))
    await fetchAdminRoutes()
  } catch (e) { alert('删除失败') }
}

// Hotel management
const adminHotels = ref<any[]>([])
const showHotelModal = ref(false)
const editingHotel = ref<any>({})
const hotelForm = ref({ name: '', location: '', phone: '', priceRange: '', rating: 0, imageUrl: '', facilities: '' })

const fetchAdminHotels = async () => {
  try {
    const res = await api.get(endpoints.adminHotels.list)
    adminHotels.value = Array.isArray(res.data) ? res.data : []
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
    alert('保存成功')
  } catch (e) { alert('保存失败') }
}

const deleteHotel = async (id: number) => {
  if (!confirm('确定删除？')) return
  try {
    await api.delete(endpoints.adminHotels.delete(id))
    await fetchAdminHotels()
  } catch (e) { alert('删除失败') }
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
  } catch (e) { alert('添加失败') }
}

const deleteRoomTypeInline = async (roomTypeId: number, hotelId: number) => {
  if (!confirm('确定删除该房型？')) return
  try {
    await api.delete(endpoints.adminHotels.deleteRoomType(roomTypeId))
    const res = await api.get(endpoints.adminHotels.roomTypes(hotelId))
    expandedRoomTypes.value = Array.isArray(res.data) ? res.data : []
  } catch (e) { alert('删除失败') }
}

onMounted(() => {
  loadCurrentUser()
  fetchStats()
  fetchUsers()
  fetchSpots()
  fetchHotelOrders()
  fetchNews()
  fetchCarousels()
  fetchAdminRoutes()
  fetchAdminHotels()
  fetchAuditLogs()
})

onUnmounted(() => {
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

@keyframes scale-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.animate-scale-in {
  animation: scale-in 0.2s ease-out;
}
</style>
