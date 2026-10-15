import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { jsonResponse } from '../test/fixtures'
import { UrlForm } from './UrlForm'

function renderForm() {
  render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<UrlForm />} />
        <Route path="/analisis/:id" element={<p>Página del informe</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('UrlForm', () => {
  it('validates on the client without calling the API', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    renderForm()

    await userEvent.click(screen.getByRole('button', { name: 'Analizar' }))

    expect(screen.getByRole('alert')).toHaveTextContent('Escribe la dirección de tu web.')
    expect(screen.getByLabelText('Dirección de tu web')).toHaveAttribute('aria-invalid', 'true')
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('sends the URL and navigates to the report', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse({ id: 'abc', status: 'PENDING' }, 202))
    renderForm()

    await userEvent.type(screen.getByLabelText('Dirección de tu web'), 'minegocio.es')
    await userEvent.click(screen.getByRole('button', { name: 'Analizar' }))

    expect(await screen.findByText('Página del informe')).toBeInTheDocument()
    const [path, init] = fetchSpy.mock.calls[0]!
    expect(path).toBe('/api/v1/analyses')
    expect(JSON.parse(String(init?.body))).toEqual({ url: 'https://minegocio.es' })
  })

  it('shows the server message when the URL is rejected', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse({ code: 'URL_REJECTED', detail: 'La dirección apunta a una red privada o reservada' }, 422),
    )
    renderForm()

    await userEvent.type(screen.getByLabelText('Dirección de tu web'), 'intranet.local.es')
    await userEvent.click(screen.getByRole('button', { name: 'Analizar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('red privada o reservada')
    expect(screen.getByRole('button', { name: 'Analizar' })).toBeEnabled()
  })
})
