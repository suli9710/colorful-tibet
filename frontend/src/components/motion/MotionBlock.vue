<template>
  <motion.div
    :initial="initialState"
    :animate="inView ? undefined : animateState"
    :whileInView="inView ? animateState : undefined"
    :inViewOptions="inView ? inViewOnce : undefined"
    :transition="transitionState"
  >
    <slot />
  </motion.div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { motion } from 'motion-v'
import {
  inViewOnce,
  motionBlockVariants
} from '../../motion/presets'

type MotionBlockVariant = keyof typeof motionBlockVariants

const props = withDefaults(defineProps<{
  variant?: MotionBlockVariant
  index?: number
  delay?: number
  inView?: boolean
}>(), {
  variant: 'reveal',
  index: 0,
  delay: 0,
  inView: false
})

const activeVariant = computed(() => motionBlockVariants[props.variant])
const initialState = computed(() => activeVariant.value.initial)
const animateState = computed(() => activeVariant.value.animate)
const transitionState = computed(() => activeVariant.value.transition(props.index, props.delay))
</script>
