import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

export function useAuthEvents() {
  const router = useRouter()
  const authExpired = ref(false)
  const handleAuthExpired = () => { authExpired.value = true }
  const handleAuthRestored = () => { authExpired.value = false }

  onMounted(() => {
    window.addEventListener('auth-expired', handleAuthExpired)
    window.addEventListener('auth-restored', handleAuthRestored)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('auth-expired', handleAuthExpired)
    window.removeEventListener('auth-restored', handleAuthRestored)
  })

  return {
    authExpired,
    goLogin: () => router.push('/login')
  }
}
