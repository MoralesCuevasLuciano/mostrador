import type { Product } from '../models/product'
import type { Variant } from '../models/variant'
import { formatPrice } from '../utils/money'
import { VariantThumb } from './VariantThumb'

/** Texto de una variante en el listado: marca, distintivo, precio, SKU. */
function variantSummary(variant: Variant) {
  const parts = [
    variant.brand?.name,
    variant.label === 'Única' ? null : variant.label,
    formatPrice(variant.price),
    `SKU: ${variant.sku}`,
    variant.barcode ? `Código de barras: ${variant.barcode}` : null,
    variant.itemCondition === 'DEFECTUOSA' ? 'Defectuosa' : null,
  ].filter(Boolean)
  return parts.join(' · ')
}

type ProductCardProps = {
  product: Product
  busy: boolean
  onEdit: () => void
  onDeactivate: () => void
  onReactivate: () => void
}

/** Tarjeta de un producto en el listado: ficha, variantes, editar y baja/reactivar. */
export function ProductCard({
  product,
  busy,
  onEdit,
  onDeactivate,
  onReactivate,
}: ProductCardProps) {
  return (
    <article className={product.active ? 'card' : 'card card-inactive'}>
      <header className="card-header">
        <h2>{product.name}</h2>
        {!product.active && <span className="badge">Dado de baja</span>}
        <div className="card-actions">
          {product.active && (
            <button type="button" className="small" disabled={busy} onClick={onEdit}>
              Editar
            </button>
          )}
          {product.active ? (
            <button type="button" className="small danger" disabled={busy} onClick={onDeactivate}>
              {busy ? 'Dando de baja…' : 'Dar de baja'}
            </button>
          ) : (
            <button type="button" className="small" disabled={busy} onClick={onReactivate}>
              {busy ? 'Reactivando…' : 'Reactivar'}
            </button>
          )}
        </div>
      </header>
      <p className="meta">
        {[
          product.category?.name ?? 'Sin categoría',
          `IVA ${product.vatRate}%`,
          product.allowsEmployeeDiscount ? null : 'Sin desc. empleado',
          product.tracksStock ? null : 'Sin inventario',
        ]
          .filter(Boolean)
          .join(' · ')}
      </p>
      {product.description && <p className="description">{product.description}</p>}
      <ul className="variants">
        {product.variants.map((variant) => (
          <li key={variant.id} className={variant.active ? undefined : 'inactive'}>
            <VariantThumb imageUrl={variant.imageUrl} />
            <span>
              {variantSummary(variant)}
              {!variant.active && ' · Baja'}
            </span>
          </li>
        ))}
      </ul>
    </article>
  )
}
