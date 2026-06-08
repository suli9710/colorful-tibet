<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Image, Loader2, UploadCloud, X } from 'lucide-vue-next'
import api from '@/api'
import { summarizeClientError } from '@/utils/errorMonitoring'

const MAX_UPLOAD_BYTES = 5 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])
const ALLOWED_IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.webp']
const uploadFallbacks = {
  pasteUrl: '粘贴图片 URL',
  selectImageFile: '请选择 JPG、PNG 或 WebP 图片',
  imageTooLarge: '图片不能超过 5MB',
  missingUploadUrl: '上传完成但未返回图片地址，请重试',
  uploadFailed: '上传失败，请检查网络后重试',
  previewAlt: '已选择图片预览',
  imageLoadFailed: '图片预览加载失败',
  changeImage: '更换图片',
  removeImage: '移除图片',
  uploading: '上传中...',
  chooseOrDrag: '选择图片或拖到这里',
  formatHint: '支持 JPG、PNG、WebP，最大 5MB',
  retry: '重试上传'
} as const
let nextUploadFieldId = 1

const props = withDefaults(defineProps<{
  modelValue: string
  uploadEndpoint?: string
  placeholder?: string
  disabled?: boolean
}>(), {
  uploadEndpoint: '/admin/upload-image',
  placeholder: '',
  disabled: false
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
const retryFile = ref<File | null>(null)
const fieldId = `image-upload-${nextUploadFieldId++}`
const errorId = `${fieldId}-error`
const statusId = `${fieldId}-status`
const uploadLabel = (key: keyof typeof uploadFallbacks) => {
  const i18nKey = `upload.${key}`
  const translated = t(i18nKey)
  return translated === i18nKey ? uploadFallbacks[key] : translated
}
const uploadFailedLabel = () => {
  const translated = t('upload.uploadFailed')
  return translated === 'upload.uploadFailed' ? uploadFallbacks.uploadFailed : translated
}
const inputPlaceholder = computed(() => props.placeholder || uploadLabel('pasteUrl'))
const isDisabled = computed(() => props.disabled || uploading.value)
const describedBy = computed(() => [statusId, uploadError.value ? errorId : ''].filter(Boolean).join(' '))
const statusMessage = computed(() => {
  if (uploading.value) return uploadLabel('uploading')
  if (imageError.value) return uploadLabel('imageLoadFailed')
  return ''
})

watch(() => props.modelValue, value => {
  urlValue.value = value || ''
  imageError.value = false
})

const openFilePicker = () => {
  if (!isDisabled.value) {
    fileInput.value?.click()
  }
}

const updateUrl = () => {
  imageError.value = false
  uploadError.value = ''
  emit('update:modelValue', urlValue.value.trim())
}

const clearImage = () => {
  if (isDisabled.value) return
  urlValue.value = ''
  imageError.value = false
  uploadError.value = ''
  retryFile.value = null
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
  if (isDisabled.value) {
    return
  }

  const file = event.dataTransfer?.files?.[0]
  if (file) {
    await uploadFile(file)
  }
}

const handleDragOver = () => {
  if (!isDisabled.value) {
    dragging.value = true
  }
}

const retryUpload = async () => {
  if (!retryFile.value || isDisabled.value) return
  await uploadFile(retryFile.value, false)
}

const hasAllowedImageExtension = (fileName: string) => {
  const normalizedFileName = fileName.toLowerCase()
  return ALLOWED_IMAGE_EXTENSIONS.some(extension => normalizedFileName.endsWith(extension))
}

const uploadFile = async (file: File, rememberForRetry = true) => {
  uploadError.value = ''
  imageError.value = false

  if (!ALLOWED_IMAGE_TYPES.has(file.type) || !hasAllowedImageExtension(file.name)) {
    retryFile.value = null
    uploadError.value = uploadLabel('selectImageFile')
    return
  }

  if (file.size > MAX_UPLOAD_BYTES) {
    retryFile.value = null
    uploadError.value = uploadLabel('imageTooLarge')
    return
  }

  if (rememberForRetry) {
    retryFile.value = file
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post(props.uploadEndpoint, formData)
    const imageUrl = response.data?.imageUrl || response.data?.url
    if (!imageUrl) {
      uploadError.value = uploadLabel('missingUploadUrl')
      return
    }
    urlValue.value = imageUrl
    retryFile.value = null
    emit('update:modelValue', imageUrl)
  } catch (error) {
    if (import.meta.env.DEV) {
      console.warn('Image upload failed:', summarizeClientError(error))
    }
    uploadError.value = uploadFailedLabel()
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="space-y-3">
    <div
      class="relative overflow-hidden rounded-lg border border-dashed bg-stone-50 transition-colors"
      :class="[
        dragging ? 'border-blue-500 bg-blue-50' : 'border-stone-300 hover:border-blue-400',
        isDisabled ? 'cursor-not-allowed opacity-70' : ''
      ]"
      :aria-busy="uploading"
      :aria-disabled="isDisabled"
      :aria-describedby="describedBy"
      @dragover.prevent="handleDragOver"
      @dragleave.prevent="dragging = false"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        class="hidden"
        :disabled="isDisabled"
        @change="handleFileInput"
      >

      <div v-if="modelValue" class="relative h-48 bg-stone-100">
        <img
          :src="modelValue"
          :alt="uploadLabel('previewAlt')"
          class="h-full w-full object-cover"
          @error="imageError = true"
          @load="imageError = false"
        >
        <div v-if="imageError" role="alert" class="absolute inset-0 flex items-center justify-center bg-red-50 text-sm text-red-600">
          {{ uploadLabel('imageLoadFailed') }}
        </div>
        <div v-if="uploading" role="status" aria-live="polite" class="absolute inset-0 flex items-center justify-center bg-black/45 text-white">
          <Loader2 class="h-6 w-6 animate-spin" />
          <span class="sr-only">{{ uploadLabel('uploading') }}</span>
        </div>
        <div class="absolute right-3 top-3 flex gap-2">
          <button
            type="button"
            class="rounded-lg bg-white/95 p-2 text-stone-700 shadow hover:bg-white"
            :title="uploadLabel('changeImage')"
            :aria-label="uploadLabel('changeImage')"
            :disabled="isDisabled"
            @click="openFilePicker"
          >
            <UploadCloud class="h-4 w-4" />
          </button>
          <button
            type="button"
            class="rounded-lg bg-white/95 p-2 text-red-600 shadow hover:bg-white"
            :title="uploadLabel('removeImage')"
            :aria-label="uploadLabel('removeImage')"
            :disabled="isDisabled"
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
        :disabled="isDisabled"
        @click="openFilePicker"
      >
        <Loader2 v-if="uploading" class="h-8 w-8 animate-spin text-blue-500" />
        <Image v-else class="h-8 w-8 text-stone-400" />
        <span class="text-sm font-medium text-stone-700">
          {{ uploading ? uploadLabel('uploading') : uploadLabel('chooseOrDrag') }}
        </span>
        <span class="text-xs text-stone-500">{{ uploadLabel('formatHint') }}</span>
      </button>
    </div>

    <input
      v-model="urlValue"
      type="url"
      class="w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
      :placeholder="inputPlaceholder"
      :disabled="isDisabled"
      :aria-invalid="Boolean(uploadError)"
      :aria-describedby="describedBy"
      @input="updateUrl"
    >
    <p :id="statusId" role="status" aria-live="polite" class="sr-only">{{ statusMessage }}</p>
    <div v-if="uploadError" :id="errorId" role="alert" class="flex flex-wrap items-center gap-2 text-xs text-red-600">
      <span>{{ uploadError }}</span>
      <button
        v-if="retryFile"
        type="button"
        class="rounded-lg border border-red-200 bg-white px-2.5 py-1 font-semibold text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="isDisabled"
        @click="retryUpload"
      >
        {{ uploadLabel('retry') }}
      </button>
    </div>
  </div>
</template>
