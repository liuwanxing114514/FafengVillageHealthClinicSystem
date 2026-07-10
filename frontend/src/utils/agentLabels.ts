/** Agent 工具与状态的中文展示 */
export const AGENT_TOOL_LABELS: Record<string, string> = {
  searchPatient: '查患者',
  searchPatientVisit: '查病历',
  searchMedicine: '查药品',
  queryInventory: '查库存',
  queryExpiringMedicine: '查临期药',
  generateOutboundDraft: '生成出库草稿',
}

export const AI_PROVIDER_LABELS: Record<string, string> = {
  deepseek: '对话模型已就绪',
  noop: '未启用',
  local: '本地模型',
}

export function toolLabel(toolName: string): string {
  return AGENT_TOOL_LABELS[toolName] ?? toolName
}

export function providerLabel(provider: string, enabled: boolean): string {
  if (!enabled) return '未启用'
  return AI_PROVIDER_LABELS[provider] ?? '已连接'
}

export function formatToolArgs(argsSummary: string): string {
  if (!argsSummary?.trim()) return ''
  try {
    const args = JSON.parse(argsSummary) as Record<string, unknown>
    const parts: string[] = []
    if (typeof args.keyword === 'string' && args.keyword) {
      parts.push(`关键词：${args.keyword}`)
    }
    if (typeof args.medicineName === 'string' && args.medicineName) {
      parts.push(`药品：${args.medicineName}`)
    }
    if (typeof args.barcode === 'string' && args.barcode) {
      parts.push(`条码：${args.barcode}`)
    }
    if (args.page != null || args.size != null) {
      parts.push(`第 ${args.page ?? 1} 页，${args.size ?? 10} 条`)
    }
    if (parts.length > 0) return parts.join(' · ')
  } catch {
    // 原样展示
  }
  return argsSummary
}
