<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import {
  confirmMedicineImport,
  downloadMedicineImportTemplate,
  previewMedicineImport,
} from '@/api/import'
import type { MedicineImportPreview } from '@/types/import'

const router = useRouter()
const uploading = ref(false)
const confirming = ref(false)
const selectedFile = ref<File | null>(null)
const preview = ref<MedicineImportPreview | null>(null)

async function onDownloadTemplate() {
  await downloadMedicineImportTemplate()
}

function onFileChange(uploadFile: UploadFile) {
  selectedFile.value = uploadFile.raw ?? null
  preview.value = null
}

async function onPreview() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }
  uploading.value = true
  try {
    preview.value = await previewMedicineImport(selectedFile.value)
    if (preview.value.errorCount > 0) {
      ElMessage.warning(`共 ${preview.value.errorCount} 行有误，请修正后重新上传`)
    } else {
      ElMessage.success(`解析成功，共 ${preview.value.validCount} 行可导入`)
    }
  } finally {
    uploading.value = false
  }
}

async function onConfirm() {
  if (!preview.value?.canConfirm) {
    ElMessage.warning('存在错误行或未解析数据，无法确认导入')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认导入 ${preview.value.validCount} 条药品资料？确认后将写入数据库。`,
      '确认导入',
      { type: 'warning' },
    )
  } catch {
    return
  }

  confirming.value = true
  try {
    const result = await confirmMedicineImport(preview.value.previewId)
    ElMessage.success(`导入完成：${result.medicineCount} 个药品，${result.inventoryCount} 条初始库存`)
    router.push('/medicine')
  } finally {
    confirming.value = false
  }
}
</script>

<template>
  <main class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>Excel 药品导入</span>
          <el-button @click="router.push('/medicine')">返回药品列表</el-button>
        </div>
      </template>

      <p class="hint">
        请下载系统固定模板，按表头填写后上传。系统会先预览并标出错误行，确认后才正式写入。
      </p>

      <div class="actions">
        <el-button type="primary" plain @click="onDownloadTemplate">下载模板</el-button>
        <el-upload
          :auto-upload="false"
          :show-file-list="true"
          accept=".xlsx"
          :limit="1"
          :on-change="onFileChange"
        >
          <el-button>选择文件</el-button>
        </el-upload>
        <el-button type="primary" :loading="uploading" @click="onPreview">上传并预览</el-button>
      </div>

      <div v-if="preview" class="summary">
        <el-tag>共 {{ preview.totalRows }} 行</el-tag>
        <el-tag type="success">有效 {{ preview.validCount }}</el-tag>
        <el-tag type="danger">错误 {{ preview.errorCount }}</el-tag>
      </div>

      <el-table v-if="preview" :data="preview.rows" size="small" style="margin-top: 16px">
        <el-table-column prop="rowNumber" label="行号" width="70" />
        <el-table-column prop="name" label="药品名称" min-width="120" />
        <el-table-column prop="baseUnit" label="基本单位" width="90" />
        <el-table-column label="进货单价" width="100">
          <template #default="{ row }">{{ row.purchasePrice ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批号" width="100" />
        <el-table-column label="初始库存" width="100">
          <template #default="{ row }">{{ row.initialStock ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.valid ? 'success' : 'danger'">{{ row.valid ? '有效' : '错误' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="错误信息" min-width="200">
          <template #default="{ row }">
            <span v-if="row.errors.length" class="error-text">{{ row.errors.join('；') }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="preview?.canConfirm" class="footer-actions">
        <el-button type="primary" :loading="confirming" @click="onConfirm">确认导入</el-button>
      </div>
    </el-card>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
.hint { color: #606266; margin-bottom: 16px; }
.actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.summary { display: flex; gap: 8px; margin-top: 16px; }
.error-text { color: #f56c6c; }
.footer-actions { margin-top: 20px; }
</style>
