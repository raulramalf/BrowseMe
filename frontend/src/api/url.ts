/** Validación en cliente: solo para dar respuesta rápida. La real la hace el servidor. */
export const MAX_URL_LENGTH = 2048

export type UrlCheck = { ok: true; url: string } | { ok: false; message: string }

export function checkUrl(raw: string): UrlCheck {
  const value = raw.trim()
  if (value === '') {
    return { ok: false, message: 'Escribe la dirección de tu web.' }
  }
  // Si falta el esquema, se asume https://, que es lo que escribe la mayoría de la gente.
  const withScheme = /^[a-z][a-z0-9+.-]*:/i.test(value) ? value : `https://${value}`
  if (withScheme.length > MAX_URL_LENGTH) {
    return { ok: false, message: `La dirección no puede superar ${MAX_URL_LENGTH} caracteres.` }
  }
  let parsed: URL
  try {
    parsed = new URL(withScheme)
  } catch {
    return { ok: false, message: 'La dirección no tiene un formato válido.' }
  }
  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    return { ok: false, message: 'Solo se pueden analizar direcciones http o https.' }
  }
  if (parsed.username || parsed.password) {
    return { ok: false, message: 'La dirección no puede incluir usuario ni contraseña.' }
  }
  if (!parsed.hostname.includes('.')) {
    return { ok: false, message: 'Escribe un dominio completo, por ejemplo minegocio.es.' }
  }
  return { ok: true, url: withScheme }
}
