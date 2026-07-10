import { deleteData, getData, postData } from '@/api/http'
import type {
  AgentChatResponse,
  AgentConversation,
  AgentExecutionLog,
  AgentMessageRecord,
} from '@/types/agent'

export async function postAgentChat(message: string, sessionId?: string): Promise<AgentChatResponse> {
  return postData<AgentChatResponse>('/agent/chat', { message, sessionId })
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
