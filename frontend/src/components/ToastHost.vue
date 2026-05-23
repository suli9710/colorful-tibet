<script setup lang="ts">
import { computed } from 'vue'
import { dismissToast, useToast, type ToastType } from '../composables/useToast'

const { toasts } = useToast()

const toastClass = computed<Record<ToastType, string>>(() => ({
  info: 'border-tibet-gold/40 bg-white text-tibet-dark',
  success: 'border-emerald-300 bg-emerald-50 text-emerald-900',
  warning: 'border-amber-300 bg-amber-50 text-amber-950',
  error: 'border-rose-300 bg-rose-50 text-rose-950'
}))

const accentClass = computed<Record<ToastType, string>>(() => ({
  info: 'bg-tibet-gold',
  success: 'bg-emerald-500',
  warning: 'bg-amber-500',
  error: 'bg-rose-500'
}))
</script>

<template>
  <Teleport to="body">
    <div
      class="mobile-toast-host fixed right-4 top-24 z-[120] flex w-[calc(100vw-2rem)] max-w-sm flex-col gap-3 sm:right-6"
      aria-live="polite"
      aria-atomic="true"
    >
      <TransitionGroup
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="translate-x-6 opacity-0"
        enter-to-class="translate-x-0 opacity-100"
        leave-active-class="transition duration-200 ease-in"
        leave-from-class="translate-x-0 opacity-100"
        leave-to-class="translate-x-6 opacity-0"
      >
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="relative overflow-hidden rounded-2xl border px-4 py-3 pr-10 shadow-xl shadow-tibet-brown/10 backdrop-blur"
          :class="toastClass[toast.type]"
          role="status"
        >
          <span
            class="absolute inset-y-0 left-0 w-1"
            :class="accentClass[toast.type]"
            aria-hidden="true"
          ></span>
          <p class="text-sm leading-6">{{ toast.message }}</p>
          <button
            type="button"
            class="absolute right-3 top-3 rounded-full text-current/60 transition hover:text-current focus:outline-none focus:ring-2 focus:ring-tibet-gold/50"
            aria-label="关闭通知"
            @click="dismissToast(toast.id)"
          >
            ×
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
@media (max-width: 640px) {
  .mobile-toast-host {
    left: 1rem;
    right: 1rem;
    top: 5.5rem;
    width: auto;
    max-width: none;
  }
}
</style>
