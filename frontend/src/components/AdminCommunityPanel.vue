<template>
  <section class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden mt-8">
    <div
      class="flex flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-300 sm:flex-row sm:items-center sm:justify-between sm:px-6 cursor-pointer"
      role="button"
      tabindex="0"
      :aria-expanded="showPanel"
      aria-controls="admin-community-panel-content"
      :aria-label="t('admin.communityManagement')"
      @click="togglePanel"
      @keydown.enter="togglePanelFromKeyboard"
      @keydown.space="togglePanelFromKeyboard"
    >
      <div class="flex min-w-0 items-center gap-3">
        <div class="w-9 h-9 rounded-lg bg-rose-100 flex items-center justify-center">
          <MessagesSquare class="w-5 h-5 text-rose-600" />
        </div>
        <h3 class="text-lg font-bold text-stone-800">
          {{ t('admin.communityManagement') }}
          <span class="text-sm font-normal text-stone-400">({{ totalCommunityItems }}{{ t('common.items') }})</span>
        </h3>
      </div>
      <div class="flex flex-wrap items-center gap-2">
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

    <div v-if="showPanel" id="admin-community-panel-content" class="space-y-5 p-4 sm:p-6">
      <div class="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0 sm:pb-0">
        <button
          v-for="tab in communityTabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          class="inline-flex shrink-0 items-center gap-2 px-3 py-2 rounded-lg text-sm border transition-colors"
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

      <div v-else-if="communityError" class="rounded-lg border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700" role="alert" aria-live="assertive" aria-atomic="true">
        {{ communityError }}
      </div>

      <div v-else class="-mx-4 overflow-x-auto px-4 sm:mx-0 sm:px-0">
        <div
          v-if="activeCommunityTabError"
          class="mb-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
        >
          {{ activeCommunityTabError }}
        </div>
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
                <button
                  type="button"
                  :title="t('common.edit')"
                  :aria-label="`${t('common.edit')} ${route.title || route.id}`.trim()"
                  :disabled="savingCommunity"
                  @click="openEditItem('route', route)"
                  class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Edit3 class="w-4 h-4" />
                </button>
                <button
                  type="button"
                  :title="t('common.delete')"
                  :aria-label="`${t('common.delete')} ${route.title || route.id}`.trim()"
                  :disabled="savingCommunity || isDeletingCommunityItem('route', route.id)"
                  :aria-busy="isDeletingCommunityItem('route', route.id)"
                  @click="deleteCommunityItem('route', route.id)"
                  class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
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
                <button
                  type="button"
                  :title="t('common.edit')"
                  :aria-label="`${t('common.edit')} ${question.title || question.id}`.trim()"
                  :disabled="savingCommunity"
                  @click="openEditItem('question', question)"
                  class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Edit3 class="w-4 h-4" />
                </button>
                <button
                  type="button"
                  :title="t('common.delete')"
                  :aria-label="`${t('common.delete')} ${question.title || question.id}`.trim()"
                  :disabled="savingCommunity || isDeletingCommunityItem('question', question.id)"
                  :aria-busy="isDeletingCommunityItem('question', question.id)"
                  @click="deleteCommunityItem('question', question.id)"
                  class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
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
                <button
                  type="button"
                  :title="t('common.edit')"
                  :aria-label="`${t('common.edit')} ${comment.routeTitle || comment.id}`.trim()"
                  :disabled="savingCommunity"
                  @click="openEditItem('comment', comment)"
                  class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Edit3 class="w-4 h-4" />
                </button>
                <button
                  type="button"
                  :title="t('common.delete')"
                  :aria-label="`${t('common.delete')} ${comment.routeTitle || comment.id}`.trim()"
                  :disabled="savingCommunity || isDeletingCommunityItem('comment', comment.id)"
                  :aria-busy="isDeletingCommunityItem('comment', comment.id)"
                  @click="deleteCommunityItem('comment', comment.id)"
                  class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
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
                <button
                  type="button"
                  :title="t('common.edit')"
                  :aria-label="`${t('common.edit')} ${comment.spotName || comment.id}`.trim()"
                  :disabled="savingCommunity"
                  @click="openEditItem('spotComment', comment)"
                  class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Edit3 class="w-4 h-4" />
                </button>
                <button
                  type="button"
                  :title="t('common.delete')"
                  :aria-label="`${t('common.delete')} ${comment.spotName || comment.id}`.trim()"
                  :disabled="savingCommunity || isDeletingCommunityItem('spotComment', comment.id)"
                  :aria-busy="isDeletingCommunityItem('spotComment', comment.id)"
                  @click="deleteCommunityItem('spotComment', comment.id)"
                  class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
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
                <button
                  type="button"
                  :title="t('common.edit')"
                  :aria-label="`${t('common.edit')} ${answer.questionTitle || answer.id}`.trim()"
                  :disabled="savingCommunity"
                  @click="openEditItem('answer', answer)"
                  class="inline-flex p-2 rounded-lg text-blue-600 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Edit3 class="w-4 h-4" />
                </button>
                <button
                  type="button"
                  :title="t('common.delete')"
                  :aria-label="`${t('common.delete')} ${answer.questionTitle || answer.id}`.trim()"
                  :disabled="savingCommunity || isDeletingCommunityItem('answer', answer.id)"
                  :aria-busy="isDeletingCommunityItem('answer', answer.id)"
                  @click="deleteCommunityItem('answer', answer.id)"
                  class="inline-flex p-2 rounded-lg text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
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
      panel-class="max-w-2xl rounded-2xl bg-white p-4 sm:p-8 max-h-[88dvh] overflow-y-auto"
      @close="closeCommunityModal"
    >
      <form @submit.prevent="saveCommunityItem" class="space-y-5" :aria-busy="savingCommunity">
        <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
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

        <div class="flex flex-col gap-3 pt-2 sm:flex-row sm:gap-4">
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
import { useConfirm } from '../composables/useConfirm'
import { useToast } from '../composables/useToast'
import { safeClientErrorMessage, summarizeClientError } from '../utils/errorMonitoring'

