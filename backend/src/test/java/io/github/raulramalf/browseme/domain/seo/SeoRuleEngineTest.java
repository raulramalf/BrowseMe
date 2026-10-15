package io.github.raulramalf.browseme.domain.seo;

import static io.github.raulramalf.browseme.domain.seo.SnapshotFixture.perfect;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.raulramalf.browseme.domain.page.Heading;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.page.RobotsTxtStatus;
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
                RuleId.H1_MULTIPLE, RuleId.HEADING_SKIP, RuleId.THIN_CONTENT);
    }

    @Test
    void rejects_duplicate_rules() {
        assertThatThrownBy(() -> new SeoRuleEngine(List.of(new NotHttpsRule(), new NotHttpsRule())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
