<template>
  <div class="min-h-screen bg-apple-gray-50 relative overflow-hidden flex items-center justify-center py-20">
    <!-- Liquid Background Animation -->
    <div class="absolute top-0 left-0 w-full h-full overflow-hidden z-0">
      <div class="absolute top-0 left-1/4 w-96 h-96 bg-purple-300 rounded-full mix-blend-multiply filter blur-xl opacity-70 animate-blob"></div>
      <div class="absolute top-0 right-1/4 w-96 h-96 bg-yellow-300 rounded-full mix-blend-multiply filter blur-xl opacity-70 animate-blob animation-delay-2000"></div>
      <div class="absolute -bottom-32 left-1/3 w-96 h-96 bg-pink-300 rounded-full mix-blend-multiply filter blur-xl opacity-70 animate-blob animation-delay-4000"></div>
    </div>

    <div class="relative z-10 w-full max-w-4xl px-4">
      <div class="glass rounded-3xl p-8 md:p-12 animate-scale-in">
        <div class="text-center mb-10">
          <h2 class="text-4xl font-bold text-apple-gray-900 mb-4">{{ t('contact.title') }}</h2>
          <p class="text-lg text-apple-gray-600">{{ t('contact.subtitle') }}</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-12">
          <!-- Contact Info -->
          <div class="space-y-8">
            <div class="flex items-start space-x-4">
              <div class="bg-blue-100 p-3 rounded-2xl text-blue-600">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
              </div>
              <div>
                <h3 class="text-lg font-semibold text-apple-gray-900">{{ t('contact.phone') }}</h3>
                <p class="text-apple-gray-500 mt-1">+86 891 1234 5678</p>
                <p class="text-sm text-apple-gray-400 mt-1">{{ t('contact.phoneHours') }}</p>
              </div>
            </div>

            <div class="flex items-start space-x-4">
              <div class="bg-purple-100 p-3 rounded-2xl text-purple-600">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <div>
                <h3 class="text-lg font-semibold text-apple-gray-900">{{ t('contact.email') }}</h3>
                <p class="text-apple-gray-500 mt-1">support@tibet-tourism.com</p>
                <p class="text-sm text-apple-gray-400 mt-1">{{ t('contact.emailResponse') }}</p>
              </div>
            </div>

            <div class="flex items-start space-x-4">
              <div class="bg-green-100 p-3 rounded-2xl text-green-600">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </div>
              <div>
                <h3 class="text-lg font-semibold text-apple-gray-900">{{ t('contact.address') }}</h3>
                <p class="text-apple-gray-500 mt-1">{{ t('contact.addressLine1') }}</p>
                <p class="text-apple-gray-500">{{ t('contact.addressLine2') }}</p>
              </div>
            </div>
          </div>

          <!-- Contact Form -->
          <form @submit.prevent="submitForm" class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">{{ t('contact.name') }}</label>
              <input type="text" class="w-full px-4 py-3 rounded-xl bg-white/50 border border-white/30 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 outline-none transition-all backdrop-blur-sm" :placeholder="t('contact.namePlaceholder')" required>
            </div>
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">{{ t('contact.emailLabel') }}</label>
              <input type="email" class="w-full px-4 py-3 rounded-xl bg-white/50 border border-white/30 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 outline-none transition-all backdrop-blur-sm" placeholder="your@email.com" required>
            </div>
            <div>
              <label class="block text-sm font-medium text-apple-gray-700 mb-2">{{ t('contact.message') }}</label>
              <textarea rows="4" class="w-full px-4 py-3 rounded-xl bg-white/50 border border-white/30 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 outline-none transition-all backdrop-blur-sm resize-none" :placeholder="t('contact.messagePlaceholder')" required></textarea>
            </div>
            <button type="submit" class="w-full bg-apple-blue hover:bg-apple-blue-hover text-white font-medium py-3 px-6 rounded-xl transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] shadow-lg hover:shadow-blue-500/30">
              {{ t('contact.sendMessage') }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const submitForm = () => {
  alert(t('contact.messageSent'))
}
</script>

<style scoped>
.animation-delay-2000 {
  animation-delay: 2s;
}
.animation-delay-4000 {
  animation-delay: 4s;
}
</style>
