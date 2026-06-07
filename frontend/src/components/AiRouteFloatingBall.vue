<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { AnimatePresence, motion } from 'motion-v'
import { useRouteGenerationStore } from '../stores/routeGeneration'
import { softSpring } from '../motion/presets'
import GuideChatPanel from './GuideChatPanel.vue'
import Live2DCharacter from './Live2DCharacter.vue'

const router = useRouter()
const route = useRoute()
const store = useRouteGenerationStore()

const isOnRoutePlanner = computed(() => route.name === 'route-planner')
const visible = computed(() => !isOnRoutePlanner.value)
const isIdle = computed(() => !store.isGenerating && !store.isCompleted)
const isGenerating = computed(() => store.isGenerating)
const isCompleted = computed(() => store.isCompleted)

const chatOpen = ref(false)
const live2dRef = ref<InstanceType<typeof Live2DCharacter>>()

// Character size: proportional to model atlas (192:208)
const charWidth = 150
const charHeight = 163

const greetings = [
  '想去西藏旅行吗？点我聊天！',
  '嘿～让我帮你规划路线吧！',
  '西藏美景等着你，点我开始！',
  '想去哪里玩？我来帮你安排！',
  '一起探索神秘的西藏吧！'
]
const greetingIndex = ref(Math.floor(Math.random() * greetings.length))
const currentGreeting = computed(() => greetings[greetingIndex.value])

const bubbleText = computed(() => {
  if (isCompleted.value) return '路线已生成，快来看看吧！'
  if (isGenerating.value) return '正在为你规划路线...'
  return currentGreeting.value
})

// Motion mapping: app state → Live2D motion name
const currentMotion = computed(() => {
  if (isCompleted.value) return 'jumping'
  if (isGenerating.value) return 'waiting'
  if (chatOpen.value) return 'waving'
  return 'idle'
})

// When jumping finishes (non-loop), switch to waving for completed state
let jumpCheckTimer: ReturnType<typeof setInterval> | null = null

watch(currentMotion, (motion) => {
  if (motion === 'jumping') {
    if (jumpCheckTimer) clearInterval(jumpCheckTimer)
    jumpCheckTimer = setInterval(() => {
      if (isCompleted.value && live2dRef.value) {
        live2dRef.value.setMotion('waving')
      }
    }, 1200)
  } else {
    if (jumpCheckTimer) {
      clearInterval(jumpCheckTimer)
      jumpCheckTimer = null
    }
  }
})

onBeforeUnmount(() => {
  if (jumpCheckTimer) clearInterval(jumpCheckTimer)
})

function handleClick() {
  if (isIdle.value) {
    greetingIndex.value = (greetingIndex.value + 1) % greetings.length
    chatOpen.value = !chatOpen.value
    if (chatOpen.value) {
      live2dRef.value?.setMotion('waving')
    }
  } else {
    if (isCompleted.value) {
      store.acknowledgeResult()
    }
    router.push('/route-planner')
  }
}

function handleChatClose() {
  chatOpen.value = false
}

// Hover: play waving briefly
function onCharacterEnter() {
  showBubble.value = true
  if (hideBubbleTimer) {
    clearTimeout(hideBubbleTimer)
    hideBubbleTimer = undefined
  }
  if (isIdle.value && !chatOpen.value) {
    live2dRef.value?.setMotion('waving')
  }
}

function onCharacterLeave() {
  if (isIdle.value && !chatOpen.value) {
    live2dRef.value?.setMotion('idle')
    hideBubbleTimer = window.setTimeout(() => {
      showBubble.value = false
    }, 600)
  }
}

// Speech bubble visibility
const showBubble = ref(false)
let hideBubbleTimer: number | undefined

const hasShownInitialBubble = ref(false)
watch([visible, isIdle], ([v, idle]) => {
  if (v && idle && !hasShownInitialBubble.value) {
    hasShownInitialBubble.value = true
    showBubble.value = true
    hideBubbleTimer = window.setTimeout(() => {
      showBubble.value = false
    }, 5000)
  }
  if (v && (isGenerating.value || isCompleted.value)) {
    showBubble.value = true
  }
}, { immediate: true })
</script>

