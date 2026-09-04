import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useBarcodeScan } from '../hooks/useBarcodeScan'
import { categoryOptions } from '../mappers/categoryMapper'
import { emptyDraft, emptyVariant, productToDraft, describeBarcodeOwner, variantToDraft } from '../mappers/productMapper'
import type { Brand } from '../models/brand'
import type { Category } from '../models/category'
import type { ProductDraft, VariantDraft } from '../models/drafts'
import type { Product } from '../models/product'
import type { Variant } from '../models/variant'
import { fetchBrands } from '../services/brandService'
import { fetchCategories } from '../services/categoryService'
import { createProduct, fetchBarcodeMatches, reactivateVariant, updateProduct } from '../services/productService'
import { uploadImage } from '../services/uploadService'
import { DoubleConfirm } from './DoubleConfirm'
import { InactiveVariantsModal } from './InactiveVariantsModal'
import { NewBrandField } from './NewBrandField'
import { NewCategoryField } from './NewCategoryField'

type ProductFormProps = {
  product?: Product | null
  onSaved: () => void
}

/** Conflicto de código de barras pendiente de confirmar. */
type BarcodePending = {
  code: string
  index: number
  ifCancel: ProductDraft
  owners: string[]
  applyOnConfirm: boolean
}

/** Formulario de ficha + variantes. Sirve para alta y para edición. */
export function ProductForm({ product, onSaved }: ProductFormProps) {
  const [draft, setDraft] = useState<ProductDraft>(product ? productToDraft(product) : emptyDraft)
  const [brands, setBrands] = useState<Brand[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [uploadingIndex, setUploadingIndex] = useState<number | null>(null)
  const [barcodePending, setBarcodePending] = useState<BarcodePending | null>(null)
  const [inactiveVariants, setInactiveVariants] = useState<Variant[]>(
    product?.variants.filter((variant) => !variant.active) ?? [],
  )
  const [showInactive, setShowInactive] = useState(false)
  const [reactivatingId, setReactivatingId] = useState<number | null>(null)
  const draftRef = useRef(draft)
  const variantIndexRef = useRef(0)
  const barcodeInputRefs = useRef<Array<HTMLInputElement | null>>([])
  const barcodeAtFocusRef = useRef<Array<string>>([])
  const onScannedRef = useRef<(code: string, index: number, before: ProductDraft) => void>(() => undefined)
  draftRef.current = draft

  useBarcodeScan(draftRef, variantIndexRef, barcodeInputRefs, onScannedRef)

  useEffect(() => {
    fetchBrands().then(setBrands).catch(() => undefined)
    fetchCategories().then(setCategories).catch(() => undefined)
  }, [])

  useEffect(() => {
    setDraft(product ? productToDraft(product) : emptyDraft())
    setInactiveVariants(product?.variants.filter((variant) => !variant.active) ?? [])
    setShowInactive(false)
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

  /** Escribe el código en la variante, partiendo de un draft conocido. */
  function applyBarcode(from: ProductDraft, index: number, code: string) {
    setDraft({
      ...from,
      variants: from.variants.map((variant, i) => (i === index ? { ...variant, barcode: code } : variant)),
    })
  }

  /** Quién más ya tiene ese código: catálogo y otras variantes de este formulario. */
  async function ownersOf(code: string, index: number, source: ProductDraft) {
    const currentId = source.variants[index]?.id
    const matches = await fetchBarcodeMatches(code)
    const fromApi = matches
      .filter((match) => match.variantId !== currentId)
      .map((match) => describeBarcodeOwner(match, product?.id))
    const fromDraft = source.variants.flatMap((variant, i) => {
      if (i === index || variant.barcode.trim() !== code) {
        return []
      }
      return [`este producto, ${variant.label.trim() || `variante ${i + 1}`}`]
    })
    return [...new Set([...fromApi, ...fromDraft])]
  }

  /** Tras un scan: avisa si el código está repetido; si no, lo carga. */
  async function considerScannedBarcode(code: string, index: number, before: ProductDraft) {
    try {
      const owners = await ownersOf(code, index, before)
      if (owners.length === 0) {
        applyBarcode(before, index, code)
        return
      }
      setBarcodePending({ code, index, ifCancel: before, owners, applyOnConfirm: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo consultar el código de barras')
      applyBarcode(before, index, code)
    }
  }

  onScannedRef.current = (code, index, before) => {
    void considerScannedBarcode(code, index, before)
  }

  /** Al salir del input: si el código cambió y ya existe, pide confirmación. */
  async function considerTypedBarcode(index: number) {
    const code = draft.variants[index].barcode.trim()
    const previous = barcodeAtFocusRef.current[index] ?? ''
    if (code === '' || code === previous) {
      return
    }
    try {
      const owners = await ownersOf(code, index, draft)
      if (owners.length === 0) {
        return
      }
      const ifCancel = {
        ...draft,
        variants: draft.variants.map((variant, i) =>
          i === index ? { ...variant, barcode: previous } : variant,
        ),
      }
      setBarcodePending({ code, index, ifCancel, owners, applyOnConfirm: false })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo consultar el código de barras')
    }
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

  /** Reactiva una variante dada de baja y la suma al formulario. */
  async function restoreVariant(variant: Variant) {
    if (!product) {
      return
    }
    setError(null)
    setReactivatingId(variant.id)
    try {
      await reactivateVariant(product.id, variant.id)
      setDraft((current) => {
        const restored = variantToDraft(variant)
        if (current.variants.length === 0) {
          return { ...current, variants: [restored] }
        }
        const variants = current.variants.map((item, index) =>
          index === 0 && item.label === 'Única' ? { ...item, label: '' } : item,
        )
        return { ...current, variants: [...variants, restored] }
      })
      setInactiveVariants((current) => current.filter((item) => item.id !== variant.id))
      setShowInactive((open) => open && inactiveVariants.length > 1)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo reactivar la variante')
    } finally {
      setReactivatingId(null)
    }
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
    <>
    <div className="page-header">
      <h1>{editing ? 'Editar producto' : 'Cargar producto'}</h1>
      {editing && inactiveVariants.length > 0 && (
        <div className="page-actions">
          <button type="button" className="secondary" onClick={() => setShowInactive(true)}>
            Variantes dadas de baja ({inactiveVariants.length})
          </button>
        </div>
      )}
    </div>
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
      <label className="checkbox">
        <input
          type="checkbox"
          checked={draft.tracksStock}
          onChange={(event) => updateDraft({ tracksStock: event.target.checked })}
        />
        Lleva inventario
      </label>
      <span className="hint">Desmarcar en caramelos sueltos, fotocopias y lo que no se cuenta.</span>

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
              onFocus={() => {
                barcodeAtFocusRef.current[index] = variant.barcode
              }}
              onBlur={() => {
                void considerTypedBarcode(index)
              }}
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
      {showInactive && (
        <InactiveVariantsModal
          variants={inactiveVariants}
          reactivatingId={reactivatingId}
          onReactivate={(variant) => void restoreVariant(variant)}
          onClose={() => setShowInactive(false)}
        />
      )}
      {barcodePending && (
        <DoubleConfirm
          key={`${barcodePending.code}-${barcodePending.index}`}
          title="Código de barras repetido"
          firstMessage={
            barcodePending.owners.length === 1
              ? `Este código ya lo tiene ${barcodePending.owners[0]}. ¿Estás seguro de usarlo igual?`
              : `Este código ya lo tienen ${barcodePending.owners.join('; ')}. ¿Estás seguro de usarlo igual?`
          }
          secondMessage="¿Realmente estás seguro? El código de barras va a quedar repetido en más de un artículo."
          onConfirm={() => {
            if (barcodePending.applyOnConfirm) {
              applyBarcode(barcodePending.ifCancel, barcodePending.index, barcodePending.code)
            }
            setBarcodePending(null)
          }}
          onCancel={() => {
            setDraft(barcodePending.ifCancel)
            setBarcodePending(null)
          }}
        />
      )}
    </>
  )
}
