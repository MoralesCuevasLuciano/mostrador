/** Cadena vacía o solo espacios → null (campos opcionales hacia la API). */
export function blankToNull(value: string) {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}
