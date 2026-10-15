import { useDocumentTitle } from './useDocumentTitle'

const FAQ: [string, string][] = [
  ['¿Cuánto cuesta?', 'Nada. BrowseMe es un proyecto open source y gratuito.'],
  ['¿Guardáis mi web o mis datos?', 'Guardamos la dirección analizada y el resultado durante 7 días para que puedas consultar el informe; después se borran automáticamente. No guardamos el contenido completo de tu página ni usamos cookies de seguimiento.'],
  ['¿La IA ve mi web?', 'La IA recibe solo un resumen breve de lo detectado (por ejemplo, el título y los hallazgos), nunca la página completa. Funciona en nuestro propio servidor y no envía datos a terceros.'],
  ['¿Por qué no me da una nota del 0 al 100?', 'Porque una puntuación global sería arbitraria. Preferimos mostrar cada hallazgo con su evidencia y su importancia.'],
  ['¿Garantiza que saldré primero en Google?', 'No. Revisa aspectos técnicos básicos; el posicionamiento depende de muchos factores que este análisis no puede ver.'],
  ['¿Por qué falla con mi web?', 'Algunas webs bloquean descargas automáticas, tardan demasiado o generan todo el contenido con JavaScript. El informe indica el motivo cuando lo conoce.'],
]

export function AboutPage() {
  useDocumentTitle('Sobre el proyecto')
  return (
    <article className="prose">
      <h1>Sobre BrowseMe</h1>
      <p>
        BrowseMe ayuda a pequeñas empresas, autónomos y profesionales a entender cómo ven los buscadores su página web,
        sin tecnicismos.
      </p>
      <h2>Cómo funciona</h2>
      <p>
        Primero, unas reglas automáticas y deterministas comprueban la página: los resultados son hechos verificables,
        cada uno con su evidencia. Después, un modelo de IA local redacta una explicación y un plan de acción a partir
        de esos hechos. La IA no detecta problemas por su cuenta y su texto se marca siempre como interpretación.
      </p>
      <h2>Limitaciones</h2>
      <p>
        Solo se analiza una página y sin ejecutar JavaScript. No se mide la velocidad real, la experiencia en móviles,
        la calidad del contenido ni el posicionamiento.
      </p>
      <h2>Preguntas frecuentes</h2>
      {FAQ.map(([question, answer]) => (
        <details key={question} className="faq">
          <summary>{question}</summary>
          <p>{answer}</p>
        </details>
      ))}
      <h2>Contacto</h2>
      <p>Para dudas o sugerencias: [COMPLETAR: email de contacto].</p>
      <p>
        El código fuente está publicado bajo licencia Apache 2.0: [COMPLETAR: URL del repositorio].
      </p>
    </article>
  )
}