type CommunityTab = 'routes' | 'questions' | 'comments' | 'spotComments' | 'answers'
type CommunityType = 'route' | 'question' | 'comment' | 'spotComment' | 'answer'

interface CommunityTabConfig {
  key: CommunityTab
  type: CommunityType
  label: string
  count: number
  icon: Component
}

interface CommunityAuthor {
  nickname?: string | null
  username?: string | null
}

interface AdminCommunityBaseItem {
  id: number
  title?: string | null
  content?: string | null
  createdAt?: string | null
  days?: number | null
  budget?: string | null
  preference?: string | null
  tags?: string | null
  rating?: number | null
  isResolved?: boolean | null
  isAccepted?: boolean | null
}

interface AdminCommunityRoute extends AdminCommunityBaseItem {
  title?: string | null
  author?: CommunityAuthor | null
  days?: number | null
  budget?: string | null
  preference?: string | null
  viewCount?: number | null
  likeCount?: number | null
  commentCount?: number | null
}

interface AdminCommunityQuestion extends AdminCommunityBaseItem {
  title?: string | null
  tags?: string | null
  author?: CommunityAuthor | null
  isResolved?: boolean | null
  answerCount?: number | null
  likeCount?: number | null
}

interface AdminCommunityComment extends AdminCommunityBaseItem {
  routeTitle?: string | null
  routeId?: number | null
  user?: CommunityAuthor | null
}

interface AdminCommunitySpotComment extends AdminCommunityBaseItem {
  spotName?: string | null
  spotId?: number | null
  user?: CommunityAuthor | null
  rating?: number | null
  likeCount?: number | null
}

interface AdminCommunityAnswer extends AdminCommunityBaseItem {
  questionTitle?: string | null
  questionId?: number | null
  user?: CommunityAuthor | null
  isAccepted?: boolean | null
}

type CommunityItem =
  | AdminCommunityRoute
  | AdminCommunityQuestion
  | AdminCommunityComment
  | AdminCommunitySpotComment
  | AdminCommunityAnswer

type CommunityListResponse<T> = T[] | { content?: T[] | null } | null | undefined

