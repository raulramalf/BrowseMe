package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class MetaDescriptionMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.META_DESC_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!Evidence.isBlank(snapshot.metaDescription())) {
            return Optional.empty();
        }
        String evidence = snapshot.metaDescription() == null
                ? "La página no tiene metadescripción."
                : "La metadescripción está vacía.";
        return issue(evidence,
                "Google puede generar su propio fragmento a partir del contenido aunque exista metadescripción.",
                "Añade una metadescripción que resuma la página y anime a hacer clic.");
    }
}
