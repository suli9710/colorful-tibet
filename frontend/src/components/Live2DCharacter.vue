<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { summarizeClientError } from '../utils/errorMonitoring'

declare global {
  interface Window {
    CapybaraLuluV2?: {
      CapybaraLuluLive2D: new (...args: any[]) => any
      mount: (container: string | Element, options: Record<string, unknown>) => Promise<any>
    }
  }
}

interface WidgetInstance {
  setMotion: (name: string) => boolean
  resize: (width: number, height: number) => void
  dispose: () => void
}

const props = withDefaults(defineProps<{
  motion?: string
  width?: number
  height?: number
}>(), {
  motion: 'idle',
  width: 160,
  height: 173
})

const emit = defineEmits<{
  ready: [widget: WidgetInstance]
}>()

const motionToGif: Record<string, string> = {
  idle: '/live2d/previews/idle.gif',
  waving: '/live2d/previews/waving.gif',
  jumping: '/live2d/previews/jumping.gif',
  failed: '/live2d/previews/failed.gif',
  waiting: '/live2d/previews/waiting.gif',
  running: '/live2d/previews/running.gif',
  runningRight: '/live2d/previews/running-right.gif',
  runningLeft: '/live2d/previews/running-left.gif',
  review: '/live2d/previews/review.gif'
}

const containerRef = ref<HTMLDivElement>()
const widget = ref<WidgetInstance | null>(null)
const scriptLoaded = ref(false)
const loadError = ref(false)

const fallbackSrc = computed(() => motionToGif[props.motion] || motionToGif.idle)

async function loadScript(): Promise<void> {
  if (window.CapybaraLuluV2) {
    scriptLoaded.value = true
    return
  }

  return new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-live2d-widget]')
    if (existing) {
      scriptLoaded.value = true
      resolve()
      return
    }

    const script = document.createElement('script')
    script.src = '/live2d/widget.js'
    script.setAttribute('data-live2d-widget', 'capybara-lulu')
    script.onload = () => {
      scriptLoaded.value = true
      resolve()
    }
    script.onerror = () => {
      loadError.value = true
      reject(new Error('Failed to load Live2D widget script'))
    }
    document.head.appendChild(script)
  })
}

async function initWidget() {
  if (!containerRef.value || !scriptLoaded.value || !window.CapybaraLuluV2) return

  try {
    const instance = await window.CapybaraLuluV2.mount(containerRef.value, {
      model: '/live2d/persimmon-hippo.model.json',
      width: props.width,
      height: props.height,
      autoInteract: false
    }) as WidgetInstance

    widget.value = instance
    instance.setMotion(props.motion)
    emit('ready', instance)
  } catch (err) {
    console.error('Live2D widget init failed:', summarizeClientError(err))
    loadError.value = true
  }
}

watch(() => props.motion, (newMotion) => {
  if (widget.value) {
    widget.value.setMotion(newMotion)
  }
})

watch(() => [props.width, props.height], ([w, h]) => {
  if (widget.value) {
    widget.value.resize(w as number, h as number)
  }
})

onMounted(async () => {
  await loadScript()
  await initWidget()
})

onBeforeUnmount(() => {
  if (widget.value) {
    widget.value.dispose()
  }
})

defineExpose({
  setMotion: (name: string) => widget.value?.setMotion(name),
  resize: (w: number, h: number) => widget.value?.resize(w, h),
  getWidget: () => widget.value
})
</script>

<template>
  <div class="live2d-character-wrapper" :style="{ width: width + 'px', height: height + 'px' }">
    <div
      v-if="!loadError"
      ref="containerRef"
      class="live2d-character-container"
      role="img"
      aria-label="水豚噜噜 虚拟导游"
    ></div>
    <img
      v-else
      :src="fallbackSrc"
      :width="width"
      :height="height"
      class="live2d-fallback-gif"
      alt="水豚噜噜"
    />
  </div>
</template>

<style scoped>
.live2d-character-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}

.live2d-character-container {
  width: 100%;
  height: 100%;
}

.live2d-fallback-gif {
  object-fit: contain;
  image-rendering: auto;
}
</style>
