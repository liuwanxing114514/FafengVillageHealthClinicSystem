import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'
import { MAX_TABS } from '@/config/menu'
import { routeTabPath } from '@/utils/routeKey'

export interface TabItem {
  path: string
  title: string
}

function resolveTitle(route: RouteLocationNormalized, customTitle?: string): string {
  if (customTitle) return customTitle
  if (typeof route.meta.title === 'string') return route.meta.title

  const name = route.name as string | undefined
  if (name === 'patient-detail') {
    return route.params.id === 'new' ? '新建患者' : `患者 #${route.params.id}`
  }
  if (name === 'medicine-edit') {
    return route.params.id === 'new' ? '新建药品' : `药品 #${route.params.id}`
  }
  if (name === 'visit-form') {
    return route.params.id === 'new' ? '新建病历' : `病历 #${route.params.id}`
  }
  if (name === 'prescription-edit') {
    return route.params.id === 'new' ? '新建处方' : `处方 #${route.params.id}`
  }
  if (name === 'inventory-outbound' && route.query.prescriptionId) {
    return `出库 · 处方 #${route.query.prescriptionId}`
  }
  return route.path
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<TabItem[]>([])
  const activePath = ref('')

  function syncFromRoute(route: RouteLocationNormalized) {
    if (route.meta.standalone || route.meta.public) return

    const path = routeTabPath(route)
    const title = resolveTitle(route)

    // 合并旧版 /ai?c= 标签，避免顶部出现两个「AI 助手」
    if (route.name === 'ai-assistant') {
      tabs.value = tabs.value.filter((t) => !t.path.startsWith('/ai'))
    }

    const existing = tabs.value.find((t) => t.path === path)
    if (!existing) {
      if (tabs.value.length >= MAX_TABS) {
        tabs.value.shift()
      }
      tabs.value.push({ path, title })
    }
    activePath.value = path
  }

  function updateTitle(path: string, title: string) {
    const tab = tabs.value.find((t) => t.path === path)
    if (tab && title) tab.title = title
  }

  function setActive(path: string) {
    activePath.value = path
  }

  function removeTab(path: string): string | null {
    const index = tabs.value.findIndex((t) => t.path === path)
    if (index < 0) return null

    tabs.value.splice(index, 1)
    if (activePath.value !== path) return null

    if (!tabs.value.length) return '/'
    const next = tabs.value[Math.min(index, tabs.value.length - 1)]
    activePath.value = next.path
    return next.path
  }

  function reset() {
    tabs.value = []
    activePath.value = ''
  }

  return {
    tabs,
    activePath,
    syncFromRoute,
    updateTitle,
    setActive,
    removeTab,
    reset,
    resolveTitle,
  }
})
