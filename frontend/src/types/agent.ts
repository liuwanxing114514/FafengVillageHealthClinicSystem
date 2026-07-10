/** Agent 单次工具调用记录 */
export interface AgentToolCall {
  toolName: string
  argsSummary: string
  displayArgsSummary?: string
  resultSummary: string
  dataJson?: string
  durationMs: number
  success: boolean
}

export interface PendingAction {
  draftId: number
  draftType: string
  summary: string
}

export interface AgentReference {
  refType: 'patient' | 'visit' | 'medicine' | string
  refId: number
  label: string
  hint?: string
}

export interface AgentChatResponse {
  sessionId: string
  answer: string
  toolCalls: AgentToolCall[]
  pendingActions: PendingAction[]
  references: AgentReference[]
}

export interface AgentConversation {
  id: string
  title: string
  messageCount: number
  createdAt: string
  updatedAt: string
}

export interface AgentMessageRecord {
  id: number
  role: 'user' | 'assistant'
  content: string
  toolCalls: AgentToolCall[]
  references: AgentReference[]
  pendingActions: PendingAction[]
  createdAt: string
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
  failed?: boolean
  toolCalls?: AgentToolCall[]
  pendingActions?: PendingAction[]
  references?: AgentReference[]
}

export const EXAMPLE_PROMPTS = [
  '搜索患者',
  '最近一位患者是谁',
  '阿莫西林还有多少',
  '哪些药快过期',
] as const

export const DRAFT_STORAGE_KEY = 'clinic-agent-draft'

/** 侧栏本地草稿会话 id（仅前端，不落库） */
export const DRAFT_CONVERSATION_ID = '__draft__'
