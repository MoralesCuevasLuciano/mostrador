import type { Branch } from '../models/branch'
import { getJson } from './http'

/** Llamadas a /api/branches. */
/** GET /api/branches — sucursales para el switch de la barra. */
export function fetchBranches() {
  return getJson<Branch[]>('/api/branches', 'No se pudieron cargar las sucursales')
}
