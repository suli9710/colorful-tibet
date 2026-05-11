<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Image, Loader2, UploadCloud, X } from 'lucide-vue-next'
import api from '@/api'

const props = withDefaults(defineProps<{
  modelValue: string
  uploadEndpoint?: string
  placeholder?: string
}>(), {
  uploadEndpoint: '/admin/upload-image',
  placeholder: ''
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const { t } = useI18n()
const urlValue = ref(props.modelValue || '')
const dragging = ref(false)
const uploading = ref(false)
const imageError = ref(false)
const uploadError = ref('')
const inputPlaceholder = computed(() => props.placeholder || t('upload.pasteUrl'))

watch(() => props.modelValue, value => {
  urlValue.value = value || ''
  imageError.value = false
})

const openFilePicker = () => {
  if (!uploading.value) {
    fileInput.value?.click()
  }
}

const updateUrl = () => {
  imageError.value = false
  uploadError.value = ''
  emit('update:modelValue', urlValue.value.trim())
}

const clearImage = () => {
  urlValue.value = ''
  imageError.value = false
  uploadError.value = ''
  emit('update:modelValue', '')
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const handleFileInput = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    await uploadFile(file)
  }
  target.value = ''
}

const handleDrop = async (event: DragEvent) => {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    await uploadFile(file)
  }
}

const uploadFile = async (file: File) => {
  uploadError.value = ''
  imageError.value = false

  if (!file.type.startsWith('image/')) {
    uploadError.value = t('upload.selectImageFile')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    uploadError.value = t('upload.imageTooLarge')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post(props.uploadEndpoint, formData)
    const imageUrl = response.data?.imageUrl || response.data?.url
    if (!imageUrl) {
      throw new Error(t('upload.missingUploadUrl'))
    }
    urlValue.value = imageUrl
    emit('update:modelValue', imageUrl)
  } catch (error: any) {
    uploadError.value = error.response?.data?.error || error.response?.data?.message || error.message || t('upload.uploadFailed')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="space-y-3">
    <div
      class="relative overflow-hidden rounded-lg border border-dashed bg-stone-50 transition-colors"
      :class="dragging ? 'border-blue-500 bg-blue-50' : 'border-stone-300 hover:border-blue-400'"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        class="hidden"
        @change="handleFileInput"
      >

      <div v-if="modelValue" class="relative h-48 bg-stone-100">
        <img
          :src="modelValue"
          :alt="t('upload.previewAlt')"
          class="h-full w-full object-cover"
          @error="imageError = true"
          @load="imageError = false"
        >
        <div v-if="imageError" class="absolute inset-0 flex items-center justify-center bg-red-50 text-sm text-red-600">
          {{ t('upload.imageLoadFailed') }}
        </div>
        <div v-if="uploading" class="absolute inset-0 flex items-center justify-center bg-black/45 text-white">
          <Loader2 class="h-6 w-6 animate-spin" />
        </div>
        <div class="absolute right-3 top-3 flex gap-2">
          <button
            type="button"
            class="rounded-lg bg-white/95 p-2 text-stone-700 shadow hover:bg-white"
            :title="t('upload.changeImage')"
            :disabled="uploading"
            @click="openFilePicker"
          >
            <UploadCloud class="h-4 w-4" />
          </button>
          <button
            type="button"
            class="rounded-lg bg-white/95 p-2 text-red-600 shadow hover:bg-white"
            :title="t('upload.removeImage')"
            :disabled="uploading"
            @click="clearImage"
          >
            <X class="h-4 w-4" />
          </button>
        </div>
      </div>

      <button
        v-else
        type="button"
        class="flex min-h-40 w-full flex-col items-center justify-center gap-3 px-4 py-8 text-center"
        :disabled="uploading"
        @click="openFilePicker"
      >
        <Loader2 v-if="uploading" class="h-8 w-8 animate-spin text-blue-500" />
        <Image v-else class="h-8 w-8 text-stone-400" />
        <span class="text-sm font-medium text-stone-700">
          {{ uploading ? t('upload.uploading') : t('upload.chooseOrDrag') }}
        </span>
        <span class="text-xs text-stone-500">{{ t('upload.formatHint') }}</span>
      </button>
    </div>

    <input
      v-model="urlValue"
      type="url"
      class="w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
      :placeholder="inputPlaceholder"
      @input="updateUrl"
    >
    <p v-if="uploadError" class="text-xs text-red-600">{{ uploadError }}</p>
  </div>
</template>
