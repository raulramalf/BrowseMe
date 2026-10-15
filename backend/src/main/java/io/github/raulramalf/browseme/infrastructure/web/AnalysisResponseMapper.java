package io.github.raulramalf.browseme.infrastructure.web;

import io.github.raulramalf.browseme.domain.analysis.AiInterpretation;
import io.github.raulramalf.browseme.domain.analysis.ReportLimitations;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.IssueSeverity;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.AiDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.ErrorDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.ImagesDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.IssueDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.PageSummaryDto;
import io.github.raulramalf.browseme.infrastructure.web.dto.AnalysisResponse.RecommendationDto;
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
                s.viewportPresent(), s.visibleWordCount(), s.robotsTxt().name());
    }

    private static IssueDto issue(SeoIssue issue) {
        return new IssueDto(issue.ruleId().name(), issue.category().name(), issue.severity().name(),
                RuleTitles.of(issue.ruleId()), issue.evidence(), issue.limitation(), issue.recommendation());
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
