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
