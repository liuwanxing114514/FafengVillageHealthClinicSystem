<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveInboundDraft,
  getAiDraft,
  getOcrStatus,
  parseInboundPayload,
  rejectAiDraft,
  stringifyInboundPayload,
  updateAiDraftPayload,
} from '@/api/ai'
import { searchMedicines } from '@/api/medicine'
import type { InboundDraftLine, InboundDraftPayload } from '@/types/ai'
import type { MedicineListItem } from '@/types/medicine'

const route = useRoute()
const router = useRouter()
const draftId = computed(() => Number(route.params.id))
const loading = ref(true)
const saving = ref(false)
const approving = ref(false)
const ocrAvailable = ref(false)
const payload = ref<InboundDraftPayload>({ lines: [] })
const medicineOptions = ref<MedicineListItem[]>([])

onMounted(async () => {
  try {
    const [draft, ocrStatus] = await Promise.all([getAiDraft(draftId.value), getOcrStatus()])
    payload.value = parseInboundPayload(draft.payload)
    ocrAvailable.value = ocrStatus.available
  } finally {
    loading.value = false
  }
})

async function searchMedicineOptions(keyword: string) {
  if (!keyword.trim()) return
  const page = await searchMedicines({ keyword: keyword.trim(), page: 1, size: 20 })
  medicineOptions.value = page.records
}

function addLine() {
  payload.value.lines.push({
    medicineName: '',
    quantity: '',
    unit: '',
    batchNo: '',
    expiryDate: '',
    purchasePrice: '',
  })
}

function removeLine(index: number) {
  payload.value.lines.splice(index, 1)
}

function onMedicineSelect(line: InboundDraftLine, id: number) {
  const med = medicineOptions.value.find((m) => m.id === id)
  if (med) {
    line.medicineId = med.id
    line.medicineName = med.name
    line.unit = line.unit || med.baseUnit
    line.matchNote = `已选择：${med.name}`
  }
}

async function saveDraft() {
  saving.value = true
  try {
    await updateAiDraftPayload(draftId.value, stringifyInboundPayload(payload.value))
    ElMessage.success('草稿已保存')
  } finally {
    saving.value = false
  }
}

async function onApprove() {
  await ElMessageBox.confirm('确认后将按表格逐条生成入库流水，此操作不可撤销。', '批准入库', {
    type: 'warning',
    confirmButtonText: '批准入库',
  })
  saving.value = true
  approving.value = true
  try {
    await updateAiDraftPayload(draftId.value, stringifyInboundPayload(payload.value))
    const result = await approveInboundDraft(draftId.value)
    ElMessage.success(`入库成功，共 ${result.successCount} 条`)
    router.push('/inventory/flows')
  } finally {
    saving.value = false
    approving.value = false
  }
}

async function onReject() {
  await ElMessageBox.confirm('确定拒绝此 OCR 入库草稿？', '拒绝草稿')
  await rejectAiDraft(draftId.value)
  ElMessage.success('已拒绝')
  router.push('/inventory/inbound')
}
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span>OCR 入库草稿审核</span>
          <el-space>
            <el-button @click="router.push('/inventory/inbound')">返回</el-button>
            <el-button :loading="saving" @click="saveDraft">保存修改</el-button>
            <el-button type="danger" plain @click="onReject">拒绝</el-button>
            <el-button type="primary" :loading="approving" @click="onApprove">批准入库</el-button>
          </el-space>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="请逐条核对药品、数量、批号后再批准。批准前不会写入库存。"
        style="margin-bottom: 16px"
      />

      <el-form label-width="80px" style="max-width: 640px; margin-bottom: 16px">
        <el-form-item label="供应商">
          <el-input v-model="payload.supplier" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="payload.remark" type="textarea" />
        </el-form-item>
      </el-form>

      <div style="margin-bottom: 8px">
        <el-button type="primary" link @click="addLine">+ 添加一行</el-button>
      </div>

      <el-table :data="payload.lines" border stripe>
        <el-table-column label="药品" min-width="180">
          <template #default="{ row }">
            <el-select
              v-model="row.medicineId"
              filterable
              remote
              :remote-method="searchMedicineOptions"
              placeholder="搜索药品"
              style="width: 100%"
              @change="(id: number) => onMedicineSelect(row, id)"
            >
              <el-option
                v-for="m in medicineOptions"
                :key="m.id"
                :label="`${m.name} ${m.specification ?? ''}`"
                :value="m.id"
              />
            </el-select>
            <div v-if="row.matchNote" class="hint">{{ row.matchNote }}</div>
          </template>
        </el-table-column>
        <el-table-column label="规格" width="120">
          <template #default="{ row }">
            <el-input v-model="row.specification" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="90">
          <template #default="{ row }">
            <el-input v-model="row.quantity" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="80">
          <template #default="{ row }">
            <el-input v-model="row.unit" />
          </template>
        </el-table-column>
        <el-table-column label="批号" width="120">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" />
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="140">
          <template #default="{ row }">
            <el-date-picker v-model="row.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="90">
          <template #default="{ row }">
            <el-input v-model="row.purchasePrice" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeLine($index)">删</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-collapse v-if="payload.ocrText" style="margin-top: 16px">
        <el-collapse-item title="OCR 原文（仅供参考）" name="ocr">
          <pre class="ocr-text">{{ payload.ocrText }}</pre>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
.hint { font-size: 12px; color: #909399; margin-top: 4px; }
.ocr-text { white-space: pre-wrap; font-size: 13px; color: #606266; }
</style>
