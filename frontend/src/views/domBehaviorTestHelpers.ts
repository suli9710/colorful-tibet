import { createApp, defineComponent, h, nextTick, type App, type Component, type Plugin, type PropType } from 'vue'
import { createPinia, type Pinia } from 'pinia'
import { createI18n } from 'vue-i18n'

type RouterLocationLike = string | {
  name?: string | symbol
  params?: Record<string, unknown>
  path?: string
  query?: Record<string, unknown>
}

export type TestI18nMessageValue = string | TestI18nMessageTree

export interface TestI18nMessageTree {
  [key: string]: TestI18nMessageValue
}

export type TestI18nMessages = Record<string, TestI18nMessageTree>

export type MountedView = {
  app: App
  cleanup: () => void
  flush: (turns?: number) => Promise<void>
  root: HTMLElement
}

export type DeferredPromise<T> = {
  promise: Promise<T>
  reject: (reason?: unknown) => void
  resolve: (value: T | PromiseLike<T>) => void
}

export type TextMatcher = string | RegExp

export type FindByTextOptions = {
  exact?: boolean
  normalizer?: (value: string) => string
  selector?: string
}

export type DispatchWindowEventOptions = EventInit & {
  turns?: number
}

export type MountWithPluginsOptions = {
  components?: Record<string, Component>
  i18n?: ReturnType<typeof createTestI18n> | false
  installRouterStubs?: boolean
  messages?: TestI18nMessages
  pinia?: Pinia | false
  plugins?: Plugin[]
  props?: Record<string, unknown>
  root?: HTMLElement
  rootTag?: keyof HTMLElementTagNameMap
  settle?: boolean | number
}

const mountedViews: MountedView[] = []

const normalizeText = (value: string) => value.replace(/\s+/g, ' ').trim()

const formatTextMatcher = (matcher: TextMatcher) =>
  typeof matcher === 'string' ? matcher : matcher.toString()

const matchesText = (
  value: string,
  matcher: TextMatcher,
  options: Pick<FindByTextOptions, 'exact' | 'normalizer'> = {}
) => {
  const normalizer = options.normalizer ?? normalizeText
  const normalizedValue = normalizer(value)

  if (typeof matcher === 'string') {
    const normalizedMatcher = normalizer(matcher)
    return options.exact ? normalizedValue === normalizedMatcher : normalizedValue.includes(normalizedMatcher)
  }

  matcher.lastIndex = 0
  return matcher.test(normalizedValue)
}

const hrefFromRoute = (to: RouterLocationLike = '#') => {
  if (typeof to === 'string') return to
  return to.path ?? (to.name ? `#${String(to.name)}` : '#')
}

export const RouterLinkStub = defineComponent({
  name: 'RouterLinkStub',
  inheritAttrs: false,
  props: {
    custom: {
      type: Boolean,
      default: false
    },
    to: {
      type: [String, Object] as PropType<RouterLocationLike>,
      default: '#'
    }
  },
  setup(props, { attrs, slots }) {
    const navigate = (event?: Event) => {
      event?.preventDefault()
    }

    return () => {
      const href = hrefFromRoute(props.to)

      if (props.custom && slots.default) {
        return slots.default({
          href,
          isActive: false,
          isExactActive: false,
          navigate,
          route: {
            fullPath: href,
            params: typeof props.to === 'string' ? {} : props.to.params ?? {},
            path: href,
            query: typeof props.to === 'string' ? {} : props.to.query ?? {}
          }
        })
      }

      return h('a', { ...attrs, href, onClick: navigate }, slots.default?.())
    }
  }
})

export const RouterViewStub = defineComponent({
  name: 'RouterViewStub',
  setup(_, { slots }) {
    return () => slots.default?.() ?? null
  }
})

export const createTestI18n = (
  messages: TestI18nMessages = { zh: {} },
  locale = 'zh'
) => createI18n({
  fallbackLocale: locale,
  fallbackWarn: false,
  legacy: false,
  locale,
  messages,
  missingWarn: false
})

export const flushPromises = async (turns = 8) => {
  for (let index = 0; index < turns; index += 1) {
    await Promise.resolve()
    await nextTick()
  }
}

