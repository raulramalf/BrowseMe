import type { Issue, PageSummary } from '../api/types'
import { AREA_LABEL, formatGain } from './labels'

/** Lo que la página ya hace bien, a partir de las señales detectadas (hechos, no consejos). */
function strengths(summary: PageSummary): string[] {
  const e = summary.engagement
  const found: string[] = []
  if (e.phoneLinks > 0) found.push('Tienes un teléfono pulsable')
  if (e.whatsappLinks > 0) found.push('Los clientes pueden escribirte por WhatsApp')
  if (e.emailLinks > 0 || e.forms > 0) found.push('Hay forma de enviarte un mensaje (email o formulario)')
  if (e.callsToAction > 0) found.push('Hay botones o enlaces que invitan a actuar')
  if (e.emailForms > 0) found.push('Puedes recoger el email de quien te visita')
  if (e.testimonials || e.schemaTypes.some((t) => t === 'Review' || t === 'AggregateRating')) {
    found.push('Muestras opiniones de clientes')
  }
  if (e.socialNetworks.length > 0) found.push(`Enlazas tus redes (${e.socialNetworks.join(', ')})`)
  if (e.openGraphTitle && e.openGraphImage) found.push('Tu enlace se ve bien al compartirlo')
  if (summary.https) found.push('Tu web usa HTTPS, un detalle que da confianza')
  return found
}

/**
 * Plan para conseguir más clientes, sin IA: los hallazgos que más influyen en
 * que te encuentren, confíen y contacten, ordenados por los puntos que sumaría
 * corregirlos. Los pasos son una guía fija del backend; la IA no interviene.
 */
export function GrowthPlan({ issues, summary }: { issues: Issue[]; summary: PageSummary | null }) {
  const items = issues
    .filter((issue) => issue.affectsClients)
    .sort((a, b) => b.scoreGain - a.scoreGain || a.title.localeCompare(b.title, 'es'))
  const good = summary ? strengths(summary) : []

  return (
    <section className="panel-dark growth" aria-labelledby="clientes-titulo">
      <span className="eyebrow">Plan de acción</span>
      <h2 id="clientes-titulo">Cómo conseguir más clientes</h2>
      <p className="growth-note">
        Lo que más influye en que te encuentren, confíen y te contacten, ordenado por lo que sumaría a tu nota. Son
        pistas a partir de lo que se ve en tu página: no medimos tus visitas ni tus ventas.
      </p>

      {items.length === 0 ? (
        <p className="notice" role="note">
          No hemos detectado carencias importantes para captar clientes en esta página. Sigue revisando que tus
          datos de contacto estén al día.
        </p>
      ) : (
        <ol className="growth-list">
          {items.map((issue, index) => (
            <li key={issue.ruleId} className="growth-item">
              <div className="growth-head">
                <span className="growth-rank" aria-hidden="true">
                  {index + 1}
                </span>
                <h3>{issue.title}</h3>
                {issue.scoreGain > 0 && (
                  <span className="badge badge-gain" title="Puntos que sumaría a tu nota de 0 a 10">
                    {formatGain(issue.scoreGain)} puntos
                  </span>
                )}
              </div>
              <p className="growth-why">{issue.why}</p>
              <ul className="growth-steps">
                {issue.steps.map((step) => (
                  <li key={step}>{step}</li>
                ))}
              </ul>
              <p className="growth-meta">
                {AREA_LABEL[issue.area]} · En tu página: {issue.evidence}
              </p>
            </li>
          ))}
        </ol>
      )}

      {good.length > 0 && (
        <div className="growth-good">
          <h3>Ya lo estás haciendo bien</h3>
          <ul className="check-list">
            {good.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}
