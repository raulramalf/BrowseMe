package io.github.raulramalf.browseme.infrastructure.fetch;

import io.github.raulramalf.browseme.domain.page.DesignSignals;
import io.github.raulramalf.browseme.domain.page.EngagementSignals;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Señales de captación y de diseño a partir del HTML ya parseado. Todo el
 * contenido es dato no confiable: solo se cuentan elementos y se reconocen
 * valores de listas cerradas; no se ejecuta ni se sigue nada.
 */
final class SignalsExtractor {

    private static final Map<String, String> SOCIAL_DOMAINS = Map.of(
            "facebook.com", "facebook", "instagram.com", "instagram", "twitter.com", "x", "x.com", "x",
            "linkedin.com", "linkedin", "tiktok.com", "tiktok", "youtube.com", "youtube", "youtu.be", "youtube",
            "pinterest.com", "pinterest", "pinterest.es", "pinterest");
    private static final Set<String> WHATSAPP_HOSTS = Set.of("wa.me", "api.whatsapp.com", "web.whatsapp.com",
            "chat.whatsapp.com");
    private static final Set<String> GENERIC_LINK_TEXTS = Set.of("clic aqui", "click aqui", "haz clic aqui",
            "haz click aqui", "pulsa aqui", "aqui", "leer mas", "ver mas", "mas", "mas info", "mas informacion",
            "saber mas", "click here", "here", "read more", "more", "learn more", "info");
    private static final Set<String> NON_FIELD_INPUTS = Set.of("hidden", "submit", "button", "reset", "image");
    private static final Pattern TESTIMONIALS = Pattern.compile(
            "\\b(?:opinion\\w*|resena\\w*|testimoni\\w*|valoracion\\w*|reviews?|testimonials?|lo que dicen)\\b");
    private static final Pattern EMAIL_NAME = Pattern.compile("(?i)e-?mail|correo");

    private static final int MAX_JSON_LD_SCRIPTS = 20;
    private static final int MAX_JSON_LD_LENGTH = 100_000;
    private static final int MAX_JSON_DEPTH = 10;

    private final JsonMapper json = JsonMapper.builder().build();

    EngagementSignals engagement(Document document) {
        int phone = 0;
        int email = 0;
        int whatsapp = 0;
        Set<String> social = new HashSet<>();
        for (Element anchor : document.select("a[href]")) {
            String href = anchor.attr("href").strip().toLowerCase(Locale.ROOT);
            if (href.startsWith("tel:")) {
                phone++;
            } else if (href.startsWith("mailto:")) {
                email++;
            } else if (href.startsWith("whatsapp:")) {
                whatsapp++;
            } else {
                String host = host(anchor.absUrl("href"));
                if (WHATSAPP_HOSTS.contains(host)) {
                    whatsapp++;
                }
                SOCIAL_DOMAINS.forEach((domain, name) -> {
                    if (host.equals(domain) || host.endsWith("." + domain)) {
                        social.add(name);
                    }
                });
            }
        }
        int forms = 0;
        int emailForms = 0;
        for (Element form : document.select("form")) {
            forms++;
            boolean hasEmail = !form.select("input[type=email], input[autocomplete=email]").isEmpty()
                    || form.select("input[name]").stream().anyMatch(i -> EMAIL_NAME.matcher(i.attr("name")).find());
            if (hasEmail) {
                emailForms++;
            }
        }
        int ctas = 0;
        for (Element element : document.select("a[href], button, [role=button], input[type=submit], input[type=button]")) {
            String text = element.tagName().equals("input") ? element.attr("value") : element.text();
            if (CallToActionVocabulary.matches(text) || CallToActionVocabulary.matches(element.attr("aria-label"))) {
                ctas++;
            }
        }
        boolean testimonials = document.select("h1, h2, h3, h4").stream()
                .anyMatch(h -> TESTIMONIALS.matcher(CallToActionVocabulary.normalize(h.text())).find());
        return new EngagementSignals(phone, email, whatsapp, forms, emailForms, ctas, social,
                schemaTypes(document), testimonials,
                hasContent(document, "meta[property=og:title]"), hasContent(document, "meta[property=og:image]"));
    }

