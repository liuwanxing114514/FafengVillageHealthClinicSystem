<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listFlows, exportInventoryFlows } from '@/api/inventory'
import { searchMedicines } from '@/api/medicine'
import type { FlowItem } from '@/types/inventory'
import type { MedicineListItem } from '@/types/medicine'

const loading = ref(false)
const exporting = ref(false)
const records = ref<FlowItem[]>([])
const total = ref(0)
const page = ref(1)

const filterMedicineId = ref<number | null>(null)
const filterFlowType = ref<string>('')
const medicineSearching = ref(false)
const medicineOptions = ref<MedicineListItem[]>([])

async function searchMedicineOptions(keyword: string) {
  if (!keyword.trim()) {
    medicineOptions.value = []
    return
  }
  medicineSearching.value = true
  try {
    const result = await searchMedicines({ keyword: keyword.trim(), page: 1, size: 20 })
    medicineOptions.value = result.records
  } finally {
    medicineSearching.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const result = await listFlows({
      medicineId: filterMedicineId.value ?? undefined,
      flowType: filterFlowType.value || undefined,
      page: page.value,
      size: 20,
    })
    records.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  void load()
}

function onResetFilters() {
  filterMedicineId.value = null
  filterFlowType.value = ''
  medicineOptions.value = []
  page.value = 1
  void load()
}

async function onExport() {
  exporting.value = true
  try {
    await exportInventoryFlows({
      medicineId: filterMedicineId.value ?? undefined,
      flowType: filterFlowType.value || undefined,
    })
    ElMessage.success('Excel 已保存到下载文件夹')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
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
        <div class="header-row">
          <span>库存流水</span>
          <el-button :loading="exporting" @click="onExport">导出 Excel</el-button>
        </div>
      </template>

      <el-form inline class="filters" @submit.prevent="onSearch">
        <el-form-item label="药品">
          <el-select
            v-model="filterMedicineId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="搜索药品"
            :remote-method="searchMedicineOptions"
            :loading="medicineSearching"
            style="width: 220px"
          >
            <el-option
              v-for="item in medicineOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterFlowType" clearable placeholder="全部" style="width: 120px">
            <el-option label="入库" value="INBOUND" />
            <el-option label="出库" value="OUTBOUND" />
            <el-option label="盘点" value="ADJUST" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onResetFilters">重置</el-button>
        </el-form-item>
      </el-form>

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
.page {
  min-height: 100vh;
  padding: 24px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters {
  margin-bottom: 16px;
}
</style>
