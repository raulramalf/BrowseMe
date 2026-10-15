package io.github.raulramalf.browseme.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AnalysisJpaDao extends JpaRepository<AnalysisEntity, UUID> {

    List<AnalysisEntity> findByStatusIn(Collection<String> statuses);

    /** Transacción propia: no depende de que el adaptador que la llama esté envuelto en un proxy. */
    @Transactional
    @Modifying
    @Query("delete from AnalysisEntity a where a.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
