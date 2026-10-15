package io.github.raulramalf.browseme.infrastructure.ai;

import java.util.List;

/**
 * Forma exacta que se exige al modelo. De esta clase sale el JSON Schema que
 * Ollama recibe como {@code format}, así que nombres y tipos son el contrato.
 */
public record AiResponseDto(
        String summary,
        List<String> strengths,
        List<String> opportunities,
        List<RecommendationDto> recommendations,
        List<String> actionPlan) {

    public record RecommendationDto(String text, String priority, List<String> ruleIds, Boolean generalSuggestion) {
    }
}
