import { onMounted, onUnmounted } from 'vue'

let sharedObserver: IntersectionObserver | null = null
let isInitialized = false

export function useScrollAnimation() {
  const init = () => {
    if (sharedObserver) {
      sharedObserver.disconnect()
    }

    sharedObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('revealed')
          sharedObserver?.unobserve(entry.target)
        }
      })
    }, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' })

    document.querySelectorAll('.animate-on-scroll:not(.revealed)').forEach(el => {
      sharedObserver!.observe(el)
    })
  }

  onMounted(() => {
    if (!isInitialized) {
      init()
      isInitialized = true
    }
  })

  onUnmounted(() => {
    sharedObserver?.disconnect()
    isInitialized = false
  })

  return { init }
}
