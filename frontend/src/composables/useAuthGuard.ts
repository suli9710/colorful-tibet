import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/auth'
import { showToast } from './useToast'

export function useAuthGuard() {
  const auth = useAuthStore()
  const router = useRouter()
  const { t } = useI18n()

  async function requireAuth(): Promise<boolean> {
    if (await auth.ensureSession()) return true
    showToast(t('common.loginRequired'), 'warning')
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    return false
  }

  return { requireAuth }
}
