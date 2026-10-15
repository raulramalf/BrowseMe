package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class ViewportMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.VIEWPORT_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.viewportPresent()) {
            return Optional.empty();
        }
        return issue("La página no tiene la etiqueta <meta name=\"viewport\">.",
                "Es un indicio, no una prueba: no se comprueba cómo se ve realmente en un móvil.",
                "Añade <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">.");
    }
}
