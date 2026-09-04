import type { Product } from '../models/product'
import type { StockBalance, StockItem, StockMovementType } from '../models/stock'
import type { Variant } from '../models/variant'

const MOVEMENT_LABELS: Record<StockMovementType, string> = {
  VENTA: 'Venta',
  ANULACION_VENTA: 'Anulación de venta',
  AJUSTE_INICIAL: 'Ajuste inicial',
  AJUSTE_RECUENTO: 'Recuento',
  CONSUMO_INTERNO: 'Consumo interno',
  TRASLADO: 'Traslado',
  ENTRADA: 'Entrada',
  'EXTRAVÍO': 'Extravío',
}

/** Texto en español para un tipo de movimiento. */
export function movementLabel(type: StockMovementType) {
  return MOVEMENT_LABELS[type] ?? type
}

/** Título de la tarjeta: producto, y el distintivo si no es “Única”. */
export function stockItemTitle(productName: string, variantLabel: string) {
  return variantLabel === 'Única' || variantLabel.trim() === ''
    ? productName
    : `${productName} · ${variantLabel}`
}

/** Fecha del movimiento para el historial. */
export function formatMovementAt(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('es-AR')
}

/** Cantidad del movimiento con signo, para el historial. */
export function formatSignedQuantity(quantity: number) {
  return quantity > 0 ? `+${quantity}` : String(quantity)
}

type CatalogEntry = {
  productName: string
  productActive: boolean
  variant: Variant
}

/** Índice variante → producto, para armar las filas de inventario. */
export function indexCatalogByVariant(products: Product[]) {
  const index = new Map<number, CatalogEntry>()
  for (const product of products) {
    for (const variant of product.variants) {
      index.set(variant.id, { productName: product.name, productActive: product.active, variant })
    }
  }
  return index
}

/** Filas ya contadas. No incluye variantes ni productos dados de baja. */
export function toCountedItems(balances: StockBalance[], catalog: Map<number, CatalogEntry>): StockItem[] {
  return balances
    .flatMap((balance) => {
      const entry = catalog.get(balance.variantId)
      if (entry == null || !entry.productActive || !entry.variant.active) {
        return []
      }
      return [{
        variantId: balance.variantId,
        sku: balance.sku,
        variantLabel: balance.variantLabel,
        productName: entry.productName,
        quantity: balance.quantity,
        inventoried: true,
        variantActive: true,
      }]
    })
    .toSorted(compareStockItems)
}

/**
 * Variantes activas que llevan inventario y todavía no se contaron en este local.
 */
export function toPendingItems(
  products: Product[],
  inventoriedIds: Set<number>,
): StockItem[] {
  const items: StockItem[] = []
  for (const product of products) {
    if (!product.active || !product.tracksStock) {
      continue
    }
    for (const variant of product.variants) {
      if (!variant.active || inventoriedIds.has(variant.id)) {
        continue
      }
      items.push({
        variantId: variant.id,
        sku: variant.sku,
        variantLabel: variant.label,
        productName: product.name,
        quantity: null,
        inventoried: false,
        variantActive: true,
      })
    }
  }
  return items.toSorted(compareStockItems)
}

/** Ordena por nombre de producto y distintivo. */
function compareStockItems(a: StockItem, b: StockItem) {
  const byName = a.productName.localeCompare(b.productName, 'es')
  return byName !== 0 ? byName : a.variantLabel.localeCompare(b.variantLabel, 'es')
}
