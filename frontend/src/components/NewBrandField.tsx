import { useState } from 'react'
import type { Brand } from '../models/brand'
import { createBrand } from '../services/brandService'

type NewBrandFieldProps = {
  onCreated: (brand: Brand) => void
}

/** Alta de marca sin salir del formulario de producto. */
export function NewBrandField({ onCreated }: NewBrandFieldProps) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  /** POST de la marca y avisa al padre para seleccionarla. */
  async function save() {
    setError(null)
    setSaving(true)
    try {
      const brand = await createBrand(name)
      setName('')
      setOpen(false)
      onCreated(brand)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear la marca')
    } finally {
      setSaving(false)
    }
  }

  if (!open) {
    return (
      <button type="button" className="link-add" onClick={() => setOpen(true)}>
        Nueva marca
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
