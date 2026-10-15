package io.github.raulramalf.browseme.infrastructure.persistence;

import io.github.raulramalf.browseme.domain.analysis.AiInterpretation;
import io.github.raulramalf.browseme.domain.analysis.AiStatus;
import io.github.raulramalf.browseme.domain.analysis.AnalysisErrorCode;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import java.net.URI;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Traduce entre el agregado y la fila. Los value objects del dominio son
 * records, así que se serializan tal cual a JSON; al leerlos se vuelven a
 * construir por su constructor, que repite las validaciones y los recortes.
 */
final class AnalysisJsonMapper {

    private static final TypeReference<List<SeoIssue>> ISSUES = new TypeReference<>() { };

    private final JsonMapper json = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    void copyInto(WebsiteAnalysis analysis, AnalysisEntity entity) {
        WebsiteAnalysis.Snapshot state = analysis.toSnapshot();
        entity.setRequestedUrl(state.requestedUrl().toString());
        entity.setFinalUrl(state.finalUrl() == null ? null : state.finalUrl().toString());
        entity.setStatus(state.status().name());
        entity.setSnapshot(state.snapshot() == null ? null : json.writeValueAsString(state.snapshot()));
        entity.setIssues(json.writeValueAsString(state.issues()));
        entity.setAiResult(state.ai() == null ? null : json.writeValueAsString(state.ai()));
        entity.setAiStatus(state.aiStatus() == null ? null : state.aiStatus().name());
        entity.setErrorCode(state.errorCode() == null ? null : state.errorCode().name());
        entity.setCreatedAt(state.createdAt());
        entity.setCompletedAt(state.completedAt());
        entity.setExpiresAt(state.expiresAt());
        entity.setVersion(state.version());
    }

    WebsiteAnalysis toDomain(AnalysisEntity entity) {
        return WebsiteAnalysis.restore(new WebsiteAnalysis.Snapshot(
                entity.getId(),
                URI.create(entity.getRequestedUrl()),
                entity.getFinalUrl() == null ? null : URI.create(entity.getFinalUrl()),
                AnalysisStatus.valueOf(entity.getStatus()),
                entity.getSnapshot() == null ? null : json.readValue(entity.getSnapshot(), PageSnapshot.class),
                entity.getIssues() == null ? List.of() : json.readValue(entity.getIssues(), ISSUES),
                entity.getAiResult() == null ? null : json.readValue(entity.getAiResult(), AiInterpretation.class),
                entity.getAiStatus() == null ? null : AiStatus.valueOf(entity.getAiStatus()),
                entity.getErrorCode() == null ? null : AnalysisErrorCode.valueOf(entity.getErrorCode()),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getExpiresAt(),
                entity.getVersion()));
    }
}
