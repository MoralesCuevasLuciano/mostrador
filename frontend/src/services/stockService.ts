import type { StockBalance, StockMovement, StockTransfer } from '../models/stock'
import { getJson, sendJson } from './http'

/** Llamadas a /api/stock. */

/** GET /api/stock/{branchId} — variantes ya contadas en esa sucursal. */
export function fetchStockByBranch(branchId: number) {
  return getJson<StockBalance[]>(`/api/stock/${branchId}`, 'No se pudo cargar el inventario')
}

/** GET /api/stock/{branchId}/{variantId}/movements — historial. */
export function fetchStockMovements(branchId: number, variantId: number) {
  return getJson<StockMovement[]>(
    `/api/stock/${branchId}/${variantId}/movements`,
    'No se pudo cargar el historial',
  )
}

/** POST recuento: el usuario dice cuántas hay ahora. */
export function recountStock(
  branchId: number,
  variantId: number,
  countedQuantity: number,
  description: string,
) {
  return sendJson<StockBalance>(
    `/api/stock/${branchId}/${variantId}/recount`,
    'No se pudo guardar el recuento',
    {
      method: 'POST',
      body: JSON.stringify({
        countedQuantity,
        description: description.trim() === '' ? null : description.trim(),
      }),
    },
  )
}

/** POST entrada de mercadería. */
export function registerStockEntry(
  branchId: number,
  variantId: number,
  quantity: number,
  description: string,
) {
  return sendJson<StockBalance>(
    `/api/stock/${branchId}/${variantId}/entries`,
    'No se pudo registrar la entrada',
    {
      method: 'POST',
      body: JSON.stringify({
        quantity,
        description: description.trim() === '' ? null : description.trim(),
      }),
    },
  )
}

/** POST consumo interno. */
export function registerInternalConsumption(
  branchId: number,
  variantId: number,
  quantity: number,
  description: string,
) {
  return sendJson<StockBalance>(
    `/api/stock/${branchId}/${variantId}/internal-consumption`,
    'No se pudo registrar el consumo interno',
    {
      method: 'POST',
      body: JSON.stringify({
        quantity,
        description: description.trim() === '' ? null : description.trim(),
      }),
    },
  )
}

/** POST extravío. */
export function registerStockLoss(
  branchId: number,
  variantId: number,
  quantity: number,
  description: string,
) {
  return sendJson<StockBalance>(
    `/api/stock/${branchId}/${variantId}/losses`,
    'No se pudo registrar el extravío',
    {
      method: 'POST',
      body: JSON.stringify({
        quantity,
        description: description.trim() === '' ? null : description.trim(),
      }),
    },
  )
}

/** POST traslado a otro local. */
export function transferStock(
  variantId: number,
  fromBranchId: number,
  toBranchId: number,
  quantity: number,
  description: string,
) {
  return sendJson<StockTransfer>('/api/stock/transfers', 'No se pudo registrar el traslado', {
    method: 'POST',
    body: JSON.stringify({
      variantId,
      fromBranchId,
      toBranchId,
      quantity,
      description: description.trim() === '' ? null : description.trim(),
    }),
  })
}
