// Espejo de los DTO del backend (infrastructure/web/dto). Todo el texto que
// llega aquí puede venir de una web de terceros: se pinta siempre como texto.

export type AnalysisStatus =
  | 'PENDING'
  | 'FETCHING'
  | 'ANALYZING'
  | 'INTERPRETING'
  | 'COMPLETED'
  | 'COMPLETED_WITHOUT_AI'
  | 'FAILED'

export type Severity = 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO'
export type Category = 'TECHNICAL' | 'EDITORIAL'
export type AiStatus = 'AVAILABLE' | 'UNAVAILABLE' | 'INVALID_OUTPUT'

export interface AnalysisCreated {
  id: string
  status: AnalysisStatus
}

export interface Issue {
  ruleId: string
  category: Category
  severity: Severity
  title: string
  evidence: string
  limitation: string
  recommendation: string
}

export interface PageSummary {
  httpStatus: number
  redirectCount: number
  https: boolean
  title: string | null
  metaDescription: string | null
  lang: string | null
  canonical: string | null
  robotsDirectives: string[]
  headingCounts: Record<string, number>
  internalLinks: number
  externalLinks: number
  images: { total: number; withAlt: number; decorative: number; missingAlt: number }
  viewportPresent: boolean
  visibleWordCount: number
  robotsTxt: 'ALLOWED' | 'BLOCKED' | 'NOT_FOUND' | 'UNAVAILABLE'
}

export interface Recommendation {
  text: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  ruleIds: string[]
  generalSuggestion: boolean
}

export interface AiResult {
  status: AiStatus
  summary: string | null
  strengths: string[]
  opportunities: string[]
  recommendations: Recommendation[]
  actionPlan: string[]
}

export interface Analysis {
  id: string
  status: AnalysisStatus
  requestedUrl: string
  finalUrl: string | null
  createdAt: string
  analyzedAt: string | null
  expiresAt: string
  pageSummary: PageSummary | null
  issues: Issue[]
  severityCounts: Record<Severity, number>
  ai: AiResult | null
  limitations: string[]
  error: { code: string; message: string } | null
}

export const FINAL_STATUSES: readonly AnalysisStatus[] = ['COMPLETED', 'COMPLETED_WITHOUT_AI', 'FAILED']

export function isFinal(status: AnalysisStatus): boolean {
  return FINAL_STATUSES.includes(status)
}
