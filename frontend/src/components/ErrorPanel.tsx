import { Link } from 'react-router'

/** Errores 404/500/503 como componente dentro de la vista, no como ruta. */
export function ErrorPanel({ title, message, retry }: { title: string; message: string; retry?: boolean }) {
  return (
    <section className="panel panel-error" role="alert" aria-labelledby="error-titulo">
      <h1 id="error-titulo">{title}</h1>
      <p>{message}</p>
      {retry !== false && (
        <p>
          <Link to="/">Volver a analizar una web</Link>
        </p>
      )}
    </section>
  )
}
