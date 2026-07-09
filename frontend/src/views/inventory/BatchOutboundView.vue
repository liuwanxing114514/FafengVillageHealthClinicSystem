<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmBatchOutbound, previewBatchOutbound } from '@/api/inventory'
import { searchMedicines } from '@/api/medicine'
import BarcodeScanPanel from '@/components/inventory/BarcodeScanPanel.vue'
import type { OutboundPreview, OutboundPreviewLine } from '@/types/inventory'
import type { MedicineListItem } from '@/types/medicine'

interface EditableLine {
  key: number
  medicineId: number | null
  medicineName: string
  baseUnit: string
  packageUnit: string
  quantity: number | null
  unit: string
}

const lineKey = ref(1)
const previewing = ref(false)
const saving = ref(false)
const reason = ref('')
const preview = ref<OutboundPreview | null>(null)
const selectedLineId = ref<number | null>(null)
const medicineOptions = ref<MedicineListItem[]>([])
const allocations = reactive<Record<number, Record<number, number>>>({})

const lines = ref<EditableLine[]>([emptyLine()])

const selectedPreviewLine = computed(() =>
  preview.value?.lines.find((l) => l.medicineId === selectedLineId.value) ?? null,
)

const canPreview = computed(() =>
  lines.value.some((l) => l.medicineId && l.quantity && l.unit),
)

const canSubmit = computed(() =>
  !!preview.value?.sufficient && reason.value.trim().length > 0 && (preview.value?.lines.length ?? 0) > 0,
)

function emptyLine(): EditableLine {
  return {
    key: lineKey.value++,
    medicineId: null,
    medicineName: '',
    baseUnit: '',
    packageUnit: '',
    quantity: null,
    unit: '',
  }
}

function unitOptions(line: EditableLine) {
  const units = new Set<string>()
  if (line.baseUnit) units.add(line.baseUnit)
  if (line.packageUnit) units.add(line.packageUnit)
  return Array.from(units)
}

async function searchMedicineOptions(keyword: string) {
  if (!keyword.trim()) {
    medicineOptions.value = []
    return
  }
  const page = await searchMedicines({ keyword: keyword.trim(), page: 1, size: 20 })
  medicineOptions.value = page.records
}

function onMedicineSelect(line: EditableLine, medicineId: number | null) {
  if (!medicineId) {
    line.medicineId = null
    line.medicineName = ''
    line.baseUnit = ''
    line.packageUnit = ''
    line.unit = ''
    preview.value = null
    selectedLineId.value = null
    return
  }
  const med = medicineOptions.value.find((m) => m.id === medicineId)
  if (!med) return
  line.medicineId = med.id
  line.medicineName = med.name
  line.baseUnit = med.baseUnit
  line.packageUnit = med.packageUnit ?? ''
  line.unit = med.baseUnit
  preview.value = null
  selectedLineId.value = null
}

function addLine() {
  lines.value.push(emptyLine())
}

function removeLine(index: number) {
  if (lines.value.length <= 1) return
  lines.value.splice(index, 1)
  preview.value = null
  selectedLineId.value = null
}

function buildPreviewItems() {
  return lines.value
    .filter((l) => l.medicineId && l.quantity && l.unit)
    .map((l) => ({
      medicineId: l.medicineId!,
      quantity: l.quantity!,
      unit: l.unit,
    }))
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
}

async function onPreview() {
  const items = buildPreviewItems()
  if (!items.length) {
    ElMessage.warning('请至少添加一种药品')
    return
  }
  previewing.value = true
  try {
    preview.value = await previewBatchOutbound({ items })
    initAllocations(preview.value)
    if (!preview.value.sufficient) {
      ElMessage.warning('部分药品库存不足，整单无法出库')
    } else {
      ElMessage.success('预览完成，请核对批次后提交')
    }
  } finally {
    previewing.value = false
  }
}

function selectPreviewLine(line: OutboundPreviewLine) {
  if (!line.sufficient) return
  selectedLineId.value = line.medicineId
}

function onBarcodeMatched(medicine: MedicineListItem) {
  const existing = lines.value.find((l) => l.medicineId === medicine.id)
  if (existing) {
    selectPreviewLine(
      preview.value?.lines.find((l) => l.medicineId === medicine.id) ?? {
        medicineId: medicine.id,
        medicineName: medicine.name,
        requestedQuantity: 0,
        unit: medicine.baseUnit,
        requestedBaseQuantity: 0,
        baseUnit: medicine.baseUnit,
        sufficient: true,
        recommendedAllocations: [],
      },
    )
    ElMessage.success(`已定位 ${medicine.name}`)
    return
  }
  const line = lines.value.find((l) => !l.medicineId) ?? emptyLine()
  if (!lines.value.includes(line)) {
    lines.value.push(line)
  }
  line.medicineId = medicine.id
  line.medicineName = medicine.name
  line.baseUnit = medicine.baseUnit
  line.packageUnit = medicine.packageUnit ?? ''
  line.unit = medicine.baseUnit
  line.quantity = 1
  preview.value = null
  ElMessage.success(`已添加 ${medicine.name}，请填写数量后预览`)
}

