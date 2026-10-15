package io.github.raulramalf.browseme.domain.analysis;

import io.github.raulramalf.browseme.domain.seo.RuleId;
import java.util.Objects;
import java.util.Set;

/**
 * Recomendación de la IA. O se apoya en hallazgos realmente detectados
 * ({@code ruleIds}) o se marca como sugerencia general; nunca ambas cosas a medias.
 */
public record Recommendation(String text, Priority priority, Set<RuleId> ruleIds, boolean generalSuggestion) {

    public Recommendation {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(priority, "priority");
        ruleIds = ruleIds == null ? Set.of() : Set.copyOf(ruleIds);
        if (text.isBlank()) {
            throw new IllegalArgumentException("Una recomendación necesita texto");
        }
        if (generalSuggestion != ruleIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Una recomendación cita hallazgos detectados o es una sugerencia general");
        }
    }
}
