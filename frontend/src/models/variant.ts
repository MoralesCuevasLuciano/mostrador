import type { BrandSummary } from './brand'

/** Condición de la unidad: igual que el enum del backend. */
export type ItemCondition = 'NUEVA' | 'DEFECTUOSA'

/** Variante vendible (SKU, precio, foto, código de barras). */
export type Variant = {
  id: number
  sku: string
  label: string
  barcode: string | null
  price: number
  itemCondition: ItemCondition
  imageUrl: string | null
  active: boolean
  brand: BrandSummary | null
}
