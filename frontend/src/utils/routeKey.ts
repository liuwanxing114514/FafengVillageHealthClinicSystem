import type { RouteLocationNormalized } from 'vue-router'

/** AI 助手仅 query 变化时复用同一 keep-alive 实例，避免 /ai 与 /ai?c= 双实例 */
export function routeComponentKey(route: RouteLocationNormalized): string {
  if (route.name === 'ai-assistant') return '/ai'
  return route.fullPath
}

/** 标签栏路径：AI 助手不随 ?c= 拆成多个标签 */
export function routeTabPath(route: RouteLocationNormalized): string {
  if (route.name === 'ai-assistant') return '/ai'
  return route.fullPath
}
