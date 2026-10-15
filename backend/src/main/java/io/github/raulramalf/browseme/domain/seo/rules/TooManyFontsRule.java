package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Demasiadas familias tipográficas distintas. */
public final class TooManyFontsRule implements SeoRule {

    static final int MAX_FAMILIES = 3;

    @Override
    public RuleId id() {
        return RuleId.TOO_MANY_FONTS;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!snapshot.visual().done()) {
            return Optional.empty();
        }
        VisualMetrics metrics = snapshot.visual().metrics();
        if (metrics.fontFamilies().size() <= MAX_FAMILIES) {
            return Optional.empty();
        }
        return issue("Se usan " + metrics.fontFamilies().size() + " familias tipográficas: "
                        + String.join(", ", metrics.fontFamilies().stream().map(Evidence::quote).toList()) + ".",
                "Las fuentes de iconos también cuentan como familias tipográficas.",
                "Limita el diseño a una o dos familias (una para títulos y otra para texto) para dar una imagen más cuidada.");
    }
}