<template>
  <AnimatePresence>
    <div v-if="visible" key="ai-guide-container" class="ai-guide-container fixed bottom-4 right-3 z-[90] flex max-w-[calc(100vw-1.5rem)] flex-col items-end gap-2 sm:bottom-6 sm:right-6">
      <!-- Chat Panel (idle only) -->
      <GuideChatPanel :visible="chatOpen && isIdle" @close="handleChatClose" />

      <!-- Speech bubble -->
      <AnimatePresence>
        <motion.div
          v-if="showBubble || isGenerating || isCompleted"
          key="ai-guide-bubble"
          :initial="{ opacity: 0, y: 8, scale: 0.9 }"
          :animate="{ opacity: 1, y: 0, scale: 1 }"
          :exit="{ opacity: 0, y: 6, scale: 0.92 }"
          :transition="{ ...softSpring, duration: 0.35 }"
          class="speech-bubble relative mb-1 mr-2 select-none"
          :class="{
            'speech-bubble-idle': isIdle,
            'speech-bubble-generating': isGenerating,
            'speech-bubble-completed': isCompleted
          }"
        >
          <span class="relative z-10 block max-w-[calc(100vw-7rem)] text-right text-xs font-semibold leading-relaxed sm:max-w-none sm:whitespace-nowrap">
            {{ bubbleText }}
          </span>
          <!-- Sparkles for completed -->
          <span v-if="isCompleted" class="sparkle sparkle-1">✦</span>
          <span v-if="isCompleted" class="sparkle sparkle-2">✦</span>
          <span v-if="isCompleted" class="sparkle sparkle-3">✧</span>
        </motion.div>
      </AnimatePresence>

      <!-- Live2D Character -->
      <motion.button
        @click="handleClick"
        @mouseenter="onCharacterEnter"
        @mouseleave="onCharacterLeave"
        class="live2d-char-btn"
        :class="{
          'char-state-idle': isIdle,
          'char-state-generating': isGenerating,
          'char-state-completed': isCompleted
        }"
        :initial="{ opacity: 0, scale: 0, y: 24 }"
        :animate="{ opacity: 1, scale: 1, y: 0 }"
        :exit="{ opacity: 0, scale: 0, y: 24 }"
        :transition="softSpring"
        :whileHover="{ scale: 1.06 }"
        :whileTap="{ scale: 0.94 }"
        :title="isIdle ? '点击和我聊天！' : isCompleted ? '路线已生成，点击查看' : 'AI 路线生成中，点击查看'"
      >
        <Live2DCharacter
          ref="live2dRef"
          :motion="currentMotion"
          :width="charWidth"
          :height="charHeight"
        />

        <!-- Completed badge -->
        <span
          v-if="isCompleted"
          class="absolute -top-1 -right-1 flex items-center gap-1 rounded-full bg-emerald-500 px-2.5 py-0.5 text-[10px] font-bold text-white shadow-lg character-badge"
        >
          <span class="w-1.5 h-1.5 rounded-full bg-white badge-dot"></span>
          完成
        </span>
      </motion.button>
    </div>
  </AnimatePresence>
</template>

<style scoped>
/* ===== Character button wrapper ===== */
.live2d-char-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: v-bind(charWidth + 'px');
  height: v-bind(charHeight + 'px');
  cursor: pointer;
  position: relative;
  border: none;
  background: transparent;
  padding: 0;
  border-radius: 50%;
  transition: filter 0.3s ease;
}

.live2d-char-btn:hover {
  filter: drop-shadow(0 4px 12px rgba(245, 158, 11, 0.3));
}

/* ===== State glow effects ===== */
.char-state-idle {
  filter: drop-shadow(0 4px 12px rgba(245, 158, 11, 0.15));
}

