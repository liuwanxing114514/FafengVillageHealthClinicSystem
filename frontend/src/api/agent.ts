import http, { deleteData, getData } from '@/api/http'
import type { ApiResult } from '@/types/api'
import type {
  AgentChatResponse,
  AgentConversation,
  AgentExecutionLog,
  AgentMessageRecord,
} from '@/types/agent'

/** Agent 含工具调用与多轮 LLM，需长于默认 15s */
const AGENT_CHAT_TIMEOUT_MS = 120_000

export async function postAgentChat(
  message: string,
  sessionId?: string,
  signal?: AbortSignal,
): Promise<AgentChatResponse> {
  const { data } = await http.post<ApiResult<AgentChatResponse>>(
    '/agent/chat',
    { message, sessionId },
    { timeout: AGENT_CHAT_TIMEOUT_MS, signal },
  )
  if (data.code !== 0) {
    throw data
  }
  return data.data
}

export async function getAgentConversations(limit = 50): Promise<AgentConversation[]> {
  return getData<AgentConversation[]>(`/agent/conversations?limit=${limit}`)
}

export async function getAgentMessages(conversationId: string): Promise<AgentMessageRecord[]> {
  return getData<AgentMessageRecord[]>(`/agent/conversations/${conversationId}/messages`)
}

export async function deleteAgentConversation(conversationId: string): Promise<void> {
  await deleteData(`/agent/conversations/${conversationId}`)
}

export async function getAgentLogs(sessionId?: string, limit = 20): Promise<AgentExecutionLog[]> {
  const params = new URLSearchParams()
  if (sessionId) params.set('sessionId', sessionId)
  params.set('limit', String(limit))
  const query = params.toString()
  return getData<AgentExecutionLog[]>(`/agent/logs${query ? `?${query}` : ''}`)
}