    DesignSignals design(Document document) {
        boolean navigation = document.selectFirst("nav, [role=navigation]") != null;
        boolean favicon = document.select("link[rel]").stream().anyMatch(link -> {
            List<String> rels = List.of(link.attr("rel").toLowerCase(Locale.ROOT).split("\\s+"));
            return rels.contains("icon") || rels.contains("apple-touch-icon");
        });

        int generic = 0;
        for (Element anchor : document.select("a[href]")) {
            String text = CallToActionVocabulary.normalize(anchor.text()).replaceAll("[.…:!>»›]+$", "").strip();
            if (GENERIC_LINK_TEXTS.contains(text)) {
                generic++;
            }
        }

        int empty = 0;
        for (Element element : document.select("a[href], button")) {
            if (element.hasAttr("hidden") || "true".equalsIgnoreCase(element.attr("aria-hidden"))) {
                continue;
            }
            if (element.text().isBlank() && !hasAttrText(element, "aria-label", "aria-labelledby", "title")
                    && element.select("img[alt]").stream().allMatch(img -> img.attr("alt").isBlank())
                    && element.select("svg title").stream().allMatch(t -> t.text().isBlank())) {
                empty++;
            }
        }

        Set<String> labelledIds = new HashSet<>();
        for (Element label : document.select("label[for]")) {
            labelledIds.add(label.attr("for"));
        }
        int fields = 0;
        int unlabeled = 0;
        for (Element field : document.select("input, select, textarea")) {
            if (field.tagName().equals("input")
                    && NON_FIELD_INPUTS.contains(field.attr("type").strip().toLowerCase(Locale.ROOT))) {
                continue;
            }
            fields++;
            boolean labelled = (!field.id().isEmpty() && labelledIds.contains(field.id()))
                    || field.closest("label") != null
                    || hasAttrText(field, "aria-label", "aria-labelledby", "title");
            if (!labelled) {
                unlabeled++;
            }
        }

        int withoutDimensions = 0;
        for (Element image : document.select("img")) {
            if (!image.hasAttr("width") || !image.hasAttr("height")) {
                withoutDimensions++;
            }
        }
        return new DesignSignals(navigation, favicon, generic, empty, fields, unlabeled, withoutDimensions);
    }

    /** Tipos de schema.org de JSON-LD y microdatos, sin el prefijo de la URL. */
    private Set<String> schemaTypes(Document document) {
        Set<String> types = new LinkedHashSet<>();
        int scripts = 0;
        for (Element script : document.select("script[type=application/ld+json]")) {
            if (++scripts > MAX_JSON_LD_SCRIPTS) {
                break;
            }
            String content = script.data();
            if (content.length() > MAX_JSON_LD_LENGTH) {
                continue;
            }
            try {
                collectTypes(json.readTree(content), types, 0);
            } catch (JacksonException e) {
                // JSON-LD mal formado: se ignora, como haría un buscador.
            }
        }
        for (Element item : document.select("[itemtype]")) {
            for (String type : item.attr("itemtype").strip().split("\\s+")) {
                addType(type, types);
            }
        }
        return types;
    }

    private static void collectTypes(JsonNode node, Set<String> types, int depth) {
        if (node == null || depth > MAX_JSON_DEPTH || types.size() >= EngagementSignals.MAX_SCHEMA_TYPES) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectTypes(child, types, depth + 1);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode type = node.get("@type");
        if (type != null && type.isString()) {
            addType(type.asString(), types);
        } else if (type != null && type.isArray()) {
            for (JsonNode value : type) {
                if (value.isString()) {
                    addType(value.asString(), types);
                }
            }
        }
        for (JsonNode child : node) {
            if (child.isContainer()) {
                collectTypes(child, types, depth + 1);
            }
        }
    }

    private static void addType(String raw, Set<String> types) {
        String value = raw.strip();
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf(':'));
        String name = slash >= 0 ? value.substring(slash + 1) : value;
        if (name.matches("[A-Za-z][A-Za-z0-9]{0,59}") && types.size() < EngagementSignals.MAX_SCHEMA_TYPES) {
            types.add(name);
        }
    }

    private static boolean hasContent(Document document, String selector) {
        Element element = document.selectFirst(selector);
        return element != null && !element.attr("content").isBlank();
    }

    private static boolean hasAttrText(Element element, String... attributes) {
        for (String attribute : attributes) {
            if (!element.attr(attribute).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static String host(String absoluteUrl) {
        if (absoluteUrl == null || absoluteUrl.isEmpty()) {
            return "";
        }
        try {
            String host = URI.create(absoluteUrl).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
