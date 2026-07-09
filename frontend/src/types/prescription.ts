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

export interface PrescriptionPrintField {
  key: string
  topMm: number
  leftMm: number
  fontSizePt: number
}

export interface PrescriptionPrintTemplate {
  type: 'full-page' | 'overlay'
  title?: string
  fields?: PrescriptionPrintField[]
  staticValues?: Record<string, string>
  itemsArea?: {
    topMm: number
    leftMm: number
    lineHeightMm: number
    fontSizePt: number
    rpLabel?: string
  }
}

export interface PrescriptionPrintConfig {
  templates: Record<string, PrescriptionPrintTemplate>
}

export interface PrescriptionPrintData {
  clinicName: string
  title: string
  activeTemplate: string
  templateConfigJson: string
  visitRecordNo: number
  department: string
  patientName: string
  gender: string
  age?: number
  address: string
  phone: string
  diagnosis?: string
  prescriptionDate: string
  prescriptionYear?: number
  prescriptionMonth?: number
  prescriptionDay?: number
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
