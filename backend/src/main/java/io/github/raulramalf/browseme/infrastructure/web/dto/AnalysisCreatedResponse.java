package io.github.raulramalf.browseme.infrastructure.web.dto;

import java.util.UUID;

public record AnalysisCreatedResponse(UUID id, String status) {
}
