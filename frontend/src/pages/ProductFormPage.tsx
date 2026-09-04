import { ProductForm } from '../components/ProductForm'
import type { Product } from '../models/product'

type ProductFormPageProps = {
  product?: Product | null
  onSaved: () => void
}

/** Página de alta o edición: el formulario arma el encabezado. */
export function ProductFormPage({ product, onSaved }: ProductFormPageProps) {
  return <ProductForm key={product?.id ?? 'new'} product={product} onSaved={onSaved} />
}