function buildConfirmLines() {
  if (!preview.value) return []
  return preview.value.lines
    .filter((l) => l.sufficient)
    .map((line) => {
      const source = lines.value.find((l) => l.medicineId === line.medicineId)
      const allocs = line.recommendedAllocations
        .filter((a) => (allocations[line.medicineId]?.[a.batchId] ?? 0) > 0)
        .map((a) => ({
          batchId: a.batchId,
          quantity: allocations[line.medicineId]?.[a.batchId] ?? a.recommendedQuantity,
        }))
      return {
        medicineId: line.medicineId,
        quantity: source?.quantity ?? line.requestedQuantity,
        unit: source?.unit ?? line.unit,
        allocations: allocs,
      }
    })
}

async function onConfirm() {
  if (!canSubmit.value || !preview.value) return
  const confirmLines = buildConfirmLines()
  if (!confirmLines.length) {
    ElMessage.warning('没有可出库的药品')
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
      `出库原因：${reason.value.trim()}\n\n将出库以下 ${confirmLines.length} 种药品：\n${summary}\n\n确认后无法撤销，是否继续？`,
      '批量出库确认',
      { type: 'warning', confirmButtonText: '确认出库', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  saving.value = true
  try {
    const result = await confirmBatchOutbound({
      reason: reason.value.trim(),
      lines: confirmLines,
    })
    ElMessage.success(`批量出库成功，共 ${result.flowCount} 条流水`)
    lines.value = [emptyLine()]
    reason.value = ''
    preview.value = null
    selectedLineId.value = null
    for (const key of Object.keys(allocations)) {
      delete allocations[Number(key)]
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <main class="page">
    <el-card shadow="never">
      <template #header>
        <span>批量出库</span>
      </template>

      <p class="hint">
        适用于盘点损耗、集中领用、过期集中处理等场景。须填写统一原因；任一药品库存不足则整单阻止出库。
      </p>

      <BarcodeScanPanel @matched="onBarcodeMatched" />

      <el-table :data="lines" class="line-table">
        <el-table-column label="药品" min-width="220">
          <template #default="{ row }">
            <el-select
              v-model="row.medicineId"
              filterable
              remote
              clearable
              placeholder="搜索药品"
              :remote-method="searchMedicineOptions"
              style="width: 100%"
              @change="(id: number | null) => onMedicineSelect(row, id)"
            >
              <el-option
                v-for="m in medicineOptions"
                :key="m.id"
                :label="`${m.name} ${m.specification}`"
                :value="m.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="0.001" :precision="3" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="单位" width="120">
          <template #default="{ row }">
            <el-select v-model="row.unit" placeholder="单位" style="width: 100%">
              <el-option v-for="u in unitOptions(row)" :key="u" :label="u" :value="u" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" :disabled="lines.length <= 1" @click="removeLine($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="toolbar">
        <el-button @click="addLine">添加药品</el-button>
        <el-button type="primary" :loading="previewing" :disabled="!canPreview" @click="onPreview">
          预览批次
        </el-button>
      </div>

      <template v-if="preview">
        <el-divider />
        <el-table :data="preview.lines" @row-click="selectPreviewLine">
          <el-table-column prop="medicineName" label="药品" />
          <el-table-column label="出库数量">
            <template #default="{ row }">{{ row.requestedQuantity }} {{ row.unit }}</template>
          </el-table-column>
          <el-table-column label="库存">
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

        <el-form label-width="80px" class="reason-form">
          <el-form-item label="出库原因" required>
            <el-input
              v-model="reason"
              type="textarea"
              :rows="2"
              maxlength="256"
              show-word-limit
              placeholder="如：盘点损耗、过期集中处理"
            />
          </el-form-item>
        </el-form>

        <el-button
          type="primary"
          :loading="saving"
          :disabled="!canSubmit"
          @click="onConfirm"
        >
          确认批量出库
        </el-button>
      </template>
    </el-card>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.hint { margin: 0 0 16px; color: #606266; font-size: 14px; }
.line-table { margin-top: 16px; }
.toolbar { margin-top: 12px; display: flex; gap: 12px; }
.batch-panel { margin-top: 20px; }
.batch-panel h3 { margin: 0 0 12px; font-size: 15px; }
.reason-form { margin-top: 20px; max-width: 560px; }
</style>
