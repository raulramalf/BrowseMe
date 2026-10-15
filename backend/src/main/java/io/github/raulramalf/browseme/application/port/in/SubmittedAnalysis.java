package io.github.raulramalf.browseme.application.port.in;

import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import java.util.UUID;

public record SubmittedAnalysis(UUID id, AnalysisStatus status) {
}
