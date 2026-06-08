import { describe, expect, it } from 'vitest'
import aiRouteFloatingBallSource from './AiRouteFloatingBall.vue?raw'

describe('AI route floating ball Live2D loading safeguards', () => {
  it('keeps the Live2D character behind desktop interaction or idle loading', () => {
    expect(aiRouteFloatingBallSource).toContain('const live2dRequested = ref(false)')
    expect(aiRouteFloatingBallSource).toContain('const shouldRenderLive2d = computed(() => isDesktop.value && live2dRequested.value)')
    expect(aiRouteFloatingBallSource).toContain('function requestLive2dLoad()')
    expect(aiRouteFloatingBallSource).toContain('if (!isDesktop.value || live2dRequested.value) return')
    expect(aiRouteFloatingBallSource).toContain('function scheduleIdleLive2dLoad()')
    expect(aiRouteFloatingBallSource).toContain('const idleLive2dDelay = 2200')
    expect(aiRouteFloatingBallSource).toContain('requestIdleCallback')
    expect(aiRouteFloatingBallSource).toContain('v-if="shouldRenderLive2d"')
  })

  it('keeps user focus, hover, and click as explicit load triggers', () => {
    expect(aiRouteFloatingBallSource).toContain('@click="handleClick"')
    expect(aiRouteFloatingBallSource).toContain('@focus="onCharacterFocus"')
    expect(aiRouteFloatingBallSource).toContain('@mouseenter="onCharacterEnter"')
    expect(aiRouteFloatingBallSource).toContain('requestLive2dLoad()')
  })

  it('keeps mobile from instantiating the Live2D component', () => {
    expect(aiRouteFloatingBallSource).toContain('const isDesktop = ref(false)')
    expect(aiRouteFloatingBallSource).toContain('const showMobileLiteButton = computed(() => viewportReady.value && !isDesktop.value)')
    expect(aiRouteFloatingBallSource).toContain('v-if="showDesktopCharacterShell"')
    expect(aiRouteFloatingBallSource).toContain('mobile-lite-btn')
  })
})
