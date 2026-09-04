import type { Category } from '../models/category'
import { getJson, sendJson } from './http'

/** Llamadas a /api/categories. */
/** GET /api/categories — listado para el select del formulario. */
export function fetchCategories() {
  return getJson<Category[]>('/api/categories', 'No se pudieron cargar las categorías')
}

/** POST /api/categories — alta inline. parentId vacío = rubro raíz. */
export function createCategory(name: string, parentId: string) {
  return sendJson<Category>('/api/categories', 'No se pudo crear la categoría', {
    method: 'POST',
    body: JSON.stringify({
      name: name.trim(),
      parentId: parentId === '' ? null : Number(parentId),
    }),
  })
}

/** PUT /api/categories/{id} — cambia nombre o padre. */
export function updateCategory(id: number, name: string, parentId: string) {
  return sendJson<Category>(`/api/categories/${id}`, 'No se pudo guardar la categoría', {
    method: 'PUT',
    body: JSON.stringify({
      name: name.trim(),
      parentId: parentId === '' ? null : Number(parentId),
    }),
  })
}

/** DELETE /api/categories/{id} — baja lógica. */
export function deactivateCategory(id: number) {
  return sendJson<Category>(`/api/categories/${id}`, 'No se pudo dar de baja la categoría', {
    method: 'DELETE',
  })
}

/** POST /api/categories/{id}/activate — reactivar. */
export function reactivateCategory(id: number) {
  return sendJson<Category>(`/api/categories/${id}/activate`, 'No se pudo reactivar la categoría', {
    method: 'POST',
  })
}
