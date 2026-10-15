import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { isPublicSite } from '../site'
import { FAQ } from './faq'
import { useDocumentTitle } from './useDocumentTitle'

/** Cifras que describen el propio servicio (no métricas de terceros). */
const STATS: [string, string][] = [
  ['1', 'Página por análisis'],
  ['0', 'Cookies de seguimiento'],
  ['100 %', 'En tu ordenador'],
  ['Apache 2.0', 'Código abierto'],
]

type IconName = 'evidence' | 'ai' | 'lock' | 'scale'

/** Iconos lineales decorativos; heredan el color del texto. */
function Icon({ name }: { name: IconName }) {
  const paths: Record<IconName, ReactNode> = {
    evidence: (
      <>
        <rect x="4" y="3" width="16" height="18" rx="2" />
        <path d="M8 8h8M8 12h5" />
        <path d="m9 16 2 2 4-4" />
      </>
    ),
    ai: (
      <>
        <path d="M12 3v3M12 18v3M3 12h3M18 12h3" />
        <path d="M12 8.5 13.2 11l2.3 1-2.3 1L12 15.5 10.8 13l-2.3-1 2.3-1z" />
      </>
    ),
    lock: (
      <>
        <rect x="5" y="10" width="14" height="11" rx="2" />
        <path d="M8 10V7a4 4 0 0 1 8 0v3" />
      </>
    ),
    scale: (
      <>
        <path d="M12 4v16M7 20h10M5 8h14" />
        <path d="m5 8-2.5 6a2.5 2.5 0 0 0 5 0zM19 8l-2.5 6a2.5 2.5 0 0 0 5 0z" />
      </>
    ),
  }
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
      {paths[name]}
    </svg>
  )
}

const FEATURES: [IconName, string, string, string][] = [
  ['evidence', 'Hechos', 'Hallazgos con evidencia', 'Cada problema sale de una regla fija y muestra exactamente qué se encontró en tu página.'],
  ['ai', 'IA local', 'Explicado sin tecnicismos', 'Un modelo de IA que se ejecuta en tu ordenador traduce los hallazgos a un plan de acción claro.'],
  ['lock', 'Privacidad', 'Sin registro ni rastreo', 'No hay cuentas, analítica ni publicidad. Tus análisis no salen de tu ordenador.'],
  ['scale', 'Transparencia', 'Una nota que se explica', 'Valoración de 0 a 10 con fórmula pública y desglose por área. La misma página, siempre la misma nota.'],
]

const FAQ_PREVIEW = FAQ.slice(0, 5)

