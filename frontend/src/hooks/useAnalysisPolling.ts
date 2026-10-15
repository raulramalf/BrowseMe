import { useEffect, useState } from 'react'
import { ApiError, getAnalysis } from '../api/client'
import { isFinal, type Analysis } from '../api/types'

export const POLL_INTERVAL_MS = 2000

export interface PollingState {
  analysis: Analysis | null
  error: ApiError | null
}

/** Consulta el análisis cada pocos segundos hasta que llega a un estado final. */
export function useAnalysisPolling(id: string, intervalMs = POLL_INTERVAL_MS): PollingState {
  const [state, setState] = useState<PollingState>({ analysis: null, error: null })

  useEffect(() => {
    let cancelled = false
    let timer: ReturnType<typeof setTimeout> | undefined

    const poll = async () => {
      try {
        const analysis = await getAnalysis(id)
        if (cancelled) return
        setState({ analysis, error: null })
        if (!isFinal(analysis.status)) {
          timer = setTimeout(poll, intervalMs)
        }
      } catch (e) {
        if (cancelled) return
        const error = e instanceof ApiError ? e : new ApiError(0, 'UNKNOWN', 'Error inesperado.')
        if (error.status === 429 || error.status === 503 || error.status === 0) {
          // Saturación o red: se reintenta respetando Retry-After, sin perder lo ya mostrado.
          setState((previous) => ({ ...previous, error }))
          timer = setTimeout(poll, (error.retryAfterSeconds ?? intervalMs / 1000) * 1000)
          return
        }
        setState({ analysis: null, error })
      }
    }

    setState({ analysis: null, error: null })
    void poll()
    return () => {
      cancelled = true
      if (timer) clearTimeout(timer)
    }
  }, [id, intervalMs])

  return state
}
