export interface QuickPhraseField {
  key: string
  label: string
}

export interface QuickPhraseItem {
  id: number
  fieldKey: string
  fieldLabel: string
  content: string
  source: 'MANUAL' | 'HISTORY'
  useCount: number
  lastUsedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface QuickPhraseCleanupResult {
  removedCount: number
}

export type QuickPhraseFieldKey =
  | 'chief_complaint'
  | 'present_illness'
  | 'past_history'
  | 'allergy_history'
  | 'diagnosis'
  | 'treatment'
  | 'remark'
  | 'prescription_usage'
