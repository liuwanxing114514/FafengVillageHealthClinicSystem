<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAiDraft, rejectAiDraft } from '@/api/ai'
import { useTabTitle } from '@/composables/useTabTitle'

interface OutboundItem {
  medicineId?: number
  medicineName?: string
  specification?: string
  quantity?: string
  unit?: string
  usage?: string
}

interface OutboundPayload {
  prescriptionId?: number
  patientId?: number
  patientName?: string
  diagnosis?: string
  remark?: string
  items?: OutboundItem[]
}

const route = useRoute()
const loading = ref(false)
const draftId = computed(() => Number(route.params.id))
const payload = ref<OutboundPayload>({})
const isPending = ref(true)

useTabTitle(computed(() => `出库草稿 #${draftId.value}`))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const draft = await getAiDraft(draftId.value)
    isPending.value = draft.status === 'PENDING'
    payload.value = JSON.parse(draft.payload) as OutboundPayload
  } finally {
    loading.value = false
  }
}

async function reject() {
  await ElMessageBox.confirm('确定拒绝此出库草稿？', '确认')
  await rejectAiDraft(draftId.value)
  ElMessage.success('已拒绝')
  isPending.value = false
}
</script>

<template>
  <main class="page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span>待确认出库清单</span>
      </template>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="此为 AI 生成的待确认清单，尚未扣减库存。确认出库功能将在 v2.1 完善。"
        style="margin-bottom: 16px"
      />

      <el-descriptions v-if="payload.patientName" :column="2" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="患者">{{ payload.patientName }}</el-descriptions-item>
        <el-descriptions-item label="诊断">{{ payload.diagnosis || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="payload.prescriptionId" label="处方号">
          #{{ payload.prescriptionId }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ payload.remark || '—' }}</el-descriptions-item>
      </el-descriptions>

      <el-table :data="payload.items ?? []" stripe>
        <el-table-column prop="medicineName" label="药品" />
        <el-table-column prop="specification" label="规格" width="120" />
        <el-table-column label="数量" width="100">
          <template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column prop="usage" label="用法" />
      </el-table>

      <div v-if="isPending" class="actions">
        <el-button type="danger" @click="reject">拒绝</el-button>
      </div>
    </el-card>
  </main>
</template>

<style scoped>
.actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
