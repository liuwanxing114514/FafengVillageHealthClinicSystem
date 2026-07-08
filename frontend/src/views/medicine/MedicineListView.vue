<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchMedicines, updateMedicineStatus, deleteMedicine } from '@/api/medicine'
import type { MedicineListItem } from '@/types/medicine'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const records = ref<MedicineListItem[]>([])

async function loadList() {
  loading.value = true
  try {
    const result = await searchMedicines({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value || undefined,
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
  router.push('/medicine/new')
}

function goEdit(id: number) {
  router.push(`/medicine/${id}`)
}

async function toggleStatus(row: MedicineListItem) {
  const nextStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  const action = nextStatus === 'INACTIVE' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}「${row.name}」吗？`, '确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await updateMedicineStatus(row.id, nextStatus)
    ElMessage.success(`已${action}`)
    await loadList()
  } catch {
    // cancelled or failed
  }
}

async function onDelete(row: MedicineListItem) {
  if (row.status !== 'INACTIVE') {
    ElMessage.warning('请先停用药品后再删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.name}」吗？删除后不可恢复，条码将被释放。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteMedicine(row.id)
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
    <el-card shadow="hover">
      <template #header>
        <div class="header-row">
          <span class="title">药品管理</span>
          <div class="actions">
            <el-button link type="primary" @click="router.push('/')">返回首页</el-button>
            <el-button type="primary" @click="goCreate">新增药品</el-button>
          </div>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索名称、拼音首字母或条码"
          clearable
          style="width: 280px"
          @keyup.enter="onSearch"
        />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
        <el-button type="primary" @click="onSearch">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="name" label="药品名称" min-width="140" />
        <el-table-column prop="specification" label="规格" min-width="100" />
        <el-table-column prop="dosageForm" label="剂型" width="90" />
        <el-table-column label="单位" width="100">
          <template #default="{ row }">
            {{ row.baseUnit }}
            <span v-if="row.packageUnit && row.packageUnit !== row.baseUnit">
              / {{ row.packageUnit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="purchasePrice" label="进货单价" width="100" />
        <el-table-column label="库存下限" width="130">
          <template #default="{ row }">
            {{ row.stockThreshold }} {{ row.baseUnit }}
            <span v-if="row.packageUnit !== row.baseUnit" class="sub">
              (约 {{ row.stockThresholdInPackages }} {{ row.packageUnit }})
            </span>
          </template>
        </el-table-column>
        <el-table-column label="条码" min-width="120">
          <template #default="{ row }">
            {{ row.barcodes?.join('、') || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goEdit(row.id)">编辑</el-button>
            <el-button link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.status !== 'INACTIVE'"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
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
  font-size: 1.25rem;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.sub {
  color: #909399;
  font-size: 0.85rem;
}
</style>
