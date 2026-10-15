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
/** SEO, captación y fidelización de clientes, y patrones de diseño. */
export type Area = 'SEO' | 'ENGAGEMENT' | 'DESIGN'
export type VisualStatus = 'DONE' | 'DISABLED' | 'BROWSER_NOT_FOUND' | 'FAILED' | 'NOT_RUN'
export type AiStatus = 'AVAILABLE' | 'UNAVAILABLE' | 'INVALID_OUTPUT'

export interface AnalysisCreated {
  id: string
  status: AnalysisStatus
}

export interface Issue {
  ruleId: string
  category: Category
  severity: Severity
  area: Area
  title: string
  evidence: string
  limitation: string
  recommendation: string
  /** Puntos (0 a 10) que subiría la nota global si se corrigiera este hallazgo. */
  scoreGain: number
  /** Guía fija del backend: por qué importa y qué hacer. No describe la página. */
  why: string
  steps: string[]
  /** Influye directamente en que lleguen clientes. */
  affectsClients: boolean
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
  engagement: Engagement
  design: Design
  visual: Visual
}

export interface Engagement {
  phoneLinks: number
  emailLinks: number
  whatsappLinks: number
  forms: number
  emailForms: number
  callsToAction: number
  socialNetworks: string[]
  schemaTypes: string[]
  testimonials: boolean
  openGraphTitle: boolean
  openGraphImage: boolean
}

export interface Design {
  navigation: boolean
  favicon: boolean
  genericLinkTexts: number
  emptyInteractive: number
  formFields: number
  unlabeledFormFields: number
  imagesWithoutDimensions: number
}

export interface VisualMetrics {
  viewportWidth: number
  contentWidth: number
  textElementsChecked: number
  lowContrastTexts: number
  smallTexts: number
  tapTargetsChecked: number
  smallTapTargets: number
  fontFamilies: string[]
  callToActionAboveFoldMobile: boolean
  callToActionAboveFoldDesktop: boolean
  pageHeightScreens: number
}

export interface Visual {
  status: VisualStatus
  metrics: VisualMetrics | null
  mobileScreenshot: boolean
  desktopScreenshot: boolean
}

export interface AreaScore {
  area: Area
  value: number
  weightPercent: number
  findings: number
  partial: boolean
}

/** Nota de 0 a 10 calculada por el backend con una fórmula fija (nunca por la IA). */
export interface Score {
  value: number
  partial: boolean
  areas: AreaScore[]
  penalties: Record<Severity, number>
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
  score: Score | null
  ai: AiResult | null
  limitations: string[]
  error: { code: string; message: string } | null
}

export const FINAL_STATUSES: readonly AnalysisStatus[] = ['COMPLETED', 'COMPLETED_WITHOUT_AI', 'FAILED']

export function isFinal(status: AnalysisStatus): boolean {
  return FINAL_STATUSES.includes(status)
}
