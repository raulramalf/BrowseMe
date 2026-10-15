package io.github.raulramalf.browseme.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.raulramalf.browseme.domain.analysis.AnalysisErrorCode;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MaintenanceAndQueryTest {

    private static final Instant START = Instant.parse("2026-10-15T10:00:00Z");
    private final InMemoryRepositoryFake repository = new InMemoryRepositoryFake();

    private WebsiteAnalysis saved(Duration retention) {
        WebsiteAnalysis analysis = WebsiteAnalysis.create(UUID.randomUUID(), new UrlPolicy().parse("https://example.com"),
                START, retention);
        repository.save(analysis);
        return analysis;
    }

    @Test
    void expired_analyses_are_invisible_and_purged() {
        WebsiteAnalysis old = saved(Duration.ofDays(7));
        WebsiteAnalysis recent = saved(Duration.ofDays(30));
        Clock eightDaysLater = Clock.fixed(START.plus(Duration.ofDays(8)), ZoneOffset.UTC);

        assertThat(new GetAnalysisService(repository, eightDaysLater).get(old.id())).isEmpty();
        assertThat(new GetAnalysisService(repository, eightDaysLater).get(recent.id())).isPresent();
        assertThat(new AnalysisMaintenance(repository, eightDaysLater).purgeExpired()).isEqualTo(1);
        assertThat(repository.store).containsOnlyKeys(recent.id());
    }

    @Test
    void interrupted_analyses_are_failed_at_startup() {
        WebsiteAnalysis pending = saved(Duration.ofDays(7));
        WebsiteAnalysis fetching = saved(Duration.ofDays(7));
        fetching.startFetching();
        repository.save(fetching);

        int failed = new AnalysisMaintenance(repository, Clock.fixed(START, ZoneOffset.UTC)).failInterrupted();

        assertThat(failed).isEqualTo(2);
        for (UUID id : new UUID[] {pending.id(), fetching.id()}) {
            WebsiteAnalysis analysis = repository.findById(id).orElseThrow();
            assertThat(analysis.status()).isEqualTo(AnalysisStatus.FAILED);
            assertThat(analysis.errorCode()).contains(AnalysisErrorCode.INTERRUPTED);
        }
    }
}
