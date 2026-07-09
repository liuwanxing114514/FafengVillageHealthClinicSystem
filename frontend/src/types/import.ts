export interface MedicineImportRow {
  rowNumber: number
  valid: boolean
  errors: string[]
  name: string
  genericName: string
  dosageForm: string
  specification: string
  baseUnit: string
  packageUnit: string
  conversionText: string
  manufacturer: string
  barcode: string
  purchasePrice: number | null
  stockThreshold: number | null
  batchNo: string
  expiryDate: string | null
  initialStock: number | null
  remark: string
}

export interface MedicineImportPreview {
  previewId: string
  totalRows: number
  validCount: number
  errorCount: number
  canConfirm: boolean
  rows: MedicineImportRow[]
}

export interface MedicineImportConfirmResult {
  medicineCount: number
  inventoryCount: number
}
