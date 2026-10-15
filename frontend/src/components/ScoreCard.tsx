import type { Score, Severity } from '../api/types'
import { AREA_DESCRIPTION, AREA_LABEL, formatScore, SEVERITY_LABEL } from './labels'

const SEVERITIES: Severity[] = ['HIGH', 'MEDIUM', 'LOW', 'INFO']

/**
 * Nota de 0 a 10 y su desglose. La calcula el backend con una fórmula fija; aquí
 * solo se muestra y se explica con los mismos datos que la produjeron.
 */
export function ScoreCard({ score }: { score: Score }) {
  return (
    <section className="panel-dark score" aria-labelledby="nota-titulo">
      <div className="score-main">
        <span className="eyebrow">Valoración</span>
        <h2 id="nota-titulo">Nota de la página</h2>
        <p className="score-value">
          <span className="score-number">{formatScore(score.value)}</span>
          <span className="score-max">de 10</span>
        </p>
        <p className="score-note">
          Calculada con reglas fijas a partir de los hallazgos. La IA no interviene en la nota.
          {score.partial && ' La parte de diseño solo cuenta el código, porque no hubo análisis visual.'}
        </p>
      </div>

      <ul className="score-areas" aria-label="Nota por área">
        {score.areas.map((area) => (
          <li key={area.area} className="score-area">
            <div className="score-area-head">
              <span className="score-area-name">{AREA_LABEL[area.area]}</span>
              <span className="score-area-value">{formatScore(area.value)}</span>
            </div>
            <div className="meter" aria-hidden="true">
              <span className="meter-fill" style={{ width: `${Math.max(0, Math.min(10, area.value)) * 10}%` }} />
            </div>
            <p className="score-area-meta">
              {AREA_DESCRIPTION[area.area]} Peso {area.weightPercent} % ·{' '}
              {area.findings === 1 ? '1 hallazgo' : `${area.findings} hallazgos`}
              {area.partial && ' · parcial'}
            </p>
          </li>
        ))}
      </ul>

      <details className="score-method">
        <summary>Cómo se calcula</summary>
        <ol>
          <li>Cada área empieza en 10.</li>
          <li>
            Cada hallazgo resta según su importancia:{' '}
            {SEVERITIES.map((s) => `${SEVERITY_LABEL[s].toLowerCase()} ${formatScore(score.penalties[s] ?? 0)}`).join(
              ', ',
            )}
            . Un área no baja de 0.
          </li>
          <li>
            La nota final es la media ponderada:{' '}
            {score.areas.map((a) => `${AREA_LABEL[a.area]} ${a.weightPercent} %`).join(', ')}.
          </li>
          <li>La misma página recibe siempre la misma nota. Es orientativa: no mide ventas ni visitas.</li>
        </ol>
      </details>
    </section>
  )
}
