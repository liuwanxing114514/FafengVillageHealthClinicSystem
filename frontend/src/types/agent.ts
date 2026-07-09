export interface AgentToolCall {
  toolName: string
  argsSummary: string
  resultSummary: string
  durationMs: number
  success: boolean
}

export interface PendingAction {
  draftId: number
  draftType: string
  summary: string
}

export interface AgentChatResponse {
  sessionId: string
  answer: string
  toolCalls: AgentToolCall[]
  pendingActions: PendingAction[]
}

export interface AgentExecutionLog {
  id: number
  sessionId: string
  toolName: string
  argsSummary: string
  resultSummary: string
  durationMs: number
  createdAt: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  toolCalls?: AgentToolCall[]
  pendingActions?: PendingAction[]
}
