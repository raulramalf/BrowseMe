/**
 * La misma base de código se compila de dos formas:
 * - App (por defecto): la app de escritorio o Docker, con el analizador.
 * - Web informativa (VITE_PUBLIC_SITE=true, la que se publica en Vercel): sin
 *   backend; en lugar del analizador ofrece la descarga de la app.
 */
export function isPublicSite(): boolean {
  return import.meta.env.VITE_PUBLIC_SITE === 'true'
}

export const RELEASES_URL = 'https://github.com/raulramalf/BrowseMe/releases'

/** Enlace estable a un archivo de la última versión publicada. */
export function downloadUrl(file: string): string {
  return `${RELEASES_URL}/latest/download/${file}`
}
