/** Marca reducida (id + nombre), como viene embebida en una variante. */
export type BrandSummary = {
  id: number
  name: string
}

/** Marca completa, como sale de GET /api/brands. */
export type Brand = BrandSummary & {
  active: boolean
}
