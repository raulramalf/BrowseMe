import type { Severity } from '../api/types'
import { SEVERITY_LABEL } from './labels'

const ORDER: Severity[] = ['HIGH', 'MEDIUM', 'LOW', 'INFO']

export function SeverityCounts({ counts }: { counts: Record<Severity, number> }) {
  return (
    <ul className="severity-counts" aria-label="Hallazgos por severidad">
      {ORDER.map((severity) => (
        <li key={severity} className={`sev sev-dot-${severity.toLowerCase()}`}>
          <span className="sev-count">{counts[severity] ?? 0}</span>
          <span className="sev-label">{SEVERITY_LABEL[severity]}</span>
        </li>
      ))}
    </ul>
  )
}
