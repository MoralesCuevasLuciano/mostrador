import { useEffect, useState } from 'react'
import { CategoryCard } from '../components/CategoryCard'
import { DoubleConfirm } from '../components/DoubleConfirm'
import { categoryParentName, listCategoriesTree } from '../mappers/categoryMapper'
import type { Category } from '../models/category'
import {
  createCategory,
  deactivateCategory,
  fetchCategories,
  reactivateCategory,
} from '../services/categoryService'

type ListMode = 'active' | 'inactive'

type CategoryListPageProps = {
  onBack: () => void
}

/** Gestión de rubros y subcategorías: alta, edición, baja y reactivar. */
export function CategoryListPage({ onBack }: CategoryListPageProps) {
  const [listMode, setListMode] = useState<ListMode>('active')
  const [categories, setCategories] = useState<Category[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')
  const [newParentId, setNewParentId] = useState('')
  const [saving, setSaving] = useState(false)
  const [pending, setPending] = useState<{ category: Category; action: 'deactivate' | 'reactivate' } | null>(
    null,
  )

  /** Recarga las categorías desde el backend. */
  function loadCategories() {
    setError(null)
    return fetchCategories()
      .then(setCategories)
      .catch(() => setError('No se pudieron cargar las categorías. ¿Está el backend en el puerto 8080?'))
  }

  useEffect(() => {
    loadCategories()
  }, [])

  /** Alta de rubro o subcategoría desde el panel de la página. */
  async function handleCreate() {
    if (newName.trim() === '') {
      return
    }
    setError(null)
    setSaving(true)
    try {
      await createCategory(newName, newParentId)
      setNewName('')
      setNewParentId('')
      setCreating(false)
      await loadCategories()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear la categoría')
    } finally {
      setSaving(false)
    }
  }

  /** Ejecuta la baja o reactivación confirmada y refresca el listado. */
  async function runPendingAction() {
    if (!pending) {
      return
    }
    const { category, action } = pending
    setPending(null)
    setBusyId(category.id)
    setError(null)
    try {
      if (action === 'deactivate') {
        await deactivateCategory(category.id)
      } else {
        await reactivateCategory(category.id)
      }
      await loadCategories()
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : action === 'deactivate'
            ? 'No se pudo dar de baja la categoría'
            : 'No se pudo reactivar la categoría',
      )
    } finally {
      setBusyId(null)
    }
  }

  const all = categories ?? []
  const visible =
    categories === null
      ? null
      : listCategoriesTree(categories.filter((category) => category.active === (listMode === 'active')))
  const activeRoots = all.filter((category) => category.active && category.parentId == null)
  const hasChildren = (id: number) => all.some((category) => category.parentId === id)

  return (
    <>
      <div className="page-header">
        <h1>{listMode === 'active' ? 'Categorías' : 'Categorías dadas de baja'}</h1>
        <div className="page-actions">
          <button type="button" className="secondary" onClick={onBack}>
            Volver a productos
          </button>
          {listMode === 'active' ? (
            <>
              <button type="button" className="secondary" onClick={() => setListMode('inactive')}>
                Ver dadas de baja
              </button>
              <button type="button" onClick={() => setCreating(true)}>
                Nueva categoría
              </button>
            </>
          ) : (
            <button type="button" className="secondary" onClick={() => setListMode('active')}>
              Volver al listado
            </button>
          )}
        </div>
      </div>
      {creating && listMode === 'active' && (
        <form
          className="card form"
          onSubmit={(event) => {
            event.preventDefault()
            void handleCreate()
          }}
        >
          <label>
            Nombre
            <input
              required
              maxLength={100}
              value={newName}
              onChange={(event) => setNewName(event.target.value)}
            />
          </label>
          <label>
            Tipo
            <select value={newParentId} onChange={(event) => setNewParentId(event.target.value)}>
              <option value="">Rubro (sin padre)</option>
              {activeRoots.map((root) => (
                <option key={root.id} value={root.id}>
                  Hija de {root.name}
                </option>
              ))}
            </select>
          </label>
          <div className="form-actions">
            <button
              type="button"
              className="secondary"
              onClick={() => {
                setCreating(false)
                setNewName('')
                setNewParentId('')
              }}
            >
              Cancelar
            </button>
            <button type="submit" disabled={saving || newName.trim() === ''}>
              {saving ? 'Guardando…' : 'Guardar categoría'}
            </button>
          </div>
        </form>
      )}
      {error && <p className="error">{error}</p>}
      {!error && visible === null && <p>Cargando…</p>}
      {visible && visible.length === 0 && (
        <p>
          {listMode === 'active' ? 'No hay categorías cargadas.' : 'No hay categorías dadas de baja.'}
        </p>
      )}
      {visible && visible.length > 0 && (
        <section className="list">
          {visible.map((category) => (
            <CategoryCard
              key={category.id}
              category={category}
              parentName={categoryParentName(category, all)}
              parentOptions={activeRoots.filter((root) => root.id !== category.id)}
              canChangeParent={category.parentId != null || !hasChildren(category.id)}
              busy={busyId === category.id}
              onUpdated={() => void loadCategories()}
              onDeactivate={() => setPending({ category, action: 'deactivate' })}
              onReactivate={() => setPending({ category, action: 'reactivate' })}
            />
          ))}
        </section>
      )}
      {pending && (
        <DoubleConfirm
          key={`${pending.action}-${pending.category.id}`}
          title={pending.action === 'deactivate' ? 'Dar de baja' : 'Reactivar'}
          firstMessage={
            pending.action === 'deactivate'
              ? `Si das de baja “${pending.category.name}”, no se va a poder usar en productos nuevos. ¿Estás seguro?`
              : `¿Estás seguro de reactivar “${pending.category.name}”?`
          }
          secondMessage={
            pending.action === 'deactivate'
              ? pending.category.parentId == null
                ? '¿Realmente estás seguro? Un rubro con subcategorías activas no se puede dar de baja.'
                : '¿Realmente estás seguro? Una categoría dada de baja no se puede elegir hasta que la reactives.'
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
