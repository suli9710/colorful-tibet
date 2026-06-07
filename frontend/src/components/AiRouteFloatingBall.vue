<script setup lang="ts">
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bot } from 'lucide-vue-next'
import { AnimatePresence, motion } from 'motion-v'
import { useRouteGenerationStore } from '../stores/routeGeneration'
import { softSpring } from '../motion/presets'
import GuideChatPanel from './GuideChatPanel.vue'

interface Live2DCharacterHandle {
  setMotion: (name: string) => boolean | undefined
}

const Live2DCharacter = defineAsyncComponent(() => import('./Live2DCharacter.vue'))

const router = useRouter()
const route = useRoute()
const store = useRouteGenerationStore()

const chatOpen = ref(false)
const showBubble = ref(false)
const live2dRef = ref<Live2DCharacterHandle>()
const isDesktop = ref(true)
const hasShownInitialBubble = ref(false)

const isOnRoutePlanner = computed(() => route.name === 'route-planner')
const visible = computed(() => !isOnRoutePlanner.value)
const isIdle = computed(() => !store.isGenerating && !store.isCompleted)
const isGenerating = computed(() => store.isGenerating)
const isCompleted = computed(() => store.isCompleted)

const charWidth = 150
const charHeight = 163

const greetings = [
  '想去西藏旅行吗？点我聊聊',
  '我可以帮你规划路线',
  '西藏美景等着你，点我开始',
  '想去哪儿玩？我来帮你安排',
  '一起探索西藏吧'
]
const greetingIndex = ref(Math.floor(Math.random() * greetings.length))
const currentGreeting = computed(() => greetings[greetingIndex.value])

const bubbleText = computed(() => {
  if (isCompleted.value) return '路线已生成，快来看看'
  if (isGenerating.value) return '正在为你规划路线...'
  return currentGreeting.value
})

const buttonLabel = computed(() => {
  if (isCompleted.value) return '查看已生成路线'
  if (isGenerating.value) return '查看 AI 路线生成进度'
  return '打开 AI 路线助手'
})

const currentMotion = computed(() => {
  if (isCompleted.value) return 'jumping'
  if (isGenerating.value) return 'waiting'
  if (chatOpen.value) return 'waving'
  return 'idle'
})

let jumpCheckTimer: ReturnType<typeof setInterval> | null = null
let hideBubbleTimer: number | undefined
let desktopMediaQuery: MediaQueryList | undefined

function updateDesktopState(event?: MediaQueryListEvent) {
  isDesktop.value = event?.matches ?? desktopMediaQuery?.matches ?? true
}

function clearHideBubbleTimer() {
  if (hideBubbleTimer) {
    window.clearTimeout(hideBubbleTimer)
    hideBubbleTimer = undefined
  }
}

watch(currentMotion, (motion) => {
  if (motion === 'jumping') {
    if (jumpCheckTimer) clearInterval(jumpCheckTimer)
    jumpCheckTimer = setInterval(() => {
      if (isCompleted.value && live2dRef.value) {
        live2dRef.value.setMotion('waving')
      }
    }, 1200)
    return
  }

  if (jumpCheckTimer) {
    clearInterval(jumpCheckTimer)
    jumpCheckTimer = null
  }
})

watch([visible, isIdle], ([v, idle]) => {
  if (v && idle && !hasShownInitialBubble.value) {
    hasShownInitialBubble.value = true
    showBubble.value = true
    clearHideBubbleTimer()
    hideBubbleTimer = window.setTimeout(() => {
      showBubble.value = false
    }, 5000)
  }
  if (v && (isGenerating.value || isCompleted.value)) {
    showBubble.value = true
  }
}, { immediate: true })

onMounted(() => {
  desktopMediaQuery = window.matchMedia('(min-width: 768px)')
  updateDesktopState()
  desktopMediaQuery.addEventListener('change', updateDesktopState)
})

onBeforeUnmount(() => {
  if (jumpCheckTimer) clearInterval(jumpCheckTimer)
  clearHideBubbleTimer()
  desktopMediaQuery?.removeEventListener('change', updateDesktopState)
})

