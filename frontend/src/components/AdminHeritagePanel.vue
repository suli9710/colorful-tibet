<template>
  <div class="mt-8 overflow-hidden rounded-xl border border-stone-200 bg-white shadow-sm">
    <div
      class="flex cursor-pointer flex-col gap-3 border-b border-stone-100 bg-gradient-to-r from-stone-50 to-white px-4 py-4 transition-colors hover:bg-stone-100/50 lg:flex-row lg:items-center lg:justify-between sm:px-6"
      @click="showHeritage = !showHeritage"
    >
      <div class="flex min-w-0 items-center gap-3">
        <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-rose-100">
          <svg class="h-5 w-5 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
        </div>
        <h3 class="text-lg font-bold text-stone-800">
          {{ text('admin.heritageManagement', '非遗管理') }}
          <span class="text-sm font-normal text-stone-400">({{ filteredItems.length }}{{ text('common.items', '项') }})</span>
        </h3>
      </div>
      <div class="grid w-full grid-cols-2 gap-2 sm:flex sm:flex-wrap sm:items-center lg:w-auto">
        <input
          v-model.trim="keyword"
          type="search"
          class="col-span-2 w-full rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100 sm:w-56 sm:text-xs"
          :placeholder="text('admin.searchHeritage', '搜索项目/类别/地区')"
          @click.stop
        >
        <button
          class="mobile-touch-target rounded-lg bg-stone-100 px-3 py-2 text-xs text-stone-600 transition-colors hover:bg-stone-200 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="loading"
          @click.stop="fetchItems"
        >
          {{ loading ? text('common.loading', '加载中') : text('common.refresh', '刷新') }}
        </button>
        <button
          class="mobile-touch-target rounded-lg bg-rose-500 px-3 py-2 text-xs text-white transition-colors hover:bg-rose-600"
          @click.stop="openCreateItemModal"
        >
          {{ text('admin.createHeritage', '新增非遗') }}
        </button>
        <svg class="col-span-2 mx-auto h-5 w-5 text-stone-400 transition-transform duration-200 sm:col-span-1 sm:mx-0" :class="{ 'rotate-180': showHeritage }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </div>
    </div>

    <div v-if="loading" class="p-8 text-center text-stone-500">
      <div class="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-b-2 border-rose-500"></div>
      <p>{{ text('admin.loadingHeritage', '正在加载非遗项目') }}</p>
    </div>

    <div v-else-if="showHeritage" class="divide-y divide-stone-200">
      <div v-for="item in filteredItems" :key="item.id" class="px-4 py-4 transition-colors hover:bg-stone-50 sm:px-6">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start">
          <div class="h-36 w-full flex-shrink-0 overflow-hidden rounded-lg bg-stone-100 lg:h-24 lg:w-28">
            <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" class="h-full w-full object-cover">
            <div v-else class="flex h-full w-full items-center justify-center bg-rose-50 text-2xl font-bold text-rose-500">
              {{ item.name?.charAt(0) || 'H' }}
            </div>
          </div>

          <div class="min-w-0 flex-1">
            <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
              <div class="min-w-0 flex-1">
                <div class="mb-1 flex flex-wrap items-center gap-2">
                  <h4 class="truncate text-lg font-bold text-stone-800">{{ item.name }}</h4>
                  <span v-if="item.category" class="rounded-full bg-rose-50 px-2 py-1 text-xs font-medium text-rose-700">{{ item.category }}</span>
                  <span v-if="item.protectionLevel" class="rounded-full bg-amber-50 px-2 py-1 text-xs font-medium text-amber-700">{{ item.protectionLevel }}</span>
                </div>
                <p class="line-clamp-2 text-sm text-stone-600">{{ item.description || '-' }}</p>
                <div class="mt-2 flex flex-wrap gap-3 text-xs text-stone-500">
                  <span>ID: {{ item.id }}</span>
                  <span>{{ text('admin.region', '地区') }}: {{ item.region || '-' }}</span>
                  <span>{{ text('admin.views', '浏览') }} {{ item.viewCount || 0 }}</span>
                  <span>{{ text('community.likeCount', '点赞') }} {{ item.likeCount || 0 }}</span>
                  <span>{{ text('heritage.commentsTitle', '评论') }} {{ item.commentCount || 0 }}</span>
                </div>
              </div>

              <div class="grid flex-shrink-0 grid-cols-3 gap-2 text-center text-sm sm:flex sm:flex-wrap sm:text-left">
                <button class="rounded-lg bg-rose-50 px-2 py-2 font-medium text-rose-600 hover:text-rose-800 sm:bg-transparent sm:px-0 sm:py-0" @click="toggleRelations(item)">
                  {{ expandedItemId === item.id ? text('common.collapse', '收起') : text('admin.manageRelations', '传承人/活动') }}
                </button>
                <button class="rounded-lg bg-blue-50 px-2 py-2 font-medium text-blue-600 hover:text-blue-800 sm:bg-transparent sm:px-0 sm:py-0" @click="openEditItemModal(item)">
                  {{ text('common.edit', '编辑') }}
                </button>
                <button class="rounded-lg bg-red-50 px-2 py-2 font-medium text-red-600 hover:text-red-800 sm:bg-transparent sm:px-0 sm:py-0" @click="deleteItem(item)">
                  {{ text('common.delete', '删除') }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="expandedItemId === item.id" class="mt-4 rounded-lg border border-stone-200 bg-stone-50/70 p-4">
          <div v-if="relationsLoading" class="py-6 text-center text-sm text-stone-500">{{ text('common.loading', '加载中') }}</div>
          <div v-else class="grid gap-4 xl:grid-cols-2">
            <section class="rounded-lg border border-stone-200 bg-white p-4">
              <div class="mb-3 flex items-center justify-between">
                <h5 class="font-semibold text-stone-800">{{ text('heritage.inheritors', '代表性传承人') }}</h5>
                <button class="text-xs font-medium text-rose-600 hover:text-rose-800" @click="startCreateInheritor">{{ text('admin.addNew', '新增') }}</button>
              </div>

              <div class="mb-4 space-y-2">
                <div v-for="inheritor in inheritors" :key="inheritor.id" class="flex flex-col gap-2 rounded-lg border border-stone-100 px-3 py-2 sm:flex-row sm:items-start sm:justify-between">
                  <div class="min-w-0">
                    <div class="font-medium text-stone-800">{{ inheritor.name }}</div>
                    <div class="text-xs text-stone-500">{{ [inheritor.level, inheritor.region].filter(Boolean).join(' · ') || '-' }}</div>
                    <p class="mt-1 line-clamp-2 text-xs text-stone-500">{{ inheritor.bio || inheritor.story || '-' }}</p>
                  </div>
                  <div class="flex flex-shrink-0 gap-2 text-xs">
                    <button class="text-blue-600" @click="startEditInheritor(inheritor)">{{ text('common.edit', '编辑') }}</button>
                    <button class="text-red-600" @click="deleteInheritor(inheritor.id)">{{ text('common.delete', '删除') }}</button>
                  </div>
                </div>
                <div v-if="inheritors.length === 0" class="rounded-lg border border-dashed border-stone-200 py-6 text-center text-sm text-stone-400">
                  {{ text('admin.noInheritors', '暂无传承人') }}
                </div>
              </div>

              <form class="grid gap-2" @submit.prevent="saveInheritor">
                <div class="grid gap-2 sm:grid-cols-2">
                  <input v-model.trim="inheritorForm.name" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.name', '名称') + ' *'">
                  <input v-model.trim="inheritorForm.level" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.level', '级别')">
                  <input v-model.trim="inheritorForm.region" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.region', '地区')">
                  <input v-model.trim="inheritorForm.avatarUrl" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.avatarUrl', '头像 URL')">
                </div>
                <textarea v-model.trim="inheritorForm.bio" rows="2" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.bio', '简介')"></textarea>
                <textarea v-model.trim="inheritorForm.story" rows="2" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.inheritorStory', '传承故事')"></textarea>
                <div class="flex flex-col gap-2 sm:flex-row">
                  <button class="rounded-lg bg-rose-500 px-3 py-2 text-xs font-medium text-white hover:bg-rose-600 disabled:cursor-not-allowed disabled:opacity-60" :disabled="!inheritorForm.name || savingInheritor">
                    {{ savingInheritor ? text('admin.saving', '保存中') : (editingInheritorId ? text('common.save', '保存') : text('admin.addNew', '新增')) }}
                  </button>
                  <button v-if="editingInheritorId" type="button" class="rounded-lg bg-stone-200 px-3 py-2 text-xs font-medium text-stone-700 hover:bg-stone-300" @click="resetInheritorForm">
                    {{ text('common.cancel', '取消') }}
                  </button>
                </div>
              </form>
            </section>

            <section class="rounded-lg border border-stone-200 bg-white p-4">
              <div class="mb-3 flex items-center justify-between">
                <h5 class="font-semibold text-stone-800">{{ text('heritage.relatedEvents', '相关活动') }}</h5>
                <button class="text-xs font-medium text-rose-600 hover:text-rose-800" @click="startCreateEvent">{{ text('admin.addNew', '新增') }}</button>
              </div>

              <div class="mb-4 space-y-2">
                <div v-for="event in events" :key="event.id" class="flex flex-col gap-2 rounded-lg border border-stone-100 px-3 py-2 sm:flex-row sm:items-start sm:justify-between">
                  <div class="min-w-0">
                    <div class="font-medium text-stone-800">{{ event.title }}</div>
                    <div class="text-xs text-stone-500">{{ [event.eventDate, event.location].filter(Boolean).join(' · ') || '-' }}</div>
                    <p class="mt-1 line-clamp-2 text-xs text-stone-500">{{ event.description || '-' }}</p>
                  </div>
                  <div class="flex flex-shrink-0 gap-2 text-xs">
                    <button class="text-blue-600" @click="startEditEvent(event)">{{ text('common.edit', '编辑') }}</button>
                    <button class="text-red-600" @click="deleteEvent(event.id)">{{ text('common.delete', '删除') }}</button>
                  </div>
                </div>
                <div v-if="events.length === 0" class="rounded-lg border border-dashed border-stone-200 py-6 text-center text-sm text-stone-400">
                  {{ text('admin.noEvents', '暂无活动') }}
                </div>
              </div>

              <form class="grid gap-2" @submit.prevent="saveEvent">
                <input v-model.trim="eventForm.title" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.titleLabel', '标题') + ' *'">
                <div class="grid gap-2 sm:grid-cols-2">
                  <input v-model="eventForm.eventDate" type="date" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400">
                  <input v-model="eventForm.endDate" type="date" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400">
                  <input v-model.trim="eventForm.location" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.location', '地点')">
                  <input v-model.trim="eventForm.contactInfo" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.contactInfo', '联系方式')">
                </div>
                <input v-model.trim="eventForm.imageUrl" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.imageUrl', '图片 URL')">
                <textarea v-model.trim="eventForm.description" rows="2" class="rounded-lg border border-stone-200 px-3 py-2 text-sm outline-none focus:border-rose-400" :placeholder="text('admin.content', '内容')"></textarea>
                <div class="flex flex-col gap-2 sm:flex-row">
                  <button class="rounded-lg bg-rose-500 px-3 py-2 text-xs font-medium text-white hover:bg-rose-600 disabled:cursor-not-allowed disabled:opacity-60" :disabled="!eventForm.title || savingEvent">
                    {{ savingEvent ? text('admin.saving', '保存中') : (editingEventId ? text('common.save', '保存') : text('admin.addNew', '新增')) }}
                  </button>
                  <button v-if="editingEventId" type="button" class="rounded-lg bg-stone-200 px-3 py-2 text-xs font-medium text-stone-700 hover:bg-stone-300" @click="resetEventForm">
                    {{ text('common.cancel', '取消') }}
                  </button>
                </div>
              </form>
            </section>
          </div>
        </div>
      </div>

      <div v-if="filteredItems.length === 0" class="px-6 py-8 text-center text-stone-500">
        {{ text('admin.noHeritage', '暂无非遗项目') }}
      </div>
    </div>
    <div v-else-if="items.length > 0" class="px-6 py-4 text-center text-sm text-stone-500">
      {{ text('admin.expandHeritage', '展开管理非遗项目') }}
    </div>

    <MotionModal
      :show="showItemModal"
      modal-key="admin-heritage-modal"
      panel-class="max-w-3xl rounded-2xl bg-white p-4 sm:p-8 max-h-[90dvh] overflow-y-auto"
      @close="closeItemModal"
    >
      <h2 class="mb-5 text-xl font-bold text-stone-800 sm:mb-6 sm:text-2xl">
        {{ editingItem?.id ? text('admin.editHeritage', '编辑非遗项目') : text('admin.createHeritage', '新增非遗') }}
      </h2>

      <div class="grid gap-4 sm:grid-cols-2">
        <label class="block text-sm font-medium text-stone-700">
          {{ text('admin.name', '名称') }} <span class="text-red-500">*</span>
          <input v-model.trim="itemForm.name" class="mt-2 w-full rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100">
        </label>
        <label class="block text-sm font-medium text-stone-700">
          {{ text('admin.nameTibetan', '藏文名称') }}
          <input v-model.trim="itemForm.nameTibetan" class="mt-2 w-full rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100">
        </label>
        <label class="block text-sm font-medium text-stone-700">
          {{ text('admin.category', '类别') }}
          <input v-model.trim="itemForm.category" class="mt-2 w-full rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100">
        </label>
        <label class="block text-sm font-medium text-stone-700">
          {{ text('admin.region', '地区') }}
          <input v-model.trim="itemForm.region" class="mt-2 w-full rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100">
        </label>
        <label class="block text-sm font-medium text-stone-700">
          {{ text('admin.protectionLevel', '保护级别') }}
          <input v-model.trim="itemForm.protectionLevel" class="mt-2 w-full rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100">
        </label>
        <label class="block text-sm font-medium text-stone-700">
          {{ text('admin.videoUrl', '视频 URL') }}
          <input v-model.trim="itemForm.videoUrl" class="mt-2 w-full rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100">
        </label>
        <label class="block text-sm font-medium text-stone-700 sm:col-span-2">
          {{ text('admin.image', '图片') }}
          <ImageUploadField v-model="itemForm.imageUrl" :upload-endpoint="endpoints.admin.uploadImage" />
        </label>
        <label class="block text-sm font-medium text-stone-700 sm:col-span-2">
          {{ text('admin.baikeUrl', '百科链接') }}
          <input v-model.trim="itemForm.baikeUrl" class="mt-2 w-full rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100">
        </label>
        <label class="block text-sm font-medium text-stone-700 sm:col-span-2">
          {{ text('admin.content', '内容') }}
          <textarea v-model.trim="itemForm.description" rows="4" class="mt-2 w-full resize-none rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100"></textarea>
        </label>
        <label class="block text-sm font-medium text-stone-700 sm:col-span-2">
          {{ text('admin.tibetanDescription', '藏文描述') }}
          <textarea v-model.trim="itemForm.descriptionTibetan" rows="3" class="mt-2 w-full resize-none rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100"></textarea>
        </label>
        <label class="block text-sm font-medium text-stone-700 sm:col-span-2">
          {{ text('heritage.originStory', '起源故事') }}
          <textarea v-model.trim="itemForm.originStory" rows="3" class="mt-2 w-full resize-none rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100"></textarea>
        </label>
        <label class="block text-sm font-medium text-stone-700 sm:col-span-2">
          {{ text('heritage.culturalValue', '文化价值') }}
          <textarea v-model.trim="itemForm.significance" rows="3" class="mt-2 w-full resize-none rounded-lg border border-stone-300 px-4 py-3 outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-100"></textarea>
        </label>
      </div>

      <div class="mt-6 flex flex-col gap-3 sm:flex-row sm:gap-4">
        <button
          class="flex-1 rounded-lg bg-rose-500 py-3 text-white transition-colors hover:bg-rose-600 disabled:cursor-not-allowed disabled:bg-stone-300"
          :disabled="savingItem || !itemForm.name"
          @click="saveItem"
        >
          {{ savingItem ? text('admin.saving', '保存中') : text('common.save', '保存') }}
        </button>
        <button
          class="flex-1 rounded-lg bg-stone-200 py-3 text-stone-700 transition-colors hover:bg-stone-300 disabled:cursor-not-allowed"
          :disabled="savingItem"
          @click="closeItemModal"
        >
          {{ text('common.cancel', '取消') }}
        </button>
      </div>
    </MotionModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import api, {
  endpoints,
  type HeritageEventItem,
  type HeritageInheritorItem,
  type HeritageItem
} from '../api'
import ImageUploadField from './ImageUploadField.vue'
import MotionModal from './motion/MotionModal.vue'

type HeritageItemForm = {
  name: string
  nameTibetan: string
  description: string
  descriptionTibetan: string
  category: string
  imageUrl: string
  videoUrl: string
  originStory: string
  significance: string
  baikeUrl: string
  region: string
  protectionLevel: string
}

type InheritorForm = {
  name: string
  nameTibetan: string
  avatarUrl: string
  level: string
  bio: string
  bioTibetan: string
  story: string
  region: string
}

type EventForm = {
  title: string
  titleTibetan: string
  description: string
  descriptionTibetan: string
  eventDate: string
  endDate: string
  location: string
  imageUrl: string
  contactInfo: string
}

const { t, te } = useI18n()

const text = (key: string, fallback: string) => te(key) ? t(key) : fallback
const adminPageParams = { page: 0, size: 100 }
const toList = (value: any) => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  return []
}

