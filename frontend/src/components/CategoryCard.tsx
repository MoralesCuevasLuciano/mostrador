import { useEffect, useState } from 'react'
import type { Category } from '../models/category'
import { updateCategory } from '../services/categoryService'

type CategoryCardProps = {
  category: Category
  parentName: string | null
  /** Rubros activos que pueden ser padre (sin el propio id). */
  parentOptions: Category[]
  /** Un rubro con hijas no puede pasar a ser subcategoría. */
  canChangeParent: boolean
  busy: boolean
  onUpdated: () => void
  onDeactivate: () => void
  onReactivate: () => void
}

/** Tarjeta de un rubro o subcategoría: nombre, padre, edición, baja y reactivar. */
export function CategoryCard({
  category,
  parentName,
  parentOptions,
  canChangeParent,
  busy,
  onUpdated,
  onDeactivate,
  onReactivate,
}: CategoryCardProps) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(category.name)
  const [parentId, setParentId] = useState(category.parentId == null ? '' : String(category.parentId))
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setName(category.name)
    setParentId(category.parentId == null ? '' : String(category.parentId))
    setEditing(false)
    setError(null)
  }, [category])

  /** Guarda nombre y padre, y recarga el listado. */
  async function save() {
    if (name.trim() === '') {
      return
    }
    setError(null)
    setSaving(true)
    try {
      await updateCategory(category.id, name, parentId)
      setEditing(false)
      onUpdated()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo guardar la categoría')
    } finally {
      setSaving(false)
    }
  }

  const locked = busy || saving
  const child = category.parentId != null

  return (
    <article className={category.active ? 'card' : 'card card-inactive'}>
      <header className="card-header">
        {editing ? (
          <input
            className="inline-edit"
            maxLength={100}
            value={name}
            disabled={locked}
            onChange={(event) => setName(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault()
                void save()
              }
              if (event.key === 'Escape') {
                setName(category.name)
                setParentId(category.parentId == null ? '' : String(category.parentId))
                setEditing(false)
              }
            }}
          />
        ) : (
          <h2>{child ? `— ${category.name}` : category.name}</h2>
        )}
        {!category.active && <span className="badge">Dada de baja</span>}
        <div className="card-actions">
          {editing ? (
            <>
              <button type="button" className="small" disabled={locked || name.trim() === ''} onClick={() => void save()}>
                {saving ? 'Guardando…' : 'Guardar'}
              </button>
              <button
                type="button"
                className="small secondary"
                disabled={locked}
                onClick={() => {
                  setName(category.name)
                  setParentId(category.parentId == null ? '' : String(category.parentId))
                  setEditing(false)
                  setError(null)
                }}
              >
                Cancelar
              </button>
            </>
          ) : (
            <>
              {category.active && (
                <button type="button" className="small" disabled={locked} onClick={() => setEditing(true)}>
                  Editar
                </button>
              )}
              {category.active ? (
                <button type="button" className="small danger" disabled={locked} onClick={onDeactivate}>
                  {busy ? 'Dando de baja…' : 'Dar de baja'}
                </button>
              ) : (
                <button type="button" className="small" disabled={locked} onClick={onReactivate}>
                  {busy ? 'Reactivando…' : 'Reactivar'}
                </button>
              )}
            </>
          )}
        </div>
      </header>
      {editing ? (
        canChangeParent && (
          <label className="meta-field">
            Tipo
            <select value={parentId} disabled={locked} onChange={(event) => setParentId(event.target.value)}>
              <option value="">Rubro (sin padre)</option>
              {parentOptions.map((root) => (
                <option key={root.id} value={root.id}>
                  Hija de {root.name}
                </option>
              ))}
            </select>
          </label>
        )
      ) : (
        <p className="meta">{child ? `Subcategoría de ${parentName ?? 'un rubro'}` : 'Rubro'}</p>
      )}
      {error && <p className="error">{error}</p>}
    </article>
  )
}
