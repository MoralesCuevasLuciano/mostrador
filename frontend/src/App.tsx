import { useState } from 'react'
import { Nav } from './components/Nav'
import type { Product } from './models/product'
import { BrandListPage } from './pages/BrandListPage'
import { CategoryListPage } from './pages/CategoryListPage'
import { ProductFormPage } from './pages/ProductFormPage'
import { ProductListPage } from './pages/ProductListPage'

/** Pantalla actual: catálogo, formulario, o gestión de rubros/marcas. */
type Screen = 'list' | 'form' | 'categories' | 'brands'

/** Shell de la app: barra de navegación y la pantalla activa. */
export default function App() {
  const [screen, setScreen] = useState<Screen>('list')
  const [editing, setEditing] = useState<Product | null>(null)

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

  return (
    <>
      <Nav
        listActive={screen === 'list' || screen === 'categories' || screen === 'brands'}
        createActive={screen === 'form' && !editing}
        onList={goToList}
        onCreate={openCreate}
      />
      <main>
        {screen === 'form' ? (
          <ProductFormPage product={editing} onSaved={goToList} />
        ) : screen === 'categories' ? (
          <CategoryListPage onBack={goToList} />
        ) : screen === 'brands' ? (
          <BrandListPage onBack={goToList} />
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
