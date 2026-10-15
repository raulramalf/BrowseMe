package io.github.raulramalf.browseme.application.port.in;

import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import java.util.Optional;
import java.util.UUID;

public interface GetAnalysisUseCase {

    /** Vacío si no existe o ya ha caducado. */
    Optional<WebsiteAnalysis> get(UUID id);
}