.char-state-generating {
  filter: drop-shadow(0 0 16px rgba(59, 130, 246, 0.35));
  animation: pulse-blue-glow 1.8s ease-in-out infinite;
}

.char-state-completed {
  filter: drop-shadow(0 0 14px rgba(245, 158, 11, 0.35)) drop-shadow(0 0 28px rgba(239, 68, 68, 0.25));
  animation: pulse-gold-glow 1.5s ease-in-out infinite;
}

@keyframes pulse-blue-glow {
  0%, 100% { filter: drop-shadow(0 0 12px rgba(59, 130, 246, 0.25)); }
  50% { filter: drop-shadow(0 0 22px rgba(59, 130, 246, 0.5)) drop-shadow(0 0 36px rgba(59, 130, 246, 0.2)); }
}

@keyframes pulse-gold-glow {
  0%, 100% {
    filter: drop-shadow(0 0 12px rgba(245, 158, 11, 0.3)) drop-shadow(0 0 24px rgba(239, 68, 68, 0.2));
  }
  50% {
    filter: drop-shadow(0 0 20px rgba(245, 158, 11, 0.55)) drop-shadow(0 0 40px rgba(239, 68, 68, 0.35));
  }
}

/* ===== Character Badge ===== */
.character-badge {
  animation: badge-bounce 2s ease-in-out infinite;
}

@keyframes badge-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

.badge-dot {
  animation: dot-blink 1.4s ease-in-out infinite;
}

@keyframes dot-blink {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.5); }
}

/* ===== Speech Bubble ===== */
.speech-bubble {
  padding: 10px 16px;
  border-radius: 18px 18px 4px 18px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1), 0 1px 3px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: visible;
}

.speech-bubble::after {
  content: '';
  position: absolute;
  bottom: -8px;
  right: 16px;
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
}

.speech-bubble-idle {
  background: linear-gradient(135deg, #FFFBEB, #FFF7ED);
  border: 1.5px solid #FCD34D;
  color: #B45309;
}

.speech-bubble-idle::after {
  border-top: 10px solid #FFFBEB;
}

.speech-bubble-generating {
  background: linear-gradient(135deg, #EBF4FF, #F0F9FF);
  border: 1.5px solid #93C5FD;
  color: #1E40AF;
}

.speech-bubble-generating::after {
  border-top: 10px solid #EBF4FF;
}

.speech-bubble-completed {
  background: linear-gradient(135deg, #FFF7ED, #FFFBEB);
  border: 1.5px solid #FCD34D;
  color: #92400E;
}

.speech-bubble-completed::after {
  border-top: 10px solid #FFF7ED;
}

/* Sparkles around speech bubble */
.sparkle {
  position: absolute;
  font-size: 12px;
  color: #D4AF37;
  pointer-events: none;
}

.sparkle-1 {
  top: -8px;
  left: 8px;
  animation: float-sparkle 2s ease-in-out 0s infinite;
}
.sparkle-2 {
  top: -4px;
  right: 20px;
  animation: float-sparkle 2s ease-in-out 0.5s infinite;
}
.sparkle-3 {
  bottom: -6px;
  left: 20px;
  animation: float-sparkle 2s ease-in-out 1s infinite;
}

@keyframes float-sparkle {
  0%, 100% { opacity: 0; transform: translateY(0) scale(0.5); }
  40% { opacity: 0.9; transform: translateY(-4px) scale(1.1); }
  70% { opacity: 0.3; transform: translateY(-2px) scale(0.8); }
}
</style>

<style scoped>
@media (max-width: 767px) {
  .ai-guide-container {
    bottom: calc(6rem + env(safe-area-inset-bottom));
  }

  .live2d-char-btn {
    width: 92px;
    height: 100px;
  }

  .live2d-char-btn :deep(.live2d-character-wrapper) {
    transform: scale(0.62);
    transform-origin: center bottom;
  }

  .speech-bubble {
    max-width: min(17rem, calc(100vw - 2rem));
    padding: 8px 12px;
  }
}
</style>
