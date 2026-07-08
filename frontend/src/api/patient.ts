import { getData, postData, putData } from '@/api/http'
import type { PageResult, PatientDetail, PatientListItem, SavePatientPayload } from '@/types/patient'
import type { VisitListItem } from '@/types/visit'

export async function searchPatients(params: {
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<PatientListItem>> {
  const query = new URLSearchParams()
  if (params.keyword) query.set('keyword', params.keyword)
  query.set('page', String(params.page ?? 1))
  query.set('size', String(params.size ?? 20))
  return getData<PageResult<PatientListItem>>(`/patients?${query}`)
}

export async function getPatient(id: number): Promise<PatientDetail> {
  return getData<PatientDetail>(`/patients/${id}`)
}

export async function createPatient(payload: SavePatientPayload): Promise<PatientDetail> {
  return postData<PatientDetail>('/patients', payload)
}

export async function updatePatient(id: number, payload: SavePatientPayload): Promise<PatientDetail> {
  return putData<PatientDetail>(`/patients/${id}`, payload)
}

export async function listPatientVisits(patientId: number): Promise<VisitListItem[]> {
  return getData<VisitListItem[]>(`/patients/${patientId}/visits`)
}
