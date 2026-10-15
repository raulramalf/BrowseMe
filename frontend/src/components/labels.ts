import type { AnalysisStatus, Category, Severity } from '../api/types'

export const SEVERITY_LABEL: Record<Severity, string> = {
  HIGH: 'Alta',
  MEDIUM: 'Media',
  LOW: 'Baja',
  INFO: 'Informativa',
}

export const CATEGORY_LABEL: Record<Category, string> = {
  TECHNICAL: 'Técnico',
  EDITORIAL: 'Sugerencia editorial',
}

export const PRIORITY_LABEL = { HIGH: 'Prioridad alta', MEDIUM: 'Prioridad media', LOW: 'Prioridad baja' } as const

export const STATUS_STEP: Record<AnalysisStatus, string> = {
  PENDING: 'En cola…',
  FETCHING: 'Descargando la página de forma segura…',
  ANALYZING: 'Aplicando las comprobaciones SEO…',
  INTERPRETING: 'Preparando la explicación con IA (puede tardar un minuto)…',
  COMPLETED: 'Análisis completado',
  COMPLETED_WITHOUT_AI: 'Análisis completado sin interpretación de IA',
  FAILED: 'No se pudo completar el análisis',
}

export function formatDate(iso: string | null): string {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('es-ES', { dateStyle: 'long', timeStyle: 'short' }).format(new Date(iso))
}
