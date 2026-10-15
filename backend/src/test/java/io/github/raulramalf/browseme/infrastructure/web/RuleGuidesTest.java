package io.github.raulramalf.browseme.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.raulramalf.browseme.domain.seo.RuleArea;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RuleGuidesTest {

    @Test
    void every_rule_has_a_title_and_a_guide_with_concrete_steps() {
        for (RuleId id : RuleId.values()) {
            RuleGuides.Guide guide = RuleGuides.of(id);
            assertThat(RuleTitles.of(id)).as(id + " título").isNotBlank();
            assertThat(guide).as(id + " guía").isNotNull();
            assertThat(guide.why()).as(id + " por qué").hasSizeGreaterThan(20);
            assertThat(guide.steps()).as(id + " pasos").isNotEmpty().allSatisfy(step -> assertThat(step).isNotBlank());
        }
    }

    @Test
    void all_customer_acquisition_rules_are_flagged_as_affecting_clients() {
        Arrays.stream(RuleId.values()).filter(id -> id.area() == RuleArea.ENGAGEMENT)
                .forEach(id -> assertThat(RuleGuides.of(id).clients()).as(id.name()).isTrue());
    }

    @Test
    void contact_guide_gives_the_steps_a_small_business_can_follow() {
        assertThat(RuleGuides.of(RuleId.CONTACT_MISSING).steps())
                .anyMatch(step -> step.contains("WhatsApp")).anyMatch(step -> step.contains("teléfono"));
    }
}
