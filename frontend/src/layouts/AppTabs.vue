<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useTabsStore } from '@/stores/tabs'

const router = useRouter()
const tabsStore = useTabsStore()
const { tabs, activePath } = storeToRefs(tabsStore)

function onTabClick(path: string) {
  if (path !== activePath.value) {
    router.push(path)
  }
}

function onTabRemove(path: string) {
  const next = tabsStore.removeTab(path)
  if (next !== null && path === router.currentRoute.value.fullPath) {
    router.push(next)
  }
}
</script>

<template>
  <div v-if="tabs.length" class="tabs-bar">
    <div class="tabs-scroll">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="tab-item"
        :class="{ active: tab.path === activePath }"
        @click="onTabClick(tab.path)"
      >
        <span class="tab-title">{{ tab.title }}</span>
        <button
          v-if="tabs.length > 1"
          type="button"
          class="tab-close"
          aria-label="关闭标签"
          @click.stop="onTabRemove(tab.path)"
        >
          ×
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tabs-bar {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 6px 8px 0;
}

.tabs-scroll {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
  overflow-x: auto;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 180px;
  padding: 6px 10px;
  border: 1px solid #e4e7ed;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  background: #f5f7fa;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.tab-item.active {
  background: #fff;
  color: #409eff;
  border-color: #dcdfe6;
  font-weight: 500;
}

.tab-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tab-close {
  border: none;
  background: transparent;
  color: #909399;
  font-size: 16px;
  line-height: 1;
  padding: 0 2px;
  cursor: pointer;
}

.tab-close:hover {
  color: #f56c6c;
}
</style>
