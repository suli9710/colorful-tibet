<template>
  <div class="min-h-screen tibet-page-shell relative overflow-hidden flex items-center justify-center py-20">
    <div class="absolute inset-0 z-0 opacity-50 tibet-cloud-pattern"></div>

    <div class="relative z-10 w-full max-w-4xl px-4">
      <div class="tibet-panel rounded-3xl p-8 md:p-12 animate-scale-in">
        <div class="text-center mb-10">
          <h2 class="tibet-heading inline-flex justify-center text-4xl font-bold text-tibet-dark mb-4">{{ t('contact.title') }}</h2>
          <p class="text-lg text-tibet-brown/80">{{ t('contact.subtitle') }}</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-12">
          <!-- Contact Info -->
          <div class="space-y-8">
            <div class="flex items-start space-x-4">
              <div class="bg-tibet-blue/10 p-3 rounded-2xl text-tibet-blue border border-tibet-blue/15">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
              </div>
              <div>
                <h3 class="text-lg font-semibold text-tibet-dark">{{ t('contact.phone') }}</h3>
                <p class="text-tibet-brown/70 mt-1">+86 891 1234 5678</p>
                <p class="text-sm text-tibet-brown/50 mt-1">{{ t('contact.phoneHours') }}</p>
              </div>
            </div>

            <div class="flex items-start space-x-4">
              <div class="bg-tibet-red/10 p-3 rounded-2xl text-tibet-red border border-tibet-red/15">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <div>
                <h3 class="text-lg font-semibold text-tibet-dark">{{ t('contact.email') }}</h3>
                <p class="text-tibet-brown/70 mt-1">support@tibet-tourism.com</p>
                <p class="text-sm text-tibet-brown/50 mt-1">{{ t('contact.emailResponse') }}</p>
              </div>
            </div>

            <div class="flex items-start space-x-4">
              <div class="bg-tibet-turquoise/10 p-3 rounded-2xl text-tibet-turquoise border border-tibet-turquoise/15">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </div>
              <div>
                <h3 class="text-lg font-semibold text-tibet-dark">{{ t('contact.address') }}</h3>
                <p class="text-tibet-brown/70 mt-1">{{ t('contact.addressLine1') }}</p>
                <p class="text-tibet-brown/70">{{ t('contact.addressLine2') }}</p>
              </div>
            </div>
          </div>

          <!-- Contact Form -->
          <form @submit.prevent="submitForm" class="space-y-6">
            <div>
              <label class="block text-sm font-medium text-tibet-dark/80 mb-2">{{ t('contact.name') }}</label>
              <input type="text" class="w-full px-4 py-3 rounded-xl bg-white/60 border border-tibet-gold/25 focus:border-tibet-gold focus:ring-2 focus:ring-tibet-gold/20 outline-none transition-all backdrop-blur-sm" :placeholder="t('contact.namePlaceholder')" required>
            </div>
            <div>
              <label class="block text-sm font-medium text-tibet-dark/80 mb-2">{{ t('contact.emailLabel') }}</label>
              <input type="email" class="w-full px-4 py-3 rounded-xl bg-white/60 border border-tibet-gold/25 focus:border-tibet-gold focus:ring-2 focus:ring-tibet-gold/20 outline-none transition-all backdrop-blur-sm" placeholder="your@email.com" required>
            </div>
            <div>
              <label class="block text-sm font-medium text-tibet-dark/80 mb-2">{{ t('contact.message') }}</label>
              <textarea rows="4" class="w-full px-4 py-3 rounded-xl bg-white/60 border border-tibet-gold/25 focus:border-tibet-gold focus:ring-2 focus:ring-tibet-gold/20 outline-none transition-all backdrop-blur-sm resize-none" :placeholder="t('contact.messagePlaceholder')" required></textarea>
            </div>
            <button type="submit" class="w-full bg-tibet-red hover:bg-tibet-red/90 text-tibet-yellow font-medium py-3 px-6 rounded-xl transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] shadow-lg hover:shadow-tibet-red/20">
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
