package io.github.raulramalf.browseme.domain.seo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SeoIssueTest {

    @Test
    void category_and_severity_come_from_the_rule_catalog() {
        SeoIssue issue = new SeoIssue(RuleId.TITLE_LENGTH, "El título tiene 82 caracteres",
                "Google trunca por píxeles", "Acorta el título");

        assertThat(issue.category()).isEqualTo(IssueCategory.EDITORIAL);
        assertThat(issue.severity()).isEqualTo(IssueSeverity.LOW);
    }

    @Test
    void every_issue_needs_evidence() {
        assertThatThrownBy(() -> new SeoIssue(RuleId.TITLE_MISSING, " ", "", "Añade un título"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeoIssue(RuleId.TITLE_MISSING, null, "", "Añade un título"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void evidence_is_bounded() {
        SeoIssue issue = new SeoIssue(RuleId.THIN_CONTENT, "x".repeat(2000), "", "Amplía el contenido");

        assertThat(issue.evidence()).hasSize(SeoIssue.MAX_EVIDENCE_LENGTH);
    }

    @Test
    void catalog_matches_the_specification() {
        // 18 de SEO, 7 de captación y fidelización, 6 de diseño del HTML y 6 de diseño visual.
        assertThat(RuleId.values()).hasSize(37);
        assertThat(RuleId.NOINDEX_DETECTED.severity()).isEqualTo(IssueSeverity.HIGH);
        assertThat(RuleId.H1_MULTIPLE.category()).isEqualTo(IssueCategory.EDITORIAL);
        assertThat(RuleId.H1_MULTIPLE.severity()).isEqualTo(IssueSeverity.INFO);
        assertThat(RuleId.IMAGES_NO_ALT.severity()).isEqualTo(IssueSeverity.MEDIUM);
    }
}
