import type { Variant } from '../models/variant'

type InactiveVariantsModalProps = {
  variants: Variant[]
  reactivatingId: number | null
  onReactivate: (variant: Variant) => void
  onClose: () => void
}

/** Popup con las variantes dadas de baja, para reactivarlas una por una. */
export function InactiveVariantsModal({
  variants,
  reactivatingId,
  onReactivate,
  onClose,
}: InactiveVariantsModalProps) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal modal-list"
        role="dialog"
        aria-modal="true"
        aria-labelledby="inactive-variants-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="inactive-variants-title">Variantes dadas de baja</h2>
        {variants.length === 0 ? (
          <p>No quedan variantes dadas de baja.</p>
        ) : (
          <ul className="inactive-variants">
            {variants.map((variant) => (
              <li key={variant.id}>
                <span>
                  {[variant.label, `SKU: ${variant.sku}`, variant.brand?.name]
                    .filter(Boolean)
                    .join(' · ')}
                </span>
                <button
                  type="button"
                  className="small"
                  disabled={reactivatingId === variant.id}
                  onClick={() => onReactivate(variant)}
                >
                  {reactivatingId === variant.id ? 'Reactivando…' : 'Reactivar'}
                </button>
              </li>
            ))}
          </ul>
        )}
        <div className="modal-actions">
          <button type="button" className="secondary" onClick={onClose}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  )
}
