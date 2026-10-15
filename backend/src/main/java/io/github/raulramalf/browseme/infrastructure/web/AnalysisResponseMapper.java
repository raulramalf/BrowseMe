package io.github.raulramalf.browseme.infrastructure.web;

import io.github.raulramalf.browseme.domain.analysis.AiInterpretation;
import io.github.raulramalf.browseme.domain.analysis.AnalysisStatus;
import io.github.raulramalf.browseme.domain.analysis.ReportLimitations;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import io.github.raulramalf.browseme.domain.page.DesignSignals;
import io.github.raulramalf.browseme.domain.page.EngagementSignals;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.page.VisualCheck;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import io.github.raulramalf.browseme.domain.score.ScoreCalculator;
import io.github.raulramalf.browseme.domain.score.ScoreCard;
import io.github.raulramalf.browseme.domain.seo.IssueSeverity;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.AiDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.AreaScoreDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.DesignDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.EngagementDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.ErrorDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.ImagesDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.IssueDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.PageSummaryDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.RecommendationDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.ScoreDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.VisualDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.VisualMetricsDto;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AnalysisResponseMapper {

    private AnalysisResponseMapper() {
    }

    static AnalysisResponse toResponse(WebsiteAnalysis analysis) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (IssueSeverity severity : IssueSeverity.values()) {
            counts.put(severity.name(), 0);
        }
        analysis.issues().forEach(issue -> counts.merge(issue.severity().name(), 1, Integer::sum));

        return new AnalysisResponse(
                analysis.id(),
                analysis.status().name(),
                analysis.requestedUrl().toString(),
                analysis.finalUrl().map(Object::toString).orElse(null),
                analysis.createdAt(),
                analysis.completedAt().orElse(null),
                analysis.expiresAt(),
                analysis.snapshot().map(AnalysisResponseMapper::summary).orElse(null),
                analysis.issues().stream().map(AnalysisResponseMapper::issue).toList(),
                counts,
                score(analysis),
                ai(analysis),
                ReportLimitations.of(analysis),
                analysis.errorCode().map(code -> new ErrorDto(code.name(), ErrorMessages.of(code))).orElse(null));
    }

    private static PageSummaryDto summary(PageSnapshot s) {
        Map<String, Long> headings = new LinkedHashMap<>();
        for (int level = 1; level <= 6; level++) {
            headings.put("h" + level, s.headingCount(level));
        }
        return new PageSummaryDto(s.httpStatus(), s.redirectCount(), s.https(), s.title(), s.metaDescription(),
                s.lang(), s.canonical(), s.robotsDirectives().stream().sorted().toList(), headings,
                s.links().internal(), s.links().external(),
                new ImagesDto(s.images().total(), s.images().withAlt(), s.images().decorative(), s.images().missingAlt()),
                s.viewportPresent(), s.visibleWordCount(), s.robotsTxt().name(),
                engagement(s.engagement()), design(s.design()), visual(s.visual()));
    }

    /** Estados en los que los hallazgos ya son definitivos y se puede puntuar. */
    private static final Set<AnalysisStatus> SCORABLE = Set.of(AnalysisStatus.INTERPRETING,
            AnalysisStatus.COMPLETED, AnalysisStatus.COMPLETED_WITHOUT_AI);

    private static ScoreDto score(WebsiteAnalysis analysis) {
        if (!SCORABLE.contains(analysis.status()) || analysis.snapshot().isEmpty()) {
            return null;
        }
        ScoreCard card = ScoreCalculator.calculate(analysis.issues(), analysis.snapshot().get().visual().status());
        Map<String, Double> penalties = new LinkedHashMap<>();
        for (IssueSeverity severity : IssueSeverity.values()) {
            penalties.put(severity.name(), ScoreCalculator.PENALTY_TENTHS.get(severity) / 10.0);
        }
        return new ScoreDto(card.tenths() / 10.0, card.partial(),
                card.areas().stream().map(a -> new AreaScoreDto(a.area().name(), a.tenths() / 10.0,
                        a.weightPercent(), a.findings(), a.partial())).toList(),
                penalties);
    }

    private static EngagementDto engagement(EngagementSignals e) {
        return new EngagementDto(e.phoneLinks(), e.emailLinks(), e.whatsappLinks(), e.forms(), e.emailForms(),
                e.callsToAction(), e.socialNetworks().stream().sorted().toList(),
                e.schemaTypes().stream().sorted().toList(), e.testimonials(), e.openGraphTitle(), e.openGraphImage());
    }

    private static DesignDto design(DesignSignals d) {
        return new DesignDto(d.navigation(), d.favicon(), d.genericLinkTexts(), d.emptyInteractive(), d.formFields(),
                d.unlabeledFormFields(), d.imagesWithoutDimensions());
    }

    private static VisualDto visual(VisualCheck v) {
        VisualMetrics m = v.metrics();
        VisualMetricsDto metrics = m == null ? null : new VisualMetricsDto(m.viewportWidth(), m.contentWidth(),
                m.textElementsChecked(), m.lowContrastTexts(), m.smallTexts(), m.tapTargetsChecked(),
                m.smallTapTargets(), m.fontFamilies(), m.callToActionAboveFoldMobile(),
                m.callToActionAboveFoldDesktop(), m.pageHeightScreens());
        return new VisualDto(v.status().name(), metrics, v.mobileScreenshot() != null,
                v.desktopScreenshot() != null);
    }

    private static IssueDto issue(SeoIssue issue) {
        return new IssueDto(issue.ruleId().name(), issue.category().name(), issue.severity().name(),
                issue.area().name(), RuleTitles.of(issue.ruleId()), issue.evidence(), issue.limitation(), issue.recommendation());
    }

    private static AiDto ai(WebsiteAnalysis analysis) {
        if (analysis.aiStatus().isEmpty()) {
            return null;
        }
        String status = analysis.aiStatus().get().name();
        if (analysis.ai().isEmpty()) {
            return new AiDto(status, null, List.of(), List.of(), List.of(), List.of());
        }
        AiInterpretation ai = analysis.ai().get();
        return new AiDto(status, ai.summary(), ai.strengths(), ai.opportunities(),
                ai.recommendations().stream().map(r -> new RecommendationDto(r.text(), r.priority().name(),
                        r.ruleIds().stream().map(Enum::name).sorted().toList(), r.generalSuggestion())).toList(),
                ai.actionPlan());
    }
}
