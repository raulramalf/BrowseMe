import type { Analysis } from '../api/types'

export function completedAnalysis(overrides: Partial<Analysis> = {}): Analysis {
  return {
    id: '7c9e6679-7425-40de-944b-e07fc1f90ae7',
    status: 'COMPLETED',
    requestedUrl: 'http://example.com/',
    finalUrl: 'http://example.com/',
    createdAt: '2026-10-15T10:00:00Z',
    analyzedAt: '2026-10-15T10:00:40Z',
    expiresAt: '2026-10-22T10:00:00Z',
    pageSummary: {
      httpStatus: 200,
      redirectCount: 0,
      https: false,
      title: 'Panadería Ejemplo',
      metaDescription: null,
      lang: 'es',
      canonical: null,
      robotsDirectives: [],
      headingCounts: { h1: 1, h2: 2, h3: 0, h4: 0, h5: 0, h6: 0 },
      internalLinks: 4,
      externalLinks: 1,
      images: { total: 3, withAlt: 1, decorative: 1, missingAlt: 1 },
      viewportPresent: true,
      visibleWordCount: 300,
      robotsTxt: 'ALLOWED',
    },
    issues: [
      {
        ruleId: 'NOT_HTTPS',
        category: 'TECHNICAL',
        severity: 'HIGH',
        title: 'La página no usa HTTPS',
        evidence: 'La página final se sirve por HTTP, sin cifrar: http://example.com/.',
        limitation: '',
        recommendation: 'Activa HTTPS.',
      },
      {
        ruleId: 'META_DESC_MISSING',
        category: 'TECHNICAL',
        severity: 'MEDIUM',
        title: 'Falta la metadescripción',
        evidence: 'La página no tiene metadescripción.',
        limitation: 'Google puede generar su propio fragmento.',
        recommendation: 'Añade una metadescripción.',
      },
    ],
    severityCounts: { HIGH: 1, MEDIUM: 1, LOW: 0, INFO: 0 },
    ai: {
      status: 'AVAILABLE',
      summary: 'Tu web funciona pero le falta HTTPS.',
      strengths: ['Tiene un título claro'],
      opportunities: [],
      recommendations: [{ text: 'Activa HTTPS con tu proveedor', priority: 'HIGH', ruleIds: ['NOT_HTTPS'], generalSuggestion: false }],
      actionPlan: ['Llama a tu proveedor de alojamiento'],
    },
    limitations: ['No mide la velocidad real de carga de la página.'],
    error: null,
    ...overrides,
  }
}

export function jsonResponse(body: unknown, status = 200, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json', ...headers },
  })
}
