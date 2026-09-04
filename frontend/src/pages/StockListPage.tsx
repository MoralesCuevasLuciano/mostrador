import { useEffect, useState } from 'react'
import { StockCard } from '../components/StockCard'
import type { Branch } from '../models/branch'
import type { StockItem } from '../models/stock'
import { fetchProducts } from '../services/productService'
import { fetchStockByBranch } from '../services/stockService'
import { indexCatalogByVariant, toCountedItems, toPendingItems } from '../utils/stock'

type ListMode = 'counted' | 'pending'

type StockListPageProps = {
  branch: Branch | null
  otherBranches: Branch[]
}

/** Inventario del local activo: lo ya contado y lo que falta contar. */
export function StockListPage({ branch, otherBranches }: StockListPageProps) {
  const [listMode, setListMode] = useState<ListMode>('pending')
  const [counted, setCounted] = useState<StockItem[] | null>(null)
  const [pending, setPending] = useState<StockItem[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  /** Carga catálogo + saldos y arma las dos listas. */
  function loadStock(branchId: number) {
    setError(null)
    return Promise.all([fetchProducts(), fetchStockByBranch(branchId)])
      .then(([products, balances]) => {
        const catalog = indexCatalogByVariant(products)
        const inventoriedIds = new Set(balances.map((balance) => balance.variantId))
        setCounted(toCountedItems(balances, catalog))
        setPending(toPendingItems(products, inventoriedIds))
      })
      .catch(() => {
        setCounted(null)
        setPending(null)
        setError('No se pudo cargar el inventario. ¿Está el backend en el puerto 8080?')
      })
  }

  useEffect(() => {
    if (branch == null) {
      setCounted(null)
      setPending(null)
      return
    }
    setCounted(null)
    setPending(null)
    void loadStock(branch.id)
  }, [branch?.id])

  if (branch == null) {
    return (
      <>
        <div className="page-header">
          <h1>Inventario</h1>
        </div>
        <p>Elegí una sucursal en la barra de arriba.</p>
      </>
    )
  }

  const visible = listMode === 'counted' ? counted : pending
  const pendingCount = pending?.length ?? 0

  return (
    <>
      <div className="page-header">
        <h1>{listMode === 'counted' ? `Inventario · ${branch.name}` : `Sin contar · ${branch.name}`}</h1>
        <div className="page-actions">
          {listMode === 'pending' ? (
            <button type="button" className="secondary" onClick={() => setListMode('counted')}>
              Ver en este local
            </button>
          ) : (
            <button type="button" className="secondary" onClick={() => setListMode('pending')}>
              {pendingCount > 0 ? `Ver sin contar (${pendingCount})` : 'Ver sin contar'}
            </button>
          )}
        </div>
      </div>
      {error && <p className="error">{error}</p>}
      {!error && visible === null && <p>Cargando…</p>}
      {visible && visible.length === 0 && (
        <p>
          {listMode === 'counted'
            ? 'Todavía no se contó nada en este local.'
            : 'No hay variantes pendientes de contar.'}
        </p>
      )}
      {visible && visible.length > 0 && (
        <section className="list">
          {visible.map((item) => (
            <StockCard
              key={item.variantId}
              item={item}
              branchId={branch.id}
              otherBranches={otherBranches}
              busy={busyId === item.variantId}
              onBusy={setBusyId}
              onChanged={() => void loadStock(branch.id)}
            />
          ))}
        </section>
      )}
    </>
  )
}
