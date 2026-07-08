<script setup lang="ts">
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import AppSidebar from '@/layouts/AppSidebar.vue'
import AppTabs from '@/layouts/AppTabs.vue'
import { useAuthStore } from '@/stores/auth'

const collapsed = ref(false)
const auth = useAuthStore()
const router = useRouter()
const { operator } = storeToRefs(auth)

async function logout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="main-layout">
    <AppSidebar :collapsed="collapsed" @toggle="collapsed = !collapsed" />
    <el-container direction="vertical" class="main-body">
      <header class="top-bar">
        <AppTabs />
        <div class="top-bar-right">
          <span v-if="operator" class="operator">{{ operator }}</span>
          <el-button link type="primary" @click="logout">退出</el-button>
        </div>
      </header>
      <el-main class="main-content">
        <router-view v-slot="{ Component, route: currentRoute }">
          <keep-alive :max="10">
            <component :is="Component" :key="currentRoute.fullPath" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
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
</style>
