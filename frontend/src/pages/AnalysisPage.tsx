import { useParams } from 'react-router'
import { AiSection } from '../components/AiSection'
import { ErrorPanel } from '../components/ErrorPanel'
import { IssueList } from '../components/IssueList'
import { formatDate } from '../components/labels'
import { Limitations } from '../components/Limitations'
import { PageFacts } from '../components/PageFacts'
import { SeverityCounts } from '../components/SeverityCounts'
import { StatusView } from '../components/StatusView'
import { useAnalysisPolling } from '../hooks/useAnalysisPolling'
import { isFinal } from '../api/types'
import { useDocumentTitle } from './useDocumentTitle'

export function AnalysisPage({ pollIntervalMs }: { pollIntervalMs?: number }) {
  const { id = '' } = useParams()
  const { analysis, error } = useAnalysisPolling(id, pollIntervalMs)
  useDocumentTitle('Informe')

  if (!analysis && error) {
    if (error.status === 404 || error.status === 400) {
      return <ErrorPanel title="Informe no encontrado" message="Este análisis no existe o ya ha caducado (se borran a los 7 días)." />
    }
    return <ErrorPanel title="Algo ha fallado" message={error.message} />
  }
  if (!analysis) {
    return (
      <p role="status" aria-live="polite">
        Cargando el análisis…
      </p>
    )
  }
  if (!isFinal(analysis.status)) {
    return (
      <>
        {error && (
          <p className="notice" role="status">
            {error.message} Reintentaremos automáticamente.
          </p>
        )}
        <StatusView status={analysis.status} url={analysis.requestedUrl} />
      </>
    )
  }

  const failed = analysis.status === 'FAILED'
  return (
    <article className="report" aria-labelledby="informe-titulo">
      <header className="report-header">
        <span className="eyebrow">Informe · Una página analizada</span>
        <h1 id="informe-titulo">Informe SEO</h1>
        <p className="url-text">{analysis.finalUrl ?? analysis.requestedUrl}</p>
        <p className="meta">
          Analizado el {formatDate(analysis.analyzedAt)} · Disponible hasta el {formatDate(analysis.expiresAt)}
        </p>
      </header>

      {failed && analysis.error && (
        <ErrorPanel title="No se pudo completar el análisis" message={analysis.error.message} />
      )}

      <section className="panel panel-facts" aria-labelledby="verificado-titulo">
        <h2 id="verificado-titulo" className="panel-heading">
          Verificado por reglas <span className="badge badge-facts">Comprobado automáticamente</span>
        </h2>
        <SeverityCounts counts={analysis.severityCounts} />
        <IssueList issues={analysis.issues} />
        {analysis.pageSummary && <PageFacts summary={analysis.pageSummary} />}
      </section>

      {!failed && <AiSection ai={analysis.ai} />}

      <Limitations items={analysis.limitations} />
    </article>
  )
}
