import type { PageSummary } from '../api/types'
import { VISUAL_STATUS_LABEL } from './labels'

const ROBOTS_LABEL: Record<PageSummary['robotsTxt'], string> = {
  ALLOWED: 'Permite el rastreo de esta página',
  BLOCKED: 'Bloquea esta página a Google',
  NOT_FOUND: 'No existe (sin restricciones)',
  UNAVAILABLE: 'No se pudo leer',
}

function show(value: string | null): string {
  if (value === null) return 'No existe'
  if (value.trim() === '') return 'Vacío'
  return value
}

export function PageFacts({ summary }: { summary: PageSummary }) {
  const e = summary.engagement
  const d = summary.design
  const m = summary.visual.metrics
  const rows: [string, string][] = [
    ['Código de respuesta', String(summary.httpStatus)],
    ['HTTPS', summary.https ? 'Sí' : 'No'],
    ['Redirecciones', String(summary.redirectCount)],
    ['Título', show(summary.title)],
    ['Metadescripción', show(summary.metaDescription)],
    ['Idioma declarado', show(summary.lang)],
    ['URL canónica', show(summary.canonical)],
    ['Encabezados H1 / H2 / H3', `${summary.headingCounts.h1 ?? 0} / ${summary.headingCounts.h2 ?? 0} / ${summary.headingCounts.h3 ?? 0}`],
    ['Enlaces internos / externos', `${summary.internalLinks} / ${summary.externalLinks}`],
    ['Imágenes (sin alt / decorativas / total)', `${summary.images.missingAlt} / ${summary.images.decorative} / ${summary.images.total}`],
    ['Configuración para móviles (viewport)', summary.viewportPresent ? 'Sí' : 'No'],
    ['Palabras visibles (aprox.)', String(summary.visibleWordCount)],
    ['robots.txt', ROBOTS_LABEL[summary.robotsTxt]],
    ['Directivas de robots', summary.robotsDirectives.length ? summary.robotsDirectives.join(', ') : 'Ninguna'],
    ['Contacto (teléfono / email / WhatsApp / formularios)', `${e.phoneLinks} / ${e.emailLinks} / ${e.whatsappLinks} / ${e.forms}`],
    ['Llamadas a la acción detectadas', String(e.callsToAction)],
    ['Redes sociales enlazadas', e.socialNetworks.length ? e.socialNetworks.join(', ') : 'Ninguna'],
    ['Datos estructurados (schema.org)', e.schemaTypes.length ? e.schemaTypes.join(', ') : 'Ninguno'],
    ['Vista previa al compartir (og:title / og:image)', `${e.openGraphTitle ? 'Sí' : 'No'} / ${e.openGraphImage ? 'Sí' : 'No'}`],
    ['Menú de navegación / favicon', `${d.navigation ? 'Sí' : 'No'} / ${d.favicon ? 'Sí' : 'No'}`],
    ['Campos de formulario sin etiqueta', `${d.unlabeledFormFields} de ${d.formFields}`],
    ['Análisis visual', VISUAL_STATUS_LABEL[summary.visual.status]],
    ...(m
      ? ([
          ['Ancho del contenido en móvil', `${m.contentWidth} px de ${m.viewportWidth} px`],
          ['Textos con poco contraste', `${m.lowContrastTexts} de ${m.textElementsChecked}`],
          ['Elementos pulsables pequeños en móvil', `${m.smallTapTargets} de ${m.tapTargetsChecked}`],
          ['Tipografías', m.fontFamilies.length ? m.fontFamilies.join(', ') : 'Ninguna'],
        ] as [string, string][])
      : []),
  ]
  return (
    <details className="facts">
      <summary>Datos extraídos de la página</summary>
      <dl className="facts-grid">
        {rows.map(([label, value]) => (
          <div key={label}>
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
    </details>
  )
}
