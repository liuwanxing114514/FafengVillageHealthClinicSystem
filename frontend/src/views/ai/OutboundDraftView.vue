<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveOutboundDraft,
  getAiDraft,
  parseOutboundPayload,
  rejectAiDraft,
} from '@/api/ai'
import { previewOutbound } from '@/api/inventory'
import { useTabTitle } from '@/composables/useTabTitle'
import type { OutboundDraftPayload } from '@/types/ai'
import type { OutboundPreview, OutboundPreviewLine } from '@/types/inventory'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const approving = ref(false)
const draftId = computed(() => Number(route.params.id))
const payload = ref<OutboundDraftPayload>({})
const isPending = ref(true)
const preview = ref<OutboundPreview | null>(null)
const selectedLineId = ref<number | null>(null)
const allocations = reactive<Record<number, Record<number, number>>>({})

const selectedPreviewLine = computed(() =>
  preview.value?.lines.find((l) => l.medicineId === selectedLineId.value) ?? null,
)

const canApprove = computed(
  () =>
    isPending.value &&
    !!preview.value?.sufficient &&
    !!payload.value.prescriptionId &&
    (preview.value?.lines.length ?? 0) > 0,
)

useTabTitle(computed(() => `出库草稿 #${draftId.value}`))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const draft = await getAiDraft(draftId.value)
    isPending.value = draft.status === 'PENDING'
    payload.value = parseOutboundPayload(draft.payload)
    if (!payload.value.prescriptionId || !payload.value.patientId) {
      preview.value = null
      return
    }
    const items = (payload.value.items ?? [])
      .filter((i) => i.medicineId && i.quantity && i.unit)
      .map((i) => ({
        medicineId: i.medicineId!,
        quantity: Number(i.quantity),
        unit: i.unit!,
      }))
    if (!items.length) {
      preview.value = null
      return
    }
    preview.value = await previewOutbound({
      patientId: payload.value.patientId,
      prescriptionId: payload.value.prescriptionId,
      items,
    })
    initAllocations(preview.value)
    if (!preview.value.sufficient) {
      ElMessage.warning('部分药品库存不足，无法出库')
    }
  } finally {
    loading.value = false
  }
}

function initAllocations(result: OutboundPreview) {
  for (const key of Object.keys(allocations)) {
    delete allocations[Number(key)]
  }
  for (const line of result.lines) {
    allocations[line.medicineId] = {}
    for (const a of line.recommendedAllocations) {
      allocations[line.medicineId][a.batchId] = Number(a.recommendedQuantity)
    }
  }
  if (result.lines.length > 0) {
    selectedLineId.value = result.lines[0].medicineId
  }
}

function selectPreviewLine(line: OutboundPreviewLine) {
  if (!line.sufficient) return
  selectedLineId.value = line.medicineId
}

function buildConfirmLines() {
  if (!preview.value) return []
  return preview.value.lines
    .filter((l) => l.sufficient)
    .map((line) => {
      const source = payload.value.items?.find((i) => i.medicineId === line.medicineId)
      const allocs = line.recommendedAllocations
        .filter((a) => (allocations[line.medicineId]?.[a.batchId] ?? 0) > 0)
        .map((a) => ({
          batchId: a.batchId,
          quantity: allocations[line.medicineId]?.[a.batchId] ?? a.recommendedQuantity,
        }))
      return {
        medicineId: line.medicineId,
        quantity: source?.quantity ? Number(source.quantity) : line.requestedQuantity,
        unit: source?.unit ?? line.unit,
        allocations: allocs,
      }
    })
}

async function onApprove() {
  if (!canApprove.value || !preview.value) return
  const confirmLines = buildConfirmLines()
  if (!confirmLines.length) {
    ElMessage.warning('没有可出库的药品')
    return
  }
  if (confirmLines.some((l) => !l.allocations.length)) {
    ElMessage.warning('请为每种药品确认批次分配')
    return
  }

  const summary = confirmLines
    .map((l) => {
      const name = preview.value!.lines.find((p) => p.medicineId === l.medicineId)?.medicineName ?? ''
      return `${name} ${l.quantity} ${l.unit}`
    })
    .join('\n')

  try {
    await ElMessageBox.confirm(
      `将按以下清单扣减库存：\n${summary}\n\n确认后无法撤销，是否继续？`,
      '确认出库',
      { type: 'warning', confirmButtonText: '确认出库', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  approving.value = true
  try {
    const result = await approveOutboundDraft(draftId.value, confirmLines)
    ElMessage.success(`出库成功，共 ${result.flowCount} 条流水`)
    isPending.value = false
    const printUrl = router.resolve(`/prescription/${result.prescriptionId}/print`).href
    window.open(printUrl, '_blank')
  } finally {
    approving.value = false
  }
}

async function reject() {
  await ElMessageBox.confirm('确定拒绝此出库草稿？', '确认')
  await rejectAiDraft(draftId.value)
  ElMessage.success('已拒绝')
  isPending.value = false
}

function goPrescription() {
  if (payload.value.prescriptionId) {
    router.push(`/prescription/${payload.value.prescriptionId}`)
  }
}
</script>

<template>
  <main class="page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <span>待确认出库清单</span>
      </template>

      <el-alert
        v-if="!payload.prescriptionId"
        type="warning"
        :closable="false"
        show-icon
        title="此草稿未关联处方，请使用库存出库页手动操作。"
        style="margin-bottom: 16px"
      />
      <el-alert
        v-else-if="isPending"
        type="info"
        :closable="false"
        show-icon
        title="请核对药品数量与批次，确认后才会扣减库存。"
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

      <el-table v-if="preview" :data="preview.lines" stripe @row-click="selectPreviewLine">
        <el-table-column prop="medicineName" label="药品" />
        <el-table-column prop="specification" label="规格" width="120">
          <template #default="{ row }">
            {{ payload.items?.find((i) => i.medicineId === row.medicineId)?.specification || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="数量" width="120">
          <template #default="{ row }">{{ row.requestedQuantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="库存" width="100">
          <template #default="{ row }">
            <el-tag :type="row.sufficient ? 'success' : 'danger'">
              {{ row.sufficient ? '充足' : '不足' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.sufficient" @click.stop="selectPreviewLine(row)">
              调整批次
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="selectedPreviewLine" class="batch-panel">
        <h3>FEFO 推荐批次 — {{ selectedPreviewLine.medicineName }}</h3>
        <el-table :data="selectedPreviewLine.recommendedAllocations" size="small">
          <el-table-column prop="batchNo" label="批号" />
          <el-table-column prop="expiryDate" label="有效期" />
          <el-table-column label="可用">
            <template #default="{ row }">{{ row.availableQuantity }}</template>
          </el-table-column>
          <el-table-column label="出库数量">
            <template #default="{ row }">
              <el-input-number
                v-model="allocations[selectedPreviewLine.medicineId][row.batchId]"
                :min="0"
                :max="row.availableQuantity"
                :precision="3"
                size="small"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="actions">
        <el-button v-if="payload.prescriptionId" @click="goPrescription">返回处方</el-button>
        <template v-if="isPending && payload.prescriptionId">
          <el-button type="primary" :loading="approving" :disabled="!canApprove" @click="onApprove">
            确认出库
          </el-button>
          <el-button type="danger" @click="reject">拒绝</el-button>
        </template>
        <template v-else-if="isPending">
          <el-button type="danger" @click="reject">拒绝</el-button>
        </template>
      </div>
    </el-card>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
}

.batch-panel {
  margin-top: 20px;
}

.batch-panel h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