export const createDeferred = <T = void>(): DeferredPromise<T> => {
  let resolve!: DeferredPromise<T>['resolve']
  let reject!: DeferredPromise<T>['reject']
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, reject, resolve }
}

export const mountWithPlugins = async (
  component: Component,
  options: MountWithPluginsOptions = {}
): Promise<MountedView> => {
  const root = options.root ?? document.createElement(options.rootTag ?? 'div')

  if (!root.isConnected) {
    document.body.appendChild(root)
  }

  const app = createApp(component, options.props ?? {})

  if (options.installRouterStubs ?? true) {
    app.component('router-link', RouterLinkStub)
    app.component('router-view', RouterViewStub)
  }

  if (options.pinia !== false) {
    app.use(options.pinia ?? createPinia())
  }

  if (options.i18n !== false) {
    app.use(options.i18n ?? createTestI18n(options.messages))
  }

  options.plugins?.forEach(plugin => app.use(plugin))
  Object.entries(options.components ?? {}).forEach(([name, testComponent]) => {
    app.component(name, testComponent)
  })

  let cleaned = false
  const mountedView: MountedView = {
    app,
    root,
    flush: flushPromises,
    cleanup: () => {
      if (cleaned) return
      cleaned = true
      app.unmount()
      root.remove()
    }
  }

  mountedViews.push(mountedView)

  try {
    app.mount(root)
    if (options.settle !== false) {
      await flushPromises(typeof options.settle === 'number' ? options.settle : undefined)
    }
  } catch (error) {
    mountedView.cleanup()
    throw error
  }

  return mountedView
}

export const cleanupMountedViews = (options: { clearBody?: boolean } = {}) => {
  while (mountedViews.length > 0) {
    mountedViews.pop()?.cleanup()
  }

  if (options.clearBody ?? true) {
    document.body.innerHTML = ''
  }
}

export const findByText = <T extends HTMLElement = HTMLElement>(
  root: ParentNode,
  matcher: TextMatcher,
  options: FindByTextOptions = {}
) => {
  const selector = options.selector ?? '*'
  const preferDeepestMatch = options.selector === undefined || selector === '*'

  return Array.from(root.querySelectorAll<T>(selector)).find(element => {
    if (!matchesText(element.textContent ?? '', matcher, options)) return false
    if (!preferDeepestMatch) return true

    return !Array.from(element.children).some(child =>
      matchesText(child.textContent ?? '', matcher, options)
    )
  }) ?? null
}

export const getByText = <T extends HTMLElement = HTMLElement>(
  root: ParentNode,
  matcher: TextMatcher,
  options: FindByTextOptions = {}
) => {
  const element = findByText<T>(root, matcher, options)

  if (!element) {
    throw new Error(`Unable to find element containing text: ${formatTextMatcher(matcher)}`)
  }

  return element
}

export const findButtonByText = (root: ParentNode, text: string) =>
  findByText<HTMLButtonElement>(root, text, { selector: 'button' })

export const clickButtonByText = async (root: ParentNode, text: string, turns?: number) => {
  const button = findButtonByText(root, text)

  if (!button) {
    throw new Error(`Unable to find button containing text: ${text}`)
  }

  button.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
  await flushPromises(turns)
}

export const setFieldValue = async (
  field: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement,
  value: string,
  turns?: number
) => {
  field.value = value
  field.dispatchEvent(new Event('input', { bubbles: true, cancelable: true }))
  field.dispatchEvent(new Event('change', { bubbles: true, cancelable: true }))
  await flushPromises(turns)
}

export const setFieldValueBySelector = async (
  root: ParentNode,
  selector: string,
  value: string,
  turns?: number
) => {
  const field = root.querySelector<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(selector)

  if (!field) {
    throw new Error(`Unable to find field matching selector: ${selector}`)
  }

  await setFieldValue(field, value, turns)
}

export const dispatchWindowEvent = async (
  event: Event | string,
  options: DispatchWindowEventOptions = {}
) => {
  const { turns, ...eventInit } = options
  const windowEvent = typeof event === 'string' ? new Event(event, eventInit) : event

  window.dispatchEvent(windowEvent)
  await flushPromises(turns)

  return windowEvent
}