export function HomePage() {
  useDocumentTitle('Análisis SEO para pequeños negocios')
  const publicSite = isPublicSite()
  const primary = publicSite
    ? { to: '/descargar', hero: 'Descargar BrowseMe gratis', band: 'Descargar BrowseMe' }
    : { to: '/analizar', hero: 'Analizar mi web gratis', band: 'Empezar el análisis' }
  return (
    <>
      <section className="hero" aria-labelledby="inicio-titulo">
        <div>
          <span className="eyebrow">Nota 0-10 · Captación de clientes · Diseño · IA local</span>
          <h1 id="inicio-titulo">¿Cómo ven los buscadores la web de tu negocio?</h1>
          <p className="lead">
            BrowseMe revisa la página que indiques, le pone una nota de 0 a 10 y te dice cómo captar y fidelizar más
            clientes y qué patrones de diseño mejorar. Todo explicado en lenguaje
            sencillo. Es gratis, no necesitas registrarte y no usamos cookies de seguimiento.
          </p>
          <div className="cta-row">
            <Link to={primary.to} className="button">
              {primary.hero}
            </Link>
            <a href="#como-funciona" className="button button-ghost">
              Ver cómo funciona
            </a>
          </div>
        </div>

        <figure className="preview" aria-label="Vista de ejemplo de un informe">
          <div className="preview-chrome" aria-hidden="true">
            <span />
            <span />
            <span />
            <span className="preview-address">browseme · informe</span>
          </div>
          <div className="preview-body" aria-hidden="true">
            <span className="preview-eyebrow">Verificado por reglas</span>
            <div className="preview-counts">
              <span className="sev-high">Alta</span>
              <span className="sev-medium">Media</span>
              <span className="sev-low">Baja</span>
            </div>
            <div className="preview-issue sev-border-high">
              <strong>La página no usa HTTPS</strong>
              <span>Evidencia: la página se sirve por HTTP</span>
            </div>
            <div className="preview-issue sev-border-medium">
              <strong>Falta la metadescripción</strong>
              <span>Evidencia: no hay etiqueta description</span>
            </div>
            <div className="preview-ai">
              <span className="badge badge-ai">IA · No verificada</span>
              <span>Empieza por activar HTTPS con tu proveedor…</span>
            </div>
          </div>
          <figcaption>Ejemplo ilustrativo de informe</figcaption>
        </figure>
      </section>

      <ul className="stats" aria-label="BrowseMe en cifras">
        {STATS.map(([number, label]) => (
          <li key={label}>
            <span className="stat-number">{number}</span>
            <span className="stat-label">{label}</span>
          </li>
        ))}
      </ul>

      <section className="section" aria-labelledby="ventajas-titulo">
        <div className="section-head on-dark">
          <span className="eyebrow">Por qué BrowseMe</span>
          <h2 id="ventajas-titulo">Un informe en el que puedes confiar</h2>
          <p>Separamos lo que está comprobado de lo que es interpretación, para que sepas siempre qué es un hecho.</p>
        </div>
        <ul className="feature-grid">
          {FEATURES.map(([icon, tag, title, text]) => (
            <li key={title} className="feature-tile">
              <span className="feature-icon">
                <Icon name={icon} />
              </span>
              <span className="eyebrow">{tag}</span>
              <h3>{title}</h3>
              <p>{text}</p>
            </li>
          ))}
        </ul>
      </section>

      <section id="como-funciona" className="card-light section" aria-labelledby="como-titulo">
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
              Un modelo de IA que funciona en tu propio ordenador explica y prioriza esos hallazgos. No detecta nada
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
            <li>SEO: título, descripción, encabezados, HTTPS e indexación</li>
            <li>Captación: contacto, llamadas a la acción, suscripción y opiniones</li>
            <li>Fidelización: redes sociales, datos del negocio y vista previa al compartir</li>
            <li>Diseño del código: menú, formularios con etiquetas y enlaces claros</li>
            <li>Diseño dibujado: desbordes en móvil, contraste, botones y tipografías</li>
            <li>Una nota de 0 a 10 con desglose y fórmula pública</li>
          </ul>
        </div>
        <div className="card-dark">
          <span className="eyebrow">Fuera de alcance</span>
          <h2>Qué no puede analizar</h2>
          <ul className="check-list is-limit">
            <li>Visitas, ventas ni clientes que repiten de tu negocio</li>
            <li>La velocidad real de carga</li>
            <li>La calidad de tus textos o tu posición en Google</li>
            <li>El resto de páginas de tu sitio</li>
            <li>Dibujar la página si no hay Chrome, Edge o Chromium instalado</li>
          </ul>
        </div>
      </section>

      <section className="card-light section" aria-labelledby="faq-titulo">
        <div className="section-head">
          <span className="eyebrow">Dudas</span>
          <h2 id="faq-titulo">Preguntas frecuentes</h2>
        </div>
        {FAQ_PREVIEW.map(([question, answer]) => (
          <details key={question} className="faq">
            <summary>{question}</summary>
            <p>{answer}</p>
          </details>
        ))}
        <p className="faq-more">
          <Link to="/sobre">Ver todas las preguntas</Link>
        </p>
      </section>

      <section className="cta-band section" aria-labelledby="cta-titulo">
        <span className="eyebrow">Gratis · Sin registro</span>
        <h2 id="cta-titulo">Descubre qué ven los buscadores de tu web</h2>
        <p>
          {publicSite
            ? 'Descarga la app para Windows, macOS o Linux, escribe la dirección de tu página y recibe un informe claro.'
            : 'Escribe la dirección de tu página y recibe un informe claro en pocos minutos.'}
        </p>
        <Link to={primary.to} className="button">
          {primary.band}
        </Link>
        <p className="cta-note">
          El análisis es automático y orientativo. Seguir sus recomendaciones no garantiza ningún resultado en
          buscadores.
        </p>
      </section>
    </>
  )
}
