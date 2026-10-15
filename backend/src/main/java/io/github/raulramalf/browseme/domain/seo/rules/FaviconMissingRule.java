package io.github.raulramalf.browseme.domain.seo.rules;

import io.github.raulramalf.browseme.domain.page.PageSnapshot;
import io.github.raulramalf.browseme.domain.seo.RuleId;
import io.github.raulramalf.browseme.domain.seo.SeoIssue;
import io.github.raulramalf.browseme.domain.seo.SeoRule;
import java.util.Optional;

/** Sin icono del sitio (favicon) declarado. */
public final class FaviconMissingRule implements SeoRule {

    @Override
    public RuleId id() {
        return RuleId.FAVICON_MISSING;
    }

    @Override
    public Optional<SeoIssue> evaluate(PageSnapshot snapshot) {
        if (snapshot.design().favicon()) {
            return Optional.empty();
        }
        return issue("No hay ningún <link rel=\"icon\">.",
                "Los navegadores buscan /favicon.ico por defecto; puede existir sin declararse.",
                "Declara un favicon con tu logotipo: hace tu web reconocible en pestañas, favoritos y resultados de búsqueda.");
    }
}
