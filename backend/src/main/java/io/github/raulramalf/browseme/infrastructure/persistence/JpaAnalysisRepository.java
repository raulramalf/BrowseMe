package io.github.raulramalf.browseme.infrastructure.persistence;

import io.github.raulramalf.browseme.application.port.out.AnalysisRepository;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador PostgreSQL. El bloqueo optimista lo aplica Hibernate con {@code @Version}. */
public class JpaAnalysisRepository implements AnalysisRepository {

    private final AnalysisJpaDao dao;
    private final AnalysisJsonMapper mapper = new AnalysisJsonMapper();

    public JpaAnalysisRepository(AnalysisJpaDao dao) {
        this.dao = dao;
    }

    @Override
    @Transactional
    public void save(WebsiteAnalysis analysis) {
        AnalysisEntity entity = new AnalysisEntity(analysis.id());
        mapper.copyInto(analysis, entity);
        AnalysisEntity saved = dao.saveAndFlush(entity);
        analysis.assignVersion(saved.getVersion());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WebsiteAnalysis> findById(UUID id) {
        return dao.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebsiteAnalysis> findByStatusIn(Set<AnalysisStatus> statuses) {
        return dao.findByStatusIn(statuses.stream().map(Enum::name).toList()).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        dao.deleteById(id);
    }

    @Override
    @Transactional
    public int deleteExpired(Instant now) {
        return dao.deleteExpired(now);
    }
}
