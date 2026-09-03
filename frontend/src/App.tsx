import { useEffect, useState } from 'react'
import { fetchProducts } from './api'
import { ProductForm } from './ProductForm'
import type { Product, Variant } from './types'

function formatPrice(price: number) {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
  }).format(price)
}

function variantSummary(variant: Variant) {
  const parts = [
    variant.brand?.name,
    variant.label === 'Única' ? null : variant.label,
    formatPrice(variant.price),
    variant.sku,
    variant.barcode,
    variant.itemCondition === 'DEFECTUOSA' ? 'Defectuosa' : null,
  ].filter(Boolean)
  return parts.join(' · ')
}

function ProductCard({ product }: { product: Product }) {
  const showVariantList = product.variants.length > 1
    || product.variants.some((variant) => variant.label !== 'Única')

  return (
    <article className={product.active ? 'card' : 'card card-inactive'}>
      <header className="card-header">
        <h2>{product.name}</h2>
        {!product.active && <span className="badge">Dado de baja</span>}
      </header>
      <p className="meta">
        {[
          product.category?.name ?? 'Sin categoría',
          `IVA ${product.vatRate}%`,
          product.allowsEmployeeDiscount ? null : 'Sin desc. empleado',
        ]
          .filter(Boolean)
          .join(' · ')}
      </p>
      {product.description && <p className="description">{product.description}</p>}
      {showVariantList ? (
        <ul className="variants">
          {product.variants.map((variant) => (
            <li key={variant.id} className={variant.active ? undefined : 'inactive'}>
              {variant.imageUrl && (
                <img src={variant.imageUrl} alt="" className="thumb" />
              )}
              <span>
                {variantSummary(variant)}
                {!variant.active && ' · Baja'}
              </span>
            </li>
          ))}
        </ul>
      ) : (
        product.variants[0] && (
          <p className="meta">
            {variantSummary(product.variants[0])}
            {!product.variants[0].active && ' · Baja'}
          </p>
        )
      )}
    </article>
  )
}

type Screen = 'list' | 'create'

function App() {
  const [screen, setScreen] = useState<Screen>('list')
  const [products, setProducts] = useState<Product[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  function loadProducts() {
    setError(null)
    fetchProducts()
      .then(setProducts)
      .catch(() => setError('No se pudo cargar el catálogo. ¿Está el backend en el puerto 8080?'))
  }

  useEffect(() => {
    if (screen === 'list') {
      loadProducts()
    }
  }, [screen])

  function handleCreated() {
    setScreen('list')
  }

  return (
    <>
      <nav className="nav">
        <p className="nav-brand">Mostrador</p>
        <button
          type="button"
          className={screen === 'list' ? 'nav-link active' : 'nav-link'}
          onClick={() => setScreen('list')}
        >
          Productos
        </button>
        <button
          type="button"
          className={screen === 'create' ? 'nav-link active' : 'nav-link'}
          onClick={() => setScreen('create')}
        >
          Cargar producto
        </button>
      </nav>
      <main>
        {screen === 'create' ? (
          <>
            <h1>Cargar producto</h1>
            <ProductForm onCreated={handleCreated} />
          </>
        ) : (
          <>
            <h1>Productos</h1>
            {error && <p className="error">{error}</p>}
            {!error && products === null && <p>Cargando…</p>}
            {products && products.length === 0 && (
              <p>No hay productos cargados.</p>
            )}
            {products && products.length > 0 && (
              <section className="list">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </section>
            )}
          </>
        )}
      </main>
    </>
  )
}

export default App
