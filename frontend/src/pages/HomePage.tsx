import { UrlForm } from '../components/UrlForm'
import { useDocumentTitle } from './useDocumentTitle'

/** Cifras que describen el propio servicio (no métricas de terceros). */
const STATS: [string, string][] = [
  ['1', 'Página por análisis'],
  ['0', 'Cookies de seguimiento'],
  ['7 días', 'Y el informe se borra'],
  ['100 %', 'Código abierto'],
]

export function HomePage() {
  useDocumentTitle('Análisis SEO para pequeños negocios')
  return (
    <>
      <section className="hero" aria-labelledby="inicio-titulo">
        <div>
          <span className="eyebrow">Análisis SEO · Reglas verificables · IA local</span>
          <h1 id="inicio-titulo">¿Cómo ven los buscadores la web de tu negocio?</h1>
          <p className="lead">
            BrowseMe revisa la página que indiques, comprueba los puntos básicos de SEO y te lo explica en lenguaje
            sencillo. Es gratis, no necesitas registrarte y no usamos cookies de seguimiento.
          </p>
          <UrlForm />
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="hero-art-module hero-art-white">
            <span className="hero-art-index">01</span>
            <span className="hero-art-title">
              Descarga segura
              <span className="hero-art-sub">Solo la página indicada</span>
            </span>
          </div>
          <div className="hero-art-module hero-art-mint">
            <span className="hero-art-index">02</span>
            <span className="hero-art-title">
              Reglas deterministas
              <span className="hero-art-sub">Cada hallazgo, con su evidencia</span>
            </span>
          </div>
          <div className="hero-art-module hero-art-yellow">
            <span className="hero-art-index">03</span>
            <span className="hero-art-title">
              Explicación con IA local
              <span className="hero-art-sub">Interpreta, no detecta</span>
            </span>
          </div>
        </div>
      </section>

      <ul className="stats" aria-label="BrowseMe en cifras">
        {STATS.map(([number, label]) => (
          <li key={label}>
            <span className="stat-number">{number}</span>
            <span className="stat-label">{label}</span>
          </li>
        ))}
      </ul>

      <section className="card-light section" aria-labelledby="como-titulo">
        <div className="section-head">
          <span className="eyebrow">Proceso</span>
          <h2 id="como-titulo">Cómo funciona</h2>
          <p>Los hechos los producen reglas fijas en código. La IA solo explica y prioriza lo ya detectado.</p>
        </div>
        <ol className="steps-grid">
          <li className="step-card">
            <span className="step-number">1</span>
            <h3>Descarga segura</h3>
            <p>Descargamos solo la página que indicas, como lo haría un navegador sin ejecutar JavaScript.</p>
          </li>
          <li className="step-card">
            <span className="step-number">2</span>
            <h3>Comprobaciones automáticas</h3>
            <p>
              Unas reglas fijas revisan título, descripción, encabezados, HTTPS, imágenes, indexación y más. Cada
              hallazgo muestra su evidencia.
            </p>
          </li>
          <li className="step-card">
            <span className="step-number">3</span>
            <h3>Explicación con IA local</h3>
            <p>
              Un modelo de IA que funciona en nuestro propio servidor explica y prioriza esos hallazgos. No detecta nada
              por su cuenta y lo marcamos como interpretación.
            </p>
          </li>
        </ol>
      </section>

      <section className="two-columns" aria-label="Alcance del análisis">
        <div className="card-dark">
          <span className="eyebrow">Incluido</span>
          <h2>Qué analiza</h2>
          <ul className="check-list">
            <li>Título, metadescripción e idioma</li>
            <li>Encabezados (H1, H2…) y su estructura</li>
            <li>HTTPS, redirecciones y errores de respuesta</li>
            <li>Indexación: noindex, canonical y robots.txt</li>
            <li>Imágenes sin texto alternativo y cantidad de texto</li>
            <li>Configuración básica para móviles</li>
          </ul>
        </div>
        <div className="card-dark">
          <span className="eyebrow">Fuera de alcance</span>
          <h2>Qué no puede analizar</h2>
          <ul className="check-list is-limit">
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
