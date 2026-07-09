<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchVisits } from '@/api/visit'
import type { VisitListItem } from '@/types/visit'

const router = useRouter()
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const records = ref<VisitListItem[]>([])

const filters = reactive({
  keyword: '',
  dateFrom: '',
  dateTo: '',
  arrearsOnly: false,
})

function genderLabel(gender: string) {
  if (gender === 'M') return '男'
  if (gender === 'F') return '女'
  return '未知'
}

function formatDateTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

function formatMoney(value: number) {
  return Number(value ?? 0).toFixed(2)
}

async function loadList() {
  loading.value = true
  try {
    const result = await searchVisits({
      keyword: filters.keyword.trim() || undefined,
      dateFrom: filters.dateFrom || undefined,
      dateTo: filters.dateTo || undefined,
      arrearsOnly: filters.arrearsOnly || undefined,
      page: page.value,
      size: pageSize.value,
    })
    records.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  loadList()
}

function onReset() {
  filters.keyword = ''
  filters.dateFrom = ''
  filters.dateTo = ''
  filters.arrearsOnly = false
  onSearch()
}

function goCreate() {
  router.push('/visit/new')
}

function goDetail(id: number) {
  router.push(`/visit/${id}`)
}

function goPatient(patientId: number) {
  router.push(`/patient/${patientId}`)
}

onMounted(loadList)
</script>

<template>
  <main class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span class="title">病历管理</span>
          <el-button type="primary" @click="goCreate">新建病历</el-button>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索患者姓名、电话、诊断、主诉"
          clearable
          class="keyword"
          @keyup.enter="onSearch"
        />
        <el-date-picker
          v-model="filters.dateFrom"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="开始日期"
          clearable
        />
        <el-date-picker
          v-model="filters.dateTo"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="结束日期"
          clearable
        />
        <el-checkbox v-model="filters.arrearsOnly">仅欠款</el-checkbox>
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe empty-text="暂无病历">
        <el-table-column label="就诊时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.visitTime) }}</template>
        </el-table-column>
        <el-table-column label="患者" min-width="100">
          <template #default="{ row }">
            <el-link type="primary" @click="goPatient(row.patientId)">{{ row.patientName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ genderLabel(row.patientGender) }}</template>
        </el-table-column>
        <el-table-column prop="diagnosis" label="诊断" min-width="120" show-overflow-tooltip />
        <el-table-column label="应收" width="90" align="right">
          <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
        </el-table-column>
        <el-table-column label="实收" width="90" align="right">
          <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
        </el-table-column>
        <el-table-column label="欠款" width="90" align="right">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.balance > 0 }">{{ formatMoney(row.balance) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadList"
          @size-change="onSearch"
        />
      </div>
    </el-card>
  </main>
</template>

<style scoped>
.page {
  padding: 16px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

.keyword {
  width: 280px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.text-danger {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
