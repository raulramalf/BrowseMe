package io.github.raulramalf.browseme.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import java.util.ConcurrentModificationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InMemoryAnalysisRepositoryTest {

    private final InMemoryAnalysisRepository repository = new InMemoryAnalysisRepository();

    @Test
    void saves_versions_and_reads_back_copies() {
        WebsiteAnalysis analysis = PersistenceFixtures.pending();
        repository.save(analysis);
        assertThat(analysis.version()).isZero();

        analysis.startFetching();
        repository.save(analysis);

        WebsiteAnalysis stored = repository.findById(analysis.id()).orElseThrow();
        assertThat(stored.version()).isEqualTo(1);
        assertThat(stored.status()).isEqualTo(AnalysisStatus.FETCHING);
        assertThat(stored).isNotSameAs(analysis);
    }

    @Test
    void rejects_stale_writes() {
        WebsiteAnalysis analysis = PersistenceFixtures.pending();
        repository.save(analysis);
        WebsiteAnalysis stale = repository.findById(analysis.id()).orElseThrow();
        analysis.startFetching();
        repository.save(analysis);

        stale.startFetching();

        assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void filters_by_status_and_deletes_expired() {
        WebsiteAnalysis pending = PersistenceFixtures.pending();
        WebsiteAnalysis completed = PersistenceFixtures.completed();
        repository.save(pending);
        repository.save(completed);

        assertThat(repository.findByStatusIn(Set.of(AnalysisStatus.PENDING))).extracting(WebsiteAnalysis::id)
                .containsExactly(pending.id());
        assertThat(repository.deleteExpired(pending.expiresAt())).isEqualTo(2);
        assertThat(repository.findById(pending.id())).isEmpty();
    }
}
