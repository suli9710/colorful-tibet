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
        >
          <slot />
        </motion.div>
      </motion.div>
    </AnimatePresence>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
</script>
