import { describe, expect, it } from 'vitest'
import { checkUrl } from './url'

describe('checkUrl', () => {
  it('adds https:// when the scheme is missing', () => {
    expect(checkUrl('  minegocio.es/servicios ')).toEqual({ ok: true, url: 'https://minegocio.es/servicios' })
  })

  it.each([
    ['', 'Escribe la dirección'],
    ['ftp://minegocio.es', 'http o https'],
    ['javascript:alert(1)', 'http o https'],
    ['https://usuario:clave@minegocio.es', 'usuario ni contraseña'],
    ['localhost', 'dominio completo'],
    ['https://' + 'a'.repeat(2100) + '.es', '2048'],
    ['http://exa mple.com', 'formato válido'],
  ])('rejects %s', (input, message) => {
    const result = checkUrl(input)
    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.message).toContain(message)
  })
})
