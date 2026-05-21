import { ref, onMounted, onBeforeUnmount } from 'vue'

export interface BehaviorPayload {
  mousePoints: Array<{ x: number; y: number; t: number }>
  clicks: Array<{ x: number; y: number; t: number }>
  keyIntervals: number[]
}

const MAX_MOUSE_POINTS = 200
const MAX_CLICKS = 50
const MAX_KEY_INTERVALS = 200
const THROTTLE_MS = 50

export function useBehaviorTracker() {
  const mousePoints = ref<Array<{ x: number; y: number; t: number }>>([])
  const clicks = ref<Array<{ x: number; y: number; t: number }>>([])
  const keyIntervals = ref<number[]>([])
  let lastMouseTime = 0
  let lastKeyTime = 0

  function onMouseMove(e: MouseEvent) {
    const now = performance.now()
    if (now - lastMouseTime < THROTTLE_MS) return
    lastMouseTime = now

    mousePoints.value.push({ x: e.clientX, y: e.clientY, t: Math.round(now) })
    if (mousePoints.value.length > MAX_MOUSE_POINTS) {
      mousePoints.value.shift()
    }
  }

  function onClick(e: MouseEvent) {
    clicks.value.push({ x: e.clientX, y: e.clientY, t: Math.round(performance.now()) })
    if (clicks.value.length > MAX_CLICKS) {
      clicks.value.shift()
    }
  }

  function onKeyDown() {
    const now = performance.now()
    if (lastKeyTime > 0) {
      keyIntervals.value.push(Math.round(now - lastKeyTime))
      if (keyIntervals.value.length > MAX_KEY_INTERVALS) {
        keyIntervals.value.shift()
      }
    }
    lastKeyTime = now
  }

  function getBehaviorData(): BehaviorPayload {
    return {
      mousePoints: [...mousePoints.value],
      clicks: [...clicks.value],
      keyIntervals: [...keyIntervals.value],
    }
  }

  function encodeBehaviorData(): string {
    try {
      return btoa(JSON.stringify(getBehaviorData()))
    } catch {
      return ''
    }
  }

  function reset() {
    mousePoints.value = []
    clicks.value = []
    keyIntervals.value = []
    lastMouseTime = 0
    lastKeyTime = 0
  }

  onMounted(() => {
    document.addEventListener('mousemove', onMouseMove, { passive: true })
    document.addEventListener('click', onClick, { passive: true })
    document.addEventListener('keydown', onKeyDown, { passive: true })
  })

  onBeforeUnmount(() => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('click', onClick)
    document.removeEventListener('keydown', onKeyDown)
  })

  return { getBehaviorData, encodeBehaviorData, reset }
}
