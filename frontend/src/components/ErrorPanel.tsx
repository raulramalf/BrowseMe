import { Link } from 'react-router'

/** Errores 404/500/503 como componente dentro de la vista, no como ruta. */
export function ErrorPanel({ title, message, retry }: { title: string; message: string; retry?: boolean }) {
  return (
    <section className="panel-dark panel-error" role="alert" aria-labelledby="error-titulo">
      <h1 id="error-titulo">{title}</h1>
      <p>{message}</p>
      {retry !== false && (
        <p>
          <Link to="/analizar">Volver a analizar una web</Link>
        </p>
      )}
    </section>
  )
}
