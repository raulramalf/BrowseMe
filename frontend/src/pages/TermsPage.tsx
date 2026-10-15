import { LegalNotice } from './LegalNotice'
import { useDocumentTitle } from './useDocumentTitle'

export function TermsPage() {
  useDocumentTitle('Términos de uso')
  return (
    <article className="prose">
      <h1>Términos de uso</h1>
      <LegalNotice />
      <h2>El servicio</h2>
      <p>
        BrowseMe ofrece un análisis automático y orientativo de una página web pública. Los resultados no constituyen
        asesoramiento profesional y no garantizan ningún resultado en buscadores.
      </p>
      <h2>Uso permitido</h2>
      <p>
        Analiza páginas públicas, preferentemente propias o sobre las que tengas interés legítimo. No uses el servicio
        para intentar acceder a redes privadas, saturar sitios de terceros ni eludir los límites de uso.
      </p>
      <h2>Interpretación con IA</h2>
      <p>
        La explicación generada por IA puede contener errores. Los hechos verificables son los que aparecen en la
        sección «Verificado por reglas».
      </p>
      <h2>Disponibilidad y responsabilidad</h2>
      <p>[COMPLETAR: condiciones de disponibilidad y limitación de responsabilidad, a revisar con asesoramiento profesional].</p>
      <h2>Titular</h2>
      <p>[COMPLETAR: titular del servicio y datos de contacto].</p>
    </article>
  )
}
