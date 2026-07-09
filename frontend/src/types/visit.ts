export interface VisitListItem {
  id: number
  patientId: number
  patientName: string
  patientGender: string
  visitTime: string
  chiefComplaint: string | null
  diagnosis: string | null
  amountDue: number
  amountPaid: number
  balance: number
  status: string
}

export interface VisitDetail {
  id: number
  patientId: number
  patientName: string
  visitTime: string
  chiefComplaint: string | null
  presentIllness: string | null
  pastHistory: string | null
  temperature: number | null
  bloodPressure: string | null
  spo2: number | null
  etco2: number | null
  heartRate: number | null
  pulse: string | null
  allergyHistory: string | null
  diagnosis: string | null
  treatment: string | null
  remark: string | null
  amountDue: number
  amountPaid: number
  balance: number
  suggestedAmountDue: number
  referencePurchaseCost: number
  patientTotalArrears: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface VisitFeeSummary {
  suggestedAmountDue: number
  referencePurchaseCost: number
}

export interface SaveVisitPayload {
  patientId: number
  visitTime?: string
  chiefComplaint?: string
  presentIllness?: string
  pastHistory?: string
  temperature?: number | null
  bloodPressure?: string
  spo2?: number | null
  etco2?: number | null
  heartRate?: number | null
  pulse?: string
  allergyHistory?: string
  diagnosis?: string
  treatment?: string
  remark?: string
  amountDue?: number | null
  amountPaid?: number | null
}

export interface VisitSearchParams {
  keyword?: string
  dateFrom?: string
  dateTo?: string
  arrearsOnly?: boolean
  page?: number
  size?: number
}
