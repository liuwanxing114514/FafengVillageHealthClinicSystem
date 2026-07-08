<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getInventoryAlerts } from '@/api/inventory'
import type { InventoryAlerts } from '@/types/inventory'

const router = useRouter()
const loading = ref(false)
const alerts = ref<InventoryAlerts | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    alerts.value = await getInventoryAlerts()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span>库存预警</span>
          <el-button @click="router.push('/')">返回首页</el-button>
        </div>
      </template>

      <h3>库存不足</h3>
      <el-table :data="alerts?.lowStock ?? []" empty-text="暂无">
        <el-table-column prop="medicineName" label="药品" />
        <el-table-column prop="specification" label="规格" />
        <el-table-column label="当前/下限">
          <template #default="{ row }">{{ row.currentQuantity }} / {{ row.threshold }} {{ row.baseUnit }}</template>
        </el-table-column>
      </el-table>

      <h3 style="margin-top: 24px">临期（3个月内）</h3>
      <el-table :data="alerts?.expiring ?? []" empty-text="暂无">
        <el-table-column prop="medicineName" label="药品" />
        <el-table-column prop="batchNo" label="批号" />
        <el-table-column prop="expiryDate" label="有效期" />
        <el-table-column label="数量">
          <template #default="{ row }">{{ row.quantity }} {{ row.baseUnit }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.header-row { display: flex; justify-content: space-between; }
h3 { margin: 0 0 8px; font-size: 1rem; }
</style>
