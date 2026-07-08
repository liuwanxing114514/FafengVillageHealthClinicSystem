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
