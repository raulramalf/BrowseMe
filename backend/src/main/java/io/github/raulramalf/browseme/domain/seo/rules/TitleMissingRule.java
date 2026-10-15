package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class TitleMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.TITLE_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.title() != null && !snapshot.title().isBlank()) {
            return Optional.empty();
        }
        String evidence = snapshot.title() == null
                ? "La página no tiene etiqueta <title>."
                : "La etiqueta <title> está vacía.";
        return issue(evidence,
                "Si el título se añade con JavaScript, este análisis no lo ve.",
                "Añade un título único que diga qué ofrece la página y el nombre del negocio.");
    }
}
