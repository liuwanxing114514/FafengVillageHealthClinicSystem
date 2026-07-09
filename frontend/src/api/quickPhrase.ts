import { deleteData, getData, postData, putData } from '@/api/http'
import type {
  QuickPhraseCleanupResult,
  QuickPhraseField,
  QuickPhraseItem,
} from '@/types/quickPhrase'

export function fetchQuickPhraseFields() {
  return getData<QuickPhraseField[]>('/quick-phrases/fields')
}

export function fetchQuickPhraseCandidates(fieldKey: string, limit = 12) {
  const query = new URLSearchParams({ fieldKey, limit: String(limit) })
  return getData<QuickPhraseItem[]>(`/quick-phrases/candidates?${query}`)
}

export function fetchQuickPhrases(fieldKey?: string) {
  const query = fieldKey ? `?fieldKey=${encodeURIComponent(fieldKey)}` : ''
  return getData<QuickPhraseItem[]>(`/quick-phrases${query}`)
}

export function createQuickPhrase(fieldKey: string, content: string) {
  return postData<QuickPhraseItem>('/quick-phrases', { fieldKey, content })
}

export function updateQuickPhrase(id: number, fieldKey: string, content: string) {
  return putData<QuickPhraseItem>(`/quick-phrases/${id}`, { fieldKey, content })
}

export function deleteQuickPhrase(id: number) {
  return deleteData(`/quick-phrases/${id}`)
}

export function useQuickPhrase(id: number) {
  return postData<QuickPhraseItem>(`/quick-phrases/${id}/use`)
}

export function syncQuickPhraseHistory() {
  return postData<void>('/quick-phrases/sync-history')
}

export function cleanupQuickPhrases() {
  return postData<QuickPhraseCleanupResult>('/quick-phrases/cleanup')
}
