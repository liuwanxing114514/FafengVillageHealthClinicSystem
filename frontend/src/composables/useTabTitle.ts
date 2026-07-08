import { onMounted, watch, type MaybeRefOrGetter, toValue } from 'vue'
import { useRoute } from 'vue-router'
import { useTabsStore } from '@/stores/tabs'

/** 详情页加载数据后更新当前标签标题，例如「患者：张三」 */
export function useTabTitle(title: MaybeRefOrGetter<string>) {
  const route = useRoute()
  const tabsStore = useTabsStore()

  function apply() {
    const value = toValue(title)
    if (value) {
      tabsStore.updateTitle(route.fullPath, value)
    }
  }

  onMounted(apply)
  watch(() => toValue(title), apply)
}
