/** Rubro reducido, como viene embebido en un producto. */
export type CategorySummary = {
  id: number
  name: string
}

/** Categoría completa. parentId null = rubro raíz. */
export type Category = CategorySummary & {
  parentId: number | null
  active: boolean
}
