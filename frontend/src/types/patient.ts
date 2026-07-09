export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface PatientListItem {
  id: number
  name: string
  gender: string
  idCard: string | null
  birthDate: string | null
  age: number | null
  ageManual: boolean
  phone: string
  address: string
  updatedAt: string
}

export interface PatientDetail {
  id: number
  name: string
  gender: string
  idCard: string | null
  birthDate: string | null
  age: number | null
  ageManual: boolean
  phone: string
  address: string
  remark: string | null
  totalArrears: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface SavePatientPayload {
  name: string
  gender: 'M' | 'F' | 'UNKNOWN'
  idCard?: string
  birthDate?: string
  age?: number
  phone?: string
  address?: string
  remark?: string
}
