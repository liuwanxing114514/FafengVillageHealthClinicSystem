/** Agent 工具名、Provider 的中文展示映射（仅 UI，不改后端协议）。 */

export const AGENT_TOOL_LABELS: Record<string, string> = {
  searchMedicine: '查药品',
  queryInventory: '查库存',
  queryExpiringMedicine: '查临期',
  searchPatient: '查患者',
  searchPatientVisit: '查病历',
  generateOutboundDraft: '生成出库草稿',
}

const PROVIDER_LABELS: Record<string, string> = {
  deepseek: 'DeepSeek',
  noop: '未启用',
  local: '本地模型',
}

export function toolLabel(toolName: string): string {
  return AGENT_TOOL_LABELS[toolName] ?? toolName
}

export function providerLabel(provider: string, available: boolean): string {
  if (!available) return 'AI 不可用'
  return PROVIDER_LABELS[provider] ?? provider
}

/** 将 args JSON 转为简短中文可读串 */
export function formatToolArgs(argsSummary: string | undefined): string {
  if (!argsSummary) return ''
  try {
    const obj = JSON.parse(argsSummary) as Record<string, unknown>
    const parts: string[] = []
    if (obj.keyword != null && String(obj.keyword)) parts.push(`关键词: ${obj.keyword}`)
    if (obj.medicineName != null && String(obj.medicineName)) parts.push(`药品: ${obj.medicineName}`)
    if (obj.medicineId != null) parts.push(`药品ID: ${obj.medicineId}`)
    if (obj.patientId != null) parts.push(`患者ID: ${obj.patientId}`)
    if (obj.page != null) parts.push(`页: ${obj.page}`)
    if (obj.size != null) parts.push(`条数: ${obj.size}`)
    return parts.join(' · ')
  } catch {
    return argsSummary.length > 80 ? argsSummary.slice(0, 80) + '…' : argsSummary
  }
}

export function formatDisplayToolArgs(call: { displayArgsSummary?: string; argsSummary: string }): string {
  const raw = call.displayArgsSummary ?? call.argsSummary
  return formatToolArgs(raw) || raw
}
