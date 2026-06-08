<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { AnimatePresence, motion } from 'motion-v'
import { softSpring } from '../motion/presets'
import { useRouteGenerationStore } from '../stores/routeGeneration'
import {
  type ChatMessage,
  type GuideChatHistoryItem,
  MAX_GUIDE_CHAT_MESSAGE_CHARS,
  createNetworkFallbackGuideMessage,
  createPendingGuideMessage,
  createUserMessage,
  getGreeting,
  normalizeGuideChatInput,
  quickReplies,
  requestGuideChat,
  summarizeGuideChatError
} from '../services/guideChat'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const router = useRouter()
const { t } = useI18n()
const generationStore = useRouteGenerationStore()

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const inputField = ref<HTMLTextAreaElement | null>(null)
const isSending = ref(false)
const cooldownRemaining = ref(0)

const SEND_COOLDOWN_SECONDS = 6
const MAX_UI_COOLDOWN_SECONDS = 300
const inputLimit = MAX_GUIDE_CHAT_MESSAGE_CHARS
const panelTitleId = 'guide-chat-title'
const privacyNoteId = 'guide-chat-privacy-note'
const cooldownId = 'guide-chat-cooldown'
const inputLimitId = 'guide-chat-input-limit'
const messagesLogId = 'guide-chat-messages'
let cooldownTimer: number | undefined

const normalizedInput = computed(() => normalizeGuideChatInput(inputText.value))
const inputLength = computed(() => Array.from(inputText.value).length)
const inputNearLimit = computed(() => inputLength.value >= inputLimit - 30)
const canSend = computed(() => Boolean(normalizedInput.value) && !isSending.value && cooldownRemaining.value <= 0)
const interactionLocked = computed(() => isSending.value || cooldownRemaining.value > 0)
const sendButtonLabel = computed(() => {
  if (isSending.value) return t('common.submitting')
  if (cooldownRemaining.value > 0) return t('guideChat.cooldown', { seconds: cooldownRemaining.value })
  return t('guideChat.send')
})
const inputDescribedBy = computed(() => [
  privacyNoteId,
  cooldownRemaining.value > 0 ? cooldownId : '',
  inputNearLimit.value ? inputLimitId : ''
].filter(Boolean).join(' '))

