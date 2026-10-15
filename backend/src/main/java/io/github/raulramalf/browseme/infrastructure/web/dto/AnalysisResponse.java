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
        ScoreDto score,
        AiDto ai,
        List<String> limitations,
        ErrorDto error) {

    public record PageSummaryDto(int httpStatus, int redirectCount, boolean https, String title,
                                 String metaDescription, String lang, String canonical, List<String> robotsDirectives,
                                 Map<String, Long> headingCounts, int internalLinks, int externalLinks,
                                 ImagesDto images, boolean viewportPresent, int visibleWordCount, String robotsTxt,
                                 EngagementDto engagement, DesignDto design, VisualDto visual) {
    }

    public record EngagementDto(int phoneLinks, int emailLinks, int whatsappLinks, int forms, int emailForms,
                                int callsToAction, List<String> socialNetworks, List<String> schemaTypes,
                                boolean testimonials, boolean openGraphTitle, boolean openGraphImage) {
    }

    public record DesignDto(boolean navigation, boolean favicon, int genericLinkTexts, int emptyInteractive,
                            int formFields, int unlabeledFormFields, int imagesWithoutDimensions) {
    }

    /**
     * {@code status}: DONE, DISABLED, BROWSER_NOT_FOUND, FAILED o NOT_RUN. Las
     * métricas solo existen con DONE; las capturas se piden aparte.
     */
    public record VisualDto(String status, VisualMetricsDto metrics, boolean mobileScreenshot,
                            boolean desktopScreenshot) {
    }

    public record VisualMetricsDto(int viewportWidth, int contentWidth, int textElementsChecked,
                                   int lowContrastTexts, int smallTexts, int tapTargetsChecked, int smallTapTargets,
                                   List<String> fontFamilies, boolean callToActionAboveFoldMobile,
                                   boolean callToActionAboveFoldDesktop, int pageHeightScreens) {
    }

    /**
     * Nota de 0 a 10 con una decimal, su desglose y la fórmula con la que se
     * calcula, para que el informe pueda explicarla.
     */
    public record ScoreDto(double value, boolean partial, List<AreaScoreDto> areas, Map<String, Double> penalties) {
    }

    public record AreaScoreDto(String area, double value, int weightPercent, int findings, boolean partial) {
    }

    public record ImagesDto(int total, int withAlt, int decorative, int missingAlt) {
    }

    /**
     * @param scoreGain     puntos (0 a 10) que subiría la nota global si se corrigiera este hallazgo
     * @param why           por qué importa (texto fijo de la guía, no dato de la página)
     * @param steps         pasos concretos para corregirlo (texto fijo de la guía)
     * @param affectsClients influye directamente en que lleguen clientes
     */
    public record IssueDto(String ruleId, String category, String severity, String area, String title,
                           String evidence, String limitation, String recommendation, double scoreGain,
                           String why, List<String> steps, boolean affectsClients) {
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
