package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class CanonicalMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.CANONICAL_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!Evidence.isBlank(snapshot.canonical())) {
            return Optional.empty();
        }
        return issue("La página no declara una URL canónica.",
                "Muchas páginas sencillas funcionan bien sin canonical.",
                "Si la misma página se puede abrir con varias direcciones, declara la principal con <link rel=\"canonical\">.");
    }
}
