import type { Branch } from '../models/branch'

type NavProps = {
  listActive: boolean
  createActive: boolean
  inventoryActive: boolean
  cashActive: boolean
  onList: () => void
  onCreate: () => void
  onInventory: () => void
  onCash: () => void
  branches: Branch[]
  selectedBranchId: number | null
  onSelectBranch: (id: number) => void
  branchesLoading: boolean
}

/** Barra superior: caja, catálogo, inventario y sucursal de trabajo. */
export function Nav({
  listActive,
  createActive,
  inventoryActive,
  cashActive,
  onList,
  onCreate,
  onInventory,
  onCash,
  branches,
  selectedBranchId,
  onSelectBranch,
  branchesLoading,
}: NavProps) {
  return (
    <nav className="nav">
      <p className="nav-brand">Mostrador</p>
      <button
        type="button"
        className={cashActive ? 'nav-link active' : 'nav-link'}
        onClick={onCash}
      >
        Caja
      </button>
      <button type="button" className={listActive ? 'nav-link active' : 'nav-link'} onClick={onList}>
        Productos
      </button>
      <button
        type="button"
        className={createActive ? 'nav-link active' : 'nav-link'}
        onClick={onCreate}
      >
        Cargar producto
      </button>
      <button
        type="button"
        className={inventoryActive ? 'nav-link active' : 'nav-link'}
        onClick={onInventory}
      >
        Inventario
      </button>
      <label className="nav-branch">
        Sucursal
        <select
          value={selectedBranchId ?? ''}
          disabled={branchesLoading || branches.length === 0}
          onChange={(event) => onSelectBranch(Number(event.target.value))}
        >
          {branches.length === 0 && (
            <option value="">{branchesLoading ? 'Cargando…' : 'Sin sucursales'}</option>
          )}
          {branches.map((branch) => (
            <option key={branch.id} value={branch.id}>
              {branch.name}
            </option>
          ))}
        </select>
      </label>
    </nav>
  )
}
