<template>
  <div class="min-h-screen tibet-page-shell">
    <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div class="text-center mb-12">
        <h1 class="tibet-heading inline-flex justify-center text-4xl md:text-5xl font-bold text-tibet-dark mb-4">
          {{ t('terms.title') }}
        </h1>
        <p class="text-tibet-brown/50 text-sm">{{ t('terms.lastUpdated') }}</p>
      </div>

      <div class="tibet-panel legal-panel rounded-2xl p-8 md:p-12">
        <article class="legal-markdown" v-html="renderedContent"></article>
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
import { renderMarkdownToSafeHtml } from '../utils/sanitize'
import { getLegalMarkdown } from '../content/legal'

const { t, locale } = useI18n()

const renderedContent = computed(() => {
  const markdown = getLegalMarkdown('terms', locale.value)
  return renderMarkdownToSafeHtml(markdown)
})
</script>

<style scoped>
.legal-panel {
  background: rgba(255, 255, 255, 0.92);
}

.legal-markdown :deep(h2) {
  align-items: center;
  color: #111827;
  display: flex;
  font-size: 1.5rem;
  font-weight: 700;
  gap: 0.75rem;
  line-height: 1.3;
  margin: 2rem 0 1rem;
}

.legal-markdown :deep(h2:first-child) {
  margin-top: 0;
}

.legal-markdown :deep(h2::before) {
  background: linear-gradient(180deg, #d6a84f, #b91c1c);
  border-radius: 999px;
  content: '';
  display: inline-block;
  flex: 0 0 auto;
  height: 2rem;
  width: 0.25rem;
}

.legal-markdown :deep(p),
.legal-markdown :deep(li) {
  color: #374151;
  font-size: 1rem;
  line-height: 1.85;
}

.legal-markdown :deep(ul),
.legal-markdown :deep(ol) {
  list-style-position: inside;
  margin: 0.75rem 0 1.25rem;
}

.legal-markdown :deep(a),
.legal-markdown :deep(code) {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.legal-markdown :deep(a) {
  color: #2563eb;
  text-decoration: underline;
  text-underline-offset: 0.18em;
}

.legal-markdown :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
}
</style>
