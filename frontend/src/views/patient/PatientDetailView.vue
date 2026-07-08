<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPatient, getPatient, listPatientVisits, updatePatient } from '@/api/patient'
import type { PatientDetail } from '@/types/patient'
import type { VisitListItem } from '@/types/visit'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const detail = ref<PatientDetail | null>(null)
const visits = ref<VisitListItem[]>([])

const isNew = computed(() => route.params.id === 'new')
const patientId = computed(() => (isNew.value ? null : Number(route.params.id)))

const form = reactive({
  name: '',
  gender: 'UNKNOWN' as 'M' | 'F' | 'UNKNOWN',
  idCard: '',
  birthDate: '',
  age: null as number | null,
  phone: '',
  address: '',
  remark: '',
})

function formatDateTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

async function loadDetail() {
  if (isNew.value || !patientId.value) return
  loading.value = true
  try {
    detail.value = await getPatient(patientId.value)
    form.name = detail.value.name
    form.gender = detail.value.gender as 'M' | 'F' | 'UNKNOWN'
    form.idCard = detail.value.idCard ?? ''
    form.birthDate = detail.value.birthDate ?? ''
    form.age = detail.value.age
    form.phone = detail.value.phone
    form.address = detail.value.address
    form.remark = detail.value.remark ?? ''
    visits.value = await listPatientVisits(patientId.value)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写姓名')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      gender: form.gender,
      idCard: form.idCard.trim() || undefined,
      birthDate: form.birthDate || undefined,
      age: form.age ?? undefined,
      phone: form.phone.trim(),
      address: form.address.trim(),
      remark: form.remark.trim() || undefined,
    }
    if (isNew.value) {
      const created = await createPatient(payload)
      ElMessage.success('患者已创建')
      router.replace(`/patient/${created.id}`)
    } else if (patientId.value) {
      await updatePatient(patientId.value, payload)
      ElMessage.success('已保存')
      await loadDetail()
    }
  } finally {
    saving.value = false
  }
}

function goNewVisit() {
  if (patientId.value) {
    router.push(`/visit/new?patientId=${patientId.value}`)
  }
}

function goVisit(id: number) {
  router.push(`/visit/${id}`)
}

onMounted(loadDetail)
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span class="title">{{ isNew ? '新增患者' : '患者详情' }}</span>
          <div class="actions">
            <el-button @click="router.push('/patient')">返回列表</el-button>
            <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          </div>
        </div>
      </template>

      <el-form label-width="96px" class="form-grid">
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="性别" required>
          <el-radio-group v-model="form.gender">
            <el-radio value="M">男</el-radio>
            <el-radio value="F">女</el-radio>
            <el-radio value="UNKNOWN">未知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" maxlength="18" placeholder="填写后自动推算出生日期与年龄" />
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="无身份证时可填写"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="0" :max="150" />
          <span v-if="detail && !detail.ageManual" class="hint">系统根据身份证/出生日期计算</span>
          <span v-else class="hint">无身份证且无出生日期时需手填</span>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="住址">
          <el-input v-model="form.address" maxlength="256" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template v-if="!isNew && detail">
        <el-divider />
        <div class="section-header">
          <h3>历史病历</h3>
          <el-button type="primary" plain @click="goNewVisit">新建病历</el-button>
        </div>
        <el-table :data="visits" stripe empty-text="暂无病历">
          <el-table-column label="就诊时间" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.visitTime) }}</template>
          </el-table-column>
          <el-table-column prop="chiefComplaint" label="主诉" min-width="160" show-overflow-tooltip />
          <el-table-column prop="diagnosis" label="诊断" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="goVisit(row.id)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
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

.form-grid {
  max-width: 720px;
}

.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 0.85rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  font-size: 1rem;
}
</style>
