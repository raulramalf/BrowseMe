import { UrlForm } from '../components/UrlForm'
import { useDocumentTitle } from './useDocumentTitle'

export function HomePage() {
  useDocumentTitle('Análisis SEO para pequeños negocios')
  return (
    <>
      <section className="hero" aria-labelledby="inicio-titulo">
        <h1 id="inicio-titulo">¿Cómo ven los buscadores la web de tu negocio?</h1>
        <p className="lead">
          BrowseMe revisa la página que indiques, comprueba los puntos básicos de SEO y te lo explica en lenguaje
          sencillo. Es gratis, no necesitas registrarte y no usamos cookies de seguimiento.
        </p>
        <UrlForm />
      </section>

      <section aria-labelledby="como-titulo">
        <h2 id="como-titulo">Cómo funciona</h2>
        <ol className="how">
          <li>
            <strong>Descarga segura.</strong> Descargamos solo la página que indicas, como lo haría un navegador sin
            ejecutar JavaScript.
          </li>
          <li>
            <strong>Comprobaciones automáticas.</strong> Unas reglas fijas revisan título, descripción, encabezados,
            HTTPS, imágenes, indexación y más. Cada hallazgo muestra su evidencia.
          </li>
          <li>
            <strong>Explicación con IA local.</strong> Un modelo de IA que funciona en nuestro propio servidor explica y
            prioriza esos hallazgos. No detecta nada por su cuenta y lo marcamos como interpretación.
          </li>
        </ol>
      </section>

      <section className="two-columns" aria-label="Alcance del análisis">
        <div>
          <h2>Qué analiza</h2>
          <ul>
            <li>Título, metadescripción e idioma</li>
            <li>Encabezados (H1, H2…) y su estructura</li>
            <li>HTTPS, redirecciones y errores de respuesta</li>
            <li>Indexación: noindex, canonical y robots.txt</li>
            <li>Imágenes sin texto alternativo y cantidad de texto</li>
            <li>Configuración básica para móviles</li>
          </ul>
        </div>
        <div>
          <h2>Qué no puede analizar</h2>
          <ul>
            <li>La velocidad real de carga</li>
            <li>Cómo se usa realmente en un móvil</li>
            <li>La calidad de tus textos o tu posición en Google</li>
            <li>Contenido generado con JavaScript</li>
            <li>El resto de páginas de tu sitio</li>
          </ul>
        </div>
      </section>

      <p className="notice">
        El análisis es automático y orientativo. Seguir sus recomendaciones no garantiza ningún resultado en
        buscadores.
      </p>
    </>
  )
}
