import { describe, expect, it } from 'vitest'
import mobileBottomNavSource from '../components/MobileBottomNav.vue?raw'
import navBarSource from '../components/NavBar.vue?raw'
import loginSource from './Login.vue?raw'
import registerSource from './Register.vue?raw'
import userProfileSource from './UserProfile.vue?raw'

describe('auth and account accessibility safeguards', () => {
  it('keeps login fields connected to browser autofill and inline errors', () => {
    expect(loginSource).toContain('autocomplete="username"')
    expect(loginSource).toContain('autocomplete="current-password"')
    expect(loginSource).toContain('autocomplete="one-time-code"')
    expect(loginSource).toContain('autocapitalize="none"')
    expect(loginSource).toContain('spellcheck="false"')
    expect(loginSource).toContain('const loginErrorId = \'login-form-error\'')
    expect(loginSource).toContain(':aria-describedby="usernameDescription"')
    expect(loginSource).toContain(':aria-describedby="passwordDescription"')
    expect(loginSource).toContain(':aria-describedby="secondaryPasswordDescription"')
    expect(loginSource).toContain(':aria-invalid="loginFieldInvalid"')
    expect(loginSource).toContain(':aria-busy="loading"')
    expect(loginSource).toContain('role="alert"')
    expect(loginSource).toContain('aria-live="assertive"')
  })

  it('keeps registration fields described and error-associated', () => {
    expect(registerSource).toContain('autocomplete="username"')
    expect(registerSource).toContain('autocomplete="nickname"')
    expect(registerSource.match(/autocomplete="new-password"/g)?.length).toBeGreaterThanOrEqual(2)
    expect(registerSource.match(/minlength="6"/g)?.length).toBeGreaterThanOrEqual(2)
    expect(registerSource).toContain('const registerErrorId = \'register-form-error\'')
    expect(registerSource).toContain('registerErrorMessage')
    expect(registerSource).toContain(':aria-describedby="passwordDescription"')
    expect(registerSource).toContain(':aria-describedby="confirmPasswordDescription"')
    expect(registerSource).toContain(':aria-invalid="registerFieldInvalid"')
    expect(registerSource).toContain(':aria-busy="loading"')
    expect(registerSource).toContain('role="alert"')
  })

  it('keeps account entry controls and password fields accessible', () => {
    expect(userProfileSource).toContain(':aria-label="`${t(\'common.edit\')} ${t(\'profile.avatar\')}`"')
    expect(userProfileSource).toContain('id="avatar-upload"')
    expect(userProfileSource).toContain(':disabled="uploadingAvatar"')
    expect(userProfileSource).toContain(':aria-busy="uploadingAvatar"')
    expect(userProfileSource).toContain(':aria-label="t(\'profile.editNickname\')"')
    expect(userProfileSource).toContain('const profileNicknameErrorId = \'profile-nickname-error\'')
    expect(userProfileSource).toContain(':aria-describedby="nicknameDescription"')
    expect(userProfileSource).toContain(':aria-invalid="nicknameFieldInvalid"')
    expect(userProfileSource).toContain('labelled-by="password-modal-title"')
    expect(userProfileSource).toContain('const profilePasswordErrorId = \'profile-password-error\'')
    expect(userProfileSource).toContain('autocomplete="current-password"')
    expect(userProfileSource.match(/autocomplete="new-password"/g)?.length).toBeGreaterThanOrEqual(2)
    expect(userProfileSource).toContain(':aria-describedby="currentPasswordDescription"')
    expect(userProfileSource).toContain(':aria-invalid="passwordFieldInvalid"')
    expect(userProfileSource).toContain('role="alert"')
  })

  it('keeps auth and profile forms guarded against duplicate submits', () => {
    expect(loginSource).toContain('const isLoginSubmitDisabled = computed(() => loading.value || lockCountdown.value > 0)')
    expect(loginSource).toContain('if (isLoginSubmitDisabled.value) return')
    expect(registerSource).toContain('if (loading.value) return')
    expect(userProfileSource).toContain('if (changingPassword.value) return')
    expect(userProfileSource).toContain('if (updatingNickname.value) return')
    expect(userProfileSource).toContain('if (uploadingAvatar.value) return')
  })

  it('keeps ordinary login available when the third-party reCAPTCHA script is unavailable', () => {
    expect(loginSource).toContain('const getOptionalLoginRecaptchaToken = async () => {')
    expect(loginSource).toContain("return await getRecaptchaToken('login')")
    expect(loginSource).toContain("console.warn('Login reCAPTCHA unavailable; continuing with server-side risk controls:'")
    expect(loginSource).toContain('const recaptchaToken = await getOptionalLoginRecaptchaToken()')
  })

  it('keeps auth/profile action button text from overflowing on small screens', () => {
    for (const source of [loginSource, registerSource, userProfileSource]) {
      expect(source).toContain('whitespace-normal break-words')
      expect(source).toContain('min-w-0')
    }
  })

  it('keeps icon-only and mobile navigation controls labelled', () => {
    expect(navBarSource).toContain(':title="t(\'common.logout\')"')
    expect(navBarSource).toContain(':title="isOpen ? t(\'common.closeMenu\') : t(\'common.openMenu\')"')
    expect(navBarSource).toContain('aria-hidden="true"')

    expect(mobileBottomNavSource).toContain('const getTabAriaLabel')
    expect(mobileBottomNavSource).toContain('mobileNav.currentPage')
    expect(mobileBottomNavSource).toContain(':aria-current="tab.active ? \'page\' : undefined"')
    expect(mobileBottomNavSource).toContain(':aria-label="getTabAriaLabel(tab.label, tab.active)"')
    expect(mobileBottomNavSource).toContain(':title="getTabTitle(tab.label, tab.active)"')
    expect(mobileBottomNavSource).toContain('touch-action: manipulation')
  })
})
