import type { AiResult } from '../api/types'
import { PRIORITY_LABEL } from './labels'

function TextList({ title, items }: { title: string; items: string[] }) {
  if (items.length === 0) return null
  return (
    <>
      <h3>{title}</h3>
      <ul>
        {items.map((item, index) => (
          <li key={index}>{item}</li>
        ))}
      </ul>
    </>
  )
}

/**
 * Interpretación de la IA, separada visualmente de los hechos verificados.
 * Nunca se presenta como resultado técnico comprobado.
 */
export function AiSection({ ai }: { ai: AiResult | null }) {
  return (
    <section className="panel panel-ai" aria-labelledby="ia-titulo">
      <h2 id="ia-titulo">
        Interpretación de IA <span className="badge badge-ai">No verificada</span>
      </h2>
      <p className="ai-disclaimer">
        Texto generado por un modelo de IA local a partir de los hallazgos anteriores. Puede equivocarse: los datos
        comprobados son los de la sección «Verificado por reglas».
      </p>
      {ai === null || ai.status !== 'AVAILABLE' || ai.summary === null ? (
        <p role="note" className="notice">
          {ai?.status === 'INVALID_OUTPUT'
            ? 'La respuesta de la IA no superó la validación y no se muestra.'
            : 'La interpretación con IA no está disponible para este análisis.'}{' '}
          Los hallazgos verificados por reglas siguen siendo válidos.
        </p>
      ) : (
        <>
          <p>{ai.summary}</p>
          <TextList title="Puntos fuertes" items={ai.strengths} />
          <TextList title="Oportunidades" items={ai.opportunities} />
          {ai.recommendations.length > 0 && (
            <>
              <h3>Recomendaciones</h3>
              <ul className="recommendations">
                {ai.recommendations.map((rec, index) => (
                  <li key={index}>
                    <span className="badge badge-neutral">{PRIORITY_LABEL[rec.priority]}</span> {rec.text}
                    <span className="rec-source">
                      {rec.generalSuggestion ? ' (sugerencia general)' : ` (basada en: ${rec.ruleIds.join(', ')})`}
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
          <TextList title="Plan de acción" items={ai.actionPlan} />
        </>
      )}
    </section>
  )
}
