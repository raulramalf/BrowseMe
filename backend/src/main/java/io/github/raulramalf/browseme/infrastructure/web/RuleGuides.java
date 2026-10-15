package io.github.raulramalf.browseme.infrastructure.web;

import io.github.raulramalf.browseme.domain.seo.RuleId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Guía de mejora de cada regla: por qué importa y qué hacer, en pasos concretos.
 * Es conocimiento general y fijo, escrito aquí y no generado: no describe lo que
 * hay en la página (eso lo hacen las reglas), sino cómo corregirlo.
 *
 * <p>{@code clients} marca las reglas que influyen directamente en que lleguen
 * clientes (que te encuentren, confíen, te contacten o compren); alimentan la
 * sección «Cómo conseguir más clientes» del informe.
 */
final class RuleGuides {

    record Guide(String why, List<String> steps, boolean clients) {
    }

    private static final Map<RuleId, Guide> GUIDES = new EnumMap<>(RuleId.class);

    private static void add(RuleId id, boolean clients, String why, String... steps) {
        GUIDES.put(id, new Guide(why, List.of(steps), clients));
    }

    static {
        // --- SEO ---------------------------------------------------------------------------
        add(RuleId.TITLE_MISSING, true,
                "El título es lo primero que ve un cliente en Google y en la pestaña del navegador.",
                "Añade un <title> único con el nombre del negocio y lo que haces, por ejemplo «Panadería Luna · Pan artesano en Valencia».",
                "Mantenlo entre 30 y 65 caracteres para que Google no lo corte.");
        add(RuleId.TITLE_LENGTH, false,
                "Un título demasiado corto desaprovecha espacio y uno muy largo se corta en los resultados.",
                "Resume en 30-65 caracteres qué ofreces y dónde.",
                "Pon primero lo más importante: es lo que más se lee.");
        add(RuleId.META_DESC_MISSING, true,
                "Es el texto que aparece bajo tu enlace en Google; una buena descripción anima a hacer clic.",
                "Escribe 1 o 2 frases (70-160 caracteres) con lo que ofreces y una invitación a actuar.",
                "Usa una descripción distinta en cada página.");
        add(RuleId.META_DESC_LENGTH, false,
                "Si es muy corta no convence y si es larga Google la recorta.",
                "Ajústala a 70-160 caracteres.",
                "Termina con una acción: «Pide presupuesto», «Reserva tu mesa».");
        add(RuleId.H1_MISSING, false,
                "El titular principal ayuda a buscadores y visitantes a entender de qué trata la página en un segundo.",
                "Añade un único titular H1 que diga qué haces y para quién.",
                "Usa H2 y H3 para ordenar el resto de secciones.");
        add(RuleId.H1_MULTIPLE, false,
                "Varios titulares principales confunden sobre cuál es el tema de la página.",
                "Deja un único H1 y convierte los demás en H2.");
        add(RuleId.HEADING_SKIP, false,
                "Saltarse niveles (de H2 a H4) desordena la estructura para lectores de pantalla y buscadores.",
                "Baja un nivel cada vez: H1, H2, H3…",
                "Elige el nivel por la estructura, no por el tamaño de letra.");
        add(RuleId.LANG_MISSING, false,
                "Sin idioma declarado, navegadores y buscadores pueden tratar mal el texto o ofrecer traducciones innecesarias.",
                "Añade lang=\"es\" (o el idioma que corresponda) en la etiqueta <html>.");
        add(RuleId.CANONICAL_MISSING, false,
                "Si hay varias direcciones para la misma página, Google puede repartir su valor entre ellas.",
                "Añade <link rel=\"canonical\"> con la dirección preferida de cada página.");
        add(RuleId.CANONICAL_DIFFERENT, false,
                "La página le dice a Google que la versión «oficial» es otra dirección.",
                "Comprueba que la canónica apunta donde quieres; si es un error, corrígela.");
        add(RuleId.NOINDEX_DETECTED, true,
                "Con «noindex» la página no aparece en Google: nadie te encuentra buscando.",
                "Si quieres que te encuentren, elimina la directiva noindex de la página.",
                "Revisa también los ajustes de visibilidad de tu gestor de contenidos (WordPress, Wix…).");
        add(RuleId.ROBOTS_TXT_BLOCKS, true,
                "Si robots.txt bloquea la página, Google no la visita y no puede mostrarla.",
                "Revisa el archivo robots.txt y quita la regla Disallow que afecta a esta página.");
        add(RuleId.NOT_HTTPS, true,
                "Los navegadores marcan «No seguro» las webs sin HTTPS y muchos clientes se van antes de contactar o comprar.",
                "Pide a tu proveedor de alojamiento que active el certificado SSL (suele ser gratuito).",
                "Redirige todas las direcciones http:// a https://.");
        add(RuleId.VIEWPORT_MISSING, true,
                "Sin configuración para móviles, la página se ve diminuta en el teléfono, donde entra la mayoría de clientes.",
                "Añade <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"> en la cabecera.");
        add(RuleId.IMAGES_NO_ALT, false,
                "El texto alternativo describe las imágenes a quien no las ve y a Google.",
                "Describe cada imagen informativa en su atributo alt.",
                "Pon alt=\"\" en las meramente decorativas.");
        add(RuleId.THIN_CONTENT, false,
                "Con muy poco texto, Google tiene poco que entender y el visitante poco que le convenza.",
                "Explica qué ofreces, para quién, dónde y por qué elegirte, con tus propias palabras.",
                "Añade preguntas frecuentes: responden dudas antes de que el cliente las plantee.");
        add(RuleId.REDIRECT_CHAIN, false,
                "Cada redirección retrasa la carga y puede perder visitantes.",
                "Haz que cada dirección antigua redirija directamente a la definitiva, en un solo salto.");
        add(RuleId.FETCH_HTTP_ERROR, true,
                "Si la página devuelve un error, ni los clientes ni Google pueden verla.",
                "Comprueba que la dirección es correcta y que el servidor responde.",
                "Si el error persiste, contacta con tu proveedor de alojamiento.");

        // --- Captación y fidelización --------------------------------------------------------
        add(RuleId.CONTACT_MISSING, true,
                "Si contactar cuesta esfuerzo, el cliente se va a la competencia: es el fallo que más clientes hace perder.",
                "Pon tu teléfono en la cabecera como enlace pulsable (tel:), para llamar con un toque desde el móvil.",
                "Añade un botón de WhatsApp: es el canal que más usan los clientes locales.",
                "Incluye un formulario corto (nombre, email y mensaje) o tu email visible.");
        add(RuleId.CTA_MISSING, true,
                "Un visitante que no sabe qué hacer a continuación, no hace nada.",
                "Elige UNA acción principal (reservar, pedir presupuesto, llamar) y ponla en un botón visible arriba.",
                "Usa verbos claros: «Reserva tu mesa», «Pide tu presupuesto gratis».",
                "Repite el botón al final de la página.");
        add(RuleId.SUBSCRIBE_MISSING, true,
                "Solo una pequeña parte de los visitantes compra a la primera; dejar su email permite volver a hablarles.",
                "Añade un formulario de suscripción con un motivo (descuento de bienvenida, novedades, consejos).",
                "Envía un correo breve de vez en cuando con una oferta o novedad útil.",
                "Cumple el RGPD: pide consentimiento claro y enlaza tu política de privacidad.");
        add(RuleId.SOCIAL_PROOF_MISSING, true,
                "Las opiniones de otros clientes son lo que más convence a un cliente nuevo.",
                "Pide reseñas a tus clientes satisfechos justo después de atenderlos.",
                "Muestra 3 o 4 opiniones reales con nombre en tu página, bajo un encabezado como «Lo que dicen nuestros clientes».",
                "Marca las valoraciones con datos estructurados (schema.org Review) para que Google pueda mostrar estrellas.");
        add(RuleId.SOCIAL_LINKS_MISSING, true,
                "Las redes mantienen el contacto con quien aún no ha comprado y dan confianza de que el negocio está activo.",
                "Enlaza solo las redes que uses de verdad (Instagram, Facebook…) en la cabecera o el pie.",
                "Publica con regularidad: un perfil abandonado resta confianza.");
        add(RuleId.BUSINESS_DATA_MISSING, true,
                "Con datos estructurados, Google puede mostrar tu dirección, horario y teléfono directamente en los resultados.",
                "Añade un bloque JSON-LD LocalBusiness con nombre, dirección, teléfono, horario y web.",
                "Completa también tu ficha de Google Business Profile: es gratuita y te lleva clientes de la zona.");
        add(RuleId.SHARE_PREVIEW_MISSING, true,
                "Cuando un cliente comparte tu enlace por WhatsApp o redes, una buena vista previa atrae más visitas.",
                "Añade las etiquetas Open Graph og:title y og:image (una imagen de 1200×630 px).",
                "Comprueba el resultado pegando tu enlace en una conversación de WhatsApp.");

        // --- Diseño: estructura del HTML ------------------------------------------------------
        add(RuleId.NAV_MISSING, false,
                "Un menú claro ayuda a encontrar lo que se busca; quien no lo encuentra se va.",
                "Agrupa el menú principal en un elemento <nav>.",
                "Limítalo a 5 o 7 opciones con nombres claros («Carta», «Contacto»).");
        add(RuleId.GENERIC_LINK_TEXT, false,
                "«Clic aquí» no dice adónde lleva: ni al visitante que ojea la página ni a Google.",
                "Sustituye los textos genéricos por la acción real: «Ver la carta», «Horarios y ubicación».");
        add(RuleId.EMPTY_INTERACTIVE, false,
                "Un botón o icono sin nombre es invisible para lectores de pantalla y puede confundir a todos.",
                "Añade un texto o un aria-label a cada botón o enlace con icono («Abrir menú», «Instagram»).");
        add(RuleId.FORM_LABELS_MISSING, true,
                "Un formulario difícil de entender se abandona: es una consulta que no llega.",
                "Asocia cada campo a un <label> visible que diga qué pide.",
                "No uses solo el placeholder: desaparece al escribir.",
                "Pide solo lo imprescindible: cuantos menos campos, más formularios enviados.");
        add(RuleId.IMAGES_NO_DIMENSIONS, false,
                "Si las imágenes no reservan su hueco, el contenido «salta» al cargar y se pulsa lo que no se quería.",
                "Indica width y height en cada imagen, o fija su proporción con CSS (aspect-ratio).");
        add(RuleId.FAVICON_MISSING, false,
                "El icono hace tu web reconocible en pestañas, favoritos y resultados.",
                "Añade un favicon con tu logotipo (<link rel=\"icon\">), en formato PNG o SVG.");

        // --- Diseño: página dibujada ----------------------------------------------------------
        add(RuleId.MOBILE_HORIZONTAL_SCROLL, true,
                "Si en el móvil hay que desplazarse a los lados, la página se siente rota y se abandona.",
                "Localiza el elemento más ancho que la pantalla (tabla, imagen o bloque de ancho fijo).",
                "Dale max-width: 100% y evita anchos fijos en píxeles.",
                "Pruébalo en el móvil, no solo en el ordenador.");
        add(RuleId.LOW_CONTRAST_TEXT, true,
                "El texto con poco contraste cansa y, a pleno sol en el móvil, no se lee.",
                "Oscurece el texto o aclara el fondo hasta una relación de contraste de 4,5:1.",
                "Evita el gris claro sobre blanco y el texto sobre fotos sin una capa oscura.");
        add(RuleId.SMALL_TAP_TARGETS, true,
                "Los botones diminutos se pulsan mal con el dedo y generan frustración.",
                "Haz los botones y enlaces sueltos de al menos 44×44 px.",
                "Sepáralos entre sí para no pulsar el de al lado.");
        add(RuleId.SMALL_TEXT_MOBILE, false,
                "El texto muy pequeño obliga a ampliar la pantalla.",
                "Usa 16 px para el texto principal y no bajes de 12 px en las notas.");
        add(RuleId.CTA_NOT_VISIBLE_MOBILE, true,
                "Muchos visitantes del móvil no bajan: si la acción principal no se ve al entrar, no la ven.",
                "Sube el botón principal (llamar, reservar, pedir) a la primera pantalla.",
                "Si hace falta, deja un botón fijo en la parte inferior del móvil.");
        add(RuleId.TOO_MANY_FONTS, false,
                "Muchas tipografías dan una imagen desordenada y ralentizan la página.",
                "Limítate a una o dos familias: una para títulos y otra para el texto.");

        if (GUIDES.size() != RuleId.values().length) {
            throw new IllegalStateException("Falta la guía de alguna regla");
        }
    }

    private RuleGuides() {
    }

    static Guide of(RuleId id) {
        return GUIDES.get(id);
    }
}
