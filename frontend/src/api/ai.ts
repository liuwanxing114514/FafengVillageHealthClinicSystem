import { getData, postData, putData } from '@/api/http'
import http from '@/api/http'
import type {
  AiDraft,
  AiStatus,
  ApproveInboundResult,
  ApproveOutboundLine,
  ApproveOutboundResult,
  InboundDraftPayload,
  OcrStatus,
  OutboundDraftPayload,
  SimilarVisitSearchResult,
  VisitDraftPayload,
  VisitEmbeddingStatus,
  VisitEmbeddingSyncResult,
  VoiceStatus,
  VoiceTranscription,
} from '@/types/ai'

export async function getAiStatus(): Promise<AiStatus> {
  return getData<AiStatus>('/ai/status')
}

export async function getOcrStatus(): Promise<OcrStatus> {
  return getData<OcrStatus>('/ai/ocr/status')
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

export async function listAiDrafts(draftType?: string, status?: string): Promise<AiDraft[]> {
  const params = new URLSearchParams()
  if (draftType) params.set('draftType', draftType)
  if (status) params.set('status', status)
  const query = params.toString()
  return getData<AiDraft[]>(`/ai/drafts${query ? `?${query}` : ''}`)
}

export async function getAiDraft(id: number): Promise<AiDraft> {
  return getData<AiDraft>(`/ai/drafts/${id}`)
}

export async function updateAiDraftPayload(id: number, payload: string): Promise<AiDraft> {
  return putData<AiDraft>(`/ai/drafts/${id}/payload`, { payload })
}

export async function rejectAiDraft(id: number): Promise<AiDraft> {
  return http.patch<{ code: number; data: AiDraft }>(`/ai/drafts/${id}`, { status: 'REJECTED' }).then((r) => {
    if (r.data.code !== 0) throw r.data
    return r.data.data
  })
}

export async function ocrInbound(file: File): Promise<AiDraft> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<{ code: number; data: AiDraft; message?: string }>(
    '/ai/ocr/inbound',
    form,
    { timeout: 180000 },
  )
  if (data.code !== 0) {
    throw data
  }
  return data.data
}

export async function structureVisit(text: string, patientId?: number): Promise<AiDraft> {
  return postData<AiDraft>('/ai/structure/visit', { text, patientId })
}

export async function approveInboundDraft(id: number): Promise<ApproveInboundResult> {
  return postData<ApproveInboundResult>(`/ai/drafts/${id}/approve-inbound`, {})
}

export async function approveVisitDraft(id: number, patientId: number): Promise<{ id: number }> {
  return postData<{ id: number }>(`/ai/drafts/${id}/approve-visit`, { patientId })
}

export async function approveOutboundDraft(
  id: number,
  lines: ApproveOutboundLine[],
): Promise<ApproveOutboundResult> {
  return postData<ApproveOutboundResult>(`/ai/drafts/${id}/approve-outbound`, { lines })
}

export function parseInboundPayload(payload: string): InboundDraftPayload {
  return JSON.parse(payload) as InboundDraftPayload
}

export function parseVisitPayload(payload: string): VisitDraftPayload {
  return JSON.parse(payload) as VisitDraftPayload
}

export function parseOutboundPayload(payload: string): OutboundDraftPayload {
  return JSON.parse(payload) as OutboundDraftPayload
}

export function stringifyInboundPayload(payload: InboundDraftPayload): string {
  return JSON.stringify(payload)
}

export function stringifyVisitPayload(payload: VisitDraftPayload): string {
  return JSON.stringify(payload)
}

export async function getEmbeddingStatus(): Promise<VisitEmbeddingStatus> {
  return getData<VisitEmbeddingStatus>('/ai/embeddings/status')
}

const EMBEDDING_SYNC_TIMEOUT_MS = 600_000

export async function syncEmbeddingsFull(): Promise<VisitEmbeddingSyncResult> {
  const { data } = await http.post<{ code: number; data: VisitEmbeddingSyncResult; message?: string }>(
    '/ai/embeddings/sync-full',
    {},
    { timeout: EMBEDDING_SYNC_TIMEOUT_MS },
  )
  if (data.code !== 0) {
    throw data
  }
  return data.data
}

export async function syncEmbeddingsIncremental(): Promise<VisitEmbeddingSyncResult> {
  const { data } = await http.post<{ code: number; data: VisitEmbeddingSyncResult; message?: string }>(
    '/ai/embeddings/sync-incremental',
    {},
    { timeout: EMBEDDING_SYNC_TIMEOUT_MS },
  )
  if (data.code !== 0) {
    throw data
  }
  return data.data
}

export async function searchSimilarVisits(payload: {
  chiefComplaint?: string
  presentIllness?: string
  diagnosis?: string
  patientId?: number | null
  excludeVisitId?: number | null
}): Promise<SimilarVisitSearchResult> {
  return postData<SimilarVisitSearchResult>('/ai/embeddings/search-similar', payload)
}
