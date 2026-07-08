<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deletePatient, searchPatients } from '@/api/patient'
import type { PatientListItem } from '@/types/patient'

const router = useRouter()
const loading = ref(false)
const showAdvanced = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const records = ref<PatientListItem[]>([])

const filters = reactive({
  keyword: '',
  name: '',
  phone: '',
  idCard: '',
  address: '',
  gender: '',
  remark: '',
  ageMin: null as number | null,
  ageMax: null as number | null,
})

function genderLabel(gender: string) {
  if (gender === 'M') return '男'
  if (gender === 'F') return '女'
  return '未知'
}

function buildSearchParams() {
  return {
    keyword: filters.keyword.trim() || undefined,
    name: filters.name.trim() || undefined,
    phone: filters.phone.trim() || undefined,
    idCard: filters.idCard.trim() || undefined,
    address: filters.address.trim() || undefined,
    gender: filters.gender || undefined,
    remark: filters.remark.trim() || undefined,
    ageMin: filters.ageMin ?? undefined,
    ageMax: filters.ageMax ?? undefined,
    page: page.value,
    size: pageSize.value,
  }
}

async function loadList() {
  loading.value = true
  try {
    const result = await searchPatients(buildSearchParams())
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
  filters.name = ''
  filters.phone = ''
  filters.idCard = ''
  filters.address = ''
  filters.gender = ''
  filters.remark = ''
  filters.ageMin = null
  filters.ageMax = null
  onSearch()
}

function goCreate() {
  router.push('/patient/new')
}

function goDetail(id: number) {
  router.push(`/patient/${id}`)
}

async function onDelete(row: PatientListItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除患者「${row.name}」吗？删除后列表不再显示，历史病历仍保留。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deletePatient(row.id)
    ElMessage.success('已删除')
    await loadList()
  } catch {
    // cancelled or failed
  }
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
          v-model="filters.keyword"
          placeholder="综合搜索：姓名、电话、身份证、住址、备注、年龄"
          clearable
          style="width: 360px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="showAdvanced = !showAdvanced">
          {{ showAdvanced ? '收起条件' : '更多条件' }}
        </el-button>
        <el-button @click="onReset">重置</el-button>
      </div>

      <el-form v-show="showAdvanced" label-width="72px" class="advanced-form" inline>
        <el-form-item label="姓名">
          <el-input v-model="filters.name" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="filters.phone" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="身份证">
          <el-input v-model="filters.idCard" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="住址">
          <el-input v-model="filters.address" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="filters.gender" clearable style="width: 100px">
            <el-option label="男" value="M" />
            <el-option label="女" value="F" />
            <el-option label="未知" value="UNKNOWN" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="filters.remark" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="filters.ageMin" :min="0" :max="150" placeholder="最小" />
          <span class="age-sep">—</span>
          <el-input-number v-model="filters.ageMax" :min="0" :max="150" placeholder="最大" />
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="phone" label="电话" min-width="120" />
        <el-table-column prop="idCard" label="身份证号" min-width="180" />
        <el-table-column prop="address" label="住址" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
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
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.advanced-form {
  margin-bottom: 16px;
  padding: 12px 12px 0;
  background: #fafafa;
  border-radius: 6px;
}

.age-sep {
  margin: 0 8px;
  color: #909399;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
