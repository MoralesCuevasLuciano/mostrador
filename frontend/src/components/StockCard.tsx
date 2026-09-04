import { useState } from 'react'
import type { Branch } from '../models/branch'
import type { StockItem, StockMovement } from '../models/stock'
import {
  fetchStockMovements,
  recountStock,
  registerInternalConsumption,
  registerStockEntry,
  registerStockLoss,
  transferStock,
} from '../services/stockService'
import {
  formatMovementAt,
  formatSignedQuantity,
  movementLabel,
  stockItemTitle,
} from '../utils/stock'

type ActionKind = 'recount' | 'entry' | 'consume' | 'loss' | 'transfer'

type StockCardProps = {
  item: StockItem
  branchId: number
  otherBranches: Branch[]
  busy: boolean
  onBusy: (variantId: number | null) => void
  onChanged: () => void
}

/** Tarjeta de una variante en inventario: saldo, movimientos e historial. */
export function StockCard({
  item,
  branchId,
  otherBranches,
  busy,
  onBusy,
  onChanged,
}: StockCardProps) {
  const [action, setAction] = useState<ActionKind | null>(item.inventoried ? null : 'recount')
  const [quantity, setQuantity] = useState(item.inventoried && item.quantity != null ? String(item.quantity) : '')
  const [description, setDescription] = useState('')
  const [toBranchId, setToBranchId] = useState(otherBranches[0] ? String(otherBranches[0].id) : '')
  const [error, setError] = useState<string | null>(null)
  const [history, setHistory] = useState<StockMovement[] | null>(null)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [historyError, setHistoryError] = useState<string | null>(null)

  /** Abre un formulario de movimiento y resetea los campos. */
  function openAction(next: ActionKind) {
    setError(null)
    setAction((current) => (current === next ? null : next))
    setQuantity(next === 'recount' && item.quantity != null ? String(item.quantity) : '')
    setDescription('')
    if (otherBranches[0]) {
      setToBranchId(String(otherBranches[0].id))
    }
  }

  /** Carga o oculta el historial de esta variante. */
  async function toggleHistory() {
    if (historyOpen) {
      setHistoryOpen(false)
      return
    }
    setHistoryError(null)
    setHistoryOpen(true)
    if (history != null) {
      return
    }
    try {
      setHistory(await fetchStockMovements(branchId, item.variantId))
    } catch (err) {
      setHistoryError(err instanceof Error ? err.message : 'No se pudo cargar el historial')
    }
  }

  /** Envía el movimiento del formulario abierto. */
  async function submitAction() {
    const parsed = Number(quantity)
    if (!Number.isInteger(parsed) || (action === 'recount' ? parsed < 0 : parsed < 1)) {
      setError(action === 'recount' ? 'Indicá cuántas hay (0 o más).' : 'Indicá una cantidad mayor a cero.')
      return
    }
    if (action === 'transfer' && toBranchId === '') {
      setError('Elegí la sucursal de destino.')
      return
    }
    setError(null)
    onBusy(item.variantId)
    try {
      if (action === 'recount') {
        await recountStock(branchId, item.variantId, parsed, description)
      } else if (action === 'entry') {
        await registerStockEntry(branchId, item.variantId, parsed, description)
      } else if (action === 'consume') {
        await registerInternalConsumption(branchId, item.variantId, parsed, description)
      } else if (action === 'loss') {
        await registerStockLoss(branchId, item.variantId, parsed, description)
      } else if (action === 'transfer') {
        await transferStock(item.variantId, branchId, Number(toBranchId), parsed, description)
      }
      setAction(null)
      setDescription('')
      setHistory(null)
      setHistoryOpen(false)
      onChanged()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo guardar el movimiento')
    } finally {
      onBusy(null)
    }
  }

  const title = stockItemTitle(item.productName, item.variantLabel)
  const qtyClass =
    item.quantity != null && item.quantity < 0 ? 'qty qty-neg' : 'qty'

  return (
    <article className="card">
      <header className="card-header">
        <h2>{title}</h2>
        {!item.inventoried && <span className="badge">Sin contar</span>}
        {!item.variantActive && <span className="badge">Variante dada de baja</span>}
        {item.inventoried && item.quantity != null && (
          <span className={`card-actions ${qtyClass}`}>{item.quantity}</span>
        )}
      </header>
      <p className="meta">SKU: {item.sku}</p>

      {item.inventoried && (
        <div className="stock-actions">
          <button type="button" className="small" disabled={busy} onClick={() => openAction('recount')}>
            Recuento
          </button>
          <button type="button" className="small" disabled={busy} onClick={() => openAction('entry')}>
            Entrada
          </button>
          <button type="button" className="small" disabled={busy} onClick={() => openAction('consume')}>
            Consumo
          </button>
          <button type="button" className="small" disabled={busy} onClick={() => openAction('loss')}>
            Extravío
          </button>
          {otherBranches.length > 0 && (
            <button type="button" className="small" disabled={busy} onClick={() => openAction('transfer')}>
              Traslado
            </button>
          )}
          <button type="button" className="small secondary" disabled={busy} onClick={() => void toggleHistory()}>
            {historyOpen ? 'Ocultar historial' : 'Historial'}
          </button>
        </div>
      )}

      {action && (
        <form
          className="form stock-form"
          onSubmit={(event) => {
            event.preventDefault()
            void submitAction()
          }}
        >
          <label>
            {action === 'recount' ? 'Cantidad contada' : 'Unidades'}
            <input
              type="number"
              min={action === 'recount' ? 0 : 1}
              step={1}
              required
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
            />
          </label>
          {action === 'transfer' && (
            <label>
              Destino
              <select value={toBranchId} onChange={(event) => setToBranchId(event.target.value)}>
                {otherBranches.map((branch) => (
                  <option key={branch.id} value={branch.id}>
                    {branch.name}
                  </option>
                ))}
              </select>
            </label>
          )}
          <label>
            Nota (opcional)
            <input
              maxLength={255}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
          </label>
          {error && <p className="error">{error}</p>}
          <div className="form-actions">
            {item.inventoried && (
              <button type="button" className="secondary" onClick={() => setAction(null)}>
                Cancelar
              </button>
            )}
            <button type="submit" disabled={busy}>
              {busy ? 'Guardando…' : actionLabel(action)}
            </button>
          </div>
        </form>
      )}

      {historyOpen && (
        <>
          {historyError && <p className="error">{historyError}</p>}
          {history === null && !historyError && <p className="meta">Cargando historial…</p>}
          {history && history.length === 0 && <p className="meta">Todavía no hay movimientos.</p>}
          {history && history.length > 0 && (
            <ul className="movements">
              {history.map((movement) => (
                <li key={movement.id}>
                  <span>
                    {movementLabel(movement.movementType)}
                    {movement.description ? ` · ${movement.description}` : ''}
                    <span className="hint"> · {formatMovementAt(movement.movementAt)}</span>
                  </span>
                  <span className={movement.quantity < 0 ? 'qty qty-neg' : 'qty'}>
                    {formatSignedQuantity(movement.quantity)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </article>
  )
}

/** Texto del botón de guardar según el movimiento. */
function actionLabel(action: ActionKind) {
  if (action === 'recount') {
    return 'Guardar recuento'
  }
  if (action === 'entry') {
    return 'Registrar entrada'
  }
  if (action === 'consume') {
    return 'Registrar consumo'
  }
  if (action === 'loss') {
    return 'Registrar extravío'
  }
  return 'Trasladar'
}
