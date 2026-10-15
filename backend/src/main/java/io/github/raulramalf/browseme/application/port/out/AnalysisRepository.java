package io.github.raulramalf.browseme.application.port.out;

import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AnalysisRepository {

    /** Inserta o actualiza. Las implementaciones aplican bloqueo optimista por versión. */
    void save(WebsiteAnalysis analysis);

    Optional<WebsiteAnalysis> findById(UUID id);

    List<WebsiteAnalysis> findByStatusIn(Set<AnalysisStatus> statuses);

    void delete(UUID id);

    /** @return número de análisis borrados */
    int deleteExpired(Instant now);
}
