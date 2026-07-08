import { getData, postData } from '@/api/http'
import type { PageResult } from '@/types/medicine'
import type {
  AdjustPayload,
  BatchItem,
  DashboardSummary,
  FlowItem,
  InboundPayload,
  InventoryAlerts,
  OutboundConfirmPayload,
  OutboundPreview,
  OutboundPreviewPayload,
} from '@/types/inventory'

export async function inbound(payload: InboundPayload): Promise<FlowItem> {
  return postData<FlowItem>('/inventory/inbound', payload)
}

export async function previewOutbound(payload: OutboundPreviewPayload): Promise<OutboundPreview> {
  return postData<OutboundPreview>('/inventory/outbound/preview', payload)
}

export async function confirmOutbound(payload: OutboundConfirmPayload): Promise<FlowItem[]> {
  return postData<FlowItem[]>('/inventory/outbound', payload)
}

export async function adjustInventory(payload: AdjustPayload): Promise<FlowItem> {
  return postData<FlowItem>('/inventory/adjust', payload)
}

export async function listBatches(medicineId?: number): Promise<BatchItem[]> {
  const query = medicineId ? `?medicineId=${medicineId}` : ''
  return getData<BatchItem[]>(`/inventory/batches${query}`)
}

export async function listFlows(params: {
  medicineId?: number
  flowType?: string
  page?: number
  size?: number
}): Promise<PageResult<FlowItem>> {
  const query = new URLSearchParams()
  if (params.medicineId) query.set('medicineId', String(params.medicineId))
  if (params.flowType) query.set('flowType', params.flowType)
  query.set('page', String(params.page ?? 1))
  query.set('size', String(params.size ?? 20))
  return getData<PageResult<FlowItem>>(`/inventory/flows?${query}`)
}

export async function getInventoryAlerts(): Promise<InventoryAlerts> {
  return getData<InventoryAlerts>('/inventory/alerts')
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  return getData<DashboardSummary>('/dashboard/summary')
}
