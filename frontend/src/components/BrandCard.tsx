import { useEffect, useState } from 'react'
import type { Brand } from '../models/brand'
import { updateBrand } from '../services/brandService'

type BrandCardProps = {
  brand: Brand
  busy: boolean
  onUpdated: () => void
  onDeactivate: () => void
  onReactivate: () => void
}

/** Tarjeta de una marca: nombre, edición inline, baja y reactivar. */
export function BrandCard({ brand, busy, onUpdated, onDeactivate, onReactivate }: BrandCardProps) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(brand.name)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setName(brand.name)
    setEditing(false)
    setError(null)
  }, [brand])

  /** Guarda el nombre editado y recarga el listado. */
  async function save() {
    if (name.trim() === '') {
      return
    }
    setError(null)
    setSaving(true)
    try {
      await updateBrand(brand.id, name)
      setEditing(false)
      onUpdated()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo guardar la marca')
    } finally {
      setSaving(false)
    }
  }

  const locked = busy || saving

  return (
    <article className={brand.active ? 'card' : 'card card-inactive'}>
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
                setName(brand.name)
                setEditing(false)
              }
            }}
          />
        ) : (
          <h2>{brand.name}</h2>
        )}
        {!brand.active && <span className="badge">Dada de baja</span>}
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
                  setName(brand.name)
                  setEditing(false)
                  setError(null)
                }}
              >
                Cancelar
              </button>
            </>
          ) : (
            <>
              {brand.active && (
                <button type="button" className="small" disabled={locked} onClick={() => setEditing(true)}>
                  Editar
                </button>
              )}
              {brand.active ? (
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
      {error && <p className="error">{error}</p>}
    </article>
  )
}
