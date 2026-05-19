<template>
  <MotionModal
    :show="show"
    modal-key="payment-modal"
    :close-on-backdrop="true"
    panel-class="max-w-sm rounded-2xl bg-white p-0 overflow-hidden"
    @close="$emit('close')"
  >
    <div class="relative">
      <!-- Header -->
      <div class="bg-gradient-to-r from-red-600 to-red-700 px-6 py-4">
        <h3 class="text-white text-lg font-bold text-center">{{ $t('payment.title') }}</h3>
      </div>

      <!-- QR Code -->
      <div class="px-6 py-6 flex flex-col items-center">
        <div class="bg-gray-50 rounded-xl p-3 border border-gray-100">
          <img
            src="/images/payment-qr.jpg"
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

        <!-- Action buttons -->
        <div class="mt-6 w-full space-y-3">
          <button
            @click="handlePaid"
            class="w-full py-3 rounded-xl bg-tibet-red text-white font-bold hover:bg-red-700 transition-colors"
          >
            {{ $t('payment.confirmPaid') }}
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
import MotionModal from './motion/MotionModal.vue'

defineProps<{
  show: boolean
  amount?: number | string | null
}>()

const emit = defineEmits<{
  close: []
  paid: []
}>()

const handlePaid = () => {
  emit('paid')
}
</script>
