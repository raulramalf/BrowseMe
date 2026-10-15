package io.github.raulramalf.browseme.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SnapshotFixture;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebsiteAnalysisTest {

    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    private static WebsiteAnalysis newAnalysis() {
        return WebsiteAnalysis.create(UUID.randomUUID(), new UrlPolicy().parse("https://example.com"), NOW,
                Duration.ofDays(7));
    }

    private static AiInterpretation interpretation() {
        return new AiInterpretation("Resumen", List.of(), List.of(),
                List.of(new Recommendation("Añade HTTPS", Priority.HIGH, Set.of(RuleId.NOT_HTTPS), false)), List.of());
    }

    @Test
    void walks_the_happy_path() {
        WebsiteAnalysis analysis = newAnalysis();
        assertThat(analysis.status()).isEqualTo(AnalysisStatus.PENDING);
        assertThat(analysis.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));

        analysis.startFetching();
        analysis.startAnalyzing(SnapshotFixture.perfect().finalUrl("https://example.com/final").build());
        analysis.startInterpreting(List.of());
        analysis.complete(interpretation(), NOW.plusSeconds(30));

        assertThat(analysis.status()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(analysis.aiStatus()).contains(AiStatus.AVAILABLE);
        assertThat(analysis.finalUrl()).hasValueSatisfying(u -> assertThat(u).hasToString("https://example.com/final"));
        assertThat(analysis.completedAt()).contains(NOW.plusSeconds(30));
    }

    @Test
    void can_complete_without_ai() {
        WebsiteAnalysis analysis = newAnalysis();
        analysis.startFetching();
        analysis.startAnalyzing(SnapshotFixture.perfect().build());
        analysis.startInterpreting(List.of());

        analysis.completeWithoutAi(AiStatus.UNAVAILABLE, NOW);

        assertThat(analysis.status()).isEqualTo(AnalysisStatus.COMPLETED_WITHOUT_AI);
        assertThat(analysis.ai()).isEmpty();
        assertThatThrownBy(() -> newAnalysisInterpreting().completeWithoutAi(AiStatus.AVAILABLE, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static WebsiteAnalysis newAnalysisInterpreting() {
        WebsiteAnalysis analysis = newAnalysis();
        analysis.startFetching();
        analysis.startAnalyzing(SnapshotFixture.perfect().build());
        analysis.startInterpreting(List.of());
        return analysis;
    }

    @Test
    void rejects_invalid_transitions() {
        WebsiteAnalysis analysis = newAnalysis();

        assertThatThrownBy(() -> analysis.startAnalyzing(SnapshotFixture.perfect().build()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> analysis.startInterpreting(List.of())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> analysis.complete(interpretation(), NOW)).isInstanceOf(IllegalStateException.class);
        analysis.startFetching();
        assertThatThrownBy(analysis::startFetching).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void can_fail_from_any_non_final_state_but_not_twice() {
        WebsiteAnalysis analysis = newAnalysis();
        analysis.startFetching();

        analysis.fail(AnalysisErrorCode.TIMEOUT, NOW);

        assertThat(analysis.status()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.errorCode()).contains(AnalysisErrorCode.TIMEOUT);
        assertThatThrownBy(() -> analysis.fail(AnalysisErrorCode.INTERNAL_ERROR, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void an_http_error_keeps_the_snapshot_and_the_issue_that_explains_it() {
        WebsiteAnalysis analysis = newAnalysis();
        analysis.startFetching();
        analysis.startAnalyzing(SnapshotFixture.perfect().httpStatus(404).build());
        SeoIssue issue = new SeoIssue(RuleId.FETCH_HTTP_ERROR, "HTTP 404", "", "Revisa la URL");

        analysis.fail(AnalysisErrorCode.HTTP_ERROR, List.of(issue), NOW);

        assertThat(analysis.snapshot()).isPresent();
        assertThat(analysis.issues()).containsExactly(issue);
    }

    @Test
    void expires_at_the_retention_limit() {
        WebsiteAnalysis analysis = newAnalysis();

        assertThat(analysis.isExpired(NOW.plus(Duration.ofDays(7)).minusSeconds(1))).isFalse();
        assertThat(analysis.isExpired(NOW.plus(Duration.ofDays(7)))).isTrue();
    }

    @Test
    void round_trips_through_its_snapshot() {
        WebsiteAnalysis analysis = newAnalysisInterpreting();
        analysis.complete(interpretation(), NOW);
        analysis.assignVersion(4);

        WebsiteAnalysis restored = WebsiteAnalysis.restore(analysis.toSnapshot());

        assertThat(restored.toSnapshot()).isEqualTo(analysis.toSnapshot());
    }

    @Test
    void recommendations_cite_detected_rules_or_are_general_suggestions() {
        assertThatThrownBy(() -> new Recommendation("x", Priority.LOW, Set.of(), false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Recommendation("x", Priority.LOW, Set.of(RuleId.NOT_HTTPS), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Recommendation("x", Priority.LOW, Set.of(), true).generalSuggestion()).isTrue();
    }

    @Test
    void limitations_always_include_what_cannot_be_claimed() {
        WebsiteAnalysis analysis = newAnalysisInterpreting();
        analysis.completeWithoutAi(AiStatus.UNAVAILABLE, NOW);

        List<String> limitations = ReportLimitations.of(analysis);

        assertThat(limitations).containsAll(ReportLimitations.ALWAYS);
        assertThat(limitations).anyMatch(text -> text.contains("IA no está disponible"));
    }
}
