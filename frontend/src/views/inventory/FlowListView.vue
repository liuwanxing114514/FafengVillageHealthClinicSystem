<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listFlows } from '@/api/inventory'
import type { FlowItem } from '@/types/inventory'

const loading = ref(false)
const records = ref<FlowItem[]>([])
const total = ref(0)
const page = ref(1)

async function load() {
  loading.value = true
  try {
    const result = await listFlows({ page: page.value, size: 20 })
    records.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function flowTypeLabel(type: string) {
  if (type === 'INBOUND') return '入库'
  if (type === 'OUTBOUND') return '出库'
  if (type === 'ADJUST') return '盘点'
  return type
}

onMounted(load)
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <span>库存流水</span>
      </template>
      <el-table :data="records">
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">{{ row.createdAt.replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column prop="medicineName" label="药品" />
        <el-table-column prop="batchNo" label="批号" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">{{ flowTypeLabel(row.flowType) }}</template>
        </el-table-column>
        <el-table-column label="变动">
          <template #default="{ row }">{{ row.quantityChange }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="结余">
          <template #default="{ row }">{{ row.quantityAfter }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="80" />
      </el-table>
      <el-pagination
        v-model:current-page="page"
        :total="total"
        layout="prev, pager, next"
        style="margin-top: 16px"
        @current-change="load"
      />
    </el-card>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.header-row { display: flex; justify-content: space-between; }
</style>
