<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchPatients } from '@/api/patient'
import type { PatientListItem } from '@/types/patient'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const records = ref<PatientListItem[]>([])

function genderLabel(gender: string) {
  if (gender === 'M') return '男'
  if (gender === 'F') return '女'
  return '未知'
}

async function loadList() {
  loading.value = true
  try {
    const result = await searchPatients({
      keyword: keyword.value.trim() || undefined,
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

function goCreate() {
  router.push('/patient/new')
}

function goDetail(id: number) {
  router.push(`/patient/${id}`)
}

onMounted(loadList)
</script>

<template>
  <main class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span class="title">患者管理</span>
          <div class="actions">
            <el-button @click="router.push('/')">返回首页</el-button>
            <el-button type="primary" @click="goCreate">新增患者</el-button>
          </div>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索姓名、电话或身份证号"
          clearable
          style="width: 320px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="phone" label="电话" min-width="120" />
        <el-table-column prop="idCard" label="身份证号" min-width="180" />
        <el-table-column prop="address" label="住址" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
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
  min-height: 100vh;
  padding: 24px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 1.1rem;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
