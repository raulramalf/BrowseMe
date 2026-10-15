import { ErrorPanel } from '../components/ErrorPanel'
import { useDocumentTitle } from './useDocumentTitle'

export function NotFoundPage() {
  useDocumentTitle('Página no encontrada')
  return <ErrorPanel title="Página no encontrada" message="La dirección que buscas no existe en BrowseMe." />
}
