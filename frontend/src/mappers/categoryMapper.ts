import type { Category } from '../models/category'

/**
 * Opciones del select de categoría: rubros y subcategorías indentadas.
 * Incluye la seleccionada aunque esté dada de baja, para no perderla al editar.
 */
export function categoryOptions(categories: Category[], selectedId: string) {
  const selected = selectedId === '' ? null : Number(selectedId)
  const visible = categories.filter((category) => category.active || category.id === selected)
  const roots = visible.filter((category) => category.parentId == null)
  const childrenOf = (id: number) => visible.filter((category) => category.parentId === id)
  return roots.flatMap((root) => [
    { id: root.id, label: root.name },
    ...childrenOf(root.id).map((child) => ({ id: child.id, label: `— ${child.name}` })),
  ])
}

/** Rubros y debajo sus hijas. Si el padre no está en la lista (p. ej. dados de baja), la hija se muestra igual. */
export function listCategoriesTree(categories: Category[]) {
  const ids = new Set(categories.map((category) => category.id))
  const roots = categories.filter((category) => category.parentId == null || !ids.has(category.parentId))
  const childrenOf = (id: number) => categories.filter((category) => category.parentId === id)
  return roots.flatMap((root) => [root, ...childrenOf(root.id)])
}

/** Nombre del rubro padre, o null si es raíz. */
export function categoryParentName(category: Category, categories: Category[]) {
  if (category.parentId == null) {
    return null
  }
  return categories.find((candidate) => candidate.id === category.parentId)?.name ?? null
}
