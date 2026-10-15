import type { ReactNode } from 'react'
import { Link, NavLink } from 'react-router'

export function Layout({ children }: { children: ReactNode }) {
  return (
    <>
      <a className="skip-link" href="#contenido">
        Saltar al contenido
      </a>
      <header className="site-header">
        <div className="container header-inner">
          <Link to="/" className="brand">
            BrowseMe
          </Link>
          <nav aria-label="Principal">
            <ul className="nav-list">
              <li>
                <NavLink to="/" end>
                  Analizar
                </NavLink>
              </li>
              <li>
                <NavLink to="/sobre">Sobre el proyecto</NavLink>
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
          <p>BrowseMe es un proyecto open source. El análisis es automático y no garantiza resultados SEO.</p>
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
