package io.github.raulramalf.browseme.domain.analysis;

import java.util.List;
import java.util.Objects;

/** Interpretación de la IA ya validada: explica y prioriza, no aporta hechos nuevos. */
public record AiInterpretation(
        String summary,
        List<String> strengths,
        List<String> opportunities,
        List<Recommendation> recommendations,
        List<String> actionPlan) {

    public AiInterpretation {
        Objects.requireNonNull(summary, "summary");
        if (summary.isBlank()) {
            throw new IllegalArgumentException("La interpretación necesita un resumen");
        }
        strengths = List.copyOf(strengths);
        opportunities = List.copyOf(opportunities);
        recommendations = List.copyOf(recommendations);
        actionPlan = List.copyOf(actionPlan);
    }
}
