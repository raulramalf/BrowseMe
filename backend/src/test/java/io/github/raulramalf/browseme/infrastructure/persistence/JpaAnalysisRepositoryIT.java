package io.github.raulramalf.browseme.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Repositorio y migraciones Flyway contra un PostgreSQL real. Necesita Docker:
 * sin él, el test se omite (en CI sí se ejecuta).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaAnalysisRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-alpine");

    @Autowired
    private AnalysisJpaDao dao;

    private JpaAnalysisRepository repository() {
        return new JpaAnalysisRepository(dao);
    }

    @Test
    void stores_and_reads_a_completed_analysis_with_jsonb() {
        WebsiteAnalysis analysis = PersistenceFixtures.pending();
        JpaAnalysisRepository repository = repository();
        repository.save(analysis);
        WebsiteAnalysis full = PersistenceFixtures.completed();
        repository.save(full);

        WebsiteAnalysis stored = repository.findById(full.id()).orElseThrow();

        assertThat(stored.toSnapshot()).usingRecursiveComparison().ignoringFields("version")
                .isEqualTo(full.toSnapshot());
        assertThat(stored.version()).isZero();
    }

    @Test
    void increments_the_version_and_rejects_stale_writes() {
        JpaAnalysisRepository repository = repository();
        WebsiteAnalysis analysis = PersistenceFixtures.pending();
        repository.save(analysis);
        WebsiteAnalysis stale = repository.findById(analysis.id()).orElseThrow();

        analysis.startFetching();
        repository.save(analysis);
        assertThat(analysis.version()).isEqualTo(1);

        stale.startFetching();
        assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void finds_by_status_and_deletes_expired() {
        JpaAnalysisRepository repository = repository();
        WebsiteAnalysis pending = PersistenceFixtures.pending();
        repository.save(pending);

        assertThat(repository.findByStatusIn(Set.of(AnalysisStatus.PENDING))).extracting(WebsiteAnalysis::id)
                .contains(pending.id());
        assertThat(repository.deleteExpired(pending.expiresAt())).isGreaterThanOrEqualTo(1);
        assertThat(repository.findById(pending.id())).isEmpty();
    }
}
