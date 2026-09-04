import { useState } from 'react'
import { Nav } from './components/Nav'
import { useBranch } from './hooks/useBranch'
import type { Product } from './models/product'
import { BrandListPage } from './pages/BrandListPage'
import { CategoryListPage } from './pages/CategoryListPage'
import { ProductFormPage } from './pages/ProductFormPage'
import { ProductListPage } from './pages/ProductListPage'
import { StockListPage } from './pages/StockListPage'

/** Pantalla actual: catálogo, formulario, rubros/marcas o inventario. */
type Screen = 'list' | 'form' | 'categories' | 'brands' | 'stock'

/** Shell de la app: barra de navegación, sucursal activa y la pantalla. */
export default function App() {
  const [screen, setScreen] = useState<Screen>('list')
  const [editing, setEditing] = useState<Product | null>(null)
  const { activeBranches, selected, selectBranch, error, loading } = useBranch()

  /** Abre el formulario vacío para un producto nuevo. */
  function openCreate() {
    setEditing(null)
    setScreen('form')
  }

  /** Abre el formulario precargado con el producto elegido. */
  function openEdit(product: Product) {
    setEditing(product)
    setScreen('form')
  }

  /** Vuelve al listado de productos. */
  function goToList() {
    setEditing(null)
    setScreen('list')
  }

  const otherBranches = activeBranches.filter((branch) => branch.id !== selected?.id)

  return (
    <>
      <Nav
        listActive={screen === 'list' || screen === 'categories' || screen === 'brands'}
        createActive={screen === 'form' && !editing}
        inventoryActive={screen === 'stock'}
        onList={goToList}
        onCreate={openCreate}
        onInventory={() => {
          setEditing(null)
          setScreen('stock')
        }}
        branches={activeBranches}
        selectedBranchId={selected?.id ?? null}
        onSelectBranch={selectBranch}
        branchesLoading={loading}
      />
      <main>
        {error && <p className="error">{error}</p>}
        {screen === 'form' ? (
          <ProductFormPage product={editing} onSaved={goToList} />
        ) : screen === 'categories' ? (
          <CategoryListPage onBack={goToList} />
        ) : screen === 'brands' ? (
          <BrandListPage onBack={goToList} />
        ) : screen === 'stock' ? (
          <StockListPage branch={selected} otherBranches={otherBranches} />
        ) : (
          <ProductListPage
            onEdit={openEdit}
            onManageCategories={() => setScreen('categories')}
            onManageBrands={() => setScreen('brands')}
          />
        )}
      </main>
    </>
  )
}
