import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'

function renderAt(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App routes', () => {
  it.each([
    ['/', '¿Cómo ven los buscadores la web de tu negocio?'],
    ['/analizar', 'Analiza tu web'],
    ['/descargar', 'Descarga BrowseMe'],
    ['/sobre', 'Sobre BrowseMe'],
    ['/privacidad', 'Política de privacidad'],
    ['/terminos', 'Términos de uso'],
    ['/cookies', 'Cookies'],
    ['/no-existe', 'Página no encontrada'],
  ])('%s', (path, heading) => {
    renderAt(path)
    expect(screen.getByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
  })

  it('legal pages carry placeholders and the not-legal-advice notice', () => {
    renderAt('/privacidad')
    expect(screen.getByText(/No es asesoramiento jurídico/)).toBeInTheDocument()
    expect(screen.getAllByText(/\[COMPLETAR:/).length).toBeGreaterThan(0)
  })

  it('landing links to the analyzer', () => {
    renderAt('/')
    expect(screen.getByRole('link', { name: 'Analizar mi web gratis' })).toHaveAttribute('href', '/analizar')
    expect(screen.getByRole('link', { name: 'Analizar mi web' })).toHaveAttribute('href', '/analizar')
  })

  it('analyzer page shows the URL form', () => {
    renderAt('/analizar')
    expect(screen.getByLabelText('Dirección de tu web')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Analizar' })).toBeInTheDocument()
  })

  it('has a skip link and labelled navigation', () => {
    renderAt('/')
    expect(screen.getByRole('link', { name: 'Saltar al contenido' })).toHaveAttribute('href', '#contenido')
    expect(screen.getByRole('navigation', { name: 'Principal' })).toBeInTheDocument()
  })
})

describe('public site (VITE_PUBLIC_SITE=true)', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('offers the download instead of the analyzer', () => {
    vi.stubEnv('VITE_PUBLIC_SITE', 'true')
    renderAt('/')
    expect(screen.getByRole('link', { name: 'Descargar BrowseMe gratis' })).toHaveAttribute('href', '/descargar')
    expect(screen.getByRole('link', { name: 'Descargar' })).toHaveAttribute('href', '/descargar')
    expect(screen.queryByRole('link', { name: 'Analizar mi web' })).not.toBeInTheDocument()
  })

  it('redirects the analyzer to the download page', () => {
    vi.stubEnv('VITE_PUBLIC_SITE', 'true')
    renderAt('/analizar')
    expect(screen.getByRole('heading', { level: 1, name: 'Descarga BrowseMe' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Dirección de tu web')).not.toBeInTheDocument()
  })

  it('links each platform to the latest release', () => {
    vi.stubEnv('VITE_PUBLIC_SITE', 'true')
    renderAt('/descargar')
    expect(screen.getByRole('link', { name: 'Descargar para Windows' })).toHaveAttribute(
      'href',
      'https://github.com/raulramalf/BrowseMe/releases/latest/download/BrowseMe-windows.zip',
    )
  })
})
