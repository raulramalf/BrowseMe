import { useId, useState } from 'react'
import type { Issue, Severity } from '../api/types'
import { CATEGORY_LABEL, SEVERITY_LABEL } from './labels'

type Filter = 'ALL' | Severity

export function IssueList({ issues }: { issues: Issue[] }) {
  const selectId = useId()
  const [filter, setFilter] = useState<Filter>('ALL')
  const visible = filter === 'ALL' ? issues : issues.filter((issue) => issue.severity === filter)

  if (issues.length === 0) {
    return <p>Las comprobaciones automáticas no han encontrado problemas en esta página.</p>
  }

  return (
    <>
      <div className="filter">
        <label htmlFor={selectId}>Mostrar</label>
        <select id={selectId} value={filter} onChange={(e) => setFilter(e.target.value as Filter)}>
          <option value="ALL">Todos ({issues.length})</option>
          {(Object.keys(SEVERITY_LABEL) as Severity[]).map((severity) => (
            <option key={severity} value={severity}>
              Severidad {SEVERITY_LABEL[severity].toLowerCase()}
            </option>
          ))}
        </select>
      </div>
      {visible.length === 0 ? (
        <p>No hay hallazgos con esta severidad.</p>
      ) : (
        <ul className="issue-list">
          {visible.map((issue) => (
            <li key={issue.ruleId} className={`issue sev-border-${issue.severity.toLowerCase()}`}>
              <h3>{issue.title}</h3>
              <p className="issue-meta">
                <span className={`badge sev-${issue.severity.toLowerCase()}`}>Severidad {SEVERITY_LABEL[issue.severity].toLowerCase()}</span>
                <span className="badge badge-neutral">{CATEGORY_LABEL[issue.category]}</span>
                <code>{issue.ruleId}</code>
              </p>
              <p>{issue.recommendation}</p>
              <details>
                <summary>Ver evidencia y limitaciones</summary>
                <dl>
                  <dt>Evidencia</dt>
                  <dd>{issue.evidence}</dd>
                  {issue.limitation && (
                    <>
                      <dt>Limitación</dt>
                      <dd>{issue.limitation}</dd>
                    </>
                  )}
                </dl>
              </details>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
