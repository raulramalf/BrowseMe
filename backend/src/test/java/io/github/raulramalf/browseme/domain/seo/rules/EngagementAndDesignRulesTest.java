package io.github.raulramalf.browseme.domain.seo.rules;

import static io.github.raulramalf.browseme.domain.seo.SnapshotFixture.goodVisual;
import static io.github.raulramalf.browseme.domain.seo.SnapshotFixture.perfect;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.raulramalf.browseme.domain.page.DesignSignals;
import io.github.raulramalf.browseme.domain.page.EngagementSignals;
import io.github.raulramalf.browseme.domain.page.VisualCheck;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import io.github.raulramalf.browseme.domain.page.VisualStatus;
import io.github.raulramalf.browseme.domain.seo.RuleArea;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRuleEngine;
import io.github.raulramalf.browseme.domain.seo.SnapshotFixture;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EngagementAndDesignRulesTest {

    private static final SeoRuleEngine ENGINE = SeoRuleEngine.withDefaultRules();

    private static List<RuleId> detected(SnapshotFixture fixture) {
        return ENGINE.evaluate(fixture.build()).stream().map(SeoIssue::ruleId).toList();
    }

    private static SeoIssue only(SnapshotFixture fixture, RuleId id) {
        return ENGINE.evaluate(fixture.build()).stream().filter(i -> i.ruleId() == id).findFirst().orElseThrow();
    }

    private static EngagementSignals engagement(int phone, int email, int whatsapp, int forms, int emailForms,
                                                int ctas, Set<String> social, Set<String> schema,
                                                boolean testimonials, boolean ogTitle, boolean ogImage) {
        return new EngagementSignals(phone, email, whatsapp, forms, emailForms, ctas, social, schema, testimonials,
                ogTitle, ogImage);
    }

    @Test
    void the_perfect_page_triggers_no_rule_with_or_without_visual_check() {
        assertThat(detected(perfect())).isEmpty();
        assertThat(detected(perfect().visual(goodVisual()))).isEmpty();
    }

    @Test
    void every_new_rule_belongs_to_its_area() {
        assertThat(RuleId.CONTACT_MISSING.area()).isEqualTo(RuleArea.ENGAGEMENT);
        assertThat(RuleId.NAV_MISSING.area()).isEqualTo(RuleArea.DESIGN);
        assertThat(RuleId.LOW_CONTRAST_TEXT.area()).isEqualTo(RuleArea.DESIGN);
        assertThat(RuleId.NOT_HTTPS.area()).isEqualTo(RuleArea.SEO);
    }

    @Nested
    class Engagement {

        @Test
        void a_page_without_any_signal_triggers_every_engagement_rule() {
            assertThat(detected(perfect().engagement(EngagementSignals.none()))).containsExactlyInAnyOrder(
                    RuleId.CONTACT_MISSING, RuleId.CTA_MISSING, RuleId.SUBSCRIBE_MISSING,
                    RuleId.SOCIAL_PROOF_MISSING, RuleId.SOCIAL_LINKS_MISSING, RuleId.BUSINESS_DATA_MISSING,
                    RuleId.SHARE_PREVIEW_MISSING);
        }

        @Test
        void any_contact_channel_is_enough() {
            for (EngagementSignals signals : List.of(
                    engagement(1, 0, 0, 0, 0, 1, Set.of(), Set.of(), false, true, true),
                    engagement(0, 1, 0, 0, 0, 1, Set.of(), Set.of(), false, true, true),
                    engagement(0, 0, 1, 0, 0, 1, Set.of(), Set.of(), false, true, true),
                    engagement(0, 0, 0, 1, 0, 1, Set.of(), Set.of(), false, true, true))) {
                assertThat(detected(perfect().engagement(signals))).doesNotContain(RuleId.CONTACT_MISSING);
            }
        }

        @Test
        void reviews_count_as_social_proof_by_schema_or_by_heading() {
            EngagementSignals bySchema = engagement(1, 0, 0, 0, 1, 1, Set.of("x"), Set.of("Review", "LocalBusiness"),
                    false, true, true);
            EngagementSignals byHeading = engagement(1, 0, 0, 0, 1, 1, Set.of("x"), Set.of("Bakery"), true, true,
                    true);
            assertThat(detected(perfect().engagement(bySchema))).isEmpty();
            assertThat(detected(perfect().engagement(byHeading))).isEmpty();
        }

        @Test
        void share_preview_names_the_missing_tags() {
            EngagementSignals noImage = engagement(1, 0, 0, 0, 1, 1, Set.of("x"), Set.of("Bakery", "Review"), true,
                    true, false);
            assertThat(only(perfect().engagement(noImage), RuleId.SHARE_PREVIEW_MISSING).evidence())
                    .contains("og:image").doesNotContain("og:title");
        }
    }

    @Nested
    class StructuralDesign {

        @Test
        void bad_structure_triggers_every_structural_rule() {
            DesignSignals bad = new DesignSignals(false, false, 2, 1, 4, 3, 5);
            assertThat(detected(perfect().images(5, 5, 0, 0).design(bad))).containsExactlyInAnyOrder(
                    RuleId.NAV_MISSING, RuleId.FAVICON_MISSING, RuleId.GENERIC_LINK_TEXT, RuleId.EMPTY_INTERACTIVE,
                    RuleId.FORM_LABELS_MISSING, RuleId.IMAGES_NO_DIMENSIONS);
        }

        @Test
        void evidence_carries_the_counts() {
            DesignSignals unlabeled = new DesignSignals(true, true, 0, 0, 4, 3, 0);
            assertThat(only(perfect().design(unlabeled), RuleId.FORM_LABELS_MISSING).evidence())
                    .startsWith("3 de 4 campos");
        }
    }

    @Nested
    class VisualDesign {

        private VisualMetrics metrics(int contentWidth, int lowContrast, int smallTexts, int smallTaps,
                                      List<String> fonts, boolean ctaMobile) {
            return new VisualMetrics(390, contentWidth, 40, lowContrast, List.of("Gris claro"), smallTexts, 20,
                    smallTaps, fonts, ctaMobile, true, 4);
        }

        @Test
        void visual_rules_only_run_when_the_page_was_drawn() {
            for (VisualStatus status : List.of(VisualStatus.NOT_RUN, VisualStatus.DISABLED,
                    VisualStatus.BROWSER_NOT_FOUND, VisualStatus.FAILED)) {
                assertThat(detected(perfect().visual(VisualCheck.of(status)))).isEmpty();
            }
        }

        @Test
        void a_badly_drawn_page_triggers_every_visual_rule() {
            VisualMetrics bad = metrics(600, 5, 6, 7, List.of("A", "B", "C", "D"), false);
            assertThat(detected(perfect().visual(bad))).containsExactlyInAnyOrder(
                    RuleId.MOBILE_HORIZONTAL_SCROLL, RuleId.LOW_CONTRAST_TEXT, RuleId.SMALL_TEXT_MOBILE,
                    RuleId.SMALL_TAP_TARGETS, RuleId.CTA_NOT_VISIBLE_MOBILE, RuleId.TOO_MANY_FONTS);
        }

        @Test
        void thresholds_tolerate_small_deviations() {
            VisualMetrics almost = metrics(390 + MobileHorizontalScrollRule.TOLERANCE_PX, 2, 2, 2,
                    List.of("A", "B", "C"), true);
            assertThat(detected(perfect().visual(almost))).isEmpty();
        }

        @Test
        void hidden_cta_is_only_reported_if_the_page_has_one() {
            VisualMetrics noCtaVisible = metrics(390, 0, 0, 0, List.of("A"), false);
            EngagementSignals noCta = new EngagementSignals(1, 0, 0, 0, 1, 0, Set.of("x"), Set.of("Bakery", "Review"),
                    true, true, true);
            assertThat(detected(perfect().engagement(noCta).visual(noCtaVisible)))
                    .containsExactly(RuleId.CTA_MISSING);
        }

        @Test
        void low_contrast_evidence_quotes_samples_as_data() {
            VisualMetrics bad = metrics(390, 4, 0, 0, List.of("A"), true);
            assertThat(only(perfect().visual(bad), RuleId.LOW_CONTRAST_TEXT).evidence())
                    .contains("4 de 40").contains("«Gris claro»");
        }
    }
}
