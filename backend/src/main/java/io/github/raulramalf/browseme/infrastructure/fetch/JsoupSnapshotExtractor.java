package io.github.raulramalf.browseme.infrastructure.fetch;

import io.github.raulramalf.browseme.domain.page.DesignSignals;
import io.github.raulramalf.browseme.domain.page.EngagementSignals;
import io.github.raulramalf.browseme.domain.page.Heading;
import io.github.raulramalf.browseme.domain.page.ImageStats;
import io.github.raulramalf.browseme.domain.page.LinkStats;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.page.RobotsTxtStatus;
import io.github.raulramalf.browseme.domain.page.VisualCheck;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Convierte el HTML descargado en un {@link PageSnapshot}. Solo parsea: Jsoup
 * nunca descarga nada aquí ({@code Jsoup.connect()} está prohibido porque
 * resolvería DNS por su cuenta, sin la política anti-SSRF).
 */
final class JsoupSnapshotExtractor {

    private static final Set<String> NON_NAVIGABLE_SCHEMES = Set.of("javascript", "mailto", "tel", "data");
    private static final String GOOGLEBOT_PREFIX = "googlebot:";

    private final SignalsExtractor signals = new SignalsExtractor();

    /** Datos de la respuesta HTTP que el extractor necesita además del HTML. */
    record ResponseInfo(URI requestedUrl, URI finalUrl, int httpStatus, int redirectCount,
                        List<String> xRobotsTagHeaders, RobotsTxtStatus robotsTxt) {
    }

    PageSnapshot extract(byte[] html, String charset, ResponseInfo info) {
        Document document;
        try {
            // charset null: Jsoup lo detecta por BOM o <meta charset>, con UTF-8 por defecto.
            document = Jsoup.parse(new ByteArrayInputStream(html), charset, info.finalUrl().toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Set<String> robots = new HashSet<>();
        for (Element meta : document.select("meta[name=robots], meta[name=googlebot]")) {
            addDirectives(meta.attr("content"), robots);
        }
        for (String header : info.xRobotsTagHeaders()) {
            addHeaderDirectives(header, robots);
        }

        return new PageSnapshot(
                info.requestedUrl(),
                info.finalUrl(),
                info.httpStatus(),
                info.redirectCount(),
                title(document),
                attributeOfFirst(document, "meta[name=description]", "content"),
                attributeOfFirst(document, "html[lang]", "lang"),
                canonical(document),
                robots,
                headings(document),
                links(document, info.finalUrl()),
                images(document),
                document.selectFirst("meta[name=viewport]") != null,
                visibleWordCount(document),
                info.robotsTxt(),
                signals.engagement(document),
                signals.design(document),
                // El análisis visual se hace después, con un navegador real.
                VisualCheck.notRun());
    }

    /** Snapshot mínimo para respuestas 4xx/5xx: no se analiza el cuerpo de una página de error. */
    PageSnapshot errorSnapshot(ResponseInfo info) {
        return new PageSnapshot(info.requestedUrl(), info.finalUrl(), info.httpStatus(), info.redirectCount(),
                null, null, null, null, Set.of(), List.of(), new LinkStats(0, 0), ImageStats.none(), false, 0,
                info.robotsTxt(), EngagementSignals.none(), DesignSignals.none(), VisualCheck.notRun());
    }

    private static String title(Document document) {
        Element title = document.selectFirst("title");
        return title == null ? null : title.text();
    }

    private static String attributeOfFirst(Document document, String selector, String attribute) {
        Element element = document.selectFirst(selector);
        return element == null ? null : element.attr(attribute);
    }

    private static String canonical(Document document) {
        for (Element link : document.select("link[rel][href]")) {
            for (String rel : link.attr("rel").toLowerCase(Locale.ROOT).split("\\s+")) {
                if (rel.equals("canonical")) {
                    String absolute = link.absUrl("href");
                    return absolute.isEmpty() ? link.attr("href") : absolute;
                }
            }
        }
        return null;
    }

    private static List<Heading> headings(Document document) {
        List<Heading> headings = new ArrayList<>();
        for (Element heading : document.select("h1, h2, h3, h4, h5, h6")) {
            if (headings.size() >= PageSnapshot.MAX_HEADINGS) {
                break;
            }
            headings.add(new Heading(heading.tagName().charAt(1) - '0', heading.text()));
        }
        return headings;
    }

    private static LinkStats links(Document document, URI finalUrl) {
        String pageHost = withoutWww(finalUrl.getHost());
        int internal = 0;
        int external = 0;
        for (Element anchor : document.select("a[href]")) {
            String href = anchor.attr("href").strip();
            if (href.isEmpty() || href.startsWith("#")) {
                continue;
            }
            URI target;
            try {
                target = URI.create(anchor.absUrl("href"));
            } catch (IllegalArgumentException e) {
                continue;
            }
            String scheme = target.getScheme() == null ? "" : target.getScheme().toLowerCase(Locale.ROOT);
            if (NON_NAVIGABLE_SCHEMES.contains(scheme) || target.getHost() == null) {
                continue;
            }
            if (withoutWww(target.getHost()).equalsIgnoreCase(pageHost)) {
                internal++;
            } else {
                external++;
            }
        }
        return new LinkStats(internal, external);
    }

    private static ImageStats images(Document document) {
        int withAlt = 0;
        int decorative = 0;
        int missing = 0;
        for (Element image : document.select("img")) {
            if (!image.hasAttr("alt")) {
                missing++;
            } else if (image.attr("alt").isBlank()) {
                decorative++;
            } else {
                withAlt++;
            }
        }
        return new ImageStats(withAlt + decorative + missing, withAlt, decorative, missing);
    }

    /** Palabras del texto visible aproximado: sin scripts, estilos ni plantillas. */
    private static int visibleWordCount(Document document) {
        Element body = document.body();
        if (body == null) {
            return 0;
        }
        Element copy = body.clone();
        copy.select("script, style, noscript, template, svg, [hidden]").remove();
        int words = 0;
        for (String token : copy.text().split("\\s+")) {
            if (token.codePoints().anyMatch(Character::isLetterOrDigit)) {
                words++;
            }
        }
        return words;
    }

    private static void addDirectives(String content, Set<String> target) {
        for (String directive : content.split(",")) {
            String normalized = directive.strip().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    /**
     * X-Robots-Tag puede llevar un agente delante ({@code googlebot: noindex}).
     * Se aceptan las directivas generales y las dirigidas a Googlebot.
     */
    private static void addHeaderDirectives(String header, Set<String> target) {
        String value = header.strip();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith(GOOGLEBOT_PREFIX)) {
            addDirectives(value.substring(GOOGLEBOT_PREFIX.length()), target);
            return;
        }
        int colon = value.indexOf(':');
        boolean agentPrefixed = colon > 0 && !lower.startsWith("unavailable_after")
                && !lower.substring(0, colon).contains(",") && !lower.substring(0, colon).startsWith("max-");
        if (!agentPrefixed) {
            addDirectives(value, target);
        }
    }

    private static String withoutWww(String host) {
        if (host == null) {
            return "";
        }
        String lower = host.toLowerCase(Locale.ROOT);
        return lower.startsWith("www.") ? lower.substring(4) : lower;
    }
}
