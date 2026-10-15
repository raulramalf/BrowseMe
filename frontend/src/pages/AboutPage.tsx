import { FAQ } from './faq'
import { useDocumentTitle } from './useDocumentTitle'

export function AboutPage() {
  useDocumentTitle('Sobre el proyecto')
  return (
    <article className="prose">
      <h1>Sobre BrowseMe</h1>
      <p>
        BrowseMe ayuda a pequeñas empresas, autónomos y profesionales a entender cómo ven los buscadores y los clientes
        su página web, sin tecnicismos: una valoración de 0 a 10, qué mejorar para captar y fidelizar clientes y qué
        patrones de diseño corregir.
      </p>
      <h2>Cómo funciona</h2>
      <p>
        Primero, unas reglas automáticas y deterministas comprueban la página: los resultados son hechos verificables,
        cada uno con su evidencia. Después, un modelo de IA local redacta una explicación y un plan de acción a partir
        de esos hechos. La IA no detecta problemas por su cuenta y su texto se marca siempre como interpretación.
      </p>
      <h2>Limitaciones</h2>
      <p>
        Solo se analiza una página, una vez. No se mide la velocidad real, la calidad del contenido ni el
        posicionamiento, y no se ven visitas, ventas ni clientes que repiten: la captación y la fidelización se
        valoran por las señales que hay en la página. La nota es orientativa.
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
