package io.github.raulramalf.browseme.domain.page;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lo que se extrajo de una página analizada. Inmutable y sin HTML crudo: todos
 * los textos vienen de una fuente no confiable y se guardan acotados.
 *
 * <p>Los campos de texto pueden ser {@code null} cuando el elemento no existe en
 * la página, y cadena vacía cuando existe pero está vacío. Las reglas SEO
 * necesitan distinguir ambos casos.
 *
 * @param canonical         valor literal del {@code href} canónico, sin interpretar
 * @param robotsDirectives  directivas de {@code meta robots} y {@code X-Robots-Tag}, en minúsculas
 */
public record PageSnapshot(
        URI requestedUrl,
        URI finalUrl,
        int httpStatus,
        int redirectCount,
        String title,
        String metaDescription,
        String lang,
        String canonical,
        Set<String> robotsDirectives,
        List<Heading> headings,
        LinkStats links,
        ImageStats images,
        boolean viewportPresent,
        int visibleWordCount,
        RobotsTxtStatus robotsTxt) {

    public static final int MAX_TITLE_LENGTH = 300;
    public static final int MAX_META_DESCRIPTION_LENGTH = 500;
    public static final int MAX_LANG_LENGTH = 35;
    public static final int MAX_CANONICAL_LENGTH = 2048;
    public static final int MAX_HEADINGS = 200;
    public static final int MAX_ROBOTS_DIRECTIVES = 20;

    public PageSnapshot {
        Objects.requireNonNull(requestedUrl, "requestedUrl");
        Objects.requireNonNull(finalUrl, "finalUrl");
        Objects.requireNonNull(links, "links");
        Objects.requireNonNull(images, "images");
        Objects.requireNonNull(robotsTxt, "robotsTxt");
        if (httpStatus < 100 || httpStatus > 599) {
            throw new IllegalArgumentException("Código de estado HTTP no válido: " + httpStatus);
        }
        if (redirectCount < 0 || visibleWordCount < 0) {
            throw new IllegalArgumentException("Los recuentos no pueden ser negativos");
        }
        title = Texts.truncate(title, MAX_TITLE_LENGTH);
        metaDescription = Texts.truncate(metaDescription, MAX_META_DESCRIPTION_LENGTH);
        lang = Texts.truncate(lang, MAX_LANG_LENGTH);
        canonical = Texts.truncate(canonical, MAX_CANONICAL_LENGTH);
        robotsDirectives = robotsDirectives == null ? Set.of() : robotsDirectives.stream()
                .filter(Objects::nonNull)
                .map(d -> d.strip().toLowerCase(Locale.ROOT))
                .filter(d -> !d.isEmpty())
                .limit(MAX_ROBOTS_DIRECTIVES)
                .collect(Collectors.toUnmodifiableSet());
        headings = headings == null ? List.of() : List.copyOf(headings.subList(0, Math.min(headings.size(), MAX_HEADINGS)));
    }

    /** Si la URL final, tras redirecciones, se sirve por HTTPS. */
    public boolean https() {
        return "https".equalsIgnoreCase(finalUrl.getScheme());
    }

    public boolean hasRobotsDirective(String directive) {
        return robotsDirectives.contains(directive.toLowerCase(Locale.ROOT));
    }

    public long headingCount(int level) {
        return headings.stream().filter(h -> h.level() == level).count();
    }
}
