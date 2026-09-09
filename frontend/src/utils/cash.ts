import type { CashMovementType } from '../models/cash'
import { formatPrice } from './money'

const MOVEMENT_LABELS: Record<CashMovementType, string> = {
  RETIRO_RESGUARDO: 'Retiro',
  VALE: 'Vale',
  GASTO: 'Gasto',
  INGRESO_EFECTIVO: 'Ingreso',
}

/** Tipos que se pueden anotar a mano en la planilla. */
export const CASH_MOVEMENT_TYPES: CashMovementType[] = [
  'RETIRO_RESGUARDO',
  'VALE',
  'GASTO',
  'INGRESO_EFECTIVO',
]

/** Lunes y domingo de una semana, en `yyyy-MM-dd`. */
export type WeekRange = {
  from: string
  to: string
}

/** Hoy en la zona del negocio, `yyyy-MM-dd`. */
export function todayIso() {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Argentina/Buenos_Aires',
  }).format(new Date())
}

/** Convierte `yyyy-MM-dd` a Date local, sin UTC. */
function parseIso(iso: string) {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day)
}

/** Date local → `yyyy-MM-dd`. */
function toIso(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** Suma (o resta) días a una fecha ISO. */
function addDays(iso: string, days: number) {
  const date = parseIso(iso)
  date.setDate(date.getDate() + days)
  return toIso(date)
}

/** Semana lunes–domingo que contiene esa fecha. */
export function weekContaining(iso: string): WeekRange {
  const date = parseIso(iso)
  const weekday = date.getDay()
  const toMonday = weekday === 0 ? -6 : 1 - weekday
  const from = addDays(iso, toMonday)
  return { from, to: addDays(from, 6) }
}

/** Corre la semana `weeks` hacia adelante o atrás. */
export function shiftWeek(from: string, weeks: number): WeekRange {
  return weekContaining(addDays(from, weeks * 7))
}

/** Rótulo corto de una semana: 7 – 13 de septiembre. */
export function formatWeekRange(from: string, to: string) {
  const start = parseIso(from)
  const end = parseIso(to)
  const sameMonth = start.getMonth() === end.getMonth() && start.getFullYear() === end.getFullYear()
  if (sameMonth) {
    return `${start.getDate()} – ${end.toLocaleDateString('es-AR', { day: 'numeric', month: 'long' })}`
  }
  return `${start.toLocaleDateString('es-AR', { day: 'numeric', month: 'short' })} – ${end.toLocaleDateString('es-AR', { day: 'numeric', month: 'short' })}`
}

/** Fecha de planilla para mostrar (lunes 9 de septiembre). */
export function formatBusinessDate(iso: string) {
  return parseIso(iso).toLocaleDateString('es-AR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  })
}

/** Hora del movimiento en la planilla. */
export function formatMovementAt(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('es-AR', { hour: '2-digit', minute: '2-digit' })
}

/** Texto en español de un tipo de movimiento. */
export function cashMovementLabel(type: CashMovementType) {
  return MOVEMENT_LABELS[type]
}

/** Interpreta el input de plata; coma o punto. */
export function parseMoney(value: string) {
  const parsed = Number(value.trim().replace(',', '.'))
  if (!Number.isFinite(parsed)) {
    return null
  }
  return parsed
}

/** Texto corto de la diferencia de cierre: falta, sobra o cuadra. */
export function differenceLabel(difference: number) {
  const rounded = Math.round(difference * 100) / 100
  if (rounded === 0) {
    return 'Cuadra'
  }
  return rounded > 0 ? `Sobra ${formatPrice(rounded)}` : `Falta ${formatPrice(Math.abs(rounded))}`
}
