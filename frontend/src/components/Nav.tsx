type NavProps = {
  listActive: boolean
  createActive: boolean
  onList: () => void
  onCreate: () => void
}

/** Barra superior: Productos (incluye gestión de rubros/marcas) | Cargar producto. */
export function Nav({ listActive, createActive, onList, onCreate }: NavProps) {
  return (
    <nav className="nav">
      <p className="nav-brand">Mostrador</p>
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
    </nav>
  )
}
