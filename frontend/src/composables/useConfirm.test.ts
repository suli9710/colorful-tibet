import { afterEach, describe, expect, it } from 'vitest'
import { activeConfirm, clearConfirms, resolveConfirm, showConfirm } from './useConfirm'

describe('useConfirm', () => {
  afterEach(() => {
    clearConfirms()
  })

  it('normalizes requests and resolves accepted decisions', async () => {
    const decision = showConfirm({
      title: ' Delete ',
      message: '  Delete this record?  ',
      confirmLabel: ' Delete ',
      cancelLabel: ' Keep ',
      tone: 'danger'
    })

    expect(activeConfirm.value).toMatchObject({
      title: 'Delete',
      message: 'Delete this record?',
      confirmLabel: 'Delete',
      cancelLabel: 'Keep',
      tone: 'danger'
    })

    resolveConfirm(true)

    await expect(decision).resolves.toBe(true)
    expect(activeConfirm.value).toBeNull()
  })

  it('queues requests and resolves them in order', async () => {
    const first = showConfirm('First?')
    const second = showConfirm('Second?')

    expect(activeConfirm.value?.message).toBe('First?')

    resolveConfirm(false)
    await expect(first).resolves.toBe(false)
    expect(activeConfirm.value?.message).toBe('Second?')

    resolveConfirm(true)
    await expect(second).resolves.toBe(true)
    expect(activeConfirm.value).toBeNull()
  })

  it('ignores empty messages', async () => {
    await expect(showConfirm('   ')).resolves.toBe(false)
    expect(activeConfirm.value).toBeNull()
  })
})
