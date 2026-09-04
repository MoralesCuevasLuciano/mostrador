import { useEffect, useState } from 'react'
import { BrandCard } from '../components/BrandCard'
import { DoubleConfirm } from '../components/DoubleConfirm'
import type { Brand } from '../models/brand'
import { createBrand, deactivateBrand, fetchBrands, reactivateBrand } from '../services/brandService'

type ListMode = 'active' | 'inactive'

type BrandListPageProps = {
  onBack: () => void
}

/** Gestión de marcas: alta, edición, baja y reactivar. */
export function BrandListPage({ onBack }: BrandListPageProps) {
  const [listMode, setListMode] = useState<ListMode>('active')
  const [brands, setBrands] = useState<Brand[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')
  const [saving, setSaving] = useState(false)
  const [pending, setPending] = useState<{ brand: Brand; action: 'deactivate' | 'reactivate' } | null>(null)

  /** Recarga las marcas desde el backend. */
  function loadBrands() {
    setError(null)
    return fetchBrands()
      .then(setBrands)
      .catch(() => setError('No se pudieron cargar las marcas. ¿Está el backend en el puerto 8080?'))
  }

  useEffect(() => {
    loadBrands()
  }, [])

  /** Alta de marca desde el panel de la página. */
  async function handleCreate() {
    if (newName.trim() === '') {
      return
    }
    setError(null)
    setSaving(true)
    try {
      await createBrand(newName)
      setNewName('')
      setCreating(false)
      await loadBrands()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear la marca')
    } finally {
      setSaving(false)
    }
  }

  /** Ejecuta la baja o reactivación confirmada y refresca el listado. */
  async function runPendingAction() {
    if (!pending) {
      return
    }
    const { brand, action } = pending
    setPending(null)
    setBusyId(brand.id)
    setError(null)
    try {
      if (action === 'deactivate') {
        await deactivateBrand(brand.id)
      } else {
        await reactivateBrand(brand.id)
      }
      await loadBrands()
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : action === 'deactivate'
            ? 'No se pudo dar de baja la marca'
            : 'No se pudo reactivar la marca',
      )
    } finally {
      setBusyId(null)
    }
  }

  const visibleBrands =
    brands === null ? null : brands.filter((brand) => brand.active === (listMode === 'active'))

  return (
    <>
      <div className="page-header">
        <h1>{listMode === 'active' ? 'Marcas' : 'Marcas dadas de baja'}</h1>
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
                Nueva marca
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
          <div className="form-actions">
            <button
              type="button"
              className="secondary"
              onClick={() => {
                setCreating(false)
                setNewName('')
              }}
            >
              Cancelar
            </button>
            <button type="submit" disabled={saving || newName.trim() === ''}>
              {saving ? 'Guardando…' : 'Guardar marca'}
            </button>
          </div>
        </form>
      )}
      {error && <p className="error">{error}</p>}
      {!error && visibleBrands === null && <p>Cargando…</p>}
      {visibleBrands && visibleBrands.length === 0 && (
        <p>{listMode === 'active' ? 'No hay marcas cargadas.' : 'No hay marcas dadas de baja.'}</p>
      )}
      {visibleBrands && visibleBrands.length > 0 && (
        <section className="list">
          {visibleBrands.map((brand) => (
            <BrandCard
              key={brand.id}
              brand={brand}
              busy={busyId === brand.id}
              onUpdated={() => void loadBrands()}
              onDeactivate={() => setPending({ brand, action: 'deactivate' })}
              onReactivate={() => setPending({ brand, action: 'reactivate' })}
            />
          ))}
        </section>
      )}
      {pending && (
        <DoubleConfirm
          key={`${pending.action}-${pending.brand.id}`}
          title={pending.action === 'deactivate' ? 'Dar de baja' : 'Reactivar'}
          firstMessage={
            pending.action === 'deactivate'
              ? `Si das de baja “${pending.brand.name}”, no se va a poder usar en productos nuevos. ¿Estás seguro?`
              : `¿Estás seguro de reactivar “${pending.brand.name}”?`
          }
          secondMessage={
            pending.action === 'deactivate'
              ? '¿Realmente estás seguro? Una marca dada de baja no se puede elegir hasta que la reactives.'
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
