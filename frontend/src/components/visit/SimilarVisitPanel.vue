<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { searchSimilarVisits } from '@/api/ai'
import type { SimilarVisitItem } from '@/types/ai'

const props = defineProps<{
  chiefComplaint: string
  presentIllness: string
  diagnosis: string
  patientId: number | null
  excludeVisitId: number | null
  embeddingEnabled: boolean
}>()

const loading = ref(false)
const available = ref(false)
const items = ref<SimilarVisitItem[]>([])
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const hasQueryText = computed(() =>
  [props.chiefComplaint, props.presentIllness, props.diagnosis].some((v) => v.trim().length > 0),
)

const emptyHint = computed(() => {
  if (!props.embeddingEnabled) {
    return '相似病例检索未启用'
  }
  if (!available.value && !loading.value) {
    return '向量服务暂不可用'
  }
  if (!hasQueryText.value) {
    return '填写主诉、现病史或诊断后可参考相似病例'
  }
  return '暂无相似病例'
})

function formatVisitTime(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

function formatSimilarity(value: number) {
  return `${Math.round(value * 100)}%`
}

async function fetchSimilar() {
  if (!props.embeddingEnabled || !hasQueryText.value) {
    available.value = props.embeddingEnabled
    items.value = []
    return
  }
  loading.value = true
  try {
    const result = await searchSimilarVisits({
      chiefComplaint: props.chiefComplaint.trim() || undefined,
      presentIllness: props.presentIllness.trim() || undefined,
      diagnosis: props.diagnosis.trim() || undefined,
      patientId: props.patientId ?? undefined,
      excludeVisitId: props.excludeVisitId ?? undefined,
    })
    available.value = result.available
    items.value = result.items ?? []
  } catch {
    available.value = false
    items.value = []
  } finally {
    loading.value = false
  }
}

function scheduleFetch() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    void fetchSimilar()
  }, 800)
}

watch(
  () => [
    props.chiefComplaint,
    props.presentIllness,
    props.diagnosis,
    props.patientId,
    props.excludeVisitId,
    props.embeddingEnabled,
  ],
  () => scheduleFetch(),
  { immediate: true },
)

onBeforeUnmount(() => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
})
</script>

<template>
  <aside class="similar-panel">
    <div class="panel-header">
      <h3>相似病例参考</h3>
      <p class="disclaimer">仅供参考，不替代诊断</p>
    </div>

    <div v-loading="loading" class="panel-body">
      <el-empty v-if="!loading && items.length === 0" :description="emptyHint" :image-size="72" />

      <article v-for="item in items" :key="item.visitId" class="similar-card">
        <header class="card-meta">
          <span>病历 #{{ item.visitId }}</span>
          <span v-if="item.visitTime">{{ formatVisitTime(item.visitTime) }}</span>
          <span class="score">相似度 {{ formatSimilarity(item.similarity) }}</span>
        </header>
        <pre class="summary">{{ item.textSummary }}</pre>
      </article>
    </div>
  </aside>
</template>

<style scoped>
.similar-panel {
  width: 320px;
  flex-shrink: 0;
  position: sticky;
  top: 24px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.panel-header {
  padding: 16px 16px 8px;
  border-bottom: 1px solid #f0f2f5;
}

.panel-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.disclaimer {
  margin: 8px 0 0;
  font-size: 12px;
  color: #e6a23c;
  font-weight: 500;
}

.panel-body {
  min-height: 160px;
  padding: 12px 16px 16px;
}

.similar-card {
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.similar-card:last-child {
  margin-bottom: 0;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.score {
  color: #409eff;
  font-weight: 500;
}

.summary {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
}

@media (max-width: 1100px) {
  .similar-panel {
    width: 100%;
    position: static;
  }
}
</style>
