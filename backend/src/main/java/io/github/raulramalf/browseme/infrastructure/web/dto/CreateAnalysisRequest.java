package io.github.raulramalf.browseme.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAnalysisRequest(@NotBlank @Size(max = 2048) String url) {
}
