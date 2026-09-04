import { sendForm } from './http'

/** Subida de fotos a /api/uploads. */
/** Sube una foto a /api/uploads y devuelve la URL pública. */
export async function uploadImage(file: File): Promise<string> {
  const body = new FormData()
  body.append('file', file)
  const data = await sendForm<{ url: string }>('/api/uploads', body, 'No se pudo subir la imagen')
  return data.url
}
