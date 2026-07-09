import { deleteData, getData, postData, putData } from '@/api/http'
import type { PageResult } from '@/types/medicine'
import type {
  SaveVisitPayload,
  VisitDetail,
  VisitFeeSummary,
  VisitListItem,
  VisitSearchParams,
} from '@/types/visit'

export async function searchVisits(params: VisitSearchParams): Promise<PageResult<VisitListItem>> {
  const query = new URLSearchParams()
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.dateFrom) query.set('dateFrom', params.dateFrom)
  if (params.dateTo) query.set('dateTo', params.dateTo)
  if (params.arrearsOnly) query.set('arrearsOnly', 'true')
  query.set('page', String(params.page ?? 1))
  query.set('size', String(params.size ?? 20))
  return getData<PageResult<VisitListItem>>(`/visits?${query.toString()}`)
}

export async function getVisit(id: number): Promise<VisitDetail> {
  return getData<VisitDetail>(`/visits/${id}`)
}

export async function getVisitFeeSummary(id: number): Promise<VisitFeeSummary> {
  return getData<VisitFeeSummary>(`/visits/${id}/fee-summary`)
}

export async function createVisit(payload: SaveVisitPayload): Promise<VisitDetail> {
  return postData<VisitDetail>('/visits', payload)
}

export async function updateVisit(id: number, payload: SaveVisitPayload): Promise<VisitDetail> {
  return putData<VisitDetail>(`/visits/${id}`, payload)
}

export async function deleteVisit(id: number): Promise<void> {
  await deleteData(`/visits/${id}`)
}
