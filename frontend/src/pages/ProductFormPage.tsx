import { ProductForm } from '../components/ProductForm'
import type { Product } from '../models/product'

type ProductFormPageProps = {
  product?: Product | null
  onSaved: () => void
}

/** Título + formulario de alta o edición. */
export function ProductFormPage({ product, onSaved }: ProductFormPageProps) {
  return (
    <>
      <h1>{product ? 'Editar producto' : 'Cargar producto'}</h1>
      <ProductForm key={product?.id ?? 'new'} product={product} onSaved={onSaved} />
    </>
  )
}
