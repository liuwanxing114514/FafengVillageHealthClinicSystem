import type { AgentToolCall } from '@/types/agent'

export interface FollowUpChip {
  label: string
  /** 填入输入框并发送 */
  message?: string
  /** 直接路由跳转 */
  route?: string
}

export function buildFollowUpChips(toolCalls: AgentToolCall[] | undefined): FollowUpChip[] {
  if (!toolCalls?.length) return []
  const names = new Set(toolCalls.map((t) => t.toolName))
  const chips: FollowUpChip[] = []

  if (names.has('searchMedicine')) {
    chips.push({ label: '查库存', message: '查一下上面药品的库存' })
    chips.push({ label: '查看药品详情', message: '打开药品详情' })
  }
  if (names.has('queryInventory')) {
    chips.push({ label: '查看药品详情', message: '查看该药品详情' })
  }
  if (names.has('queryExpiringMedicine')) {
    chips.push({ label: '打开临期预警页', route: '/inventory/alerts' })
  }
  if (names.has('searchPatient')) {
    chips.push({ label: '查病历', message: '查这位患者的病历' })
  }
  if (names.has('searchPatientVisit')) {
    chips.push({ label: '查看病历', message: '打开最近一条病历' })
  }

  return chips.slice(0, 3)
}
