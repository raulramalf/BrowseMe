package io.github.raulramalf.browseme.domain.seo;

import java.util.Objects;

/**
 * Hallazgo producido por una regla determinista. Es un hecho con su evidencia,
 * nunca una opinión de la IA.
 *
 * @param evidence       qué se observó en la página (por ejemplo «El título tiene 82 caracteres»)
 * @param limitation     por qué el hallazgo puede no ser concluyente; cadena vacía si no aplica
 * @param recommendation acción básica sugerida por la regla
 */
public record SeoIssue(RuleId ruleId, String evidence, String limitation, String recommendation) {

    public static final int MAX_EVIDENCE_LENGTH = 500;

    public SeoIssue {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(limitation, "limitation");
        Objects.requireNonNull(recommendation, "recommendation");
        if (evidence.isBlank()) {
            throw new IllegalArgumentException("Todo hallazgo necesita evidencia");
        }
        if (evidence.length() > MAX_EVIDENCE_LENGTH) {
            evidence = evidence.substring(0, MAX_EVIDENCE_LENGTH);
        }
    }

    public IssueCategory category() {
        return ruleId.category();
    }

    public IssueSeverity severity() {
        return ruleId.severity();
    }
}
