package io.github.raulramalf.browseme.application.port.out;

import java.util.List;

/**
 * Respuesta del modelo tal como llega, solo deserializada. Los campos pueden ser
 * {@code null} y los {@code ruleIds} pueden ser inventados: nada de esto se
 * muestra sin pasar por {@code AiOutputValidator}.
 */
public record AiDraft(String summary, List<String> strengths, List<String> opportunities,
                      List<Item> recommendations, List<String> actionPlan) {

    public record Item(String text, String priority, List<String> ruleIds, Boolean generalSuggestion) {
    }
}
