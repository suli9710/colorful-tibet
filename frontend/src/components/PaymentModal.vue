<template>
  <MotionModal
    :show="show"
    modal-key="payment-modal"
    labelled-by="payment-modal-title"
    :close-on-backdrop="true"
    panel-class="max-w-sm rounded-2xl bg-white p-0 overflow-hidden"
    @close="$emit('close')"
  >
    <div class="relative">
      <!-- Header -->
      <div class="bg-gradient-to-r from-red-600 to-red-700 px-6 py-4">
        <h3 id="payment-modal-title" class="text-white text-lg font-bold text-center">{{ $t('payment.operationTitle') }}</h3>
      </div>

      <!-- QR Code -->
      <div class="px-6 py-6 flex flex-col items-center">
        <div class="bg-gray-50 rounded-xl p-3 border border-gray-100">
          <img
            :src="paymentQrSrc"
            :alt="$t('payment.qrAlt')"
            class="w-56 h-56 object-contain rounded-lg"
          />
        </div>

        <p class="mt-4 text-sm text-gray-600 text-center">
          {{ $t('payment.scanHint') }}
        </p>

        <!-- Amount display -->
        <div v-if="amount" class="mt-3 bg-amber-50 rounded-lg px-4 py-2 border border-amber-200">
          <span class="text-sm text-amber-700">{{ $t('payment.payAmount') }}</span>
          <span class="text-2xl font-bold text-red-600 ml-2">¥{{ amount }}</span>
        </div>

        <div v-if="requiresCaptcha" class="mt-4 flex w-full justify-center">
          <div ref="captchaContainer" class="min-h-[78px]" />
        </div>

        <p v-if="captchaError" class="mt-3 text-center text-sm text-red-600">
          {{ $t('security.recaptchaFailed') }}
        </p>

        <!-- Action buttons -->
        <div class="mt-6 w-full space-y-3">
          <button
            @click="handleStatusCheck"
            :disabled="confirmDisabled"
            class="w-full py-3 rounded-xl bg-tibet-red text-white font-bold hover:bg-red-700 transition-colors disabled:cursor-not-allowed disabled:opacity-50"
          >
            {{ $t('payment.confirmOperationComplete') }}
          </button>
          <button
            @click="$emit('close')"
            class="w-full py-3 rounded-xl bg-gray-100 text-gray-600 font-medium hover:bg-gray-200 transition-colors"
          >
            {{ $t('payment.cancel') }}
          </button>
        </div>
      </div>
    </div>
  </MotionModal>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import MotionModal from './motion/MotionModal.vue'
import {
  getRecaptchaToken,
  getRecaptchaWidgetResponse,
  isRecaptchaV2Enabled,
  isRecaptchaV3Enabled,
  renderRecaptchaCheckbox,
  resetRecaptchaWidget
} from '../utils/recaptcha'

const props = defineProps<{
  show: boolean
  amount?: number | string | null
  recaptchaAction?: string
}>()

const emit = defineEmits<{
  close: []
  'status-check': [recaptchaToken?: string]
}>()

const paymentQrSrc = '/images/payment-qr.jpg'
const captchaContainer = ref<HTMLElement | null>(null)
const captchaToken = ref('')
const captchaError = ref(false)
const captchaLoading = ref(false)
const widgetId = ref<number | null>(null)

const hasRecaptchaAction = computed(() => Boolean(props.recaptchaAction))
const requiresCaptcha = computed(() => hasRecaptchaAction.value && isRecaptchaV2Enabled())
const confirmDisabled = computed(() => captchaLoading.value || (requiresCaptcha.value && !captchaToken.value))

const resetCaptchaState = () => {
  resetRecaptchaWidget(widgetId.value)
  captchaToken.value = ''
  captchaError.value = false
  captchaLoading.value = false
  widgetId.value = null
}

const renderCaptcha = async () => {
  if (!props.show || !requiresCaptcha.value || !captchaContainer.value) return

  captchaLoading.value = true
  captchaError.value = false
  captchaToken.value = ''

  try {
    widgetId.value = await renderRecaptchaCheckbox(captchaContainer.value, {
      onVerify: token => {
        captchaToken.value = token
        captchaError.value = false
      },
      onExpired: () => {
        captchaToken.value = ''
      },
      onError: () => {
        captchaToken.value = ''
        captchaError.value = true
      }
    })
  } catch {
    captchaError.value = true
  } finally {
    captchaLoading.value = false
  }
}

watch(
  () => [props.show, props.recaptchaAction] as const,
  async ([show]) => {
    if (!show) {
      resetCaptchaState()
      return
    }
    await nextTick()
    await renderCaptcha()
  }
)

const handleStatusCheck = async () => {
  captchaError.value = false

  if (requiresCaptcha.value) {
    const token = captchaToken.value || getRecaptchaWidgetResponse(widgetId.value)
    if (!token) {
      captchaError.value = true
      return
    }
    emit('status-check', token)
    return
  }

  if (hasRecaptchaAction.value && isRecaptchaV3Enabled()) {
    captchaLoading.value = true
    try {
      const token = await getRecaptchaToken(props.recaptchaAction || '')
      emit('status-check', token)
    } catch {
      captchaError.value = true
    } finally {
      captchaLoading.value = false
    }
    return
  }

  emit('status-check')
}
</script>
