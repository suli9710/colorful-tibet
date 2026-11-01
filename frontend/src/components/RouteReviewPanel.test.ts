// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, defineComponent, h, KeepAlive, nextTick, reactive, type App } from 'vue'
import { createI18n } from 'vue-i18n'
import RouteReviewPanel from './RouteReviewPanel.vue'
import messages from '../i18n/locales/planner-review.zh.json'

const { showConfirm } = vi.hoisted(() => ({ showConfirm: vi.fn() }))
vi.mock('../composables/useConfirm', () => ({ showConfirm }))

let app: App | undefined
const settle = async () => { await Promise.resolve(); await nextTick(); await Promise.resolve(); await nextTick() }

const mountPanel = async () => {
  const state = reactive({ routeKey: 'route-1', visible: true })
  const onNotes = vi.fn()
  const onJump = vi.fn()
  const root = document.createElement('div')
  document.body.append(root)
  app = createApp(defineComponent({
    setup: () => () => h(KeepAlive, null, {
      default: () => state.visible ? h(RouteReviewPanel, {
        routeKey: state.routeKey,
        days: [{ day: '第1天', title: '拉萨适应', summary: '轻松游览，留出休息时间。' }],
        'onUpdate:notes': onNotes,
        onJump
      }) : null
    })
  }))
  app.use(createI18n({ legacy: false, locale: 'zh', messages: { zh: messages } }))
  app.mount(root)
  await settle()
  return { root, state, onNotes, onJump }
}

const typeNote = async (root: HTMLElement, text: string) => {
  const input = root.querySelector('textarea')!
  input.value = text
  input.dispatchEvent(new Event('input', { bubbles: true }))
  await settle()
}
const clickText = async (root: HTMLElement, text: string) => {
  const button = [...root.querySelectorAll('button')].find(item => item.textContent?.trim() === text)
  expect(button).toBeTruthy()
  button!.click()
  await settle()
}

beforeEach(() => showConfirm.mockReset())
afterEach(() => { app?.unmount(); app = undefined; document.body.innerHTML = '' })

describe('route review notes', () => {
  it('emits plain notes for export, preserves them across kept-alive navigation and guards refresh', async () => {
    const { root, state, onNotes } = await mountPanel()
    await typeNote(root, '下午改为休息，尚未改动原安排')
    expect(onNotes).toHaveBeenLastCalledWith({ '第1天': '下午改为休息，尚未改动原安排' })
    state.visible = false
    await settle()
    state.visible = true
    await settle()
    expect(root.querySelector('textarea')?.value).toBe('下午改为休息，尚未改动原安排')
    const event = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(event)
    expect(event.defaultPrevented).toBe(true)
  })

  it('retains text when clear is cancelled and restores confirmed clear with undo', async () => {
    const { root, onNotes } = await mountPanel()
    await typeNote(root, '晚一点出发')
    showConfirm.mockResolvedValueOnce(false)
    await clickText(root, '清空备注')
    expect(root.querySelector('textarea')?.value).toBe('晚一点出发')
    showConfirm.mockResolvedValueOnce(true)
    await clickText(root, '清空备注')
    expect(onNotes).toHaveBeenLastCalledWith({ '第1天': '' })
    await clickText(root, '撤销清空')
    expect(root.querySelector('textarea')?.value).toBe('晚一点出发')
    expect(onNotes).toHaveBeenLastCalledWith({ '第1天': '晚一点出发' })
  })

  it('resets personal notes and checklist for a new route without claiming verified availability', async () => {
    const { root, state, onNotes } = await mountPanel()
    await typeNote(root, '联系酒店确认')
    root.querySelector<HTMLInputElement>('input[type="checkbox"]')!.click()
    await settle()
    expect(root.textContent).toContain('个人已勾选 1 / 3 项')
    expect(root.textContent).toContain('未经平台核验')
    state.routeKey = 'route-2'
    await settle()
    expect(root.querySelector('textarea')?.value).toBe('')
    expect(root.querySelector<HTMLInputElement>('input[type="checkbox"]')?.checked).toBe(false)
    expect(onNotes).toHaveBeenLastCalledWith({})
    const event = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(event)
    expect(event.defaultPrevented).toBe(false)
  })

  it('keeps original itinerary navigation available', async () => {
    const { root, onJump } = await mountPanel()
    await clickText(root, '查看原安排')
    expect(onJump).toHaveBeenCalledWith('第1天')
  })
})
