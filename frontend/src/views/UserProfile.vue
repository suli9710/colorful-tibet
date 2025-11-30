<script setup lang="ts">
import NavBar from '@/components/NavBar.vue'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import api, { endpoints } from '@/api'

const { t } = useI18n()

const router = useRouter()
const user = ref<any>(null)
const userInfo = ref<any>(null)
const stats = ref<any>(null)
const myRoutes = ref<any[]>([])
const bookings = ref<any[]>([])
const spotComments = ref<any[]>([])
const routeComments = ref<any[]>([])
const loading = ref(true)
const activeTab = ref<'routes' | 'bookings' | 'comments'>('routes')
const showPasswordModal = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const changingPassword = ref(false)
const showNicknameModal = ref(false)
const nicknameForm = ref({
  nickname: ''
})
const updatingNickname = ref(false)
const uploadingAvatar = ref(false)
const avatarFileInput = ref<HTMLInputElement | null>(null)

onMounted(async () => {
  const userStr = localStorage.getItem('user')
  if (!userStr) {
    router.push('/login')
    return
  }
  try {
    user.value = JSON.parse(userStr)
    // 检查是否有token
    if (!user.value?.token) {
      router.push('/login')
      return
    }
    await Promise.all([
      fetchUserInfo(),
      fetchStats(),
      fetchMyRoutes(),
      fetchBookings(),
      fetchMyComments()
    ])
  } catch (e: any) {
    // 如果API调用失败（特别是401），响应拦截器会处理跳转
    console.error('Failed to load user profile:', e)
    if (e.response?.status !== 401) {
      // 如果不是401错误，显示错误信息
      alert(t('profile.loadFailed'))
    }
  } finally {
    loading.value = false
  }
})

const fetchUserInfo = async () => {
  try {
    const response = await api.get('/auth/me')
    userInfo.value = response.data
  } catch (e: any) {
    console.error('Failed to fetch user info:', e)
    // 如果是401错误，说明token无效，让响应拦截器处理跳转
    if (e.response?.status === 401) {
      throw e // 重新抛出，让响应拦截器处理
    }
  }
}

const fetchStats = async () => {
  try {
    const response = await api.get('/auth/me/stats')
    stats.value = response.data
  } catch (e: any) {
    console.error('Failed to fetch stats:', e)
    // 如果是401错误，说明token无效，让响应拦截器处理跳转
    if (e.response?.status === 401) {
      throw e // 重新抛出，让响应拦截器处理
    }
  }
}

const fetchMyRoutes = async () => {
  try {
    const response = await api.get('/routes/my-routes')
    myRoutes.value = response.data || []
  } catch (e: any) {
    console.error('Failed to fetch my routes:', e)
    // 如果是401错误，说明token无效，让响应拦截器处理跳转
    if (e.response?.status === 401) {
      throw e // 重新抛出，让响应拦截器处理
    }
    myRoutes.value = []
  }
}

const fetchBookings = async () => {
  try {
    const response = await api.get(endpoints.bookings.my)
    bookings.value = response.data || []
  } catch (e) {
    console.error('Failed to fetch bookings:', e)
    bookings.value = []
  }
}

const fetchMyComments = async () => {
  try {
    const response = await api.get('/auth/me/comments')
    spotComments.value = response.data.spotComments || []
    routeComments.value = response.data.routeComments || []
  } catch (e: any) {
    console.error('Failed to fetch my comments:', e)
    if (e.response?.status === 401) {
      throw e
    }
    spotComments.value = []
    routeComments.value = []
  }
}

const cancelBooking = async (id: number) => {
  if (!confirm(t('profile.confirmCancelBooking'))) return
  
  try {
    await api.post(endpoints.bookings.cancel(id))
    fetchBookings()
  } catch (e) {
    console.error(e)
    alert(t('profile.cancelFailed'))
  }
}

const deleteRoute = async (id: number) => {
  if (!confirm(t('profile.confirmDeleteRoute'))) return
  
  try {
    await api.delete(`/routes/shared/${id}`)
    alert(t('profile.deleteSuccess'))
    fetchMyRoutes()
  } catch (e) {
    console.error(e)
    alert(t('profile.deleteFailed'))
  }
}