function handleClick() {
  if (isIdle.value) {
    greetingIndex.value = (greetingIndex.value + 1) % greetings.length
    chatOpen.value = !chatOpen.value
    if (chatOpen.value) {
      live2dRef.value?.setMotion('waving')
    }
    return
  }

  if (isCompleted.value) {
    store.acknowledgeResult()
  }
  router.push('/route-planner')
}

function handleChatClose() {
  chatOpen.value = false
}

function onCharacterEnter() {
  showBubble.value = true
  clearHideBubbleTimer()
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
</script>

<template>
  <AnimatePresence>
    <div
      v-if="visible"
      key="ai-guide-container"
      class="ai-guide-container fixed bottom-4 right-3 z-[90] flex max-w-[calc(100vw-1.5rem)] flex-col items-end gap-2 sm:bottom-6 sm:right-6"
    >
      <GuideChatPanel :visible="chatOpen && isIdle" @close="handleChatClose" />

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
          <span v-if="isCompleted" class="sparkle sparkle-1" aria-hidden="true">*</span>
          <span v-if="isCompleted" class="sparkle sparkle-2" aria-hidden="true">*</span>
          <span v-if="isCompleted" class="sparkle sparkle-3" aria-hidden="true">*</span>
        </motion.div>
      </AnimatePresence>

      <motion.button
        class="live2d-char-btn"
        :class="{
          'char-state-idle': isIdle,
          'char-state-generating': isGenerating,
          'char-state-completed': isCompleted,
          'mobile-lite-btn': !isDesktop
        }"
        :aria-label="buttonLabel"
        :title="buttonLabel"
        :initial="{ opacity: 0, scale: 0, y: 24 }"
        :animate="{ opacity: 1, scale: 1, y: 0 }"
        :exit="{ opacity: 0, scale: 0, y: 24 }"
        :transition="softSpring"
        :whileHover="{ scale: 1.06 }"
        :whileTap="{ scale: 0.94 }"
        @click="handleClick"
        @mouseenter="onCharacterEnter"
        @mouseleave="onCharacterLeave"
      >
        <Live2DCharacter
          v-if="isDesktop"
          ref="live2dRef"
          :motion="currentMotion"
          :width="charWidth"
          :height="charHeight"
        />
        <span v-else class="mobile-ai-icon" aria-hidden="true">
          <Bot :size="26" :stroke-width="2.2" />
        </span>

        <span
          v-if="isCompleted"
          class="character-badge absolute -right-1 -top-1 flex items-center gap-1 rounded-full bg-emerald-500 px-2.5 py-0.5 text-[10px] font-bold text-white shadow-lg"
        >
          <span class="badge-dot h-1.5 w-1.5 rounded-full bg-white"></span>
          完成
        </span>
      </motion.button>
    </div>
  </AnimatePresence>
</template>

<style scoped>
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
  transition: filter 0.3s ease, background 0.3s ease, box-shadow 0.3s ease;
}

.live2d-char-btn:hover {
  filter: drop-shadow(0 4px 12px rgba(245, 158, 11, 0.3));
}

.mobile-lite-btn {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #0f766e, #d97706);
  box-shadow: 0 10px 24px rgba(15, 118, 110, 0.28);
}

.mobile-ai-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #ffffff;
}

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
  background: linear-gradient(135deg, #fff7ed, #f0fdfa);
  border: 1.5px solid #f59e0b;
  color: #92400e;
}

.speech-bubble-idle::after {
  border-top: 10px solid #fff7ed;
}

.speech-bubble-generating {
  background: linear-gradient(135deg, #eff6ff, #f0fdfa);
  border: 1.5px solid #38bdf8;
  color: #1e40af;
}

.speech-bubble-generating::after {
  border-top: 10px solid #eff6ff;
}

.speech-bubble-completed {
  background: linear-gradient(135deg, #ecfdf5, #fff7ed);
  border: 1.5px solid #10b981;
  color: #047857;
}

.speech-bubble-completed::after {
  border-top: 10px solid #ecfdf5;
}

.sparkle {
  position: absolute;
  font-size: 12px;
  color: #d97706;
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

@media (max-width: 767px) {
  .ai-guide-container {
    bottom: calc(6rem + env(safe-area-inset-bottom));
  }

  .speech-bubble {
    max-width: min(17rem, calc(100vw - 2rem));
    padding: 8px 12px;
  }
}
</style>
