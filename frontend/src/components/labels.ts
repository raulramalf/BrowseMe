import type { AnalysisStatus, Area, Category, Severity, VisualStatus } from '../api/types'

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

export const AREA_LABEL: Record<Area, string> = {
  SEO: 'SEO',
  ENGAGEMENT: 'Captación y fidelización',
  DESIGN: 'Diseño',
}

export const AREA_DESCRIPTION: Record<Area, string> = {
  SEO: 'Que los buscadores encuentren y entiendan tu página.',
  ENGAGEMENT: 'Que quien llega pueda contactarte, comprar o volver.',
  DESIGN: 'Patrones de diseño y usabilidad, en el código y en la página dibujada.',
}

export const VISUAL_STATUS_LABEL: Record<VisualStatus, string> = {
  DONE: 'Hecho con un navegador real',
  DISABLED: 'Desactivado',
  BROWSER_NOT_FOUND: 'No se encontró Chrome, Edge ni Chromium en este equipo',
  FAILED: 'El navegador no pudo dibujar la página a tiempo',
  NOT_RUN: 'No realizado',
}

/** Número con coma decimal y una cifra: 7.6 -> «7,6». */
export function formatScore(value: number): string {
  return value.toLocaleString('es-ES', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
}

export const PRIORITY_LABEL = { HIGH: 'Prioridad alta', MEDIUM: 'Prioridad media', LOW: 'Prioridad baja' } as const

export const STATUS_STEP: Record<AnalysisStatus, string> = {
  PENDING: 'En cola…',
  FETCHING: 'Descargando la página de forma segura…',
  ANALYZING: 'Aplicando las comprobaciones y dibujando la página en un navegador…',
  INTERPRETING: 'Preparando la explicación con IA (puede tardar un minuto)…',
  COMPLETED: 'Análisis completado',
  COMPLETED_WITHOUT_AI: 'Análisis completado sin interpretación de IA',
  FAILED: 'No se pudo completar el análisis',
}

export function formatDate(iso: string | null): string {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('es-ES', { dateStyle: 'long', timeStyle: 'short' }).format(new Date(iso))
}
