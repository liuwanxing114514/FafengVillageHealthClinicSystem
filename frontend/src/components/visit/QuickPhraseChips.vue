<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { fetchQuickPhraseCandidates, useQuickPhrase } from '@/api/quickPhrase'
import type { QuickPhraseFieldKey, QuickPhraseItem } from '@/types/quickPhrase'

const props = defineProps<{
  fieldKey: QuickPhraseFieldKey
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const loading = ref(false)
const candidates = ref<QuickPhraseItem[]>([])

async function loadCandidates() {
  loading.value = true
  try {
    candidates.value = await fetchQuickPhraseCandidates(props.fieldKey)
  } catch {
    candidates.value = []
  } finally {
    loading.value = false
  }
}

async function onSelect(phrase: QuickPhraseItem) {
  const current = props.modelValue.trim()
  const next = current ? `${current} ${phrase.content}` : phrase.content
  emit('update:modelValue', next)
  try {
    await useQuickPhrase(phrase.id)
  } catch {
    // usage tracking is best-effort
  }
}

onMounted(loadCandidates)
watch(() => props.fieldKey, loadCandidates)
</script>

<template>
  <div v-if="loading || candidates.length" class="quick-phrases">
    <span v-if="loading" class="hint">加载快捷语…</span>
    <el-tag
      v-for="phrase in candidates"
      :key="phrase.id"
      class="phrase-tag"
      type="info"
      effect="plain"
      :title="phrase.content"
      @click="onSelect(phrase)"
    >
      {{ phrase.content.length > 18 ? `${phrase.content.slice(0, 18)}…` : phrase.content }}
    </el-tag>
  </div>
</template>

<style scoped>
.quick-phrases {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.hint {
  font-size: 12px;
  color: #909399;
}

.phrase-tag {
  cursor: pointer;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
