import type { Visual } from '../api/types'

/** Capturas de la primera pantalla tomadas en el análisis visual (son imágenes, nunca HTML). */
export function Screenshots({ id, visual }: { id: string; visual: Visual }) {
  if (visual.status !== 'DONE' || (!visual.mobileScreenshot && !visual.desktopScreenshot)) {
    return null
  }
  const base = `/api/v1/analyses/${encodeURIComponent(id)}/screenshots`
  return (
    <section className="panel-dark shots" aria-labelledby="capturas-titulo">
      <span className="eyebrow">Análisis visual</span>
      <h2 id="capturas-titulo">Así se ve tu página al entrar</h2>
      <p className="shots-note">
        Primera pantalla en un móvil simulado de 390 px y en un escritorio de 1366 px, dibujada con un navegador real.
      </p>
      <div className="shots-grid">
        {visual.mobileScreenshot && (
          <figure className="shot shot-mobile">
            <img
              src={`${base}/mobile`}
              width={390}
              height={844}
              loading="lazy"
              alt="Captura de la primera pantalla de la página en un móvil"
            />
            <figcaption>Móvil</figcaption>
          </figure>
        )}
        {visual.desktopScreenshot && (
          <figure className="shot shot-desktop">
            <img
              src={`${base}/desktop`}
              width={1366}
              height={800}
              loading="lazy"
              alt="Captura de la primera pantalla de la página en un ordenador"
            />
            <figcaption>Escritorio</figcaption>
          </figure>
        )}
      </div>
    </section>
  )
}
