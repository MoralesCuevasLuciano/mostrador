/** Tipos de movimiento, iguales al enum del backend. */
export type StockMovementType =
  | 'VENTA'
  | 'ANULACION_VENTA'
  | 'AJUSTE_INICIAL'
  | 'AJUSTE_RECUENTO'
  | 'CONSUMO_INTERNO'
  | 'TRASLADO'
  | 'ENTRADA'
  | 'EXTRAVÍO'

/** Saldo de una variante en un local. */
export type StockBalance = {
  id: number | null
  variantId: number
  sku: string
  variantLabel: string
  branchId: number
  branchName: string
  quantity: number | null
  minQuantity: number | null
  inventoried: boolean
}

/** Movimiento de stock, como sale del historial. */
export type StockMovement = {
  id: number
  variantId: number
  sku: string
  branchId: number
  branchName: string
  movementType: StockMovementType
  quantity: number
  relatedMovementId: number | null
  description: string | null
  movementAt: string
}

/** Resultado de un traslado entre sucursales. */
export type StockTransfer = {
  outbound: StockMovement
  inbound: StockMovement
  from: StockBalance
  to: StockBalance
}

/** Fila de inventario en pantalla: saldo más nombre de producto. */
export type StockItem = {
  variantId: number
  sku: string
  variantLabel: string
  productName: string
  quantity: number | null
  inventoried: boolean
  variantActive: boolean
}
