import type { ItemCondition } from './variant'

/** Estado editable de una variante en el formulario (strings porque salen de inputs). */
export type VariantDraft = {
  id?: number
  brandId: string
  label: string
  barcode: string
  price: string
  itemCondition: ItemCondition
  imageUrl: string
}

/** Estado editable de la ficha + variantes mientras se carga o edita un producto. */
export type ProductDraft = {
  name: string
  description: string
  categoryId: string
  allowsEmployeeDiscount: boolean
  tracksStock: boolean
  variants: VariantDraft[]
}
