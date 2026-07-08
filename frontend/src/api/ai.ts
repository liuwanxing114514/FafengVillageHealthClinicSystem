import { getData } from '@/api/http'
import type { AiStatus } from '@/types/ai'

export async function getAiStatus(): Promise<AiStatus> {
  return getData<AiStatus>('/ai/status')
}
