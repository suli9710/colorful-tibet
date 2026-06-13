<script setup lang="ts">
import { computed } from 'vue'
import { useReducedMotion } from 'motion-v'

/**
 * 风马旗 / 经幡 (Lung ta prayer-flag bunting).
 *
 * Renders a rope of rectangular prayer flags in the canonical five-element
 * colour order — 蓝(天/sky) 白(风/air) 红(火/fire) 绿(水/water) 黄(土/earth) —
 * each fluttering independently like wind over a mountain pass. Purely
 * decorative, so it is hidden from assistive tech and honours reduced motion.
 */
const props = withDefaults(defineProps<{ count?: number }>(), { count: 15 })

const PALETTE = ['#2D5F8A', '#F7F3EE', '#8B2E3A', '#3BA99C', '#F2C94C']

const reduced = useReducedMotion()

const flags = computed(() =>
  Array.from({ length: Math.max(1, props.count) }, (_, index) => ({
    color: PALETTE[index % PALETTE.length],
    index
  }))
)
</script>

<template>
  <div class="prayer-flags" :class="{ 'prayer-flags--static': reduced }" aria-hidden="true">
    <span class="prayer-flags__rope"></span>
    <span
      v-for="flag in flags"
      :key="flag.index"
      class="prayer-flag"
      :style="{
        '--flag-color': flag.color,
        '--flag-index': flag.index,
        '--flag-droop': `${Math.sin((flag.index / Math.max(1, count - 1)) * Math.PI) * 12}px`,
        '--flag-tilt': flag.index % 2 === 0 ? 2 : -2
      }"
    >
      <span class="prayer-flag__script"></span>
    </span>
  </div>
</template>

<style scoped>
.prayer-flags {
  position: relative;
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-start;
  justify-content: center;
  gap: 4px;
  width: 100%;
  padding-top: 2px;
  pointer-events: none;
}

/* The string the flags hang from, sagging gently like a real garland. */
.prayer-flags__rope {
  position: absolute;
  top: 2px;
  left: 4%;
  right: 4%;
  height: 16px;
  border-top: 1.5px solid rgba(92, 61, 46, 0.6);
  border-radius: 50% / 100%;
  opacity: 0.75;
}

.prayer-flag {
  position: relative;
  flex: 0 0 auto;
  width: clamp(14px, 4.4vw, 26px);
  height: clamp(20px, 6vw, 34px);
  background: var(--flag-color);
  border-radius: 1px 1px 2px 2px;
  /* Garland sag plus a small hand-strung tilt so it never reads as a flat bar. */
  transform: translateY(var(--flag-droop)) rotate(calc((var(--flag-tilt)) * 1deg));
  transform-origin: top center;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.4),
    0 6px 10px rgba(26, 21, 32, 0.2);
  animation: flagFlutter 3.4s ease-in-out infinite;
  animation-delay: calc(var(--flag-index) * -0.31s);
  will-change: transform;
}

/* The little fold of cloth knotted over the rope. */
.prayer-flag::before {
  content: '';
  position: absolute;
  top: -3px;
  left: 50%;
  width: 70%;
  height: 5px;
  transform: translateX(-50%);
  background: rgba(26, 21, 32, 0.28);
  border-radius: 2px 2px 0 0;
}

/* Faint woodblock mantra lines printed on each flag. */
.prayer-flag__script {
  position: absolute;
  inset: 7px 4px 5px;
  background: repeating-linear-gradient(
    0deg,
    rgba(26, 21, 32, 0.16) 0 1px,
    transparent 1px 5px
  );
  opacity: 0.55;
  border-radius: 1px;
}

@keyframes flagFlutter {
  0%, 100% {
    transform: translateY(var(--flag-droop)) rotate(calc(var(--flag-tilt) * 1deg)) skewX(0deg);
  }
  25% {
    transform: translateY(var(--flag-droop)) rotate(calc(var(--flag-tilt) * 1deg + 2.2deg)) skewX(-4deg) scaleY(0.98);
  }
  55% {
    transform: translateY(var(--flag-droop)) rotate(calc(var(--flag-tilt) * 1deg - 1.6deg)) skewX(3deg);
  }
  80% {
    transform: translateY(var(--flag-droop)) rotate(calc(var(--flag-tilt) * 1deg + 1deg)) skewX(-2deg);
  }
}

.prayer-flags--static .prayer-flag {
  animation: none;
}

@media (prefers-reduced-motion: reduce) {
  .prayer-flag {
    animation: none;
  }
}
</style>