const { t, locale } = useI18n()
const { showConfirm } = useConfirm()
const { showToast } = useToast()

const showPanel = ref(false)
const activeTab = ref<CommunityTab>('routes')
const loadingCommunity = ref(false)
const savingCommunity = ref(false)
const communityError = ref('')
const emptyCommunityTabErrors = (): Record<CommunityTab, string> => ({
  routes: '',
  questions: '',
  comments: '',
  spotComments: '',
  answers: ''
})
const communityTabErrors = ref<Record<CommunityTab, string>>(emptyCommunityTabErrors())
const deletingCommunityKeys = ref<ReadonlySet<string>>(new Set())
let communityRequestId = 0

const communityRoutes = ref<AdminCommunityRoute[]>([])
const communityQuestions = ref<AdminCommunityQuestion[]>([])
const communityComments = ref<AdminCommunityComment[]>([])
const communitySpotComments = ref<AdminCommunitySpotComment[]>([])
const communityAnswers = ref<AdminCommunityAnswer[]>([])

const showCommunityModal = ref(false)
const editingType = ref<CommunityType>('route')
const editingItem = ref<CommunityItem | null>(null)
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
const activeCommunityTabError = computed(() => communityTabErrors.value[activeTab.value] || '')

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

const togglePanel = () => {
  showPanel.value = !showPanel.value
}

const togglePanelFromKeyboard = (event: KeyboardEvent) => {
  if (event.target !== event.currentTarget) return
  event.preventDefault()
  togglePanel()
}

const toArray = <T>(value: CommunityListResponse<T>): T[] => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  return []
}

const pageParams = { page: 0, size: 100 }

const fetchCommunityList = async <T>(url: string) => {
  const response = await api.get<CommunityListResponse<T>>(url, { params: pageParams })
  return toArray<T>(response.data)
}

const isCurrentCommunityRequest = (requestId: number) => requestId === communityRequestId

const fetchCommunityContent = async () => {
  const requestId = ++communityRequestId
  loadingCommunity.value = true
  communityError.value = ''
  communityTabErrors.value = emptyCommunityTabErrors()
  const results = await Promise.allSettled([
    fetchCommunityList<AdminCommunityRoute>(endpoints.adminCommunity.routes),
    fetchCommunityList<AdminCommunityQuestion>(endpoints.adminCommunity.questions),
    fetchCommunityList<AdminCommunityComment>(endpoints.adminCommunity.comments),
    fetchCommunityList<AdminCommunitySpotComment>(endpoints.adminCommunity.spotComments),
    fetchCommunityList<AdminCommunityAnswer>(endpoints.adminCommunity.answers)
  ])
  if (!isCurrentCommunityRequest(requestId)) return

  const resultEntries = [
    { key: 'routes', result: results[0] },
    { key: 'questions', result: results[1] },
    { key: 'comments', result: results[2] },
    { key: 'spotComments', result: results[3] },
    { key: 'answers', result: results[4] }
  ] as const
  const failed = results
    .map((result, index) => ({ result, request: resultEntries[index] }))
    .filter(({ result }) => result.status === 'rejected')

  if (results[0].status === 'fulfilled') communityRoutes.value = results[0].value
  if (results[1].status === 'fulfilled') communityQuestions.value = results[1].value
  if (results[2].status === 'fulfilled') communityComments.value = results[2].value
  if (results[3].status === 'fulfilled') communitySpotComments.value = results[3].value
  if (results[4].status === 'fulfilled') communityAnswers.value = results[4].value

  if (failed.length > 0) {
    console.error('Failed to load some community content:', failed.map(({ result, request }) => ({
      key: request.key,
      error: result.status === 'rejected' ? summarizeClientError(result.reason) : 'unknown'
    })))
    if (failed.length === resultEntries.length) {
      const firstError: unknown = failed[0].result.status === 'rejected' ? failed[0].result.reason : null
      communityError.value = safeClientErrorMessage(firstError, t('admin.loadCommunityFailed'))
    } else {
      communityTabErrors.value = failed.reduce<Record<CommunityTab, string>>((errors, { result, request }) => {
        const error = result.status === 'rejected' ? result.reason : null
        errors[request.key] = safeClientErrorMessage(error, t('admin.loadCommunityFailed'))
        return errors
      }, emptyCommunityTabErrors())
    }
  }
  if (isCurrentCommunityRequest(requestId)) {
    loadingCommunity.value = false
  }
}

