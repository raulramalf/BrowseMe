package io.github.raulramalf.browseme.application.service;

import io.github.raulramalf.browseme.application.port.out.AiDraft;
import io.github.raulramalf.browseme.domain.analysis.AiInterpretation;
import io.github.raulramalf.browseme.domain.analysis.Priority;
import io.github.raulramalf.browseme.domain.analysis.Recommendation;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Filtra la salida del modelo antes de mostrarla. Reglas:
 * <ul>
 *   <li>Sin resumen no hay interpretación.</li>
 *   <li>Una recomendación que cita un {@code ruleId} no detectado (o inventado)
 *       se descarta entera: no se «arregla».</li>
 *   <li>Una recomendación sin {@code ruleIds} solo vale si viene marcada como
 *       sugerencia general.</li>
 *   <li>Longitudes y número de elementos acotados.</li>
 * </ul>
 */
public final class AiOutputValidator {

    public static final int MAX_SUMMARY = 600;
    public static final int MAX_ITEM = 300;
    public static final int MAX_LIST_ITEMS = 5;
    public static final int MAX_RECOMMENDATIONS = 8;

    public Optional<AiInterpretation> validate(AiDraft draft, Set<RuleId> detected) {
        if (draft == null || draft.summary() == null || draft.summary().isBlank()) {
            return Optional.empty();
        }
        List<Recommendation> recommendations = new ArrayList<>();
        if (draft.recommendations() != null) {
            for (AiDraft.Item item : draft.recommendations()) {
                toRecommendation(item, detected).ifPresent(recommendations::add);
                if (recommendations.size() == MAX_RECOMMENDATIONS) {
                    break;
                }
            }
        }
        return Optional.of(new AiInterpretation(
                truncate(draft.summary(), MAX_SUMMARY),
                texts(draft.strengths()),
                texts(draft.opportunities()),
                recommendations,
                texts(draft.actionPlan())));
    }

    private static Optional<Recommendation> toRecommendation(AiDraft.Item item, Set<RuleId> detected) {
        if (item == null || item.text() == null || item.text().isBlank()) {
            return Optional.empty();
        }
        Optional<Priority> priority = parse(Priority.class, item.priority());
        if (priority.isEmpty()) {
            return Optional.empty();
        }
        Set<RuleId> cited = new LinkedHashSet<>();
        for (String raw : item.ruleIds() == null ? List.<String>of() : item.ruleIds()) {
            Optional<RuleId> ruleId = parse(RuleId.class, raw);
            if (ruleId.isEmpty() || !detected.contains(ruleId.get())) {
                return Optional.empty();
            }
            cited.add(ruleId.get());
        }
        boolean general = Boolean.TRUE.equals(item.generalSuggestion());
        if (general != cited.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Recommendation(truncate(item.text(), MAX_ITEM), priority.get(), cited, general));
    }

    private static List<String> texts(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(v -> !v.isEmpty())
                .limit(MAX_LIST_ITEMS)
                .map(v -> truncate(v, MAX_ITEM))
                .toList();
    }

    private static <E extends Enum<E>> Optional<E> parse(Class<E> type, String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(type, raw.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String truncate(String value, int max) {
        String stripped = value.strip();
        if (stripped.length() <= max) {
            return stripped;
        }
        int end = Character.isHighSurrogate(stripped.charAt(max - 1)) ? max - 1 : max;
        return stripped.substring(0, end) + "…";
    }
}
