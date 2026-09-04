import type { Brand } from '../models/brand'
import { getJson, sendJson } from './http'

/** Llamadas a /api/brands. */
/** GET /api/brands — listado para el select del formulario. */
export function fetchBrands() {
  return getJson<Brand[]>('/api/brands', 'No se pudieron cargar las marcas')
}

/** POST /api/brands — alta inline desde el formulario. */
export function createBrand(name: string) {
  return sendJson<Brand>('/api/brands', 'No se pudo crear la marca', {
    method: 'POST',
    body: JSON.stringify({ name: name.trim() }),
  })
}

/** PUT /api/brands/{id} — cambia el nombre. */
export function updateBrand(id: number, name: string) {
  return sendJson<Brand>(`/api/brands/${id}`, 'No se pudo guardar la marca', {
    method: 'PUT',
    body: JSON.stringify({ name: name.trim() }),
  })
}

/** DELETE /api/brands/{id} — baja lógica. */
export function deactivateBrand(id: number) {
  return sendJson<Brand>(`/api/brands/${id}`, 'No se pudo dar de baja la marca', {
    method: 'DELETE',
  })
}

/** POST /api/brands/{id}/activate — reactivar. */
export function reactivateBrand(id: number) {
  return sendJson<Brand>(`/api/brands/${id}/activate`, 'No se pudo reactivar la marca', {
    method: 'POST',
  })
}
