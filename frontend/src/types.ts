export type BrandSummary = {
  id: number
  name: string
}

export type Brand = BrandSummary & {
  active: boolean
}

export type CategorySummary = {
  id: number
  name: string
}

export type Category = CategorySummary & {
  parentId: number | null
  active: boolean
}

export type Variant = {
  id: number
  sku: string
  label: string
  barcode: string | null
  price: number
  itemCondition: 'NUEVA' | 'DEFECTUOSA'
  imageUrl: string | null
  active: boolean
  brand: BrandSummary | null
}

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