const changePassword = async () => {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    alert(t('profile.fillAllFields'))
    return
  }
  
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    alert(t('profile.passwordMismatch'))
    return
  }
  
  if (passwordForm.value.newPassword.length < 6) {
    alert(t('profile.passwordMinLength'))
    return
  }
  
  changingPassword.value = true
  try {
    await api.post('/auth/me/change-password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    alert(t('profile.passwordChangeSuccess'))
    showPasswordModal.value = false
    passwordForm.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (e: any) {
    console.error('Failed to change password:', e)
    const errorMsg = e.response?.data?.error || t('profile.passwordChangeFailed')
    alert(errorMsg)
  } finally {
    changingPassword.value = false
  }
}

const formatDate = (dateStr: string) => {
  const locale = localStorage.getItem('locale') || 'zh'
  return new Date(dateStr).toLocaleDateString(locale === 'bo' ? 'bo-CN' : 'zh-CN')
}

const openNicknameModal = () => {
  nicknameForm.value.nickname = userInfo.value?.nickname || ''
  showNicknameModal.value = true
}

const updateNickname = async () => {
  if (!nicknameForm.value.nickname.trim()) {
    alert(t('profile.nicknameRequired'))
    return
  }
  
  updatingNickname.value = true
  try {
    const response = await api.put('/auth/me/nickname', {
      nickname: nicknameForm.value.nickname.trim()
    })
    alert(t('profile.nicknameUpdateSuccess'))
    showNicknameModal.value = false
    await fetchUserInfo()
    // 更新localStorage中的用户信息
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const userData = JSON.parse(userStr)
      userData.nickname = nicknameForm.value.nickname.trim()
      localStorage.setItem('user', JSON.stringify(userData))
    }
  } catch (e: any) {
    console.error('Failed to update nickname:', e)
    const errorMsg = e.response?.data?.error || t('profile.nicknameUpdateFailed')
    alert(errorMsg)
  } finally {
    updatingNickname.value = false
  }
}

const handleAvatarClick = () => {
  avatarFileInput.value?.click()
}

const handleAvatarUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    alert(t('profile.selectImageFile'))
    return
  }
  
  // 验证文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    alert(t('profile.imageTooLarge'))
    return
  }
  
  uploadingAvatar.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await api.post('/auth/me/upload-avatar', formData)
    
    alert(t('profile.avatarUploadSuccess'))
    await fetchUserInfo()
    // 更新localStorage中的用户信息
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const userData = JSON.parse(userStr)
      userData.avatar = response.data.avatarUrl
      localStorage.setItem('user', JSON.stringify(userData))
    }
  } catch (e: any) {
    console.error('Failed to upload avatar:', e)
    const errorMsg = e.response?.data?.error || t('profile.avatarUploadFailed')
    alert(errorMsg)
  } finally {
    uploadingAvatar.value = false
    // 清空input
    if (target) {
      target.value = ''
    }
  }
}

const getAvatarUrl = () => {
  if (userInfo.value?.avatar) {
    return userInfo.value.avatar
  }
  return null
}
</script>

