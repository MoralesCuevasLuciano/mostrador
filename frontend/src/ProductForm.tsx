import { useEffect, useRef, useState, type FormEvent } from 'react'
import {
  createBrand,
  createCategory,
  createProduct,
  fetchBrands,
  fetchCategories,
  type ProductDraft,
  type VariantDraft,
} from './api'
import type { Brand, Category } from './types'

const emptyVariant = (): VariantDraft => ({
  brandId: '',
  label: 'Única',
  barcode: '',
  price: '',
  itemCondition: 'NUEVA',
  imageUrl: '',
})

const emptyDraft = (): ProductDraft => ({
  name: '',
  description: '',
  categoryId: '',
  allowsEmployeeDiscount: true,
  variants: [emptyVariant()],
})

function categoryOptions(categories: Category[]) {
  const active = categories.filter((category) => category.active)
  const roots = active.filter((category) => category.parentId == null)
  const childrenOf = (id: number) => active.filter((category) => category.parentId === id)
  return roots.flatMap((root) => [
    { id: root.id, label: root.name },
    ...childrenOf(root.id).map((child) => ({ id: child.id, label: `— ${child.name}` })),
  ])
}

function NewCategoryPanel({
  categories,
  onCreated,
}: {
  categories: Category[]
  onCreated: (category: Category) => void
}) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [parentId, setParentId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const roots = categories.filter((category) => category.active && category.parentId == null)

  async function save() {
    setError(null)
    setSaving(true)
    try {
      const category = await createCategory(name, parentId)
      setName('')
      setParentId('')
      setOpen(false)
      onCreated(category)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear la categoría')
    } finally {
      setSaving(false)
    }
  }

  if (!open) {
    return (
      <button type="button" className="link-add" onClick={() => setOpen(true)}>
        Nueva categoría
      </button>
    )
  }

  return (
    <div className="inline-create">
      <input
        maxLength={100}
        placeholder="Nombre"
        value={name}
        onChange={(event) => setName(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault()
            void save()
          }
        }}
      />
      <select value={parentId} onChange={(event) => setParentId(event.target.value)}>
        <option value="">Rubro (sin padre)</option>
        {roots.map((root) => (
          <option key={root.id} value={root.id}>
            Hija de {root.name}
          </option>
        ))}
      </select>
      <button type="button" disabled={saving || name.trim() === ''} onClick={() => void save()}>
        Crear
      </button>
      <button type="button" className="linkish" onClick={() => setOpen(false)}>
        Cancelar
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  )
}

function NewBrandPanel({ onCreated }: { onCreated: (brand: Brand) => void }) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  async function save() {
    setError(null)
    setSaving(true)
    try {
      const brand = await createBrand(name)
      setName('')
      setOpen(false)
      onCreated(brand)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear la marca')
    } finally {
      setSaving(false)
    }
  }

  if (!open) {
    return (
      <button type="button" className="link-add" onClick={() => setOpen(true)}>
        Nueva marca
      </button>
    )
  }

  return (
    <div className="inline-create">
      <input
        maxLength={100}
        placeholder="Nombre"
        value={name}
        onChange={(event) => setName(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault()
            void save()
          }
        }}
      />
      <button type="button" disabled={saving || name.trim() === ''} onClick={() => void save()}>
        Crear
      </button>
      <button type="button" className="linkish" onClick={() => setOpen(false)}>
        Cancelar
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  )
}

type ProductFormProps = {
  onCreated: () => void
}

export function ProductForm({ onCreated }: ProductFormProps) {
  const [draft, setDraft] = useState<ProductDraft>(emptyDraft)
  const [brands, setBrands] = useState<Brand[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const draftRef = useRef(draft)
  const variantIndexRef = useRef(0)
  const barcodeInputRefs = useRef<Array<HTMLInputElement | null>>([])
  draftRef.current = draft

  useEffect(() => {
    fetchBrands().then(setBrands).catch(() => undefined)
    fetchCategories().then(setCategories).catch(() => undefined)
  }, [])

  useEffect(() => {
    const scanGapMs = 50
    const idleMs = 120
    const minLength = 8
    let buffer = ''
    let lastAt = 0
    let snapshot: ProductDraft | null = null
    let idleTimer: number | undefined

    function looksLikeBarcode(value: string) {
      return value.length >= minLength && /^[0-9]{8,50}$/.test(value)
    }

    function apply(code: string) {
      const before = snapshot ?? structuredClone(draftRef.current)
      const index = Math.min(variantIndexRef.current, before.variants.length - 1)
      setDraft({
        ...before,
        variants: before.variants.map((variant, i) =>
          i === index ? { ...variant, barcode: code } : variant,
        ),
      })
      window.setTimeout(() => barcodeInputRefs.current[index]?.focus(), 0)
      snapshot = null
      buffer = ''
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.ctrlKey || event.metaKey || event.altKey) {
        return
      }
      const now = performance.now()
      const finishKeys = event.key === 'Enter' || event.key === 'Tab'
      if (finishKeys) {
        if (looksLikeBarcode(buffer)) {
          event.preventDefault()
          event.stopPropagation()
          window.clearTimeout(idleTimer)
          apply(buffer)
        } else {
          buffer = ''
          snapshot = null
        }
        return
      }
      if (event.key.length !== 1) {
        return
      }
      if (now - lastAt > scanGapMs) {
        buffer = event.key
        snapshot = structuredClone(draftRef.current)
      } else {
        buffer += event.key
        event.preventDefault()
      }
      lastAt = now
      window.clearTimeout(idleTimer)
      idleTimer = window.setTimeout(() => {
        if (looksLikeBarcode(buffer)) {
          apply(buffer)
        } else {
          buffer = ''
          snapshot = null
        }
      }, idleMs)
    }

    window.addEventListener('keydown', onKeyDown, true)
    return () => {
      window.removeEventListener('keydown', onKeyDown, true)
      window.clearTimeout(idleTimer)
    }
  }, [])

  function updateDraft(patch: Partial<ProductDraft>) {
    setDraft((current) => ({ ...current, ...patch }))
  }

  function updateVariant(index: number, patch: Partial<VariantDraft>) {
    setDraft((current) => ({
      ...current,
      variants: current.variants.map((variant, i) => (i === index ? { ...variant, ...patch } : variant)),
    }))
  }

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

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSaving(true)
    try {
      await createProduct(draft)
      setDraft(emptyDraft())
      onCreated()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear el producto')
    } finally {
      setSaving(false)
    }
  }

  const activeBrands = brands.filter((brand) => brand.active)
  const manyVariants = draft.variants.length > 1

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
            {categoryOptions(categories).map((category) => (
              <option key={category.id} value={category.id}>
                {category.label}
              </option>
            ))}
          </select>
        </label>
        <NewCategoryPanel
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
          key={index}
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
                {activeBrands.map((brand) => (
                  <option key={brand.id} value={brand.id}>
                    {brand.name}
                  </option>
                ))}
              </select>
            </label>
            <NewBrandPanel
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
          <label>
            Foto (URL)
            <input
              maxLength={500}
              value={variant.imageUrl}
              onChange={(event) => updateVariant(index, { imageUrl: event.target.value })}
            />
          </label>
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
          {saving ? 'Guardando…' : 'Guardar producto'}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </form>
  )
}