function scrollToBottom() {
  nextTick(() => {
    const el = messagesContainer.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function clearCooldownTimer() {
  if (cooldownTimer !== undefined) {
    window.clearInterval(cooldownTimer)
    cooldownTimer = undefined
  }
}

function startCooldown(seconds = SEND_COOLDOWN_SECONDS) {
  const safeSeconds = Number.isFinite(seconds) ? seconds : SEND_COOLDOWN_SECONDS
  const nextSeconds = Math.max(SEND_COOLDOWN_SECONDS, Math.min(Math.ceil(safeSeconds), MAX_UI_COOLDOWN_SECONDS))
  cooldownRemaining.value = nextSeconds
  clearCooldownTimer()
  cooldownTimer = window.setInterval(() => {
    cooldownRemaining.value = Math.max(0, cooldownRemaining.value - 1)
    if (cooldownRemaining.value <= 0) {
      clearCooldownTimer()
    }
  }, 1000)
}

onBeforeUnmount(clearCooldownTimer)

watch(() => props.visible, (v) => {
  if (!v) return

  if (messages.value.length === 0) {
    messages.value = [getGreeting()]
    scrollToBottom()
  }

  nextTick(() => inputField.value?.focus())
})

function replaceMessage(id: number, nextMessage: ChatMessage) {
  const index = messages.value.findIndex(msg => msg.id === id)
  if (index >= 0) {
    messages.value[index] = nextMessage
  }
}

function buildHistory(): GuideChatHistoryItem[] {
  return messages.value
    .filter(msg => !msg.pending)
    .slice(-8)
    .map(msg => ({
      role: msg.role,
      content: normalizeGuideChatInput(msg.text)
    }))
    .filter(item => item.content)
}

async function sendMessage(text: string) {
  const trimmed = normalizeGuideChatInput(text)
  if (!trimmed || isSending.value || cooldownRemaining.value > 0) return

  const history = buildHistory()
  const userMessage = createUserMessage(trimmed)
  const pendingMessage = createPendingGuideMessage()
  messages.value.push(userMessage, pendingMessage)
  inputText.value = ''
  scrollToBottom()

  isSending.value = true
  try {
    const guideMessage = await requestGuideChat(trimmed, history)
    replaceMessage(pendingMessage.id, guideMessage)
    startCooldown(guideMessage.limited ? guideMessage.retryAfterSeconds : SEND_COOLDOWN_SECONDS)
  } catch (error) {
    if (import.meta.env.DEV) {
      console.warn('AI guide chat failed, using local fallback:', summarizeGuideChatError(error))
    }
    replaceMessage(pendingMessage.id, createNetworkFallbackGuideMessage(trimmed))
    startCooldown(SEND_COOLDOWN_SECONDS)
  } finally {
    isSending.value = false
    scrollToBottom()
  }
}

function handleQuickReply(reply: (typeof quickReplies)[number]) {
  sendMessage(reply.text)
}

function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  const chars = Array.from(target.value)
  const nextValue = chars.length > inputLimit ? chars.slice(0, inputLimit).join('') : target.value
  inputText.value = nextValue
  if (target.value !== nextValue) {
    target.value = nextValue
  }
}

function retryNetworkFallback(msg: ChatMessage) {
  if (!msg.networkFallback || !msg.retryText || interactionLocked.value) return
  sendMessage(msg.retryText)
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
  if (e.isComposing) return

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
      role="dialog"
      :aria-labelledby="panelTitleId"
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
            <p :id="panelTitleId" class="text-sm font-bold text-gray-800 leading-tight">{{ t('guideChat.title') }}</p>
            <p class="text-[10px] text-gray-400 flex items-center gap-1">
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
              {{ t('guideChat.online') }}
            </p>
          </div>
        </div>
        <button
          type="button"
          @click="emit('close')"
          :aria-label="t('guideChat.closePanel')"
          class="flex items-center justify-center w-7 h-7 rounded-full hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Messages -->
      <div
        :id="messagesLogId"
        ref="messagesContainer"
        class="chat-messages"
        role="log"
        aria-live="polite"
        aria-relevant="additions text"
        :aria-busy="isSending"
      >
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
              <div class="chat-bubble-guide" :class="{ 'chat-bubble-pending': msg.pending }">
                <span v-if="msg.pending" class="typing-dots" aria-hidden="true">
                  <span></span>
                  <span></span>
                  <span></span>
                </span>
                <span v-if="msg.pending" class="sr-only" role="status" aria-live="polite">{{ msg.text }}</span>
                <span :aria-hidden="msg.pending ? 'true' : undefined">{{ msg.text }}</span>
                <div v-if="!msg.pending && (msg.limited || msg.fallback || msg.networkFallback || msg.challengeRequired)" class="chat-message-flags">
                  <span v-if="msg.limited && !msg.challengeRequired" class="chat-message-flag">{{ t('guideChat.rateLimited') }}</span>
                  <span v-if="msg.fallback || msg.networkFallback" class="chat-message-flag">{{ t('guideChat.localFallback') }}</span>
                  <span v-if="msg.challengeRequired" class="chat-message-flag">{{ t('guideChat.securityCheck') }}</span>
                </div>
              </div>
              <button
                v-if="msg.networkFallback && msg.retryText && !msg.pending"
                type="button"
                @click="retryNetworkFallback(msg)"
                :disabled="interactionLocked"
                :aria-label="t('guideChat.retryCloud')"
                class="chat-retry-btn"
                :class="{ 'opacity-50 cursor-not-allowed': interactionLocked }"
              >
                {{ t('guideChat.retryCloud') }}
              </button>
              <!-- Action button -->
              <motion.button
                v-if="msg.action && !msg.pending"
                type="button"
                @click="handleAction(msg)"
                class="mt-2 inline-flex items-center gap-1.5 rounded-full bg-gradient-to-r from-tibet-red to-rose-600 px-3.5 py-1.5 text-xs font-semibold text-white shadow-md shadow-tibet-red/20"
                :whileHover="{ scale: 1.04 }"
                :whileTap="{ scale: 0.96 }"
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
                {{ msg.actionLabel || t('guideChat.goNow') }}
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
          type="button"
          @click="handleQuickReply(reply)"
          :disabled="interactionLocked"
          :aria-label="reply.label"
          class="quick-reply-chip"
          :class="{ 'opacity-50 cursor-not-allowed': interactionLocked }"
        >
          {{ reply.label }}
        </button>
      </div>

      <!-- Input area -->
      <div class="chat-input-shell">
        <div class="chat-input-area">
          <textarea
            ref="inputField"
            v-model="inputText"
            @input="handleInput"
            @keydown="handleKeydown"
            :disabled="isSending"
            :maxlength="inputLimit"
            :aria-label="t('guideChat.placeholder')"
            :aria-describedby="inputDescribedBy"
            rows="1"
            enterkeyhint="send"
            inputmode="text"
            :placeholder="t('guideChat.placeholder')"
            class="chat-input"
          ></textarea>
          <button
            type="button"
            @click="sendMessage(inputText)"
            :disabled="!canSend"
            :aria-label="sendButtonLabel"
            :aria-controls="messagesLogId"
            class="chat-send-btn"
            :class="{ 'opacity-30 cursor-not-allowed': !canSend }"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </button>
        </div>
        <p :id="privacyNoteId" class="chat-privacy-note">
          {{ t('guideChat.privacyNote') }}
        </p>
        <p v-if="inputNearLimit" :id="inputLimitId" role="status" aria-live="polite" class="chat-input-limit">
          {{ inputLength }}/{{ inputLimit }}
        </p>
        <p v-if="cooldownRemaining > 0" :id="cooldownId" role="status" aria-live="polite" class="chat-cooldown">
          {{ t('guideChat.cooldown', { seconds: cooldownRemaining }) }}
        </p>
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
    padding: 10px 12px 6px;
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

.chat-bubble-pending {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #6B7280;
}

.chat-message-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.chat-message-flag {
  border-radius: 999px;
  border: 1px solid #FCD34D;
  background: #FFFBEB;
  color: #92400E;
  padding: 2px 7px;
  font-size: 10px;
  font-weight: 700;
  line-height: 1.4;
}

.chat-retry-btn {
  margin-top: 6px;
  border-radius: 999px;
  border: 1px solid #BFDBFE;
  background: #EFF6FF;
  color: #1D4ED8;
  padding: 5px 10px;
  font-size: 11px;
  font-weight: 700;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.chat-retry-btn:hover:not(:disabled) {
  border-color: #60A5FA;
  background: #DBEAFE;
}

.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.typing-dots span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #D97706;
  animation: typing-dot 1s ease-in-out infinite;
}

.typing-dots span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dots span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-dot {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
  40% { transform: translateY(-3px); opacity: 1; }
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

.quick-reply-chip:disabled {
  transform: none;
}

/* ===== Input ===== */
.chat-input-shell {
  border-top: 1px solid #F3F4F6;
  flex-shrink: 0;
}

.chat-input-area {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px 6px;
}

.chat-cooldown {
  min-height: 18px;
  padding: 0 18px 8px;
  font-size: 11px;
  color: #92400E;
}

.chat-privacy-note,
.chat-input-limit {
  padding: 0 18px;
  font-size: 10.5px;
  line-height: 1.45;
}

.chat-privacy-note {
  color: #6B7280;
}

.chat-input-limit {
  padding-top: 2px;
  color: #B45309;
  text-align: right;
}

.chat-input {
  flex: 1;
  min-height: 36px;
  max-height: 86px;
  padding: 8px 14px;
  border: 1px solid #E5E7EB;
  border-radius: 18px;
  font-size: 13px;
  line-height: 1.45;
  outline: none;
  resize: none;
  transition: border-color 0.2s ease;
  color: #374151;
  background: #F9FAFB;
  overflow-y: auto;
}

.chat-input:focus {
  border-color: #F59E0B;
  background: white;
}

.chat-input:disabled {
  cursor: not-allowed;
  color: #9CA3AF;
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
