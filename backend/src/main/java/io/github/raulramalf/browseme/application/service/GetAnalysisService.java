package io.github.raulramalf.browseme.application.service;

import io.github.raulramalf.browseme.application.port.in.GetAnalysisUseCase;
import io.github.raulramalf.browseme.application.port.out.AnalysisRepository;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public final class GetAnalysisService implements GetAnalysisUseCase {

    private final AnalysisRepository repository;
    private final Clock clock;

    public GetAnalysisService(AnalysisRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Optional<WebsiteAnalysis> get(UUID id) {
        // Un análisis caducado es invisible aunque la limpieza horaria aún no lo haya borrado.
        return repository.findById(id).filter(analysis -> !analysis.isExpired(clock.instant()));
    }
}
