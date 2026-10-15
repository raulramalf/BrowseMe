package io.github.raulramalf.browseme.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.raulramalf.browseme.application.port.in.AnalysisCapacityExceededException;
import io.github.raulramalf.browseme.application.port.in.SubmittedAnalysis;
import io.github.raulramalf.browseme.application.port.out.AiDraft;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.seo.SeoRuleEngine;
import io.github.raulramalf.browseme.domain.seo.SnapshotFixture;
import io.github.raulramalf.browseme.domain.url.InvalidUrlException;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import io.github.raulramalf.browseme.domain.url.UrlRejectedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class SubmitAnalysisServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC);
    private final UrlPolicy policy = new UrlPolicy();
    private final InMemoryRepositoryFake repository = new InMemoryRepositoryFake();
    private final List<Runnable> queued = new ArrayList<>();

    private SubmitAnalysisService service(Executor executor) {
        AnalysisProcessor processor = new AnalysisProcessor(repository, url -> SnapshotFixture.perfect().build(),
                SeoRuleEngine.withDefaultRules(),
                input -> new AiDraft("Resumen", List.of(), List.of(), List.of(), List.of()), policy, clock);
        return new SubmitAnalysisService(policy, repository, processor, executor, clock, Duration.ofDays(7));
    }

    @Test
    void saves_a_pending_analysis_and_queues_it() {
        SubmittedAnalysis submitted = service(queued::add).submit("https://example.com");

        assertThat(submitted.status()).isEqualTo(AnalysisStatus.PENDING);
        assertThat(submitted.id().version()).isEqualTo(4);
        assertThat(repository.findById(submitted.id())).isPresent();
        assertThat(queued).hasSize(1);

        queued.getFirst().run();
        assertThat(repository.findById(submitted.id()).orElseThrow().status()).isEqualTo(AnalysisStatus.COMPLETED);
    }

    @Test
    void the_same_url_in_progress_returns_the_existing_analysis() {
        SubmitAnalysisService service = service(queued::add);

        SubmittedAnalysis first = service.submit("https://example.com");
        SubmittedAnalysis second = service.submit("HTTPS://EXAMPLE.COM:443/#x");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(queued).hasSize(1);
    }

    @Test
    void a_finished_analysis_does_not_block_a_new_one() {
        SubmitAnalysisService service = service(Runnable::run);

        SubmittedAnalysis first = service.submit("https://example.com");
        SubmittedAnalysis second = service.submit("https://example.com");

        assertThat(second.id()).isNotEqualTo(first.id());
    }

    @Test
    void a_full_queue_is_reported_and_leaves_no_orphan() {
        SubmitAnalysisService service = service(task -> {
            throw new RejectedExecutionException("cola llena");
        });

        assertThatThrownBy(() -> service.submit("https://example.com"))
                .isInstanceOf(AnalysisCapacityExceededException.class);
        assertThat(repository.store).isEmpty();

        // Tras el rechazo la URL queda libre.
        SubmittedAnalysis retry = service(queued::add).submit("https://example.com");
        assertThat(retry.status()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    void validates_before_saving_anything() {
        SubmitAnalysisService service = service(queued::add);

        assertThatThrownBy(() -> service.submit("ftp://example.com")).isInstanceOf(InvalidUrlException.class);
        assertThatThrownBy(() -> service.submit("http://127.0.0.1")).isInstanceOf(UrlRejectedException.class);
        assertThat(repository.store).isEmpty();
        assertThat(queued).isEmpty();
    }
}
