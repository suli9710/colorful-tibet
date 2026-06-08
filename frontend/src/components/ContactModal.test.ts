// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, nextTick, ref, type App } from 'vue'
import { createI18n } from 'vue-i18n'
import ContactModal from './ContactModal.vue'

vi.mock('motion-v', async () => {
  const vue = await vi.importActual<typeof import('vue')>('vue')

  const motionPassthrough = (tag: 'button' | 'div') => vue.defineComponent({
    inheritAttrs: false,
    setup(_, { attrs, slots }) {
      return () => {
        const {
          animate,
          exit,
          initial,
          transition,
          whileHover,
          whileTap,
          ...domAttrs
        } = attrs

        return vue.h(tag, domAttrs, slots.default?.())
      }
    }
  })

  return {
    AnimatePresence: vue.defineComponent({
      setup(_, { slots }) {
        return () => vue.h(vue.Fragment, slots.default?.())
      }
    }),
    motion: {
      button: motionPassthrough('button'),
      div: motionPassthrough('div')
    },
    useReducedMotion: () => false
  }
})

const mountedApps: App[] = []

const createTestI18n = () => createI18n({
  legacy: false,
  locale: 'zh',
  messages: {
    zh: {
      common: {
        closeMenu: 'Close'
      },
      contact: {
        title: 'Contact us',
        subtitle: 'We are listening.',
        phoneConsult: 'Phone support',
        phoneHours: 'Weekdays',
        emailContact: 'Email support',
        emailResponse: 'Within 24 hours',
        companyAddress: 'Company address',
        closingMessage: 'Talk soon',
        copiedToClipboard: 'Copied'
      },
      footer: {
        phone: '123-456',
        email: 'hello@example.test',
        addressLine1: 'Lhasa',
        addressLine2: 'Tibet'
      },
      routePlanner: {
        copyText: 'Copy'
      }
    }
  }
})

const settleVue = async () => {
  for (let index = 0; index < 4; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

const mountContactModal = async () => {
  const root = document.createElement('div')
  document.body.appendChild(root)

  const Host = defineComponent({
    setup() {
      const isOpen = ref(false)

      return () => h('div', [
        h('button', {
          id: 'open-contact',
          type: 'button',
          onClick: () => {
            isOpen.value = true
          }
        }, 'Open contact'),
        h(ContactModal, {
          modelValue: isOpen.value,
          'onUpdate:modelValue': (value: boolean) => {
            isOpen.value = value
          }
        })
      ])
    }
  })

  const app = createApp(Host)
  app.use(createTestI18n())
  app.mount(root)
  mountedApps.push(app)
  await settleVue()

  return root
}

const getButtonByLabel = (label: string) => {
  return Array.from(document.querySelectorAll<HTMLButtonElement>('button'))
    .find(button => button.getAttribute('aria-label') === label) ?? null
}

const openModal = async () => {
  const trigger = document.getElementById('open-contact') as HTMLButtonElement
  trigger.focus()
  trigger.click()
  await settleVue()

  return trigger
}

afterEach(() => {
  mountedApps.splice(0).forEach(app => app.unmount())
  document.body.innerHTML = ''
  vi.clearAllMocks()
})

describe('ContactModal accessibility behavior', () => {
  it('exposes dialog semantics and names icon buttons', async () => {
    await mountContactModal()
    await openModal()

    const dialog = document.querySelector<HTMLElement>('[role="dialog"]')
    const title = document.getElementById('contact-modal-title')

    expect(dialog).not.toBeNull()
    expect(dialog?.getAttribute('aria-modal')).toBe('true')
    expect(dialog?.getAttribute('aria-labelledby')).toBe('contact-modal-title')
    expect(title?.textContent).toContain('Contact us')
    expect(getButtonByLabel('Close Contact us')).not.toBeNull()
    expect(getButtonByLabel('Copy Phone support')).not.toBeNull()
    expect(getButtonByLabel('Copy Email support')).not.toBeNull()
  })

  it('moves focus into the dialog, closes on Escape, and restores focus', async () => {
    await mountContactModal()
    const trigger = await openModal()
    const closeButton = getButtonByLabel('Close Contact us')

    expect(document.activeElement).toBe(closeButton)

    closeButton?.dispatchEvent(new KeyboardEvent('keydown', {
      bubbles: true,
      cancelable: true,
      key: 'Escape'
    }))
    await settleVue()

    expect(document.querySelector('[role="dialog"]')).toBeNull()
    expect(document.activeElement).toBe(trigger)
  })

  it('wraps Tab focus inside the dialog controls', async () => {
    await mountContactModal()
    await openModal()

    const closeButton = getButtonByLabel('Close Contact us')
    const emailCopyButton = getButtonByLabel('Copy Email support')

    emailCopyButton?.focus()
    emailCopyButton?.dispatchEvent(new KeyboardEvent('keydown', {
      bubbles: true,
      cancelable: true,
      key: 'Tab'
    }))

    expect(document.activeElement).toBe(closeButton)

    closeButton?.dispatchEvent(new KeyboardEvent('keydown', {
      bubbles: true,
      cancelable: true,
      key: 'Tab',
      shiftKey: true
    }))

    expect(document.activeElement).toBe(emailCopyButton)
  })
})
