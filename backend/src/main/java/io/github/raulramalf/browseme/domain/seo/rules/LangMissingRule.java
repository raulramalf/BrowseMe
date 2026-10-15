package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

public final class LangMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.LANG_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (!Evidence.isBlank(snapshot.lang())) {
            return Optional.empty();
        }
        return issue("La etiqueta <html> no declara el idioma (atributo lang).",
                "",
                "Añade lang=\"es\" (o el idioma que corresponda) a la etiqueta <html>.");
    }
}
