import type { Brand, Category, Product } from './types'

export type VariantDraft = {
  brandId: string
  label: string
  barcode: string
  price: string
  itemCondition: 'NUEVA' | 'DEFECTUOSA'
  imageUrl: string
}

export type ProductDraft = {
  name: string
  description: string
  categoryId: string
  allowsEmployeeDiscount: boolean
  variants: VariantDraft[]
}

async function readError(response: Response, fallback: string) {
  try {
    const body = await response.json()
    return typeof body.detail === 'string' ? body.detail : fallback
  } catch {
    return fallback
  }
}

async function getJson<T>(path: string, fallback: string): Promise<T> {
  const response = await fetch(path)
  if (!response.ok) {
    throw new Error(await readError(response, fallback))
  }
  return response.json()
}

export function fetchProducts() {
  return getJson<Product[]>('/api/products', 'No se pudo cargar el catálogo')
}

export function fetchBrands() {
  return getJson<Brand[]>('/api/brands', 'No se pudieron cargar las marcas')
}

export function fetchCategories() {
  return getJson<Category[]>('/api/categories', 'No se pudieron cargar las categorías')
}

export async function createBrand(name: string): Promise<Brand> {
  const response = await fetch('/api/brands', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: name.trim() }),
  })
  if (!response.ok) {
    throw new Error(await readError(response, 'No se pudo crear la marca'))
  }
  return response.json()
}

export async function createCategory(name: string, parentId: string): Promise<Category> {
  const response = await fetch('/api/categories', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: name.trim(),
      parentId: parentId === '' ? null : Number(parentId),
    }),
  })
  if (!response.ok) {
    throw new Error(await readError(response, 'No se pudo crear la categoría'))
  }
  return response.json()
}

function blankToNull(value: string) {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

function parsePrice(value: string) {
  const normalized = value.trim().replace(',', '.')
  const price = Number(normalized)
  if (!Number.isFinite(price) || price < 0) {
    throw new Error('El precio tiene que ser un número mayor o igual a cero')
  }
  return price.toFixed(2)
}

export async function createProduct(draft: ProductDraft): Promise<Product> {
  const variants = draft.variants.map((variant, index) => {
    const label = variant.label.trim() || (draft.variants.length === 1 ? 'Única' : '')
    if (!label) {
      throw new Error(`La variante ${index + 1} necesita un nombre`)
    }
    return {
      brandId: variant.brandId === '' ? null : Number(variant.brandId),
      label,
      barcode: blankToNull(variant.barcode),
      price: parsePrice(variant.price),
      itemCondition: variant.itemCondition,
      imageUrl: blankToNull(variant.imageUrl),
    }
  })

  const response = await fetch('/api/products', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: draft.name.trim(),
      description: blankToNull(draft.description),
      categoryId: draft.categoryId === '' ? null : Number(draft.categoryId),
      allowsEmployeeDiscount: draft.allowsEmployeeDiscount,
      variants,
    }),
  })
  if (!response.ok) {
    throw new Error(await readError(response, 'No se pudo crear el producto'))
  }
  return response.json()
}
