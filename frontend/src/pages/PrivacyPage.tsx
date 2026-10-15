import { LegalNotice } from './LegalNotice'
import { useDocumentTitle } from './useDocumentTitle'

export function PrivacyPage() {
  useDocumentTitle('Privacidad')
  return (
    <article className="prose">
      <h1>Política de privacidad</h1>
      <LegalNotice />
      <h2>Responsable</h2>
      <p>[COMPLETAR: nombre o razón social del responsable], [COMPLETAR: NIF], [COMPLETAR: domicilio], contacto: [COMPLETAR: email].</p>
      <h2>Qué datos tratamos</h2>
      <ul>
        <li>La dirección web que envías y la dirección final tras las redirecciones.</li>
        <li>El resultado del análisis: datos extraídos de esa página pública (título, encabezados, recuentos…), los hallazgos y, si está disponible, la interpretación de la IA. No guardamos el contenido completo de la página.</li>
        <li>Tu dirección IP, solo en memoria y durante unos minutos para limitar abusos (límite de peticiones). No se guarda en base de datos.</li>
        <li>Registros técnicos del servidor, sin el contenido de las páginas analizadas ni direcciones IP.</li>
      </ul>
      <p>No hay cuentas de usuario, analítica ni publicidad.</p>
      <h2>Para qué y durante cuánto tiempo</h2>
      <p>
        Para realizar el análisis y mostrarte el informe. Los análisis se borran automáticamente a los 7 días. La IP de
        control de abusos desaparece de la memoria del servidor en minutos o al reiniciarse.
      </p>
      <h2>Base jurídica</h2>
      <p>[COMPLETAR: base jurídica del tratamiento, a determinar con asesoramiento profesional].</p>
      <h2>Destinatarios</h2>
      <p>
        La interpretación con IA se realiza con un modelo que funciona en nuestro propio servidor: no se envían datos a
        terceros para ello. Para hacer el análisis, nuestro servidor se conecta a la web que indicas.
        [COMPLETAR: proveedor de alojamiento, si aplica].
      </p>
      <h2>Tus derechos</h2>
      <p>[COMPLETAR: derechos de las personas interesadas y cómo ejercerlos, incluida la reclamación ante la autoridad de control].</p>
    </article>
  )
}
