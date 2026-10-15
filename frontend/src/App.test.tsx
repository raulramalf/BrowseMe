import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
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
