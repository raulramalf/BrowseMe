package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import io.github.raulramalf.browseme.domain.page.VisualMetrics;
import java.util.Optional;

/** Botones o enlaces demasiado pequeños para pulsarlos con el dedo. */
public final class SmallTapTargetsRule implements SeoRule {

    static final int MIN_ELEMENTS = 3;

    @Override
    public RuleId id() {
        return RuleId.SMALL_TAP_TARGETS;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!snapshot.visual().done()) {
            return Optional.empty();
        }
        VisualMetrics metrics = snapshot.visual().metrics();
        if (metrics.smallTapTargets() < MIN_ELEMENTS) {
            return Optional.empty();
        }
        return issue(metrics.smallTapTargets() + " de " + metrics.tapTargetsChecked()
                        + " elementos pulsables miden menos de 24×24 px en móvil.",
                "No se cuentan los enlaces dentro de un párrafo, que WCAG permite más pequeños.",
                "Haz los botones y enlaces sueltos de al menos 24×24 px (mejor 44×44) y sepáralos entre sí.");
    }
}
