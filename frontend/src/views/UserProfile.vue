<script setup lang="ts">
import NavBar from '@/components/NavBar.vue'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api, { endpoints } from '@/api'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const bookings = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  if (!user.value) {
    router.push('/login')
    return
  }
  fetchBookings()
})

const fetchBookings = async () => {
  try {
    const response = await api.get(endpoints.bookings.my)
    bookings.value = response.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const cancelBooking = async (id: number) => {
  if (!confirm('确定要取消这个订单吗？')) return
  
  try {
    await api.post(endpoints.bookings.cancel(id))
    // Refresh list
    fetchBookings()
  } catch (e) {
    console.error(e)
    alert('取消失败')
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <NavBar />
    
    <main class="max-w-7xl mx-auto px-4 py-8 mt-16">
      <div class="bg-white rounded-lg shadow p-6 mb-8">
        <div class="flex items-center space-x-4">
          <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center text-2xl text-blue-600 font-bold">
            {{ user?.username?.charAt(0).toUpperCase() }}
          </div>
          <div>
            <h1 class="text-2xl font-bold text-gray-900">{{ user?.nickname || user?.username }}</h1>
            <p class="text-gray-500">普通会员</p>
          </div>
        </div>
      </div>

      <h2 class="text-2xl font-bold text-gray-900 mb-6">我的订单</h2>
      
      <div v-if="loading" class="text-center py-8">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
      </div>

      <div v-else-if="bookings.length === 0" class="text-center py-12 bg-white rounded-lg shadow">
        <p class="text-gray-500 mb-4">暂无预订记录</p>
        <router-link to="/spots" class="text-blue-600 hover:text-blue-800 font-medium">
          去浏览景点 →
        </router-link>
      </div>

      <div v-else class="space-y-4">
        <div v-for="booking in bookings" :key="booking.id" class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 flex flex-col md:flex-row justify-between items-center transition-all hover:shadow-md">
          <div class="flex items-center space-x-4 mb-4 md:mb-0 w-full md:w-auto">
            <div class="h-16 w-16 rounded-xl bg-gray-100 overflow-hidden flex-shrink-0">
               <img :src="booking.spot?.imageUrl" class="w-full h-full object-cover" alt="">
            </div>
            <div>
              <h3 class="text-lg font-bold text-gray-900 mb-1">{{ booking.spot?.name }}</h3>
              <div class="flex items-center text-gray-500 text-sm space-x-4">
                <span>📅 {{ booking.visitDate }}</span>
                <span>🎫 {{ booking.ticketCount }}张</span>
              </div>
            </div>
          </div>
          
          <div class="flex items-center space-x-6 w-full md:w-auto justify-between md:justify-end">
            <div class="text-right">
              <div class="text-xl font-bold text-apple-blue mb-1">¥{{ booking.totalPrice }}</div>
              <span :class="{
                'bg-green-100 text-green-800': booking.status === 'CONFIRMED',
                'bg-yellow-100 text-yellow-800': booking.status === 'PENDING',
                'bg-red-100 text-red-800': booking.status === 'CANCELLED'
              }" class="px-2.5 py-0.5 rounded-full text-xs font-medium">
                {{ booking.status === 'CONFIRMED' ? '预订成功' : (booking.status === 'PENDING' ? '待确认' : '已取消') }}
              </span>
            </div>
            
            <button v-if="booking.status === 'CONFIRMED' || booking.status === 'PENDING'"
                    @click="cancelBooking(booking.id)"
                    class="px-4 py-2 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded-xl transition-colors">
              取消
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
