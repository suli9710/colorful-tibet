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
const hasSecondaryAction = computed(() => Boolean(props.secondaryLabel))
const actionBarLabel = computed(() =>
  [props.eyebrow, props.title, props.meta].filter(Boolean).join(' / ') || props.primaryLabel
)
</script>

<template>
  <Teleport to="body">
    <div
      v-if="show"
      class="mobile-sticky-action fixed inset-x-0 bottom-0 z-[92] md:hidden"
      role="region"
      :aria-label="actionBarLabel"
    >
      <div class="mobile-sticky-action__shell rounded-3xl border border-tibet-gold/20 bg-white/[0.94] p-3 shadow-2xl shadow-tibet-dark/15 backdrop-blur-xl">
        <div
          class="mobile-sticky-action__content"
          :class="[
            hasSummary ? 'mobile-sticky-action__content--summary' : 'mobile-sticky-action__content--actions-only',
            hasSecondaryAction ? 'mobile-sticky-action__content--secondary' : 'mobile-sticky-action__content--single'
          ]"
        >
          <div v-if="hasSummary" class="mobile-sticky-action__summary min-w-0" aria-live="polite">
            <p v-if="eyebrow" class="text-[11px] font-semibold uppercase tracking-wide text-tibet-brown/45">{{ eyebrow }}</p>
            <p v-if="title" class="truncate text-base font-bold leading-tight text-tibet-dark">{{ title }}</p>
            <p v-if="meta" class="truncate text-xs leading-tight text-tibet-brown/58">{{ meta }}</p>
          </div>
          <button
            v-if="secondaryLabel"
            type="button"
            class="mobile-sticky-action__button mobile-sticky-action__button--secondary min-h-12 rounded-2xl border border-tibet-gold/25 px-4 py-2.5 text-sm font-semibold leading-tight text-tibet-brown transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-45 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white"
            :disabled="secondaryDisabled"
            :aria-label="secondaryLabel"
            @click="emit('secondary')"
          >
            <span class="line-clamp-2 break-words">{{ secondaryLabel }}</span>
          </button>
          <button
            type="button"
            class="mobile-sticky-action__button mobile-sticky-action__button--primary min-h-12 rounded-2xl bg-tibet-red px-5 py-2.5 text-sm font-bold leading-tight text-tibet-yellow shadow-lg shadow-tibet-red/20 transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-45 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-tibet-gold/70 focus-visible:ring-offset-2 focus-visible:ring-offset-white"
            :disabled="primaryDisabled"
            :aria-label="primaryLabel"
            @click="emit('primary')"
          >
            <span class="line-clamp-2 break-words">{{ primaryLabel }}</span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.mobile-sticky-action {
  padding:
    0 max(0.75rem, env(safe-area-inset-right))
    calc(5.85rem + env(safe-area-inset-bottom))
    max(0.75rem, env(safe-area-inset-left));
  pointer-events: none;
}

.mobile-sticky-action__shell {
  pointer-events: auto;
}

.mobile-sticky-action__content {
  display: grid;
  align-items: center;
  gap: 0.75rem;
}

.mobile-sticky-action__content--summary.mobile-sticky-action__content--secondary {
  grid-template-columns: minmax(0, 1fr) auto auto;
}

.mobile-sticky-action__content--summary.mobile-sticky-action__content--single {
  grid-template-columns: minmax(0, 1fr) auto;
}

.mobile-sticky-action__content--actions-only.mobile-sticky-action__content--secondary {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.mobile-sticky-action__content--actions-only.mobile-sticky-action__content--single {
  grid-template-columns: minmax(0, 1fr);
}

.mobile-sticky-action__button {
  min-width: 0;
  max-width: 100%;
  -webkit-tap-highlight-color: transparent;
  touch-action: manipulation;
}

.mobile-sticky-action__content--summary .mobile-sticky-action__button {
  max-width: min(44vw, 13rem);
}

@media (max-width: 380px) {
  .mobile-sticky-action {
    padding-left: max(0.5rem, env(safe-area-inset-left));
    padding-right: max(0.5rem, env(safe-area-inset-right));
  }

  .mobile-sticky-action__content--summary.mobile-sticky-action__content--secondary {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }

  .mobile-sticky-action__content--summary.mobile-sticky-action__content--single {
    grid-template-columns: minmax(0, 1fr);
  }

  .mobile-sticky-action__summary {
    grid-column: 1 / -1;
  }

  .mobile-sticky-action__content--summary .mobile-sticky-action__button {
    width: 100%;
    max-width: none;
  }
}

@media (max-height: 420px) and (max-width: 767px) {
  .mobile-sticky-action {
    padding-bottom: max(0.75rem, env(safe-area-inset-bottom));
  }

  .mobile-sticky-action__shell {
    padding: 0.625rem;
  }

  .mobile-sticky-action__summary {
    display: none;
  }

  .mobile-sticky-action__button {
    min-height: 44px;
  }
}
</style>
