package io.github.raulramalf.browseme.domain.seo;

import static io.github.raulramalf.browseme.domain.seo.SnapshotFixture.perfect;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.raulramalf.browseme.domain.page.DesignSignals;
import io.github.raulramalf.browseme.domain.page.EngagementSignals;
import io.github.raulramalf.browseme.domain.page.Heading;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.page.RobotsTxtStatus;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import io.github.raulramalf.browseme.domain.seo.rules.NotHttpsRule;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SeoRuleEngineTest {

    private final SeoRuleEngine engine = SeoRuleEngine.withDefaultRules();

    @Test
    void every_rule_of_the_catalog_can_be_triggered() {
        PageSnapshot worst = perfect()
                .finalUrl("http://example.com/").redirects(3)
                .title("Hola").metaDescription("Corta").lang(null).canonical("https://otra.example/")
                .robots("noindex")
                .headings(new Heading(2, "A"),
                        new Heading(4, "B"))
                .images(1, 0, 0, 1).viewport(false).words(10).robotsTxt(RobotsTxtStatus.BLOCKED)
                .build();
        PageSnapshot worseHeadings = perfect().title(null).metaDescription(null).canonical(null)
                .headings(new Heading(1, "A"),
                        new Heading(1, "B"))
                .build();

        Set<RuleId> triggered = engine.evaluate(worst).stream().map(SeoIssue::ruleId).collect(Collectors.toSet());
        engine.evaluate(worseHeadings).forEach(issue -> triggered.add(issue.ruleId()));
        triggered.addAll(engine.evaluate(perfect().httpStatus(404).build()).stream().map(SeoIssue::ruleId).toList());
        PageSnapshot noEngagementBadDesign = perfect().images(3, 3, 0, 0)
                .engagement(new EngagementSignals(0, 0, 0, 0, 0, 1, Set.of(), Set.of(), false, false, false))
                .design(new DesignSignals(false, false, 1, 1, 2, 2, 3))
                .visual(new VisualMetrics(390, 700, 30, 4, List.of(), 5, 10, 6, List.of("A", "B", "C", "D"),
                        false, false, 5))
                .build();
        triggered.addAll(engine.evaluate(noEngagementBadDesign).stream().map(SeoIssue::ruleId).toList());
        triggered.addAll(engine.evaluate(perfect().engagement(EngagementSignals.none()).build()).stream()
                .map(SeoIssue::ruleId).toList());

        assertThat(triggered).containsExactlyInAnyOrder(RuleId.values());
    }

    @Test
    void a_perfect_page_has_no_issues() {
        assertThat(engine.evaluate(perfect().build())).isEmpty();
    }

    @Test
    void an_http_error_page_only_reports_the_error() {
        List<SeoIssue> issues = engine.evaluate(perfect().httpStatus(500).title(null).viewport(false).build());

        assertThat(issues).extracting(SeoIssue::ruleId).containsExactly(RuleId.FETCH_HTTP_ERROR);
    }

    @Test
    void issues_are_ordered_by_severity_then_catalog() {
        List<SeoIssue> issues = engine.evaluate(perfect()
                .finalUrl("http://example.com/").canonical("http://example.com/")
                .words(10).viewport(false).lang(null).title(null).build());

        assertThat(issues).extracting(SeoIssue::ruleId).containsExactly(
                RuleId.TITLE_MISSING, RuleId.NOT_HTTPS,
                RuleId.VIEWPORT_MISSING,
                RuleId.LANG_MISSING,
                RuleId.THIN_CONTENT);
        assertThat(issues).extracting(SeoIssue::severity).isSortedAccordingTo(Enum::compareTo);
    }

    @Test
    void is_deterministic() {
        PageSnapshot snapshot = perfect().title("x").words(3).viewport(false).build();

        assertThat(engine.evaluate(snapshot)).isEqualTo(engine.evaluate(snapshot));
    }

    @Test
    void editorial_rules_are_flagged_as_suggestions() {
        Set<RuleId> editorial = Arrays.stream(RuleId.values())
                .filter(id -> id.category() == IssueCategory.EDITORIAL).collect(Collectors.toSet());

        assertThat(editorial).containsExactlyInAnyOrder(RuleId.TITLE_LENGTH, RuleId.META_DESC_LENGTH,
                RuleId.H1_MULTIPLE, RuleId.HEADING_SKIP, RuleId.THIN_CONTENT,
                // Captación y diseño que dependen del tipo de negocio
                RuleId.CONTACT_MISSING, RuleId.CTA_MISSING, RuleId.SUBSCRIBE_MISSING, RuleId.SOCIAL_PROOF_MISSING,
                RuleId.SOCIAL_LINKS_MISSING, RuleId.NAV_MISSING, RuleId.GENERIC_LINK_TEXT,
                RuleId.CTA_NOT_VISIBLE_MOBILE, RuleId.TOO_MANY_FONTS);
    }

    @Test
    void rejects_duplicate_rules() {
        assertThatThrownBy(() -> new SeoRuleEngine(List.of(new NotHttpsRule(), new NotHttpsRule())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
