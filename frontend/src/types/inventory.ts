export interface FlowItem {
  id: number
  medicineId: number
  medicineName: string
  batchId?: number
  batchNo?: string
  flowType: string
  quantityChange: number
  quantityBefore: number
  quantityAfter: number
  unit: string
  patientId?: number
  prescriptionId?: number
  reason?: string
  remark?: string
  operator: string
  createdAt: string
}

export interface BatchItem {
  id: number
  medicineId: number
  medicineName: string
  batchNo: string
  expiryDate?: string
  quantity: number
  baseUnit: string
  purchasePrice?: number
  supplier: string
  status: string
  createdAt: string
}

export interface BatchAllocation {
  batchId: number
  batchNo: string
  expiryDate?: string
  availableQuantity: number
  recommendedQuantity: number
}

export interface OutboundPreviewLine {
  medicineId: number
  medicineName: string
  requestedQuantity: number
  unit: string
  requestedBaseQuantity: number
  baseUnit: string
  sufficient: boolean
  recommendedAllocations: BatchAllocation[]
}

export interface OutboundPreview {
  sufficient: boolean
  lines: OutboundPreviewLine[]
}

export interface LowStockAlert {
  medicineId: number
  medicineName: string
  specification?: string
  currentQuantity: number
  threshold: number
  baseUnit: string
}

export interface ExpiringAlert {
  batchId: number
  medicineId: number
  medicineName: string
  batchNo: string
  expiryDate: string
  quantity: number
  baseUnit: string
}

export interface InventoryAlerts {
  lowStock: LowStockAlert[]
  expiring: ExpiringAlert[]
}

export interface DashboardSummary {
  lowStockCount: number
  expiringCount: number
  lowStockPreview: LowStockAlert[]
  expiringPreview: ExpiringAlert[]
}

export interface InboundPayload {
  medicineId: number
  quantity: number
  unit: string
  batchNo: string
  expiryDate?: string
  purchasePrice?: number
  supplier?: string
  remark?: string
}

export interface OutboundPreviewPayload {
  patientId: number
  prescriptionId: number
  items: Array<{ medicineId: number; quantity: number; unit: string }>
}

export interface OutboundConfirmPayload {
  patientId: number
  prescriptionId: number
  medicineId: number
  allocations: Array<{ batchId: number; quantity: number }>
}

export interface AdjustPayload {
  medicineId: number
  batchId: number
  quantityChange: number
  unit: string
  reason: string
  remark?: string
}
