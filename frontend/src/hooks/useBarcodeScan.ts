import { useEffect, type Dispatch, type MutableRefObject, type SetStateAction } from 'react'
import type { ProductDraft } from '../models/drafts'

/**
 * Escucha el teclado a nivel ventana y detecta ráfagas del lector de códigos
 * (dígitos muy juntos). Escribe el código en la variante que tiene el foco.
 */
export function useBarcodeScan(
  draftRef: MutableRefObject<ProductDraft>,
  variantIndexRef: MutableRefObject<number>,
  barcodeInputRefs: MutableRefObject<Array<HTMLInputElement | null>>,
  setDraft: Dispatch<SetStateAction<ProductDraft>>,
) {
  useEffect(() => {
    const scanGapMs = 50
    const idleMs = 120
    const minLength = 8
    let buffer = ''
    let lastAt = 0
    let snapshot: ProductDraft | null = null
    let idleTimer: number | undefined

    /** True si parece un EAN/UPC (8 a 50 dígitos). */
    function looksLikeBarcode(value: string) {
      return value.length >= minLength && /^[0-9]{8,50}$/.test(value)
    }

    /** Pone el código en la variante activa y enfoca el input de barras. */
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

    /** Captura teclas: Enter/Tab cierran el scan; teclas lentas se ignoran. */
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
  }, [barcodeInputRefs, draftRef, setDraft, variantIndexRef])
}
