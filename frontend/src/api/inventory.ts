import { getData, postData } from '@/api/http'
import http from '@/api/http'
import type { PageResult } from '@/types/medicine'
import type {
  AdjustPayload,
  BatchItem,
  BatchOutboundConfirmPayload,
  BatchOutboundPreviewPayload,
  BatchOutboundResult,
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

export async function previewBatchOutbound(payload: BatchOutboundPreviewPayload): Promise<OutboundPreview> {
  return postData<OutboundPreview>('/inventory/outbound/batch/preview', payload)
}

export async function confirmBatchOutbound(payload: BatchOutboundConfirmPayload): Promise<BatchOutboundResult> {
  return postData<BatchOutboundResult>('/inventory/outbound/batch', payload)
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

export async function exportInventoryFlows(params: {
  medicineId?: number
  flowType?: string
}): Promise<void> {
  const query = new URLSearchParams()
  if (params.medicineId) query.set('medicineId', String(params.medicineId))
  if (params.flowType) query.set('flowType', params.flowType)
  const suffix = query.toString() ? `?${query}` : ''
  const response = await http.get(`/inventory/flows/export${suffix}`, { responseType: 'blob' })
  const blob = new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  const date = new Date().toISOString().slice(0, 10)
  link.href = url
  link.download = `inventory-flows-${date}.xlsx`
  link.click()
  URL.revokeObjectURL(url)
}

export async function getInventoryAlerts(): Promise<InventoryAlerts> {
  return getData<InventoryAlerts>('/inventory/alerts')
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  return getData<DashboardSummary>('/dashboard/summary')
}
