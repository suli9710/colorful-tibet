<template>
  <section class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
    <div
      class="px-6 py-4 border-b border-stone-100 flex justify-between items-center bg-gradient-to-r from-stone-50 to-white cursor-pointer hover:bg-stone-100/50 transition-colors"
      @click="showPanel = !showPanel"
    >
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-lg bg-rose-100 flex items-center justify-center">
          <MessagesSquare class="w-5 h-5 text-rose-600" />
        </div>
        <h3 class="text-lg font-bold text-stone-800">
          {{ t('admin.communityManagement') }}
          <span class="text-sm font-normal text-stone-400">({{ totalCommunityItems }}{{ t('common.items') }})</span>
        </h3>
      </div>
      <div class="flex items-center gap-2">
        <button
          @click.stop="fetchCommunityContent"
          :disabled="loadingCommunity"
          class="inline-flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg bg-stone-100 text-stone-600 hover:bg-stone-200 transition-colors disabled:opacity-60"
        >
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': loadingCommunity }" />
          {{ loadingCommunity ? t('common.loading') : t('common.refresh') }}
        </button>
        <ChevronDown class="w-5 h-5 text-stone-400 transition-transform duration-200" :class="{ 'rotate-180': showPanel }" />
      </div>
    </div>

    <div v-if="showPanel" class="p-6 space-y-5">
      <div class="flex flex-wrap gap-2">
        <button
          v-for="tab in communityTabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          class="inline-flex items-center gap-2 px-3 py-2 rounded-lg text-sm border transition-colors"
          :class="activeTab === tab.key ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-stone-200 bg-white text-stone-600 hover:bg-stone-50'"
        >
          <component :is="tab.icon" class="w-4 h-4" />
          <span>{{ tab.label }}</span>
          <span class="rounded-full bg-white/80 px-1.5 py-0.5 text-xs">{{ tab.count }}</span>
        </button>
      </div>

      <div v-if="loadingCommunity" class="py-10 text-center text-stone-500">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-rose-500 mx-auto mb-3"></div>
        {{ t('admin.loadingCommunity') }}
      </div>

      <div v-else-if="communityError" class="rounded-lg border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
        {{ communityError }}
      </div>

      <div v-else class="overflow-x-auto">
        <table v-if="activeTab === 'routes'" class="min-w-full divide-y divide-stone-200">
          <thead class="bg-stone-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.titleLabel') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.communityAuthor') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.communityMeta') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.content') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.createdAt') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-stone-500 uppercase">{{ t('admin.action') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200">
            <tr v-for="route in communityRoutes" :key="route.id" class="hover:bg-stone-50">
              <td class="px-4 py-3 text-sm font-semibold text-stone-800 max-w-[220px]">
                <span class="block truncate">{{ route.title }}</span>
                <span class="text-xs font-normal text-stone-400">ID: {{ route.id }}</span>
              </td>
              <td class="px-4 py-3 text-sm text-stone-600">{{ authorName(route.author) }}</td>
              <td class="px-4 py-3 text-sm text-stone-500">
                <div>{{ t('admin.daysValue', { count: route.days || 0 }) }} · {{ route.budget || '-' }} · {{ route.preference || '-' }}</div>
                <div class="text-xs text-stone-400">{{ t('admin.communityCounts', { views: route.viewCount || 0, likes: route.likeCount || 0, comments: route.commentCount || 0 }) }}</div>
              </td>
              <td class="px-4 py-3 text-sm text-stone-600 max-w-[320px]">{{ contentPreview(route.content) }}</td>
              <td class="px-4 py-3 text-sm text-stone-500 whitespace-nowrap">{{ formatDateTime(route.createdAt) }}</td>
              <td class="px-4 py-3 text-right whitespace-nowrap">
                <button :title="t('common.edit')" @click="openEditItem('route', route)" class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50">
                  <Edit3 class="w-4 h-4" />
                </button>
                <button :title="t('common.delete')" @click="deleteCommunityItem('route', route.id)" class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50">
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
            <tr v-if="communityRoutes.length === 0">
              <td colspan="6" class="px-4 py-8 text-center text-stone-500">{{ t('admin.noCommunityRoutes') }}</td>
            </tr>
          </tbody>
        </table>

        <table v-else-if="activeTab === 'questions'" class="min-w-full divide-y divide-stone-200">
          <thead class="bg-stone-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.titleLabel') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.communityAuthor') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.status') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.content') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.createdAt') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-stone-500 uppercase">{{ t('admin.action') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200">
            <tr v-for="question in communityQuestions" :key="question.id" class="hover:bg-stone-50">
              <td class="px-4 py-3 text-sm font-semibold text-stone-800 max-w-[220px]">
                <span class="block truncate">{{ question.title }}</span>
                <span class="text-xs font-normal text-stone-400">{{ question.tags || '-' }}</span>
              </td>
              <td class="px-4 py-3 text-sm text-stone-600">{{ authorName(question.author) }}</td>
              <td class="px-4 py-3 text-sm">
                <span :class="question.isResolved ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'" class="px-2 py-1 rounded-full text-xs font-medium">
                  {{ question.isResolved ? t('admin.resolved') : t('admin.unresolved') }}
                </span>
                <div class="mt-1 text-xs text-stone-400">{{ t('admin.communityQuestionCounts', { answers: question.answerCount || 0, likes: question.likeCount || 0 }) }}</div>
              </td>
              <td class="px-4 py-3 text-sm text-stone-600 max-w-[320px]">{{ contentPreview(question.content) }}</td>
              <td class="px-4 py-3 text-sm text-stone-500 whitespace-nowrap">{{ formatDateTime(question.createdAt) }}</td>
              <td class="px-4 py-3 text-right whitespace-nowrap">
                <button :title="t('common.edit')" @click="openEditItem('question', question)" class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50">
                  <Edit3 class="w-4 h-4" />
                </button>
                <button :title="t('common.delete')" @click="deleteCommunityItem('question', question.id)" class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50">
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
            <tr v-if="communityQuestions.length === 0">
              <td colspan="6" class="px-4 py-8 text-center text-stone-500">{{ t('admin.noCommunityQuestions') }}</td>
            </tr>
          </tbody>
        </table>

        <table v-else-if="activeTab === 'comments'" class="min-w-full divide-y divide-stone-200">
          <thead class="bg-stone-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.communitySource') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.communityAuthor') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.content') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.createdAt') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-stone-500 uppercase">{{ t('admin.action') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200">
            <tr v-for="comment in communityComments" :key="comment.id" class="hover:bg-stone-50">
              <td class="px-4 py-3 text-sm font-medium text-stone-800 max-w-[260px]">
                <span class="block truncate">{{ comment.routeTitle || '-' }}</span>
                <span class="text-xs font-normal text-stone-400">Route ID: {{ comment.routeId || '-' }}</span>
              </td>
              <td class="px-4 py-3 text-sm text-stone-600">{{ authorName(comment.user) }}</td>
              <td class="px-4 py-3 text-sm text-stone-600 max-w-[360px]">{{ contentPreview(comment.content) }}</td>
              <td class="px-4 py-3 text-sm text-stone-500 whitespace-nowrap">{{ formatDateTime(comment.createdAt) }}</td>
              <td class="px-4 py-3 text-right whitespace-nowrap">
                <button :title="t('common.edit')" @click="openEditItem('comment', comment)" class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50">
                  <Edit3 class="w-4 h-4" />
                </button>
                <button :title="t('common.delete')" @click="deleteCommunityItem('comment', comment.id)" class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50">
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
            <tr v-if="communityComments.length === 0">
              <td colspan="5" class="px-4 py-8 text-center text-stone-500">{{ t('admin.noCommunityComments') }}</td>
            </tr>
          </tbody>
        </table>

        <table v-else-if="activeTab === 'spotComments'" class="min-w-full divide-y divide-stone-200">
          <thead class="bg-stone-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.spotName') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.communityAuthor') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.rating') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.content') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.createdAt') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-stone-500 uppercase">{{ t('admin.action') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200">
            <tr v-for="comment in communitySpotComments" :key="comment.id" class="hover:bg-stone-50">
              <td class="px-4 py-3 text-sm font-medium text-stone-800 max-w-[260px]">
                <span class="block truncate">{{ comment.spotName || '-' }}</span>
                <span class="text-xs font-normal text-stone-400">Spot ID: {{ comment.spotId || '-' }}</span>
              </td>
              <td class="px-4 py-3 text-sm text-stone-600">{{ authorName(comment.user) }}</td>
              <td class="px-4 py-3 text-sm text-stone-600">
                <div>{{ comment.rating || '-' }}</div>
                <div class="text-xs text-stone-400">{{ t('admin.likesCount', { count: comment.likeCount || 0 }) }}</div>
              </td>
              <td class="px-4 py-3 text-sm text-stone-600 max-w-[360px]">{{ contentPreview(comment.content) }}</td>
              <td class="px-4 py-3 text-sm text-stone-500 whitespace-nowrap">{{ formatDateTime(comment.createdAt) }}</td>
              <td class="px-4 py-3 text-right whitespace-nowrap">
                <button :title="t('common.edit')" @click="openEditItem('spotComment', comment)" class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50">
                  <Edit3 class="w-4 h-4" />
                </button>
                <button :title="t('common.delete')" @click="deleteCommunityItem('spotComment', comment.id)" class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50">
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
            <tr v-if="communitySpotComments.length === 0">
              <td colspan="6" class="px-4 py-8 text-center text-stone-500">{{ t('admin.noCommunitySpotComments') }}</td>
            </tr>
          </tbody>
        </table>

        <table v-else class="min-w-full divide-y divide-stone-200">
          <thead class="bg-stone-50">
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.communitySource') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.communityAuthor') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.status') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.content') }}</th>
              <th class="px-4 py-3 text-left text-xs font-medium text-stone-500 uppercase">{{ t('admin.createdAt') }}</th>
              <th class="px-4 py-3 text-right text-xs font-medium text-stone-500 uppercase">{{ t('admin.action') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-stone-200">
            <tr v-for="answer in communityAnswers" :key="answer.id" class="hover:bg-stone-50">
              <td class="px-4 py-3 text-sm font-medium text-stone-800 max-w-[260px]">
                <span class="block truncate">{{ answer.questionTitle || '-' }}</span>
                <span class="text-xs font-normal text-stone-400">Question ID: {{ answer.questionId || '-' }}</span>
              </td>
              <td class="px-4 py-3 text-sm text-stone-600">{{ authorName(answer.user) }}</td>
              <td class="px-4 py-3 text-sm">
                <span :class="answer.isAccepted ? 'bg-emerald-100 text-emerald-700' : 'bg-stone-100 text-stone-600'" class="px-2 py-1 rounded-full text-xs font-medium">
                  {{ answer.isAccepted ? t('admin.accepted') : t('admin.notAccepted') }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-stone-600 max-w-[360px]">{{ contentPreview(answer.content) }}</td>
              <td class="px-4 py-3 text-sm text-stone-500 whitespace-nowrap">{{ formatDateTime(answer.createdAt) }}</td>
              <td class="px-4 py-3 text-right whitespace-nowrap">
                <button :title="t('common.edit')" @click="openEditItem('answer', answer)" class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50">
                  <Edit3 class="w-4 h-4" />
                </button>
                <button :title="t('common.delete')" @click="deleteCommunityItem('answer', answer.id)" class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50">
                  <Trash2 class="w-4 h-4" />
                </button>
              </td>
            </tr>
            <tr v-if="communityAnswers.length === 0">
              <td colspan="6" class="px-4 py-8 text-center text-stone-500">{{ t('admin.noCommunityAnswers') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="totalCommunityItems > 0" class="px-6 py-4 text-center text-stone-500 text-sm">
      {{ t('admin.expandCommunity', { count: totalCommunityItems }) }}
    </div>

    <MotionModal
      :show="showCommunityModal"
      modal-key="admin-community-modal"
      panel-class="max-w-2xl rounded-2xl bg-white p-8 max-h-[85vh] overflow-y-auto"
      @close="closeCommunityModal"
    >
      <form @submit.prevent="saveCommunityItem" class="space-y-5">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wide text-rose-600">{{ typeLabel(editingType) }}</p>
            <h2 class="mt-1 text-2xl font-bold text-stone-800">{{ t('admin.communityEditTitle') }}</h2>
          </div>
          <span class="text-xs text-stone-400">ID: {{ editingItem?.id || '-' }}</span>
        </div>

        <div v-if="editingType === 'route' || editingType === 'question'">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.titleLabel') }}</label>
          <input
            v-model="communityForm.title"
            required
            class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 outline-none"
            :placeholder="t('admin.communityTitlePlaceholder')"
          >
        </div>

        <div>
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.content') }}</label>
          <textarea
            v-model="communityForm.content"
            required
            rows="8"
            class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 outline-none resize-none"
            :placeholder="t('admin.communityContentPlaceholder')"
          ></textarea>
          <p class="text-xs text-stone-500 mt-2">{{ t('admin.currentWordCount', { count: communityForm.content.length }) }}</p>
        </div>

        <div v-if="editingType === 'route'" class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.days') }}</label>
            <input v-model.number="communityForm.days" type="number" min="1" class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 outline-none">
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.budget') }}</label>
            <select v-model="communityForm.budget" class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 outline-none">
              <option value="">{{ t('common.unknown') }}</option>
              <option v-for="option in budgetOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.preference') }}</label>
            <select v-model="communityForm.preference" class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 outline-none">
              <option value="">{{ t('common.unknown') }}</option>
              <option v-for="option in preferenceOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </div>
        </div>

        <div v-if="editingType === 'question'" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.tags') }}</label>
            <input v-model="communityForm.tags" class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 outline-none" :placeholder="t('admin.tagsPlaceholder')">
          </div>
          <label class="inline-flex items-center gap-2 text-sm text-stone-700">
            <input v-model="communityForm.isResolved" type="checkbox" class="rounded border-stone-300 text-rose-600 focus:ring-rose-500">
            {{ t('admin.markResolved') }}
          </label>
        </div>

        <label v-if="editingType === 'answer'" class="inline-flex items-center gap-2 text-sm text-stone-700">
          <input v-model="communityForm.isAccepted" type="checkbox" class="rounded border-stone-300 text-rose-600 focus:ring-rose-500">
          {{ t('admin.markAccepted') }}
        </label>

        <div v-if="editingType === 'spotComment'">
          <label class="block text-sm font-medium text-stone-700 mb-2">{{ t('admin.rating') }}</label>
          <input v-model.number="communityForm.rating" type="number" min="1" max="5" class="w-full px-4 py-3 border border-stone-300 rounded-lg focus:ring-2 focus:ring-rose-500 focus:border-rose-500 outline-none">
        </div>

        <div class="flex gap-4 pt-2">
          <button type="submit" :disabled="savingCommunity" class="flex-1 bg-rose-600 text-white py-3 rounded-lg hover:bg-rose-700 transition-colors disabled:bg-stone-300">
            {{ savingCommunity ? t('admin.saving') : t('admin.saveEditing') }}
          </button>
          <button type="button" @click="closeCommunityModal" :disabled="savingCommunity" class="flex-1 bg-stone-200 text-stone-700 py-3 rounded-lg hover:bg-stone-300 transition-colors disabled:opacity-60">
            {{ t('common.cancel') }}
          </button>
        </div>
      </form>
    </MotionModal>
  </section>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ChevronDown,
  Edit3,
  HelpCircle,
  MessageCircle,
  MessageSquare,
  MessagesSquare,
  RefreshCw,
  Route as RouteIcon,
  Trash2
} from 'lucide-vue-next'
import MotionModal from './motion/MotionModal.vue'
import api, { endpoints } from '../api'

type CommunityTab = 'routes' | 'questions' | 'comments' | 'spotComments' | 'answers'
type CommunityType = 'route' | 'question' | 'comment' | 'spotComment' | 'answer'

interface CommunityTabConfig {
  key: CommunityTab
  type: CommunityType
  label: string
  count: number
  icon: Component
}

const { t, locale } = useI18n()

const showPanel = ref(false)
const activeTab = ref<CommunityTab>('routes')
const loadingCommunity = ref(false)
const savingCommunity = ref(false)
const communityError = ref('')

const communityRoutes = ref<any[]>([])
const communityQuestions = ref<any[]>([])
const communityComments = ref<any[]>([])
const communitySpotComments = ref<any[]>([])
const communityAnswers = ref<any[]>([])

const showCommunityModal = ref(false)
const editingType = ref<CommunityType>('route')
const editingItem = ref<any>(null)
const communityForm = ref({
  title: '',
  content: '',
  days: 1,
  budget: '',
  preference: '',
  tags: '',
  rating: 5,
  isResolved: false,
  isAccepted: false
})

const communityTabs = computed<CommunityTabConfig[]>(() => [
  { key: 'routes', type: 'route', label: t('admin.communityRoutes'), count: communityRoutes.value.length, icon: RouteIcon },
  { key: 'questions', type: 'question', label: t('admin.communityQuestions'), count: communityQuestions.value.length, icon: HelpCircle },
  { key: 'comments', type: 'comment', label: t('admin.communityComments'), count: communityComments.value.length, icon: MessageSquare },
  { key: 'spotComments', type: 'spotComment', label: t('admin.communitySpotComments'), count: communitySpotComments.value.length, icon: MessageSquare },
  { key: 'answers', type: 'answer', label: t('admin.communityAnswers'), count: communityAnswers.value.length, icon: MessageCircle }
])

const totalCommunityItems = computed(() =>
  communityRoutes.value.length + communityQuestions.value.length + communityComments.value.length + communitySpotComments.value.length + communityAnswers.value.length
)

const activeDateLocale = computed(() => locale.value === 'bo' ? 'bo-CN' : 'zh-CN')

const budgetOptions = computed(() => [
  { value: '经济型', label: t('routePlanner.budget.economy') },
  { value: '舒适型', label: t('routePlanner.budget.comfort') },
  { value: '豪华型', label: t('routePlanner.budget.luxury') }
])

const preferenceOptions = computed(() => [
  { value: '自然风光', label: t('routePlanner.preferenceOptions.natural') },
  { value: '人文历史', label: t('routePlanner.preferenceOptions.cultural') },
  { value: '深度摄影', label: t('routePlanner.preferenceOptions.photography') },
  { value: '休闲度假', label: t('routePlanner.preferenceOptions.relaxation') }
])

const toArray = (value: any) => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  return []
}

const pageParams = { page: 0, size: 100 }

const fetchCommunityContent = async () => {
  loadingCommunity.value = true
  communityError.value = ''
  const requests = [
    { key: 'routes', load: () => api.get(endpoints.adminCommunity.routes, { params: pageParams }), assign: (items: any[]) => { communityRoutes.value = items } },
    { key: 'questions', load: () => api.get(endpoints.adminCommunity.questions, { params: pageParams }), assign: (items: any[]) => { communityQuestions.value = items } },
    { key: 'comments', load: () => api.get(endpoints.adminCommunity.comments, { params: pageParams }), assign: (items: any[]) => { communityComments.value = items } },
    { key: 'spotComments', load: () => api.get(endpoints.adminCommunity.spotComments, { params: pageParams }), assign: (items: any[]) => { communitySpotComments.value = items } },
    { key: 'answers', load: () => api.get(endpoints.adminCommunity.answers, { params: pageParams }), assign: (items: any[]) => { communityAnswers.value = items } }
  ]

  const results = await Promise.allSettled(requests.map(request => request.load()))
  const failed = results
    .map((result, index) => ({ result, request: requests[index] }))
    .filter(({ result }) => result.status === 'rejected')

  results.forEach((result, index) => {
    if (result.status === 'fulfilled') {
      requests[index].assign(toArray(result.value.data))
    }
  })

  if (failed.length > 0) {
    console.error('Failed to load some community content:', failed)
    if (failed.length === requests.length) {
      const firstError: any = failed[0].result.status === 'rejected' ? failed[0].result.reason : null
      communityError.value = firstError?.response?.data?.error || firstError?.response?.data?.message || t('admin.loadCommunityFailed')
    }
  }
  loadingCommunity.value = false
}

const authorName = (author: any) => {
  if (!author) return '-'
  return author.nickname || author.username || '-'
}

const contentPreview = (content: string) => {
  if (!content) return '-'
  const normalized = content.replace(/\s+/g, ' ').trim()
  return normalized.length > 90 ? `${normalized.slice(0, 90)}...` : normalized
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString(activeDateLocale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const typeLabel = (type: CommunityType) => {
  const labels: Record<CommunityType, string> = {
    route: t('admin.communityTypeRoute'),
    question: t('admin.communityTypeQuestion'),
    comment: t('admin.communityTypeComment'),
    spotComment: t('admin.communityTypeSpotComment'),
    answer: t('admin.communityTypeAnswer')
  }
  return labels[type]
}

const openEditItem = (type: CommunityType, item: any) => {
  editingType.value = type
  editingItem.value = item
  communityForm.value = {
    title: item.title || '',
    content: item.content || '',
    days: item.days || 1,
    budget: item.budget || '',
    preference: item.preference || '',
    tags: item.tags || '',
    rating: item.rating || 5,
    isResolved: Boolean(item.isResolved),
    isAccepted: Boolean(item.isAccepted)
  }
  showCommunityModal.value = true
}

const closeCommunityModal = () => {
  showCommunityModal.value = false
  editingItem.value = null
}

const saveCommunityItem = async () => {
  if (!editingItem.value) return
  savingCommunity.value = true
  try {
    const id = editingItem.value.id
    if (editingType.value === 'route') {
      await api.put(endpoints.adminCommunity.updateRoute(id), {
        title: communityForm.value.title,
        content: communityForm.value.content,
        days: communityForm.value.days,
        budget: communityForm.value.budget,
        preference: communityForm.value.preference
      })
    } else if (editingType.value === 'question') {
      await api.put(endpoints.adminCommunity.updateQuestion(id), {
        title: communityForm.value.title,
        content: communityForm.value.content,
        tags: communityForm.value.tags,
        isResolved: communityForm.value.isResolved
      })
    } else if (editingType.value === 'comment') {
      await api.put(endpoints.adminCommunity.updateComment(id), {
        content: communityForm.value.content
      })
    } else if (editingType.value === 'spotComment') {
      await api.put(endpoints.adminCommunity.updateSpotComment(id), {
        content: communityForm.value.content,
        rating: communityForm.value.rating
      })
    } else {
      await api.put(endpoints.adminCommunity.updateAnswer(id), {
        content: communityForm.value.content,
        isAccepted: communityForm.value.isAccepted
      })
    }

    await fetchCommunityContent()
    closeCommunityModal()
    alert(t('admin.saveSuccess'))
  } catch (error: any) {
    console.error('Failed to save community item:', error)
    alert(error.response?.data?.error || t('admin.saveFailed'))
  } finally {
    savingCommunity.value = false
  }
}

const deleteCommunityItem = async (type: CommunityType, id: number) => {
  if (!confirm(t('admin.confirmDeleteCommunityItem', { type: typeLabel(type) }))) return

  try {
    if (type === 'route') {
      await api.delete(endpoints.adminCommunity.deleteRoute(id))
    } else if (type === 'question') {
      await api.delete(endpoints.adminCommunity.deleteQuestion(id))
    } else if (type === 'comment') {
      await api.delete(endpoints.adminCommunity.deleteComment(id))
    } else if (type === 'spotComment') {
      await api.delete(endpoints.adminCommunity.deleteSpotComment(id))
    } else {
      await api.delete(endpoints.adminCommunity.deleteAnswer(id))
    }
    await fetchCommunityContent()
    alert(t('admin.deleteSuccess'))
  } catch (error: any) {
    console.error('Failed to delete community item:', error)
    alert(error.response?.data?.error || t('admin.deleteFailed'))
  }
}

onMounted(fetchCommunityContent)
</script>
