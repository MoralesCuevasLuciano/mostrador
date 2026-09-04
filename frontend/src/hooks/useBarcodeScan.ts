import { useEffect, type MutableRefObject } from 'react'
import type { ProductDraft } from '../models/drafts'

type ScanHandler = (code: string, index: number, before: ProductDraft) => void

/**
 * Escucha el teclado a nivel ventana y detecta ráfagas del lector de códigos
 * (dígitos muy juntos). Entrega el código a onScanned sin escribirlo todavía.
 */
export function useBarcodeScan(
  draftRef: MutableRefObject<ProductDraft>,
  variantIndexRef: MutableRefObject<number>,
  barcodeInputRefs: MutableRefObject<Array<HTMLInputElement | null>>,
  onScannedRef: MutableRefObject<ScanHandler>,
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

    /** Entrega el código de la ráfaga y enfoca el input de barras. */
    function apply(code: string) {
      const before = snapshot ?? structuredClone(draftRef.current)
      const index = Math.min(variantIndexRef.current, Math.max(before.variants.length - 1, 0))
      snapshot = null
      buffer = ''
      onScannedRef.current(code, index, before)
      window.setTimeout(() => barcodeInputRefs.current[index]?.focus(), 0)
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
  }, [barcodeInputRefs, draftRef, onScannedRef, variantIndexRef])
}
