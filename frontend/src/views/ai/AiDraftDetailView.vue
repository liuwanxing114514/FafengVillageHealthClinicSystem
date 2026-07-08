<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveVisitDraft,
  fetchAiDraft,
  rejectAiDraft,
  updateVisitDraftPayload,
} from '@/api/ai'
import { useTabTitle } from '@/composables/useTabTitle'
import type { VisitDraftPayload } from '@/types/ai'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const draftId = computed(() => Number(route.params.id))

const form = reactive<VisitDraftPayload>({
  patientId: null,
  visitId: null,
  inputText: '',
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  allergyHistory: '',
  diagnosis: '',
  treatment: '',
  remark: '',
})

const isPending = ref(true)

useTabTitle(computed(() => `AI 草稿 #${draftId.value}`))

function applyPayload(payload: VisitDraftPayload) {
  form.patientId = payload.patientId
  form.visitId = payload.visitId
  form.inputText = payload.inputText ?? ''
  form.chiefComplaint = payload.chiefComplaint ?? ''
  form.presentIllness = payload.presentIllness ?? ''
  form.pastHistory = payload.pastHistory ?? ''
  form.allergyHistory = payload.allergyHistory ?? ''
  form.diagnosis = payload.diagnosis ?? ''
  form.treatment = payload.treatment ?? ''
  form.remark = payload.remark ?? ''
}

async function loadDraft() {
  if (!draftId.value) return
  loading.value = true
  try {
    const draft = await fetchAiDraft(draftId.value)
    isPending.value = draft.status === 'PENDING'
    applyPayload(JSON.parse(draft.payload) as VisitDraftPayload)
  } finally {
    loading.value = false
  }
}

async function onSavePayload() {
  if (!form.patientId) {
    ElMessage.warning('请填写患者 ID')
    return
  }
  saving.value = true
  try {
    await updateVisitDraftPayload(draftId.value, { ...form })
    ElMessage.success('草稿已保存')
  } finally {
    saving.value = false
  }
}

async function onApprove() {
  if (!form.patientId) {
    ElMessage.warning('请填写患者 ID 后再确认写入')
    return
  }
  try {
    await ElMessageBox.confirm('确认后将写入正式病历，是否继续？', '确认写入', {
      type: 'warning',
      confirmButtonText: '确认写入',
      cancelButtonText: '取消',
    })
    saving.value = true
    await updateVisitDraftPayload(draftId.value, { ...form })
    const visit = await approveVisitDraft(draftId.value)
    ElMessage.success('已写入正式病历')
    router.replace(`/visit/${visit.id}`)
  } catch {
    // cancelled
  } finally {
    saving.value = false
  }
}

async function onReject() {
  try {
    await ElMessageBox.confirm('确定拒绝此草稿吗？', '拒绝确认', { type: 'warning' })
    await rejectAiDraft(draftId.value)
    ElMessage.success('已拒绝')
    router.replace('/ai')
  } catch {
    // cancelled
  }
}

onMounted(loadDraft)
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span class="title">病历草稿核对</span>
          <div class="actions">
            <el-button @click="router.push('/ai')">返回 AI 助手</el-button>
            <el-button v-if="isPending" :loading="saving" @click="onSavePayload">保存草稿</el-button>
            <el-button v-if="isPending" type="primary" :loading="saving" @click="onApprove">
              确认写入病历
            </el-button>
            <el-button v-if="isPending" type="danger" plain @click="onReject">拒绝</el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="请逐字段核对后再确认。下方展示的是原文整理结果，不是脱敏后的 API 内容。"
        class="alert"
      />

      <el-form label-width="96px" class="form">
        <el-form-item label="患者 ID">
          <el-input-number v-model="form.patientId" :min="1" :disabled="!isPending" />
        </el-form-item>
        <el-form-item label="病历 ID">
          <el-input-number v-model="form.visitId" :min="1" :disabled="!isPending" />
          <span class="field-hint">留空则新建病历；填写则更新已有病历</span>
        </el-form-item>
        <el-form-item label="原始输入">
          <el-input v-model="form.inputText" type="textarea" :rows="3" disabled />
        </el-form-item>
        <el-form-item label="主诉">
          <el-input v-model="form.chiefComplaint" type="textarea" :rows="2" :disabled="!isPending" />
        </el-form-item>
        <el-form-item label="现病史">
          <el-input v-model="form.presentIllness" type="textarea" :rows="3" :disabled="!isPending" />
        </el-form-item>
        <el-form-item label="既往史">
          <el-input v-model="form.pastHistory" type="textarea" :rows="2" :disabled="!isPending" />
        </el-form-item>
        <el-form-item label="过敏史">
          <el-input v-model="form.allergyHistory" type="textarea" :rows="2" :disabled="!isPending" />
        </el-form-item>
        <el-form-item label="诊断">
          <el-input v-model="form.diagnosis" type="textarea" :rows="2" :disabled="!isPending" />
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="form.treatment" type="textarea" :rows="3" :disabled="!isPending" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" :disabled="!isPending" />
        </el-form-item>
      </el-form>
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
  gap: 12px;
  flex-wrap: wrap;
}

.title {
  font-size: 1.1rem;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.alert {
  margin-bottom: 16px;
}

.form {
  max-width: 760px;
}

.field-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 13px;
}
</style>
