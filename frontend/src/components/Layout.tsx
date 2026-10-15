import type { ReactNode } from 'react'
import { Link, NavLink } from 'react-router'
import { isPublicSite } from '../site'

export function Layout({ children }: { children: ReactNode }) {
  return (
    <>
      <a className="skip-link" href="#contenido">
        Saltar al contenido
      </a>
      <header className="site-header">
        <div className="container header-inner">
          <Link to="/" className="brand">
            <svg className="brand-mark" viewBox="0 0 28 28" aria-hidden="true" focusable="false">
              <rect width="28" height="28" rx="8" fill="#0e1a19" />
              <circle cx="12.5" cy="12.5" r="5.5" fill="none" stroke="#ffffff" strokeWidth="2" />
              <path d="M16.6 16.6 21 21" stroke="#3ddc91" strokeWidth="2.4" strokeLinecap="round" />
            </svg>
            BrowseMe
          </Link>
          <nav aria-label="Principal">
            <ul className="nav-list">
              <li>
                <NavLink to="/" end>
                  Inicio
                </NavLink>
              </li>
              <li>
                <NavLink to="/sobre">Sobre el proyecto</NavLink>
              </li>
              <li>
                {isPublicSite() ? (
                  <NavLink to="/descargar" className="nav-cta">
                    Descargar
                  </NavLink>
                ) : (
                  <NavLink to="/analizar" className="nav-cta">
                    Analizar mi web
                  </NavLink>
                )}
              </li>
            </ul>
          </nav>
        </div>
      </header>
      <main id="contenido" className="container" tabIndex={-1}>
        {children}
      </main>
      <footer className="site-footer">
        <div className="container footer-inner">
          <p>
            BrowseMe es un proyecto open source. El análisis es automático y no garantiza resultados SEO.{' '}
            {!isPublicSite() && <span className="version">Versión {__APP_VERSION__}</span>}
          </p>
          <nav aria-label="Legal">
            <ul className="nav-list">
              <li>
                <Link to="/privacidad">Privacidad</Link>
              </li>
              <li>
                <Link to="/terminos">Términos</Link>
              </li>
              <li>
                <Link to="/cookies">Cookies</Link>
              </li>
            </ul>
          </nav>
        </div>
      </footer>
    </>
  )
}
