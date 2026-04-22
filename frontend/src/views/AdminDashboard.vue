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
        <div class="bg-white rounded-lg shadow overflow-hidden">
          <div class="px-6 py-4 border-b border-stone-200 flex justify-between items-center cursor-pointer hover:bg-stone-50 transition-colors" @click="showAllHotelOrders = !showAllHotelOrders">
            <h3 class="text-lg font-bold text-stone-800">酒店订单 <span class="text-sm font-normal text-stone-500">(共{{ hotelOrders.length }}条)</span></h3>
            <div class="flex items-center space-x-3">
              <button @click.stop="fetchHotelOrders" class="text-sm text-blue-600 hover:text-blue-800" :disabled="loadingHotelOrders">
                {{ loadingHotelOrders ? '加载中...' : '刷新列表' }}
              </button>
              <button @click.stop="showAllHotelOrders = !showAllHotelOrders" class="text-sm text-stone-600 hover:text-stone-800 flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 transition-transform duration-200" :class="{ 'rotate-180': showAllHotelOrders }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
                <span class="ml-1">{{ showAllHotelOrders ? '收起' : '展开' }}</span>
              </button>
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

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <!-- Recent Bookings -->
          <div class="bg-white rounded-lg shadow overflow-hidden">
            <div class="px-6 py-4 border-b border-stone-200">
              <h3 class="text-lg font-bold text-stone-800">最新订单</h3>
            </div>
            <div class="divide-y divide-stone-200">
              <template v-if="recentOrders.length > 0">
                <div v-for="order in recentOrders" :key="order.id" class="px-6 py-4 flex items-center justify-between">
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
              <div v-else class="px-6 py-4 text-center text-stone-500">
                暂无订单
              </div>
            </div>
          </div>

          <!-- Popular Spots -->
          <div class="bg-white rounded-lg shadow overflow-hidden">
            <div class="px-6 py-4 border-b border-stone-200">
              <h3 class="text-lg font-bold text-stone-800">热门景点</h3>
            </div>
            <div class="divide-y divide-stone-200">
              <div v-for="(spot, index) in sortedPopularSpots" :key="spot.id" class="px-6 py-4 flex items-center">
                <span class="text-lg font-bold text-stone-400 w-8">{{ index + 1 }}</span>
                <img :src="spot.imageUrl" class="w-10 h-10 rounded object-cover mr-4" alt="">
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-stone-800">{{ spot.name }}</p>
                  <p class="text-xs text-stone-500 mb-2">{{ spot.location }}</p>
                  <!-- 点击量条 -->
                  <div class="flex items-center gap-2 mt-1">
                    <div class="flex-1 bg-stone-100 rounded-full h-2 overflow-hidden">
                      <div 
                        class="h-full bg-gradient-to-r from-blue-400 to-blue-600 rounded-full transition-all duration-500"
                        :style="{ width: getClickCountPercentage(spot) + '%' }"
                      ></div>
                    </div>
                    <span class="text-xs text-stone-600 font-medium whitespace-nowrap">
                      {{ spot.visitCount || 0 }}次
                    </span>
                  </div>
                </div>
                <div class="text-right ml-4">
                  <p class="text-sm font-bold text-red-600">¥{{ spot.ticketPrice }}</p>
                </div>
              </div>
              <div v-if="!sortedPopularSpots?.length" class="px-6 py-4 text-center text-stone-500">
                暂无数据
              </div>
            </div>
          </div>
        </div>
        
        <!-- Spots Management Section -->
        <div class="bg-white rounded-lg shadow overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-200 flex justify-between items-center">
            <h3 class="text-lg font-bold text-stone-800">景点管理 <span class="text-sm font-normal text-stone-500">(共{{ spots.length }}个)</span></h3>
            <div class="flex space-x-3">
              <button @click="fetchSpots" class="text-sm text-blue-600 hover:text-blue-800" :disabled="loadingSpots">
                {{ loadingSpots ? '加载中...' : '刷新列表' }}
              </button>
              <button v-if="spots.length > 6 && !showAllSpots" @click="showAllSpots = true" class="text-sm text-blue-600 hover:text-blue-800">
                查看全部
              </button>
              <button v-if="showAllSpots" @click="showAllSpots = false" class="text-sm text-stone-600 hover:text-stone-800">
                收起
              </button>
            </div>
          </div>
          
          <!-- Loading State -->
          <div v-if="loadingSpots" class="p-8 text-center text-stone-500">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
            <p>正在加载景点数据...</p>
          </div>
          
          <!-- Error State -->
          <div v-else-if="spotsError" class="p-8 text-center text-red-500">
            <p class="mb-2">❌ 加载失败</p>
            <p class="text-sm">{{ spotsError }}</p>
            <button @click="fetchSpots" class="mt-4 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">
              重试
            </button>
          </div>
          
          <!-- Empty State -->
          <div v-else-if="spots.length === 0" class="p-8 text-center text-stone-500">
            <p>暂无景点数据</p>
            <p class="text-sm mt-2">请确保后端服务已启动并且数据库中有景点数据</p>
          </div>
          
          <!-- Spots Grid (Compact View) -->
          <div v-else class="p-6">
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
              <div v-for="spot in displayedSpots" :key="spot.id" 
                   class="group cursor-pointer border border-stone-200 rounded-lg overflow-hidden hover:shadow-lg hover:border-blue-400 transition-all"
                   @click="openEditModal(spot)">
                <div class="relative h-24 bg-gray-200">
                  <img v-if="spot.imageUrl" :src="spot.imageUrl" :alt="spot.name" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-300">
                  <div v-else class="w-full h-full flex items-center justify-center bg-gradient-to-br from-blue-500 to-purple-600 text-white text-2xl font-bold">
                    {{ spot.name.charAt(0) }}
                  </div>
                  <!-- Overlay on hover -->
                  <div class="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-30 transition-all flex items-center justify-center">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-white opacity-0 group-hover:opacity-100 transition-opacity" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </div>
                </div>
                <div class="p-2">
                  <h4 class="font-bold text-stone-800 text-sm truncate">{{ spot.name }}</h4>
                  <p class="text-xs text-red-600 font-semibold">¥{{ spot.ticketPrice }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- User Management Section -->
        <div class="bg-white rounded-lg shadow overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-200 flex justify-between items-center">
            <h3 class="text-lg font-bold text-stone-800">用户管理</h3>
            <button @click="fetchUsers" class="text-sm text-blue-600 hover:text-blue-800">刷新列表</button>
          </div>
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
                        <span class="text-xs bg-gray-100 px-2 py-1 rounded" title="密码已加密存储">
                          已加密存储
                        </span>
                        <button 
                          v-if="isSuperAdmin" 
                          @click="openPasswordModal(u)"
                          class="text-xs text-blue-600 hover:text-blue-800 hover:underline"
                          title="点击查看原始密码（仅超级管理员）"
                        >
                          查看密码
                        </button>
                        <span 
                          v-else
                          class="text-xs text-gray-400"
                          title="仅超级管理员可以查看密码"
                        >
                          无权限
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
            <h3 class="text-lg font-bold text-stone-800">密码解密审计日志</h3>
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

        <!-- News Management Section -->
        <div class="bg-white rounded-lg shadow overflow-hidden mt-8">
          <div class="px-6 py-4 border-b border-stone-200 flex justify-between items-center cursor-pointer hover:bg-stone-50 transition-colors" @click="showAllNews = !showAllNews">
            <h3 class="text-lg font-bold text-stone-800">旅游资讯管理 <span class="text-sm font-normal text-stone-500">(共{{ newsList.length }}条)</span></h3>
            <div class="flex items-center space-x-3">
              <button @click.stop="fetchNews" class="text-sm text-blue-600 hover:text-blue-800" :disabled="loadingNews">
                {{ loadingNews ? '加载中...' : '刷新列表' }}
              </button>
              <button @click.stop="openCreateNewsModal" class="text-sm bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600 transition-colors">
                创建资讯
              </button>
              <button @click.stop="showAllNews = !showAllNews" class="text-sm text-stone-600 hover:text-stone-800 flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 transition-transform duration-200" :class="{ 'rotate-180': showAllNews }" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
                <span class="ml-1">{{ showAllNews ? '收起' : '展开' }}</span>
              </button>
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
      </div>
    </div>

    <!-- 查看密码弹窗 -->
    <div
      v-if="showPasswordModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="closePasswordModal"
    >
      <div class="bg-white rounded-2xl max-w-md w-full p-6">
        <h2 class="text-xl font-bold mb-4 text-stone-800">查看用户密码</h2>
        <p class="text-sm text-stone-600 mb-3">用户：{{ selectedUserForPassword?.username }}</p>

        <div v-if="passwordModalLoading" class="text-sm text-stone-500">正在解密，请稍候...</div>
        <div v-else-if="passwordModalError" class="text-sm text-red-600 bg-red-50 p-3 rounded">{{ passwordModalError }}</div>
        <div v-else class="bg-green-50 border border-green-200 rounded p-3">
          <p class="text-xs text-green-700 mb-1">明文密码</p>
          <p class="font-mono text-base text-green-900 break-all">{{ revealedPassword }}</p>
        </div>

        <div class="mt-5 flex justify-end">
          <button @click="closePasswordModal" class="px-4 py-2 bg-stone-200 text-stone-700 rounded-lg hover:bg-stone-300">
            关闭
          </button>
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
import api, { endpoints } from '../api'

interface Stats {
  userCount: number
  orderCount: number
  totalRevenue: number
  recentBookings: any[]
  recentHotelBookings: any[]
  popularSpots: any[]
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

const fetchStats = async () => {
  try {
    // 检查用户登录状态
    const userStr = localStorage.getItem('user')
    if (!userStr) {
      alert('未登录，请先登录管理员账户')
      window.location.href = '/login'
      return
    }
    
    const user = JSON.parse(userStr)
    if (!user.token) {
      alert('登录已过期，请重新登录')
      localStorage.removeItem('user')
      window.location.href = '/login'
      return
    }
    
    if (user.role !== 'ADMIN') {
      alert('您没有管理员权限')
      window.location.href = '/'
      return
    }
    
    console.log('📊 [AdminDashboard] 开始获取统计数据，用户:', user.username, '角色:', user.role)
    const response = await api.get(endpoints.admin.stats)
    stats.value = response.data
    console.log('✅ [AdminDashboard] 统计数据获取成功')
  } catch (error: any) {
    console.error('❌ [AdminDashboard] 获取统计数据失败:', error)
    if (error.response?.status === 401) {
      alert('未授权：请先登录管理员账户')
      localStorage.removeItem('user')
      window.location.href = '/login'
    } else if (error.response?.status === 403) {
      alert('禁止访问：您没有管理员权限')
      window.location.href = '/'
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
const showPasswordModal = ref(false)
const passwordModalLoading = ref(false)
const passwordModalError = ref('')
const selectedUserForPassword = ref<any>(null)
const revealedPassword = ref('')
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
    
    if (Array.isArray(response.data)) {
      spots.value = response.data
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
    await fetchUsers() // Refresh list
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

const openPasswordModal = async (user: any) => {
  if (!isSuperAdmin.value) {
    alert('权限不足：仅超级管理员可以查看密码')
    return
  }

  selectedUserForPassword.value = user
  revealedPassword.value = ''
  passwordModalError.value = ''
  showPasswordModal.value = true
  passwordModalLoading.value = true

  try {
    const response = await api.post(endpoints.admin.decryptPassword(user.id))
    if (response.data.password) {
      revealedPassword.value = response.data.password
    } else {
      passwordModalError.value = response.data.message || '该用户没有可解密密码'
    }
  } catch (error: any) {
    console.error('Failed to decrypt password:', error)
    passwordModalError.value = error.response?.data?.error || error.response?.data?.message || '解密失败，请重试'
  } finally {
    passwordModalLoading.value = false
    await fetchAuditLogs()
  }
}

const closePasswordModal = () => {
  showPasswordModal.value = false
  selectedUserForPassword.value = null
  revealedPassword.value = ''
  passwordModalError.value = ''
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

const fetchHotelOrders = async () => {
  loadingHotelOrders.value = true
  try {
    const response = await api.get(endpoints.hotelBookings.all)
    hotelOrders.value = Array.isArray(response.data) ? response.data : []
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
    
    if (Array.isArray(response.data)) {
      newsList.value = response.data
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
    case 'DECRYPT_PASSWORD': return '查看密码成功'
    case 'DECRYPT_PASSWORD_DENIED': return '查看密码拒绝'
    case 'DELETE_USER': return '删除用户成功'
    case 'DELETE_USER_DENIED': return '删除用户拒绝'
    default: return action
  }
}

const getAuditActionClass = (action: string) => {
  switch (action) {
    case 'DECRYPT_PASSWORD':
    case 'DELETE_USER':
      return 'bg-green-100 text-green-800'
    case 'DECRYPT_PASSWORD_DENIED':
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

onMounted(() => {
  loadCurrentUser()
  fetchStats()
  fetchUsers()
  fetchSpots()
  fetchHotelOrders()
  fetchNews()
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
