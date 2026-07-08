export interface PrescriptionItem {
  id?: number
  medicineId: number
  dosageForm?: string
  medicineName: string
  specification?: string
  quantity: number
  unit: string
  usage?: string
  sortOrder?: number
}

export interface PrescriptionDetail {
  id: number
  patientId: number
  patientName: string
  visitId: number
  prescriptionDate: string
  diagnosis?: string
  status: string
  items: PrescriptionItem[]
  createdAt: string
  updatedAt: string
}

export interface SavePrescriptionPayload {
  patientId: number
  visitId: number
  prescriptionDate?: string
  diagnosis?: string
  items: Array<{
    medicineId: number
    quantity: number
    unit: string
    usage?: string
  }>
}

export interface PrescriptionPrintData {
  clinicName: string
  title: string
  patientName: string
  gender: string
  age?: number
  address: string
  phone: string
  diagnosis?: string
  prescriptionDate: string
  items: PrescriptionItem[]
  doctorSignatureLabel: string
}

export interface OutboundDraft {
  prescriptionId: number
  patientId: number
  patientName: string
  diagnosis?: string
  items: Array<{
    medicineId: number
    medicineName: string
    specification?: string
    quantity: number
    unit: string
    usage?: string
  }>
}
