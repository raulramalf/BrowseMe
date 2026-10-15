package io.github.raulramalf.browseme.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Informe expuesto por la API. Nunca se exponen las clases del dominio. */
public record AnalysisResponse(
        UUID id,
        String status,
        String requestedUrl,
        String finalUrl,
        Instant createdAt,
        Instant analyzedAt,
        Instant expiresAt,
        PageSummaryDto pageSummary,
        List<IssueDto> issues,
        Map<String, Integer> severityCounts,
        AiDto ai,
        List<String> limitations,
        ErrorDto error) {

    public record PageSummaryDto(int httpStatus, int redirectCount, boolean https, String title,
                                 String metaDescription, String lang, String canonical, List<String> robotsDirectives,
                                 Map<String, Long> headingCounts, int internalLinks, int externalLinks,
                                 ImagesDto images, boolean viewportPresent, int visibleWordCount, String robotsTxt) {
    }

    public record ImagesDto(int total, int withAlt, int decorative, int missingAlt) {
    }

    public record IssueDto(String ruleId, String category, String severity, String title, String evidence,
                           String limitation, String recommendation) {
    }

    /** {@code status} es AVAILABLE, UNAVAILABLE o INVALID_OUTPUT; el resto es null si no hay IA. */
    public record AiDto(String status, String summary, List<String> strengths, List<String> opportunities,
                        List<RecommendationDto> recommendations, List<String> actionPlan) {
    }

    public record RecommendationDto(String text, String priority, List<String> ruleIds, boolean generalSuggestion) {
    }

    public record ErrorDto(String code, String message) {
    }
}
