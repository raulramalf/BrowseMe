import type { Analysis, AnalysisCreated } from './types'

/** Error de la API con el `code` estable de Problem Details (RFC 9457). */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly retryAfterSeconds: number | null

  constructor(status: number, code: string, message: string, retryAfterSeconds: number | null = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.retryAfterSeconds = retryAfterSeconds
  }
}

const NETWORK_ERROR = 'No se pudo contactar con el servidor. Comprueba tu conexión.'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, { ...init, headers: { Accept: 'application/json', ...init?.headers } })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', NETWORK_ERROR)
  }
  if (response.ok) {
    return (await response.json()) as T
  }
  const retryAfter = Number(response.headers.get('Retry-After'))
  let code = 'HTTP_' + response.status
  let detail =
    response.status === 502 || response.status === 504
      ? 'El servidor de análisis no está disponible ahora mismo. Vuelve a intentarlo en un momento.'
      : 'Se produjo un error inesperado. Vuelve a intentarlo más tarde.'
  try {
    const problem = (await response.json()) as { code?: unknown; detail?: unknown }
    if (typeof problem.code === 'string') code = problem.code
    if (typeof problem.detail === 'string') detail = problem.detail
  } catch {
    // Cuerpo no JSON (por ejemplo, un proxy caído): se mantiene el mensaje genérico.
  }
  throw new ApiError(response.status, code, detail, Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : null)
}

export function createAnalysis(url: string): Promise<AnalysisCreated> {
  return request<AnalysisCreated>('/api/v1/analyses', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ url }),
  })
}

export function getAnalysis(id: string): Promise<Analysis> {
  return request<Analysis>(`/api/v1/analyses/${encodeURIComponent(id)}`)
}
