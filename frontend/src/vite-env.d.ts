interface ImportMetaEnv {
  /** "true" al compilar la web informativa (Vercel): sin analizador, con descarga. */
  readonly VITE_PUBLIC_SITE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
