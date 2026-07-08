import http, { postData } from '@/api/http'
import type { MedicineImportConfirmResult, MedicineImportPreview } from '@/types/import'

export async function downloadMedicineImportTemplate(): Promise<void> {
  const response = await http.get('/import/medicine/template', { responseType: 'blob' })
  const blob = new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'medicine-import-template.xlsx'
  link.click()
  URL.revokeObjectURL(url)
}

export async function previewMedicineImport(file: File): Promise<MedicineImportPreview> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<{ code: number; data: MedicineImportPreview }>(
    '/import/medicine/preview',
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  if (data.code !== 0) {
    return Promise.reject(data)
  }
  return data.data
}

export async function confirmMedicineImport(previewId: string): Promise<MedicineImportConfirmResult> {
  return postData<MedicineImportConfirmResult>('/import/medicine/confirm', { previewId })
}
