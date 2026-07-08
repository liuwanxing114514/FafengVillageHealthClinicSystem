import { getData, postData, putData } from '@/api/http'
import type { SaveVisitPayload, VisitDetail } from '@/types/visit'

export async function getVisit(id: number): Promise<VisitDetail> {
  return getData<VisitDetail>(`/visits/${id}`)
}

export async function createVisit(payload: SaveVisitPayload): Promise<VisitDetail> {
  return postData<VisitDetail>('/visits', payload)
}

export async function updateVisit(id: number, payload: SaveVisitPayload): Promise<VisitDetail> {
  return putData<VisitDetail>(`/visits/${id}`, payload)
}
