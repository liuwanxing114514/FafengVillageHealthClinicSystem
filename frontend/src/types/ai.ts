export interface AiStatus {
  enabled: boolean
  provider: string
  providerAvailable: boolean
}

export interface OcrStatus {
  configured: boolean
  available: boolean
}

export interface VoiceStatus {
  configured: boolean
  available: boolean
}

export interface VoiceTranscription {
  text: string
}

export interface AiDraft {
  id: number
  draftType: string
  status: string
  payload: string
  source: string
  createdAt: string
  updatedAt: string
}

export interface InboundDraftLine {
  medicineId?: number | null
  medicineName?: string
  specification?: string
  quantity?: string
  unit?: string
  batchNo?: string
  expiryDate?: string
  purchasePrice?: string
  matchNote?: string
}

export interface InboundDraftPayload {
  supplier?: string
  remark?: string
  imagePath?: string
  ocrText?: string
  lines: InboundDraftLine[]
}

export interface VisitDraftPayload {
  patientId?: number | null
  sourceText?: string
  chiefComplaint?: string
  presentIllness?: string
  pastHistory?: string
  diagnosis?: string
  treatment?: string
  remark?: string
}

export interface ApproveInboundResult {
  successCount: number
  totalCount: number
}

export interface OutboundDraftLine {
  medicineId?: number
  medicineName?: string
  specification?: string
  quantity?: string
  unit?: string
  usage?: string
}

export interface OutboundDraftPayload {
  prescriptionId?: number
  patientId?: number
  patientName?: string
  diagnosis?: string
  remark?: string
  items?: OutboundDraftLine[]
}

export interface ApproveOutboundLine {
  medicineId: number
  quantity: number
  unit: string
  allocations: { batchId: number; quantity: number }[]
}

export interface ApproveOutboundResult {
  prescriptionId: number
  lineCount: number
  flowCount: number
}
