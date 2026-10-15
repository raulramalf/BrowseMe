interface ImportMetaEnv {
  /** Origen del backend si se aloja aparte (sin barra final). Vacío = mismo origen. */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
