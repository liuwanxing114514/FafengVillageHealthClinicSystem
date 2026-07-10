<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/layouts/AppSidebar.vue'
import AppTabs from '@/layouts/AppTabs.vue'
import { useBreakpoint } from '@/composables/useBreakpoint'
import { MENU_ITEMS } from '@/config/menu'
import { useAuthStore } from '@/stores/auth'
import { routeComponentKey } from '@/utils/routeKey'
import {
  ChatDotRound,
  Document,
  Download,
  FirstAidKit,
  HomeFilled,
  List,
  Menu,
  Operation,
  Setting,
  Upload,
  User,
  Warning,
} from '@element-plus/icons-vue'

const collapsed = ref(false)
const drawerVisible = ref(false)
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const { operator } = storeToRefs(auth)
const { isMobile } = useBreakpoint()

const iconMap: Record<string, typeof HomeFilled> = {
  HomeFilled,
  User,
  Document,
  FirstAidKit,
  Download,
  Upload,
  List,
  Warning,
  Operation,
  ChatDotRound,
  Setting,
}

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/patient')) return '/patient'
  if (path.startsWith('/medicine')) return '/medicine'
  if (path.startsWith('/inventory/inbound')) return '/inventory/inbound'
  if (path.startsWith('/inventory/outbound/batch')) return '/inventory/outbound/batch'
  if (path.startsWith('/inventory/outbound')) return '/inventory/outbound'
  if (path.startsWith('/inventory/flows')) return '/inventory/flows'
  if (path.startsWith('/inventory/alerts')) return '/inventory/alerts'
  if (path.startsWith('/visit')) return '/visit'
  if (path.startsWith('/ai')) return '/ai'
  if (path.startsWith('/settings')) return '/settings'
  if (path === '/') return '/'
  return path
})

async function logout() {
  await auth.logout()
  router.push('/login')
}

function navigate(path: string) {
  drawerVisible.value = false
  router.push(path)
}
</script>

<template>
  <el-container class="main-layout">
    <AppSidebar v-if="!isMobile" :collapsed="collapsed" @toggle="collapsed = !collapsed" />
    <el-container direction="vertical" class="main-body">
      <header class="top-bar">
        <el-button
          v-if="isMobile"
          link
          class="menu-trigger"
          aria-label="打开菜单"
          @click="drawerVisible = true"
        >
          <el-icon :size="20"><Menu /></el-icon>
        </el-button>
        <AppTabs />
        <div class="top-bar-right">
          <span v-if="operator && !isMobile" class="operator">{{ operator }}</span>
          <el-button link type="primary" @click="logout">退出</el-button>
        </div>
      </header>
      <el-main class="main-content">
        <router-view v-slot="{ Component, route: currentRoute }">
          <keep-alive :max="10">
            <component :is="Component" :key="routeComponentKey(currentRoute)" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      size="240px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-brand">发凤村卫生室</div>
      <el-menu :default-active="activeMenu" class="drawer-menu" @select="navigate">
        <el-menu-item v-for="item in MENU_ITEMS" :key="item.path" :index="item.path">
          <el-icon><component :is="iconMap[item.icon]" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>
  </el-container>
</template>

<style scoped>
.main-layout {
  height: 100vh;
  overflow: hidden;
}

.main-body {
  min-width: 0;
  background: #f5f7fa;
}

.top-bar {
  display: flex;
  align-items: flex-end;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding-right: 12px;
  min-height: 42px;
}

.menu-trigger {
  flex-shrink: 0;
  margin: 0 4px 6px 8px;
  color: #606266 !important;
}

.top-bar :deep(.tabs-bar) {
  flex: 1;
  min-width: 0;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding-bottom: 8px;
}

.operator {
  font-size: 13px;
  color: #909399;
}

.main-content {
  padding: 16px;
  overflow: auto;
  height: calc(100vh - 42px);
}

.main-content :deep(.page) {
  min-height: auto;
  padding: 0;
}

.drawer-brand {
  height: 48px;
  line-height: 48px;
  padding: 0 16px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
}

.drawer-menu {
  border-right: none;
}

@media (max-width: 768px) {
  .main-content {
    padding: 8px;
  }
}
</style>

<style>
.mobile-drawer .el-drawer__body {
  padding: 0;
}
</style>
