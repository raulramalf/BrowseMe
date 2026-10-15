package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class H1MissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.H1_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.headingCount(1) > 0) {
            return Optional.empty();
        }
        return issue("La página no tiene ningún encabezado H1.",
                "Si los encabezados se generan con JavaScript, este análisis no los ve.",
                "Añade un H1 que resuma de qué trata la página.");
    }
}
