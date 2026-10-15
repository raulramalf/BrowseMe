package io.github.raulramalf.browseme.infrastructure.web;

import io.github.raulramalf.browseme.domain.seo.RuleId;
import java.util.EnumMap;
import java.util.Map;

/** Nombre legible de cada regla. Lo pone el backend, nunca la IA. */
final class RuleTitles {

    private static final Map<RuleId, String> TITLES = new EnumMap<>(RuleId.class);

    static {
        TITLES.put(RuleId.TITLE_MISSING, "Falta el título de la página");
        TITLES.put(RuleId.TITLE_LENGTH, "Longitud del título mejorable");
        TITLES.put(RuleId.META_DESC_MISSING, "Falta la metadescripción");
        TITLES.put(RuleId.META_DESC_LENGTH, "Longitud de la metadescripción mejorable");
        TITLES.put(RuleId.H1_MISSING, "No hay encabezado principal (H1)");
        TITLES.put(RuleId.H1_MULTIPLE, "Hay varios encabezados H1");
        TITLES.put(RuleId.HEADING_SKIP, "Saltos en los niveles de encabezado");
        TITLES.put(RuleId.LANG_MISSING, "No se declara el idioma");
        TITLES.put(RuleId.CANONICAL_MISSING, "No se declara URL canónica");
        TITLES.put(RuleId.CANONICAL_DIFFERENT, "La URL canónica apunta a otra dirección");
        TITLES.put(RuleId.NOINDEX_DETECTED, "La página pide no aparecer en buscadores");
        TITLES.put(RuleId.ROBOTS_TXT_BLOCKS, "robots.txt bloquea esta página");
        TITLES.put(RuleId.NOT_HTTPS, "La página no usa HTTPS");
        TITLES.put(RuleId.VIEWPORT_MISSING, "Falta la configuración para móviles (viewport)");
        TITLES.put(RuleId.IMAGES_NO_ALT, "Imágenes sin texto alternativo");
        TITLES.put(RuleId.THIN_CONTENT, "Poco texto visible");
        TITLES.put(RuleId.REDIRECT_CHAIN, "Demasiadas redirecciones");
        TITLES.put(RuleId.FETCH_HTTP_ERROR, "La página responde con un error");
        TITLES.put(RuleId.CONTACT_MISSING, "No hay una forma directa de contacto");
        TITLES.put(RuleId.CTA_MISSING, "Falta una llamada a la acción");
        TITLES.put(RuleId.SUBSCRIBE_MISSING, "No se puede dejar el email para volver a contactar");
        TITLES.put(RuleId.SOCIAL_PROOF_MISSING, "No se muestran opiniones de clientes");
        TITLES.put(RuleId.SOCIAL_LINKS_MISSING, "No hay enlaces a redes sociales");
        TITLES.put(RuleId.BUSINESS_DATA_MISSING, "Faltan los datos estructurados del negocio");
        TITLES.put(RuleId.SHARE_PREVIEW_MISSING, "La página no se ve bien al compartirla");
        TITLES.put(RuleId.NAV_MISSING, "No hay un menú de navegación marcado");
        TITLES.put(RuleId.GENERIC_LINK_TEXT, "Enlaces con texto genérico");
        TITLES.put(RuleId.EMPTY_INTERACTIVE, "Botones o enlaces sin nombre");
        TITLES.put(RuleId.FORM_LABELS_MISSING, "Campos de formulario sin etiqueta");
        TITLES.put(RuleId.IMAGES_NO_DIMENSIONS, "Imágenes sin tamaño reservado");
        TITLES.put(RuleId.FAVICON_MISSING, "Falta el icono del sitio (favicon)");
        TITLES.put(RuleId.MOBILE_HORIZONTAL_SCROLL, "En móvil la página se sale de la pantalla");
        TITLES.put(RuleId.LOW_CONTRAST_TEXT, "Textos con poco contraste");
        TITLES.put(RuleId.SMALL_TAP_TARGETS, "Botones y enlaces difíciles de pulsar en móvil");
        TITLES.put(RuleId.SMALL_TEXT_MOBILE, "Texto demasiado pequeño en móvil");
        TITLES.put(RuleId.CTA_NOT_VISIBLE_MOBILE, "La acción principal no se ve al entrar desde el móvil");
        TITLES.put(RuleId.TOO_MANY_FONTS, "Demasiadas tipografías distintas");
        if (TITLES.size() != RuleId.values().length) {
            throw new IllegalStateException("Falta el título de alguna regla");
        }
    }

    private RuleTitles() {
    }

    static String of(RuleId id) {
        return TITLES.get(id);
    }
}
