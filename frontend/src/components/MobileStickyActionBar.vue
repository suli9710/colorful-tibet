<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  show?: boolean
  eyebrow?: string
  title?: string
  meta?: string
  primaryLabel: string
  secondaryLabel?: string
  primaryDisabled?: boolean
  secondaryDisabled?: boolean
}>(), {
  show: true,
  eyebrow: '',
  title: '',
  meta: '',
  secondaryLabel: '',
  primaryDisabled: false,
  secondaryDisabled: false
})

const emit = defineEmits<{
  primary: []
  secondary: []
}>()

const hasSummary = computed(() => Boolean(props.eyebrow || props.title || props.meta))
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="mobile-sticky-action fixed inset-x-0 bottom-0 z-[92] md:hidden">
      <div class="mx-3 mb-[5.85rem] rounded-3xl border border-tibet-gold/20 bg-white/[0.94] p-3 shadow-2xl shadow-tibet-dark/15 backdrop-blur-xl">
        <div class="flex items-center gap-3">
          <div v-if="hasSummary" class="min-w-0 flex-1">
            <p v-if="eyebrow" class="text-[11px] font-semibold uppercase tracking-wide text-tibet-brown/45">{{ eyebrow }}</p>
            <p v-if="title" class="truncate text-base font-bold text-tibet-dark">{{ title }}</p>
            <p v-if="meta" class="truncate text-xs text-tibet-brown/58">{{ meta }}</p>
          </div>
          <button
            v-if="secondaryLabel"
            type="button"
            class="shrink-0 rounded-2xl border border-tibet-gold/25 px-4 py-3 text-sm font-semibold text-tibet-brown transition active:scale-95 disabled:opacity-45"
            :disabled="secondaryDisabled"
            @click="emit('secondary')"
          >
            {{ secondaryLabel }}
          </button>
          <button
            type="button"
            class="shrink-0 rounded-2xl bg-tibet-red px-5 py-3 text-sm font-bold text-tibet-yellow shadow-lg shadow-tibet-red/20 transition active:scale-95 disabled:opacity-45"
            :disabled="primaryDisabled"
            @click="emit('primary')"
          >
            {{ primaryLabel }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.mobile-sticky-action {
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
