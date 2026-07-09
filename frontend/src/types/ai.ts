export interface AiStatus {
  enabled: boolean
  provider: string
  providerAvailable: boolean
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

export interface VisitDraftPayload {
  patientId: number | null
  visitId: number | null
  inputText: string
  chiefComplaint: string | null
  presentIllness: string | null
  pastHistory: string | null
  allergyHistory: string | null
  diagnosis: string | null
  treatment: string | null
  remark: string | null
}
