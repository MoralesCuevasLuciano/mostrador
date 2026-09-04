import { useState } from 'react'
import type { Category } from '../models/category'
import { createCategory } from '../services/categoryService'

type NewCategoryFieldProps = {
  categories: Category[]
  onCreated: (category: Category) => void
}

/** Alta de rubro o subcategoría sin salir del formulario de producto. */
export function NewCategoryField({ categories, onCreated }: NewCategoryFieldProps) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const roots = categories.filter((category) => category.active && category.parentId == null)

  /** POST de la categoría y la deja seleccionada en el formulario. */
  async function save() {
    setError(null)
    setSaving(true)
    try {
      const category = await createCategory(name, parentId)
      setName('')
      setParentId('')
      setOpen(false)
      onCreated(category)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear la categoría')
    } finally {
      setSaving(false)
    }
  }

  if (!open) {
    return (
      <button type="button" className="link-add" onClick={() => setOpen(true)}>
        Nueva categoría
      </button>
    )
  }

  return (
    <div className="inline-create">
      <input
        maxLength={100}
        placeholder="Nombre"
        value={name}
        onChange={(event) => setName(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault()
            void save()
          }
        }}
      />
      <select value={parentId} onChange={(event) => setParentId(event.target.value)}>
        <option value="">Rubro (sin padre)</option>
        {roots.map((root) => (
          <option key={root.id} value={root.id}>
            Hija de {root.name}
          </option>
        ))}
      </select>
      <button type="button" disabled={saving || name.trim() === ''} onClick={() => void save()}>
        Crear
      </button>
      <button type="button" className="linkish" onClick={() => setOpen(false)}>
        Cancelar
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  )
}
