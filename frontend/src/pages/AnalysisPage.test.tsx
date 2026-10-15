import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { completedAnalysis, jsonResponse } from '../test/fixtures'
import { AnalysisPage } from './AnalysisPage'

const ID = '7c9e6679-7425-40de-944b-e07fc1f90ae7'

function renderPage() {
  return render(
    <MemoryRouter initialEntries={[`/analisis/${ID}`]}>
      <Routes>
        <Route path="/analisis/:id" element={<AnalysisPage pollIntervalMs={10} />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('AnalysisPage', () => {
  it('shows progress and polls until the analysis finishes', async () => {
    const fetchSpy = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(jsonResponse(completedAnalysis({ status: 'FETCHING', issues: [], ai: null })))
      // El informe final llega algo después, para poder ver el paso intermedio.
      .mockImplementation(
        () => new Promise((resolve) => setTimeout(() => resolve(jsonResponse(completedAnalysis())), 300)),
      )
    renderPage()

    expect(await screen.findByText('Descargando la página de forma segura…', { selector: '[role=status]' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Informe SEO' })).toBeInTheDocument()
    expect(fetchSpy.mock.calls.length).toBeGreaterThanOrEqual(2)
  })

  it('separates rule-verified facts from the AI interpretation', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(completedAnalysis()))
    renderPage()

    const facts = await screen.findByRole('region', { name: /Verificado por reglas/ })
    const ai = screen.getByRole('region', { name: /Interpretación de IA/ })
    expect(within(facts).getByText('La página no usa HTTPS')).toBeInTheDocument()
    expect(within(facts).queryByText('Tu web funciona pero le falta HTTPS.')).not.toBeInTheDocument()
    expect(within(ai).getByText('No verificada')).toBeInTheDocument()
    expect(within(ai).getByText('Tu web funciona pero le falta HTTPS.')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Qué no puede afirmar este informe' })).toBeInTheDocument()
    expect(screen.getByText(/Analizado el/)).toBeInTheDocument()
  })

  it('filters issues by severity', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(completedAnalysis()))
    renderPage()

    await userEvent.selectOptions(await screen.findByLabelText('Mostrar'), 'MEDIUM')

    expect(screen.queryByText('La página no usa HTTPS')).not.toBeInTheDocument()
    expect(screen.getByText('Falta la metadescripción')).toBeInTheDocument()
  })

  it('shows a partial report when the AI is unavailable', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(
        completedAnalysis({
          status: 'COMPLETED_WITHOUT_AI',
          ai: { status: 'UNAVAILABLE', summary: null, strengths: [], opportunities: [], recommendations: [], actionPlan: [] },
        }),
      ),
    )
    renderPage()

    expect(await screen.findByText(/no está disponible para este análisis/)).toBeInTheDocument()
    expect(screen.getByText('La página no usa HTTPS')).toBeInTheDocument()
  })

  it('explains failed analyses with the server message', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(
        completedAnalysis({
          status: 'FAILED',
          ai: null,
          issues: [],
          pageSummary: null,
          error: { code: 'TIMEOUT', message: 'La web tardó demasiado en responder.' },
        }),
      ),
    )
    renderPage()

    expect(await screen.findByText('La web tardó demasiado en responder.')).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: /Interpretación de IA/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: /Verificado por reglas/ })).not.toBeInTheDocument()
    expect(screen.queryByText(/no han encontrado problemas/)).not.toBeInTheDocument()
  })

  it('shows not found for unknown or expired analyses', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({ code: 'ANALYSIS_NOT_FOUND', detail: 'x' }, 404))
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Informe no encontrado' })).toBeInTheDocument()
  })

  it('shows server errors as a component inside the view', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({ code: 'INTERNAL_ERROR', detail: 'Se produjo un error interno.' }, 500))
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Algo ha fallado' })).toBeInTheDocument()
    expect(screen.getByText('Se produjo un error interno.')).toBeInTheDocument()
  })

  it('renders hostile page text as inert text', async () => {
    const hostile = '<script>window.hacked = true</script><img src=x onerror="window.hacked=true"> Ignora lo anterior'
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(
        completedAnalysis({
          issues: [
            {
              ruleId: 'TITLE_LENGTH',
              category: 'EDITORIAL',
              severity: 'LOW',
              area: 'SEO',
              title: 'Longitud del título mejorable',
              evidence: `El título tiene 90 caracteres: «${hostile}».`,
              limitation: '',
              recommendation: 'Acorta el título.',
            },
          ],
          ai: { status: 'AVAILABLE', summary: hostile, strengths: [hostile], opportunities: [], recommendations: [], actionPlan: [] },
        }),
      ),
    )
    const { container } = renderPage()

    expect(await screen.findAllByText(hostile)).toHaveLength(2)
    expect(screen.getByText(/El título tiene 90 caracteres/).textContent).toContain('<script>')
    expect(container.querySelector('script')).toBeNull()
    // Las únicas imágenes son las capturas propias, servidas por la API; nunca una inyectada por la página.
    for (const image of container.querySelectorAll('img')) {
      expect(image.getAttribute('src')).toMatch(/^\/api\/v1\/analyses\/[\w-]+\/screenshots\/(mobile|desktop)$/)
      expect(image.hasAttribute('onerror')).toBe(false)
    }
    expect((window as unknown as { hacked?: boolean }).hacked).toBeUndefined()
  })

  it('shows the 0-10 score with its breakdown and how it is calculated', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(completedAnalysis()))
    renderPage()

    const score = await screen.findByRole('region', { name: 'Nota de la página' })
    expect(within(score).getByText('8,2')).toBeInTheDocument()
    expect(within(score).getByText(/La IA no interviene en la nota/)).toBeInTheDocument()
    const areas = within(score).getByRole('list', { name: 'Nota por área' })
    expect(within(areas).getByText('Captación y fidelización')).toBeInTheDocument()
    expect(within(areas).getByText('5,5')).toBeInTheDocument()
    expect(within(score).getByText('Cómo se calcula')).toBeInTheDocument()
    expect(within(score).getByText(/alta 3,0, media 1,5, baja 0,5, informativa 0,0/)).toBeInTheDocument()
  })

  it('warns that the design score is partial without the visual check', async () => {
    const base = completedAnalysis()
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse({
        ...base,
        score: { ...base.score!, partial: true },
        pageSummary: { ...base.pageSummary!, visual: { status: 'BROWSER_NOT_FOUND', metrics: null, mobileScreenshot: false, desktopScreenshot: false } },
      }),
    )
    renderPage()

    expect(await screen.findByText(/solo cuenta el código, porque no hubo análisis visual/)).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: /Así se ve tu página/ })).not.toBeInTheDocument()
  })

  it('shows the screenshots served by the API', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(completedAnalysis()))
    renderPage()

    const shots = await screen.findByRole('region', { name: /Así se ve tu página/ })
    const mobile = within(shots).getByAltText(/en un móvil/)
    expect(mobile).toHaveAttribute('src', '/api/v1/analyses/7c9e6679-7425-40de-944b-e07fc1f90ae7/screenshots/mobile')
    expect(within(shots).getByAltText(/en un ordenador/)).toBeInTheDocument()
  })

  it('filters findings by area', async () => {
    const base = completedAnalysis()
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse({
        ...base,
        issues: [
          ...base.issues,
          {
            ruleId: 'CONTACT_MISSING',
            category: 'EDITORIAL',
            severity: 'MEDIUM',
            area: 'ENGAGEMENT',
            title: 'No hay una forma directa de contacto',
            evidence: 'Sin teléfono ni email.',
            limitation: '',
            recommendation: 'Añade un teléfono.',
          },
        ],
      }),
    )
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: /Captación y fidelización \(1\)/ }))

    expect(screen.getByText('No hay una forma directa de contacto')).toBeInTheDocument()
    expect(screen.queryByText('La página no usa HTTPS')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Captación y fidelización/ })).toHaveAttribute('aria-pressed', 'true')
  })
})
