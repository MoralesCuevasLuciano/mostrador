/** Cliente HTTP: GET/POST JSON y multipart. Los errores salen del ProblemDetail de Spring. */

/** Extrae el mensaje de un ProblemDetail de Spring, o usa el fallback. */
async function readError(response: Response, fallback: string) {
  try {
    const body = await response.json()
    return typeof body.detail === 'string' ? body.detail : fallback
  } catch {
    return fallback
  }
}

/** GET JSON. Si falla, lanza Error con el detalle del backend. */
export async function getJson<T>(path: string, fallback: string): Promise<T> {
  const response = await fetch(path)
  if (!response.ok) {
    throw new Error(await readError(response, fallback))
  }
  return response.json()
}

/** POST/PUT/DELETE con JSON. Tolera respuestas vacías (204). */
export async function sendJson<T>(
  path: string,
  fallback: string,
  options: RequestInit = {},
): Promise<T> {
  const { headers, ...rest } = options
  const response = await fetch(path, {
    ...rest,
    headers: { 'Content-Type': 'application/json', ...headers },
  })
  if (!response.ok) {
    throw new Error(await readError(response, fallback))
  }
  if (response.status === 204) {
    return undefined as T
  }
  const text = await response.text()
  return text === '' ? (undefined as T) : JSON.parse(text)
}

/** POST multipart (subida de foto). No setea Content-Type: lo arma el navegador. */
export async function sendForm<T>(path: string, body: FormData, fallback: string): Promise<T> {
  const response = await fetch(path, { method: 'POST', body })
  if (!response.ok) {
    throw new Error(await readError(response, fallback))
  }
  return response.json()
}
