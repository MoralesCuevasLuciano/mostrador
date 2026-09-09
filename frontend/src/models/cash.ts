/** Planilla de caja, tal como sale de /api/cash. */
export type CashSession = {
  id: number
  branchId: number
  branchName: string
  businessDate: string
  openingAmount: number
  openingCountedAt: string | null
  openingCounted: boolean
  totalCashSales: number
  totalCashOut: number
  totalCashIn: number
  closingAmount: number | null
  closedAt: string | null
  open: boolean
  expectedAmount: number
  difference: number | null
  note: string | null
}

/** Tipos de movimiento que no son venta. */
export type CashMovementType = 'RETIRO_RESGUARDO' | 'VALE' | 'GASTO' | 'INGRESO_EFECTIVO'

/** Retiro, vale, gasto o ingreso de una planilla. amount viene con signo. */
export type CashMovement = {
  id: number
  sessionId: number
  movementType: CashMovementType
  amount: number
  description: string | null
  movementAt: string
}
