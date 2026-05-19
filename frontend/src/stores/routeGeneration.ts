import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useRouteGenerationStore = defineStore('routeGeneration', () => {
  const isGenerating = ref(false)
  const isCompleted = ref(false)
  const completedTimestamp = ref(0)

  const hasUnreadResult = computed(() => isCompleted.value)

  function startGeneration() {
    isGenerating.value = true
    isCompleted.value = false
    completedTimestamp.value = 0
  }

  function completeGeneration() {
    isGenerating.value = false
    isCompleted.value = true
    completedTimestamp.value = Date.now()
  }

  function cancelGeneration() {
    isGenerating.value = false
    isCompleted.value = false
    completedTimestamp.value = 0
  }

  function acknowledgeResult() {
    isCompleted.value = false
  }

  function reset() {
    isGenerating.value = false
    isCompleted.value = false
    completedTimestamp.value = 0
  }

  return {
    isGenerating,
    isCompleted,
    completedTimestamp,
    hasUnreadResult,
    startGeneration,
    completeGeneration,
    cancelGeneration,
    acknowledgeResult,
    reset
  }
})
