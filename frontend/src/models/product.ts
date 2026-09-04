import type { CategorySummary } from './category'
import type { Variant } from './variant'

/** Producto del catálogo con sus variantes, tal como lo devuelve la API. */
export type Product = {
  id: number
  name: string
  description: string | null
  category: CategorySummary | null
  vatRate: number
  allowsEmployeeDiscount: boolean
  tracksStock: boolean
  active: boolean
  variants: Variant[]
}

/** Variante de otro producto (o de este) que ya usa un código de barras. */
export type BarcodeMatch = {
  productId: number
  productName: string
  variantId: number
  variantLabel: string
  sku: string
  active: boolean
}
