import type { CashMovement, CashMovementType, CashSession } from '../models/cash'
import { getJson, getJsonOrNull, sendJson } from './http'

/** Llamadas a /api/cash. */

/** GET /api/cash/{branchId}/today — planilla de hoy; la abre si no existe. */
export function fetchCashToday(branchId: number) {
  return getJson<CashSession>(`/api/cash/${branchId}/today`, 'No se pudo abrir la caja')
}

/** GET planilla de un día. null si todavía no se abrió. */
export function fetchCashSession(branchId: number, date: string) {
  return getJsonOrNull<CashSession>(
    `/api/cash/${branchId}/sessions/${date}`,
    'No se pudo cargar la caja',
  )
}

/** GET /api/cash/{branchId}/sessions?from=&to= — planillas de ese rango. */
export function fetchCashSessions(branchId: number, from: string, to: string) {
  return getJson<CashSession[]>(
    `/api/cash/${branchId}/sessions?from=${from}&to=${to}`,
    'No se pudieron cargar las cajas',
  )
}

/** GET cajas abiertas de ese local. */
export function fetchOpenCashSessions(branchId: number) {
  return getJson<CashSession[]>(`/api/cash/${branchId}/open-sessions`, 'No se pudieron cargar las cajas')
}

/** GET la planilla anterior a esa fecha. null si nunca hubo. */
export function fetchPreviousCashSession(branchId: number, before: string) {
  return getJsonOrNull<CashSession>(
    `/api/cash/${branchId}/previous-session?before=${before}`,
    'No se pudo cargar la caja anterior',
  )
}

/** GET movimientos de una planilla. */
export function fetchCashMovements(branchId: number, date: string) {
  return getJson<CashMovement[]>(
    `/api/cash/${branchId}/sessions/${date}/movements`,
    'No se pudieron cargar los movimientos',
  )
}

/** POST recuento de apertura. */
export function countCashOpening(branchId: number, date: string, openingAmount: number) {
  return sendJson<CashSession>(
    `/api/cash/${branchId}/sessions/${date}/opening-count`,
    'No se pudo guardar el recuento',
    {
      method: 'POST',
      body: JSON.stringify({ openingAmount }),
    },
  )
}

const MOVEMENT_PATH: Record<CashMovementType, string> = {
  RETIRO_RESGUARDO: 'withdrawals',
  VALE: 'vales',
  GASTO: 'expenses',
  INGRESO_EFECTIVO: 'cash-ins',
}

/** POST retiro, vale, gasto o ingreso. El monto va positivo. */
export function registerCashMovement(
  branchId: number,
  date: string,
  movementType: CashMovementType,
  amount: number,
  description: string,
) {
  return sendJson<CashMovement>(
    `/api/cash/${branchId}/sessions/${date}/${MOVEMENT_PATH[movementType]}`,
    'No se pudo anotar el movimiento',
    {
      method: 'POST',
      body: JSON.stringify({
        amount,
        description: description.trim() === '' ? null : description.trim(),
      }),
    },
  )
}

/** PUT corrige un movimiento de una caja abierta. */
export function updateCashMovement(
  branchId: number,
  movementId: number,
  movementType: CashMovementType,
  amount: number,
  description: string,
) {
  return sendJson<CashMovement>(
    `/api/cash/${branchId}/movements/${movementId}`,
    'No se pudo corregir el movimiento',
    {
      method: 'PUT',
      body: JSON.stringify({
        movementType,
        amount,
        description: description.trim() === '' ? null : description.trim(),
      }),
    },
  )
}

/** DELETE borra un movimiento de una caja abierta. */
export function deleteCashMovement(branchId: number, movementId: number) {
  return sendJson<void>(`/api/cash/${branchId}/movements/${movementId}`, 'No se pudo borrar el movimiento', {
    method: 'DELETE',
  })
}

/** POST cierre de una planilla abierta. */
export function closeCashSession(
  branchId: number,
  date: string,
  closingAmount: number,
  note: string,
) {
  return sendJson<CashSession>(
    `/api/cash/${branchId}/sessions/${date}/close`,
    'No se pudo cerrar la caja',
    {
      method: 'POST',
      body: JSON.stringify({
        closingAmount,
        note: note.trim() === '' ? null : note.trim(),
      }),
    },
  )
}
