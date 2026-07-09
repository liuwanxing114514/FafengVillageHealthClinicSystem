import { onMounted, onUnmounted, ref } from 'vue'

export function useBreakpoint(query = '(max-width: 768px)') {
  const isMobile = ref(false)
  let mediaQuery: MediaQueryList | null = null

  function update() {
    isMobile.value = mediaQuery?.matches ?? false
  }

  onMounted(() => {
    mediaQuery = window.matchMedia(query)
    update()
    mediaQuery.addEventListener('change', update)
  })

  onUnmounted(() => {
    mediaQuery?.removeEventListener('change', update)
  })

  return { isMobile }
}
