package io.github.raulramalf.browseme.domain.score;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.raulramalf.browseme.domain.page.VisualStatus;
import io.github.raulramalf.browseme.domain.seo.RuleArea;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    private static List<SeoIssue> issues(RuleId... ids) {
        return Arrays.stream(ids).map(id -> new SeoIssue(id, "evidencia", "", "recomendación")).toList();
    }

    private static AreaScore area(ScoreCard card, RuleArea area) {
        return card.areas().stream().filter(a -> a.area() == area).findFirst().orElseThrow();
    }

    @Test
    void a_page_without_findings_scores_ten() {
        ScoreCard card = ScoreCalculator.calculate(List.of(), VisualStatus.DONE);
        assertThat(card.tenths()).isEqualTo(100);
        assertThat(card.partial()).isFalse();
    }

    @Test
    void penalties_depend_on_severity_and_weights_on_area() {
        // SEO: 10 - 3 (alta) - 1,5 (media) = 5,5. Captación: 10 - 1,5 = 8,5. Diseño: 10 - 0,5 = 9,5.
        ScoreCard card = ScoreCalculator.calculate(
                issues(RuleId.NOT_HTTPS, RuleId.META_DESC_MISSING, RuleId.CONTACT_MISSING, RuleId.NAV_MISSING),
                VisualStatus.DONE);
        assertThat(area(card, RuleArea.SEO).tenths()).isEqualTo(55);
        assertThat(area(card, RuleArea.ENGAGEMENT).tenths()).isEqualTo(85);
        assertThat(area(card, RuleArea.DESIGN).tenths()).isEqualTo(95);
        // 5,5 × 0,4 + 8,5 × 0,3 + 9,5 × 0,3 = 2,2 + 2,55 + 2,85 = 7,6
        assertThat(card.tenths()).isEqualTo(76);
        assertThat(area(card, RuleArea.SEO).findings()).isEqualTo(2);
    }

    @Test
    void informative_findings_do_not_penalize() {
        ScoreCard card = ScoreCalculator.calculate(issues(RuleId.CANONICAL_MISSING, RuleId.TOO_MANY_FONTS),
                VisualStatus.DONE);
        assertThat(card.tenths()).isEqualTo(100);
    }

    @Test
    void an_area_never_goes_below_zero() {
        ScoreCard card = ScoreCalculator.calculate(issues(RuleId.NOT_HTTPS, RuleId.NOINDEX_DETECTED,
                RuleId.ROBOTS_TXT_BLOCKS, RuleId.TITLE_MISSING), VisualStatus.DONE);
        assertThat(area(card, RuleArea.SEO).tenths()).isZero();
        assertThat(card.tenths()).isEqualTo(60);
    }

    @Test
    void design_is_partial_without_the_visual_check() {
        ScoreCard card = ScoreCalculator.calculate(List.of(), VisualStatus.BROWSER_NOT_FOUND);
        assertThat(area(card, RuleArea.DESIGN).partial()).isTrue();
        assertThat(card.partial()).isTrue();
    }

    @Test
    void weights_add_up_to_one_hundred() {
        assertThat(ScoreCalculator.WEIGHT_PERCENT.values().stream().mapToInt(Integer::intValue).sum())
                .isEqualTo(100);
    }
}
