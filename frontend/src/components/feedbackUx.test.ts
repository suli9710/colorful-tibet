import { describe, expect, it } from 'vitest'
import guideChatPanelSource from './GuideChatPanel.vue?raw'
import imageUploadFieldSource from './ImageUploadField.vue?raw'
import toastHostSource from './ToastHost.vue?raw'

describe('feedback component UX safeguards', () => {
  it('keeps toast notifications announced and dismissible', () => {
    expect(toastHostSource).toContain(':role="roleClass[toast.type]"')
    expect(toastHostSource).toContain(':aria-live="liveClass[toast.type]"')
    expect(toastHostSource).toContain('aria-atomic="true"')
    expect(toastHostSource).toContain('@click="dismissToast(toast.id)"')
    expect(toastHostSource).toContain('@keydown.escape.stop="dismissToast(toast.id)"')
  })

  it('keeps image upload failure, loading, retry, and disabled states accessible', () => {
    expect(imageUploadFieldSource).toContain('disabled?: boolean')
    expect(imageUploadFieldSource).toContain('const retryUpload')
    expect(imageUploadFieldSource).toContain(':aria-busy="uploading"')
    expect(imageUploadFieldSource).toContain(':aria-disabled="isDisabled"')
    expect(imageUploadFieldSource).toContain(':aria-invalid="Boolean(uploadError)"')
    expect(imageUploadFieldSource).toContain('role="status"')
    expect(imageUploadFieldSource).toContain('role="alert"')
    expect(imageUploadFieldSource).toContain("uploadLabel('retry')")
  })

  it('keeps guide chat progressive status updates available to assistive tech', () => {
    expect(guideChatPanelSource).toContain('role="log"')
    expect(guideChatPanelSource).toContain('aria-relevant="additions text"')
    expect(guideChatPanelSource).toContain(':aria-busy="isSending"')
    expect(guideChatPanelSource).toContain('role="status"')
    expect(guideChatPanelSource).toContain('aria-live="polite"')
    expect(guideChatPanelSource).toContain(':aria-describedby="inputDescribedBy"')
    expect(guideChatPanelSource).toContain(':aria-label="t(\'guideChat.placeholder\')"')
    expect(guideChatPanelSource).toContain(':aria-label="sendButtonLabel"')
    expect(guideChatPanelSource).toContain(':disabled="!canSend"')
    expect(guideChatPanelSource).toContain('e.isComposing')
    expect(guideChatPanelSource).toContain('msg.fallback || msg.networkFallback')
  })
})
