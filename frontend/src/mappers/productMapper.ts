import type { ProductDraft, VariantDraft } from '../models/drafts'
import type { Product } from '../models/product'
import { blankToNull } from '../utils/text'

/** Convierte entre el producto de la API y el draft del formulario. */

/** Variante en blanco para el alta. Si es la única, el distintivo queda en “Única”. */
export function emptyVariant(): VariantDraft {
  return {
    brandId: '',
    label: 'Única',
    barcode: '',
    price: '',
    itemCondition: 'NUEVA',
    imageUrl: '',
  }
}

/** Formulario vacío de un producto nuevo, con una variante. */
export function emptyDraft(): ProductDraft {
  return {
    name: '',
    description: '',
    categoryId: '',
    allowsEmployeeDiscount: true,
    tracksStock: true,
    variants: [emptyVariant()],
  }
}

/** Pasa un producto de la API al estado del formulario de edición. */
export function productToDraft(product: Product): ProductDraft {
  return {
    name: product.name,
    description: product.description ?? '',
    categoryId: product.category ? String(product.category.id) : '',
    allowsEmployeeDiscount: product.allowsEmployeeDiscount,
    tracksStock: product.tracksStock,
    variants: product.variants.map((variant) => ({
      id: variant.id,
      brandId: variant.brand ? String(variant.brand.id) : '',
      label: variant.label,
      barcode: variant.barcode ?? '',
      price: String(variant.price),
      itemCondition: variant.itemCondition,
      imageUrl: variant.imageUrl ?? '',
    })),
  }
}

/** Arma el JSON de una variante para POST/PUT. Si hay una sola, el label vacío se vuelve “Única”. */
export function toVariantPayload(draft: ProductDraft, variant: VariantDraft, index: number) {
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
}

/** Acepta coma o punto y lo deja con dos decimales. */
function parsePrice(value: string) {
  const normalized = value.trim().replace(',', '.')
  const price = Number(normalized)
  if (!Number.isFinite(price) || price < 0) {
    throw new Error('El precio tiene que ser un número mayor o igual a cero')
  }
  return price.toFixed(2)
}

/** Texto para el aviso: “Cuaderno A4” (Rivadavia) o “este producto”. */
export function describeBarcodeOwner(
  match: { productId: number; productName: string; variantLabel: string; active: boolean },
  currentProductId: number | undefined,
) {
  const variant = match.variantLabel === 'Única' ? '' : `, ${match.variantLabel}`
  const baja = match.active ? '' : ', dado de baja'
  if (currentProductId != null && match.productId === currentProductId) {
    return `este producto${variant}${baja}`
  }
  return `“${match.productName}”${variant}${baja}`
}
