<template>
  <!-- Modal Backdrop with blur -->
  <Transition name="modal-fade">
    <div v-if="modelValue" 
         @click="close"
         class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-md">
      
      <!-- Modal Card with Apple Glass Effect -->
      <Transition name="modal-scale">
        <div v-if="modelValue"
             @click.stop
             class="relative w-full max-w-lg overflow-hidden rounded-3xl shadow-2xl animate-float">
          
          <!-- Glassmorphism Background -->
          <div class="absolute inset-0 bg-gradient-to-br from-white/90 via-white/80 to-white/70 backdrop-blur-2xl"></div>
          
          <!-- Animated gradient overlay -->
          <div class="absolute inset-0 bg-gradient-to-br from-blue-500/10 via-purple-500/10 to-pink-500/10 animate-gradient"></div>
          
          <!-- Content -->
          <div class="relative z-10 p-8">
            <!-- Close Button -->
            <button @click="close" 
                    class="absolute top-4 right-4 w-8 h-8 flex items-center justify-center rounded-full bg-gray-200/50 hover:bg-gray-300/70 backdrop-blur-sm transition-all duration-200 active:scale-90 group">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-600 group-hover:text-gray-800 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>

            <!-- Header -->
            <div class="text-center mb-8">
              <div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 mb-4 shadow-lg animate-pulse-slow">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <h2 class="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 mb-2">
                联系我们
              </h2>
              <p class="text-sm text-gray-600">我们随时为您服务</p>
            </div>

            <!-- Contact Cards -->
            <div class="space-y-4">
              <!-- Phone -->
              <div class="group relative overflow-hidden rounded-2xl bg-white/40 backdrop-blur-sm border border-white/50 p-5 shadow-sm hover:shadow-lg hover:scale-[1.02] transition-all duration-300">
                <div class="absolute inset-0 bg-gradient-to-r from-blue-500/0 via-blue-500/5 to-blue-500/0 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                <div class="relative flex items-start space-x-4">
                  <div class="flex-shrink-0">
                    <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center shadow-md group-hover:scale-110 transition-transform duration-300">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                      </svg>
                    </div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="text-sm font-medium text-gray-500 mb-1">电话咨询</h3>
                    <p class="text-lg font-semibold text-gray-900 mb-1">19532458802</p>
                    <p class="text-xs text-gray-500">周一至周五 9:00 - 18:00</p>
                  </div>
                  <button @click="copyToClipboard('19532458802')" 
                          class="flex-shrink-0 w-8 h-8 rounded-lg bg-gray-100/50 hover:bg-gray-200/70 flex items-center justify-center transition-all duration-200 active:scale-90">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                  </button>
                </div>
              </div>

              <!-- Email -->
              <div class="group relative overflow-hidden rounded-2xl bg-white/40 backdrop-blur-sm border border-white/50 p-5 shadow-sm hover:shadow-lg hover:scale-[1.02] transition-all duration-300">
                <div class="absolute inset-0 bg-gradient-to-r from-purple-500/0 via-purple-500/5 to-purple-500/0 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                <div class="relative flex items-start space-x-4">
                  <div class="flex-shrink-0">
                    <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-400 to-purple-600 flex items-center justify-center shadow-md group-hover:scale-110 transition-transform duration-300">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                      </svg>
                    </div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="text-sm font-medium text-gray-500 mb-1">邮箱联系</h3>
                    <p class="text-lg font-semibold text-gray-900 mb-1 break-all">lengzhehao@gmail.com</p>
                    <p class="text-xs text-gray-500">通常在 24 小时内回复</p>
                  </div>
                  <button @click="copyToClipboard('lengzhehao@gmail.com')" 
                          class="flex-shrink-0 w-8 h-8 rounded-lg bg-gray-100/50 hover:bg-gray-200/70 flex items-center justify-center transition-all duration-200 active:scale-90">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                  </button>
                </div>
              </div>

              <!-- Address -->
              <div class="group relative overflow-hidden rounded-2xl bg-white/40 backdrop-blur-sm border border-white/50 p-5 shadow-sm hover:shadow-lg hover:scale-[1.02] transition-all duration-300">
                <div class="absolute inset-0 bg-gradient-to-r from-green-500/0 via-green-500/5 to-green-500/0 opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
                <div class="relative flex items-start space-x-4">
                  <div class="flex-shrink-0">
                    <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center shadow-md group-hover:scale-110 transition-transform duration-300">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </div>
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="text-sm font-medium text-gray-500 mb-1">公司地址</h3>
                    <p class="text-lg font-semibold text-gray-900 mb-1">湖南省长沙市长沙县</p>
                    <p class="text-xs text-gray-500">湖南信息学院</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Footer -->
            <div class="mt-6 pt-6 border-t border-gray-200/50 text-center">
              <p class="text-xs text-gray-500">期待为您服务 ❤️</p>
            </div>
          </div>

          <!-- Decorative Elements -->
          <div class="absolute top-0 right-0 w-40 h-40 bg-gradient-to-br from-blue-400/20 to-transparent rounded-full blur-3xl"></div>
          <div class="absolute bottom-0 left-0 w-40 h-40 bg-gradient-to-tr from-purple-400/20 to-transparent rounded-full blur-3xl"></div>
        </div>
      </Transition>
    </div>
  </Transition>

  <!-- Toast Notification -->
  <Transition name="toast">
    <div v-if="showToast" 
         class="fixed bottom-8 right-8 z-[60] bg-gray-900/90 backdrop-blur-xl text-white px-6 py-3 rounded-2xl shadow-2xl flex items-center space-x-3">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
      </svg>
      <span class="text-sm font-medium">已复制到剪贴板</span>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const showToast = ref(false)

const close = () => {
  emit('update:modelValue', false)
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    showToast.value = true
    setTimeout(() => {
      showToast.value = false
    }, 2000)
  } catch (err) {
    console.error('Failed to copy:', err)
  }
}
</script>

<style scoped>
/* Modal Transitions */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-scale-enter-active,
.modal-scale-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-scale-enter-from,
.modal-scale-leave-to {
  opacity: 0;
  transform: scale(0.9) translateY(20px);
}

/* Toast Transition */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* Animations */
@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-5px);
  }
}

@keyframes gradient {
  0% {
    opacity: 0.5;
  }
  50% {
    opacity: 0.8;
  }
  100% {
    opacity: 0.5;
  }
}

@keyframes pulse-slow {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.animate-float {
  animation: float 3s ease-in-out infinite;
}

.animate-gradient {
  animation: gradient 3s ease-in-out infinite;
}

.animate-pulse-slow {
  animation: pulse-slow 2s ease-in-out infinite;
}
</style>



