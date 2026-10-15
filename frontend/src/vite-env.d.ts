/** Versión de BrowseMe (archivo VERSION), fijada al compilar. */
declare const __APP_VERSION__: string

interface ImportMetaEnv {
  /** "true" al compilar la web informativa (Vercel): sin analizador, con descarga. */
  readonly VITE_PUBLIC_SITE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
