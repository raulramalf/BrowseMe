import type { AnalysisStatus } from '../api/types'
import { STATUS_STEP } from './labels'

const STEPS: AnalysisStatus[] = ['PENDING', 'FETCHING', 'ANALYZING', 'INTERPRETING']

/** Progreso del análisis. aria-live anuncia cada paso a los lectores de pantalla. */
export function StatusView({ status, url }: { status: AnalysisStatus; url: string | null }) {
  const current = STEPS.indexOf(status)
  return (
    <section className="panel" aria-labelledby="progreso-titulo">
      <h1 id="progreso-titulo">Analizando{url ? ':' : '…'}</h1>
      {url && <p className="url-text">{url}</p>}
      <p role="status" aria-live="polite" className="status-line">
        {STATUS_STEP[status]}
      </p>
      <ol className="steps">
        {STEPS.map((step, index) => (
          <li key={step} aria-current={index === current ? 'step' : undefined} className={index <= current ? 'done' : ''}>
            {STATUS_STEP[step]}
          </li>
        ))}
      </ol>
    </section>
  )
}
