<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveVisitDraft,
  getAiDraft,
  parseVisitPayload,
  rejectAiDraft,
  stringifyVisitPayload,
  updateAiDraftPayload,
} from '@/api/ai'
import type { VisitDraftPayload } from '@/types/ai'

const route = useRoute()
const router = useRouter()
const draftId = computed(() => Number(route.params.id))
const patientId = computed(() => Number(route.query.patientId))
const loading = ref(true)
const saving = ref(false)
const payload = ref<VisitDraftPayload>({})

onMounted(async () => {
  try {
    const draft = await getAiDraft(draftId.value)
    payload.value = parseVisitPayload(draft.payload)
  } finally {
    loading.value = false
  }
})

async function onApprove() {
  if (!patientId.value) {
    ElMessage.warning('缺少患者信息')
    return
  }
  await ElMessageBox.confirm('确认后将写入正式病历。', '确认病历草稿', { type: 'warning' })
  saving.value = true
  try {
    await updateAiDraftPayload(draftId.value, stringifyVisitPayload(payload.value))
    const visit = await approveVisitDraft(draftId.value, patientId.value)
    ElMessage.success('病历已保存')
    router.push(`/visit/${visit.id}?patientId=${patientId.value}`)
  } finally {
    saving.value = false
  }
}

async function onReject() {
  await rejectAiDraft(draftId.value)
  ElMessage.success('已拒绝草稿')
  router.back()
}
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span>AI 病历草稿确认</span>
          <el-space>
            <el-button @click="router.back()">返回</el-button>
            <el-button type="danger" plain @click="onReject">拒绝</el-button>
            <el-button type="primary" :loading="saving" @click="onApprove">确认写入病历</el-button>
          </el-space>
        </div>
      </template>

      <el-alert type="warning" :closable="false" show-icon title="以下为 AI 整理结果，请逐字段核对后再确认。" style="margin-bottom: 16px" />

      <el-form label-width="96px" style="max-width: 720px">
        <el-form-item label="主诉">
          <el-input v-model="payload.chiefComplaint" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="现病史">
          <el-input v-model="payload.presentIllness" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="既往史">
          <el-input v-model="payload.pastHistory" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="诊断">
          <el-input v-model="payload.diagnosis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="payload.treatment" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="payload.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </el-card>
  </main>
</template>

<style scoped>
.page { min-height: 100vh; padding: 24px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
</style>
