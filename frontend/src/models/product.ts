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
  active: boolean
  variants: Variant[]
}
