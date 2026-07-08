<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MENU_ITEMS } from '@/config/menu'
import {
  ChatDotRound,
  Download,
  FirstAidKit,
  Fold,
  Expand,
  HomeFilled,
  List,
  Setting,
  Upload,
  User,
  Warning,
} from '@element-plus/icons-vue'

defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  toggle: []
}>()

const route = useRoute()
const router = useRouter()

const iconMap: Record<string, typeof HomeFilled> = {
  HomeFilled,
  User,
  FirstAidKit,
  Download,
  Upload,
  List,
  Warning,
  ChatDotRound,
  Setting,
}

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/patient')) return '/patient'
  if (path.startsWith('/medicine')) return '/medicine'
  if (path.startsWith('/inventory/inbound')) return '/inventory/inbound'
  if (path.startsWith('/inventory/outbound')) return '/inventory/outbound'
  if (path.startsWith('/inventory/flows')) return '/inventory/flows'
  if (path.startsWith('/inventory/alerts')) return '/inventory/alerts'
  if (path.startsWith('/ai')) return '/ai'
  if (path.startsWith('/settings')) return '/settings'
  if (path === '/') return '/'
  return path
})

function navigate(path: string) {
  router.push(path)
}
</script>

<template>
  <el-aside :width="collapsed ? '64px' : '200px'" class="sidebar">
    <div class="brand">
      <span v-if="!collapsed" class="brand-text">发凤村卫生室</span>
      <el-button link class="collapse-btn" @click="emit('toggle')">
        <el-icon :size="18">
          <Expand v-if="collapsed" />
          <Fold v-else />
        </el-icon>
      </el-button>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      class="sidebar-menu"
      @select="navigate"
    >
      <el-menu-item v-for="item in MENU_ITEMS" :key="item.path" :index="item.path">
        <el-icon><component :is="iconMap[item.icon]" /></el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  background: #304156;
  transition: width 0.2s;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 12px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-text {
  font-size: 0.9rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.collapse-btn {
  color: #fff !important;
  flex-shrink: 0;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 200px;
}
</style>

<style>
.sidebar .el-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #bfcbd9;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-active-color: #409eff;
}

.sidebar .el-menu-item.is-active {
  background: rgba(64, 158, 255, 0.15) !important;
}
</style>
