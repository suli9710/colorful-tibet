<template>
  <Teleport to="body">
    <AnimatePresence>
      <motion.div
        v-if="show"
        :key="modalKey"
        :class="rootClasses"
        :initial="modalRootInitial"
        :animate="modalRootAnimate"
        :exit="modalRootExit"
        :transition="modalBackdropTransition"
        role="presentation"
        @click.self="handleBackdropClick"
      >
        <motion.div
          class="pointer-events-none absolute inset-0"
          :class="backdropClass"
          :initial="modalRootInitial"
          :animate="modalRootAnimate"
          :exit="modalRootExit"
          :transition="modalBackdropTransition"
        />
        <motion.div
          :class="panelClasses"
          :initial="modalPanelInitial"
          :animate="modalPanelAnimate"
          :exit="modalPanelExit"
          :transition="modalPanelTransition"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="props.labelledBy || undefined"
          tabindex="-1"
          ref="panelRef"
        >
          <slot />
        </motion.div>
      </motion.div>
    </AnimatePresence>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { AnimatePresence, motion } from 'motion-v'
import {
  modalBackdropTransition,
  modalPanelAnimate,
  modalPanelExit,
  modalPanelInitial,
  modalPanelTransition,
  modalRootAnimate,
  modalRootExit,
  modalRootInitial
} from '../../motion/presets'

const props = withDefaults(defineProps<{
  show: boolean
  modalKey?: string
  rootClass?: string
  backdropClass?: string
  panelClass?: string
  closeOnBackdrop?: boolean
  labelledBy?: string
}>(), {
  modalKey: 'motion-modal',
  rootClass: '',
  backdropClass: 'bg-black/50 backdrop-blur-sm',
  panelClass: 'max-w-lg rounded-2xl bg-white p-6',
  closeOnBackdrop: true
})

const emit = defineEmits<{
  close: []
}>()

type PanelRef = HTMLElement | { $el?: HTMLElement }

const panelRef = ref<PanelRef | null>(null)
let previouslyFocusedElement: HTMLElement | null = null

const focusableSelector = [
  'a[href]',
  'button:not([disabled])',
  'textarea:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(',')

const getPanelElement = () => {
  const panel = panelRef.value
  if (panel instanceof HTMLElement) return panel
  return panel?.$el instanceof HTMLElement ? panel.$el : null
}

const rootClasses = computed(() => [
  'fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto',
  props.rootClass
])

const panelClasses = computed(() => [
  'relative w-full shadow-2xl',
  props.panelClass
])

const handleBackdropClick = () => {
  if (props.closeOnBackdrop) {
    emit('close')
  }
}

const getFocusableElements = () => {
  const panel = getPanelElement()
  if (!panel) return []
  return Array.from(panel.querySelectorAll<HTMLElement>(focusableSelector))
    .filter(element => !element.hasAttribute('disabled') && element.tabIndex !== -1)
}

const focusInitialElement = async () => {
  await nextTick()
  const panel = getPanelElement()
  if (!panel) return
  const [firstFocusable] = getFocusableElements()
  ;(firstFocusable || panel).focus({ preventScroll: true })
}

const restoreFocus = () => {
  const element = previouslyFocusedElement
  previouslyFocusedElement = null
  if (element && typeof element.focus === 'function' && document.contains(element)) {
    element.focus({ preventScroll: true })
  }
}

const handleKeydown = (event: KeyboardEvent) => {
  if (!props.show) return

  if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
    return
  }

  if (event.key !== 'Tab') return

  const focusable = getFocusableElements()
  if (!focusable.length) {
    event.preventDefault()
    getPanelElement()?.focus({ preventScroll: true })
    return
  }

  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  const activeElement = document.activeElement
  const panel = getPanelElement()

  if (panel && activeElement instanceof Node && !panel.contains(activeElement)) {
    event.preventDefault()
    first.focus({ preventScroll: true })
    return
  }

  if (event.shiftKey && activeElement === first) {
    event.preventDefault()
    last.focus({ preventScroll: true })
  } else if (!event.shiftKey && activeElement === last) {
    event.preventDefault()
    first.focus({ preventScroll: true })
  }
}

watch(
  () => props.show,
  show => {
    if (show) {
      previouslyFocusedElement = document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null
      document.addEventListener('keydown', handleKeydown)
      void focusInitialElement()
      return
    }

    document.removeEventListener('keydown', handleKeydown)
    restoreFocus()
  },
  { flush: 'post' }
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  restoreFocus()
})
</script>
