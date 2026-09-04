import { toVariantPayload } from '../mappers/productMapper'
import type { ProductDraft } from '../models/drafts'
import type { Product } from '../models/product'
import { getJson, sendJson } from './http'

/** Llamadas a /api/products: alta, edición, baja y reactivar. */
/** GET /api/products — catálogo completo (activos e inactivos). */
export function fetchProducts() {
  return getJson<Product[]>('/api/products', 'No se pudo cargar el catálogo')
}

/** POST /api/products — alta de ficha + variantes en un solo request. */
export function createProduct(draft: ProductDraft) {
  return sendJson<Product>('/api/products', 'No se pudo crear el producto', {
    method: 'POST',
    body: JSON.stringify({
      name: draft.name.trim(),
      description: draft.description.trim() === '' ? null : draft.description.trim(),
      categoryId: draft.categoryId === '' ? null : Number(draft.categoryId),
      allowsEmployeeDiscount: draft.allowsEmployeeDiscount,
      variants: draft.variants.map((variant, index) => toVariantPayload(draft, variant, index)),
    }),
  })
}

/**
 * Edición: PUT de la ficha, POST/PUT de cada variante del draft
 * y DELETE de las que se quitaron del formulario.
 */
export async function updateProduct(
  productId: number,
  original: Product,
  draft: ProductDraft,
): Promise<void> {
  await sendJson(`/api/products/${productId}`, 'No se pudo guardar el producto', {
    method: 'PUT',
    body: JSON.stringify({
      name: draft.name.trim(),
      description: draft.description.trim() === '' ? null : draft.description.trim(),
      categoryId: draft.categoryId === '' ? null : Number(draft.categoryId),
      allowsEmployeeDiscount: draft.allowsEmployeeDiscount,
    }),
  })

  const keptIds = new Set(draft.variants.map((variant) => variant.id).filter((id) => id != null))
  for (const [index, variant] of draft.variants.entries()) {
    const path = variant.id
      ? `/api/products/${productId}/variants/${variant.id}`
      : `/api/products/${productId}/variants`
    await sendJson(path, 'No se pudo guardar una variante', {
      method: variant.id ? 'PUT' : 'POST',
      body: JSON.stringify(toVariantPayload(draft, variant, index)),
    })
  }
  for (const variant of original.variants) {
    if (keptIds.has(variant.id) || !variant.active) {
      continue
    }
    await sendJson(
      `/api/products/${productId}/variants/${variant.id}`,
      'No se pudo dar de baja una variante',
      { method: 'DELETE' },
    )
  }
}

/** Baja: primero todas las variantes, después el producto. */
export async function deactivateProduct(id: number): Promise<void> {
  await sendJson(`/api/products/${id}/variants/deactivate`, 'No se pudieron dar de baja las variantes', {
    method: 'POST',
  })
  await sendJson(`/api/products/${id}`, 'No se pudo dar de baja el producto', { method: 'DELETE' })
}

/** Reactiva el producto y todas las variantes que estaban dadas de baja. */
export async function reactivateProduct(product: Product): Promise<void> {
  await sendJson(`/api/products/${product.id}/activate`, 'No se pudo reactivar el producto', {
    method: 'POST',
  })
  for (const variant of product.variants) {
    if (variant.active) {
      continue
    }
    await sendJson(
      `/api/products/${product.id}/variants/${variant.id}/activate`,
      'No se pudo reactivar una variante',
      { method: 'POST' },
    )
  }
}