const authorName = (author?: CommunityAuthor | null) => {
  if (!author) return '-'
  return author.nickname || author.username || '-'
}

const contentPreview = (content?: string | null) => {
  if (!content) return '-'
  const normalized = content.replace(/\s+/g, ' ').trim()
  return normalized.length > 90 ? `${normalized.slice(0, 90)}...` : normalized
}

const formatDateTime = (dateStr?: string | null) => {
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

const openEditItem = (type: CommunityType, item: CommunityItem) => {
  if (savingCommunity.value) return
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
  if (savingCommunity.value) return
  showCommunityModal.value = false
  editingItem.value = null
}

const forceCloseCommunityModal = () => {
  showCommunityModal.value = false
  editingItem.value = null
}

const isEditingCommunityItem = (type: CommunityType, id: number) =>
  editingType.value === type && editingItem.value?.id === id

const communityActionKey = (type: CommunityType, id: number) => `${type}:${id}`

const isDeletingCommunityItem = (type: CommunityType, id: number) =>
  deletingCommunityKeys.value.has(communityActionKey(type, id))

const setCommunityItemDeleting = (type: CommunityType, id: number, deleting: boolean) => {
  const next = new Set(deletingCommunityKeys.value)
  const key = communityActionKey(type, id)
  if (deleting) {
    next.add(key)
  } else {
    next.delete(key)
  }
  deletingCommunityKeys.value = next
}

const saveCommunityItem = async () => {
  if (!editingItem.value || savingCommunity.value) return
  const type = editingType.value
  const itemId = editingItem.value.id
  const form = { ...communityForm.value }
  savingCommunity.value = true
  try {
    if (type === 'route') {
      await api.put(endpoints.adminCommunity.updateRoute(itemId), {
        title: form.title,
        content: form.content,
        days: form.days,
        budget: form.budget,
        preference: form.preference
      })
    } else if (type === 'question') {
      await api.put(endpoints.adminCommunity.updateQuestion(itemId), {
        title: form.title,
        content: form.content,
        tags: form.tags,
        isResolved: form.isResolved
      })
    } else if (type === 'comment') {
      await api.put(endpoints.adminCommunity.updateComment(itemId), {
        content: form.content
      })
    } else if (type === 'spotComment') {
      await api.put(endpoints.adminCommunity.updateSpotComment(itemId), {
        content: form.content,
        rating: form.rating
      })
    } else {
      await api.put(endpoints.adminCommunity.updateAnswer(itemId), {
        content: form.content,
        isAccepted: form.isAccepted
      })
    }

    await fetchCommunityContent()
    if (isEditingCommunityItem(type, itemId)) {
      forceCloseCommunityModal()
    }
    showToast(t('admin.saveSuccess'), 'success')
  } catch (error: unknown) {
    console.error('Failed to save community item:', summarizeClientError(error))
    showToast(safeClientErrorMessage(error, t('admin.saveFailed')), 'error')
  } finally {
    savingCommunity.value = false
  }
}

const deleteCommunityItem = async (type: CommunityType, id: number) => {
  if (savingCommunity.value || isDeletingCommunityItem(type, id)) return
  setCommunityItemDeleting(type, id, true)
  try {
    const confirmed = await showConfirm({
      message: t('admin.confirmDeleteCommunityItem', { type: typeLabel(type) }),
      tone: 'danger'
    })
    if (!confirmed) return

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
    showToast(t('admin.deleteSuccess'), 'success')
  } catch (error: unknown) {
    console.error('Failed to delete community item:', summarizeClientError(error))
    showToast(safeClientErrorMessage(error, t('admin.deleteFailed')), 'error')
  } finally {
    setCommunityItemDeleting(type, id, false)
  }
}

onMounted(fetchCommunityContent)
</script>
