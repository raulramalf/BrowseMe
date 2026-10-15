package io.github.raulramalf.browseme.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.raulramalf.browseme.application.port.out.AiDraft;
import io.github.raulramalf.browseme.application.port.out.AiDraft.Item;
import io.github.raulramalf.browseme.domain.analysis.AiInterpretation;
import io.github.raulramalf.browseme.domain.analysis.Priority;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AiOutputValidatorTest {

    private static final Set<RuleId> DETECTED = Set.of(RuleId.NOT_HTTPS, RuleId.TITLE_MISSING);
    private final AiOutputValidator validator = new AiOutputValidator();

    private static AiDraft draft(Item... items) {
        return new AiDraft("Tu web funciona, pero le falta HTTPS.", List.of("Tiene metadescripción"),
                List.of("Mejorar el título"), Arrays.asList(items), List.of("Paso 1: activa HTTPS"));
    }

    @Test
    void keeps_recommendations_that_cite_detected_rules() {
        AiInterpretation result = validator.validate(
                draft(new Item("Activa HTTPS", "high", List.of("NOT_HTTPS"), false)), DETECTED).orElseThrow();

        assertThat(result.recommendations()).singleElement().satisfies(r -> {
            assertThat(r.priority()).isEqualTo(Priority.HIGH);
            assertThat(r.ruleIds()).containsExactly(RuleId.NOT_HTTPS);
        });
    }

    @Test
    void discards_recommendations_with_invented_or_undetected_rule_ids() {
        AiInterpretation result = validator.validate(draft(
                new Item("Mejora la velocidad", "HIGH", List.of("PAGE_SPEED_SLOW"), false),
                new Item("Pon un H1", "MEDIUM", List.of("H1_MISSING"), false),
                new Item("Mezcla", "LOW", List.of("NOT_HTTPS", "INVENTADO"), false),
                new Item("Activa HTTPS", "HIGH", List.of("NOT_HTTPS"), false)), DETECTED).orElseThrow();

        assertThat(result.recommendations()).extracting(r -> r.text()).containsExactly("Activa HTTPS");
    }

    @Test
    void general_suggestions_must_be_flagged_and_cite_nothing() {
        AiInterpretation result = validator.validate(draft(
                new Item("Publica contenido nuevo cada mes", "LOW", List.of(), true),
                new Item("Sin respaldo y sin marcar", "LOW", List.of(), false),
                new Item("Marcada pero con regla", "LOW", List.of("NOT_HTTPS"), true),
                new Item("Sin campo general", "LOW", null, null)), DETECTED).orElseThrow();

        assertThat(result.recommendations()).extracting(r -> r.text()).containsExactly("Publica contenido nuevo cada mes");
    }

    @Test
    void discards_items_with_invalid_priority_or_text() {
        AiInterpretation result = validator.validate(draft(
                new Item("Urgente", "CRITICAL", List.of("NOT_HTTPS"), false),
                new Item(" ", "HIGH", List.of("NOT_HTTPS"), false),
                null), DETECTED).orElseThrow();

        assertThat(result.recommendations()).isEmpty();
    }

    @Test
    void without_summary_there_is_no_interpretation() {
        assertThat(validator.validate(new AiDraft(null, null, null, null, null), DETECTED)).isEmpty();
        assertThat(validator.validate(new AiDraft("  ", List.of(), List.of(), List.of(), List.of()), DETECTED)).isEmpty();
        assertThat(validator.validate(null, DETECTED)).isEmpty();
    }

    @Test
    void tolerates_missing_lists() {
        AiInterpretation result = validator.validate(new AiDraft("Resumen", null, null, null, null), DETECTED)
                .orElseThrow();

        assertThat(result.strengths()).isEmpty();
        assertThat(result.recommendations()).isEmpty();
    }

    @Test
    void bounds_sizes_and_counts() {
        String huge = "x".repeat(5_000);
        List<String> many = Collections.nCopies(50, "punto");
        Item[] items = new Item[20];
        Arrays.fill(items, new Item(huge, "LOW", List.of(), true));

        AiInterpretation result = validator.validate(new AiDraft(huge, many, many, Arrays.asList(items), many), DETECTED)
                .orElseThrow();

        assertThat(result.summary()).hasSizeLessThanOrEqualTo(AiOutputValidator.MAX_SUMMARY + 1);
        assertThat(result.strengths()).hasSize(AiOutputValidator.MAX_LIST_ITEMS);
        assertThat(result.recommendations()).hasSize(AiOutputValidator.MAX_RECOMMENDATIONS);
        assertThat(result.recommendations().getFirst().text()).hasSizeLessThanOrEqualTo(AiOutputValidator.MAX_ITEM + 1);
    }
}
