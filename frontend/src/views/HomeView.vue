<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDashboardSummary } from '@/api/inventory'
import type { DashboardSummary } from '@/types/inventory'

const router = useRouter()
const loading = ref(false)
const summary = ref<DashboardSummary | null>(null)
const backendStatus = ref('检查中…')

onMounted(async () => {
  loading.value = true
  try {
    summary.value = await getDashboardSummary()
    try {
      const res = await fetch('/api/health')
      const data = await res.json()
      backendStatus.value = data.status === 'UP' ? '正常' : data.status
    } catch {
      backendStatus.value = '未连接'
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="hover">
      <template #header>
        <div class="header-row">
          <span class="title">发凤村卫生室诊所系统</span>
          <div class="nav">
            <el-button link type="primary" @click="router.push('/patient')">患者</el-button>
            <el-button link type="primary" @click="router.push('/medicine')">药品</el-button>
            <el-button link type="primary" @click="router.push('/inventory/inbound')">入库</el-button>
            <el-button link type="primary" @click="router.push('/inventory/outbound')">出库</el-button>
            <el-button link type="primary" @click="router.push('/inventory/alerts')">预警</el-button>
            <el-button link type="primary" @click="router.push('/settings')">设置</el-button>
          </div>
        </div>
      </template>

      <p class="version">v0.8 Excel 模板导入</p>
      <p class="hint">后端：{{ backendStatus }}</p>

      <div v-if="summary" class="stats">
        <el-statistic title="库存不足" :value="summary.lowStockCount" />
        <el-statistic title="临期批次（3个月内）" :value="summary.expiringCount" />
      </div>

      <div v-if="summary && summary.lowStockPreview.length" class="section">
        <h3>库存不足</h3>
        <el-table :data="summary.lowStockPreview" size="small">
          <el-table-column prop="medicineName" label="药品" />
          <el-table-column label="当前/下限">
            <template #default="{ row }">{{ row.currentQuantity }} / {{ row.threshold }} {{ row.baseUnit }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="summary && summary.expiringPreview.length" class="section">
        <h3>临期药品</h3>
        <el-table :data="summary.expiringPreview" size="small">
          <el-table-column prop="medicineName" label="药品" />
          <el-table-column prop="batchNo" label="批号" />
          <el-table-column prop="expiryDate" label="有效期" />
          <el-table-column label="数量">
            <template #default="{ row }">{{ row.quantity }} {{ row.baseUnit }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="links">
        <el-button @click="router.push('/inventory/flows')">库存流水</el-button>
      </div>
    </el-card>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.nav {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.title {
  font-size: 1.25rem;
  font-weight: 600;
}

.version {
  margin: 0 0 8px;
  color: #409eff;
  font-weight: 500;
}

.hint {
  margin: 0 0 16px;
  color: #606266;
}

.stats {
  display: flex;
  gap: 32px;
  margin-bottom: 20px;
}

.section {
  margin-bottom: 20px;
}

.section h3 {
  margin: 0 0 8px;
  font-size: 1rem;
}

.links {
  margin-top: 16px;
}
</style>
