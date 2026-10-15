/** Preguntas frecuentes compartidas por la portada y «Sobre el proyecto». */
export const FAQ: [string, string][] = [
  ['¿Cuánto cuesta?', 'Nada. BrowseMe es un proyecto open source y gratuito.'],
  ['¿Necesito instalar la IA?', 'No es obligatorio. Sin Ollama, BrowseMe muestra todos los hallazgos verificados por reglas; con Ollama añade además una explicación y un plan de acción.'],
  ['¿Guardáis mi web o mis datos?', 'No. BrowseMe se ejecuta en tu ordenador: la dirección analizada y el informe se quedan en él, solo mientras la app está abierta. No recibimos ningún dato ni usamos cookies de seguimiento.'],
  ['¿La IA ve mi web?', 'La IA recibe solo un resumen breve de lo detectado (por ejemplo, el título y los hallazgos), nunca la página completa. Funciona en tu propio ordenador con Ollama y no envía datos a terceros.'],
  ['¿Cómo se calcula la nota de 0 a 10?', 'Con una fórmula fija y pública, sin IA: cada área (SEO, captación y fidelización, y diseño) empieza en 10 y cada hallazgo resta según su importancia; la nota final es la media ponderada. La misma página recibe siempre la misma nota, y el informe te muestra el desglose y cómo se calcula.'],
  ['¿Analiza la retención de clientes?', 'Solo lo que se ve en la página: si hay teléfono, email o WhatsApp, llamadas a la acción, suscripción, opiniones, redes sociales y datos del negocio. No ve visitas, ventas ni clientes que repiten, así que son pistas para mejorar, no una medición.'],
  ['¿Cómo valora el diseño?', 'Combina patrones del código (menú, formularios con etiquetas, enlaces claros, imágenes con tamaño) con la página dibujada en un navegador real del equipo (Chrome, Edge o Chromium): desbordes en móvil, contraste, tamaño de botones y texto, y tipografías.'],
  ['¿Garantiza que saldré primero en Google?', 'No. Revisa aspectos técnicos básicos; el posicionamiento depende de muchos factores que este análisis no puede ver.'],
  ['¿Por qué falla con mi web?', 'Algunas webs bloquean descargas automáticas, tardan demasiado o generan todo el contenido con JavaScript. El informe indica el motivo cuando lo conoce.'],
]
