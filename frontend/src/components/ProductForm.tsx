import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useBarcodeScan } from '../hooks/useBarcodeScan'
import { categoryOptions } from '../mappers/categoryMapper'
import { emptyDraft, emptyVariant, productToDraft } from '../mappers/productMapper'
import type { Brand } from '../models/brand'
import type { Category } from '../models/category'
import type { ProductDraft, VariantDraft } from '../models/drafts'
import type { Product } from '../models/product'
import { fetchBrands } from '../services/brandService'
import { fetchCategories } from '../services/categoryService'
import { createProduct, updateProduct } from '../services/productService'
import { uploadImage } from '../services/uploadService'
import { NewBrandField } from './NewBrandField'
import { NewCategoryField } from './NewCategoryField'

type ProductFormProps = {
  product?: Product | null
  onSaved: () => void
}

/** Formulario de ficha + variantes. Sirve para alta y para edición. */
export function ProductForm({ product, onSaved }: ProductFormProps) {
  const [draft, setDraft] = useState<ProductDraft>(product ? productToDraft(product) : emptyDraft)
  const [brands, setBrands] = useState<Brand[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [uploadingIndex, setUploadingIndex] = useState<number | null>(null)
  const draftRef = useRef(draft)
  const variantIndexRef = useRef(0)
  const barcodeInputRefs = useRef<Array<HTMLInputElement | null>>([])
  draftRef.current = draft

  useBarcodeScan(draftRef, variantIndexRef, barcodeInputRefs, setDraft)

  useEffect(() => {
    fetchBrands().then(setBrands).catch(() => undefined)
    fetchCategories().then(setCategories).catch(() => undefined)
  }, [])

  useEffect(() => {
    setDraft(product ? productToDraft(product) : emptyDraft())
    setError(null)
  }, [product])

  /** Mezcla un parche en el draft de la ficha. */
  function updateDraft(patch: Partial<ProductDraft>) {
    setDraft((current) => ({ ...current, ...patch }))
  }

  /** Mezcla un parche en una variante del draft. */
  function updateVariant(index: number, patch: Partial<VariantDraft>) {
    setDraft((current) => ({
      ...current,
      variants: current.variants.map((variant, i) => (i === index ? { ...variant, ...patch } : variant)),
    }))
  }

  /** Agrega una variante. Si la primera se llamaba “Única”, le borra el distintivo. */
  function addVariant() {
    setDraft((current) => ({
      ...current,
      variants: [
        ...current.variants.map((variant, index) =>
          index === 0 && variant.label === 'Única' ? { ...variant, label: '' } : variant,
        ),
        { ...emptyVariant(), label: '' },
      ],
    }))
  }

  /** Quita una variante. Si queda una sola, el distintivo vuelve a “Única”. */
  function removeVariant(index: number) {
    setDraft((current) => {
      if (current.variants.length === 1) {
        return current
      }
      const variants = current.variants.filter((_, i) => i !== index)
      if (variants.length === 1 && variants[0].label.trim() === '') {
        variants[0] = { ...variants[0], label: 'Única' }
      }
      return { ...current, variants }
    })
  }

  /** Sube el archivo a /api/uploads y deja la URL en la variante. */
  async function handlePhotoFile(index: number, file: File | undefined) {
    if (!file) {
      return
    }
    setError(null)
    setUploadingIndex(index)
    try {
      const url = await uploadImage(file)
      updateVariant(index, { imageUrl: url })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo subir la imagen')
    } finally {
      setUploadingIndex(null)
    }
  }

  /** Alta o edición según haya producto precargado. */
  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSaving(true)
    try {
      if (product) {
        await updateProduct(product.id, product, draft)
      } else {
        await createProduct(draft)
        setDraft(emptyDraft())
      }
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo guardar el producto')
    } finally {
      setSaving(false)
    }
  }

  const manyVariants = draft.variants.length > 1
  const editing = product != null

  return (
    <form className="card form" onSubmit={handleSubmit}>
      <label>
        Nombre
        <input
          required
          maxLength={200}
          value={draft.name}
          onChange={(event) => updateDraft({ name: event.target.value })}
        />
      </label>
      <label>
        Descripción
        <textarea
          rows={2}
          value={draft.description}
          onChange={(event) => updateDraft({ description: event.target.value })}
        />
      </label>
      <div>
        <label>
          Categoría
          <select
            value={draft.categoryId}
            onChange={(event) => updateDraft({ categoryId: event.target.value })}
          >
            <option value="">Sin categoría</option>
            {categoryOptions(categories, draft.categoryId).map((category) => (
              <option key={category.id} value={category.id}>
                {category.label}
              </option>
            ))}
          </select>
        </label>
        <NewCategoryField
          categories={categories}
          onCreated={(category) => {
            setCategories((current) => [...current, category])
            updateDraft({ categoryId: String(category.id) })
          }}
        />
      </div>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={draft.allowsEmployeeDiscount}
          onChange={(event) => updateDraft({ allowsEmployeeDiscount: event.target.checked })}
        />
        Admite descuento de empleado
      </label>

      {draft.variants.map((variant, index) => (
        <fieldset
          key={variant.id ?? `new-${index}`}
          className="variant-fields"
          onFocus={() => {
            variantIndexRef.current = index
          }}
        >
          <legend>{manyVariants ? `Variante ${index + 1}` : 'Precio y código'}</legend>
          {manyVariants && (
            <label>
              Distintivo
              <input
                required
                maxLength={100}
                placeholder="Rivadavia, Rojo, Frozen…"
                value={variant.label}
                onChange={(event) => updateVariant(index, { label: event.target.value })}
              />
            </label>
          )}
          <label>
            Precio
            <input
              required
              inputMode="decimal"
              value={variant.price}
              onChange={(event) => updateVariant(index, { price: event.target.value })}
            />
          </label>
          <div>
            <label>
              Marca
              <select
                value={variant.brandId}
                onChange={(event) => updateVariant(index, { brandId: event.target.value })}
              >
                <option value="">Sin marca</option>
                {brands
                  .filter((brand) => brand.active || String(brand.id) === variant.brandId)
                  .map((brand) => (
                    <option key={brand.id} value={brand.id}>
                      {brand.name}
                    </option>
                  ))}
              </select>
            </label>
            <NewBrandField
              onCreated={(brand) => {
                setBrands((current) => [...current, brand])
                updateVariant(index, { brandId: String(brand.id) })
              }}
            />
          </div>
          <label>
            Código de barras
            <input
              ref={(element) => {
                barcodeInputRefs.current[index] = element
              }}
              maxLength={50}
              value={variant.barcode}
              onChange={(event) => updateVariant(index, { barcode: event.target.value })}
            />
            <span className="hint">El escáner lo completa aunque estés en otro campo.</span>
          </label>
          <label>
            Condición
            <select
              value={variant.itemCondition}
              onChange={(event) =>
                updateVariant(index, { itemCondition: event.target.value as VariantDraft['itemCondition'] })
              }
            >
              <option value="NUEVA">Nueva</option>
              <option value="DEFECTUOSA">Defectuosa</option>
            </select>
          </label>
          <div className="photo-field">
            <label>
              Foto (URL o archivo)
              <input
                maxLength={500}
                placeholder="https://…"
                value={variant.imageUrl}
                onChange={(event) => updateVariant(index, { imageUrl: event.target.value })}
              />
            </label>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              disabled={uploadingIndex === index}
              onChange={(event) => {
                const file = event.target.files?.[0]
                event.target.value = ''
                void handlePhotoFile(index, file)
              }}
            />
            {uploadingIndex === index && <span className="hint">Subiendo…</span>}
            {variant.imageUrl && (
              <img src={variant.imageUrl} alt="" className="photo-preview" />
            )}
          </div>
          {manyVariants && (
            <button type="button" className="linkish" onClick={() => removeVariant(index)}>
              Quitar variante
            </button>
          )}
        </fieldset>
      ))}

      <div className="form-actions">
        <button type="button" className="secondary" onClick={addVariant}>
          Agregar variante
        </button>
        <button type="submit" disabled={saving}>
          {saving ? 'Guardando…' : editing ? 'Guardar cambios' : 'Guardar producto'}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </form>
  )
}
