<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { activeConfirm, resolveConfirm } from '../composables/useConfirm'

const { t } = useI18n()
const panel = ref<HTMLDivElement | null>(null)
const cancelButton = ref<HTMLButtonElement | null>(null)
let previouslyFocusedElement: Element | null = null

const title = computed(() => activeConfirm.value?.title || t('common.confirm'))
const confirmLabel = computed(() => activeConfirm.value?.confirmLabel || t('common.confirm'))
const cancelLabel = computed(() => activeConfirm.value?.cancelLabel || t('common.cancel'))
const isDanger = computed(() => activeConfirm.value?.tone === 'danger')

watch(activeConfirm, request => {
  if (!request) {
    if (previouslyFocusedElement instanceof HTMLElement) {
      previouslyFocusedElement.focus()
    }
    previouslyFocusedElement = null
    return
  }

  if (!previouslyFocusedElement) {
    previouslyFocusedElement = document.activeElement
  }
  void nextTick(() => cancelButton.value?.focus())
})

const handleTabKey = (event: KeyboardEvent) => {
  const focusableElements = panel.value?.querySelectorAll<HTMLElement>(
    'button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'
  )
  const focusable = Array.from(focusableElements ?? [])
  const first = focusable[0]
  const last = focusable[focusable.length - 1]

  if (!first || !last) {
    event.preventDefault()
    panel.value?.focus()
    return
  }

  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="activeConfirm"
        class="fixed inset-0 z-[130] flex items-end justify-center bg-tibet-dark/45 px-4 py-5 backdrop-blur-sm sm:items-center"
        @keydown.esc.prevent="resolveConfirm(false)"
        @keydown.tab="handleTabKey"
      >
        <div
          ref="panel"
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-dialog-title"
          aria-describedby="confirm-dialog-message"
          tabindex="-1"
          class="w-full max-w-md rounded-2xl border border-white/60 bg-white p-5 shadow-2xl shadow-tibet-dark/20 outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70"
        >
          <div class="mb-5">
            <p id="confirm-dialog-title" class="text-base font-bold text-tibet-dark">
              {{ title }}
            </p>
            <p id="confirm-dialog-message" class="mt-2 text-sm leading-6 text-tibet-brown/75">
              {{ activeConfirm.message }}
            </p>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <button
              ref="cancelButton"
              type="button"
              class="min-h-11 rounded-xl border border-tibet-gold/25 px-4 py-2 text-sm font-semibold text-tibet-brown transition hover:bg-tibet-gold/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70"
              @click="resolveConfirm(false)"
            >
              {{ cancelLabel }}
            </button>
            <button
              type="button"
              class="min-h-11 rounded-xl px-4 py-2 text-sm font-bold text-white shadow-lg transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70"
              :class="isDanger ? 'bg-rose-600 shadow-rose-600/20 hover:bg-rose-700' : 'bg-tibet-red shadow-tibet-red/20 hover:bg-tibet-red/90'"
              @click="resolveConfirm(true)"
            >
              {{ confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
