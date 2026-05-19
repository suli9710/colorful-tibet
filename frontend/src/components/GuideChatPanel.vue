<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { AnimatePresence, motion } from 'motion-v'
import { softSpring } from '../motion/presets'
import { useRouteGenerationStore } from '../stores/routeGeneration'
import {
  type ChatMessage,
  processUserMessage,
  getGreeting,
  quickReplies
} from '../services/guideChat'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const router = useRouter()
const generationStore = useRouteGenerationStore()

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

function scrollToBottom() {
  nextTick(() => {
    const el = messagesContainer.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

watch(() => props.visible, (v) => {
  if (v && messages.value.length === 0) {
    messages.value = [getGreeting()]
    scrollToBottom()
  }
})

function sendMessage(text: string) {
  const trimmed = text.trim()
  if (!trimmed) return

  const newMessages = processUserMessage(trimmed)
  messages.value.push(...newMessages)
  inputText.value = ''
  scrollToBottom()
}

function handleQuickReply(reply: (typeof quickReplies)[number]) {
  sendMessage(reply.text)
}

function handleAction(msg: ChatMessage) {
  if (msg.action === 'navigate') {
    router.push('/route-planner')
    emit('close')
  } else if (msg.action === 'generate') {
    generationStore.startGeneration()
    router.push('/route-planner')
    emit('close')
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage(inputText.value)
  }
}
</script>

<template>
  <AnimatePresence>
    <motion.div
      v-if="visible"
      key="guide-chat-panel"
      class="guide-chat-panel"
      :initial="{ opacity: 0, y: 16, scale: 0.95 }"
      :animate="{ opacity: 1, y: 0, scale: 1 }"
      :exit="{ opacity: 0, y: 12, scale: 0.95 }"
      :transition="softSpring"
    >
      <!-- Header -->
      <div class="chat-header">
        <div class="flex items-center gap-2.5">
          <!-- Mini character avatar -->
          <div class="chat-avatar">
            <svg viewBox="0 0 40 44" fill="none" class="w-8 h-8">
              <circle cx="20" cy="16" r="10" fill="#FDEBD0" />
              <path d="M14 8 C14 3 18 0 20 0 C22 0 26 3 26 8" fill="#D4AF37" />
              <ellipse cx="17" cy="14" rx="2.2" ry="2.5" fill="white" />
              <ellipse cx="23" cy="14" rx="2.2" ry="2.5" fill="white" />
              <circle cx="17.5" cy="13.5" r="1.4" fill="#2D1B0E" />
              <circle cx="23.5" cy="13.5" r="1.4" fill="#2D1B0E" />
              <circle cx="18" cy="12.5" r="0.6" fill="white" />
              <circle cx="24" cy="12.5" r="0.6" fill="white" />
              <path d="M17 19 Q20 21 23 19" stroke="#C4956A" stroke-width="1.2" fill="none" stroke-linecap="round" />
              <ellipse cx="14" cy="17" rx="2" ry="1.2" fill="#FFB3B3" opacity="0.5" />
              <ellipse cx="26" cy="17" rx="2" ry="1.2" fill="#FFB3B3" opacity="0.5" />
              <path d="M10 28 C10 20 12 18 16 16 L24 16 C28 18 30 20 30 28 L30 36 C30 40 28 42 24 42 L16 42 C12 42 10 40 10 36 Z" fill="url(#miniRobe)" />
              <defs>
                <linearGradient id="miniRobe" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#C41E3A" />
                  <stop offset="100%" stop-color="#8B0000" />
                </linearGradient>
              </defs>
            </svg>
          </div>
          <div>
            <p class="text-sm font-bold text-gray-800 leading-tight">西藏小导游</p>
            <p class="text-[10px] text-gray-400 flex items-center gap-1">
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
              在线
            </p>
          </div>
        </div>
        <button
          @click="emit('close')"
          class="flex items-center justify-center w-7 h-7 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Messages -->
      <div ref="messagesContainer" class="chat-messages">
        <template v-for="msg in messages" :key="msg.id">
          <!-- Guide message -->
          <div v-if="msg.role === 'guide'" class="flex gap-2 mb-4">
            <div class="chat-avatar shrink-0 mt-1">
              <svg viewBox="0 0 40 44" fill="none" class="w-6 h-6">
                <circle cx="20" cy="16" r="10" fill="#FDEBD0" />
                <path d="M14 8 C14 3 18 0 20 0 C22 0 26 3 26 8" fill="#D4AF37" />
                <ellipse cx="17" cy="14" rx="2" ry="2.3" fill="white" />
                <ellipse cx="23" cy="14" rx="2" ry="2.3" fill="white" />
                <circle cx="17.5" cy="13.5" r="1.3" fill="#2D1B0E" />
                <circle cx="23.5" cy="13.5" r="1.3" fill="#2D1B0E" />
                <path d="M17 18.5 Q20 20.5 23 18.5" stroke="#C4956A" stroke-width="1.2" fill="none" stroke-linecap="round" />
                <path d="M10 28 C10 20 12 18 16 16 L24 16 C28 18 30 20 30 28 L30 36 C30 40 28 42 24 42 L16 42 C12 42 10 40 10 36 Z" fill="#C41E3A" />
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="chat-bubble-guide">
                {{ msg.text }}
              </div>
              <!-- Action button -->
              <motion.button
                v-if="msg.action"
                @click="handleAction(msg)"
                class="mt-2 inline-flex items-center gap-1.5 rounded-full bg-gradient-to-r from-tibet-red to-rose-600 px-3.5 py-1.5 text-xs font-semibold text-white shadow-md shadow-tibet-red/20"
                :whileHover="{ scale: 1.04 }"
                :whileTap="{ scale: 0.96 }"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
                {{ msg.actionLabel || '立即前往' }}
              </motion.button>
            </div>
          </div>

          <!-- User message -->
          <div v-else class="flex justify-end mb-4">
            <div class="chat-bubble-user">
              {{ msg.text }}
            </div>
          </div>
        </template>
      </div>

      <!-- Quick replies -->
      <div v-if="messages.length <= 2" class="quick-replies-bar">
        <button
          v-for="reply in quickReplies"
          :key="reply.label"
          @click="handleQuickReply(reply)"
          class="quick-reply-chip"
        >
          {{ reply.label }}
        </button>
      </div>

      <!-- Input area -->
      <div class="chat-input-area">
        <input
          v-model="inputText"
          @keydown="handleKeydown"
          type="text"
          placeholder="输入你的问题..."
          class="chat-input"
        />
        <button
          @click="sendMessage(inputText)"
          :disabled="!inputText.trim()"
          class="chat-send-btn"
          :class="{ 'opacity-30 cursor-not-allowed': !inputText.trim() }"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
          </svg>
        </button>
      </div>
    </motion.div>
  </AnimatePresence>
</template>

<style scoped>
/* ===== Panel ===== */
.guide-chat-panel {
  position: fixed;
  z-index: 89;
  bottom: 110px;
  right: 24px;
  width: 340px;
  max-width: calc(100vw - 48px);
  height: 460px;
  max-height: calc(100vh - 160px);
  background: white;
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.14),
    0 2px 8px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

@media (max-width: 640px) {
  .guide-chat-panel {
    left: 12px;
    right: 12px;
    bottom: 96px;
    width: auto;
    max-width: none;
    height: min(460px, calc(100dvh - 130px));
    max-height: calc(100dvh - 130px);
    border-radius: 18px;
  }

  .chat-messages {
    padding: 14px 12px;
  }

  .quick-replies-bar {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding: 8px 12px 6px;
    -webkit-overflow-scrolling: touch;
  }

  .quick-reply-chip {
    flex: 0 0 auto;
  }

  .chat-input-area {
    padding: 10px 12px;
  }
}

/* ===== Header ===== */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #F3F4F6;
  background: linear-gradient(135deg, #FFFBEB, #FFF7ED);
  flex-shrink: 0;
}

.chat-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #FEF3C7, #FDE68A);
  overflow: hidden;
  flex-shrink: 0;
}

/* ===== Messages ===== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #E5E7EB;
  border-radius: 2px;
}

.chat-bubble-guide {
  display: inline-block;
  max-width: 100%;
  padding: 10px 14px;
  background: #F3F4F6;
  border-radius: 4px 16px 16px 16px;
  font-size: 13px;
  line-height: 1.55;
  color: #374151;
  word-break: break-word;
}

.chat-bubble-user {
  display: inline-block;
  max-width: 85%;
  padding: 10px 14px;
  background: linear-gradient(135deg, #C41E3A, #DC2626);
  color: white;
  border-radius: 16px 4px 16px 16px;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}

/* ===== Quick Replies ===== */
.quick-replies-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 16px 4px;
  border-top: 1px solid #F3F4F6;
  flex-shrink: 0;
}

.quick-reply-chip {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid #FCD34D;
  background: #FFFBEB;
  color: #92400E;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.quick-reply-chip:hover {
  background: #FEF3C7;
  border-color: #F59E0B;
  transform: translateY(-1px);
}

/* ===== Input ===== */
.chat-input-area {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-top: 1px solid #F3F4F6;
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  padding: 8px 14px;
  border: 1px solid #E5E7EB;
  border-radius: 999px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s ease;
  color: #374151;
  background: #F9FAFB;
}

.chat-input:focus {
  border-color: #F59E0B;
  background: white;
}

.chat-input::placeholder {
  color: #9CA3AF;
}

.chat-send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #F59E0B, #D97706);
  color: white;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.chat-send-btn:hover:not(:disabled) {
  transform: scale(1.06);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.35);
}

.chat-send-btn:active:not(:disabled) {
  transform: scale(0.95);
}
</style>
