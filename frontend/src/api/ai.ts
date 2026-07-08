import { getData } from '@/api/http'
import http from '@/api/http'
import type { AiStatus, VoiceStatus, VoiceTranscription } from '@/types/ai'

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
