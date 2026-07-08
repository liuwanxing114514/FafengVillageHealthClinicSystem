export interface MedicineListItem {
  id: number
  name: string
  genericName: string
  dosageForm: string
  specification: string
  baseUnit: string
  packageUnit: string
  manufacturer: string
  purchasePrice: number
  stockThreshold: number
  stockThresholdInPackages: number
  status: 'ACTIVE' | 'INACTIVE'
  barcodes: string[]
}

export interface MedicineDetail {
  id: number
  name: string
  genericName: string
  dosageForm: string
  specification: string
  baseUnit: string
  packageUnit: string
  manufacturer: string
  purchasePrice: number
  stockThreshold: number
  stockThresholdInPackages: number
  status: 'ACTIVE' | 'INACTIVE'
  pinyinAbbr: string
  remark: string
  createdAt: string
  updatedAt: string
  conversions: ConversionItem[]
  barcodes: BarcodeItem[]
}

export interface ConversionItem {
  id: number
  fromUnit: string
  toUnit: string
  factor: number
}

export interface BarcodeItem {
  id: number
  barcode: string
  remark: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface SaveMedicinePayload {
  name: string
  genericName?: string
  dosageForm?: string
  specification?: string
  baseUnit: string
  packageUnit?: string
  manufacturer?: string
  purchasePrice: number
  stockThreshold?: number | null
  remark?: string
}

export interface SaveConversionPayload {
  fromUnit: string
  toUnit: string
  factor: number
}

export interface SaveBarcodePayload {
  barcode: string
  remark?: string
}
