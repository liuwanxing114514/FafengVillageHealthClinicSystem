<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cleanupQuickPhrases,
  createQuickPhrase,
  deleteQuickPhrase,
  fetchQuickPhraseFields,
  fetchQuickPhrases,
  syncQuickPhraseHistory,
  updateQuickPhrase,
} from '@/api/quickPhrase'
import type { QuickPhraseField, QuickPhraseItem } from '@/types/quickPhrase'
import { useTabTitle } from '@/composables/useTabTitle'

useTabTitle('快捷语管理')

const loading = ref(false)
const saving = ref(false)
const fields = ref<QuickPhraseField[]>([])
const phrases = ref<QuickPhraseItem[]>([])
const filterFieldKey = ref('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  fieldKey: 'chief_complaint',
  content: '',
})

const dialogTitle = computed(() => (editingId.value ? '编辑快捷语' : '新增快捷语'))

async function loadData() {
  loading.value = true
  try {
    const [fieldList, phraseList] = await Promise.all([
      fetchQuickPhraseFields(),
      fetchQuickPhrases(filterFieldKey.value || undefined),
    ])
    fields.value = fieldList
    phrases.value = phraseList
    if (!form.fieldKey && fieldList.length) {
      form.fieldKey = fieldList[0].key
    }
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.fieldKey = filterFieldKey.value || fields.value[0]?.key || 'chief_complaint'
  form.content = ''
  dialogVisible.value = true
}

function openEdit(row: QuickPhraseItem) {
  editingId.value = row.id
  form.fieldKey = row.fieldKey
  form.content = row.content
  dialogVisible.value = true
}

async function onSave() {
  const content = form.content.trim()
  if (!content) {
    ElMessage.warning('请输入快捷语内容')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateQuickPhrase(editingId.value, form.fieldKey, content)
      ElMessage.success('已更新')
    } else {
      await createQuickPhrase(form.fieldKey, content)
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function onDelete(row: QuickPhraseItem) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.content.slice(0, 24)}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteQuickPhrase(row.id)
    ElMessage.success('已删除')
    await loadData()
  } catch {
    // cancelled
  }
}

async function onSyncHistory() {
  saving.value = true
  try {
    await syncQuickPhraseHistory()
    ElMessage.success('已从历史病历同步')
    await loadData()
  } finally {
    saving.value = false
  }
}

async function onCleanup() {
  try {
    await ElMessageBox.confirm(
      '将清理长期未使用的低频历史快捷语（手动添加的不受影响），是否继续？',
      '清理确认',
      { type: 'warning' },
    )
    saving.value = true
    const result = await cleanupQuickPhrases()
    ElMessage.success(`已清理 ${result.removedCount} 条`)
    await loadData()
  } catch {
    // cancelled
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <main class="page">
    <el-card v-loading="loading" shadow="never">
      <template #header>
        <div class="header-row">
          <span class="title">快捷语管理</span>
          <div class="actions">
            <el-select v-model="filterFieldKey" clearable placeholder="全部字段" style="width: 140px" @change="loadData">
              <el-option v-for="field in fields" :key="field.key" :label="field.label" :value="field.key" />
            </el-select>
            <el-button @click="onSyncHistory">同步历史</el-button>
            <el-button @click="onCleanup">清理低频</el-button>
            <el-button type="primary" @click="openCreate">新增</el-button>
          </div>
        </div>
      </template>

      <el-table :data="phrases" stripe>
        <el-table-column prop="fieldLabel" label="字段" width="100" />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="88">
          <template #default="{ row }">
            {{ row.source === 'MANUAL' ? '手动' : '历史' }}
          </template>
        </el-table-column>
        <el-table-column prop="useCount" label="使用次数" width="96" align="center" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form label-width="72px">
        <el-form-item label="字段">
          <el-select v-model="form.fieldKey" style="width: 100%">
            <el-option v-for="field in fields" :key="field.key" :label="field.label" :value="field.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
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
  align-items: center;
}
</style>
