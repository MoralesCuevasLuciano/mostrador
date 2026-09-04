/** Sucursal tal como sale de GET /api/branches. */
export type Branch = {
  id: number
  name: string
  address: string | null
  phone: string | null
  pointOfSale: number | null
  active: boolean
}
