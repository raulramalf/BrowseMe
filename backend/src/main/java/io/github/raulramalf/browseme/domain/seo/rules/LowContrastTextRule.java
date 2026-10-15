package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import java.util.Optional;

/** Textos con poco contraste respecto a su fondo (por debajo de WCAG AA). */
public final class LowContrastTextRule implements SeoRule {

    static final int MIN_ELEMENTS = 3;

    @Override
    public RuleId id() {
        return RuleId.LOW_CONTRAST_TEXT;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!snapshot.visual().done()) {
            return Optional.empty();
        }
        VisualMetrics metrics = snapshot.visual().metrics();
        if (metrics.lowContrastTexts() < MIN_ELEMENTS) {
            return Optional.empty();
        }
        String evidence = metrics.lowContrastTexts() + " de " + metrics.textElementsChecked()
                + " textos revisados no llegan al contraste mínimo WCAG AA (4,5:1; 3:1 en textos grandes).";
        if (!metrics.lowContrastSamples().isEmpty()) {
            evidence += " Por ejemplo: " + metrics.lowContrastSamples().stream().map(Evidence::quote)
                    .reduce((a, b) -> a + ", " + b).orElse("") + ".";
        }
        return issue(evidence,
                "Los textos sobre imágenes o degradados no se pueden medir y no se cuentan.",
                "Oscurece el texto o aclara el fondo hasta llegar a 4,5:1; se lee mejor en el móvil y a pleno sol.");
    }
}
