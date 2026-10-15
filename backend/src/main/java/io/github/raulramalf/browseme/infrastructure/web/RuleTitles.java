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
