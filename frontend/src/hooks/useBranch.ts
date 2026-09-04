import { useEffect, useMemo, useState } from 'react'
import type { Branch } from '../models/branch'
import { fetchBranches } from '../services/branchService'

const STORAGE_KEY = 'mostrador.branchId'

/** Lee la sucursal guardada; null si no hay o no es un id válido. */
function readStoredBranchId(): number | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (raw == null) {
    return null
  }
  const id = Number(raw)
  return Number.isInteger(id) && id > 0 ? id : null
}

/** Persiste la sucursal activa para que sobreviva un refresh. */
function persistBranchId(id: number) {
  localStorage.setItem(STORAGE_KEY, String(id))
}

/**
 * Sucursal de trabajo: se carga al arrancar y queda en localStorage.
 * El catálogo no se filtra; inventario (y más adelante caja) sí.
 */
export function useBranch() {
  const [branches, setBranches] = useState<Branch[] | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(readStoredBranchId)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchBranches()
      .then((list) => {
        setBranches(list)
        const active = list.filter((branch) => branch.active)
        const stored = readStoredBranchId()
        const next =
          stored != null && active.some((branch) => branch.id === stored)
            ? stored
            : (active[0]?.id ?? null)
        setSelectedId(next)
        if (next != null) {
          persistBranchId(next)
        }
      })
      .catch(() => setError('No se pudieron cargar las sucursales. ¿Está el backend en el puerto 8080?'))
  }, [])

  /** Cambia la sucursal de trabajo y la recuerda. */
  function selectBranch(id: number) {
    setSelectedId(id)
    persistBranchId(id)
  }

  const activeBranches = useMemo(
    () => (branches ?? []).filter((branch) => branch.active),
    [branches],
  )
  const selected = activeBranches.find((branch) => branch.id === selectedId) ?? null

  return {
    activeBranches,
    selected,
    selectBranch,
    error,
    loading: branches === null && error === null,
  }
}
