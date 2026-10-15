// vite.config.ts solo necesita leer un archivo de texto. Se declara esa única función
// en lugar de añadir @types/node como dependencia solo para esto.
declare module 'node:fs' {
  export function readFileSync(path: URL, encoding: 'utf8'): string
}
