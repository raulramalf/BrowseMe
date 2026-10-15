import { UrlForm } from '../components/UrlForm'
import { useDocumentTitle } from './useDocumentTitle'

export function AnalyzePage() {
  useDocumentTitle('Analizar mi web')
  return (
    <section className="analyze" aria-labelledby="analizar-titulo">
      <span className="eyebrow">Nuevo análisis</span>
      <h1 id="analizar-titulo">Analiza tu web</h1>
      <p className="lead">
        Indica la dirección de la página que quieres revisar. Analizamos solo esa página, sin ejecutar JavaScript, y te
        mostramos cada hallazgo con su evidencia.
      </p>
      <div className="card-dark analyze-card">
        <UrlForm />
      </div>
      <ul className="analyze-notes">
        <li>
          <strong>Tarda poco.</strong> Las comprobaciones suelen tardar segundos. Si el servidor estaba en reposo, el
          primer análisis puede tardar hasta un minuto en empezar.
        </li>
        <li>
          <strong>Siempre con hechos.</strong> Si la explicación con IA no está disponible, verás igualmente todos los
          hallazgos verificados por reglas.
        </li>
        <li>
          <strong>Sin registro.</strong> Recibirás un enlace al informe, disponible durante 7 días.
        </li>
        <li>
          <strong>Orientativo.</strong> Seguir las recomendaciones no garantiza ningún resultado en buscadores.
        </li>
      </ul>
    </section>
  )
}
