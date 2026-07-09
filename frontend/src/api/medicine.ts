import { deleteData, getData, patchData, postData, putData } from '@/api/http'
import type {
  BarcodeItem,
  ConversionItem,
  MedicineDetail,
  MedicineListItem,
  PageResult,
  SaveBarcodePayload,
  SaveConversionPayload,
  SaveMedicinePayload,
} from '@/types/medicine'

export async function searchMedicines(params: {
  keyword?: string
  status?: string
  page?: number
  size?: number
}): Promise<PageResult<MedicineListItem>> {
  const query = new URLSearchParams()
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.status) query.set('status', params.status)
  query.set('page', String(params.page ?? 1))
  query.set('size', String(params.size ?? 20))
  return getData<PageResult<MedicineListItem>>(`/medicines?${query}`)
}

export async function getMedicine(id: number): Promise<MedicineDetail> {
  return getData<MedicineDetail>(`/medicines/${id}`)
}

export async function findMedicineByBarcode(code: string): Promise<MedicineListItem> {
  return getData<MedicineListItem>(`/medicines/by-barcode/${encodeURIComponent(code.trim())}`)
}

export async function createMedicine(payload: SaveMedicinePayload): Promise<MedicineDetail> {
  return postData<MedicineDetail>('/medicines', payload)
}

export async function updateMedicine(
  id: number,
  payload: SaveMedicinePayload,
): Promise<MedicineDetail> {
  return putData<MedicineDetail>(`/medicines/${id}`, payload)
}

export async function updateMedicineStatus(
  id: number,
  status: 'ACTIVE' | 'INACTIVE',
): Promise<MedicineDetail> {
  return patchData<MedicineDetail>(`/medicines/${id}/status`, { status })
}

export async function deleteMedicine(id: number): Promise<void> {
  await deleteData(`/medicines/${id}`)
}

export async function addConversion(
  medicineId: number,
  payload: SaveConversionPayload,
): Promise<ConversionItem> {
  return postData<ConversionItem>(`/medicines/${medicineId}/conversions`, payload)
}

export async function deleteConversion(medicineId: number, conversionId: number): Promise<void> {
  await deleteData(`/medicines/${medicineId}/conversions/${conversionId}`)
}

export async function addBarcode(
  medicineId: number,
  payload: SaveBarcodePayload,
): Promise<BarcodeItem> {
  return postData<BarcodeItem>(`/medicines/${medicineId}/barcodes`, payload)
}

export async function deleteBarcode(medicineId: number, barcodeId: number): Promise<void> {
  await deleteData(`/medicines/${medicineId}/barcodes/${barcodeId}`)
}
