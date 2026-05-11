<template>
  <div class="min-h-screen tibet-page-shell">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div class="text-center mb-12">
        <h1 class="tibet-heading inline-flex justify-center text-4xl md:text-5xl font-bold text-tibet-dark mb-4">
          {{ t('privacy.title') }}
        </h1>
        <p class="text-tibet-brown/50 text-sm">{{ t('privacy.lastUpdated') }}</p>
      </div>

      <div class="tibet-panel rounded-2xl p-8 md:p-12 space-y-8">
        <section
          v-for="section in sections"
          :key="section.title"
          :class="section.contact ? 'bg-gradient-to-r from-blue-50 to-purple-50 rounded-xl p-6 border border-blue-100' : ''"
        >
          <h2 class="text-2xl font-semibold text-gray-900 mb-4 flex items-center">
            <span class="w-1 h-8 bg-gradient-to-b from-tibet-gold to-tibet-red rounded-full mr-3"></span>
            {{ section.title }}
          </h2>

          <div class="space-y-4 text-gray-700">
            <p v-for="paragraph in section.paragraphs || []" :key="paragraph" class="leading-relaxed">
              {{ paragraph }}
            </p>

            <div v-for="block in section.blocks || []" :key="block.title">
              <h3 class="font-semibold text-gray-900 mb-2">{{ block.title }}</h3>
              <p class="leading-relaxed">{{ block.text }}</p>
            </div>

            <ul v-if="section.list?.length" class="space-y-3 list-disc list-inside">
              <li v-for="item in section.list" :key="item">{{ item }}</li>
            </ul>

            <div v-if="section.contact" class="space-y-2">
              <p><strong>{{ t('contact.email') }}：</strong>lengzhehao@gmail.com</p>
              <p><strong>{{ t('contact.phone') }}：</strong>19532458802</p>
              <p><strong>{{ t('contact.address') }}：</strong>{{ t('footer.addressLine1') }}{{ t('footer.addressLine2') }}</p>
            </div>
          </div>
        </section>
      </div>

      <div class="text-center mt-8">
        <router-link
          to="/"
          class="inline-flex items-center px-6 py-3 bg-gradient-to-r from-tibet-gold to-tibet-red text-white rounded-full font-semibold hover:from-blue-600 hover:to-purple-600 transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-xl"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          {{ t('terms.backToHome') }}
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

interface LegalBlock {
  title: string
  text: string
}

interface LegalSection {
  title: string
  paragraphs?: string[]
  blocks?: LegalBlock[]
  list?: string[]
  contact?: boolean
}

const { t, tm } = useI18n()

const sections = computed(() => tm('privacy.sections') as LegalSection[])
</script>
