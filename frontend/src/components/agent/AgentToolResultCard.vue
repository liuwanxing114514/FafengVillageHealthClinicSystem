<script setup lang="ts">
import { computed } from 'vue'
import type { AgentToolCall } from '@/types/agent'
import { toolLabel } from '@/utils/agentLabels'

const props = defineProps<{
  toolCalls: AgentToolCall[]
}>()

interface ParsedData {
  medicineName?: string
  totalStock?: string
  baseUnit?: string
  specification?: string
  batches?: { batchNo?: string; quantity?: string; expiryDate?: string }[]
  items?: Record<string, unknown>[]
  total?: number
  count?: number
}

function parseData(call: AgentToolCall): ParsedData | null {
  if (!call.dataJson) return null
  try {
    return JSON.parse(call.dataJson) as ParsedData
  } catch {
    return null
  }
}

const inventoryCall = computed(() =>
  props.toolCalls.find((c) => c.toolName === 'queryInventory' && c.success),
)
const expiringCall = computed(() =>
  props.toolCalls.find((c) => c.toolName === 'queryExpiringMedicine' && c.success),
)
const patientCall = computed(() =>
  props.toolCalls.find((c) => c.toolName === 'searchPatient' && c.success),
)

const inventoryData = computed(() => (inventoryCall.value ? parseData(inventoryCall.value) : null))
const expiringData = computed(() => (expiringCall.value ? parseData(expiringCall.value) : null))
const patientData = computed(() => (patientCall.value ? parseData(patientCall.value) : null))

const expiringRows = computed(() => {
  const items = expiringData.value?.items ?? []
  return items.slice(0, 5).map((row) => ({
    name: String(row.medicineName ?? ''),
    batch: String(row.batchNo ?? ''),
    expiry: String(row.expiryDate ?? ''),
    qty: `${row.quantity ?? ''}${row.baseUnit ?? ''}`,
  }))
})

const patientRows = computed(() => {
  const items = patientData.value?.items ?? []
  return items.slice(0, 5).map((row) => ({
    name: String(row.name ?? ''),
    gender: String(row.gender ?? ''),
    age: row.age != null ? String(row.age) : '',
  }))
})
</script>

<template>
  <div v-if="inventoryData" class="result-card">
    <div class="card-title">{{ toolLabel('queryInventory') }}</div>
    <div class="card-main">{{ inventoryData.medicineName }}</div>
    <div class="card-sub">
      总库存 {{ inventoryData.totalStock }}{{ inventoryData.baseUnit }}
      <span v-if="inventoryData.batches?.length"> · {{ inventoryData.batches.length }} 个批次</span>
    </div>
  </div>

  <div v-if="expiringRows.length" class="result-card">
    <div class="card-title">{{ toolLabel('queryExpiringMedicine') }}</div>
    <el-table :data="expiringRows" size="small" stripe>
      <el-table-column prop="name" label="药品" min-width="100" />
      <el-table-column prop="batch" label="批号" width="90" />
      <el-table-column prop="expiry" label="效期" width="100" />
      <el-table-column prop="qty" label="数量" width="80" />
    </el-table>
  </div>

  <div v-if="patientRows.length" class="result-card">
    <div class="card-title">{{ toolLabel('searchPatient') }}</div>
    <el-table :data="patientRows" size="small" stripe>
      <el-table-column prop="name" label="姓名" min-width="80" />
      <el-table-column prop="gender" label="性别" width="60" />
      <el-table-column prop="age" label="年龄" width="60" />
    </el-table>
  </div>
</template>

<style scoped>
.result-card {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  text-align: left;
}

.card-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.card-main {
  font-weight: 600;
  color: #303133;
}

.card-sub {
  font-size: 13px;
  color: #606266;
  margin-top: 4px;
}
</style>
