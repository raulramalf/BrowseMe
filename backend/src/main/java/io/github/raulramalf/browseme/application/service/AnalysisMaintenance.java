package io.github.raulramalf.browseme.application.service;

import io.github.raulramalf.browseme.application.port.out.AnalysisRepository;
import io.github.raulramalf.browseme.domain.analysis.AnalysisErrorCode;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;

/** Tareas de mantenimiento: caducidad y recuperación tras un reinicio. */
public final class AnalysisMaintenance {

    private static final Set<AnalysisStatus> IN_PROGRESS = EnumSet.of(
            AnalysisStatus.PENDING, AnalysisStatus.FETCHING, AnalysisStatus.ANALYZING, AnalysisStatus.INTERPRETING);

    private final AnalysisRepository repository;
    private final Clock clock;

    public AnalysisMaintenance(AnalysisRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** @return análisis borrados por caducidad */
    public int purgeExpired() {
        return repository.deleteExpired(clock.instant());
    }

    /**
     * Al arrancar no hay ningún análisis en ejecución, así que los que quedaron
     * en un estado intermedio se marcan como interrumpidos.
     *
     * @return análisis marcados como {@code FAILED}
     */
    public int failInterrupted() {
        int count = 0;
        for (WebsiteAnalysis analysis : repository.findByStatusIn(IN_PROGRESS)) {
            analysis.fail(AnalysisErrorCode.INTERRUPTED, clock.instant());
            repository.save(analysis);
            count++;
        }
        return count;
    }
}
