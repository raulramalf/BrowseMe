import { Link } from 'react-router'
import { downloadUrl, RELEASES_URL } from '../site'
import { useDocumentTitle } from './useDocumentTitle'

const PLATFORMS: { name: string; file: string; requirement: string; open: string }[] = [
  {
    name: 'Windows',
    file: 'BrowseMe-windows.zip',
    requirement: 'Windows 10 u 11 de 64 bits',
    open: 'Abre BrowseMe.exe. Se abre una ventana de consola: ciérrala para salir.',
  },
  {
    name: 'macOS',
    file: 'BrowseMe-macos.zip',
    requirement: 'Mac con Apple Silicon (M1 o posterior)',
    open: 'La primera vez, clic derecho en BrowseMe.app y «Abrir». Para salir, ciérrala desde el Dock.',
  },
  {
    name: 'Linux',
    file: 'BrowseMe-linux.zip',
    requirement: 'Linux de 64 bits (x86-64)',
    open: 'Ejecuta BrowseMe/bin/BrowseMe desde una terminal. Ctrl+C para salir.',
  },
]

export function DownloadPage() {
  useDocumentTitle('Descargar BrowseMe')
  return (
    <>
      <section className="download-head" aria-labelledby="descargar-titulo">
        <span className="eyebrow">Gratis · Open source · Sin registro</span>
        <h1 id="descargar-titulo">Descarga BrowseMe</h1>
        <p className="lead">
          BrowseMe funciona en tu ordenador: no necesitas cuenta ni enviamos tus datos a ningún sitio. La app incluye
          todo lo necesario para analizar; la explicación con IA es opcional y también es local.
        </p>
      </section>

      <ul className="download-grid" aria-label="Descargas por sistema operativo">
        {PLATFORMS.map((platform) => (
          <li key={platform.name} className="download-card">
            <span className="eyebrow">{platform.requirement}</span>
            <h2>{platform.name}</h2>
            <p>{platform.open}</p>
            <a className="button" href={downloadUrl(platform.file)}>
              Descargar para {platform.name}
            </a>
            <span className="download-file">{platform.file}</span>
          </li>
        ))}
      </ul>

      <section className="card-light section" aria-labelledby="pasos-titulo">
        <div className="section-head">
          <span className="eyebrow">Instalación</span>
          <h2 id="pasos-titulo">Empieza en tres pasos</h2>
          <p>No hace falta instalar Java ni nada más: viene incluido.</p>
        </div>
        <ol className="steps-grid">
          <li className="step-card">
            <span className="step-number">1</span>
            <h3>Descarga y descomprime</h3>
            <p>Elige tu sistema, descarga el archivo .zip y descomprímelo en cualquier carpeta.</p>
          </li>
          <li className="step-card">
            <span className="step-number">2</span>
            <h3>IA local (opcional)</h3>
            <p>
              Instala <a href="https://ollama.com/download">Ollama</a> y descarga el modelo con{' '}
              <code className="cmd">ollama pull llama3.2:3b</code>. Sin Ollama, verás igualmente todos los hallazgos
              verificados.
            </p>
          </li>
          <li className="step-card">
            <span className="step-number">3</span>
            <h3>Abre BrowseMe</h3>
            <p>
              Se abrirá tu navegador en <code className="cmd">http://127.0.0.1:8765</code>. Si no se abre solo, escribe
              esa dirección.
            </p>
          </li>
        </ol>
      </section>

      <section className="two-columns" aria-label="Detalles de la app">
        <div className="card-dark">
          <span className="eyebrow">Privacidad</span>
          <h2>Todo se queda en tu equipo</h2>
          <ul className="check-list">
            <li>Solo se conecta a la web que pides analizar</li>
            <li>La IA funciona en tu ordenador con Ollama</li>
            <li>Los informes se guardan mientras la app está abierta</li>
            <li>Solo es accesible desde tu propio ordenador</li>
          </ul>
        </div>
        <div className="card-dark">
          <span className="eyebrow">Antes de empezar</span>
          <h2>Bueno saber</h2>
          <ul className="check-list is-limit">
            <li>La app no está firmada: Windows o macOS pueden avisar la primera vez</li>
            <li>Para la IA se recomiendan 8 GB de RAM</li>
            <li>La IA en un ordenador sin tarjeta gráfica puede tardar un minuto</li>
            <li>
              <Link to="/sobre">Preguntas frecuentes</Link> · <a href={RELEASES_URL}>Todas las versiones</a>
            </li>
          </ul>
        </div>
      </section>
    </>
  )
}
