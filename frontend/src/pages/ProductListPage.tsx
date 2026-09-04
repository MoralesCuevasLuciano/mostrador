import { useEffect, useState } from 'react'
import type { Product } from '../models/product'
import { deactivateProduct, fetchProducts, reactivateProduct } from '../services/productService'
import { DoubleConfirm } from '../components/DoubleConfirm'
import { ProductCard } from '../components/ProductCard'

type ListMode = 'active' | 'inactive'

type ProductListPageProps = {
  onEdit: (product: Product) => void
  onManageCategories: () => void
  onManageBrands: () => void
}

/** Listado de productos activos o dados de baja, con doble confirmación. */
export function ProductListPage({ onEdit, onManageCategories, onManageBrands }: ProductListPageProps) {
  const [listMode, setListMode] = useState<ListMode>('active')
  const [products, setProducts] = useState<Product[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [pending, setPending] = useState<{ product: Product; action: 'deactivate' | 'reactivate' } | null>(null)

  /** Recarga el catálogo desde el backend. */
  function loadProducts() {
    setError(null)
    return fetchProducts()
      .then(setProducts)
      .catch(() => setError('No se pudo cargar el catálogo. ¿Está el backend en el puerto 8080?'))
  }

  useEffect(() => {
    loadProducts()
  }, [])

  /** Ejecuta la baja o reactivación confirmada y refresca el listado. */
  async function runPendingAction() {
    if (!pending) {
      return
    }
    const { product, action } = pending
    setPending(null)
    setBusyId(product.id)
    setError(null)
    try {
      if (action === 'deactivate') {
        await deactivateProduct(product.id)
      } else {
        await reactivateProduct(product)
      }
      await loadProducts()
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : action === 'deactivate'
            ? 'No se pudo dar de baja el producto'
            : 'No se pudo reactivar el producto',
      )
    } finally {
      setBusyId(null)
    }
  }

  const visibleProducts = products === null
    ? null
    : products.filter((product) => product.active === (listMode === 'active'))

  return (
    <>
      <div className="page-header">
        <h1>{listMode === 'active' ? 'Productos' : 'Productos dados de baja'}</h1>
        <div className="page-actions">
          <button type="button" className="secondary" onClick={onManageCategories}>
            Categorías
          </button>
          <button type="button" className="secondary" onClick={onManageBrands}>
            Marcas
          </button>
          {listMode === 'active' ? (
            <button type="button" className="secondary" onClick={() => setListMode('inactive')}>
              Ver dados de baja
            </button>
          ) : (
            <button type="button" className="secondary" onClick={() => setListMode('active')}>
              Volver al listado
            </button>
          )}
        </div>
      </div>
      {error && <p className="error">{error}</p>}
      {!error && visibleProducts === null && <p>Cargando…</p>}
      {visibleProducts && visibleProducts.length === 0 && (
        <p>
          {listMode === 'active'
            ? 'No hay productos cargados.'
            : 'No hay productos dados de baja.'}
        </p>
      )}
      {visibleProducts && visibleProducts.length > 0 && (
        <section className="list">
          {visibleProducts.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              busy={busyId === product.id}
              onEdit={() => onEdit(product)}
              onDeactivate={() => setPending({ product, action: 'deactivate' })}
              onReactivate={() => setPending({ product, action: 'reactivate' })}
            />
          ))}
        </section>
      )}
      {pending && (
        <DoubleConfirm
          key={`${pending.action}-${pending.product.id}`}
          title={pending.action === 'deactivate' ? 'Dar de baja' : 'Reactivar'}
          firstMessage={
            pending.action === 'deactivate'
              ? `Si das de baja “${pending.product.name}”, no se va a poder vender. ¿Estás seguro?`
              : `¿Estás seguro de reactivar “${pending.product.name}”?`
          }
          secondMessage={
            pending.action === 'deactivate'
              ? '¿Realmente estás seguro? Un producto dado de baja no se puede vender hasta que lo reactives.'
              : '¿Realmente estás seguro?'
          }
          confirmDanger={pending.action === 'deactivate'}
          onConfirm={() => void runPendingAction()}
          onCancel={() => setPending(null)}
        />
      )}
    </>
  )
}
