import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<any>(null)
  const token = ref<string | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  function restoreFromStorage() {
    const storedUser = localStorage.getItem('user')
    const storedToken = localStorage.getItem('token')
    if (storedUser && storedToken) {
      try {
        user.value = JSON.parse(storedUser)
        token.value = storedToken
      } catch {
        user.value = null
        token.value = null
      }
    }
  }

  function login(userData: any, authToken: string) {
    user.value = userData
    token.value = authToken
    localStorage.setItem('user', JSON.stringify(userData))
    localStorage.setItem('token', authToken)
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  // Restore on store creation
  restoreFromStorage()

  return { user, token, isLoggedIn, isAdmin, login, logout, restoreFromStorage }
})
