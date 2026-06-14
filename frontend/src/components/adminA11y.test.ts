import { describe, expect, it } from 'vitest'
import adminCommunityPanelSource from './AdminCommunityPanel.vue?raw'
import adminHeritagePanelSource from './AdminHeritagePanel.vue?raw'
import adminDashboardSource from '../views/AdminDashboard.vue?raw'

describe('admin component accessibility safeguards', () => {
  it('keeps collapsible admin headers keyboard-operable and stateful', () => {
    for (const source of [adminCommunityPanelSource, adminHeritagePanelSource]) {
      expect(source).toContain('role="button"')
      expect(source).toContain('tabindex="0"')
      expect(source).toContain(':aria-expanded=')
      expect(source).toContain('aria-controls=')
      expect(source).toContain('@keydown.enter=')
      expect(source).toContain('@keydown.space=')
      expect(source).toContain('event.target !== event.currentTarget')
    }

    expect(adminCommunityPanelSource).toContain('id="admin-community-panel-content"')
    expect(adminCommunityPanelSource).toContain('const togglePanelFromKeyboard = (event: KeyboardEvent) =>')
    expect(adminHeritagePanelSource).toContain('id="admin-heritage-panel-content"')
    expect(adminHeritagePanelSource).toContain('const toggleHeritagePanelFromKeyboard = (event: KeyboardEvent) =>')
  })

  it('keeps the touched admin components off broad any list helpers', () => {
    expect(adminCommunityPanelSource).toContain('type CommunityListResponse<T>')
    expect(adminCommunityPanelSource).toContain('const toArray = <T>(value: CommunityListResponse<T>): T[] =>')
    expect(adminCommunityPanelSource).toContain('const communityRoutes = ref<AdminCommunityRoute[]>([])')
    expect(adminCommunityPanelSource).toContain('const editingItem = ref<CommunityItem | null>(null)')
    expect(adminCommunityPanelSource).not.toMatch(/ref<any\[\]>/)
    expect(adminCommunityPanelSource).not.toContain('const toArray = (value: any)')

    expect(adminHeritagePanelSource).toContain('type AdminListResponse<T>')
    expect(adminHeritagePanelSource).toContain('const toList = <T>(value: AdminListResponse<T>): T[] =>')
    expect(adminHeritagePanelSource).not.toContain('const toList = (value: any)')
  })

  it('keeps admin community partial load failures visible per tab', () => {
    expect(adminCommunityPanelSource).toContain('const emptyCommunityTabErrors = (): Record<CommunityTab, string> =>')
    expect(adminCommunityPanelSource).toContain('const communityTabErrors = ref<Record<CommunityTab, string>>')
    expect(adminCommunityPanelSource).toContain('const activeCommunityTabError = computed')
    expect(adminCommunityPanelSource).toContain('v-if="activeCommunityTabError"')
    expect(adminCommunityPanelSource).toContain('role="alert"')
    expect(adminCommunityPanelSource).toContain('aria-live="assertive"')
    expect(adminCommunityPanelSource).toContain('communityTabErrors.value = failed.reduce<Record<CommunityTab, string>>')
    expect(adminCommunityPanelSource).toContain("errors[request.key] = safeClientErrorMessage(error, t('admin.loadCommunityFailed'))")
  })

  it('keeps admin community refreshes scoped to the newest request', () => {
    expect(adminCommunityPanelSource).toContain('let communityRequestId = 0')
    expect(adminCommunityPanelSource).toContain('const isCurrentCommunityRequest = (requestId: number) => requestId === communityRequestId')
    expect(adminCommunityPanelSource).toContain('const requestId = ++communityRequestId')
    expect(adminCommunityPanelSource).toContain('if (!isCurrentCommunityRequest(requestId)) return')
    expect(adminCommunityPanelSource).toContain('if (isCurrentCommunityRequest(requestId))')
    expect(adminCommunityPanelSource).toContain('loadingCommunity.value = false')
  })

  it('keeps admin community edits and deletes guarded against duplicate or stale actions', () => {
    expect(adminCommunityPanelSource).toContain('const deletingCommunityKeys = ref<ReadonlySet<string>>(new Set())')
    expect(adminCommunityPanelSource).toContain('const isEditingCommunityItem = (type: CommunityType, id: number) =>')
    expect(adminCommunityPanelSource).toContain('const isDeletingCommunityItem = (type: CommunityType, id: number) =>')
    expect(adminCommunityPanelSource).toContain('const setCommunityItemDeleting = (type: CommunityType, id: number, deleting: boolean) =>')
    expect(adminCommunityPanelSource).toContain('if (!editingItem.value || savingCommunity.value) return')
    expect(adminCommunityPanelSource).toContain('const type = editingType.value')
    expect(adminCommunityPanelSource).toContain('const itemId = editingItem.value.id')
    expect(adminCommunityPanelSource).toContain('const form = { ...communityForm.value }')
    expect(adminCommunityPanelSource).toContain('if (isEditingCommunityItem(type, itemId))')
    expect(adminCommunityPanelSource).toContain('const forceCloseCommunityModal = () =>')
    expect(adminCommunityPanelSource).toContain('forceCloseCommunityModal()')
    expect(adminCommunityPanelSource).toContain('if (savingCommunity.value || isDeletingCommunityItem(type, id)) return')
    expect(adminCommunityPanelSource).toContain('setCommunityItemDeleting(type, id, true)')
    expect(adminCommunityPanelSource).toContain('setCommunityItemDeleting(type, id, false)')
  })

  it('keeps admin community icon-only row actions named and stateful', () => {
    expect(adminCommunityPanelSource).toContain(':aria-label="`${t(\'common.edit\')} ${route.title || route.id}`.trim()"')
    expect(adminCommunityPanelSource).toContain(':aria-label="`${t(\'common.delete\')} ${route.title || route.id}`.trim()"')
    expect(adminCommunityPanelSource).toContain(':disabled="savingCommunity || isDeletingCommunityItem(\'route\', route.id)"')
    expect(adminCommunityPanelSource).toContain(':aria-busy="isDeletingCommunityItem(\'route\', route.id)"')
    expect(adminCommunityPanelSource).toContain(':aria-label="`${t(\'common.edit\')} ${question.title || question.id}`.trim()"')
    expect(adminCommunityPanelSource).toContain(':aria-label="`${t(\'common.delete\')} ${answer.questionTitle || answer.id}`.trim()"')
    expect(adminCommunityPanelSource).toContain('<form @submit.prevent="saveCommunityItem" class="space-y-5" :aria-busy="savingCommunity">')
  })

  it('keeps AdminDashboard collapsible and clickable cards keyboard-operable', () => {
    for (const id of [
      'admin-hotel-orders-content',
      'admin-recent-orders-content',
      'admin-popular-spots-content',
      'admin-spots-content',
      'admin-users-content',
      'admin-news-content',
      'admin-carousels-content',
      'admin-routes-content',
      'admin-hotels-content'
    ]) {
      expect(adminDashboardSource).toContain(`aria-controls="${id}"`)
      expect(adminDashboardSource).toContain(`id="${id}"`)
    }

    expect(adminDashboardSource).toContain('const activateKeyboardPanel = (event: KeyboardEvent, action: () => void | Promise<void>) =>')
    expect(adminDashboardSource).toContain('@keydown.enter="activateKeyboardPanel($event, toggleHotelsPanel)"')
    expect(adminDashboardSource).toContain('@keydown.space="activateKeyboardPanel($event, toggleHotelsPanel)"')
    expect(adminDashboardSource).toContain(':aria-label="`${t(\'common.edit\')} ${spot.name || \'\'}`.trim()"')
    expect(adminDashboardSource).toContain(':aria-controls="`admin-hotel-${h.id}-rooms`"')
    expect(adminDashboardSource).toContain('@keydown.enter="activateKeyboardPanel($event, () => toggleHotelExpand(h))"')
  })
})
