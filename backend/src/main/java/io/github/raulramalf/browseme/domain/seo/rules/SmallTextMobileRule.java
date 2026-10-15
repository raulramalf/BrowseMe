package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import java.util.Optional;

/** Textos de menos de 12 px en móvil. */
public final class SmallTextMobileRule implements SeoRule {

    static final int MIN_ELEMENTS = 3;

    @Override
    public RuleId id() {
        return RuleId.SMALL_TEXT_MOBILE;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!snapshot.visual().done()) {
            return Optional.empty();
        }
        VisualMetrics metrics = snapshot.visual().metrics();
        if (metrics.smallTexts() < MIN_ELEMENTS) {
            return Optional.empty();
        }
        return issue(metrics.smallTexts() + " de " + metrics.textElementsChecked()
                        + " textos se muestran a menos de 12 px en una pantalla de móvil.",
                "Algunos textos pequeños (avisos legales, notas) pueden ser intencionados.",
                "Usa al menos 16 px para el texto principal y no bajes de 12 px en notas.");
    }
}
