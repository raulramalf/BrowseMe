package io.github.raulramalf.browseme.infrastructure.persistence;

import io.github.raulramalf.browseme.domain.analysis.AiInterpretation;
import io.github.raulramalf.browseme.domain.analysis.Priority;
import io.github.raulramalf.browseme.domain.analysis.Recommendation;
import io.github.raulramalf.browseme.domain.analysis.WebsiteAnalysis;
import io.github.raulramalf.browseme.domain.page.Heading;
import io.github.raulramalf.browseme.domain.page.VisualCheck;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SnapshotFixture;
import io.github.raulramalf.browseme.domain.url.UrlPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class PersistenceFixtures {

    static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    private PersistenceFixtures() {
    }

    static WebsiteAnalysis pending() {
        return WebsiteAnalysis.create(UUID.randomUUID(), new UrlPolicy().parse("https://example.com/página"), NOW,
                Duration.ofDays(7));
    }

    /** Análisis completo con todos los campos rellenos, incluidos textos «hostiles» y no ASCII. */
    static WebsiteAnalysis completed() {
        WebsiteAnalysis analysis = pending();
        analysis.startFetching();
        analysis.startAnalyzing(SnapshotFixture.perfect()
                .finalUrl("https://example.com/final")
                .title("<script>alert('x')</script> Panadería «Ñandú» 😀")
                .robots("noindex", "follow")
                .headings(new Heading(1, "Hola"), new Heading(3, "Salto"))
                .images(3, 1, 1, 1)
                .visual(VisualCheck.done(SnapshotFixture.goodVisual(), new byte[] {(byte) 0xFF, (byte) 0xD8, 1},
                        new byte[] {(byte) 0xFF, (byte) 0xD8, 2}))
                .build());
        analysis.startInterpreting(List.of(
                new SeoIssue(RuleId.NOINDEX_DETECTED, "Incluye «noindex»", "Puede ser intencionado", "Quítalo"),
                new SeoIssue(RuleId.LANG_MISSING, "Sin lang", "", "Añade lang")));
        analysis.complete(new AiInterpretation("Resumen", List.of("Fuerte"), List.of("Mejora"),
                List.of(new Recommendation("Quita noindex", Priority.HIGH, Set.of(RuleId.NOINDEX_DETECTED), false),
                        new Recommendation("Publica más", Priority.LOW, Set.of(), true)),
                List.of("Paso 1")), NOW.plusSeconds(40));
        return analysis;
    }
}