const emptyItemForm = (): HeritageItemForm => ({
  name: '',
  nameTibetan: '',
  description: '',
  descriptionTibetan: '',
  category: '',
  imageUrl: '',
  videoUrl: '',
  originStory: '',
  significance: '',
  baikeUrl: '',
  region: '',
  protectionLevel: ''
})

const emptyInheritorForm = (): InheritorForm => ({
  name: '',
  nameTibetan: '',
  avatarUrl: '',
  level: '',
  bio: '',
  bioTibetan: '',
  story: '',
  region: ''
})

const emptyEventForm = (): EventForm => ({
  title: '',
  titleTibetan: '',
  description: '',
  descriptionTibetan: '',
  eventDate: '',
  endDate: '',
  location: '',
  imageUrl: '',
  contactInfo: ''
})

const cleanPayload = <T extends Record<string, unknown>>(form: T) => {
  return Object.fromEntries(
    Object.entries(form).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
}

const items = ref<HeritageItem[]>([])
const loading = ref(false)
const showHeritage = ref(false)
const keyword = ref('')

const showItemModal = ref(false)
const editingItem = ref<HeritageItem | null>(null)
const itemForm = ref<HeritageItemForm>(emptyItemForm())
const savingItem = ref(false)

const expandedItemId = ref<number | null>(null)
const inheritors = ref<HeritageInheritorItem[]>([])
const events = ref<HeritageEventItem[]>([])
const relationsLoading = ref(false)

const editingInheritorId = ref<number | null>(null)
const inheritorForm = ref<InheritorForm>(emptyInheritorForm())
const savingInheritor = ref(false)

const editingEventId = ref<number | null>(null)
const eventForm = ref<EventForm>(emptyEventForm())
const savingEvent = ref(false)

const filteredItems = computed(() => {
  const kw = keyword.value.toLowerCase()
  if (!kw) return items.value
  return items.value.filter(item => [
    item.name,
    item.category,
    item.region,
    item.protectionLevel,
    item.description
  ].some(value => String(value || '').toLowerCase().includes(kw)))
})

const fetchItems = async () => {
  loading.value = true
  try {
    const response = await api.get(endpoints.adminHeritage.list, { params: adminPageParams })
    items.value = toList(response.data)
  } catch (error) {
    console.error('Failed to fetch heritage items:', error)
    items.value = []
  } finally {
    loading.value = false
  }
}

const openCreateItemModal = () => {
  editingItem.value = null
  itemForm.value = emptyItemForm()
  showItemModal.value = true
}

const openEditItemModal = (item: HeritageItem) => {
  editingItem.value = item
  itemForm.value = {
    name: item.name || '',
    nameTibetan: item.nameTibetan || '',
    description: item.description || '',
    descriptionTibetan: item.descriptionTibetan || '',
    category: item.category || '',
    imageUrl: item.imageUrl || '',
    videoUrl: item.videoUrl || '',
    originStory: item.originStory || '',
    significance: item.significance || '',
    baikeUrl: item.baikeUrl || '',
    region: item.region || '',
    protectionLevel: item.protectionLevel || ''
  }
  showItemModal.value = true
}

const closeItemModal = () => {
  showItemModal.value = false
  editingItem.value = null
  itemForm.value = emptyItemForm()
}

const saveItem = async () => {
  if (!itemForm.value.name) return
  savingItem.value = true
  try {
    const payload = cleanPayload(itemForm.value)
    if (editingItem.value?.id) {
      await api.put(endpoints.adminHeritage.update(editingItem.value.id), payload)
    } else {
      await api.post(endpoints.adminHeritage.create, payload)
    }
    await fetchItems()
    closeItemModal()
    alert(text('admin.saveSuccess', '保存成功'))
  } catch (error) {
    console.error('Failed to save heritage item:', error)
    alert(text('admin.saveFailed', '保存失败'))
  } finally {
    savingItem.value = false
  }
}

const deleteItem = async (item: HeritageItem) => {
  if (!confirm(text('admin.confirmDelete', '确定要删除吗？'))) return
  try {
    await api.delete(endpoints.adminHeritage.delete(item.id))
    if (expandedItemId.value === item.id) {
      expandedItemId.value = null
    }
    await fetchItems()
  } catch (error) {
    console.error('Failed to delete heritage item:', error)
    alert(text('admin.deleteFailed', '删除失败'))
  }
}

const toggleRelations = async (item: HeritageItem) => {
  if (expandedItemId.value === item.id) {
    expandedItemId.value = null
    inheritors.value = []
    events.value = []
    return
  }
  expandedItemId.value = item.id
  resetInheritorForm()
  resetEventForm()
  await loadRelations(item.id)
}

const loadRelations = async (itemId: number) => {
  relationsLoading.value = true
  try {
    const [inheritorResponse, eventResponse] = await Promise.all([
      api.get(endpoints.adminHeritage.inheritors(itemId)),
      api.get(endpoints.adminHeritage.events(itemId))
    ])
    inheritors.value = Array.isArray(inheritorResponse.data) ? inheritorResponse.data : []
    events.value = Array.isArray(eventResponse.data) ? eventResponse.data : []
  } catch (error) {
    console.error('Failed to fetch heritage relations:', error)
    inheritors.value = []
    events.value = []
  } finally {
    relationsLoading.value = false
  }
}

const startCreateInheritor = () => {
  resetInheritorForm()
}

const startEditInheritor = (inheritor: HeritageInheritorItem) => {
  editingInheritorId.value = inheritor.id
  inheritorForm.value = {
    name: inheritor.name || '',
    nameTibetan: inheritor.nameTibetan || '',
    avatarUrl: inheritor.avatarUrl || '',
    level: inheritor.level || '',
    bio: inheritor.bio || '',
    bioTibetan: inheritor.bioTibetan || '',
    story: inheritor.story || '',
    region: inheritor.region || ''
  }
}

function resetInheritorForm() {
  editingInheritorId.value = null
  inheritorForm.value = emptyInheritorForm()
}

const saveInheritor = async () => {
  if (!expandedItemId.value || !inheritorForm.value.name) return
  savingInheritor.value = true
  try {
    const payload = cleanPayload(inheritorForm.value)
    if (editingInheritorId.value) {
      await api.put(endpoints.adminHeritage.updateInheritor(editingInheritorId.value), payload)
    } else {
      await api.post(endpoints.adminHeritage.inheritors(expandedItemId.value), payload)
    }
    await loadRelations(expandedItemId.value)
    resetInheritorForm()
  } catch (error) {
    console.error('Failed to save inheritor:', error)
    alert(text('admin.saveFailed', '保存失败'))
  } finally {
    savingInheritor.value = false
  }
}

const deleteInheritor = async (id: number) => {
  if (!expandedItemId.value || !confirm(text('admin.confirmDelete', '确定要删除吗？'))) return
  try {
    await api.delete(endpoints.adminHeritage.deleteInheritor(id))
    await loadRelations(expandedItemId.value)
  } catch (error) {
    console.error('Failed to delete inheritor:', error)
    alert(text('admin.deleteFailed', '删除失败'))
  }
}

const startCreateEvent = () => {
  resetEventForm()
}

const startEditEvent = (event: HeritageEventItem) => {
  editingEventId.value = event.id
  eventForm.value = {
    title: event.title || '',
    titleTibetan: event.titleTibetan || '',
    description: event.description || '',
    descriptionTibetan: event.descriptionTibetan || '',
    eventDate: event.eventDate || '',
    endDate: event.endDate || '',
    location: event.location || '',
    imageUrl: event.imageUrl || '',
    contactInfo: event.contactInfo || ''
  }
}

function resetEventForm() {
  editingEventId.value = null
  eventForm.value = emptyEventForm()
}

const saveEvent = async () => {
  if (!expandedItemId.value || !eventForm.value.title) return
  savingEvent.value = true
  try {
    const payload = cleanPayload(eventForm.value)
    if (editingEventId.value) {
      await api.put(endpoints.adminHeritage.updateEvent(editingEventId.value), payload)
    } else {
      await api.post(endpoints.adminHeritage.events(expandedItemId.value), payload)
    }
    await loadRelations(expandedItemId.value)
    resetEventForm()
  } catch (error) {
    console.error('Failed to save event:', error)
    alert(text('admin.saveFailed', '保存失败'))
  } finally {
    savingEvent.value = false
  }
}

const deleteEvent = async (id: number) => {
  if (!expandedItemId.value || !confirm(text('admin.confirmDelete', '确定要删除吗？'))) return
  try {
    await api.delete(endpoints.adminHeritage.deleteEvent(id))
    await loadRelations(expandedItemId.value)
  } catch (error) {
    console.error('Failed to delete event:', error)
    alert(text('admin.deleteFailed', '删除失败'))
  }
}

onMounted(fetchItems)
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
