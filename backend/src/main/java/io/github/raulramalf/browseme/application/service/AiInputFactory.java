package io.github.raulramalf.browseme.application.service;

import io.github.raulramalf.browseme.application.port.out.AiInput;
import io.github.raulramalf.browseme.application.port.out.AiInput.IssueSummary;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import java.util.List;

/**
 * Construye la entrada del modelo con un tamaño acotado. Los textos de la
 * página se recortan; el modelo recibe cifras y hallazgos, no la página.
 */
public final class AiInputFactory {

    public static final int MAX_SUMMARY_CHARS = 2_000;
    public static final int MAX_EVIDENCE_CHARS = 200;
    private static final int MAX_QUOTED_TEXT = 150;

    public AiInput create(PageSnapshot snapshot, List<SeoIssue> issues) {
        StringBuilder summary = new StringBuilder();
        line(summary, "URL analizada", snapshot.finalUrl().toString());
        line(summary, "HTTPS", snapshot.https() ? "sí" : "no");
        line(summary, "Título", quoted(snapshot.title()));
        line(summary, "Metadescripción", quoted(snapshot.metaDescription()));
        line(summary, "H1", snapshot.headings().stream().filter(h -> h.level() == 1).findFirst()
                .map(h -> quoted(h.text())).orElse("(ninguno)"));
        line(summary, "Encabezados", String.valueOf(snapshot.headings().size()));
        line(summary, "Palabras visibles aproximadas", String.valueOf(snapshot.visibleWordCount()));
        line(summary, "Enlaces internos/externos", snapshot.links().internal() + "/" + snapshot.links().external());
        line(summary, "Imágenes (total/sin alt)", snapshot.images().total() + "/" + snapshot.images().missingAlt());
        line(summary, "Idioma declarado", snapshot.lang() == null ? "(ninguno)" : snapshot.lang());

        String bounded = summary.length() > MAX_SUMMARY_CHARS ? summary.substring(0, MAX_SUMMARY_CHARS) : summary.toString();
        List<IssueSummary> summaries = issues.stream()
                .map(issue -> new IssueSummary(issue.ruleId().name(), issue.category().name(),
                        issue.severity().name(), truncate(issue.evidence(), MAX_EVIDENCE_CHARS)))
                .toList();
        return new AiInput(bounded, summaries);
    }

    private static void line(StringBuilder out, String label, String value) {
        out.append("- ").append(label).append(": ").append(value).append('\n');
    }

    private static String quoted(String text) {
        if (text == null) {
            return "(no existe)";
        }
        if (text.isBlank()) {
            return "(vacío)";
        }
        return "\"" + truncate(text.replace('"', '\''), MAX_QUOTED_TEXT) + "\"";
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