<template>
  <div class="min-h-screen bg-apple-gray-50">
    <NavBar />
    
    <main class="max-w-7xl mx-auto px-4 py-8 mt-24">
      <!-- User Info Card -->
      <div class="glass rounded-3xl p-8 mb-8 animate-slide-up shadow-xl border border-white/50">
        <div class="flex flex-col md:flex-row items-center md:items-start gap-6">
          <div class="relative group">
            <div 
              @click="handleAvatarClick"
              class="w-24 h-24 rounded-full flex items-center justify-center text-3xl text-white font-bold shadow-lg cursor-pointer overflow-hidden transition-all hover:ring-4 hover:ring-apple-blue/50"
              :class="getAvatarUrl() ? '' : 'bg-gradient-to-br from-blue-500 to-purple-600'"
            >
              <img 
                v-if="getAvatarUrl()" 
                :src="getAvatarUrl()" 
                :alt="t('profile.avatar')" 
                class="w-full h-full object-cover"
              >
              <span v-else>
                {{ (userInfo?.nickname || userInfo?.username || user?.username || 'U').charAt(0).toUpperCase() }}
              </span>
            </div>
            <div class="absolute inset-0 bg-black/0 group-hover:bg-black/20 rounded-full flex items-center justify-center transition-all cursor-pointer" @click="handleAvatarClick">
              <svg v-if="!uploadingAvatar" xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-white opacity-0 group-hover:opacity-100 transition-opacity" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <div v-else class="animate-spin rounded-full h-6 w-6 border-b-2 border-white"></div>
            </div>
            <input 
              ref="avatarFileInput"
              type="file" 
              accept="image/*" 
              @change="handleAvatarUpload"
              class="hidden"
            >
          </div>
          <div class="flex-1 text-center md:text-left">
            <div class="flex items-center justify-center md:justify-start gap-2 mb-2">
              <h1 class="text-3xl font-bold text-apple-gray-900">
                {{ userInfo?.nickname || userInfo?.username || user?.username }}
              </h1>
              <button 
                @click="openNicknameModal"
                class="text-apple-gray-500 hover:text-apple-blue transition-colors"
                :title="t('profile.editNickname')"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
            </div>
            <p class="text-apple-gray-500 mb-4">
              {{ userInfo?.role === 'ADMIN' ? t('profile.admin') : t('profile.member') }} · 
              {{ t('profile.registeredAt') }} {{ userInfo?.createdAt ? formatDate(userInfo.createdAt) : '' }}
            </p>
            <div class="flex flex-wrap gap-4 justify-center md:justify-start mb-4">
              <div class="flex items-center gap-2 px-4 py-2 bg-white/50 rounded-xl">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-apple-blue" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                </svg>
                <span class="text-sm font-medium text-apple-gray-700">
                  <span class="text-apple-blue font-bold">{{ stats?.routeCount || 0 }}</span> {{ t('profile.routesCount') }}
                </span>
              </div>
              <div class="flex items-center gap-2 px-4 py-2 bg-white/50 rounded-xl">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
                <span class="text-sm font-medium text-apple-gray-700">
                  <span class="text-green-500 font-bold">{{ stats?.commentCount || 0 }}</span> {{ t('profile.commentsCount') }}
                </span>
              </div>
              <div class="flex items-center gap-2 px-4 py-2 bg-white/50 rounded-xl">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-orange-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
                <span class="text-sm font-medium text-apple-gray-700">
                  <span class="text-orange-500 font-bold">{{ stats?.bookingCount || 0 }}</span> {{ t('profile.bookingsCount') }}
                </span>
              </div>
            </div>
            <button 
              @click="showPasswordModal = true"
              class="px-4 py-2 bg-apple-blue hover:bg-apple-blue-hover text-white rounded-xl transition-colors text-sm font-medium"
            >
              {{ t('profile.changePassword') }}
            </button>
          </div>
        </div>
      </div>

      <!-- Nickname Edit Modal -->
      <div v-if="showNicknameModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showNicknameModal = false">
        <div class="glass rounded-3xl p-8 max-w-md w-full mx-4 shadow-2xl border border-white/50">
          <h2 class="text-2xl font-bold text-apple-gray-900 mb-6">{{ t('profile.editNickname') }}</h2>
          <form @submit.prevent="updateNickname" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">{{ t('profile.nickname') }}</label>
              <input 
                v-model="nicknameForm.nickname" 
                type="text" 
                required
                maxlength="20"
                class="w-full px-4 py-2 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none"
                :placeholder="t('profile.nicknamePlaceholder')"
              />
              <p class="mt-1 text-xs text-gray-500">{{ nicknameForm.nickname.length }}/20</p>
            </div>
            <div class="flex gap-4 pt-4">
              <button 
                type="button"
                @click="showNicknameModal = false"
                class="flex-1 px-4 py-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-gray-700 font-medium transition-colors"
              >
                {{ t('profile.cancel') }}
              </button>
              <button 
                type="submit"
                :disabled="updatingNickname"
                class="flex-1 px-4 py-2 rounded-xl bg-apple-blue hover:bg-apple-blue-hover text-white font-medium transition-colors disabled:opacity-50"
              >
                {{ updatingNickname ? t('profile.updating') : t('profile.confirmUpdate') }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Password Change Modal -->
      <div v-if="showPasswordModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showPasswordModal = false">
        <div class="glass rounded-3xl p-8 max-w-md w-full mx-4 shadow-2xl border border-white/50">
          <h2 class="text-2xl font-bold text-apple-gray-900 mb-6">{{ t('profile.changePassword') }}</h2>
          <form @submit.prevent="changePassword" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">{{ t('profile.currentPassword') }}</label>
              <input 
                v-model="passwordForm.oldPassword" 
                type="password" 
                required
                class="w-full px-4 py-2 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none"
                :placeholder="t('profile.currentPasswordPlaceholder')"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">{{ t('profile.newPassword') }}</label>
              <input 
                v-model="passwordForm.newPassword" 
                type="password" 
                required
                class="w-full px-4 py-2 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none"
                :placeholder="t('profile.newPasswordPlaceholder')"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">{{ t('profile.confirmNewPassword') }}</label>
              <input 
                v-model="passwordForm.confirmPassword" 
                type="password" 
                required
                class="w-full px-4 py-2 rounded-xl bg-white/50 border border-gray-200 focus:border-apple-blue focus:ring-2 focus:ring-blue-100 outline-none"
                :placeholder="t('profile.confirmNewPasswordPlaceholder')"
              />
            </div>
            <div class="flex gap-4 pt-4">
              <button 
                type="button"
                @click="showPasswordModal = false"
                class="flex-1 px-4 py-2 rounded-xl border border-gray-200 hover:bg-gray-50 text-gray-700 font-medium transition-colors"
              >
                {{ t('profile.cancel') }}
              </button>
              <button 
                type="submit"
                :disabled="changingPassword"
                class="flex-1 px-4 py-2 rounded-xl bg-apple-blue hover:bg-apple-blue-hover text-white font-medium transition-colors disabled:opacity-50"
              >
                {{ changingPassword ? t('profile.changing') : t('profile.confirmChange') }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Tabs -->
      <div class="glass rounded-2xl p-6 mb-8 animate-slide-up shadow-xl border border-white/50">
        <div class="flex gap-4 border-b border-gray-200 mb-6 overflow-x-auto">
          <button 
            @click="activeTab = 'routes'"
            :class="[
              'px-6 py-3 font-medium transition-all duration-300 border-b-2 whitespace-nowrap',
              activeTab === 'routes' 
                ? 'text-apple-blue border-apple-blue' 
                : 'text-apple-gray-500 border-transparent hover:text-apple-gray-700'
            ]"
          >
            {{ t('profile.myRoutesTab') }} ({{ myRoutes.length }})
          </button>
          <button 
            @click="activeTab = 'bookings'"
            :class="[
              'px-6 py-3 font-medium transition-all duration-300 border-b-2 whitespace-nowrap',
              activeTab === 'bookings' 
                ? 'text-apple-blue border-apple-blue' 
                : 'text-apple-gray-500 border-transparent hover:text-apple-gray-700'
            ]"
          >
            {{ t('profile.myBookingsTab') }} ({{ bookings.length }})
          </button>
          <button 
            @click="activeTab = 'comments'"
            :class="[
              'px-6 py-3 font-medium transition-all duration-300 border-b-2 whitespace-nowrap',
              activeTab === 'comments' 
                ? 'text-apple-blue border-apple-blue' 
                : 'text-apple-gray-500 border-transparent hover:text-apple-gray-700'
            ]"
          >
            {{ t('profile.myCommentsTab') }} ({{ spotComments.length + routeComments.length }})
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-apple-blue mx-auto"></div>
        </div>

        <!-- My Routes -->
        <div v-else-if="activeTab === 'routes'">
          <div v-if="myRoutes.length === 0" class="text-center py-12">
            <p class="text-gray-500 mb-4">{{ t('profile.noRoutes') }}</p>
            <router-link to="/create-route" class="text-apple-blue hover:text-apple-blue-hover font-medium">
              {{ t('profile.createRouteLink') }}
            </router-link>
          </div>

          <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div 
              v-for="route in myRoutes" 
              :key="route.id"
              class="glass rounded-2xl p-6 hover:shadow-2xl transition-all duration-500 ease-out border border-white/20 group"
            >
              <div class="flex justify-between items-start mb-4">
                <h3 
                  @click="router.push(`/community/${route.id}`)"
                  class="text-xl font-bold text-gray-900 group-hover:text-apple-blue transition-colors duration-300 line-clamp-2 flex-1 cursor-pointer"
                >
                  {{ route.title }}
                </h3>
                <button 
                  @click="deleteRoute(route.id)"
                  class="ml-2 text-red-500 hover:text-red-700 transition-colors"
                  :title="t('profile.delete')"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
              
              <div class="flex gap-2 mb-4 text-sm text-gray-600">
                <span class="px-3 py-1.5 bg-gray-100 rounded-lg">{{ route.days }}{{ t('profile.days') }}</span>
                <span class="px-3 py-1.5 bg-gray-100 rounded-lg">{{ route.budget }}</span>
                <span class="px-3 py-1.5 bg-gray-100 rounded-lg">{{ route.preference }}</span>
              </div>
              
              <div class="flex justify-between items-center text-sm text-gray-500 pt-4 border-t border-gray-100">
                <div class="flex items-center gap-4">
                  <span class="flex items-center gap-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    {{ route.viewCount }}
                  </span>
                  <span class="flex items-center gap-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                    {{ route.likeCount }}
                  </span>
                  <span class="flex items-center gap-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                    {{ route.commentCount }}
                  </span>
                </div>
                <span class="text-xs">{{ formatDate(route.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- My Bookings -->
        <div v-else-if="activeTab === 'bookings'">
          <div v-if="bookings.length === 0" class="text-center py-12">
            <p class="text-gray-500 mb-4">{{ t('profile.noBookings') }}</p>
            <router-link to="/spots" class="text-apple-blue hover:text-apple-blue-hover font-medium">
              {{ t('profile.browseSpotsLink') }}
            </router-link>
          </div>

          <div v-else class="space-y-4">
            <div 
              v-for="booking in bookings" 
              :key="booking.id" 
              class="glass rounded-2xl p-6 flex flex-col md:flex-row justify-between items-center transition-all hover:shadow-lg border border-white/20"
            >
              <div class="flex items-center space-x-4 mb-4 md:mb-0 w-full md:w-auto">
                <div class="h-16 w-16 rounded-xl bg-gray-100 overflow-hidden flex-shrink-0">
                  <img :src="booking.spot?.imageUrl" class="w-full h-full object-cover" alt="">
                </div>
                <div>
                  <h3 class="text-lg font-bold text-gray-900 mb-1">{{ booking.spot?.name }}</h3>
                  <div class="flex items-center text-gray-500 text-sm space-x-4">
                    <span>{{ t('profile.visitDate') }} {{ booking.visitDate }}</span>
                    <span>🎫 {{ booking.ticketCount }}{{ t('profile.tickets') }}</span>
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
                    {{ booking.status === 'CONFIRMED' ? t('profile.bookingSuccess') : (booking.status === 'PENDING' ? t('profile.pending') : t('profile.cancelled')) }}
                  </span>
                </div>
                
                <button 
                  v-if="booking.status === 'CONFIRMED' || booking.status === 'PENDING'"
                  @click="cancelBooking(booking.id)"
                  class="px-4 py-2 text-sm font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded-xl transition-colors"
                >
                  {{ t('profile.cancelBooking') }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- My Comments -->
        <div v-else-if="activeTab === 'comments'">
          <div v-if="spotComments.length === 0 && routeComments.length === 0" class="text-center py-12">
            <p class="text-gray-500 mb-4">{{ t('profile.noComments') }}</p>
            <router-link to="/spots" class="text-apple-blue hover:text-apple-blue-hover font-medium">
              {{ t('profile.browseSpotsLink') }}
            </router-link>
          </div>

          <div v-else class="space-y-6">
            <!-- Spot Comments -->
            <div v-if="spotComments.length > 0">
              <h3 class="text-lg font-bold text-apple-gray-900 mb-4">{{ t('profile.spotComments') }} ({{ spotComments.length }})</h3>
              <div class="space-y-4">
                <div 
                  v-for="comment in spotComments" 
                  :key="comment.id"
                  class="glass rounded-2xl p-6 border border-white/20 hover:shadow-lg transition-all"
                >
                  <div class="flex items-start gap-4">
                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-2">
                        <router-link 
                          :to="`/spots/${comment.spot?.id}`"
                          class="font-bold text-apple-blue hover:text-apple-blue-hover"
                        >
                          {{ comment.spot?.name }}
                        </router-link>
                        <div class="flex items-center gap-1 text-yellow-500">
                          <span v-for="i in 5" :key="i" class="text-sm">
                            {{ i <= (comment.rating || 0) ? '★' : '☆' }}
                          </span>
                        </div>
                      </div>
                      <p class="text-gray-700 mb-2">{{ comment.content }}</p>
                      <div v-if="comment.imageUrl" class="mb-2">
                        <img :src="comment.imageUrl" alt="评论图片" class="max-w-xs rounded-lg">
                      </div>
                      <div class="flex items-center gap-4 text-sm text-gray-500">
                        <span>👍 {{ comment.likeCount || 0 }}</span>
                        <span>{{ formatDate(comment.createdAt) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Route Comments -->
            <div v-if="routeComments.length > 0">
              <h3 class="text-lg font-bold text-apple-gray-900 mb-4 mt-6">{{ t('profile.routeComments') }} ({{ routeComments.length }})</h3>
              <div class="space-y-4">
                <div 
                  v-for="comment in routeComments" 
                  :key="comment.id"
                  class="glass rounded-2xl p-6 border border-white/20 hover:shadow-lg transition-all"
                >
                  <div class="flex items-start gap-4">
                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-2">
                        <router-link 
                          :to="`/community/${comment.route?.id}`"
                          class="font-bold text-apple-blue hover:text-apple-blue-hover"
                        >
                          {{ comment.route?.title }}
                        </router-link>
                      </div>
                      <p class="text-gray-700 mb-2">{{ comment.content }}</p>
                      <div class="flex items-center gap-4 text-sm text-gray-500">
                        <span>{{ formatDate(comment.createdAt) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
