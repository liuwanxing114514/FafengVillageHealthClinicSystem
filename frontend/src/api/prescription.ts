import { deleteData, getData, postData, putData } from '@/api/http'
import type { AiDraft } from '@/types/ai'
import type {
  PrescriptionDetail,
  PrescriptionPrintData,
  SavePrescriptionPayload,
} from '@/types/prescription'

export async function getPrescription(id: number): Promise<PrescriptionDetail> {
  return getData<PrescriptionDetail>(`/prescriptions/${id}`)
}

export async function createPrescription(payload: SavePrescriptionPayload): Promise<PrescriptionDetail> {
  return postData<PrescriptionDetail>('/prescriptions', payload)
}

export async function updatePrescription(
  id: number,
  payload: SavePrescriptionPayload,
): Promise<PrescriptionDetail> {
  return putData<PrescriptionDetail>(`/prescriptions/${id}`, payload)
}

export async function voidPrescription(id: number): Promise<void> {
  await deleteData(`/prescriptions/${id}`)
}

export async function getPrescriptionPrint(id: number): Promise<PrescriptionPrintData> {
  return getData<PrescriptionPrintData>(`/prescriptions/${id}/print`)
}

export async function generateOutboundDraft(id: number): Promise<AiDraft> {
  return postData<AiDraft>(`/prescriptions/${id}/outbound-draft`)
}

export async function listPrescriptionsByVisit(visitId: number): Promise<PrescriptionDetail[]> {
  return getData<PrescriptionDetail[]>(`/prescriptions?visitId=${visitId}`)
}
