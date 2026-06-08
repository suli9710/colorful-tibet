import { describe, expect, it } from 'vitest'
import confirmHostSource from './ConfirmHost.vue?raw'
import footerSource from './Footer.vue?raw'
import mobileBottomNavSource from './MobileBottomNav.vue?raw'
import mobileStickyActionBarSource from './MobileStickyActionBar.vue?raw'
import navBarSource from './NavBar.vue?raw'
import toastHostSource from './ToastHost.vue?raw'

describe('mobile navigation accessibility safeguards', () => {
  it('keeps the top mobile menu keyboard-operable and stateful', () => {
    expect(navBarSource).toContain('handleGlobalKeydown')
    expect(navBarSource).toContain("event.key !== 'Escape'")
    expect(navBarSource).toContain('aria-controls="mobile-primary-menu"')
    expect(navBarSource).toContain(':aria-expanded="isOpen"')
    expect(navBarSource).toContain('const isNavItemActive')
    expect(navBarSource).toContain(':aria-current="isNavItemActive(item) ? \'page\' : undefined"')
    expect(navBarSource).toContain('handleMobileMenuTab')
    expect(navBarSource).toContain('mobileMenuPanel')
    expect(navBarSource).toContain("document.body.classList.toggle('nav-menu-open', locked)")
    expect(navBarSource).toContain('env(safe-area-inset-top)')
    expect(navBarSource).toContain('focus-visible:ring-2')
  })

  it('keeps the bottom navigation as safe-area-aware links with current-page semantics', () => {
    expect(mobileBottomNavSource).toContain('<router-link')
    expect(mobileBottomNavSource).toContain('v-slot="{ href, navigate }"')
    expect(mobileBottomNavSource).toContain(':aria-current="tab.active ? \'page\' : undefined"')
    expect(mobileBottomNavSource).toContain('mobile-bottom-nav__tab')
    expect(mobileBottomNavSource).toContain("'/hotel-booking'")
    expect(mobileBottomNavSource).toContain('mobile-bottom-nav__label')
    expect(mobileBottomNavSource).toContain('env(safe-area-inset-bottom)')
    expect(mobileBottomNavSource).toContain('env(safe-area-inset-left)')
    expect(mobileBottomNavSource).toContain('@media (max-height: 420px)')
  })

  it('keeps the sticky action bar reachable without crowding small or notched screens', () => {
    expect(mobileStickyActionBarSource).toContain('role="region"')
    expect(mobileStickyActionBarSource).toContain(':aria-label="actionBarLabel"')
    expect(mobileStickyActionBarSource).toContain('mobile-sticky-action__content--summary')
    expect(mobileStickyActionBarSource).toContain('grid-template-columns')
    expect(mobileStickyActionBarSource).toContain('touch-action: manipulation')
    expect(mobileStickyActionBarSource).toContain('env(safe-area-inset-bottom)')
    expect(mobileStickyActionBarSource).toContain('focus-visible:ring-2')
  })

  it('keeps global overlays keyboard and touch accessible', () => {
    expect(confirmHostSource).toContain('role="dialog"')
    expect(confirmHostSource).toContain('aria-modal="true"')
    expect(confirmHostSource).toContain('@keydown.tab="handleTabKey"')
    expect(confirmHostSource).toContain('previouslyFocusedElement')
    expect(confirmHostSource).toContain('cancelButton.value?.focus()')

    expect(toastHostSource).toContain(':aria-live="liveClass[toast.type]"')
    expect(toastHostSource).toContain('aria-atomic="true"')
    expect(toastHostSource).toContain(':aria-label="t(\'toast.close\')"')
    expect(toastHostSource).toContain('min-h-10 min-w-10')
  })

  it('keeps footer icon share controls named and keyboard visible', () => {
    expect(footerSource).not.toContain('href="javascript:void(0)"')
    expect(footerSource).not.toContain('href="#"')
    expect(footerSource).toContain(':aria-label="t(\'footer.shareToWechat\')"')
    expect(footerSource).toContain(':aria-label="t(\'footer.shareToQQ\')"')
    expect(footerSource).toContain(':aria-label="t(\'footer.shareToWeibo\')"')
    expect(footerSource).toContain('focus-visible:ring-2')
  })
})
