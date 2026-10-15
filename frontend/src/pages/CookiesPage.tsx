import { LegalNotice } from './LegalNotice'
import { useDocumentTitle } from './useDocumentTitle'

export function CookiesPage() {
  useDocumentTitle('Cookies')
  return (
    <article className="prose">
      <h1>Cookies</h1>
      <LegalNotice />
      <p>
        BrowseMe no usa cookies de analítica, publicidad ni seguimiento, ni de terceros. Por eso no mostramos un banner
        de cookies.
      </p>
      <p>
        Si en el futuro se añadiera alguna cookie no esencial, se informaría aquí y se pediría tu consentimiento antes
        de usarla.
      </p>
    </article>
  )
}
