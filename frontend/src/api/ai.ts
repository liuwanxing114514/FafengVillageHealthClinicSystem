import { getData, patchData, postData, putData } from '@/api/http'
import http from '@/api/http'
import type { AiDraft, AiStatus, VoiceStatus, VoiceTranscription, VisitDraftPayload } from '@/types/ai'
import type { VisitDetail } from '@/types/visit'

export async function getAiStatus(): Promise<AiStatus> {
  return getData<AiStatus>('/ai/status')
}

export async function getVoiceStatus(): Promise<VoiceStatus> {
  return getData<VoiceStatus>('/ai/voice/status')
}

export async function transcribeVoice(blob: Blob): Promise<string> {
  const form = new FormData()
  form.append('file', blob, 'recording.webm')
  const { data } = await http.post<{ code: number; data: VoiceTranscription; message?: string }>(
    '/ai/voice/transcribe',
    form,
    { timeout: 120000 },
  )
  if (data.code !== 0) {
    throw data
  }
  return data.data.text
}

export function fetchAiDrafts(draftType?: string, status?: string) {
  const params = new URLSearchParams()
  if (draftType) params.set('draftType', draftType)
  if (status) params.set('status', status)
  const query = params.toString()
  return getData<AiDraft[]>(`/ai/drafts${query ? `?${query}` : ''}`)
}

export function fetchAiDraft(id: number) {
  return getData<AiDraft>(`/ai/drafts/${id}`)
}

export function structureVisitDraft(text: string, patientId?: number, visitId?: number) {
  return postData<AiDraft>('/ai/structure/visit', {
    text,
    patientId: patientId ?? null,
    visitId: visitId ?? null,
  })
}

export function updateVisitDraftPayload(id: number, payload: VisitDraftPayload) {
  return putData<AiDraft>(`/ai/drafts/${id}/payload`, {
    payload: JSON.stringify(payload),
  })
}

export function approveVisitDraft(id: number) {
  return postData<VisitDetail>(`/ai/drafts/${id}/approve-visit`)
}

export function rejectAiDraft(id: number) {
  return patchData<AiDraft>(`/ai/drafts/${id}`, { status: 'REJECTED' })
}
