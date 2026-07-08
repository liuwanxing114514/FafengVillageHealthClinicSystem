import { deleteData, getData, postData, putData } from '@/api/http'
import type { PageResult, PatientDetail, PatientListItem, SavePatientPayload } from '@/types/patient'
import type { VisitListItem } from '@/types/visit'

export interface PatientSearchParams {
  keyword?: string
  name?: string
  phone?: string
  idCard?: string
  address?: string
  gender?: string
  remark?: string
  ageMin?: number
  ageMax?: number
  page?: number
  size?: number
}

export async function searchPatients(params: PatientSearchParams): Promise<PageResult<PatientListItem>> {
  const query = new URLSearchParams()
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.name) query.set('name', params.name)
  if (params.phone) query.set('phone', params.phone)
  if (params.idCard) query.set('idCard', params.idCard)
  if (params.address) query.set('address', params.address)
  if (params.gender) query.set('gender', params.gender)
  if (params.remark) query.set('remark', params.remark)
  if (params.ageMin != null) query.set('ageMin', String(params.ageMin))
  if (params.ageMax != null) query.set('ageMax', String(params.ageMax))
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

export async function deletePatient(id: number): Promise<void> {
  await deleteData(`/patients/${id}`)
}

export async function listPatientVisits(patientId: number): Promise<VisitListItem[]> {
  return getData<VisitListItem[]>(`/patients/${patientId}/visits`)
}
