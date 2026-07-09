import { getData, postData } from '@/api/http'
import type { AgentChatResponse, AgentExecutionLog } from '@/types/agent'

export async function postAgentChat(message: string, sessionId?: string): Promise<AgentChatResponse> {
  return postData<AgentChatResponse>('/agent/chat', { message, sessionId })
}

export async function getAgentLogs(sessionId?: string, limit = 20): Promise<AgentExecutionLog[]> {
  const params = new URLSearchParams()
  if (sessionId) params.set('sessionId', sessionId)
  params.set('limit', String(limit))
  const query = params.toString()
  return getData<AgentExecutionLog[]>(`/agent/logs${query ? `?${query}` : ''}`)
}
